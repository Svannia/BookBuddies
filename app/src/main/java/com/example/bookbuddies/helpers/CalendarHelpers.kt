package com.example.bookbuddies.helpers

import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.CalendarEvent
import timber.log.Timber
import java.util.Calendar
import kotlin.math.abs

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
data class Timezone(val label: String, val offset: String)

val TIMEZONES = listOf(
    Timezone("Pacific Daylight (PDT) / UTC-7", "UTC-7"),
    Timezone("Pacific Standard (PST) / UTC-8", "UTC-8"),
    Timezone("Mountain Daylight (MDT) / UTC-6", "UTC-6"),
    Timezone("Mountain Standard (MST) / UTC-7", "UTC-7"),
    Timezone("Central Daylight (CDT) / UTC-5", "UTC-5"),
    Timezone("Central Standard (CST) / UTC-6", "UTC-6"),
    Timezone("Eastern Daylight (EDT) / UTC-4", "UTC-4"),
    Timezone("Eastern Standard (EST) / UTC-5", "UTC-5"),
    Timezone("Greenwich Mean Time (GMT) / UTC+0", "UTC+0"),
    Timezone("British Summer (BST) / UTC+1", "UTC+1"),
    Timezone("Central Europe (CET) / UTC+1", "UTC+1"),
    Timezone("Central Europe Summer (CEST) / UTC+2", "UTC+2"),
    Timezone("China Standard (CST) / UTC+8", "UTC+8"),
    Timezone("Japan Standard (JST) / UTC+9", "UTC+9"),
    Timezone("Australia Eastern Standard (AEST) / UTC+10", "UTC+10"),
    Timezone("Australia Eastern Daylight (AEDT) / UTC+11", "UTC+11"),
)

fun getLocalTimezone(): Timezone {
    val tz = java.util.TimeZone.getDefault()
    val offsetMs = tz.getOffset(System.currentTimeMillis())
    val offsetHours = offsetMs / (1000 * 60 * 60)
    val offsetMins = abs((offsetMs / (1000 * 60)) % 60)
    val offsetStr = if (offsetMins == 0) {
        "UTC${if (offsetHours >= 0) "+$offsetHours" else "$offsetHours"}"
    } else {
        "UTC${if (offsetHours >= 0) "+$offsetHours" else "$offsetHours"}:${"%02d".format(offsetMins)}"
    }
    return TIMEZONES.find { it.offset == offsetStr }
        ?: Timezone(offsetStr, offsetStr)
}

/**
 * Takes a date and time from a specific timezone and converts them to the local date and time.
 *
 * @param epochMillis date to convert in epoch
 * @param minutes time to convert in minutes since midnight
 * @param sourceTimezone Timezone object to convert to local
 * @return pair with date and time converted to the local timezone
 */
fun convertToLocal(epochMillis: Long, minutes: Int, sourceTimezone: Timezone): Pair<Long, Int> {
    // parse the UTC offset from the timezone string
    val offsetStr = sourceTimezone.offset.removePrefix("UTC")
    val offsetMinutes = if (offsetStr.contains(":")) {
        val parts = offsetStr.split(":")
        val hours = parts[0].toInt()
        val mins = parts[1].toInt()
        if (hours < 0) hours * 60 - mins else hours * 60 + mins
    } else {
        offsetStr.toIntOrNull()?.times(60) ?: 0
    }

    // get local offset in minutes
    val localOffsetMs = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
    val localOffsetMinutes = localOffsetMs / (1000 * 60)

    // convert: remove source offset, add local offset
    val diffMinutes = localOffsetMinutes - offsetMinutes
    val totalMinutes = minutes + diffMinutes

    // handle day overflow/underflow
    val daysAdjustment = when {
        totalMinutes >= 24 * 60 -> 1
        totalMinutes < 0 -> -1
        else -> 0
    }
    val adjustedMinutes = ((totalMinutes % (24 * 60)) + 24 * 60) % (24 * 60)
    val adjustedEpoch = epochMillis + daysAdjustment * 24 * 60 * 60 * 1000L

    Timber.tag("TZ").d("sourceOffset=$offsetMinutes localOffset=$localOffsetMinutes diff=$diffMinutes")
    Timber.tag("TZ").d("inputMinutes=$minutes result=$adjustedMinutes")

    return adjustedEpoch to adjustedMinutes
}

fun getTimezoneOffset(timezoneLabel: String): String {
    if (timezoneLabel.startsWith("UTC")) return timezoneLabel
    return TIMEZONES.find { it.label == timezoneLabel }?.offset ?: timezoneLabel
}