package com.example.bookbuddies.ui.event

import androidx.compose.runtime.Composable
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.viewModels.CalendarViewModel

@Composable
fun EventCreate(calendarVM: CalendarViewModel, navigationActions: NavigationActions) {
    EventShared(
        event = null,
        onGoBack = { navigationActions.goBack() },
    )
}