package com.example.bookbuddies.ui.book

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.displayAuthor
import com.example.bookbuddies.data.displayDate
import com.example.bookbuddies.data.storeAuthor
import com.example.bookbuddies.datastore.ThemeChoice
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.system.checkPermission
import com.example.bookbuddies.system.imagePermissionVersion
import com.example.bookbuddies.ui.CoverImage
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.CustomDatePicker
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.RatingStars
import com.example.bookbuddies.ui.RowTextButton
import com.example.bookbuddies.ui.ToggleBox
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.BookViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private const val FULL_LENGTH = 300
private const val PUB_DATE = "pub"
private const val START_DATE = "start"
private const val FINISH_DATE = "finish"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShared(
    context: Context,
    navigationActions: NavigationActions,
    screenTitle: String,
    warningText: String,
    onGoBack: () -> Unit,
    themeChoice: ThemeChoice,
    bookVM: BookViewModel,
    book: Book? = null
) {
    val loading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val cancelVisible = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }
    BackHandler {
        if (dataEdited.value) cancelVisible.value = true
        else onGoBack()
    }

    val allAuthors by bookVM.allAuthors.collectAsState()
    val allSeries by bookVM.allSeries.collectAsState()
    val allGenres by bookVM.allGenres.collectAsState()
    val allPublishers by bookVM.allPublishers.collectAsState()
    val allLanguages by bookVM.allLanguages.collectAsState()
    val allFormats by bookVM.allFormats.collectAsState()
    val allBookshelves by bookVM.allBookshelves.collectAsState()
    val allBought by bookVM.allBought.collectAsState()
    val allGivers by bookVM.allGivers.collectAsState()

    val tempCover = remember { mutableStateOf(Uri.EMPTY) }

    val isbn = remember { mutableStateOf(book?.isbn ?: "") }
    val title = remember { mutableStateOf(book?.title ?: "") }
    val authors = remember { mutableStateListOf<String>().apply {
        addAll(book?.authors?.map { displayAuthor(it) } ?: emptyList()) }
    }
    val cover = remember { mutableStateOf(book?.cover) }
    val seriesName = remember { mutableStateOf(book?.seriesName ?: "") }
    val seriesNb = remember { mutableIntStateOf(book?.seriesNumber ?: -1) }
    val description = remember { mutableStateOf(book?.description ?: "") }
    val genre = remember { mutableStateOf(book?.genre ?: "") }
    val publisher = remember { mutableStateOf(book?.publisher ?: "") }
    val pubDate = remember { mutableLongStateOf(book?.publishedDate ?: 0L) }
    val rating = remember { mutableDoubleStateOf(book?.rating ?: 0.0) }
    val language = remember { mutableStateOf(book?.language ?: "") }
    val format = remember { mutableStateOf(book?.format ?: "") }
    val read = remember { mutableStateOf(false) }
    val startDate = remember { mutableLongStateOf(book?.dateStarted ?: 0L) }
    val finishDate = remember { mutableLongStateOf(book?.dateFinished ?: 0L) }
    val bookshelf = remember { mutableStateOf(book?.bookshelf ?: "") }
    val source = remember { mutableStateOf(book?.source ?: "") }
    val isGift = remember { mutableStateOf(book?.isGift ?: false) }

    LaunchedEffect(book) {
        if (book != null) {
            isbn.value = book.isbn
            title.value = book.title
            authors.clear()
            authors.addAll(book.authors.map { displayAuthor(it) })
            cover.value = book.cover
            seriesName.value = book.seriesName
            seriesNb.intValue = book.seriesNumber
            description.value = book.description
            genre.value = book.genre
            publisher.value = book.publisher
            pubDate.longValue = book.publishedDate
            rating.doubleValue = book.rating
            language.value = book.language
            format.value = book.format
            read.value = book.read
            startDate.longValue = book.dateStarted
            finishDate.longValue = book.dateFinished
            bookshelf.value = book.bookshelf
            source.value = book.source
            isGift.value = book.isGift
        }
    }
    val deleteCover = remember { mutableStateOf(false) }

    // getting image and image permissions
    val imageInput = "image/*"
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            tempCover.value = uri
            dataEdited.value = true
            deleteCover.value = false
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getImage.launch(imageInput)
        }

    // for picking a date
    var activeDateField by remember { mutableStateOf<String?>(null) }
    val datePickerVisible = remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = { Text(text = screenTitle, style = MyTypography.titleMedium) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (dataEdited.value) cancelVisible.value = true
                                else onGoBack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.go_back),
                                contentDescription = stringResource(R.string.desc_goBack)
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(65.dp)
                    .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                tonalElevation = 0.dp,
                containerColor = Color.Transparent
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // check dates validity
                        if (finishDate.longValue > 0L && startDate.longValue <= 0L) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_missingStartDate),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (startDate.longValue > 0L && finishDate.longValue > 0L && finishDate.longValue < startDate.longValue) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_wrongDateFinished),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (finishDate.longValue > 0L) read.value = true
                        loading.value = true

                        val uid = book?.uid ?: UUID.randomUUID().toString().replace("-", "")
                        val updatedBook = book?.copy(
                            isbn = isbn.value,
                            title = title.value,
                            authors = authors.filter { it.isNotBlank() },
                            cover = if (deleteCover.value) null
                                    else cover.value,
                            seriesName = seriesName.value,
                            seriesNumber = seriesNb.intValue,
                            description = description.value,
                            genre = genre.value,
                            publisher = publisher.value,
                            publishedDate = pubDate.longValue,
                            rating = rating.doubleValue,
                            language = language.value,
                            format = format.value,
                            read = read.value,
                            dateStarted = startDate.longValue,
                            dateFinished = finishDate.longValue,
                            bookshelf = bookshelf.value,
                            source = source.value,
                            isGift = isGift.value
                        ) ?: Book(
                            uid = uid,
                            isbn = isbn.value,
                            title = title.value,
                            authors = authors.filter { it.isNotBlank() },
                            cover = if (deleteCover.value) null
                                    else cover.value,
                            seriesName = seriesName.value,
                            seriesNumber = seriesNb.intValue,
                            description = description.value,
                            genre = genre.value,
                            publisher = publisher.value,
                            publishedDate = pubDate.longValue,
                            rating = rating.doubleValue,
                            language = language.value,
                            format = format.value,
                            read = read.value,
                            dateStarted = startDate.longValue,
                            dateFinished = finishDate.longValue,
                            bookshelf = bookshelf.value,
                            source = source.value,
                            isGift = isGift.value,
                            dateAdded = System.currentTimeMillis()
                        )
                        scope.launch {
                            bookVM.insertBook(updatedBook)
                        }
                        navigationActions.navigateTo("${Route.BOOK}/$uid", clearPrevious = true)

                        loading.value = false
                    },
                    enabled = dataEdited.value,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.button_save),
                        style = MyTypography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        content = { paddingValues ->
            val topPaddingPx = with(LocalDensity.current) {
                40.dp.toPx().roundToInt()
            }

            if (loading.value) MiniLoading(paddingValues)
            else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // cover (index 0)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (deleteCover.value) {
                                    CoverImage(height = 130.dp, picture = null)
                                } else if (tempCover.value != Uri.EMPTY) {
                                    CoverImage(height = 130.dp, picture = tempCover.value)
                                } else {
                                    CoverImage(height = 130.dp, picture = cover.value)
                                }

                                Column(verticalArrangement = Arrangement.Center) {
                                    RowTextButton(stringResource(R.string.button_singleCoverManual), 34.dp) {
                                        checkPermission(context, imagePermission, requestMediaPermissionLauncher) {
                                            getImage.launch(imageInput)
                                        }
                                    }
                                    RowTextButton(stringResource(R.string.button_singleCoverDelete), 34.dp) {
                                        tempCover.value = Uri.EMPTY
                                        deleteCover.value = true
                                        dataEdited.value = true
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.5.dp)
                        }
                    }

                    // title (index 1)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_title),
                            value = title.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.open_book,
                            maxLength = 65,
                            showSuggestions = false,
                            suggestions = { emptyList() },
                            onFocusEvent = {},
                            onValueChange = { title.value = it; dataEdited.value = true }
                        )
                    }

                    // authors (index 2)
                    item {
                        AuthorsListInputFields(
                            title = stringResource(R.string.title_authors),
                            listValues = authors,
                            icon = R.drawable.user,
                            suggestions = { bookVM.filterAuthors(it, allAuthors) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(2, topPaddingPx) }
                                }
                            }
                        ) { newValue, idx ->
                            authors[idx] = newValue
                            dataEdited.value = true
                        }
                    }

                    // series (index 3)
                    item {
                        SingleInputField(
                            title = "Series",
                            value = seriesName.value,
                            fieldWidth = FULL_LENGTH - 100,
                            icon = R.drawable.sheets,
                            maxLength = 30,
                            showSuggestions = true,
                            suggestions = { bookVM.filterSeries(it, allSeries) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(3, topPaddingPx) }
                                }
                            },
                            onValueChange = { seriesName.value = it; dataEdited.value = true }
                        ) {
                            Spacer(modifier = Modifier.size(16.dp))
                            NumberField(
                                number = seriesNb,
                            ) { seriesNb.intValue = it; dataEdited.value = true }
                        }
                    }

                    // rating (index 4)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.title_rating),
                                style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                RatingStars(
                                    rating = rating.doubleValue,
                                    starSize = 48.dp,
                                    starSpacing = 0.dp,
                                    emptyStarColor = MaterialTheme.colorScheme.outline
                                ) { newRating ->
                                    rating.doubleValue = newRating
                                    dataEdited.value = true
                                }
                            }
                        }
                    }

                    // genre (index 5)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_genre),
                            value = genre.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.tag,
                            maxLength = 30,
                            showSuggestions = true,
                            suggestions = { bookVM.filterGenres(it, allGenres) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(5, topPaddingPx) }
                                }
                            },
                            onValueChange = { genre.value = it; dataEdited.value = true }
                        )
                    }

                    // isbn (index 6)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_isbn),
                            value = isbn.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.barcode,
                            maxLength = 13,
                            showSuggestions = false,
                            suggestions = { emptyList() },
                            onFocusEvent = {},
                            onValueChange = { isbn.value = it; dataEdited.value = true }
                        )
                    }

                    // publisher (index 7)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_publishing),
                            value = publisher.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.house,
                            maxLength = 40,
                            showSuggestions = true,
                            suggestions = { bookVM.filterPublishers(it, allPublishers) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(7, topPaddingPx) }
                                }
                            },
                            onValueChange = { publisher.value = it; dataEdited.value = true }
                        )
                    }

                    // published date (index 8)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.title_publishedDate),
                                style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            )
                            DateInput(
                                date = pubDate.longValue,
                                onClear = { pubDate.longValue = 0L; dataEdited.value = true }
                            ) {
                                activeDateField = PUB_DATE
                                datePickerVisible.value = true
                            }
                        }
                    }

                    // language (index 9)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_language),
                            value = language.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.language,
                            maxLength = 15,
                            showSuggestions = true,
                            suggestions = { bookVM.filterLanguages(it, allLanguages) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(9, topPaddingPx) }
                                }
                            },
                            onValueChange = { language.value = it; dataEdited.value = true }
                        )
                    }

                    // format (index 10)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_format),
                            value = format.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.closed_book,
                            maxLength = 15,
                            showSuggestions = true,
                            suggestions = { bookVM.filterFormats(it, allFormats) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(10, topPaddingPx) }
                                }
                            },
                            onValueChange = { format.value = it; dataEdited.value = true }
                        )
                    }

                    // source (index 11)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.title_source),
                                style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ToggleBox(
                                    isRadio = false,
                                    boxHeight = 20.dp,
                                    rowPadding = PaddingValues(),
                                    rowSpacing = 8.dp,
                                    optionText = stringResource(R.string.txt_isGift),
                                    textStyle = MyTypography.bodyLarge,
                                    isToggled = isGift.value
                                ) { isGift.value = !isGift.value; dataEdited.value = true }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                modifier = Modifier.padding(start = 16.dp),
                                text = if (isGift.value) stringResource(R.string.txt_giftFrom)
                                        else stringResource(R.string.txt_boughtAt),
                                style = MyTypography.bodyLarge
                            )
                            Row(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                InputField(
                                    value = source.value,
                                    icon = if (isGift.value) R.drawable.gift else R.drawable.cart,
                                    width = FULL_LENGTH,
                                    maxLength = 30,
                                    singleLine = true,
                                    canExpand = true,
                                    suggestions = {
                                        if (isGift.value) {
                                            bookVM.filterGivers(it, allGivers)
                                        } else {
                                            bookVM.filterBoughtSources(it, allBought)
                                        }
                                    },
                                    onFocusEvent = { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch { lazyListState.animateScrollToItem(11, topPaddingPx) }
                                        }
                                    },
                                    onValueChange = { format.value = it; dataEdited.value = true }
                                )
                            }
                        }
                    }

                    // bookshelf (index 12)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_bookshelf),
                            value = bookshelf.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.bookshelf,
                            maxLength = 30,
                            showSuggestions = true,
                            suggestions = { bookVM.filterBookshelves(it, allBookshelves) },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(12, topPaddingPx) }
                                }
                            },
                            onValueChange = { bookshelf.value = it; dataEdited.value = true }
                        )
                    }

                    // read status and dates (index 13)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.title_readingStatus),
                                style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ToggleBox(
                                    isRadio = false,
                                    boxHeight = 20.dp,
                                    rowPadding = PaddingValues(),
                                    rowSpacing = 8.dp,
                                    optionText = stringResource(R.string.txt_bookRead),
                                    textStyle = MyTypography.bodyLarge,
                                    isToggled = read.value
                                ) { read.value = !read.value; dataEdited.value = true }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                modifier = Modifier.padding(start = 16.dp),
                                text = stringResource(R.string.txt_startReading),
                                style = MyTypography.bodyLarge
                            )
                            Row {
                                DateInput(
                                    date = startDate.longValue,
                                    onClear = {
                                        startDate.longValue = 0L
                                        dataEdited.value = true
                                    }
                                ) {
                                    activeDateField = START_DATE
                                    datePickerVisible.value = true
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                modifier = Modifier.padding(start = 16.dp),
                                text = stringResource(R.string.txt_finishReading),
                                style = MyTypography.bodyLarge
                            )
                            Row {
                                DateInput(
                                    date = finishDate.longValue,
                                    onClear = {
                                        finishDate.longValue = 0L
                                        dataEdited.value = true
                                    }
                                ) {
                                    activeDateField = FINISH_DATE
                                    datePickerVisible.value = true
                                }
                            }
                        }
                    }

                    // description (index 14)
                    item {
                        SingleInputField(
                            title = stringResource(R.string.title_description),
                            value = description.value,
                            fieldWidth = FULL_LENGTH,
                            icon = R.drawable.quill_ink,
                            maxLength = 1000,
                            singleLine = false,
                            showSuggestions = false,
                            suggestions = { emptyList() },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { lazyListState.animateScrollToItem(14, topPaddingPx) }
                                }
                            },
                            onValueChange = { description.value = it; dataEdited.value = true }
                        )
                    }
                }

                CustomDatePicker(
                    context = context,
                    themeChoice = themeChoice,
                    visible = datePickerVisible,
                    dateMillis = when (activeDateField) {
                        PUB_DATE -> pubDate.longValue
                        START_DATE -> startDate.longValue
                        FINISH_DATE -> finishDate.longValue
                        else -> 0L
                    },
                    onDateSelected = { millis ->
                        when (activeDateField) {
                            PUB_DATE -> pubDate.longValue = millis
                            START_DATE -> startDate.longValue = millis
                            FINISH_DATE -> finishDate.longValue = millis
                        }
                        dataEdited.value = true
                    }
                )

                if (cancelVisible.value) {
                    CustomContentDialogWindow(
                        visible = cancelVisible,
                        content = {
                            Text(
                                modifier = Modifier.padding(bottom = 8.dp),
                                text = warningText,
                                style = MyTypography.bodyLarge.copy(textAlign = TextAlign.Center)
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
                        leftButtonOnClick = { cancelVisible.value = false },
                        rightButtonContent = {
                            Text(
                                text = stringResource(R.string.button_leave),
                                style = MyTypography.bodyLarge,
                                color = ValidGreen
                            )
                        },
                        rightButtonOnClick = {
                            cancelVisible.value = false
                            onGoBack()
                        }
                    )
                }
            }
        }
    )
}

