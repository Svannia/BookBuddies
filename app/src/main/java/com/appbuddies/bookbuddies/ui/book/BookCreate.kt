package com.appbuddies.bookbuddies.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.Book
import com.appbuddies.bookbuddies.helpers.searchByISBN
import com.appbuddies.bookbuddies.errors.handleError
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.ui.LoadingPage
import com.appbuddies.bookbuddies.viewModels.BookViewModel
import com.appbuddies.bookbuddies.viewModels.DataViewModel

@Composable
fun BookCreate(isbn: String?, bookVM: BookViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val themeChoice by dataVM.currentTheme.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var newBook by remember { mutableStateOf<Book?>(null) }

    val noBookToast = stringResource(R.string.toast_noBookWithISBN)
    LaunchedEffect(isbn) {
        if (!isbn.isNullOrBlank()) {
            isLoading = true

            searchByISBN(context, isbn, isError =  {
                if (it) {
                    newBook = Book.empty().copy(isbn = isbn)
                    isLoading = false
                    handleError(context, noBookToast)
                }
            }) {
                newBook = it
                isLoading = false
            }
        } else {
            newBook = null
        }
    }

    if (isLoading) {
        LoadingPage()
    } else {
        EditShared(
            context = context,
            navigationActions = navigationActions,
            screenTitle = stringResource(R.string.title_bookCreate),
            warningText = stringResource(R.string.txt_createBookLeave),
            onGoBack = { navigationActions.goBack() },
            themeChoice = themeChoice,
            bookVM = bookVM,
            book = newBook,
            alwaysPopupOnLeave = newBook != null
        )
    }
}
