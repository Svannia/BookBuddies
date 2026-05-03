package com.example.bookbuddies.ui.event

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.bookbuddies.data.CalendarEvent

@Composable
fun EventShared(
    event: CalendarEvent? = null,
    onGoBack: () -> Unit,
) {
    val cancelVisible = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }

    BackHandler {
        if (dataEdited.value) cancelVisible.value = true
        else onGoBack()
    }

    val title = remember { mutableStateOf(event?.title ?: "") }
    val allDay = remember { mutableStateOf(event?.allDay ?: false) }
    val dateStart = remember { mutableLongStateOf(event?.dateStart ?: 0L) }
    val dateEnd = remember { mutableLongStateOf(event?.dateEnd ?: 0L) }
    val minuteStart = remember { mutableIntStateOf(event?.minuteStart ?: 0) }
    val minuteEnd = remember { mutableIntStateOf(event?.minuteEnd ?: 0) }
    val location = remember { mutableStateOf(event?.location ?: "") }
    val notes = remember { mutableStateOf(event?.notes ?: "") }
    val tag = remember { mutableStateOf(event?.tag ?: "")}
    val reminder = remember { mutableStateOf(event?.reminder ?: false) }
    val reminderTime = remember { mutableLongStateOf(event?.reminderTime ?: 0L) }

    LaunchedEffect(event) {
        if (event != null) {
            title.value = event.title
            allDay.value = event.allDay
            dateStart.longValue = event.dateStart
            dateEnd.longValue = event.dateEnd
            minuteStart.intValue = event.minuteStart
            minuteEnd.intValue = event.minuteEnd
            location.value = event.location
            notes.value = event.notes
            tag.value = event.tag
            reminder.value = event.reminder
            reminderTime.longValue = event.reminderTime
        }
    }
}