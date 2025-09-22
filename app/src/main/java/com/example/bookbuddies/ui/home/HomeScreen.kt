package com.example.bookbuddies.ui.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookSorting
import com.example.bookbuddies.data.displayAuthors
import com.example.bookbuddies.data.getBookSorting
import com.example.bookbuddies.data.getString
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.FastScroll
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.SingleOptionList
import com.example.bookbuddies.ui.ToggleBox
import com.example.bookbuddies.ui.theme.MyTypography
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(bookVM: BookViewModel, navigationActions: NavigationActions) {

    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }
    val books by bookVM.sortedBooks.collectAsState(emptyList())
    val sorting by bookVM.sorting.collectAsState()
    val onlyUnread by bookVM.onlyUnread.collectAsState()

    // expanded state of each group, re-initialized when the sorting or book entries are changed
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(books, sorting, onlyUnread) {
        val keys = if (sorting == BookSorting.AUTHOR_SERIES) {
            groupBooksSubheaders(context, onlyUnread, sorting, books).keys
        } else {
            groupBooks(context, onlyUnread, sorting, books).keys
        }
        // add eventual new header
        keys.forEach { key ->
            if (expandedStates[key] == null) expandedStates[key] = true
        }
        // remove unused headers (when switching sorting for example)
        val toRemove = expandedStates.keys - keys
        toRemove.forEach { expandedStates.remove(it) }
    }

    val displayedCount by remember {
        derivedStateOf {
            if (sorting == BookSorting.AUTHOR_SERIES) {
                val grouped = groupBooksSubheaders(context, onlyUnread, sorting, books)
                grouped.entries.sumOf { (author, seriesMap) ->
                    if (expandedStates[author] == true) {
                        seriesMap.values.sumOf { it.size }
                    } else 0
                }
            } else {
                val grouped = groupBooks(context, onlyUnread, sorting, books)
                grouped.entries.sumOf { (header, entries) ->
                    if (expandedStates[header] == true) entries.size
                    else 0
                }
            }
        }
    }

    // Expand/collapse all
    val expandAll: () -> Unit = {
        expandedStates.keys.forEach { expandedStates[it] = true }
    }
    val collapseAll: () -> Unit = {
        expandedStates.keys.forEach { expandedStates[it] = false }
    }

    // Selection mode
    val selectionModeActive = remember { mutableStateOf(false) }

    // Filters and sorting method
    val showFilters = remember { mutableStateOf(false) }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_homeScreen),
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // sorting and filters button
                IconButton(
                    onClick = { showFilters.value = !showFilters.value }
                ) {
                    Icon(
                        painterResource(R.drawable.sorting),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_filters)
                    )
                }
                OptionsMenu(
                    icon = R.drawable.options,
                    stringResource(R.string.button_collapseAll) to { collapseAll() },
                    stringResource(R.string.button_expandAll) to { expandAll() }
                )
            }
        }
    ) { paddingValues ->
        if (loading.value) {
            MiniLoading(paddingValues)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            {
                // Special row below the top bar, always visible (independent of lazy list scrolling)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.background)
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                ) {
                    if (selectionModeActive.value) {
                        // Selection mode
                        // TODO
                    } else {
                        // Number of books displayed
                        Text(
                            text = "Displaying $displayedCount books",
                            style = MyTypography.bodyMedium
                        )
                    }
                }

                // No books display
                if (books.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        text = stringResource(R.string.txt_noResults),
                        style = MyTypography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    if (sorting == BookSorting.AUTHOR_SERIES) {
                        val groupedBooks = groupBooksSubheaders(context, onlyUnread, sorting, books)
                        FastScroll(
                            minThumbWidth = 5,
                            maxThumbWidth = 20,
                            thumbHeight = 50,
                            bubbleWidth = 150,
                            headerResolver = remember(groupedBooks, expandedStates) {
                                { index ->
                                    val flatList = mutableListOf<String>()
                                    groupedBooks.forEach { (author, seriesMap) ->
                                        flatList += author // sticky author header counts
                                        if (expandedStates[author] == true) {
                                            seriesMap.forEach { (_, seriesBooks) ->
                                                flatList += author
                                                seriesBooks.forEach { _ ->
                                                    flatList += author
                                                }
                                            }
                                        }
                                    }
                                    flatList.getOrNull(index)
                                }
                            }
                        ) {
                            groupedBooks.forEach { (author, seriesMap) ->
                                // Author header
                                stickyHeader(key = author) {
                                    ListHeader(author, true, expandedStates[author] ?: true) {
                                        expandedStates[author] = !(expandedStates[author] ?: true)
                                    }
                                }

                                if (expandedStates[author] == true) {
                                    seriesMap.forEach { (series, seriesBooks) ->
                                        // Series subheader
                                        item {
                                            ListHeader(series, false)
                                        }

                                        // Books in the series
                                        seriesBooks.forEach { book ->
                                            item {
                                                BookEntry(
                                                    book = book,
                                                    onClick = {
                                                        navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val groupedBooks = groupBooks(context, onlyUnread, sorting, books)
                        val groupedBooksState = remember { mutableStateOf(groupedBooks) }
                        LaunchedEffect(groupedBooks) { groupedBooksState.value = groupedBooks}
                        FastScroll(
                            minThumbWidth = 5,
                            maxThumbWidth = 20,
                            thumbHeight = 50,
                            bubbleWidth = 150,
                            headerResolver = { index ->
                                val flatList = mutableListOf<String>()
                                groupedBooksState.value.forEach { (header, books) ->
                                    flatList += header
                                    if (expandedStates[header] == true) {
                                        books.forEach { _ ->
                                            flatList += header
                                        }
                                    }
                                }
                                flatList.getOrNull(index)
                            }
                        ) {
                            groupedBooks.forEach { (header, bookEntries) ->
                                // Header
                                stickyHeader {
                                    ListHeader(header, true, expandedStates[header] ?: true) {
                                        expandedStates[header] = !(expandedStates[header] ?: true)
                                    }
                                }
                                // Books in this group
                                if (expandedStates[header] == true) {
                                    bookEntries.forEach { book ->
                                        item {
                                            BookEntry(
                                                book = book,
                                                onClick = {
                                                    navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                /*LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    else {
                        if (sorting == BookSorting.AUTHOR_SERIES) {
                            // Sorting(s) that require subheaders
                            val groupedBooks = groupBooksSubheaders(context, onlyUnread, sorting, books)
                            groupedBooks.forEach { (author, seriesMap) ->
                                // Author header
                                stickyHeader {
                                    ListHeader(author, true, expandedStates[author] ?: true) {
                                        expandedStates[author] = !(expandedStates[author] ?: true)
                                    }
                                }

                                if (expandedStates[author] == true) {
                                    seriesMap.forEach { (series, seriesBooks) ->
                                        // Series subheader
                                        item {
                                            ListHeader(series, false)
                                        }

                                        // Books in the series
                                        seriesBooks.forEach { book ->
                                            item {
                                                BookEntry(
                                                    book = book,
                                                    onClick = {
                                                        navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Single header sorting methods
                            val groupedBooks = groupBooks(context, onlyUnread, sorting, books)
                            FastScrollBar(
                                groupedData = groupedBooks,
                                itemContent = { book ->
                                    BookEntry(
                                        book = book,
                                        onClick = {
                                            navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                        }
                                    )
                                },
                                headerContent = { header ->
                                    ListHeader(header, true, expandedStates[header] ?: true) {
                                        expandedStates[header] = !(expandedStates[header] ?: true)
                                    }
                                }
                            )
                            groupedBooks.forEach { (header, bookEntries) ->
                                // Header
                                stickyHeader {
                                    ListHeader(header, true, expandedStates[header] ?: true) {
                                        expandedStates[header] = !(expandedStates[header] ?: true)
                                    }
                                }

                                // Books in this group
                                if (expandedStates[header] == true) {
                                    bookEntries.forEach { book ->
                                        item {
                                            BookEntry(
                                                book = book,
                                                onClick = {
                                                    navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }*/

                if (showFilters.value) {
                    Dialog(onDismissRequest = { showFilters.value = false }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.outline,
                            tonalElevation = 0.dp,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = stringResource(R.string.title_sortBy),
                                    style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                                )
                                val sortingOptions = BookSorting.entries.map { it.getString(context) }
                                SingleOptionList(
                                    sortingOptions.size,
                                    sorting.getString(context),
                                    sortingOptions
                                ) {
                                    val newSorting = it.getBookSorting(context)
                                    bookVM.setSorting(newSorting)
                                    showFilters.value = false
                                }
                                Spacer(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp))
                                ToggleBox(
                                    isRadio = false,
                                    boxHeight = 20.dp,
                                    rowPadding = PaddingValues(),
                                    rowSpacing = 8.dp,
                                    optionText = stringResource(R.string.button_unread),
                                    textStyle = MyTypography.bodyMedium,
                                    isToggled = onlyUnread
                                ) {
                                    bookVM.switchUnreadFilter()
                                    showFilters.value = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(headerText: String, collapsable: Boolean, isExpanded: Boolean = true, onToggle: () -> Unit = {}) {
    Box(
        modifier = if (collapsable) {
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(top = 12.dp)
                .clickable { onToggle() }
        } else {
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(top = 8.dp)
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = headerText,
                    style = if (collapsable) MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            else MyTypography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (collapsable) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(if (isExpanded) R.drawable.up else R.drawable.down),
                        contentDescription = stringResource(R.string.desc_expandCollapse),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
        }
    }
}

@Composable
private fun BookEntry(book: Book, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            CoverImage(
                65.dp,
                book.cover,
                stringResource(R.string.desc_coverImage)
            )
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = book.title,
                    style = MyTypography.bodyLarge
                )
                val authors = displayAuthors(book.authors)
                if (authors.isNotBlank()) {
                    Text(
                        text = "by $authors",
                        style = MyTypography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun groupBooks(context: Context, unreadFilter: Boolean, sorting: BookSorting, books: List<Book>): Map<String, List<Book>> {
    val filteredBooks = if (unreadFilter) books.filter { !it.read } else books

    return when (sorting) {
        BookSorting.AUTHOR_SERIES -> {
            // Not handled here — use dedicated groupBooksSubheaders()
            emptyMap()
        }
        BookSorting.SERIES -> filteredBooks.groupBy { book ->
            book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
        }
        BookSorting.TITLE -> books.groupBy { book ->
            book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }
        BookSorting.RECENTLY_ADDED -> filteredBooks.groupBy { book ->
            val calendar = Calendar.getInstance().apply { timeInMillis = book.dateAdded }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            "$month $year"
        }
        BookSorting.RATING -> filteredBooks.groupBy { book ->
            val rounded = book.rating.toInt().coerceIn(1, 5)
            "$rounded ★"
        }
        BookSorting.GENRE -> filteredBooks.groupBy { book ->
            book.genre.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.LANGUAGE -> filteredBooks.groupBy { book ->
            book.language.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.FORMAT -> filteredBooks.groupBy { book ->
            book.format.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
        BookSorting.BOUGHT_AT -> filteredBooks.groupBy { book ->
            book.boughtAt.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
    }
}

private fun groupBooksSubheaders(context: Context, unreadFilter: Boolean, sorting: BookSorting, books: List<Book>): Map<String, Map<String, List<Book>>> {
    if (sorting == BookSorting.AUTHOR_SERIES) {
        val filteredBooks = if (unreadFilter) books.filter { !it.read } else books

        return filteredBooks.groupBy { book ->
            book.authors.firstOrNull()?.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownAuthor)
        }.mapValues { entry ->
            entry.value.groupBy { book ->
                book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
            }
        }
    }
    return emptyMap()
}