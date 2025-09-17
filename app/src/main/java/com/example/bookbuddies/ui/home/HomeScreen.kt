package com.example.bookbuddies.ui.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
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
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.theme.MyTypography
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun HomeScreen(bookVM: BookViewModel, navigationActions: NavigationActions) {

    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }
    val books by bookVM.sortedBooks.collectAsState(emptyList())
    val sorting by bookVM.sorting.collectAsState()

    val showFilters = remember { mutableStateOf(false) }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = "My Books",
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // filters button
                IconButton(
                    onClick = { showFilters.value = !showFilters.value }
                ) {
                    Icon(
                        painterResource(R.drawable.tuning),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_filters)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (loading.value) {
            MiniLoading(paddingValues)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // TODO: add quick scroll bar
                if (books.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            text = stringResource(R.string.txt_noResults),
                            style = MyTypography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    if (sorting == BookSorting.AUTHOR_SERIES) {
                        // Sorting(s) that require subheaders
                        val groupedBooks = groupBooksSubheaders(context, sorting, books)
                        groupedBooks.forEach { (author, seriesMap) ->
                            // Author header
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.colorScheme.background)
                                        .padding(top = 16.dp)
                                        .clickable { /* todo: collapse */ },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Bottom,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(start = 16.dp),
                                            text = author,
                                            style = MyTypography.titleMedium.copy(textAlign = TextAlign.Start),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                                    }
                                }
                            }

                            seriesMap.forEach { (series, seriesBooks) ->
                                // Series subheader
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(color = MaterialTheme.colorScheme.background)
                                            .padding(top = 10.dp)
                                            .clickable { /* todo: collapse */ },
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.Bottom,
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                modifier = Modifier.padding(start = 16.dp),
                                                text = series,
                                                style = MyTypography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                                        }
                                    }
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
                    } else {
                        // Single header sortings
                        val groupedBooks = groupBooks(context, sorting, books)
                        groupedBooks.forEach { (header, bookEntries) ->
                            // Header
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.colorScheme.background)
                                        .padding(top = 16.dp)
                                        .clickable { /* todo: collapse */ },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Bottom,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(start = 16.dp),
                                            text = header,
                                            style = MyTypography.titleMedium.copy(textAlign = TextAlign.Start),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                                    }
                                }
                            }

                            // Books in the series
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

            if (showFilters.value) {
                Dialog(onDismissRequest = { showFilters.value = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.outline,
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            // todo: show filters and sorting options (dismiss on click)
                            // todo: figure out "unread" filter
                            Text(text = "show filters here")
                        }
                    }
                }
            }
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

private fun groupBooks(context: Context, sorting: BookSorting, books: List<Book>): Map<String, List<Book>> {
    return when (sorting) {
        BookSorting.AUTHOR_SERIES -> {
            // Not handled here — use dedicated groupBooksSubheaders()
            emptyMap()
        }
        BookSorting.SERIES -> books.groupBy { book ->
            book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
        }
        BookSorting.TITLE -> books.groupBy { book ->
            book.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }
        BookSorting.RECENTLY_ADDED -> books.groupBy { book ->
            val calendar = Calendar.getInstance().apply { timeInMillis = book.dateAdded }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            "$month $year"
        }
        BookSorting.GENRE -> books.groupBy { book ->
            book.genre.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownGenre)
        }
        BookSorting.RATING -> books.groupBy { book ->
            val rounded = book.rating.toInt().coerceIn(1, 5)
            "$rounded ★"
        }
    }
}

private fun groupBooksSubheaders(context: Context, sorting: BookSorting, books: List<Book>): Map<String, Map<String, List<Book>>> {
    if (sorting == BookSorting.AUTHOR_SERIES) {
        return books.groupBy { book ->
            book.authors.firstOrNull()?.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownAuthor)
        }.mapValues { entry ->
            entry.value.groupBy { book ->
                book.seriesName.takeIf { it.isNotBlank() } ?: context.getString(R.string.txt_unknownSeries)
            }
        }
    }
    return emptyMap()
}