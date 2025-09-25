package com.example.bookbuddies.navigation

import com.example.bookbuddies.R

object Route{
    const val HOME = "Home"
    const val SETTINGS = "Settings"
    const val BOOK = "Book"
    const val BOOK_EDIT = "BookEdit"
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