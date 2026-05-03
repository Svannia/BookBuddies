package com.example.bookbuddies.navigation

import com.example.bookbuddies.R

object Route{
    const val HOME = "Home"
    const val SETTINGS = "Settings"
    const val BOOK = "Book"
    const val BOOK_EDIT = "BookEdit"
    const val BOOK_CREATE = "BookCreate"
    const val SCAN_ISBN = "ScanISBN"
    const val ENTER_ISBN = "EnterISBN"
    const val CALENDAR = "Calendar"
    const val EVENT = "Event"
    const val EVENT_EDIT = "EventEdit"
    const val EVENT_CREATE = "EventCreate"
}

/**
 * Defines a destination that a button could go to.
 *
 * @property route where this destination navigates to
 * @property icon the destination's icon in-app
 * @property text describes in-app where this button navigates to
 */
data class Destination(val route: String, val icon: Int = 0, val text: Int)

/**
 * All destinations contained in the burger menu (side drawer menu)
 */
val BURGER_DESTINATIONS = listOf(
    Destination(route = Route.SETTINGS, icon = R.drawable.settings, text = R.string.dst_settings),
)

/**
 * All destinations contained in the bottom navigation bar
 */
val BOTTOM_DESTINATIONS = listOf(
    Destination(route = Route.HOME, icon = R.drawable.home, text = R.string.dst_home),
    Destination(route = Route.CALENDAR, icon = R.drawable.calendar, text = R.string.dst_calendar),
)