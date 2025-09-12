package com.example.bookbuddies.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.R
import com.example.bookbuddies.data.displayAuthors
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.theme.MyTypography

@Composable
fun HomeScreen(bookVM: BookViewModel, navigationActions: NavigationActions) {

    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    val loading = remember { mutableStateOf(false) }
    val books by bookVM.allBooks.collectAsState(emptyList())

    PrimaryScreen(
        navigationActions = navigationActions,
        title = "My Books",
        topBarIcons = {}
    ) { paddingValues ->
        if (loading.value) {
            MiniLoading(paddingValues)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (books.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            text = stringResource(R.string.txt_noResults),
                            style = MyTypography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    books.forEach { book ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigationActions.navigateTo("${Route.BOOK}/${book.uid}")
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row (
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    CoverImage(
                                        60.dp,
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                        }
                    }
                }
            }
        }
    }
}