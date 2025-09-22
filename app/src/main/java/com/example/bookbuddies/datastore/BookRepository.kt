package com.example.bookbuddies.datastore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.displayDate
import com.example.bookbuddies.errors.handleError
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import java.io.FileReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.pow

// Repository to manage data operations and provide a clean API for data access
class BookRepository(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getBookById(uid: String) = bookDao.getBookById(uid)
    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun insertBooks(books: List<Book>) = bookDao.insertBooks(books)
    suspend fun updateMangaSeriesId(mangaId: String, seriesName: String) = bookDao.updateMangaSeriesId(mangaId, seriesName)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
    suspend fun deleteAll() = bookDao.deleteAll()
}

// CSV Headers
const val NUMBER_ID = "_id"
const val AUTHOR = "author_details"
const val TITLE = "title"
const val ISBN = "isbn"
const val PUBLISHER = "publisher"
const val DATE_PUBLISHED = "date_published"
const val RATING = "rating"
const val READ = "read"
const val SERIES = "series_details"
const val LOCATION = "location"
const val START = "read_start"
const val END = "read_end"
const val FORMAT = "format"
const val DESCRIPTION = "description"
const val GENRE = "genre"
const val LANGUAGE = "language"
const val DATE_ADDED = "date_added"
const val UUID = "book_uuid"


