package com.appbuddies.bookbuddies.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.Book
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.viewModels.BookViewModel
import com.appbuddies.bookbuddies.viewModels.DataViewModel

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
