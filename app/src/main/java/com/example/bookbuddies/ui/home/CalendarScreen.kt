package com.example.bookbuddies.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookBanner
import com.example.bookbuddies.data.coverColour
import com.example.bookbuddies.data.getBannersForWeek
import com.example.bookbuddies.data.packBanners
import com.example.bookbuddies.data.weekEndEpoch
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.ui.theme.MyTypography
import com.example.bookbuddies.ui.theme.ValidGreen
import com.example.bookbuddies.viewModels.BookViewModel
import com.example.bookbuddies.viewModels.CalendarViewModel
import java.time.Month
import java.time.format.TextStyle
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
    val currentCalendar = remember { Calendar.getInstance() }
    val todayYear = remember { currentCalendar.get(Calendar.YEAR) }
    val todayMonth = remember { currentCalendar.get(Calendar.MONTH) + 1 }
    val today = remember { currentCalendar.get(Calendar.DAY_OF_MONTH).toString() }
    var displayedYear by remember { mutableIntStateOf(todayYear) }
    var displayedMonth by remember { mutableIntStateOf(todayMonth) }
    // to toggle the visibility of read books
    val showReadBooks by calendarVM.showReadBooks.collectAsState()
    val books by bookVM.sortedBooks.collectAsState(emptyList())

    // for sliding animations
    // 1 for next month, -1 for previous month
    val slideDirection = remember { mutableIntStateOf(1) }

    // for picking a specific month
    val showMonthPicker = remember { mutableStateOf(false) }

    // for popup to add new event
    val createNewEvent = remember { mutableStateOf(false) }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_calendar),
        navigationIndex = 1,
        addPopUp = createNewEvent,
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
                        .clickable {
                            slideDirection.intValue =
                                if (displayedYear * 12 + displayedMonth < todayYear * 12 + todayMonth) 1 else -1
                            displayedYear = todayYear
                            displayedMonth = todayMonth
                        }
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
            AnimatedContent(
                targetState = displayedYear * 12 + displayedMonth,
                transitionSpec = {
                    if (slideDirection.intValue == 1) {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    }
                },
                label = "monthSlide"
            ) { targetYearMonth ->
                val targetMonth = ((targetYearMonth - 1) % 12) + 1
                val targetYear = (targetYearMonth - 1) / 12
                MonthlyCalendarView(
                    books = books,
                    year = targetYear,
                    month = targetMonth,
                    onSwipePrevious = {
                        slideDirection.intValue = -1
                        if (displayedMonth == 1) {
                            displayedMonth = 12
                            displayedYear--
                        } else {
                            displayedMonth--
                        }
                    },
                    onSwipeNext = {
                        slideDirection.intValue = 1
                        if (displayedMonth == 12) {
                            displayedMonth = 1
                            displayedYear++
                        } else {
                            displayedMonth++
                        }
                    },
                    onChooseMonth = {
                        showMonthPicker.value = true
                    }
                )
            }
        }

        if (showMonthPicker.value) {
            var pickedYear by remember { mutableIntStateOf(displayedYear)}
            var pickedMonth by remember { mutableIntStateOf(displayedMonth) }

            CustomContentDialogWindow(
                visible = showMonthPicker,
                content = {
                    val months = (1..12).map {
                        Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault())
                            .replaceFirstChar { c ->  c.uppercase() }
                    }
                    val years = (todayYear - 10..todayYear + 20).map { it.toString() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // picker for month
                        WheelPicker(
                            modifier = Modifier.width(140.dp),
                            items = months,
                            selectedIndex = pickedMonth - 1,
                            onIndexSelected = { pickedMonth = it + 1 }
                        )

                        // picker for year
                        WheelPicker(
                            modifier = Modifier.width(100.dp),
                            items = years,
                            selectedIndex = years.indexOf(pickedYear.toString()),
                            onIndexSelected = { pickedYear = years[it].toInt() }
                        )
                    }
                },
                bottomButtons = true,
                leftButtonContent = {
                    Text(
                        text = stringResource(R.string.button_cancel),
                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                },
                leftButtonOnClick = { showMonthPicker.value = false },
                rightButtonContent = {
                    Text(
                        text = stringResource(R.string.button_confirm),
                        style = MyTypography.bodyMedium.copy(textAlign = TextAlign.Center),
                        color = ValidGreen
                    )
                },
                rightButtonOnClick = {
                    slideDirection.intValue =
                        if (pickedYear * 12 + pickedMonth > displayedYear * 12 + displayedMonth) 1
                        else -1
                    displayedYear = pickedYear
                    displayedMonth = pickedMonth
                    showMonthPicker.value = false
                }
            )
        }
    }

    // adding new event
    if (createNewEvent.value) {
        navigationActions.navigateTo(Route.EVENT_CREATE)
    }
}

