package com.example.bookbuddies.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.viewModels.BookViewModel

@Composable
fun BookEdit(bookID: String, bookVM: BookViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val book by bookVM.getBookFlowById(bookID).collectAsState(initial = Book.empty())

    EditShared(
        context = context,
        screenTitle = context.getString(R.string.title_editBook),
        warningText = stringResource(R.string.txt_editLeave),
        onGoBack = { navigationActions.goBack() },
        book = book
    )


}