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
import java.util.Calendar

@Composable
fun EventEdit(eventID: String, calendarVM: CalendarViewModel, dataVM: DataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val event by calendarVM.getEventFlowById(eventID).collectAsState(initial = CalendarEvent.empty(today))
    val themeChoice by dataVM.currentTheme.collectAsState()

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
