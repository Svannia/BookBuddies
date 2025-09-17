package com.example.bookbuddies.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookSorting
import com.example.bookbuddies.datastore.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File

class BookViewModel(private val repository: BookRepository) : ViewModel() {
    private val _bookSorting = MutableStateFlow(BookSorting.AUTHOR_SERIES)
    val sorting: StateFlow<BookSorting> = _bookSorting

    private val allBooks = repository.allBooks
    val sortedBooks: Flow<List<Book>> = combine(allBooks, _bookSorting) { books, sorting ->
        when (sorting) {
            BookSorting.AUTHOR_SERIES -> books.sortedWith(
                compareBy<Book> { it.authors.firstOrNull() ?: "" }
                    .thenBy { it.seriesName }
                    .thenBy { it.seriesNumber }
            )
            BookSorting.SERIES -> books.sortedWith(
                compareBy<Book> { it.seriesName }
                    .thenBy { it.seriesNumber }
            )
            BookSorting.TITLE -> books.sortedBy { it.title }
            BookSorting.RECENTLY_ADDED -> books.sortedByDescending { it.dateAdded }
            BookSorting.GENRE -> books.sortedBy { it.genre }
            BookSorting.RATING -> books.sortedByDescending { it.rating }
        }
    }

    suspend fun getBookById(uid: String) = repository.getBookById(uid)

    suspend fun insertBooks(books: List<Book>) = repository.insertBooks(books)

    suspend fun insertBook(book: Book) = repository.insertBook(book)

    suspend fun updateMangaSeriesId(seriesName: String, mangaId: String) =
        repository.updateMangaSeriesId(seriesName, mangaId)

    suspend fun clearAllCovers(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val currentBooks = allBooks.first()
        var errorOccurred = false

        val clearedBooks = currentBooks.map { book ->
            book.cover?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Timber.tag("BookVM").d("Failed to delete cover for book ${book.title} with error: $e")
                    errorOccurred = true
                }
            }
            book.copy(cover = null)
        }
        repository.insertBooks(clearedBooks)
        if (errorOccurred) isError(true) else callBack()
    }

    fun setSorting(newSorting: BookSorting) {
        _bookSorting.value = newSorting
    }
}

class BookViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
