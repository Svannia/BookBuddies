package com.example.bookbuddies.ui.book

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.datastore.fetchCoverForBook
import com.example.bookbuddies.datastore.fetchCoverForManga
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.system.checkPermission
import com.example.bookbuddies.system.imagePermissionVersion
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.RowTextButton
import com.example.bookbuddies.ui.SecondaryScreen
import com.example.bookbuddies.viewModels.BookViewModel
import kotlinx.coroutines.launch

@Composable
fun BookEdit(bookID: String, bookVM: BookViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loading = remember { mutableStateOf(false) }

    val book by bookVM.getBookFlowById(bookID).collectAsState(initial = Book.empty())

    val cover = remember { mutableStateOf(book.cover) }
    val tempCover = remember { mutableStateOf(Uri.EMPTY) }

    LaunchedEffect(book) {
        cover.value = book.cover
    }

    // getting image and image permissions
    val imageInput = "image/*"
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            tempCover.value = uri
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getImage.launch(imageInput)
        }

    SecondaryScreen(
        title = stringResource(R.string.title_editBook),
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {}
    ) { paddingValues ->
        if (loading.value) MiniLoading(paddingValues)
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // cover
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tempCover.value != Uri.EMPTY) {
                                CoverImage(height = 130.dp, picture = tempCover.value)
                            } else {
                                CoverImage(height = 130.dp, picture = cover.value)
                            }

                            Column {
                                RowTextButton(stringResource(R.string.button_singleCoverManual), 34.dp) {
                                    checkPermission(context, imagePermission, requestMediaPermissionLauncher) {
                                        getImage.launch(imageInput)
                                    }
                                }
                                RowTextButton(stringResource(R.string.button_singleBookCoverAuto), 34.dp) {
                                    loading.value = true
                                    scope.launch {
                                        val updatedBook = fetchCoverForBook(context, book, true)
                                        bookVM.insertBook(updatedBook)
                                        loading.value = false
                                    }
                                }
                                RowTextButton(stringResource(R.string.button_singleMangaCoverAuto), 34.dp) {
                                    loading.value = true
                                    scope.launch {
                                        val updatedBook = fetchCoverForManga(context, book, bookVM::updateMangaSeriesId, true)
                                        bookVM.insertBook(updatedBook)
                                        loading.value = false
                                    }
                                }
                                RowTextButton(stringResource(R.string.button_singleCoverDelete), 34.dp) {
                                    loading.value = true
                                    scope.launch {
                                        bookVM.clearCovers(
                                            booksToClear = listOf(book),
                                            isError = {
                                                if (it) {
                                                    loading.value = false
                                                    handleError(context,
                                                        context.getString(R.string.toast_coverDeleteFail))
                                                }
                                            }
                                        ) {
                                            loading.value = false
                                        }
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