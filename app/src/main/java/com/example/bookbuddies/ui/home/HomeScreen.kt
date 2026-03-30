package com.example.bookbuddies.ui.home

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.displayAuthors
import com.example.bookbuddies.data.displayDate
import com.example.bookbuddies.data.getBookSorting
import com.example.bookbuddies.data.getString
import com.example.bookbuddies.data.findBookCovers
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.FastScroll
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.ProgressBar
import com.example.bookbuddies.ui.SingleOptionList
import com.example.bookbuddies.ui.ToggleBox
import com.example.bookbuddies.ui.settings.copyToClipboard
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.collections.mutableListOf
import kotlin.collections.set

@Composable
fun HomeScreen(bookVM: BookViewModel, navigationActions: NavigationActions) {
    Timber.tag("Debug").e("Restoring index=${bookVM.savedScrollIndex} offset=${bookVM.savedScrollOffset}")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loading = remember { mutableStateOf(false) }
    val books by bookVM.sortedBooks.collectAsState(emptyList())
    val sorting by bookVM.sorting.collectAsState()
    val onlyUnread by bookVM.onlyUnread.collectAsState()

    val deleteVisible = remember { mutableStateOf(false) }

    // list state for remembering fast-scroll position
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(bookVM.savedScrollIndex, bookVM.savedScrollOffset)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                Timber.tag("Debug").e("Saving index=$index offset=$offset")
                bookVM.savedScrollIndex = index
                bookVM.savedScrollOffset = offset
            }
    }

    // variables specifically for the "remove some covers" functionality
    val progressing = remember { mutableStateOf(false) }
    val processed = remember { mutableIntStateOf(0) }
    val total = remember { mutableIntStateOf(0) }
    val coversVisible = remember { mutableStateOf(false) }
    val failedCovers = remember { mutableListOf<String>() }
    val clipboard = LocalClipboard.current

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

    // Selection mode
    val selectionModeActive = remember { mutableStateOf(false) }
    val selectedEntries = remember { mutableStateMapOf<String, Boolean>() }
    val nbSelected by remember {
        derivedStateOf { selectedEntries.values.count { it } }
    }
    LaunchedEffect(books) {
        books.forEach { book ->
            if (selectedEntries[book.uid] == null) {
                selectedEntries[book.uid] = false
            }
        }
    }

    // when using the phone's built-in back function, stay on the current page and exit the Selection mode if active
    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
        if (selectionModeActive.value) {
            selectionModeActive.value = false
            selectedEntries.keys.forEach { key ->
                selectedEntries[key] = false
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

    // Visibility of the popup for unread filter and sorting methods
    val showFilters = remember { mutableStateOf(false) }

    if (progressing.value) {
        ProgressBar(processed.intValue, total.intValue)
    } else {
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_homeScreen),
            navigationIndex = 0,
            topBarIcons = {
                Row(
                    modifier = Modifier.padding(0.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // sorting and filter button
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
            } else if (progressing.value) {
                ProgressBar(processed.intValue, total.intValue)
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
                            .padding(vertical = 4.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectionModeActive.value) {
                            SelectionModeTopRow(
                                selectionModeActive, selectedEntries, nbSelected,

                                // option to mark some books as read
                                stringResource(R.string.button_markAsRead) to {
                                    val booksToUpdate = books.filter { selectedEntries[it.uid] == true }
                                    booksToUpdate.forEach { book ->
                                        bookVM.updateRead(true, book)
                                    }
                                    selectionModeActive.value = false
                                    selectedEntries.keys.forEach { key ->
                                        selectedEntries[key] = false
                                    }
                                },
                                // option to delete books
                                stringResource(R.string.button_deleteBooks) to  {
                                    deleteVisible.value = true
                                },
                                // option to add some covers
                                stringResource(R.string.button_addCover) to {
                                    progressing.value = true
                                    scope.launch {
                                        findBookCovers(
                                            context = context,
                                            books = books.filter { selectedEntries[it.uid] == true },
                                            insertBook = bookVM::insertBook,
                                            updateMangaSeriesId = bookVM::updateMangaSeriesId,
                                            callBack = { failedBooks ->
                                                progressing.value = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_successfulCovers),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                if (failedBooks.isEmpty()) {
                                                    selectionModeActive.value = false
                                                    selectedEntries.keys.forEach { key ->
                                                        selectedEntries[key] = false
                                                    }
                                                }
                                                else {
                                                    failedCovers.clear()
                                                    failedCovers.addAll(failedBooks)
                                                    failedCovers.sortBy { it }
                                                    coversVisible.value = true
                                                }
                                            },
                                            isError = { isError ->
                                                if (isError) {
                                                    progressing.value = false
                                                    handleError(context, context.getString(R.string.toast_coverSearchFail))
                                                }
                                            },
                                            onProgress = { processedNb, totalNb ->
                                                processed.intValue = processedNb
                                                total.intValue = totalNb
                                            }
                                        )
                                    }
                                },
                                // option to remove some covers
                                stringResource(R.string.button_removeCover) to {
                                    loading.value = true
                                    bookVM.clearCovers(
                                        books.filter { selectedEntries[it.uid] == true },
                                        {
                                            if (it) {
                                                loading.value = false
                                                handleError(context, context.getString(R.string.toast_coverRemoveFail))
                                            }
                                        }) {
                                        loading.value = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_removeSomeCovers),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        selectionModeActive.value = false
                                        selectedEntries.keys.forEach { key ->
                                            selectedEntries[key] = false
                                        }
                                    }
                                }
                            )
                        } else {
                            // Number of books displayed
                            Text(
                                modifier = Modifier.height(32.dp),
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
                        // Specific display for Author>Series sorting method, since it has subheaders
                        if (sorting == BookSorting.AUTHOR_SERIES) {
                            val groupedBooks = groupBooksSubheaders(context, onlyUnread, sorting, books)
                            FastScroll(
                                minThumbWidth = 5,
                                maxThumbWidth = 20,
                                thumbHeight = 50,
                                bubbleWidth = 150,
                                listState = listState,
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
                                                        selectionModeActive = selectionModeActive,
                                                        selectedEntries = selectedEntries
                                                    ) {
                                                        navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Display for any other sorting method
                            val groupedBooks = groupBooks(context, onlyUnread, sorting, books)
                            val groupedBooksState = remember { mutableStateOf(groupedBooks) }
                            LaunchedEffect(groupedBooks) { groupedBooksState.value = groupedBooks}
                            FastScroll(
                                minThumbWidth = 5,
                                maxThumbWidth = 20,
                                thumbHeight = 50,
                                bubbleWidth = 150,
                                listState = listState,
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
                                                    selectionModeActive = selectionModeActive,
                                                    selectedEntries = selectedEntries
                                                ) {
                                                    navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // popup with sorting options
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
                                    // sorting methods title
                                    Text(
                                        text = stringResource(R.string.title_sortBy),
                                        style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                                    )

                                    // list of sorting options with radio buttons
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

                                    // toggle box for "unread" filter
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

                    // List of failed covers
                    if (coversVisible.value) {
                        CustomContentDialogWindow(
                            visible = coversVisible,
                            content = {
                                // title
                                Text(
                                    text = context.getString(R.string.title_failedCovers),
                                    style = MyTypography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.inversePrimary, thickness = 1.5.dp)
                                // list of book titles for which covers have not been found
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .heightIn(max = 350.dp)
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    failedCovers.forEach { cover ->
                                        item {
                                            Text(text = cover, style = MyTypography.bodyMedium)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.inversePrimary, thickness = 1.5.dp)
                            },
                            bottomButtons = true,
                            leftButtonContent = {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    painter = painterResource(R.drawable.copy),
                                    tint = MaterialTheme.colorScheme.inversePrimary,
                                    contentDescription = stringResource(R.string.desc_copy)
                                )
                            },
                            leftButtonOnClick = {
                                val coversText = failedCovers.joinToString("\n")
                                copyToClipboard(context, coversText, clipboard, scope)
                            },
                            rightButtonContent = {
                                Text(
                                    text = stringResource(R.string.button_confirm),
                                    style = MyTypography.bodyLarge,
                                    color = ValidGreen
                                )
                            },
                            rightButtonOnClick = {
                                coversVisible.value = false
                                selectionModeActive.value = false
                                selectedEntries.keys.forEach { key ->
                                    selectedEntries[key] = false
                                }
                            }
                        )
                    }

                    // delete confirmation
                    if (deleteVisible.value) {
                        val booksToDelete = books.filter { selectedEntries[it.uid] == true }

                        CustomContentDialogWindow(
                            visible = deleteVisible,
                            content = {
                                Text(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    text = stringResource(R.string.txt_multiDeleteConfirm),
                                    style = MyTypography.bodyLarge
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.inversePrimary, thickness = 1.5.dp)
                                // list of book titles that have been selected for deletion
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .heightIn(max = 350.dp)
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    booksToDelete.forEach { book ->
                                        item {
                                            Text(text = book.title, style = MyTypography.bodyMedium)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.inversePrimary, thickness = 1.5.dp)
                            },
                            bottomButtons = true,
                            leftButtonContent = {
                                Text(
                                    text = stringResource(R.string.button_cancel),
                                    style = MyTypography.bodyLarge,
                                    color = MaterialTheme.colorScheme.inversePrimary
                                )
                            },
                            leftButtonOnClick = { deleteVisible.value = false },
                            rightButtonContent = {
                                Text(
                                    text = stringResource(R.string.button_confirm),
                                    style = MyTypography.bodyLarge,
                                    color = ValidGreen
                                )
                            },
                            rightButtonOnClick = {
                                loading.value = true
                                scope.launch {
                                    bookVM.deleteBooks(
                                        booksToDelete = booksToDelete,
                                        isError = {
                                            if (it) {
                                                loading.value = false
                                                handleError(context, context.getString(R.string.toast_multiDeleteFail))
                                            }
                                        }
                                    ) {
                                        deleteVisible.value = false
                                        loading.value = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_successMultiDelete),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        selectionModeActive.value = false
                                        selectedEntries.keys.forEach { key ->
                                            selectedEntries[key] = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Design for sticky headers used to group book entries depending on sorting method.
 *
 * @param headerText name of the group
 * @param collapsable whether or not this group header can be tapped to collapse/expand the group under it.
 *        If false, the header also has a smaller design
 * @param isExpanded whether or not the group under this header is expanded or collapsed. Ignored if "collapsable" is false
 * @param onToggle block that runs when the parent box or the collapse/expand button is pressed
 */
@Composable
private fun ListHeader(headerText: String, collapsable: Boolean, isExpanded: Boolean = true, onToggle: () -> Unit = {}) {
    // clickable box containing the header
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
                // header text
                Text(
                    modifier = Modifier.weight(1f),
                    text = headerText,
                    style = if (collapsable) MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            else MyTypography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                // collapse/expand icon
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

/**
 * Design for a single book entry.
 *
 * @param book Book object to display its data
 * @param selectionModeActive if true, a checkbox appears in front of the entry. The mode is activated by long-pressing any book entry
 * @param selectedEntries maps book IDs with whether or not they are selected in the current selection mode
 * @param onClick block that runs when tapping the book entry, when the selection mode is not active
 */
@Composable
private fun BookEntry(
    book: Book,
    selectionModeActive: MutableState<Boolean>,
    selectedEntries: MutableMap<String, Boolean>,
    onClick: () -> Unit,
) {
    // to animate the elements sliding left/right when exiting/entering Selection mode
    val animatedPadding by animateDpAsState(
        targetValue = if (selectionModeActive.value) 36.dp else 0.dp,
        label = "rowSlide"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionModeActive.value) selectedEntries[book.uid] =
                        !selectedEntries[book.uid]!!
                    else onClick()
                },
                onLongClick = {
                    selectionModeActive.value = true
                    selectedEntries[book.uid] = true
                }
            )
    ) {
        // checkbox to select this book entry if the selection mode is active
        if (selectionModeActive.value) {
            Checkbox(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                checked = selectedEntries[book.uid] ?: false,
                onCheckedChange = { checked ->
                    selectedEntries[book.uid] = checked
                },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }

        // rest of the book data
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = animatedPadding)
                .padding(start = 16.dp, end = 46.dp, top = 3.dp, bottom = 3.dp)
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // book cover (or placeholder if null)
            CoverImage(65.dp, book.cover)

            // book data
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // book title
                Text(
                    text = book.title,
                    style = MyTypography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                // author(s)
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
        // tick icon if the book has been read
        if (book.read) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.tick),
                    contentDescription = stringResource(R.string.desc_read)
                )
            }
        }
    }
}


/**
 * Top row when in Selection Mode, that displays various actions to take on the selected book entries.
 *
 * @param selectionModeActive whether or not the Selection mode is currently active
 * @param selectedEntries maps book IDs to whether or not they're currently selected
 * @param nbSelected number of currently selected book entries
 * @param options non-exhaustive number of pairs.
 * Each pair contains a string for the name of the action appearing in the drop-down menu, and a block to run when that button is pressed.
 */
@Composable
fun SelectionModeTopRow(selectionModeActive: MutableState<Boolean>, selectedEntries: MutableMap<String, Boolean>, nbSelected: Int, vararg options: Pair<String, () -> Unit>) {
    // left-side: selection number
    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp, alignment = Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // checkbox to (de-)select all
        Checkbox(
            modifier = Modifier.size(20.dp),
            checked = selectedEntries.values.all { it },
            onCheckedChange = { checked ->
                selectedEntries.keys.forEach { key ->
                    selectedEntries[key] = checked
                }
            },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Text(
            text = stringResource(R.string.txt_nbSelected, nbSelected),
            style = MyTypography.bodyMedium
        )
    }

    // right-side: options and cancel
    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionsMenu(
            icon = R.drawable.options,
            options = options
        )
        IconButton(
            onClick = {
                selectionModeActive.value = false
                selectedEntries.keys.forEach { key ->
                    selectedEntries[key] = false
                }
            }
        ) {
            Icon(
                painterResource(R.drawable.cancel),
                modifier = Modifier.size(24.dp),
                contentDescription = stringResource(R.string.desc_cancel)
            )
        }
    }
}

/**
 * When changing the sorting method, sorts books and groups them based on titles that make sense with the sorting method. Rewrites group headers.
 *
 * @param context to access string resources
 * @param unreadFilter whether or not to filter out books that have been read (only showing unread books)
 * @param sorting current sorting method
 * @param books current list of all Book objects
 * @return Map that maps group headers to their sorted list of books
 */
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
            displayDate(book.dateAdded, DateFormat.MONTH_YEAR)
        }
        BookSorting.RATING -> filteredBooks.groupBy { book ->
            val rounded = book.rating.toInt().coerceIn(0, 5)
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
        BookSorting.SOURCE -> filteredBooks.groupBy { book ->
            book.source.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknown)
        }
    }
}

/**
 * Specifically handles sorting and re-grouping of books for the Author>Series sorting method, since it also requires subheaders.
 *
 * @param context to access string resources
 * @param unreadFilter whether or not to filter out books that have been read (only showing unread books)
 * @param sorting current sorting method
 * @param books current list of all Book objects
 * @return Map that maps group headers to a mapping of group subheaders to their sorted list of books
 */
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