@Composable
fun MonthlyCalendarView(
    books: List<Book>,
    year: Int,
    month: Int,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onChooseMonth: () -> Unit
) {
    val todayCalendar = remember { Calendar.getInstance() }
    val todayYear = todayCalendar.get(Calendar.YEAR)
    val todayMonth = todayCalendar.get(Calendar.MONTH) + 1
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    // figure out the days in the month and the starting day of the week
    val calendar = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = ((calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) // Monday as 0, Sunday as 6

    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var consumed = false
                detectVerticalDragGestures(
                    onDragStart = { consumed = false },
                    onDragEnd = { consumed = false },
                    onDragCancel = { consumed = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (!consumed) {
                            if (dragAmount > 50) {
                                consumed = true
                                onSwipePrevious()
                            } else if (dragAmount < -50) {
                                consumed = true
                                onSwipeNext()
                            }
                        }

                    }
                )
            }
    ) {
        // month (and year) header
        val text = buildString {
            append(Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase())
            if (year != todayYear) append(" $year")
        }
        Button(
            onClick = { onChooseMonth() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors().copy(containerColor = Color.Transparent),
        ) {
            Text(
                text = text,
                style = MyTypography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.inversePrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        // headers with days of the week
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)) {
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
        val prevMonthCalendar = Calendar.getInstance().apply {
            set(year, month - 1, 1)
            add(Calendar.MONTH, -1)
        }
        val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // for displaying the book banners
        val allBanners = remember(books, year, month) {
            (0 until totalRows).map { row ->
                val mondayOffset = row * 7 - firstDayOfWeek
                val weekStartCal = Calendar.getInstance().apply {
                    set(year, month - 1, 1)
                    add(Calendar.DAY_OF_MONTH, mondayOffset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val weekStart = weekStartCal.timeInMillis
                getBannersForWeek(books, weekStart, weekEndEpoch(weekStart))
            }
        }

        repeat(totalRows) { row ->
            val banners = allBanners[row]

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // row for one week
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // one day of the week
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1
                        val isCurrentMonth = day in 1..daysInMonth
                        val overflowDay = when {
                            day < 1 -> daysInPrevMonth + day // previous month
                            day > daysInMonth -> day - daysInMonth // next month
                            else -> null
                        }
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
                                .alpha(if (isCurrentMonth) 1f else 0.3f)
                                .clickable {
                                    // todo: popup with details of day's events
                                },
                            contentAlignment = Alignment.TopCenter
                        )
                        {
                            val displayDay = if (isCurrentMonth) day else overflowDay
                            if (displayDay != null) {
                                DayCell(
                                    day = displayDay,
                                    isToday = isToday,
                                    isSunday = isSunday
                                )
                            }
                        }
                    }
                }

                // book banners
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                ) {
                    WeekBookBanners(banners)
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSunday: Boolean
) {
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

@Composable
fun WheelPicker(
    modifier: Modifier,
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val visibleItems = 5

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex}
    }

    LaunchedEffect(centerIndex) {
        onIndexSelected(centerIndex.coerceIn(0, items.size - 1))
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == centerIndex.coerceIn(0, items.size - 1)
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MyTypography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun WeekBookBanners(banners: List<BookBanner>) {
    val packedRows = remember(banners) { packBanners(banners) }
    Column(modifier = Modifier.fillMaxWidth()) {
        packedRows.forEach { rowBanners ->
            Row(modifier = Modifier.fillMaxWidth()) {
                var currentCol = 0
                rowBanners.sortedBy { it.startCol }.forEach { banner ->
                    // compute dominant colour
                    val bannerColour = remember(banner.book.cover) {
                        coverColour(banner.book)
                    }
                    // gap before this banner
                    if (banner.startCol > currentCol) {
                        Spacer(modifier = Modifier.weight((banner.startCol - currentCol).toFloat()))
                    }
                    val spanWeight = (banner.endCol - banner.startCol + 1).toFloat()
                    Box(
                        modifier = Modifier
                            .weight(spanWeight)
                            .height(18.dp)
                            .padding(
                                start = if (banner.startCol == 0 && banner.continuesBefore) 0.dp else 2.dp,
                                end = if (banner.endCol == 6 && banner.continuesAfter) 0.dp else 2.dp
                            )
                            .background(
                                color = bannerColour,
                                shape = RoundedCornerShape(
                                    topStart = if (banner.continuesBefore) 0.dp else 6.dp,
                                    bottomStart = if (banner.continuesBefore) 0.dp else 6.dp,
                                    topEnd = if (banner.continuesAfter) 0.dp else 6.dp,
                                    bottomEnd = if (banner.continuesAfter) 0.dp else 6.dp
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = banner.book.title,
                            style = MyTypography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    currentCol = banner.endCol + 1
                }
                // fill remaining space after last banner
                if (currentCol <= 6) {
                    Spacer(modifier = Modifier.weight((7 - currentCol).toFloat()))
                }
            }
        }
    }
}