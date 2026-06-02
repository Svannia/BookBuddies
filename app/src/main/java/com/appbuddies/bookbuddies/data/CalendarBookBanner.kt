package com.appbuddies.bookbuddies.data

import java.util.Calendar

data class BookBanner(
    val book: Book,
    val startCol: Int, // 0-6 for Monday-Sunday
    val endCol: Int,
    val continuesBefore: Boolean, // if the span started before this week
    val continuesAfter: Boolean
)

/**
 * Given a list of BookBanners for a calendar week, pack them into rows so that banners that don't overlap in columns share the same row.
 *
 * @param banners a list of BookBanners to pack into rows.
 * @return a list of rows, where each row is a list of BookBanners that can be displayed on the same row without column overlap.
 */
fun packBanners(banners: List<BookBanner>): List<List<BookBanner>> {
    val rows = mutableListOf<MutableList<BookBanner>>()
    for (banner in banners) {
        // find first row where this banner fits (no column overlap)
        val targetRow = rows.firstOrNull { row ->
            row.none { existing ->
                banner.startCol <= existing.endCol && banner.endCol >= existing.startCol
            }
        }
        if (targetRow != null) {
            targetRow.add(banner)
        } else {
            rows.add(mutableListOf(banner))
        }
    }
    return rows
}

/**
 * For one calendar week, compute all banners to show.
 *
 * @param books books which reading interval overlap with the computed week
 * @param weekStartEpoch start of the week in epoch millis
 * @param weekEndEpoch end of the week in epoch millis
 * @return a list of BookBanners to display on this week
 */
fun getBannersForWeek(
    books: List<Book>,
    weekStartEpoch: Long,
    weekEndEpoch: Long
): List<BookBanner> {
    return books
        // filter out books that don't have a set read interval
        .filter { it.dateStarted > 0L && it.dateFinished > 0L }
        // filter out books that haven't been read during this week
        .filter { it.dateStarted <= weekEndEpoch && it.dateFinished >= weekStartEpoch}
        .map { book ->
            // compute at which week columns the banner starts and stops
            val startCol = if (book.dateStarted <= weekStartEpoch) 0
                            else dayOfWeekCol(book.dateStarted)
            val endCol = if (book.dateFinished >= weekEndEpoch) 6
                            else dayOfWeekCol(book.dateFinished)
            BookBanner(
                book = book,
                startCol = startCol,
                endCol = endCol,
                continuesBefore = book.dateStarted < weekStartEpoch,
                continuesAfter = book.dateFinished > weekEndEpoch
            )
        }
}

/**
 * Given the epoch time of the start of a week, compute the epoch time of the end of the week (Sunday 23:59:59).
 *
 * @param weekStart the epoch time of the start of the week in milliseconds
 * @return the epoch time of the end of the week in milliseconds
 */
fun weekEndEpoch(weekStart: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = weekStart
        add(Calendar.DAY_OF_MONTH, 6)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }
    return calendar.timeInMillis
}

/**
 * Convert an epoch time to a column index (0-6) for the calendar week, where 0 is Monday and 6 is Sunday.
 */
private fun dayOfWeekCol(epoch: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = epoch }
    return (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
}
