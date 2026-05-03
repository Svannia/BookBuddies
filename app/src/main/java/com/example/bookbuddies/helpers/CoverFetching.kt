package com.example.bookbuddies.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.bookbuddies.BuildConfig
import com.example.bookbuddies.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

/**
 * Uses books ISBN to find and replace their covers.
 *
 * @param context to access local files
 * @param books list of books to check for a cover
 * @param insertBook a suspend lambda that receives a book and inserts it in the repository
 * @param updateMangaSeriesId a suspend lambda that receives a Mangadex ID and a series name, and updates all mangas of that series with the new Mangadex ID
 * @param callBack function to be called after the covers are updated, returning a list of books whose cover hasn't been found
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @param onProgress block that receives the numbered of currently processed covers and the total number, and updates the progress accordingly
 */
suspend fun findBookCovers(
    context: Context,
    books: List<Book>,
    insertBook: suspend (Book) -> Unit,
    updateMangaSeriesId: suspend (String, String) -> Unit,
    callBack: (List<String>) -> Unit,
    isError: (Boolean) -> Unit,
    onProgress: (Int, Int) -> Unit
) = coroutineScope {
    val semaphore = Semaphore(5) // max 5 parallel downloads to avoid DDOS the APIs that fetch book covers
    val failedBooks = mutableListOf<String>()
    val mutex = Mutex() // lock to protect access to shared variables
    val total = books.size
    var processed = 0

    onProgress(processed, total)
    // parallelize the processes on all books over 5 threads
    try {
        val jobs = books.map { book ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        // use different strategies for manga and regular books
                        val isManga = book.format.contains("manga", ignoreCase = true)

                        // regular book APIs use ISBN, manga API searches by series title
                        val reference = if (isManga) book.seriesName else book.isbn
                        if (reference.isNotBlank() && book.cover.isNullOrBlank()) {
                            val updatedBook = if (isManga) fetchCoverForManga(context, book, updateMangaSeriesId) else fetchCoverForBook(context, book)

                            // if no cover was found -> mark as failed
                            if (updatedBook.cover.isNullOrBlank()) {
                                mutex.withLock {
                                    failedBooks.add(book.title)
                                    Timber.tag("BookCover").d("${book.title}: No cover found. Adding to failed list.")
                                }
                            }

                            insertBook(updatedBook)
                        }

                    } catch (e: Exception) {
                        mutex.withLock {
                            failedBooks.add(book.title)
                            Timber.tag("BookCover").e("Failed to process book ${book.uid} with error $e")
                        }
                        isError(true)
                    } finally {
                        mutex.withLock {
                            processed++
                            withContext(Dispatchers.Main) {
                                onProgress(processed, total)
                            }
                        }
                    }
                }
            }
        }

        // wait for all jobs to complete before calling the callback
        jobs.awaitAll()

        withContext(Dispatchers.Main) {
            callBack(failedBooks)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Timber.tag("BookCover").e("Failed to fetch covers with error $e")
            isError(true)
        }
    }
}

/**
 * Given a book (that is not a manga), uses its ISBN to try and fetch a cover.
 *
 * @param context to access local files
 * @param book Book object whose cover is being searched for
 * @param allowOverwrite false by default. If true, will look for a new cover even if one already exists.
 * @return new Book object with the updated cover (same object as given parameter if no cover is found)
 */
