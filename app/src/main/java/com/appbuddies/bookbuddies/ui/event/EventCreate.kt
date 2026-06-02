package com.appbuddies.bookbuddies.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.CalendarEvent
import com.appbuddies.bookbuddies.navigation.NavigationActions
import com.appbuddies.bookbuddies.viewModels.CalendarViewModel
import com.appbuddies.bookbuddies.viewModels.DataViewModel

@Composable
fun EventCreate(dateStart: Long, calendarVM: CalendarViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val themeChoice by dataVM.currentTheme.collectAsState()

    val event = CalendarEvent.empty(dateStart)

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
