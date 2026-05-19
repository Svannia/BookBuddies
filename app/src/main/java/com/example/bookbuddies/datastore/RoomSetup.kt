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
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.EventTag
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Convert more complex data types to and from a format that can be stored in the database
class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String =
        Json.encodeToString(list)

    @TypeConverter
    fun toStringList(string: String): List<String> =
        Json.decodeFromString(string)

    @TypeConverter
    fun fromLongList(list: List<Long>): String = Json.encodeToString(list)

    @TypeConverter
    fun toLongList(string: String): List<Long> = Json.decodeFromString(string)
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

// DAO for the CalendarEvent entity
@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY dateStart ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}

// DAO for the EventTag entity
@Dao
interface EventTagDao {
    @Query("SELECT * FROM event_tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<EventTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: EventTag)

    @Delete
    suspend fun deleteTag(tag: EventTag)
}

// Room database that holds all DAO entities
@Database(entities = [Book::class, CalendarEvent::class, EventTag::class], version = 7)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun eventTagDao(): EventTagDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build().also { db = it }
    }
}

