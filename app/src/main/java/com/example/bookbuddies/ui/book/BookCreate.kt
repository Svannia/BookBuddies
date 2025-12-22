package com.example.bookbuddies.ui.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.searchByISBN
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.ui.LoadingPage
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.DataViewModel

@Composable
fun BookCreate(isbn: String?, bookVM: BookViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val themeChoice by dataVM.currentTheme.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var newBook by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(isbn) {
        if (!isbn.isNullOrBlank()) {
            isLoading = true

            searchByISBN(context, isbn, isError =  {
                if (it) {
                    newBook = null
                    isLoading = false
                    handleError(context, "Could not find book with ISBN: $isbn")
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
            warningText = stringResource(R.string.txt_createLeave),
            onGoBack = { navigationActions.goBack() },
            themeChoice = themeChoice,
            bookVM = bookVM,
            book = newBook,
            alwaysPopupOnLeave = newBook != null
        )
    }
}