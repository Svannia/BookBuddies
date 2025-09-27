package com.example.bookbuddies.viewModels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookSorting
import com.example.bookbuddies.datastore.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    // ---------- FETCHING DATA ----------
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
    suspend fun getBookById(uid: String): Book? {
        Timber.tag("BookVM").d("Recovering book with ID $uid")
        return repository.getBookById(uid)
    }

    fun getBookFlowById(uid: String): Flow<Book> {
        Timber.tag("BookVM").d("Recovering book flow with ID $uid")
        return allBooks.map { list -> list.find { it.uid == uid }}.filterNotNull()
    }


    // ---------- ADDING NEW ENTRIES ----------

    /**
     * Inserts new books in the repository. If the book already exists, its data is overwritten with the new one.
     *
     * @param books list of Book objects to be added
     */
    suspend fun insertBooks(books: List<Book>) {
        repository.insertBooks(books)
        Timber.tag("BookVM").d("Inserting ${books.size} into repository")
    }

    /**
     * Inserts a new book in the repository. If the book already exists, its data is overwritten with the new one.
     *
     * @param book Book object to be added
     */
    suspend fun insertBook(book: Book) {
        repository.insertBook(book)
        Timber.tag("BookVM").d("Inserting book \"${book.title}\" into repository")
    }


    // ---------- UPDATING REPOSITORY ----------

    /**
     * Updates an existing book's cover with an image copied from the user's gallery.
     *
     * @param context for accessing local files
     * @param image Uri of the user's picture
     * @param book that needs to be updated
     * @param isError returns true if there was an error executing the function
     * @param callBack block that runs after the cover was successfully updated
     */
    suspend fun updateCoverFromGallery(context: Context, image: Uri, book: Book, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val newCover = withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "${book.uid}.jpg")
                context.contentResolver.openInputStream(image)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext null
                file.absolutePath
            } catch (e: Exception) {
                Timber.tag("BookVM").e("Failed to save cover from gallery Uri $image with error: $e")
                isError(true)
                null
            }
        }
        if (newCover == null) {
            Timber.tag("BookVM").e("Failed to save cover from gallery Uri $image: returned a null file.")
            isError(true)
        } else {
            val updatedBook = book.copy(cover = newCover)
            repository.insertBook(updatedBook)
            Timber.tag("BookVM").d("Update book \"${book.title}\" with new cover.")
            callBack()
        }
    }

    /**
     * Updates all the volumes of a manga with their series ID from Mangadex.
     *
     * @param mangaId Mangadex ID for this manga series
     * @param seriesName all volumes with this series name will update their mangaID
     */
    suspend fun updateMangaSeriesId(mangaId: String, seriesName: String) {
        repository.updateMangaSeriesId(mangaId, seriesName)
        Timber.tag("BookVM").d("Update manga series \"$seriesName\" with new Mangadex ID $mangaId")
    }

    /**
     * Updates the "rating" field of a Book object with a new value.
     *
     * @param newRating new value for the "rating" field
     * @param book Book object whose value to change
     */
    suspend fun updateRating(newRating: Double, book: Book) {
        val updatedBook = book.copy(rating = newRating)
        repository.insertBook(updatedBook)
        Timber.tag("BookVM").d("Updated the \"rating\" field for book \"${book.title}\"")
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
        Timber.tag("BookVM").d("Updated the \"read\" mark for book \"${book.title}\"")
    }

    /**
     * Updates the "dateStarted" field of a Book object with a new value.
     *
     * @param dateStarted new value for the "dateStarted" field
     * @param book Book object to mark as started
     */
    suspend fun updateStart(dateStarted: Long, book: Book) {
        val updatedBook = book.copy(dateStarted = dateStarted)
        repository.insertBook(updatedBook)
        Timber.tag("BookVM").d("Updated the \"dateStarted\" for book \"${book.title}\"")
    }

    /**
     * Updates the "dateFinished" field of a Book object with a new value.
     *
     * @param dateFinished new value for the "dateFinished" field
     * @param book Book object to mark as finished
     */
    suspend fun updateFinish(dateFinished: Long, book: Book) {
        val updatedBook = book.copy(dateFinished = dateFinished)
        repository.insertBook(updatedBook)
        Timber.tag("BookVM").d("Updated the \"dateFinished\" for book \"${book.title}\"")
    }



    // ---------- UPDATING COVERS ----------

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
        if (errorOccurred) isError(true) else {
            Timber.tag("BookVM").d("Successfully deleted covers for ${booksToClear.size} books")
            callBack()
        }
    }

    /**
     * Removes the covers of all books present in the repository.
     *
     * @param isError returns true if an error occurred while running the function
     * @param callBack block to run after the covers have been deleted
     */
    suspend fun clearAllCovers(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val currentBooks = allBooks.first()
        var errorOccurred = false
        clearCovers(currentBooks, { if (it) errorOccurred = true })
        {
            if (errorOccurred) isError(true)
            else callBack()
        }
    }


    // ---------- DELETING FROM REPOSITORY ----------

    /**
     * Deletes book entry from the repository, along with its local copy of the book cover.
     *
     * @param bookToDelete Book object of the book to delete
     * @param isError returns true if an error occurred while executing the function
     * @param callBack block that runs once the book and its cover were successfully deleted
     */
    suspend fun deleteBook(bookToDelete: Book, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // first try to delete book cover in local files
        bookToDelete.cover?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Timber.tag("BookVM").e("Failed to delete cover when trying to delete book ${bookToDelete.title} with error: $e")
                isError(true)
            }
        }
        repository.deleteBook(bookToDelete)
        Timber.tag("BookVM").d("Deleted book \"${bookToDelete.title} from the repository")
        callBack()
    }

    /**
     * Deletes multiple book entries from the repository, along with its local copy of the book cover.
     *
     * @param booksToDelete list of Book objects to be deleted
     * @param isError returns true if an error occurred while executing the function
     * @param callBack block that runs once the book and its cover were successfully deleted
     */
    suspend fun deleteBooks(booksToDelete: List<Book>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        var errorOccurred = false

        booksToDelete.forEach { book ->
            deleteBook(book, { if (it) errorOccurred = true })
            {
                if (errorOccurred) isError(true)
                else callBack()
            }
        }
    }


    // ---------- SORTING METHODS AND FILTER ----------

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
