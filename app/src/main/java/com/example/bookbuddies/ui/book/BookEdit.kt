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
import com.example.bookbuddies.viewModels.BookViewModel

@Composable
fun BookEdit(bookID: String, bookVM: BookViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val book by bookVM.getBookFlowById(bookID).collectAsState(initial = Book.empty())

    val cover = remember { mutableStateOf(book.cover) }
    val tempCover = remember { mutableStateOf(Uri.EMPTY) }

    LaunchedEffect(book) {
        cover.value = book.cover
    }



    val cancelVisible = remember { mutableStateOf(false) }
    EditShared(
        context = context,
        screenTitle = context.getString(R.string.title_editBook),
        warningText = stringResource(R.string.txt_editLeave),
        onGoBack = { navigationActions.goBack() },
        book = book
    )


}