suspend fun fetchCoverForBook(context: Context, book: Book, allowOverwrite: Boolean = false): Book =
    withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
        val fileName = "${book.uid}.jpg"
        val file = File(context.filesDir, fileName)

        // if the cover is already stored locally -> use it
        if (!allowOverwrite && file.exists()) {
            return@withContext book.copy(cover = file.absolutePath)
        }

        var savedPath: String? = null

        // 1. Try Google Books API
        val googleURL = "https://www.googleapis.com/books/v1/volumes?q=isbn:${book.isbn}&key=$apiKey"
        var attempt = 0
        val maxRetries = 4

        while (attempt < maxRetries && savedPath == null) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(googleURL)
                    .header("User-Agent", "BookBuddiesApp/1.0")
                    .build()
                val response = client.newCall(request).execute()

                // if hit rate limit, retry with exponential backoff
                if (response.code == 429) {
                    val delayTime = (1000L * 2.0.pow(attempt.toDouble())).toLong()
                    Timber.tag("BookCover").d("${book.title}: Hit Google Books API rate limit. Retrying in ${delayTime}ms.")
                    delay(delayTime)
                    attempt++
                    continue
                }

                // if the API correctly responded, try to extract the cover URL
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val coverUrl = Regex("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"")
                        .find(json ?: "")?.groupValues?.get(1)
                        ?.replace("http://", "https://")

                    if (!coverUrl.isNullOrBlank()) {
                        savedPath = downloadAndSaveCover(coverUrl, file)
                        if (savedPath != null) {
                            Timber.tag("BookCover").d("${book.title}: Fetched cover from Google Books.")
                            break
                        }
                    }
                } else {
                    Timber.tag("BookCover").e("${book.title}: Google Books API failed and returned ${response.code}")
                }

            } catch (e: Exception) {
                Timber.tag("BookCover").e("${book.title}: Google Books attempt failed with $e")
                val delayTime = (500L * 2.0.pow(attempt.toDouble())).toLong()
                delay(delayTime)
            }
            attempt++
        }

        // 2. Try Open Library API
        if (savedPath == null) {
            val openLibraryURL = "https://covers.openlibrary.org/b/isbn/${book.isbn}-L.jpg"
            try {
                val localPath = validateAndSaveCover(context, openLibraryURL, book.uid)
                if (localPath != null) {
                    savedPath = localPath
                    Timber.tag("BookCover").d("${book.title}: Fetched and validated cover from Open Library.")
                } else {
                    Timber.tag("BookCover").d("${book.title}: Open Library returned invalid placeholder.")
                }
            } catch (e: Exception) {
                Timber.tag("BookCover").e("${book.title}: OpenLibrary attempt failed with $e")
            }
        }

        if (allowOverwrite && savedPath == null) {
            return@withContext book
        }
        return@withContext book.copy(cover = savedPath)
    }

/**
 * Given a book that was detected as a manga, uses its series name to try and fetch a cover.
 *
 * @param context to access local files
 * @param book Book object for the manga whose cover is being searched for
 * @param updateMangaSeriesId suspend lambda that updates all books within a series with a new Mangadex ID
 * @param allowOverwrite false by default. If true, will look for a new cover even if one already exists.
 * @return new Book object with the new cover (or same object as passed in parameter if no cover was found)
 */
