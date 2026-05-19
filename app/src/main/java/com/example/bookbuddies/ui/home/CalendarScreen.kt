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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.BookBanner
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.EventTag
import com.example.bookbuddies.data.getBannersForWeek
import com.example.bookbuddies.data.packBanners
import com.example.bookbuddies.data.weekEndEpoch
import com.example.bookbuddies.helpers.displayAuthors
import com.example.bookbuddies.helpers.formatEventTime
import com.example.bookbuddies.helpers.getBooksForDay
import com.example.bookbuddies.helpers.getCoverColour
import com.example.bookbuddies.helpers.getEventsForDay
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.ui.CustomContentDialogWindow
import com.example.bookbuddies.ui.OptionsMenu
import com.example.bookbuddies.ui.PrimaryScreen
import com.example.bookbuddies.ui.WheelPicker
import com.example.bookbuddies.ui.theme.MediumGrey
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

    // to save calendar state between navigation
    var displayedYear by remember { mutableIntStateOf(calendarVM.savedYear) }
    var displayedMonth by remember { mutableIntStateOf(calendarVM.savedMonth) }
    LaunchedEffect(displayedYear, displayedMonth) {
        calendarVM.savedYear = displayedYear
        calendarVM.savedMonth = displayedMonth
    }

    // to toggle the visibility of read books
    val showReadBooks by calendarVM.showReadBooks.collectAsState()
    val books by bookVM.sortedBooks.collectAsState(emptyList())

    // show events
    val events by calendarVM.allEvents.collectAsState(emptyList())
    val tags by calendarVM.allEventTags.collectAsState(emptyList())
    var selectedDay by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) } // year, month, day

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
                    "Delete all events" to {calendarVM.deleteAllEvents()}
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // animated calendar grid
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
                    events = events,
                    tags = tags,
                    year = targetYear,
                    month = targetMonth,
                    showReadBooks = showReadBooks,
                    onDayClick = { y, m, d -> selectedDay = Triple(y, m, d)},
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

            // show day cell
            selectedDay?.let { (y, m, d) ->
                DayDetailsWindow(
                    year = y,
                    month = m,
                    day = d,
                    books = books,
                    events = events,
                    tags = tags,
                    onChooseColour = { book, chosenColourIdx ->
                        bookVM.updateChosenCoverColour(book, chosenColourIdx)
                    },
                    onDismiss = { selectedDay = null },
                    onNavigateToBook = { bookId ->
                        selectedDay = null
                        navigationActions.navigateTo("${Route.BOOK}/$bookId")
                    },
                    onNavigateToEvent = { eventId ->
                        selectedDay = null
                        // todo: navigate to event screen
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
    events: List<CalendarEvent>,
    tags: List<EventTag>,
    year: Int,
    month: Int,
    showReadBooks: Boolean,
    onDayClick: (Int, Int, Int) -> Unit,
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
                        color = if (index == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                                    if (isCurrentMonth) onDayClick(year, month, day)
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

                if (showReadBooks) {
                    // book banners
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    ) {
                        WeekBookBanners(banners)
                    }
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
                isSunday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.inversePrimary
            }
        )
    }
    // todo: cell contents
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
                    val bannerColour = getCoverColour(banner.book)
                    // compute banner shape
                    val bannerShape = RoundedCornerShape(
                        topStart = if (banner.continuesBefore) 0.dp else 6.dp,
                        bottomStart = if (banner.continuesBefore) 0.dp else 6.dp,
                        topEnd = if (banner.continuesAfter) 0.dp else 6.dp,
                        bottomEnd = if (banner.continuesAfter) 0.dp else 6.dp
                    )
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
                            .background(color = bannerColour, shape = bannerShape)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = bannerShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = banner.book.title,
                            style = MyTypography.bodySmall,
                            color = if (bannerColour.luminance() > 0.4f) Color.Black else Color.White,
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

@Composable
fun DayDetailsWindow(
    year: Int,
    month: Int,
    day: Int,
    books: List<Book>,
    events: List<CalendarEvent>,
    tags: List<EventTag>,
    onChooseColour: (Book, Int) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToBook: (String) -> Unit,
    onNavigateToEvent: (String) -> Unit
) {
    val visible = remember { mutableStateOf(true) }
    LaunchedEffect(visible.value) {
        if (!visible.value) onDismiss()
    }

    val isSunday = remember {
        val cal = Calendar.getInstance().apply { set(year, month - 1, day) }
        cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }
    val dayBooks = remember(books, year, month, day) { getBooksForDay(books, year, month, day) }
    val dayEvents = remember(events, year, month, day) { getEventsForDay(events, year, month, day) }
    val monthName = remember { Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() } }

    // to display year or not in the header
    val todayYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val headerText = remember {
        val cal = Calendar.getInstance().apply { set(year, month - 1, day) }
        val weekday = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            ?.replaceFirstChar { it.uppercase() } ?: ""
        buildString {
            append("$weekday $day $monthName")
            if (year != todayYear) append(" $year")
        }
    }

    var colourPickerBook by remember { mutableStateOf<String?>(null) }

    CustomContentDialogWindow(
        visible = visible,
        bottomButtons = false,
        content = {
            // header with date
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                text = headerText,
                style = MyTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSunday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary
            )

            // empty day
            if (dayBooks.isEmpty() && dayEvents.isEmpty()) {
                Text(
                    text = stringResource(R.string.txt_emptyDay),
                    style = MyTypography.bodyMedium,
                )
            }

            // book items
            dayBooks.forEach { book ->
                val bannerColor = getCoverColour(book)
                val isColourPickerOpen = colourPickerBook == book.uid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(color = bannerColor, shape = RoundedCornerShape(8.dp))
                        .clickable { onNavigateToBook(book.uid) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // open book icon
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.open_book),
                        contentDescription = stringResource(R.string.desc_book),
                        tint = if (bannerColor.luminance() > 0.4f) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    // book info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // book title
                        Text(
                            text = book.title,
                            style = MyTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (bannerColor.luminance() > 0.4f) Color.Black else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // author(s)
                        val authors = displayAuthors(book.authors)
                        if (authors.isNotBlank()) {
                            Text(
                                text = authors,
                                style = MyTypography.bodySmall,
                                color = (if (bannerColor.luminance() > 0.4f) Color.Black else Color.White).copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // colour change icon
                    IconButton( onClick = {
                        colourPickerBook = if (isColourPickerOpen) null else book.uid
                    }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.palette),
                            contentDescription = stringResource(R.string.desc_colourEditIcon),
                            tint = if (bannerColor.luminance() > 0.4f) Color.Black else Color.White
                        )
                    }
                }

                // colour bucket picker
                if (isColourPickerOpen) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        book.coverColours.forEachIndexed { index, colour ->
                            val isSelected = index == book.chosenCoverColour
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color = Color(colour), shape = RoundedCornerShape(6.dp))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.inversePrimary,
                                            shape = RoundedCornerShape(6.dp)
                                        )else Modifier.border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    )
                                    .clickable {
                                        onChooseColour(book, index)
                                        colourPickerBook = null
                                    }
                            )
                        }
                    }
                }
            }

            // event items
            dayEvents.forEach { event ->
                val tagColor = tags.find { it.uid == event.tag }?.let { Color(it.colour) }
                    ?: MediumGrey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(width = 1.dp, color = tagColor, shape = RoundedCornerShape(8.dp))
                        .clickable { onNavigateToEvent(event.uid) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // color sticker
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = tagColor, shape = RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // calendar icon
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.calendar),
                        contentDescription = stringResource(R.string.desc_calendar),
                        tint = MaterialTheme.colorScheme.inversePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // event info
                    Column {
                        // event name
                        Text(
                            text = event.title,
                            style = MyTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.inversePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // event time
                        Text(
                            text = formatEventTime(event),
                            style = MyTypography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    )
}