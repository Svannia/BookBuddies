package com.example.bookbuddies.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.viewModels.DataViewModel

@Composable
fun HomeScreen(dataVM: DataViewModel, navigationActions: NavigationActions) {

    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = "My Books"
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

        }
    }
}