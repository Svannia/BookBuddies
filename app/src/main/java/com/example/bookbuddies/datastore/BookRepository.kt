package com.example.bookbuddies.datastore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.errors.handleError
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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

// Repository to manage data operations and provide a clean API for data access
class BookRepository(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun insertBooks(books: List<Book>) = bookDao.insertBooks(books)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
    suspend fun deleteAll() = bookDao.deleteAll()
}

/**
 * Uses books ISBN to find and replace their covers.
 *
 * @param books list of books to check for a cover
 * @param insertBook a suspend lambda that receives a book and inserts it in the repository
 * @param callBack function to be called after the covers are updated, returning a list of books whose cover hasn't been found
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 */
suspend fun findCovers(
    context: Context,
    books: List<Book>,
    insertBook: suspend (Book) -> Unit,
    callBack: (List<String>) -> Unit,
    isError: (Boolean) -> Unit
) = coroutineScope {
    val semaphore = Semaphore(10) // max 10 parallel downloads to avoid DDOS the APIs that fetch book covers
    val failedBooks = mutableListOf<String>()
    val mutex = Mutex() // lock to protect access to failedBooks

    // parallelize the processes on all books over 10 threads
    try {
        val jobs = books.map { book ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        if (book.isbn.isNotBlank() && book.cover.isNullOrBlank()) {
                            val updatedBook = fetchBookWithCover(context, book)

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

private suspend fun fetchBookWithCover(context: Context, book: Book): Book =
    withContext(Dispatchers.IO) {
        val fileName = "${book.uid}.jpg"
        val file = File(context.filesDir, fileName)

        // if the cover is already stored locally -> use it
        if (file.exists()) {
            return@withContext book.copy(cover = file.absolutePath)
        }

        val urlsToTry = mutableListOf<String>()

        // 1. Try Google Books API first
        urlsToTry += "https://www.googleapis.com/books/v1/volumes?q=isbn:${book.isbn}"

        // 2. Open Library API as a fallback
        urlsToTry += "https://covers.openlibrary.org/b/isbn/${book.isbn}-L.jpg"

        var savedPath: String? = null

        for (url in urlsToTry) {
            try {
                if (url.contains("googleapis")) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "BookBuddiesApp/1.0")
                        .build()
                    val response = client.newCall(request).execute()

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
                        Timber.tag("BookCover").d("${book.title}: Google Books API returned ${response.code}")
                    }

                } else {
                    // Open Library direct image link
                    val localPath = validateAndSaveCover(context, url, book.uid)
                    if (localPath != null) {
                        savedPath = localPath
                        Timber.tag("BookCover").d("${book.title}: Fetched and validated cover from Open Library.")
                        break
                    }
                    else {
                        Timber.tag("BookCover").d("${book.title}: Open Library returned invalid placeholder.")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("BookCover").d("${book.title}: Failed to fetch cover from $url with error $e")
                continue
            }
        }

        // if nothing worked, fallback to default cover
        val finalCoverPath = savedPath
        return@withContext book.copy(cover = finalCoverPath)
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
suspend fun importBooksFromCsv(context: Context, file: File, insertBooks: suspend (List<Book>) -> Unit, callBack: () -> Unit, isError: (Boolean) -> Unit) {
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
            handleError(context, "Column name $name not found for book number ${cols[0]}")
            errorOccurred = true
            return ""
        }
        return cols.getOrNull(idx)?.trim() ?: ""
    }

    val books = mutableListOf<Book>()

    // Iterate over the column cells of each row (one row = one book)
    Timber.tag("BookImport").d("Found ${allLines.size - 1} books to import")
    for (cols in allLines.drop(1)) {
        val authors = getCol(cols, "author_details")
            .split("|").map { it.trim() }.filter { it.isNotEmpty() }

        val seriesDetails = getCol(cols, "series_details")
        // ^...$ -> string start and end, (.*?) -> series name (non-greedy), (?:...) -> non-capturing group, \s* -> optional whitespace, \( -> literal '(', (\d+) -> series number, \) -> literal ')'
        val seriesRegex = Regex("""^(.*?)(?:\s*\((\d+)\))?$""")
        val (seriesName, seriesNumber) = seriesRegex.find(seriesDetails)?.destructured?.let { (name, number) ->
            name.trim() to (number.toIntOrNull() ?: -1) // set series number to -1 if not available
        } ?: (seriesDetails.trim() to -1)

        val publishedDate = parseDate(getCol(cols, "date_published")) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L

        val readValue = getCol(cols, "read").trim()
        val read = (readValue == "1")

        val dateStarted = parseDate(getCol(cols, "date_published")) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L
        val dateFinished = parseDate(getCol(cols, "date_published")) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L
        val dateAdded = parseAddedDate(getCol(cols, "date_added")) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: System.currentTimeMillis()

        val book = Book(
            uid = getCol(cols, "book_uuid"),
            isbn = getCol(cols, "isbn"),
            title = getCol(cols, "title"),
            authors = authors,
            cover = null,
            seriesName = seriesName,
            seriesNumber = seriesNumber,
            description = getCol(cols, "description"),
            genre = getCol(cols, "genre"),
            publisher = getCol(cols, "publisher"),
            publishedDate = publishedDate,
            rating = getCol(cols, "rating").toDouble(),
            language = getCol(cols, "language"),
            format = getCol(cols, "format"),
            read = read,
            dateStarted = dateStarted,
            dateFinished = dateFinished,
            boughtAt = getCol(cols, "location"),
            dateAdded = dateAdded
        )
        books.add(book)
        Timber.tag("BookImport").d("Added book number ${cols[0]}")
    }

    insertBooks(books)
    callBack()
    if (!errorOccurred) isError(false)
}

/**
 * Parses a date string into a Long (milliseconds since epoch).
 * The date can be of format "yyyy-MM-dd", "yyyy-MM", "yyyy" or ISO 8601 (yyyy-MM-ddTHH:mm:ssZ).
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
 * The date is of format dd/mm/yyyy hh:mm.
 *
 * @param dateStr the date string to parse
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @return the parsed date in milliseconds since epoch, or null if parsing failed
 */
private fun parseAddedDate(dateStr: String, isError: (Boolean) -> Unit): Long? {
    if (dateStr.isBlank()) return null

    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())

    return try {
        val localDateTime = LocalDateTime.parse(dateStr, formatter)
        localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (e: DateTimeParseException) {
        Timber.tag("BookImport").d("Failed to parse added date: $dateStr with error $e")
        isError(true)
        null
    }
}