/**
 * Section title and input field for a simple, single-line field.
 *
 * @param title of the section
 * @param value inside the input field
 * @param fieldWidth max width of the input field
 * @param icon to display at the beginning of the input field
 * @param maxLength max characters that can be entered in this input field
 * @param singleLine whether the input field is single line or multi line (default: true)
 * @param showSuggestions whether to show suggestions when typing
 * @param suggestions function that provides a list of suggestions based on the current input
 * @param onFocusEvent callback for focus events on the input field
 * @param onValueChange callback for when the input field value changes
 * @param extraActions optional content to display at the end of the input field
 */
@Composable
private fun SingleInputField(
    title: String,
    value: String,
    fieldWidth: Int,
    icon: Int,
    maxLength: Int,
    singleLine: Boolean = true,
    showSuggestions: Boolean,
    suggestions: ((String) -> List<String>),
    onFocusEvent: (FocusState) -> Unit,
    onValueChange: (String) -> Unit,
    extraActions: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start))
        Row(modifier = Modifier.padding(start = 16.dp)
        ) {
            InputField(
                value, icon, fieldWidth, maxLength, singleLine, showSuggestions,
                { suggestions(it) },
                { onFocusEvent(it) }
            ) { onValueChange(it) }
            if (extraActions != null) extraActions()
        }
    }
}