/**
 * Uses books ISBN to find and replace their covers.
 *
 * @param books list of books to check for a cover
 * @param insertBook a suspend lambda that receives a book and inserts it in the repository
 * @param callBack function to be called after the covers are updated, returning a list of books whose cover hasn't been found
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
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
    val mutex = Mutex() // lock to protect access to failedBooks
    val total = books.size
    var processed = 0

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
                            Timber.tag("BookCover").d("Failed to process book ${book.uid} with error $e")
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
            Timber.tag("BookCover").d("Failed to fetch covers with error $e")
            isError(true)
        }
    }
}

private suspend fun fetchCoverForBook(context: Context, book: Book): Book =
    withContext(Dispatchers.IO) {
        val fileName = "${book.uid}.jpg"
        val file = File(context.filesDir, fileName)

        // if the cover is already stored locally -> use it
        if (file.exists()) {
            return@withContext book.copy(cover = file.absolutePath)
        }

        var savedPath: String? = null

        // 1. Try Google Books API
        val googleURL = "https://www.googleapis.com/books/v1/volumes?q=isbn:${book.isbn}"
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
                    Timber.tag("BookCover").d("${book.title}: Google Books API failed and returned ${response.code}")
                }

            } catch (e: Exception) {
                Timber.tag("BookCover").d("${book.title}: Google Books attempt failed with $e")
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
                Timber.tag("BookCover").d("${book.title}: OpenLibrary attempt failed with $e")
            }
        }

        return@withContext book.copy(cover = savedPath)
    }

private suspend fun fetchCoverForManga(context: Context, book: Book, updateMangaSeriesId: suspend (String, String) -> Unit): Book =
    withContext(Dispatchers.IO) {
        val fileName = "${book.uid}.jpg"
        val file = File(context.filesDir, fileName)

        // if the cover is already stored locally -> use it
        if (file.exists()) {
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
                    Timber.tag("BookCover").d("${book.title}: MangaDex search API failed and returned ${response.code}")
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
                Timber.tag("BookCover").d("${book.title}: MangaDex covers fetch failed and returned ${coversResponse.code}")
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
            Timber.tag("BookCover").d("${book.title}: MangaDex search attempt failed with $e")
        }

        return@withContext book.copy(cover = savedPath)
    }

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
         Timber.tag("BookCover").d("Failed to download cover from $coverURL with error $e")
         null
     }
}

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
            Timber.tag("BookCover").d("Failed cover validation from $coverURL with error $e")
        }
        return@withContext null
}

private fun saveBitmapToFile(context: Context, bitmap: Bitmap, uid: String): String {
    val file = File(context.filesDir, "${uid}.jpg")
    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    }
    return file.absolutePath
}

/**
 * Imports books from a CSV file into the local database.
 *
 * @param context used to display a Toast in case of an error
 * @param file the CSV file to import
 * @param insertBooks a suspend lambda that receives the list of parsed books and inserts them in the repository
 * @param callBack function to be called after the import is complete
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 */
suspend fun importBooksFromCsv(
    file: File,
    insertBooks: suspend (List<Book>) -> Unit,
    getBookById: suspend (String) -> Book?,
    callBack: () -> Unit,
    isError: (Boolean) -> Unit
) {
    val reader = CSVReader(FileReader(file))
    val allLines = reader.readAll()
    reader.close()

    if (allLines.isEmpty()) {
        Timber.tag("BookImport").d("CSV file is empty")
        isError(true)
        return
    }
    var errorOccurred = false

    // Read headers to map column names
    val headers = allLines.first().map { it.trim() }
    val columnIndex = headers.mapIndexed { index, name -> name to index }.toMap()

    // Fetch the cell value of a given row at a given column name
    fun getCol(cols: Array<String>, name: String): String {
        val idx = columnIndex[name]
        if (idx == null) {
            Timber.tag("BookImport").d("Column name $name not found for book number ${cols[0]}")
            errorOccurred = true
            return ""
        }
        return cols.getOrNull(idx)?.trim() ?: ""
    }

    val books = mutableListOf<Book>()

    // Iterate over the column cells of each row (one row = one book)
    Timber.tag("BookImport").d("Found ${allLines.size - 1} books to import")

    for (cols in allLines.drop(1)) {
        val authors = getCol(cols, AUTHOR)
            .split("|").map { it.trim().trimEnd(',') }.filter { it.isNotEmpty() }

        val seriesDetails = getCol(cols, SERIES)
        // ^...$ -> string start and end, (.*?) -> series name (non-greedy), (?:...) -> non-capturing group, \s* -> optional whitespace, \( -> literal '(', (\d+) -> series number, \) -> literal ')'
        val seriesRegex = Regex("""^(.*?)(?:\s*\((\d+)\))?$""")
        val (seriesName, seriesNumber) = seriesRegex.find(seriesDetails)?.destructured?.let { (name, number) ->
            name.trim() to (number.toIntOrNull() ?: -1) // set series number to -1 if not available
        } ?: (seriesDetails.trim() to -1)

        val publishedDate = parseDate(getCol(cols, DATE_PUBLISHED)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L

        val readValue = getCol(cols, READ).trim()
        val read = (readValue == "1")

        val dateStarted = parseDate(getCol(cols, START)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L
        val dateFinished = parseDate(getCol(cols, END)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L
        val dateAdded = parseAddedDate(getCol(cols, DATE_ADDED)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: System.currentTimeMillis()

        val uid = getCol(cols, UUID)
        val existingBook = getBookById(uid)

        val book = Book(
            uid = uid,
            isbn = getCol(cols, ISBN),
            title = getCol(cols, TITLE),
            authors = authors,
            cover = existingBook?.cover,
            seriesName = seriesName,
            seriesNumber = seriesNumber,
            description = getCol(cols, DESCRIPTION),
            genre = getCol(cols, GENRE),
            publisher = getCol(cols, PUBLISHER),
            publishedDate = publishedDate,
            rating = getCol(cols, RATING).toDouble(),
            language = getCol(cols, LANGUAGE),
            format = getCol(cols, FORMAT),
            read = read,
            dateStarted = dateStarted,
            dateFinished = dateFinished,
            boughtAt = getCol(cols, LOCATION),
            dateAdded = dateAdded
        )
        books.add(book)
        Timber.tag("BookImport").d("Added book number ${cols[0]}")
    }

    insertBooks(books)
    callBack()
    if (!errorOccurred) isError(false)
}

fun exportBooksToCSV(books: List<Book>): ByteArray {
    // start writing CSV file
    val csvBuilder = StringBuilder()

    // add header row
    csvBuilder.append(
        "$NUMBER_ID," +
        "$AUTHOR," +
        "$TITLE," +
        "$ISBN," +
        "$PUBLISHER," +
        "$DATE_PUBLISHED," +
        "$RATING," +
        "$READ," +
        "$SERIES," +
        "$LOCATION," +
        "$START," +
        "$END," +
        "$FORMAT," +
        "$DESCRIPTION," +
        "$GENRE," +
        "$LANGUAGE," +
        "$DATE_ADDED," +
        UUID + "\n"
    )

    // add each book in RECENTLY_ADDED order
    val recentSortedBooks = books.sortedBy { it.dateAdded }
    for ((index, book) in recentSortedBooks.withIndex()) {
        csvBuilder.append("$index,")

        val authors = escapeCSVchar(book.authors.joinToString(" | "))
        csvBuilder.append("$authors,")

        csvBuilder.append("${escapeCSVchar(book.title)},")
        csvBuilder.append("${book.isbn},")
        csvBuilder.append("${escapeCSVchar(book.publisher)},")

        val datePublished = displayDate(book.publishedDate, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$datePublished,")

        csvBuilder.append("${book.rating},")

        val read = if (book.read) "1" else "0"
        csvBuilder.append("$read,")

        val series = if (book.seriesName.isBlank()) ""
            else escapeCSVchar(book.seriesName) +
                if (book.seriesNumber >= 0) " (${book.seriesNumber})" else ""
        csvBuilder.append("$series,")

        csvBuilder.append("${escapeCSVchar(book.boughtAt)},")

        val dateStart = displayDate(book.dateStarted, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$dateStart,")

        val dateEnd = displayDate(book.dateFinished, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$dateEnd,")

        csvBuilder.append("${escapeCSVchar(book.format)},")
        csvBuilder.append("${escapeCSVchar(book.description)},")
        csvBuilder.append("${escapeCSVchar(book.genre)},")
        csvBuilder.append("${escapeCSVchar(book.language)},")

        val dateAdded = displayDate(book.dateAdded, DateFormat.NUMBERED_WITH_TIME)
        csvBuilder.append("$dateAdded,")

        csvBuilder.append("${book.uid}\n")
    }

    val output = csvBuilder.toString().toByteArray()
    Timber.tag("BookExport").d("All books successfully written on export file.")

    return output
}

private fun escapeCSVchar(text: String): String {
    val needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n")
    val escaped = text.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}

/**
 * Parses a date string into a Long (milliseconds since epoch).
 * The date can be of format "dd/mm/yyyy", "yyyy/mm/dd", "yyyy-MM-dd", "yyyy-MM", "yyyy" or ISO 8601 (yyyy-MM-ddTHH:mm:ssZ).
 *
 * @param dateStr the date string to parse
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @return the parsed date in milliseconds since epoch, or null if parsing failed
 */
private fun parseDate(dateStr: String, isError: (Boolean) -> Unit): Long? {
    val str = dateStr.trim()
    if (dateStr.isBlank()) return null

    return try {
        when {
            str.matches(Regex("""\d{4}$""")) -> { // yyyy
                val year = Year.parse(str)
                year.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}$""")) -> { // yyyy-MM
                val ym = YearMonth.parse(str)
                ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> { // yyyy-MM-dd or longer
                try {
                    val local = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
                    local.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                } catch (_: DateTimeParseException) {
                    // fallback for datetime strings with time
                    val ldt = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
                    ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
            }
            str.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> { // dd/mm/yyyy
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}/\d{2}/\d{2}""")) -> { // yyyy/mm/dd
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""[A-Za-z]+ \d{1,2}, \d{4}""")) -> { // MMMM d, yyyy
                val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            else -> {
                Timber.tag("BookImport").d("Unknown date format: $str")
                isError(true)
                null
            }
        }
    } catch (e: DateTimeParseException) {
        Timber.tag("BookImport").d("Failed to parse date: $str with error $e")
        isError(true)
        null
    }
}

/**
 * Parses a date string into a Long (milliseconds since epoch).
 * The date is of format dd/mm/yyyy hh:mm or dd-MM-yyyy HH:mm.
 *
 * @param dateStr the date string to parse
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @return the parsed date in milliseconds since epoch, or null if parsing failed
 */
private fun parseAddedDate(dateStr: String, isError: (Boolean) -> Unit): Long? {
    val str = dateStr.trim()
    if (str.isBlank()) return null

    return try {
        when {
            str.matches(Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}(:\d{2})?""")) -> { // dd/MM/yyyy HH:mm
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}/\d{2}/\d{2} \d{2}:\d{2}(:\d{2})?""")) -> { // yyyy/MM/dd HH:mm
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{2}-\d{2}-\d{4} \d{2}:\d{2}(:\d{2})?""")) -> { // dd-MM-yyyy HH:mm
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}(:\d{2})?""")) -> { // yyyy-MM-dd HH:mm
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            else -> {
                Timber.tag("BookImport").d("Unknown added date format: $str")
                isError(true)
                null
            }
        }
    } catch (e: DateTimeParseException) {
        Timber.tag("BookImport").d("Failed to parse added date: $str with error $e")
        isError(true)
        null
    }
}