package com.example.bookbuddies.datastore

import android.content.Context
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.EventTag
import kotlinx.coroutines.flow.Flow

// Repository to manage calendar data operations and provide a clean API for data access
class CalendarRepository(context: Context) {
    private val db = DatabaseProvider.getDatabase(context)

    // CALENDAR EVENTS
    private val calendarEventDao = db.calendarEventDao()

    val allEvents: Flow<List<CalendarEvent>> = calendarEventDao.getAllEvents()

    suspend fun insertEvent(event: CalendarEvent) = calendarEventDao.insertEvent(event)
    suspend fun deleteEvent(event: CalendarEvent) = calendarEventDao.deleteEvent(event)

    // EVENT TAGS
    private val eventTagDao = db.eventTagDao()

    val allEventTags: Flow<List<EventTag>> = eventTagDao.getAllTags()

    suspend fun insertEventTag(tag: EventTag) = eventTagDao.insertTag(tag)
    suspend fun deleteEventTag(tag: EventTag) = eventTagDao.deleteTag(tag)
}