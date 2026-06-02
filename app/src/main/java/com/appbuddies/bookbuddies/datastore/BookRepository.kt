package com.appbuddies.bookbuddies.datastore

import android.content.Context
import com.appbuddies.bookbuddies.data.Book
import kotlinx.coroutines.flow.Flow

// Repository to manage data operations and provide a clean API for data access
class BookRepository(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getBookById(uid: String) = bookDao.getBookById(uid)
    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun insertBooks(books: List<Book>) = bookDao.insertBooks(books)

    /**
     * Updates all the volumes of a manga with their series ID from Mangadex.
     *
     * @param mangaId Mangadex ID for this manga series
     * @param seriesName all volumes with this series name will update their mangaID
     */
    suspend fun updateMangaSeriesId(mangaId: String, seriesName: String) = bookDao.updateMangaSeriesId(mangaId, seriesName)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
}