/**
 * Section title and list of input fields. The user can decide how many input fields to add for this section.
 *
 * @param title of the section
 * @param listValues list of values inside each input field
 * @param icon to display at the beginning of the input field
 * @param suggestions function that provides a list of suggestions based on the current input
 * @param onFocusEvent callback for focus events on the input field
 * @param onValueChange callback for when the input field value changes
 */
@Composable
private fun AuthorsListInputFields(
    title: String,
    listValues: MutableList<String>,
    icon: Int,
    suggestions: ((String) -> List<String>),
    onFocusEvent: (FocusState) -> Unit,
    onValueChange: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MyTypography.titleSmall.copy(textAlign = TextAlign.Start))
        listValues.forEachIndexed { index, value ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InputField(
                    value, icon, 250, 30, singleLine = true, true,
                    { suggestions(it) },
                    { onFocusEvent(it) }
                ) { onValueChange(it, index) }
                // bin icon to delete an author
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { listValues.remove(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.bin),
                        contentDescription = stringResource(R.string.desc_deleteButton)
                    )
                }
            }
        }
        // plus button to add an author
        IconButton(
            modifier = Modifier.padding(start = 8.dp),
            onClick = { listValues.add("") }
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                painter = painterResource(R.drawable.add),
                contentDescription = stringResource(R.string.desc_addButton)
            )
        }
    }
}

