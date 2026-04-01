package com.example.bookbuddies.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.CalendarViewModel
import java.time.Month
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

@Composable
fun CalendarScreen(bookVM: BookViewModel, calendarVM: CalendarViewModel, navigationActions: NavigationActions) {

    // when using the phone's built-in back function -> back to Home screen
    BackHandler {
        navigationActions.navigateTo(Route.HOME, true)
    }

    // to jump back the calendar to today's date
    val today = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    }
    // other calendar dates
    val currentCalendar = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH) + 1) }

    // to toggle the visibility of read books
    val showReadBooks by calendarVM.showReadBooks.collectAsState()

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_calendar),
        navigationIndex = 1,
        topBarIcons = {
            Row(
                modifier = Modifier.padding(0.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // date of the month button to jump to today
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.inversePrimary,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { /* todo: jump to today */ }
                ) {
                    Text(
                        text = today,
                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
                // options menu for various options
                OptionsMenu(
                    icon = R.drawable.options,
                    (if (showReadBooks) stringResource(R.string.button_hideRead)
                    else stringResource(R.string.button_showRead))
                            to { calendarVM.toggleShowReadBooks() },
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MonthlyCalendarView(year = displayedYear, month = displayedMonth)
        }
    }
}

@Composable
fun MonthlyCalendarView(
    year: Int,
    month: Int
) {
    val today = remember { Calendar.getInstance() }
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH) + 1
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    // figure out the days in the month and the starting day of the week
    val calendar = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = ((calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) // Monday as 0, Sunday as 6

    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).uppercase(),
            style = MyTypography.titleSmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                .clickable {
                    // todo: open popup to chose a year and month
                }

            )
        // headers with days of the week
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            dayHeaders.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                        // highlight Sunday in red
                        color = if (index == 6) Color.Red else MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        }

        // dividing line between headers and monthly spread
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline
        )

        // grid
        val totalCells = firstDayOfWeek + daysInMonth
        val totalRows = ceil(totalCells / 7f).toInt()

        repeat(totalRows) { row ->
            // row for one week
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // one day of the week
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1
                    val isCurrentMonth = day in 1..daysInMonth
                    val isToday = isCurrentMonth && year == todayYear && month == todayMonth && day == todayDay
                    val isSunday = col == 6

                    // day cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (isToday) Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(6.dp)
                                ) else Modifier
                            )
                            .clickable {
                                // todo: popup with details of day's events
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isCurrentMonth) {
                            // cell title with day number
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(28.dp)
                                    .then(
                                        if (isToday) Modifier.background(
                                                color = MaterialTheme.colorScheme.inversePrimary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    color = when {
                                        isToday -> MaterialTheme.colorScheme.background
                                        isSunday -> Color.Red
                                        else -> MaterialTheme.colorScheme.inversePrimary
                                    }
                                )
                            }
                            // todo: cell contents
                        }
                    }
                }
            }
        }
    }
    }