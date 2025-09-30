package com.example.bookbuddies.data

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bookbuddies.R
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uid: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val cover: String?,
    val seriesName: String,
    val seriesNumber: Int,
    val mangaSeriesId: String ?= null,
    val description: String,
    val genre: String,
    val publisher: String,
    val publishedDate: Long,
    val rating: Double,
    val language: String,
    val format: String,
    val read: Boolean,
    val dateStarted: Long,
    val dateFinished: Long,
    val bookshelf: String,
    val source: String,
    val isGift: Boolean,
    val dateAdded: Long
) {
    companion object {
        /**
         * Creates an empty Book data object.
         *
         * @return empty Book data object.
         */
        fun empty(): Book {
            return Book("", "", "", emptyList(), null,
                "", -1, null, "",
                "", "", 0L, 0.0, "", "",
                false, 0L, 0L, "", "", false,  System.currentTimeMillis()
            )
        }
    }
}

/**
 * Displays an author's name as <First Name> <Last Name>, instead of how it's stored as <Last Name>, <First Name>.
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

fun displaySeries(seriesName: String, seriesNumber: Int): String {
    val number = if (seriesNumber >= 0) "#$seriesNumber" else ""
    return "$seriesName $number"
}

/**
 * Translates a date stored as an Epoch number into a human-readable string, depending on the desired date format.
 *
 * @param date represents a date in milliseconds since the Unix epoch
 * @param format DateFormat entry for different types of date parsing:
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

// Various date formats for parsing
enum class DateFormat {
    FULL_DATE, FULL_SHORT_DATE, MONTH_YEAR, NUMBERED, NUMBERED_REVERSE, NUMBERED_WITH_TIME
}

// The different possible methods to sort and group books in the main page
enum class BookSorting {
    AUTHOR_SERIES, SERIES, TITLE, RECENTLY_ADDED, RATING, GENRE, LANGUAGE, FORMAT, SOURCE
}

val sortingMap = mapOf(
    BookSorting.AUTHOR_SERIES to R.string.sorting_author,
    BookSorting.SERIES to R.string.sorting_series,
    BookSorting.TITLE to R.string.sorting_title,
    BookSorting.RECENTLY_ADDED to R.string.sorting_added,
    BookSorting.RATING to R.string.sorting_rating,
    BookSorting.GENRE to R.string.sorting_genre,
    BookSorting.LANGUAGE to R.string.sorting_language,
    BookSorting.FORMAT to R.string.sorting_format,
    BookSorting.SOURCE to R.string.sorting_source
)

/**
 * Translates a BookSorting element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun BookSorting.getString(context: Context): String {
    return context.getString(sortingMap[this] ?: R.string.sorting_author)
}

/**
 * Translates a string stored in strings.xml into its corresponding BookSorting element.
 *
 * @param context used to access the string resources
 * @return entry from BookSorting enum
 */
fun String.getBookSorting(context: Context): BookSorting {
    return sortingMap.entries.firstOrNull {
        context.getString(it.value) == this
    }?.key ?: BookSorting.AUTHOR_SERIES
}