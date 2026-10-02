package com.appbuddies.bookbuddies.helpers

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.Book
import com.appbuddies.bookbuddies.data.BookSorting
import com.appbuddies.bookbuddies.data.DateFormat
import com.appbuddies.bookbuddies.ui.theme.MediumGrey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Displays a book's authors' names as <First Name> <Last Name>, instead of how it's stored as <Last Name>, <First Name>.
 * All the authors are displayed comma-separated in case the authors list is longer than 1.
 *
 * @param authorsList each item is an author stored as <Last Name>, <First Name>
 * @return single string with the author(s), or empty String if there are no authors
 */
fun displayAuthors(authorsList: List<String>): String {
    if (authorsList.isEmpty()) return ""

    return authorsList.joinToString(", ") { author ->
        val parts = author.split(",")
        if (parts.size == 2) {
            val surname = parts[0].trim()
            val names = parts[1].trim()
            "$names $surname"
        } else {
            author
        }
    }
}

/**
 * Displays a single author's name as <First Name> <Last Name>, instead of how it's stored as <Last Name>, <First Name>.
 *
 * @param author string like <Last Name>, <First Name>
 * @return single string with the author, or empty String if there are no authors
 */
fun displayAuthor(author: String): String {
    val parts = author.split(",").map { it.trim() }
    return if (parts.size == 2) "${parts[1]} ${parts[0]}" else author
}

/**
 * Given an author written as <First Name> <Last Name>, returns it as <Last Name>, <First Name> for storage.
 *
 * @param author string like <First Name> <Last Name>
 * @return single string with the author
 */
fun storeAuthor(author: String): String {
    val parts = author.split(" ").map { it.trim() }
    return if (parts.size >= 2) {
        val surname = parts.last()
        val names = parts.dropLast(1).joinToString(" ")
        "$surname, $names"
    } else {
        author
    }
}

/**
 * Displays a series info as <Series Name> #<Series Number>.
 *
 * @param seriesName name of the series
 * @param seriesNumber number in the series. If negative, it is not displayed.
 * @return single string with all series info
 */
fun displaySeries(seriesName: String, seriesNumber: Int): String {
    val number = if (seriesNumber >= 0) "#$seriesNumber" else ""
    return "$seriesName $number"
}

/**
 * Translates a date stored as an Epoch number into a human-readable string, depending on the desired date format.
 *
 * @param date represents a date in milliseconds since the Unix epoch
 * @param format DateFormat entry for different types of date parsing:
 * SHORT_DAY_DATE : Tue 24 Jul (if the year is the current year, otherwise Tue 24 Jul 2001),
 * DAY_MONTH : 24 Jul,
 * FULL_DATE : 24 July 2001,
 * FULL_SHORT_DATE : 24 Jul 2001,
 * MONTH_YEAR : July 2001,
 * NUMBERED : 24/07/2001,
 * NUMBERED_REVERSE : 2001/07/24,
 * NUMBERED_WITH_TIME : 24/07/2001 19:50
 * @return date as text, or empty String if input date is 0L
 */
fun displayDate(date: Long, format: DateFormat): String {
    if (date <= 0L) return ""
    val calendar = Calendar.getInstance().apply { timeInMillis = date }
    val locale = Locale.getDefault()

    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val longMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
    val shortMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
    val year = calendar.get(Calendar.YEAR)

    return when (format) {
        DateFormat.SHORT_DAY_DATE -> {
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
            val result = "$dayOfWeek $day $shortMonth"
            if (year != Calendar.getInstance().get(Calendar.YEAR)) "$result $year" else result
        }
        DateFormat.DAY_MONTH -> {
            "$day $shortMonth"
        }
        DateFormat.FULL_DATE -> {
            "$day $longMonth $year"
        }
        DateFormat.FULL_SHORT_DATE -> {
            "$day $shortMonth $year"
        }
        DateFormat.MONTH_YEAR -> {
            "$longMonth $year"
        }
        DateFormat.NUMBERED -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            "$numberDay/$numberMonth/$year"
        }
        DateFormat.NUMBERED_REVERSE -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            "$year/$numberMonth/$numberDay"
        }
        DateFormat.NUMBERED_WITH_TIME -> {
            val numberDay = day.toString().padStart(2, '0')
            val numberMonth = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
            "$numberDay/$numberMonth/$year $hour:$minute"
        }
    }
}

