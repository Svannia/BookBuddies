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
import com.example.bookbuddies.viewModels.DataViewModel

@Composable
fun BookEdit(bookID: String, bookVM: BookViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val book by bookVM.getBookFlowById(bookID).collectAsState(initial = Book.empty())
    val themeChoice by dataVM.currentTheme.collectAsState()

    val editBookTitle = stringResource(R.string.title_editBook)
    EditShared(
        context = context,
        navigationActions = navigationActions,
        screenTitle = editBookTitle,
        warningText = stringResource(R.string.txt_editLeave),
        onGoBack = { navigationActions.goBack() },
        themeChoice = themeChoice,
        bookVM = bookVM,
        book = book
    )
}