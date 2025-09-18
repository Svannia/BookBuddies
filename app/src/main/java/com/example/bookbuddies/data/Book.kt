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