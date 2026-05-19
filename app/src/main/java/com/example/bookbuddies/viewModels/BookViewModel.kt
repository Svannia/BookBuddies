package com.example.bookbuddies.viewModels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookSorting
import com.example.bookbuddies.datastore.BookRepository
import com.example.bookbuddies.helpers.displayAuthor
import com.example.bookbuddies.helpers.extractColours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    // to save fast-scroll position across screen recompositions
    var savedScrollIndex: Int = 0
    var savedScrollOffset: Int = 0

    private val allBooks = repository.allBooks

    // ---------- FOR AUTO-COMPLETE ----------
    private val _allAuthors = MutableStateFlow<List<String>>(emptyList())
    val allAuthors: StateFlow<List<String>> = _allAuthors
    private val _allSeries = MutableStateFlow<List<String>>(emptyList())
    val allSeries: StateFlow<List<String>> = _allSeries
    private val _allGenres = MutableStateFlow<List<String>>(emptyList())
    val allGenres: StateFlow<List<String>> = _allGenres
    private val _allPublishers = MutableStateFlow<List<String>>(emptyList())
    val allPublishers: StateFlow<List<String>> = _allPublishers
    private val _allLanguages = MutableStateFlow<List<String>>(emptyList())
    val allLanguages: StateFlow<List<String>> = _allLanguages
    private val _allFormats = MutableStateFlow<List<String>>(emptyList())
    val allFormats: StateFlow<List<String>> = _allFormats
    private val _allBookshelves = MutableStateFlow<List<String>>(emptyList())
    val allBookshelves: StateFlow<List<String>> = _allBookshelves
    private val _allBought = MutableStateFlow<List<String>>(emptyList())
    val allBought: StateFlow<List<String>> = _allBought
    private val _allGivers = MutableStateFlow<List<String>>(emptyList())
    val allGivers: StateFlow<List<String>> = _allGivers

    init {
        viewModelScope.launch {
            allBooks.collect { books ->
                _allAuthors.value =
                    books.flatMap { it.authors.map { author -> displayAuthor(author) } }.distinct()
                _allSeries.value = books.map { it.seriesName }.distinct()
                _allGenres.value = books.map { it.genre }.distinct()
                _allPublishers.value = books.map { it.publisher }.distinct()
                _allLanguages.value = books.map { it.language }.distinct()
                _allFormats.value = books.map { it.format }.distinct()
                _allBookshelves.value = books.map { it.bookshelf }.distinct()
                _allBought.value = books.filter { !it.isGift }.map { it.source }.distinct()
                _allGivers.value = books.filter { it.isGift }.map { it.source }.distinct()
            }
        }
        viewModelScope.launch {
            backfillCoverColours()
        }
    }

    // ---------- FILTERING OUT AUTO-COMPLETION RESULTS ----------
    private fun filterList(input: String, list: List<String>, transform: (String) -> String = { it }): List<String> {
        val query = input.lowercase()
        return list
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { item ->
                item.lowercase().split(" ", ",", "-", "_").any { part -> part.startsWith(query)}
            }
            .map(transform)
            .toList()
    }
    fun filterAuthors(input: String, authors: List<String>) = filterList(input, authors) { it }
    fun filterSeries(input: String, series: List<String>) = filterList(input, series)
    fun filterGenres(input: String, genres: List<String>) = filterList(input, genres)
    fun filterPublishers(input: String, publishers: List<String>) = filterList(input, publishers)
    fun filterLanguages(input: String, languages: List<String>) = filterList(input, languages)
    fun filterFormats(input: String, formats: List<String>) = filterList(input, formats)
    fun filterBookshelves(input: String, bookshelves: List<String>) = filterList(input, bookshelves)
    fun filterBoughtSources(input: String, bought: List<String>) = filterList(input, bought)
    fun filterGivers(input: String, givers: List<String>) = filterList(input, givers)

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
            BookSorting.SOURCE -> books.sortedBy { it.source }
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

    /**
     * Fetches an observable FlowState of a book given its unique ID.
     *
     * @param uid book ID
     * @return Flow for the book
     */
    fun getBookFlowById(uid: String): Flow<Book> {
        Timber.tag("BookVM").d("Recovering book flow with ID $uid")
        return allBooks.map { list -> list.find { it.uid == uid }}.filterNotNull()
    }


    // ---------- ADDING NEW ENTRIES ----------

    /**
     * Inserts new books in the repository. If the book already exists, its data is overwritten with the new one.
     * Also compute cover colours.
     *
     * @param books list of Book objects to be added
     */
    fun insertBooks(books: List<Book>) {
        viewModelScope.launch(Dispatchers.IO) {
            val booksWithColours = books.map { book ->
                if (book.cover != null) {
                    book.copy(coverColours = extractColours(book), chosenCoverColour = 0)
                } else book
            }
            repository.insertBooks(booksWithColours)
            Timber.tag("BookVM").d("Inserting ${books.size} into repository")
        }
    }

    /**
     * Inserts a new book in the repository. If the book already exists, its data is overwritten with the new one.
     * Also compute cover colours.
     *
     * @param book Book object to be added
     */
    fun insertBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookWithColour = if (book.cover != null) {
                book.copy(coverColours = extractColours(book), chosenCoverColour = 0)
            } else book
            repository.insertBook(bookWithColour)
            Timber.tag("BookVM").d("Inserting book \"${book.title}\" into repository")
        }
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
    fun updateCoverFromGallery(context: Context, image: Uri, book: Book, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
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
                    Timber.tag("BookVM")
                        .e("Failed to save cover from gallery Uri $image with error: $e")
                    isError(true)
                    null
                }
            }
            if (newCover == null) {
                Timber.tag("BookVM")
                    .e("Failed to save cover from gallery Uri $image: returned a null file.")
                isError(true)
            } else {
                val updatedBook = book.copy(cover = newCover, coverColours = emptyList(), chosenCoverColour = 0)
                insertBook(updatedBook)
                Timber.tag("BookVM").d("Update book \"${book.title}\" with new cover.")
                callBack()
            }
        }
    }

    /**
     * Updates all the volumes of a manga with their series ID from Mangadex.
     *
     * @param mangaId Mangadex ID for this manga series
     * @param seriesName all volumes with this series name will update their mangaID
     */
    fun updateMangaSeriesId(mangaId: String, seriesName: String) {
        viewModelScope.launch {
            repository.updateMangaSeriesId(mangaId, seriesName)
            Timber.tag("BookVM")
                .d("Update manga series \"$seriesName\" with new Mangadex ID $mangaId")
        }
    }

    /**
     * Updates the "rating" field of a Book object with a new value.
     *
     * @param newRating new value for the "rating" field
     * @param book Book object whose value to change
     */
    fun updateRating(newRating: Double, book: Book) {
        viewModelScope.launch {
            val updatedBook = book.copy(rating = newRating)
            repository.insertBook(updatedBook)
            Timber.tag("BookVM").d("Updated the \"rating\" field for book \"${book.title}\"")
        }
    }

    /**
     * Updates the "read" field of a Book object with a new value.
     *
     * @param isRead new value for the "read" field
     * @param book Book object to mark as (un)read
     */
    fun updateRead(isRead: Boolean, book: Book) {
        viewModelScope.launch {
            val updatedBook = book.copy(read = isRead)
            repository.insertBook(updatedBook)
            Timber.tag("BookVM").d("Updated the \"read\" mark for book \"${book.title}\"")
        }
    }

    /**
     * Updates the "dateStarted" field of a Book object with a new value.
     *
     * @param dateStarted new value for the "dateStarted" field
     * @param book Book object to mark as started
     */
    fun updateStart(dateStarted: Long, book: Book) {
        viewModelScope.launch {
            val updatedBook = book.copy(dateStarted = dateStarted)
            repository.insertBook(updatedBook)
            Timber.tag("BookVM").d("Updated the \"dateStarted\" for book \"${book.title}\"")
        }
    }

    /**
     * Updates the "dateFinished" field of a Book object with a new value.
     * This function also checks that this date is >= dateStarted and marks the book as read.
     *
     * @param dateFinished new value for the "dateFinished" field
     * @param book Book object to mark as finished
     * @param isError block that returns true if the input date is incoherent with the dateStarted
     */
    fun updateFinish(dateFinished: Long, book: Book, isError: (Boolean) -> Unit) {
        if (book.dateStarted <= 0L || dateFinished < book.dateStarted) {
            Timber.tag("BookVM")
                .e("Failed to update dateFinished for book \"${book.title}\": smaller than dateStarted")
            isError(true)
            return
        }

        viewModelScope.launch {
            val updatedBook = book.copy(dateFinished = dateFinished, read = true)
            repository.insertBook(updatedBook)
            Timber.tag("BookVM").d("Updated the \"dateFinished\" for book \"${book.title}\"")
        }
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
    fun clearCovers(booksToClear: List<Book>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
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
            book.copy(cover = null, coverColours = emptyList(), chosenCoverColour = 0)
        }
        viewModelScope.launch {
            repository.insertBooks(clearedBooks)
            if (errorOccurred) isError(true) else {
                Timber.tag("BookVM").d("Successfully deleted covers for ${booksToClear.size} books")
                callBack()
            }
        }
    }

    /**
     * Removes the covers of all books present in the repository.
     *
     * @param isError returns true if an error occurred while running the function
     * @param callBack block to run after the covers have been deleted
     */
    fun clearAllCovers(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            val currentBooks = allBooks.first()
            var errorOccurred = false
            clearCovers(currentBooks, { if (it) errorOccurred = true })
            {
                if (errorOccurred) isError(true)
                else callBack()
            }
        }
    }

    /**
     * Called when initializing the VM to re-compute the cover colours (in case of leftover legacy books)
     *
     */
    fun backfillCoverColours() {
        viewModelScope.launch(Dispatchers.IO) {
            val books = allBooks.first()
            val booksNeedingColours = books.filter { it.cover != null && it.coverColours.isEmpty() }
            if (booksNeedingColours.isEmpty()) return@launch
            Timber.tag("BookVM").d("Backfilling cover colours ${booksNeedingColours.size} books")

            val updated = booksNeedingColours.map { book ->
                val coverColours = extractColours(book)
                Timber.tag("Debug").d("Book ${book.title} has ${coverColours.size} colours")
                book.copy(coverColours = extractColours(book), chosenCoverColour = 0)
            }
            repository.insertBooks(updated)
        }
    }

    /**
     * Updates the index of the chosen cover colour (not the list of colours).
     *
     * @param book which index has changed
     * @param index new colour index
     */
    fun updateChosenCoverColour(book: Book, index: Int) {
        viewModelScope.launch {
            repository.insertBook(book.copy(chosenCoverColour = index))
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
    fun deleteBook(bookToDelete: Book, isError: (Boolean) -> Unit, callBack: () -> Unit) {
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

        viewModelScope.launch {
            repository.deleteBook(bookToDelete)
            Timber.tag("BookVM").d("Deleted book \"${bookToDelete.title} from the repository")
            callBack()
        }
    }

    /**
     * Deletes multiple book entries from the repository, along with its local copy of the book cover.
     *
     * @param booksToDelete list of Book objects to be deleted
     * @param isError returns true if an error occurred while executing the function
     * @param callBack block that runs once the book and its cover were successfully deleted
     */
    fun deleteBooks(booksToDelete: List<Book>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
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
        savedScrollIndex = 0
        savedScrollOffset = 0
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
