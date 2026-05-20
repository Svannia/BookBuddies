package com.example.bookbuddies.ui.event

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.EventTag
import com.example.bookbuddies.data.TAG_COLOURS
import com.example.bookbuddies.data.getTagForEvent
import com.example.bookbuddies.datastore.ThemeChoice
import com.example.bookbuddies.helpers.TIMEZONES
import com.example.bookbuddies.helpers.Timezone
import com.example.bookbuddies.helpers.convertToLocal
import com.example.bookbuddies.helpers.displayDate
import com.example.bookbuddies.helpers.formatMinutes
import com.example.bookbuddies.helpers.formatReminderTime
import com.example.bookbuddies.helpers.getAvailableTagColours
import com.example.bookbuddies.helpers.getLocalTimezone
import com.example.bookbuddies.helpers.getTimezoneOffset
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.CustomDatePicker
import com.example.bookbuddies.ui.CustomTextField
import com.example.bookbuddies.ui.MiniLoading
import com.example.bookbuddies.ui.ToggleOptions
import com.example.bookbuddies.ui.WheelPicker
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.CalendarViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private const val FULL_LENGTH = 300
private const val HEIGHT = 52
private const val OFFSET = 45
private const val START_DATE = "start"
private const val END_DATE = "finish"
// max characters per field
private const val TITLE_MAX = 70
private const val LOCATION_MAX = 100
private const val NOTES_MAX = 500
private const val TAG_MAX = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventShared(
    context: Context,
    navigationActions: NavigationActions,
    screenTitle: String,
    warningText: String,
    onGoBack: () -> Unit,
    themeChoice: ThemeChoice,
    calendarVM: CalendarViewModel,
    event: CalendarEvent,
) {
    val loading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val cancelVisible = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }

    val allTags by calendarVM.allEventTags.collectAsState(emptyList())

    BackHandler {
        if (dataEdited.value) cancelVisible.value = true
        else onGoBack()
    }

    val title = remember { mutableStateOf((event.title).take(TITLE_MAX)) }
    val allDay = remember { mutableStateOf(event.allDay) }
    val timezone = remember { mutableStateOf(event.timezone) }
    val dateStart = remember { mutableLongStateOf(event.dateStart) }
    val dateEnd = remember { mutableLongStateOf(event.dateEnd) }
    val minuteStart = remember { mutableIntStateOf(event.minuteStart) }
    val minuteEnd = remember { mutableIntStateOf(event.minuteEnd) }
    val location = remember { mutableStateOf((event.location).take(LOCATION_MAX)) }
    val notes = remember { mutableStateOf((event.notes).take(NOTES_MAX)) }
    val tagID = remember { mutableStateOf(event.tag) }
    val chosenTag = remember { mutableStateOf(getTagForEvent(context, tagID.value, allTags)) }
    val reminder = remember { mutableStateOf(event.reminder) }
    val reminderTime = remember { mutableLongStateOf(event.reminderTime) }

    LaunchedEffect(event) {
        title.value = event.title.take(TITLE_MAX)
        allDay.value = event.allDay
        timezone.value = event.timezone
        dateStart.longValue = event.dateStart
        dateEnd.longValue = event.dateEnd
        minuteStart.intValue = event.minuteStart
        minuteEnd.intValue = event.minuteEnd
        location.value = event.location.take(LOCATION_MAX)
        notes.value = event.notes.take(NOTES_MAX)
        tagID.value = event.tag
        chosenTag.value = getTagForEvent(context, tagID.value, allTags)
        reminder.value = event.reminder
        reminderTime.longValue = event.reminderTime
    }

    // for picking a date
    var activeDateField by remember { mutableStateOf<String?>(null) }
    val datePickerVisible = remember { mutableStateOf(false) }
    val localTimezone = remember { mutableStateOf(getLocalTimezone()) }

    // other state variables
    val showReminderPicker = remember { mutableStateOf(false) }
    val showTimezoneDropdown = remember { mutableStateOf(false) }
    val showTagsDropdown = remember { mutableStateOf(false) }
    val showEditTagWindow = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    var tagToDelete = null as EventTag?
    var tagToEdit = null as EventTag?

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = { Text(text = screenTitle, style = MyTypography.titleMedium) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (dataEdited.value) cancelVisible.value = true
                                else onGoBack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.go_back),
                                contentDescription = stringResource(R.string.desc_goBack)
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(65.dp)
                    .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                tonalElevation = 0.dp,
                containerColor = Color.Transparent
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // check dates validity
                        if (dateEnd.longValue > 0L && dateEnd.longValue < dateStart.longValue) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_wrongDateFinished),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        else if (!allDay.value && dateEnd.longValue == dateStart.longValue && minuteEnd.intValue < minuteStart.intValue) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_wrongMinuteFinished),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        loading.value = true

                        val updatedEvent = event.copy(
                            title = title.value,
                            allDay = allDay.value,
                            dateStart = dateStart.longValue,
                            dateEnd = dateEnd.longValue,
                            minuteStart = minuteStart.intValue,
                            minuteEnd = minuteEnd.intValue,
                            location = location.value,
                            notes = notes.value,
                            tag = tagID.value,
                            reminder = reminder.value,
                            reminderTime = reminderTime.longValue
                        )
                        scope.launch {
                            calendarVM.insertEvent(updatedEvent)
                            navigationActions.navigateTo("${Route.EVENT}/${updatedEvent.uid}", clearPrevious = true)
                        }
                    },
                    enabled = dataEdited.value && title.value.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.button_save),
                        style = MyTypography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        content = { paddingValues ->
            val topPaddingPx = with(LocalDensity.current) {
                40.dp.toPx().roundToInt()
            }

            if (loading.value) MiniLoading(paddingValues)
            else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // title (index 0)
                    item {
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            CustomTextField(
                                value = title.value,
                                onValueChange = { title.value = it; dataEdited.value = true },
                                icon = R.drawable.calendar,
                                iconColour = MaterialTheme.colorScheme.primary,
                                placeHolder = stringResource(R.string.title_eventName),
                                singleLine = true,
                                maxLength = TITLE_MAX,
                                width = FULL_LENGTH.dp
                            )
                        }
                    }

                    // tag (index 1)
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .height(HEIGHT.dp)
                                    .clickable { showTagsDropdown.value = true },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.dot),
                                    contentDescription = stringResource(R.string.desc_iconTag),
                                    tint = Color.Companion.fromColorLong(chosenTag.value.colour)
                                )
                                Text(text = chosenTag.value.name, style = MyTypography.bodyLarge)
                            }

                            // dropdown menu for tags
                            DropdownMenu(
                                expanded = showTagsDropdown.value,
                                onDismissRequest = { showTagsDropdown.value = false }
                            ) {
                                allTags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // tag colour and name
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(20.dp),
                                                        painter = painterResource(R.drawable.dot),
                                                        contentDescription = stringResource(R.string.desc_iconTag),
                                                        tint = Color.Companion.fromColorLong(tag.colour)
                                                    )
                                                    Text(
                                                        text = tag.name,
                                                        style = MyTypography.bodyLarge
                                                    )
                                                }
                                                // icons to edit and delete tag if not default
                                                if (!tag.isDefault) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                                    ) {
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        IconButton(
                                                            modifier = Modifier.size(20.dp),
                                                            onClick = {
                                                                tagToEdit = tag
                                                                showEditTagWindow.value = true
                                                                showTagsDropdown.value = false
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.edit),
                                                                contentDescription = stringResource(R.string.desc_edit)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            modifier = Modifier.size(20.dp),
                                                            onClick = {
                                                                tagToDelete = tag
                                                                showDeleteDialog.value = true
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.cancel),
                                                                contentDescription = stringResource(R.string.desc_deleteButton)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            tagID.value = tag.uid
                                            chosenTag.value = tag
                                            showTagsDropdown.value = false
                                            dataEdited.value = true
                                        },
                                    )
                                }
                                // last menu item to add a new tag
                                DropdownMenuItem(
                                    text = {
                                        // tag colour and name
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(20.dp),
                                                painter = painterResource(R.drawable.add),
                                                contentDescription = stringResource(R.string.desc_addButton)
                                            )
                                            Text(
                                                text = stringResource(R.string.title_newTag),
                                                style = MyTypography.bodyLarge
                                            )
                                        }
                                    },
                                    onClick = {
                                        showEditTagWindow.value = true
                                        showTagsDropdown.value = false
                                    },
                                )
                            }
                        }
                    }

                    // all day toggle + dates pickers (index 2)
                    item {
                        val showStartTimePicker = remember { mutableStateOf(false) }
                        val showEndTimePicker = remember { mutableStateOf(false) }
                        val hasEndTime = remember { mutableStateOf(minuteEnd.intValue > 0 || dateEnd.longValue > 0L) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // all day toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // all day: icon and field name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(R.drawable.clock),
                                        contentDescription = stringResource(R.string.desc_time),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.title_allDay),
                                        style = MyTypography.bodyLarge,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                                Switch(
                                    checked = allDay.value,
                                    onCheckedChange = {
                                        allDay.value = it
                                        dataEdited.value = true
                                        showStartTimePicker.value = false
                                        showEndTimePicker.value = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onBackground,
                                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                                        checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    )
                                )
                            }

                            // dates + times
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // left column with start date and time
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // start date
                                    Text(
                                        modifier = Modifier
                                            .widthIn(max = 90.dp)
                                            .clickable {
                                                activeDateField = START_DATE
                                                datePickerVisible.value = true
                                            },
                                        text = displayDate(
                                            dateStart.longValue,
                                            DateFormat.SHORT_DAY_DATE
                                        ),
                                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center)
                                    )

                                    // only show start time if all day not toggled
                                    if (!allDay.value) {
                                        Box(
                                            modifier = Modifier.clickable {
                                                showStartTimePicker.value =
                                                    !showStartTimePicker.value
                                                showEndTimePicker.value = false
                                            }
                                        ) {
                                            Text(
                                                text = formatMinutes(minuteStart.intValue),
                                                style = MyTypography.bodyLarge.copy(fontWeight = if (showStartTimePicker.value) FontWeight.Bold else FontWeight.Normal),
                                                color = if (showStartTimePicker.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }

                                // icon between start and end times
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 4.dp),
                                    painter = painterResource(R.drawable.arrow_right),
                                    contentDescription = stringResource(R.string.desc_rightArrow),
                                    tint = MaterialTheme.colorScheme.outline
                                )

                                // only show end date and time column if there is one
                                if (hasEndTime.value) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // right column with end date and time
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // end date
                                            Text(
                                                modifier = Modifier
                                                    .widthIn(max = 90.dp)
                                                    .clickable {
                                                        activeDateField = END_DATE
                                                        datePickerVisible.value = true
                                                    },
                                                text = displayDate(
                                                    dateEnd.longValue,
                                                    DateFormat.SHORT_DAY_DATE
                                                ),
                                                style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center)
                                            )

                                            // only show end time if all day not toggled
                                            if (!allDay.value) {
                                                Box(
                                                    modifier = Modifier
                                                        .clickable {
                                                            showEndTimePicker.value = !showEndTimePicker.value
                                                            showStartTimePicker.value = false
                                                        }
                                                ) {
                                                    Text(
                                                        text = formatMinutes(minuteEnd.intValue),
                                                        style = MyTypography.bodyLarge.copy(
                                                            fontWeight = if (showEndTimePicker.value) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (showEndTimePicker.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        }

                                        // x button to remove the end time
                                        Icon(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    hasEndTime.value = false
                                                    dateEnd.longValue = 0L
                                                    minuteEnd.intValue = 0
                                                    showEndTimePicker.value = false
                                                    dataEdited.value = true
                                                },
                                            painter = painterResource(R.drawable.cancel),
                                            contentDescription = stringResource(R.string.desc_cancel),
                                        )
                                    }
                                } else {
                                    // if there is no end time, just show button to add one
                                    Text(
                                        modifier = Modifier.clickable {
                                            hasEndTime.value = true
                                            dateEnd.longValue = dateStart.longValue
                                            minuteEnd.intValue = minuteStart.intValue + 60
                                            dataEdited.value = true
                                        },
                                        text = stringResource(R.string.button_endTime),
                                        style = MyTypography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    )
                                }
                            }

                            // timezone
                            if (!allDay.value) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // timezone title with button
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.title_timezone),
                                                style = MyTypography.bodyLarge
                                            )

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { showTimezoneDropdown.value = true }
                                            ) {
                                                Text(
                                                    modifier = Modifier.padding(8.dp),
                                                    text = getTimezoneOffset(timezone.value),
                                                    style = MyTypography.bodySmall.copy(textAlign = TextAlign.Center)
                                                )
                                            }
                                        }
                                        // list of most common timezones
                                        DropdownMenu(
                                            expanded = showTimezoneDropdown.value,
                                            onDismissRequest = { showTimezoneDropdown.value = false }
                                        ) {
                                            TIMEZONES.forEach { tz ->
                                                DropdownMenuItem(
                                                    text = { Text(text = tz.label, style = MyTypography.bodyMedium) },
                                                    onClick = {
                                                        timezone.value = tz.label
                                                        showTimezoneDropdown.value = false
                                                        dataEdited.value = true
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // if chosen and local timezones differ -> show conversion
                                    if (localTimezone.value.offset != getTimezoneOffset(timezone.value)) {
                                        val selectedTz = TIMEZONES.find { it.label == timezone.value }
                                            ?: Timezone(timezone.value, timezone.value)
                                        val (localDate, localMinutes) = convertToLocal(dateStart.longValue, minuteStart.intValue, selectedTz)
                                        val localTimeStr = formatMinutes(localMinutes)
                                        val localDateStr = displayDate(localDate, DateFormat.SHORT_DAY_DATE)
                                        val sameDay = localDate == dateStart.longValue

                                        Text(
                                            text = if (sameDay) stringResource(
                                                R.string.txt_localTime,
                                                localTimeStr
                                            )
                                            else stringResource(
                                                R.string.txt_localDateTime,
                                                localDateStr,
                                                localTimeStr
                                            ),
                                            style = MyTypography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            // time wheel pickers
                            if (!allDay.value && showStartTimePicker.value) {
                                TimeWheelPicker(
                                    currentMinutes = minuteStart.intValue,
                                    onConfirm = { h, m ->
                                        minuteStart.intValue = h * 60 + m
                                        dataEdited.value = true
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                            if (!allDay.value && showEndTimePicker.value) {
                                TimeWheelPicker(
                                    currentMinutes = minuteEnd.intValue,
                                    onConfirm = { h, m ->
                                        minuteEnd.intValue = h * 60 + m
                                        dataEdited.value = true
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // reminder (index 3)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showReminderPicker.value = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.notification),
                                    contentDescription = stringResource(R.string.desc_reminderIcon),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatReminderTime(context, reminder.value, reminderTime.longValue),
                                    style = MyTypography.bodyLarge,
                                    color = MaterialTheme.colorScheme.inversePrimary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // location (index 4)
                    item {
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            CustomTextField(
                                value = location.value,
                                onValueChange = { location.value = it; dataEdited.value = true },
                                icon = R.drawable.location,
                                iconColour = MaterialTheme.colorScheme.primary,
                                placeHolder = stringResource(R.string.title_location),
                                singleLine = false,
                                maxLength = LOCATION_MAX,
                                width = FULL_LENGTH.dp,
                                height = 110.dp,
                                onFocusedChanged = { focusState ->
                                    if (focusState.isFocused) {
                                        scope.launch { lazyListState.animateScrollToItem(4, topPaddingPx) }
                                    }
                                }
                            )
                        }
                    }

                    // notes (index 5)
                    item {
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            CustomTextField(
                                value = notes.value,
                                onValueChange = { notes.value = it; dataEdited.value = true },
                                icon = R.drawable.note,
                                iconColour = MaterialTheme.colorScheme.primary,
                                placeHolder = stringResource(R.string.title_notes),
                                singleLine = false,
                                maxLength = NOTES_MAX,
                                width = FULL_LENGTH.dp,
                                height = 250.dp,
                                onFocusedChanged = { focusState ->
                                    if (focusState.isFocused) {
                                        scope.launch { lazyListState.animateScrollToItem(5, topPaddingPx) }
                                    }
                                }
                            )
                        }
                    }
                }

                // date picker for start and end dates
                CustomDatePicker(
                    context = context,
                    themeChoice = themeChoice,
                    visible = datePickerVisible,
                    dateMillis = when (activeDateField) {
                        START_DATE -> dateStart.longValue
                        END_DATE -> dateEnd.longValue
                        else -> 0L
                    },
                    onDateSelected = { millis ->
                        when (activeDateField) {
                            START_DATE -> dateStart.longValue = millis
                            END_DATE -> dateEnd.longValue = millis
                        }
                        dataEdited.value = true
                    }
                )

                // reminder picker with default options and wheel pickers for custom time
                if (showReminderPicker.value) {
                    ReminderDialogWindow(
                        context = context,
                        showReminderPicker = showReminderPicker,
                        reminder = reminder,
                        reminderTime = reminderTime,
                        dataEdited = dataEdited
                    )
                }

                // window to create or edit a tag
                if (showEditTagWindow.value) {
                    val availableColours = remember(allTags) { getAvailableTagColours(allTags, tagToEdit) }
                    val newTagName = remember { mutableStateOf(tagToEdit?.name ?: "") }
                    val newTagColour = remember { mutableLongStateOf(tagToEdit?.colour ?: availableColours[0].toColorLong()) }

                    CustomContentDialogWindow(
                        visible = showEditTagWindow,
                        content = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.title_newTag),
                                    style = MyTypography.titleMedium.copy(textAlign = TextAlign.Center)
                                )
                                CustomTextField(
                                    value = newTagName.value,
                                    onValueChange = { newTagName.value = it },
                                    icon = -1,
                                    placeHolder = stringResource(R.string.field_tagName),
                                    singleLine = true,
                                    maxLength = TAG_MAX,
                                    width = FULL_LENGTH.dp
                                )
                                Text(
                                    text = stringResource(R.string.txt_tagColour),
                                    style = MyTypography.bodyMedium
                                )
                                TagColourPicker(
                                    currentSelected = Color.Companion.fromColorLong(newTagColour.longValue),
                                    availableColours = availableColours
                                ) { chosenColour ->
                                    newTagColour.longValue = chosenColour
                                }
                            }
                        },
                        bottomButtons = true,
                        leftButtonContent = {
                            Text(
                                text = stringResource(R.string.button_cancel),
                                style = MyTypography.bodyLarge,
                                color = MaterialTheme.colorScheme.inversePrimary
                            )
                        },
                        leftButtonOnClick = {
                            showEditTagWindow.value = false
                        },
                        rightButtonContent = {
                            Text(
                                text = stringResource(R.string.button_confirm),
                                style = MyTypography.bodyLarge,
                                color = ValidGreen
                            )
                        },
                        rightButtonOnClick = {
                            newTagName.value = newTagName.value.trim()
                            if (newTagName.value.isBlank()) {
                                Toast.makeText(context, R.string.toast_emptyTagName, Toast.LENGTH_SHORT).show()
                            } else {
                                showEditTagWindow.value = false
                                val newTag = EventTag(uid = tagToEdit?.uid ?: UUID.randomUUID().toString(), name = newTagName.value, colour = newTagColour.longValue)
                                calendarVM.insertTag(newTag)
                                tagID.value = newTag.uid
                                chosenTag.value = newTag
                                tagToEdit = null
                                showEditTagWindow.value = false
                                dataEdited.value = true
                            }
                        }
                    )
                }

                // window to confirm deleting a tag
                if (showDeleteDialog.value) {
                    CustomContentDialogWindow(
                        visible = showDeleteDialog,
                        content = {
                            Text(
                                modifier = Modifier.padding(bottom = 8.dp),
                                text = stringResource(R.string.txt_tagDelete),
                                style = MyTypography.bodyLarge.copy(textAlign = TextAlign.Center)
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
                        leftButtonOnClick = {
                            tagToDelete = null
                            showDeleteDialog.value = false
                        },
                        rightButtonContent = {
                            Text(
                                text = stringResource(R.string.button_confirm),
                                style = MyTypography.bodyLarge,
                                color = ValidGreen
                            )
                        },
                        rightButtonOnClick = {
                            showDeleteDialog.value = false
                            showTagsDropdown.value = false
                            if (chosenTag.value.uid == tagToDelete?.uid) {
                                tagID.value = allTags[0].uid
                                chosenTag.value = allTags[0]
                            }
                            calendarVM.deleteTag(tagToDelete!!)
                            tagToDelete = null
                        }
                    )
                }

                // window to confirm leaving
                if (cancelVisible.value) {
                    CustomContentDialogWindow(
                        visible = cancelVisible,
                        content = {
                            Text(
                                modifier = Modifier.padding(bottom = 8.dp),
                                text = warningText,
                                style = MyTypography.bodyLarge.copy(textAlign = TextAlign.Center)
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
                        leftButtonOnClick = { cancelVisible.value = false },
                        rightButtonContent = {
                            Text(
                                text = stringResource(R.string.button_leave),
                                style = MyTypography.bodyLarge,
                                color = ValidGreen
                            )
                        },
                        rightButtonOnClick = {
                            cancelVisible.value = false
                            onGoBack()
                        }
                    )
                }
            }
        }
    )
}

/**
 * Wheel picker for selecting time in hours and minutes. Minutes are in 5 minute increments.
 *
 * @param currentMinutes the currently selected time in minutes since midnight
 * @param onConfirm callback function that is called when the user selects a time, with the selected hours and minutes as parameters
 */
@Composable
private fun TimeWheelPicker(
    currentMinutes: Int,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    val hours = (0..23).map { "%02d".format(it) }
    val minutes = (0..59 step 5).map { "%02d".format(it) }
    var pickedHour by remember { mutableIntStateOf(currentMinutes / 60) }
    var pickedMinute by remember { mutableIntStateOf((currentMinutes % 60) / 5) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            modifier = Modifier.width(80.dp),
            items = hours,
            selectedIndex = pickedHour,
            onIndexSelected = {
                pickedHour = it
                onConfirm(it, pickedMinute * 5)
            }
        )
        Text(
            text = ":",
            style = MyTypography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        WheelPicker(
            modifier = Modifier.width(80.dp),
            items = minutes,
            selectedIndex = pickedMinute,
            onIndexSelected = {
                pickedMinute = it
                onConfirm(pickedHour, it * 5)
            }
        )
    }
}

/**
 * Dialog window for selecting a reminder time.
 * Offers default options (none, on time, 10 minutes before, 1 hour before, 1 day before).
 * Also offers custom option (wheel pickers to choose specific minutes or hours).
 *
 * @param context used for accessing resources
 * @param showReminderPicker controls visibility of the dialog
 * @param reminder whether a reminder is set or not
 * @param reminderTime the time of the reminder in milliseconds before the event
 * @param dataEdited tracks whether any changes have been made to the event data
 */
@Composable
private fun ReminderDialogWindow(
    context: Context,
    showReminderPicker: MutableState<Boolean>,
    reminder: MutableState<Boolean>,
    reminderTime: MutableLongState,
    dataEdited: MutableState<Boolean>
) {
    // custom is selected if reminder time is not part of the default ones
    var customSelected by remember { mutableStateOf(
        reminder.value && reminderTime.longValue !in listOf(0L, 10*60*1000L, 60 * 60 * 1000L, 24 * 60 * 60 * 1000L)
    ) }
    var pickedAmount by remember { mutableIntStateOf(4) }
    var pickedUnit by remember { mutableIntStateOf(0) }
    val customName = context.getString(R.string.reminder_custom)

    // selecting default options
    var selectedPresetValue by remember { mutableStateOf<Long?>(
        if (!reminder.value) -1L
        else reminderTime.longValue
    ) }

    CustomContentDialogWindow(
        visible = showReminderPicker,
        content = {
            val presetLabels = listOf(
                stringResource(R.string.reminder_none),
                stringResource(R.string.reminder_onTime),
                stringResource(R.string.reminder_10before),
                stringResource(R.string.reminder_1hbefore),
                stringResource(R.string.reminder_1dbefore),
                stringResource(R.string.reminder_custom)
            )
            val currentChoice = remember { mutableStateOf(
                if (formatReminderTime(context, reminder.value, reminderTime.longValue).startsWith(customName)) {
                    customName
                } else {
                    formatReminderTime(context, reminder.value, reminderTime.longValue)
                }
            ) }

            ToggleOptions(
                boxHeight = HEIGHT,
                startOffset = OFFSET,
                numberChoices = presetLabels.size,
                currentChoice = currentChoice,
                choicesNames = presetLabels
            ) { chosen ->
                customSelected = chosen == context.getString(R.string.reminder_custom)
                selectedPresetValue = when (chosen) {
                    context.getString(R.string.reminder_none) -> -1L
                    context.getString(R.string.reminder_onTime) -> 0L
                    context.getString(R.string.reminder_10before) -> 10 * 60 * 1000L
                    context.getString(R.string.reminder_1hbefore) -> 60 * 60 * 1000L
                    context.getString(R.string.reminder_1dbefore) -> 24 * 60 * 60 * 1000L
                    else -> null
                }
            }

            // wheel picker showing when "Custom" option is selected
            if (customSelected) {
                val amounts = (1..59).map { it.toString() }
                val units = listOf(stringResource(R.string.reminder_minutes),
                    stringResource(R.string.reminder_hours)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        modifier = Modifier.width(80.dp),
                        items = amounts,
                        selectedIndex = (pickedAmount - 1).coerceAtLeast(0),
                        onIndexSelected = { pickedAmount = it + 1 }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    WheelPicker(
                        modifier = Modifier.width(100.dp),
                        items = units,
                        selectedIndex = pickedUnit,
                        onIndexSelected = { pickedUnit = it }
                    )
                }
            }

        },
        bottomButtons = true,
        leftButtonContent = {
            Text(
                text = stringResource(R.string.button_cancel),
                style = MyTypography.bodyLarge,
                color = MaterialTheme.colorScheme.inversePrimary
            )
        },
        leftButtonOnClick = { showReminderPicker.value = false },
        rightButtonContent = {
            Text(
                text = stringResource(R.string.button_confirm),
                style = MyTypography.bodyLarge,
                color = ValidGreen
            )
        },
        rightButtonOnClick = {
            if (customSelected) {
                reminder.value = true
                val multiplier = if (pickedUnit == 0) 60 * 1000L else 60 * 60 * 1000L
                reminderTime.longValue = pickedAmount * multiplier
            } else {
                val selectedValue = selectedPresetValue ?: 0L
                reminder.value = selectedValue >= 0L
                if (selectedValue >= 0L) reminderTime.longValue = selectedValue
            }
            showReminderPicker.value = false
            dataEdited.value = true
        }
    )
}

/**
 * Displays a grid of available tag colours for the user to choose from.
 *
 * @param currentSelected the currently selected colour for the tag being created/edited
 * @param availableColours a list of colours that are not currently being used by other tags (except for the tag being edited)
 * @param onColourSelected callback function that is called when the user selects a colour, with the selected colour as a parameter
 */
@Composable
fun TagColourPicker(
    currentSelected: Color,
    availableColours: List<Color>,
    onColourSelected: (Long) -> Unit
) {
    val selectedColour = remember { mutableStateOf(currentSelected) }

    FlowRow (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TAG_COLOURS.forEach { colour ->
            val isTaken = !availableColours.contains(colour)
            val isSelected = colour == selectedColour.value

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color = colour)
                    .border(
                        width = if (isSelected && !isTaken) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = CircleShape
                    )
                    .then(
                        if (!isTaken) {
                            Modifier.clickable {
                                onColourSelected(colour.toColorLong())
                                selectedColour.value = colour
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTaken) {
                    Icon(
                        modifier = Modifier.size(42.dp),
                        painter = painterResource(R.drawable.unavailable),
                        contentDescription = stringResource(R.string.desc_unavailableColour),
                        tint = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        }
    }
}