package com.example.bookbuddies.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey val uid: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val allDay: Boolean,
    val dateStart: Long,
    val dateEnd: Long,
    val hourStart: Int, // minutes since midnight
    val hourEnd: Int,
    val location: String,
    val notes: String,
    val tags: List<String> = emptyList(),
    val reminder: Boolean,
    val reminderTime: Long
)

// defaults tags on download: BOOK_RELEASE, SPECIAL_SALE, AUTHOR_EVENT, ARC_DEADLINE
@Entity(tableName = "event_tags")
data class EventTag(
    @PrimaryKey val uid: String = java.util.UUID.randomUUID().toString(),
    val name: String,
)