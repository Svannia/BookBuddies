package com.example.bookbuddies.datastore

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.bookbuddies.data.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Convert more complex data types to and from a format that can be stored in the database
class Converters {
    @TypeConverter
    fun fromAuthorsList(authors: List<String>): String =
        Json.encodeToString(authors)

    @TypeConverter
    fun toAuthorsList(authorsString: String): List<String> =
        Json.decodeFromString(authorsString)
}

// Data Access Object (DAO) for the Book entity
@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE uid = :uid LIMIT 1")
    suspend fun getBookById(uid: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<Book>)

    @Query("UPDATE books SET mangaSeriesId = :mangaId WHERE seriesName = :seriesName")
    suspend fun updateMangaSeriesId(mangaId: String, seriesName: String)

    @Delete
    suspend fun deleteBook(book: Book)

}

// Room database that holds the Book entity and provides the BookDao
@Database(entities = [Book::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}

// Singleton object to provide the database instance
object DatabaseProvider {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return db ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bookbuddies.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build().also { db = it }
    }
}

