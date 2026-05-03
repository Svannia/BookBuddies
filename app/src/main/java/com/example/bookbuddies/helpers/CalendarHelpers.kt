package com.example.bookbuddies.helpers

import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.CalendarEvent
import java.util.Calendar

fun getEventsForDay(events: List<CalendarEvent>, year: Int, month: Int, day: Int): List<CalendarEvent> {
    val cal = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayStart = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    val dayEnd = cal.timeInMillis
    return events.filter { it.dateStart <= dayEnd && it.dateEnd >= dayStart }
}

fun getBooksForDay(books: List<Book>, year: Int, month: Int, day: Int): List<Book> {
    val cal = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayStart = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    val dayEnd = cal.timeInMillis
    return books.filter { it.dateStarted > 0L && it.dateFinished > 0L && it.dateStarted <= dayEnd && it.dateFinished >= dayStart }
}