/**
 * Input field specifically designed to edit book information.
 *
 * @param value inside the input field
 * @param icon to display at the beginning of the input field
 * @param width of the input field
 * @param maxLength max characters that can be entered in this input field
 * @param singleLine whether the input field is single line or multi line
 * @param canExpand whether or not suggestions should be shown when typing
 * @param suggestions function that provides a list of suggestions based on the current input
 * @param onFocusEvent callback for focus events on the input field
 * @param onValueChange callback for when the input field value changes
 */
@Composable
private fun InputField(
    value: String,
    icon: Int,
    width: Int,
    maxLength: Int,
    singleLine: Boolean,
    canExpand: Boolean,
    suggestions: ((String) -> List<String>),
    onFocusEvent: (FocusState) -> Unit,
    onValueChange: (String) -> Unit
) {
    val showMaxChar = remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false) }
    val suggestions by remember(value) { mutableStateOf(suggestions(value)) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length)))}
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    var userTyping by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.width(width.dp))
    {
        TextField(
            modifier = if (singleLine) Modifier
                .padding(0.dp)
                .onFocusEvent { onFocusEvent(it) }
            else Modifier
                .padding(0.dp)
                .height(400.dp)
                .onFocusEvent { onFocusEvent(it) },
            value = textFieldValue,
            onValueChange = {
                if (it.text.length <= maxLength) {
                    textFieldValue = it
                    onValueChange(it.text)
                }
                showMaxChar.value = it.text.length >= maxLength
                if (userTyping) expanded.value = true
            },
            textStyle = MyTypography.bodyLarge,
            prefix = {
                Row{
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = stringResource(R.string.desc_textFieldIcon),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.txt_inputFieldPlaceholder), style = MyTypography.bodySmall)
            },
            singleLine = singleLine,
            supportingText = {
                if (showMaxChar.value) {
                    Text(
                        text = stringResource(R.string.txt_maxChar, maxLength.toString()),
                        style = MyTypography.labelSmall
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (canExpand && expanded.value && suggestions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                LazyColumn {
                    items(suggestions.size) { index ->
                        val suggestion = suggestions[index]
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    userTyping = false
                                    // fill textField with chosen suggestion and jump cursor to end
                                    textFieldValue = TextFieldValue(
                                        suggestion,
                                        TextRange(suggestion.length)
                                    )
                                    onValueChange(suggestion)
                                    expanded.value = false

                                    // reset userTyping flag (to avoid having the suggestion re-triggering expanded = true)
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        userTyping = true
                                    }
                                }
                                .padding(8.dp),
                            text = suggestion,
                            style = MyTypography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Input field for numbers only. A "#" prefix is displayed before the number.
 *
 * @param number current number inside the input field
 * @param onValueChange callback for when the input field value changes
 */
@Suppress("UNUSED_PARAMETER")
@Composable
private fun NumberField(
    number: MutableIntState,
    onValueChange: (Int) -> Unit
) {
    val text = remember(number.intValue) { mutableStateOf(
        if (number.intValue >= 0) number.intValue.toString() else ""
    ) }

    TextField(
        modifier = Modifier
            .width(70.dp)
            .padding(0.dp),
        value = if (text.value.isBlank() || text.value.toInt() < 0) ""
                else text.value,
        onValueChange = { input ->
            if (input.isBlank()) {
                text.value = ""
                onValueChange(-1)
            } else {
                val filteredInput = input.filter { it.isDigit() }
                text.value = filteredInput
                filteredInput.toIntOrNull()?.let { onValueChange(it) }
            }
        },
        textStyle = MyTypography.bodyLarge,
        prefix = { Text(text = "#", style = MyTypography.bodyLarge) },
        singleLine = true,
        supportingText = {},
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
    )
}

/**
 * Input field for selecting a date. The input field itself is not editable, but clicking on it triggers a date picker dialog.
 * There is a suffix button to remove the selected date.
 *
 * @param date current date inside the input field (in milliseconds since epoch)
 * @param onClear function that runs when removing the date
 * @param onClick function that runs when selecting the input field (should open a date picker)
 */
@Composable
private fun DateInput(
    date: Long,
    onClear: () -> Unit,
    onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp)
    ) {
        TextField(
            modifier = Modifier
                .padding(0.dp)
                .clickable { onClick() },
            value = if (date > 0L) displayDate(date, DateFormat.NUMBERED)
                    else "",
            onValueChange = {},
            enabled = false,
            textStyle = MyTypography.bodyLarge,
            leadingIcon = {
                Row{
                    IconButton(onClick = { onClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = stringResource(R.string.desc_textFieldIcon),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                }
            },
            trailingIcon = {
                if (date > 0L) {
                    IconButton(onClick = { onClear() }) {
                        Icon(
                            painter = painterResource(R.drawable.cancel),
                            contentDescription = stringResource(R.string.desc_clearDate),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.field_date), style = MyTypography.bodySmall)
            },
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.Transparent,
                disabledIndicatorColor = MaterialTheme.colorScheme.inversePrimary,
                disabledTextColor = MaterialTheme.colorScheme.inversePrimary,
                disabledLeadingIconColor = MaterialTheme.colorScheme.inversePrimary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.inversePrimary
            )
        )
    }
}