/**
 * Extracts the dominant colour on a book's cover.
 *
 * @param book book to recover the dominant colour from
 * @return most dominant colour
 */
fun getCoverColour(book: Book): Color {
    if (book.coverColours.isEmpty()) return MediumGrey
    val index = book.chosenCoverColour.coerceIn(0, book.coverColours.size - 1)
    return Color(book.coverColours[index])
}

/**
 * When changing the sorting method, sorts books and groups them based on titles that make sense with the sorting method. Rewrites group headers.
 *
 * @param context to access string resources
 * @param unreadFilter whether or not to filter out books that have been read (only showing unread books)
 * @param sorting current sorting method
 * @param books current list of all Book objects
 * @return Map that maps group headers to their sorted list of books
 */
fun groupBooks(context: Context, unreadFilter: Boolean, sorting: BookSorting, books: List<Book>): Map<String, List<Book>> {
    val filteredBooks = if (unreadFilter) books.filter { !it.read } else books

    return when (sorting) {
        BookSorting.AUTHOR_SERIES -> {
            // Not handled here, use dedicated groupBooksSubheaders()
            emptyMap()
        }
        BookSorting.SERIES -> filteredBooks.groupBy { book ->
            book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
        }
        BookSorting.TITLE -> books.groupBy { book ->
            book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }
        BookSorting.RECENTLY_ADDED -> filteredBooks.groupBy { book ->
            displayDate(book.dateAdded, DateFormat.MONTH_YEAR)
        }
        BookSorting.RATING -> filteredBooks.groupBy { book ->
            val rounded = book.rating.toInt().coerceIn(0, 5)
            "$rounded ☁E"
        }
        BookSorting.GENRE -> filteredBooks.groupBy { book ->
            book.genre.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.LANGUAGE -> filteredBooks.groupBy { book ->
            book.language.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.FORMAT -> filteredBooks.groupBy { book ->
            book.format.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.SOURCE -> filteredBooks.groupBy { book ->
            book.source.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
    }
}

/**
 * Specifically handles sorting and re-grouping of books for the Author>Series sorting method, since it also requires subheaders.
 *
 * @param context to access string resources
 * @param unreadFilter whether or not to filter out books that have been read (only showing unread books)
 * @param sorting current sorting method
 * @param books current list of all Book objects
 * @return Map that maps group headers to a mapping of group subheaders to their sorted list of books
 */
fun groupBooksSubheaders(context: Context, unreadFilter: Boolean, sorting: BookSorting, books: List<Book>): Map<String, Map<String, List<Book>>> {
    if (sorting == BookSorting.AUTHOR_SERIES) {
        val filteredBooks = if (unreadFilter) books.filter { !it.read } else books

        return filteredBooks.groupBy { book ->
            book.authors.firstOrNull()?.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownAuthor)
        }.mapValues { entry ->
            entry.value.groupBy { book ->
                book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
            }
        }
    }
    return emptyMap()
}

/**
 * Copies some text as an element on the user's phone clipboard.
 *
 * @param context to access string resources
 * @param text to be copied in the clipboard
 * @param clipboard user's Clipboard
 * @param coroutineScope to launch the suspend copy operation
 */
fun copyToClipboard(context: Context, text: String, clipboard: Clipboard, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        val clipData = ClipData.newPlainText(context.getString(R.string.txt_failedCoversClipboard), text)
        val clipEntry: ClipEntry = clipData.toClipEntry()
        clipboard.setClipEntry(clipEntry)
    }
}
