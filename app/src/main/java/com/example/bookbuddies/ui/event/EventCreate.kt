package com.example.bookbuddies.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.viewModels.CalendarViewModel
import com.example.bookbuddies.viewModels.DataViewModel

@Composable
fun EventCreate(calendarVM: CalendarViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val themeChoice by dataVM.currentTheme.collectAsState()

    val event = CalendarEvent.empty()

    EventShared(
        context = context,
        navigationActions = navigationActions,
        screenTitle = stringResource(R.string.title_eventCreate),
        warningText = stringResource(R.string.txt_createEventLeave),
        onGoBack = { navigationActions.goBack() },
        themeChoice = themeChoice,
        calendarVM = calendarVM,
        event = event,
    )
}