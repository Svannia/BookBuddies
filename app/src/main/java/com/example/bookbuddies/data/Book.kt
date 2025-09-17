package com.example.bookbuddies.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    AUTHOR_SERIES, SERIES, TITLE, RECENTLY_ADDED, GENRE, RATING
}