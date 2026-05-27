package com.example.bookbuddies.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.getTagForEvent
import com.example.bookbuddies.helpers.displayDate
import com.example.bookbuddies.helpers.displayTimezoneConversion
import com.example.bookbuddies.helpers.formatMinutes
import com.example.bookbuddies.helpers.formatReminderTime
import com.example.bookbuddies.helpers.getLocalTimezone
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.SecondaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.CalendarViewModel
import java.util.Calendar

@Composable
fun EventView(eventID: String, calendarVM: CalendarViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val event by calendarVM.getEventFlowById(eventID).collectAsState(initial = CalendarEvent.empty(today))
    val allTags by calendarVM.allEventTags.collectAsState(emptyList())

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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // event title + tag
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = event.title,
                        style = MyTypography.titleSmall
                    )
                    // tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tag = getTagForEvent(context, event.tag, allTags)
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.dot),
                            contentDescription = stringResource(R.string.desc_iconTag),
                            tint = Color.Companion.fromColorLong(tag.colour)
                        )
                        Text(text = tag.name, style = MyTypography.bodyLarge)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // time and date
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.clock),
                        contentDescription = stringResource(R.string.desc_time),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    // inner column for all time info
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // inner row for time and date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = if (event.dateEnd > 0L) Arrangement.SpaceBetween else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // date start
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = displayDate(event.dateStart, DateFormat.SHORT_DAY_DATE),
                                    style = MyTypography.bodyLarge
                                )
                                // time start
                                if (!event.allDay) {
                                    Text(
                                        text = formatMinutes(event.minuteStart),
                                        style = MyTypography.bodyLarge
                                    )
                                }
                            }

                            // arrow then date end if applicable
                            if (event.dateEnd > 0L) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.arrow_right),
                                    contentDescription = stringResource(R.string.desc_rightArrow),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                // date end
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = displayDate(event.dateEnd, DateFormat.SHORT_DAY_DATE),
                                        style = MyTypography.bodyLarge
                                    )
                                    // time start
                                    if (!event.allDay) {
                                        Text(
                                            text = formatMinutes(event.minuteEnd),
                                            style = MyTypography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }

                        // timezone conversion if different from local
                        if (event.timezone != getLocalTimezone().label) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "${stringResource(R.string.title_timezone)}: ",
                                    style = MyTypography.bodyLarge
                                )
                                Text(
                                    text = event.timezone,
                                    style = MyTypography.bodyLarge
                                )
                            }
                            Text(
                                text = displayTimezoneConversion(context, event.timezone, event.dateStart, event.minuteStart, event.dateEnd, event.minuteEnd),
                                style = MyTypography.bodyLarge
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // reminder
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.notification),
                        contentDescription = stringResource(R.string.desc_reminderIcon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatReminderTime(context, event.reminder, event.reminderTime),
                        style = MyTypography.bodyLarge
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // location
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.location),
                        contentDescription = stringResource(R.string.desc_locationIcon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.location,
                        style = MyTypography.bodyLarge
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // notes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.note),
                        contentDescription = stringResource(R.string.desc_notesIcon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.notes,
                        style = MyTypography.bodyLarge
                    )
                }
            }
        }

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
                    calendarVM.deleteEvent(context, event)
                    showDeleteDialog.value = false
                    navigationActions.navigateTo(Route.CALENDAR, true)
                }
            )
        }
    }
}