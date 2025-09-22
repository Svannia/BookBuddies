package com.example.bookbuddies.data

import android.content.Context
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
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
    val boughtAt: String,
    val dateAdded: Long
)

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

enum class DateFormat {
    FULL_DATE, FULL_SHORT_DATE, MONTH_YEAR, NUMBERED, NUMBERED_REVERSE, NUMBERED_WITH_TIME
}

enum class BookSorting {
    AUTHOR_SERIES, SERIES, TITLE, RECENTLY_ADDED, RATING, GENRE, LANGUAGE, FORMAT, BOUGHT_AT
}

val sortingsMap = mapOf(
    BookSorting.AUTHOR_SERIES to R.string.sorting_author,
    BookSorting.SERIES to R.string.sorting_series,
    BookSorting.TITLE to R.string.sorting_title,
    BookSorting.RECENTLY_ADDED to R.string.sorting_added,
    BookSorting.RATING to R.string.sorting_rating,
    BookSorting.GENRE to R.string.sorting_genre,
    BookSorting.LANGUAGE to R.string.sorting_language,
    BookSorting.FORMAT to R.string.sorting_format,
    BookSorting.BOUGHT_AT to R.string.sorting_bought
)

fun BookSorting.getString(context: Context): String {
    return context.getString(sortingsMap[this] ?: R.string.sorting_author)
}

fun String.getBookSorting(context: Context): BookSorting {
    return sortingsMap.entries.firstOrNull {
        context.getString(it.value) == this
    }?.key ?: BookSorting.AUTHOR_SERIES
}