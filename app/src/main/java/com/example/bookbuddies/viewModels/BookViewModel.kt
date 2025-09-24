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

/**
 * ViewModel for viewing, sorting and updating books in the repository.
 *
 * @property repository current instance of the BookRepository
 */
class BookViewModel(private val repository: BookRepository) : ViewModel() {
    private val _bookSorting = MutableStateFlow(BookSorting.AUTHOR_SERIES)
    val sorting: StateFlow<BookSorting> = _bookSorting
    private val _onlyUnread = MutableStateFlow(false)
    val onlyUnread: StateFlow<Boolean> = _onlyUnread

    private val allBooks = repository.allBooks

    // the raw list of stored books is private. Instead we only expose the books sorted by one method ("author > series" by default)
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
            BookSorting.RATING -> books.sortedByDescending { it.rating }
            BookSorting.GENRE -> books.sortedBy { it.genre }
            BookSorting.LANGUAGE -> books.sortedBy { it.language }
            BookSorting.FORMAT -> books.sortedBy { it.format }
            BookSorting.BOUGHT_AT -> books.sortedBy { it.boughtAt }
        }
    }

    /**
     * Fetches a book given its unique ID.
     *
     * @param uid book ID
     * @return Book object for this ID. Can be null if no book was found with this ID
     */
    suspend fun getBookById(uid: String) = repository.getBookById(uid)

    /**
     * Inserts new books in the repository. If the book already exists, its data is overwritten with the new one.
     *
     * @param books list of Book objects to be added
     */
    suspend fun insertBooks(books: List<Book>) = repository.insertBooks(books)

    /**
     * Inserts a new book in the repository. If the book already exists, its data is overwritten with the new one.
     *
     * @param book Book object to be added
     */
    suspend fun insertBook(book: Book) = repository.insertBook(book)

    /**
     * Updates all the volumes of a manga with their series ID from Mangadex.
     *
     * @param mangaId Mangadex ID for this manga series
     * @param seriesName all volumes with this series name will update their mangaID
     */
    suspend fun updateMangaSeriesId(mangaId: String, seriesName: String) =
        repository.updateMangaSeriesId(mangaId, seriesName)

    /**
     * Removes the covers (replacing them with default placeholder) of some selected books.
     *
     * @param booksToClear list of Book objects whose covers need to be deleted
     * @param isError returns true if an error occurred while running the function
     * @param callBack block to run after the covers have been deleted
     * @return
     */
    suspend fun clearCovers(booksToClear: List<Book>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        var errorOccurred = false

        val clearedBooks = booksToClear.map { book ->
            book.cover?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Timber.tag("BookVM").e("Failed to delete cover for book ${book.title} with error: $e")
                    errorOccurred = true
                }
            }
            book.copy(cover = null)
        }
        repository.insertBooks(clearedBooks)
        if (errorOccurred) isError(true) else callBack()
    }

    /**
     * Removes the covers of all books present in the repository.
     *
     * @param isError returns true if an error occurred while running the function
     * @param callBack block to run after the covers have been deleted
     */
    suspend fun clearAllCovers(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val currentBooks = allBooks.first()
        clearCovers(currentBooks, { isError(it) }, { callBack() })
    }

    /**
     * Updates the "read" field of a Book object with a new value.
     *
     * @param isRead new value for the "read" field
     * @param book Book object to mark as (un)read
     */
    suspend fun updateRead(isRead: Boolean, book: Book) {
        val updatedBook = book.copy(read = isRead)
        repository.insertBook(updatedBook)
    }

    /**
     * Updates the sorting method, re-sorting the displayed books.
     *
     * @param newSorting new sorting method
     */
    fun setSorting(newSorting: BookSorting) {
        _bookSorting.value = newSorting
    }

    /**
     * Switches on/off the option to filter out the read books (only showing unread books).
     *
     */
    fun switchUnreadFilter() {
        _onlyUnread.value = !_onlyUnread.value
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
