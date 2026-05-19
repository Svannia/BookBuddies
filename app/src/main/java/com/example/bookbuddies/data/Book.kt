package com.example.bookbuddies.data

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bookbuddies.R

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uid: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val cover: String?,
    val coverColours: List<Long> = emptyList(),
    val chosenCoverColour: Int = 0,
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
            return Book("", "", "", emptyList(), null, emptyList(), 0,
                "", -1, null, "",
                "", "", 0L, 0.0, "", "",
                false, 0L, 0L, "", "", false,  System.currentTimeMillis()
            )
        }
    }
}

// Various date formats for parsing
enum class DateFormat {
    FULL_DATE, FULL_SHORT_DATE, MONTH_YEAR, NUMBERED, NUMBERED_REVERSE, NUMBERED_WITH_TIME, SHORT_DAY_DATE
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