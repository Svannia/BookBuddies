package com.example.bookbuddies.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uid: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val series: String,
    val seriesNumber: Int,
    val description: String,
    val genre: String,
    val publisher: String,
    val publishedDate: Long,
    val language: String,
    val format: String,
    val read: Boolean,
    val dateAdded: Long
)