package com.example.bookbuddies.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    val title: String,
    val allDay: Boolean,
    val dateStart: Long,
    val dateEnd: Long,
    val minuteStart: Int, // minutes since midnight
    val minuteEnd: Int,
    val location: String,
    val notes: String,
    val tag: String,
    val reminder: Boolean,
    val reminderTime: Long
) {
    companion object {
        /**
         * Creates an empty CalendarEvent data object.
         *
         * @return empty CalendarEvent data object.
         */
        fun empty(): CalendarEvent {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            return CalendarEvent(UUID.randomUUID().toString().replace("-", ""),
                "", false, today, today, 12*60, 13*60,
                "", "", "", false, -1L
            )
        }
    }
}

// defaults tags on download: BOOK_RELEASE, SPECIAL_SALE, AUTHOR_EVENT, ARC_DEADLINE
@Entity(tableName = "event_tags")
data class EventTag(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    val name: String,
    val colour: Long,
    val isDefault: Boolean = false
)