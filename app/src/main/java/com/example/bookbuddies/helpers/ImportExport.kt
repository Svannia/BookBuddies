package com.example.bookbuddies.helpers

import com.example.bookbuddies.data.DateFormat
import com.opencsv.CSVReader
import timber.log.Timber
import java.io.File
import java.io.FileReader

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
const val BOOKSHELF = "bookshelf"
const val LOCATION = "location"
const val SOURCE = "source"
const val IS_GIFT = "isGift"
const val START = "read_start"
const val END = "read_end"
const val FORMAT = "format"
const val DESCRIPTION = "description"
const val GENRE = "genre"
const val LANGUAGE = "language"
const val DATE_ADDED = "date_added"
const val UUID = "book_uuid"

/**
 * Imports books from a CSV file into the local database.
 *
 * @param file the CSV file to import
 * @param insertBooks a suspend lambda that receives the list of parsed books and inserts them in the repository
 * @param getBookById a suspend lambda that received a book's ID an fetches its Book object from the repository
 * @param callBack function to be called after the import is complete
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 */
suspend fun importBooksFromCsv(
    file: File,
    insertBooks: suspend (List<com.example.bookbuddies.data.Book>) -> Unit,
    getBookById: suspend (String) -> com.example.bookbuddies.data.Book?,
    callBack: () -> Unit,
    isError: (Boolean) -> Unit
) {
    // Opens and starts reading the CSV file
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

    // Fetches the cell value of a given row at a given column name
    fun getCol(cols: Array<String>, name: String): String {
        val idx = columnIndex[name]
        if (idx == null) {
            if (name != SOURCE && name != IS_GIFT && name != LOCATION) {
                Timber.tag("BookImport").e("Column name $name not found for book number ${cols[0]}")
                errorOccurred = true
            }
            return ""
        }
        return cols.getOrNull(idx)?.trim() ?: ""
    }

    val books = mutableListOf<com.example.bookbuddies.data.Book>()

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

        var description = getCol(cols, DESCRIPTION).replace("\\n", "\n").replace("\"\"", "\"").replace("<br />", "\n")
        description = description.replace(Regex("\\n+"), "\n").trim()

        val publishedDate = parseDate(getCol(cols, DATE_PUBLISHED)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L

        val dateStarted = parseDate(getCol(cols, START)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: 0L
        var dateFinished = if (dateStarted <= 0L) 0L else {
            parseDate(getCol(cols, END)) { isError ->
                isError(isError)
                errorOccurred = true
            } ?: 0L
        }
        // ensure that dateFinished is not smaller than dateStarted
        if (dateStarted > dateFinished) {
            Timber.tag("BookImport").d("Book number ${cols[0]}: incoherent dateFinished -> changing to 0")
            dateFinished = 0L
        }

        val readValue = getCol(cols, READ).trim()
        val read = if (dateFinished > 0L) true else (readValue == "1")

        val source = getCol(cols, LOCATION).ifBlank { getCol(cols, SOURCE) }
        val isGiftInt = getCol(cols, IS_GIFT)
        val isGift = if (isGiftInt.isBlank()) false else (isGiftInt == "1")

        val dateAdded = parseAddedDate(getCol(cols, DATE_ADDED)) { isError ->
            isError(isError)
            errorOccurred = true
        } ?: System.currentTimeMillis()

        val uid = getCol(cols, UUID)

        // since covers is the only element not present in CSV files -> avoid erasing them
        // if a book already exists, all its data except for an existing cover are overwritten with CSV file data.
        val existingBook = getBookById(uid)

        val book = _root_ide_package_.com.example.bookbuddies.data.Book(
            uid = uid,
            isbn = getCol(cols, ISBN),
            title = getCol(cols, TITLE),
            authors = authors,
            cover = existingBook?.cover,
            coverColours = existingBook?.coverColours ?: emptyList(),
            chosenCoverColour = existingBook?.chosenCoverColour ?: 0,
            seriesName = seriesName,
            seriesNumber = seriesNumber,
            description = description,
            genre = getCol(cols, GENRE),
            publisher = getCol(cols, PUBLISHER),
            publishedDate = publishedDate,
            rating = getCol(cols, RATING).toDouble(),
            language = getCol(cols, LANGUAGE),
            format = getCol(cols, FORMAT),
            read = read,
            dateStarted = dateStarted,
            dateFinished = dateFinished,
            bookshelf = getCol(cols, BOOKSHELF),
            source = source,
            isGift = isGift,
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
 * Exports all books present in the app's repository into a CSV file. All book data except covers is exported.
 *
 * @param books list of all books from repository
 * @return CSV file built as an array of bytes
 */
fun exportBooksToCSV(books: List<com.example.bookbuddies.data.Book>): ByteArray {
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
                "$SOURCE," +
                "$IS_GIFT," +
                "$START," +
                "$END," +
                "$BOOKSHELF," +
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

        val authors = escapeCSVChar(book.authors.joinToString(" | "))
        csvBuilder.append("$authors,")

        csvBuilder.append("${escapeCSVChar(book.title)},")
        csvBuilder.append("${book.isbn},")
        csvBuilder.append("${escapeCSVChar(book.publisher)},")

        val datePublished = displayDate(book.publishedDate, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$datePublished,")

        csvBuilder.append("${book.rating},")

        val read = if (book.read) "1" else "0"
        csvBuilder.append("$read,")

        val series = if (book.seriesName.isBlank()) ""
        else escapeCSVChar(book.seriesName) +
                if (book.seriesNumber >= 0) " (${book.seriesNumber})" else ""
        csvBuilder.append("$series,")

        csvBuilder.append("${escapeCSVChar(book.source)},")
        val isGift = if (book.isGift) "1" else "0"
        csvBuilder.append("$isGift,")

        val dateStart = displayDate(book.dateStarted, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$dateStart,")

        val dateEnd = displayDate(book.dateFinished, DateFormat.NUMBERED_REVERSE)
        csvBuilder.append("$dateEnd,")

        csvBuilder.append("${escapeCSVChar(book.bookshelf)},")
        csvBuilder.append("${escapeCSVChar(book.format)},")
        csvBuilder.append("${escapeCSVChar(book.description)},")
        csvBuilder.append("${escapeCSVChar(book.genre)},")
        csvBuilder.append("${escapeCSVChar(book.language)},")

        val dateAdded = displayDate(book.dateAdded, DateFormat.NUMBERED_WITH_TIME)
        csvBuilder.append("$dateAdded,")

        csvBuilder.append("${book.uid}\n")
    }

    val output = csvBuilder.toString().toByteArray()
    Timber.tag("BookExport").d("All books successfully written on export file.")

    return output
}

/**
 * Modifies a text that is supposed to fit in a single CSV file cell, to sanitize special characters (, \ \n ")
 *
 * @param text string to be sanitized
 * @return sanitized string
 */
private fun escapeCSVChar(text: String): String {
    val needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\\n")
    val escaped = text.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}