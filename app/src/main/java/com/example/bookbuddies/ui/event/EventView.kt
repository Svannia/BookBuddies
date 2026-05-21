package com.example.bookbuddies.ui.event

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.errors.handleError
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.SecondaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.CalendarViewModel

@Composable
fun EventView(eventID: String, calendarVM: CalendarViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val event by calendarVM.getEventFlowById(eventID).collectAsState(initial = CalendarEvent.empty())


    val showDeleteDialog = remember { mutableStateOf(false) }

    SecondaryScreen(
        title = "",
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // edit the event
                IconButton(
                    onClick = { navigationActions.navigateTo("${Route.EVENT_EDIT}/${event.uid}") }
                ) {
                    Icon(
                        painterResource(R.drawable.edit),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_edit)
                    )
                }
                // delete the event
                IconButton(
                    onClick = { showDeleteDialog.value = true }
                ) {
                    Icon(
                        painterResource(R.drawable.bin),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_deleteButton)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {  }

        // confirm dialog to delete the event
        if (showDeleteDialog.value) {
            CustomContentDialogWindow(
                visible = showDeleteDialog,
                content = {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringResource(R.string.txt_deleteEventConfirm),
                        style = MyTypography.bodyLarge
                    )
                },
                bottomButtons = true,
                leftButtonContent = {
                    Text(
                        text = stringResource(R.string.button_cancel),
                        style = MyTypography.bodyLarge,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                },
                leftButtonOnClick = { showDeleteDialog.value = false },
                rightButtonContent = {
                    Text(
                        text = stringResource(R.string.button_confirm),
                        style = MyTypography.bodyLarge,
                        color = ValidGreen
                    )
                },
                rightButtonOnClick = {
                    calendarVM.deleteEvent(event)
                    showDeleteDialog.value = false
                    navigationActions.navigateTo(Route.CALENDAR, true)
                }
            )
        }
    }
}