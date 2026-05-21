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
import timber.log.Timber

@Composable
fun EventEdit(eventID: String, calendarVM: CalendarViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val event by calendarVM.getEventFlowById(eventID).collectAsState(initial = CalendarEvent.empty())
    val themeChoice by dataVM.currentTheme.collectAsState()

    Timber.tag("Debug").d("Editing event with timezone ${event.timezone}")
    val editEventTitle = stringResource(R.string.title_editEvent)
    EventShared(
        context = context,
        navigationActions = navigationActions,
        screenTitle = editEventTitle,
        warningText = stringResource(R.string.txt_editLeave),
        onGoBack = { navigationActions.goBack() },
        themeChoice = themeChoice,
        calendarVM = calendarVM,
        event = event,
    )

}