suspend fun fetchCoverForManga(context: Context, book: Book, updateMangaSeriesId: suspend (String, String) -> Unit, allowOverwrite: Boolean = false): Book =
    withContext(Dispatchers.IO) {
        val fileName = "${book.uid}.jpg"
        val file = File(context.filesDir, fileName)

        // if the cover is already stored locally -> use it
        if (!allowOverwrite && file.exists()) {
            return@withContext book.copy(cover = file.absolutePath)
        }

        var savedPath: String? = null

        // Use MangaDex search API to find manga by series

        // Full URL is "https://api.mangadex.org/manga?title=${book.seriesName}" -> using builder to encode special characters
        val searchEndpoint = HttpUrl.Builder()
            .scheme("https")
            .host("api.mangadex.org")
            .addPathSegment("manga")
            .addQueryParameter("title", book.seriesName)
            .build()

        try {
            // if the mangaID is already known, skip the series search step
            var mangaId = book.mangaSeriesId
            val client = OkHttpClient()

            if (mangaId == null) {
                // 1. search at searchEndpoint
                val request = Request.Builder()
                    .url(searchEndpoint)
                    .header("User-Agent", "BookBuddiesApp/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Timber.tag("BookCover").e("${book.title}: MangaDex search API failed and returned ${response.code}")
                    return@withContext book
                }

                val body = response.body?.string() ?: return@withContext book
                val json = JSONObject(body)
                val results = json.optJSONArray("data") ?: return@withContext book

                // 2. among results, find the one with title matching series name (ignoring case and all characters that are not letters, like - or ' or :)
                fun normalize(str: String) = str.lowercase().replace(Regex("[^a-z0-9]"), "")
                val targetTitle = normalize(book.seriesName)

                for (i in 0 until results.length()) {
                    val manga = results.getJSONObject(i)
                    val attributes = manga.getJSONObject("attributes")
                    val candidateTitles = mutableListOf<String>()

                    val titles = attributes.getJSONObject("title") ?: continue
                    titles.keys().forEach { lang ->
                        val titleValue = titles.optString(lang)
                        if (titleValue.isNotBlank()) candidateTitles.add(titleValue)
                    }
                    val alternateTitles = attributes.optJSONArray("altTitles")
                    if (alternateTitles != null) {
                        for (i in 0 until alternateTitles.length()) {
                            val altObj = alternateTitles.getJSONObject(i)
                            altObj.keys().forEach { lang ->
                                val altTitle = altObj.optString(lang)
                                if (altTitle.isNotBlank()) candidateTitles.add(altTitle)
                            }
                        }
                    }

                    // 3. get id of that result
                    val match = candidateTitles.any { normalize(it) == targetTitle }
                    if (match) {
                        mangaId = manga.getString("id")
                        break
                    }
                }

                // Update mangaID for all books in the same series
                if (mangaId != null) {
                    updateMangaSeriesId(mangaId, book.seriesName)
                }

            }

            // 4. go to URL https://api.mangadex.org/cover?manga[]=<manga_id>&limit=100
            if (mangaId == null) {
                Timber.tag("BookCover").d("${book.title}: No matching manga found on MangaDex")
                return@withContext book
            }

            val coversEndpoint = "https://api.mangadex.org/cover?manga[]=${mangaId}&limit=100"

            val coversRequest = Request.Builder()
                .url(coversEndpoint).header("User-Agent", "BookBuddiesApp/1.0").build()
            val coversResponse = client.newCall(coversRequest).execute()

            if (!coversResponse.isSuccessful) {
                Timber.tag("BookCover").e("${book.title}: MangaDex covers fetch failed and returned ${coversResponse.code}")
                return@withContext book
            }

            val coversJson = JSONObject(coversResponse.body?.string() ?: "")
            val coversResults = coversJson.optJSONArray("data") ?: return@withContext book

            // 5. among results, find the one with volume = seriesNumber
            var volumeFile: String? = null
            for (i in 0 until coversResults.length()) {
                val coverObj = coversResults.getJSONObject(i)
                val coverAttr = coverObj.getJSONObject("attributes")

                val volume = coverAttr.optString("volume")
                if (volume == book.seriesNumber.toString()) {
                    // 6. If there are multiple results, chose locale = book.language, else choose en, else choose ja
                    val locale = coverAttr.optString("locale", "en")
                    if (book.language == locale || volumeFile == null) {
                        volumeFile = coverAttr.getString("fileName")
                        if (book.language == locale) break

                    }
                }
            }

            // 7. Use downloadAndSaveCover with URL https://uploads.mangadex.org/covers/<manga_id>/<filename>
            if (volumeFile != null) {
                val coverURL = "https://uploads.mangadex.org/covers/${mangaId}/${volumeFile}"
                savedPath = downloadAndSaveCover(coverURL, file)
                if (savedPath != null) {
                    Timber.tag("BookCover").d("${book.title}: Fetched cover from MangaDex.")
                }
            } else {
                Timber.tag("BookCover").d("${book.title}: No matching volume found on MangaDex")
            }

        } catch (e: Exception) {
            Timber.tag("BookCover").e("${book.title}: MangaDex search attempt failed with $e")
        }

        if (allowOverwrite && savedPath == null) {
            return@withContext book
        }
        return@withContext book.copy(cover = savedPath)
    }

/**
 * Downloads an image from a web page and saves a local copy on the phone's app files.
 *
 * @param coverURL URL of the web page containing just the cover image
 * @param file local File where the image copy should be kept
 * @return the absolute path of the image copy
 */
private fun downloadAndSaveCover(coverURL: String, file: File): String? {
    return try {
        val url = URL(coverURL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            file.outputStream().use { outputStream ->
                connection.inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } else null
    } catch (e: Exception) {
        Timber.tag("BookCover").e("Failed to download cover from $coverURL with error $e")
        null
    }
}

/**
 * Specifically for OpenLibrary: some book cover searches return a single white pixel instead of a clear no-result.
 * This function filters such results out as invalid placeholders.
 * If the cover image is valid, save a local copy.
 *
 * @param context to access local files
 * @param coverURL URL of the web page containing just the cover image
 * @param uid of the book whose cover is being searched for
 * @return the absolute path of the image copy
 */
private suspend fun validateAndSaveCover(context: Context, coverURL: String, uid: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val connection = URL(coverURL).openConnection() as HttpURLConnection
            connection.connect()
            val bitmap = BitmapFactory.decodeStream(connection.inputStream)

            if (bitmap != null && bitmap.width > 1 && bitmap.height > 1) {
                // valid image -> save locally
                return@withContext saveBitmapToFile(context, bitmap, uid)
            }
        } catch (e: Exception) {
            Timber.tag("BookCover").e("Failed cover validation from $coverURL with error $e")
        }
        return@withContext null
    }

/**
 * Saves an image given as a bitmap into a local jpg copy.
 *
 * @param context to access local files
 * @param bitmap array representing the cover image
 * @param uid of the book whose cover is being searched for
 * @return absolute path of the image copy
 */
private fun saveBitmapToFile(context: Context, bitmap: Bitmap, uid: String): String {
    val file = File(context.filesDir, "${uid}.jpg")
    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    }
    return file.absolutePath
}