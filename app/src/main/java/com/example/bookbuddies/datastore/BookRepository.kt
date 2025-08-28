package com.example.bookbuddies.datastore

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.errors.handleError
import com.opencsv.CSVReader
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Repository to manage data operations and provide a clean API for data access
class BookRepository(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun insertBooks(books: List<Book>) = bookDao.insertBooks(books)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
    suspend fun deleteAll() = bookDao.deleteAll()
}

suspend fun importBooksFromCsv(context: Context, file: File, repository: BookRepository) {
    val reader = CSVReader(FileReader(file))
    val allLines = reader.readAll()
    reader.close()

    if (allLines.isEmpty()) return

    // Read headers to map column names
    val headers = allLines.first().map { it.trim() }
    val columnIndex = headers.mapIndexed { index, name -> name to index }.toMap()

    // Fetch the cell value of a given row at a given column name
    fun getCol(cols: Array<String>, name: String): String {
        val idx = columnIndex[name] ?: return ""
        return cols.getOrNull(idx)?.trim() ?: ""
    }

    val books = mutableListOf<Book>()

    for (cols in allLines.drop(1)) {
        val publishedDate = parseDate(context, getCol(cols, "date_published")) ?: 0L
        val dateAdded = parseDate(context, getCol(cols, "date_added")) ?: System.currentTimeMillis()
    }
}

private fun parseDate(context: Context, dateStr: String): Long? {
    if (dateStr.isBlank()) return null

    val patterns = listOf(
        DateTimeFormatter.ISO_ZONED_DATE_TIME, // e.g., 2018-11-09T00:00:01Z
        DateTimeFormatter.ISO_OFFSET_DATE_TIME, // e.g., 2018-11-09T00:00:01+01:00
        DateTimeFormatter.ISO_LOCAL_DATE,       // e.g., 2016-02-25
        DateTimeFormatter.ofPattern("yyyy")     // e.g., 2011
    )

    for (fmt in patterns) {
        try {
            return when (fmt) {
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME -> {
                    val zoned = ZonedDateTime.parse(dateStr, fmt)
                    zoned.toInstant().toEpochMilli()
                }
                else -> {
                    val local = LocalDate.parse(dateStr, fmt)
                    local.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                }
            }
        } catch (e: DateTimeParseException) {
            handleError(context, "Failed to parse date: $dateStr", e)
            continue
        }
    }

    return null
}