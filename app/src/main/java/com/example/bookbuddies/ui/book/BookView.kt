package com.example.bookbuddies.ui.book

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.displayAuthors
import com.example.bookbuddies.data.displaySeries
import com.example.bookbuddies.datastore.fetchCoverForBook
import com.example.bookbuddies.datastore.fetchCoverForManga
import com.example.bookbuddies.datastore.findBookCovers
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.system.checkPermission
import com.example.bookbuddies.system.imagePermissionVersion
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.RatingStars
import com.example.bookbuddies.ui.SecondaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.BookViewModel
import kotlinx.coroutines.launch

@Composable
fun BookView(bookID: String, bookVM: BookViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val book by bookVM.getBookFlowById(bookID).collectAsState(initial = Book.empty())

    val loading = remember { mutableStateOf(false) }

    val deleteVisible = remember { mutableStateOf(false) }
    val coverOptions = remember { mutableStateOf(false) }
    val showRating = remember { mutableStateOf(false) }

    // getting image and image permissions
    val imageInput = "image/*"
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            loading.value = true
            scope.launch {
                bookVM.updateCoverFromGallery(
                    context = context,
                    image = imageUri,
                    book = book,
                    isError = {
                        if (it) {
                            coverOptions.value = false
                            loading.value = false
                            handleError(context, context.getString(R.string.toast_manualCoverFail))
                        }
                    }
                ) {
                    coverOptions.value = false
                    loading.value = false
                }
            }
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getImage.launch(imageInput)
        }

    // normal book view screen
    SecondaryScreen(
        title = "",
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // edit the book
                IconButton(
                    onClick = { navigationActions.navigateTo("${Route.BOOK_EDIT}/${book.uid}") }
                ) {
                    Icon(
                        painterResource(R.drawable.edit),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_edit)
                    )
                }
                OptionsMenu(
                    icon = R.drawable.options,
                    stringResource(R.string.button_deleteBook) to {
                        deleteVisible.value = true
                    }
                )
            }
        }
    ) { paddingValues ->
        if (loading.value) {
            MiniLoading(paddingValues)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // first row has main book data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // book cover
                        CoverImage(
                            180.dp,
                            book.cover,
                            context.getString(R.string.desc_coverImage)
                        ) {
                            coverOptions.value = true
                        }
                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp)
                                .heightIn(min = 180.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4 .dp, Alignment.Top)
                            ) {
                                // series
                                if (book.seriesName.isNotBlank()) {
                                    Text(
                                        text = displaySeries(book.seriesName, book.seriesNumber),
                                        style = MyTypography.titleSmall.copy(
                                            textAlign = TextAlign.Start, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                                // title
                                Text(
                                    text = book.title,
                                    style = MyTypography.titleMedium.copy(textAlign = TextAlign.Start)
                                )
                                // author(s)
                                Text(
                                    text = displayAuthors(book.authors),
                                    style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start, fontWeight = FontWeight.Normal)
                                )
                            }
                            // rating
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRating.value = true },
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RatingStars(
                                    rating = book.rating,
                                    starSize = 32.dp,
                                    starSpacing = 8.dp,
                                    emptyStarColor = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
                // Reading status
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // title + read icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.title_readingStatus),
                                style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            )
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(R.drawable.tick),
                                contentDescription = context.getString(R.string.desc_read)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                        // start reading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.txt_startReading),
                                style = MyTypography.bodyLarge
                            )
                            TextButton(
                                modifier = Modifier
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .background(
                                        color = Color.Transparent, shape = RoundedCornerShape(50)
                                    ),
                                onClick = {
                                    // todo: update with bookVM
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.txt_startedToday), style = MyTypography.bodySmall, color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                    }
                }
            }
        }

        // delete confirmation
        if (deleteVisible.value) {
            CustomContentDialogWindow(
                visible = deleteVisible,
                content = {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringResource(R.string.txt_deleteConfirm),
                        style = MyTypography.bodyLarge
                    )
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
                        bookVM.deleteBook(
                            bookToDelete = book,
                            isError = {
                                if (it) {
                                    loading.value = false
                                    handleError(context,
                                        context.getString(R.string.toast_deleteFail))
                                }
                            }
                        ) {
                            deleteVisible.value = false
                            loading.value = false
                            navigationActions.navigateTo(Route.HOME, true)
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_successDelete),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        // dialog window to input new rating
        if (showRating.value) {
            val currentRating = remember { mutableDoubleStateOf(book.rating) }

            CustomContentDialogWindow(
                visible = showRating,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RatingStars(
                            rating = currentRating.doubleValue,
                            starSize = 48.dp,
                            starSpacing = 0.dp,
                            emptyStarColor = MaterialTheme.colorScheme.onBackground
                        ) { newRating ->
                            currentRating.doubleValue = newRating
                        }
                    }
                },
                bottomButtons = true,
                leftButtonContent = {
                    Text(
                        text = context.getString(R.string.button_cancel),
                        style = MyTypography.bodyLarge,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                },
                leftButtonOnClick = { showRating.value = false },
                rightButtonContent = {
                    Text(
                        text = context.getString(R.string.button_confirm),
                        style = MyTypography.bodyLarge,
                        color = ValidGreen
                    )
                },
                rightButtonOnClick = {
                    scope.launch {
                        bookVM.updateRating(currentRating.doubleValue, book)
                    }
                    showRating.value = false
                }
            )
        }

        // dialog window for various options to take on the book cover
        if (coverOptions.value) {
            CustomContentDialogWindow(
                visible = coverOptions,
                content = {
                    RowTextButton(stringResource(R.string.button_singleCoverManual)) {
                        checkPermission(context, imagePermission, requestMediaPermissionLauncher) {
                            getImage.launch(imageInput)
                        }
                    }
                    RowTextButton(stringResource(R.string.button_singleBookCoverAuto)) {
                        coverOptions.value = false
                        loading.value = true
                        scope.launch {
                            val updatedBook = fetchCoverForBook(context, book, true)
                            bookVM.insertBook(updatedBook)
                            loading.value = false
                        }
                    }
                    RowTextButton(stringResource(R.string.button_singleMangaCoverAuto)) {
                        coverOptions.value = false
                        loading.value = true
                        scope.launch {
                            val updatedBook = fetchCoverForManga(context, book, bookVM::updateMangaSeriesId, true)
                            bookVM.insertBook(updatedBook)
                            loading.value = false
                        }
                    }
                    RowTextButton(stringResource(R.string.button_singleCoverDelete)) {
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
                                coverOptions.value = false
                            }
                        }
                    }
                },
                bottomButtons = false
            )
        }
    }
}

@Composable
private fun RowTextButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text, style = MyTypography.bodyLarge
        )
    }
}