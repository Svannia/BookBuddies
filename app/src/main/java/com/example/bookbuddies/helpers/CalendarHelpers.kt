package com.example.bookbuddies.helpers

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toColorLong
import com.example.bookbuddies.R
import com.example.bookbuddies.data.Book
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.DateFormat
import com.example.bookbuddies.data.EventTag
import com.example.bookbuddies.data.TAG_COLOURS
import timber.log.Timber
import java.util.Calendar
import kotlin.math.abs

/**
 * Filters a list of events to those that occur on a specific day.
 * The events are sorted by full-day events first, then by start time.
 *
 * @param events list of events to filter
 * @param year of the day to filter for
 * @param month of the day to filter for (1-12)
 * @param day of the day to filter for
 * @return list of events that occur on the specified day
 */
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
    val localOffset = getLocalTimezone().offset

    return events
        .map { event ->
            val eventOffset = getTimezoneOffset(event.timezone)
            if (!event.allDay && eventOffset != localOffset) {
                val tz = TIMEZONES.find { it.offset == eventOffset }
                    ?: Timezone(eventOffset, eventOffset)
                val (localStartDate, localStartMinutes) = convertToLocal(event.dateStart, event.minuteStart, tz)
                val (localEndDate, localEndMinutes) = if (event.dateEnd > 0L || event.minuteEnd > 0) {
                    convertToLocal(event.dateEnd.takeIf { it > 0L } ?: event.dateStart, event.minuteEnd, tz)
                } else {
                    event.dateEnd to event.minuteEnd
                }
                event.copy(
                    dateStart = localStartDate,
                    minuteStart = localStartMinutes,
                    dateEnd = localEndDate,
                    minuteEnd = localEndMinutes
                )
            } else event
        }
        .filter {
            (it.dateStart <= dayEnd && it.dateEnd >= dayStart) ||
                    (it.dateEnd <= 0L && it.dateStart <= dayEnd && it.dateStart >= dayStart)
        }
        .sortedWith(compareBy(
            { !it.allDay },
            { if (it.allDay) it.title else null },
            { if (!it.allDay) it.minuteStart else null }
        ))
}

/**
 * Filters a list of books to those that were read on a specific day (i.e. the day falls between dateStarted and dateFinished).
 *
 * @param books list of books to filter
 * @param year of the day to filter for
 * @param month of the day to filter for (1-12)
 * @param day of the day to filter for
 * @return list of books that were read on the specified day
 */
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

/**
 * Gets the local timezone as a Timezone object with label and offset.
 *
 * @return Timezone object representing the local timezone
 */
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

/**
 * Gets the UTC offset string for a given timezone label.
 * If the label is not found in the predefined list, it returns the label itself (assuming it's already an offset).
 *
 * @param timezoneLabel the label of the timezone to get the offset for
 * @return the UTC offset string corresponding to the given timezone label, or the label itself if not found
 */
fun getTimezoneOffset(timezoneLabel: String): String {
    if (timezoneLabel.startsWith("UTC")) return timezoneLabel
    return TIMEZONES.find { it.label == timezoneLabel }?.offset ?: timezoneLabel
}

/**
 * Shows the time conversion from an event's timezone to the local timezone, including the date if it falls on a different day.
 *
 * @param context for fetching string resources
 * @param timezone event's timezone label
 * @param dateStart event's start date in epoch milliseconds
 * @param minuteStart event's start time in minutes since midnight
 * @param dateEnd event's end date in epoch milliseconds (0 if no end date)
 * @param minuteEnd event's end time in minutes since midnight (0 if no end time
 * @return a string representing the local time equivalent of the event's time
 */
fun displayTimezoneConversion(
    context: Context,
    timezone: String,
    dateStart: Long,
    minuteStart: Int,
    dateEnd: Long,
    minuteEnd: Int
): String {
    // get timezone object for event's timezone
    val selectedTz = TIMEZONES.find { it.label == timezone }
        ?: Timezone(timezone, timezone)

    // convert date and time to local
    val (localDateStart, localMinuteStart) = convertToLocal(dateStart, minuteStart, selectedTz)
    val (localDateEnd, localMinuteEnd) = convertToLocal(dateEnd, minuteEnd, selectedTz)
    val sameDayStart = localDateStart == dateStart
    val sameDayEnd = dateEnd <= 0L || localDateEnd == dateEnd

    var conversionText = "${context.getString(R.string.txt_localTime)}: "
    if (!(sameDayStart && sameDayEnd)) conversionText += "${displayDate(localDateStart, DateFormat.SHORT_DAY_DATE)}, "
    conversionText += formatMinutes(localMinuteStart)
    if (minuteEnd > 0 || dateEnd > 0L) {
        conversionText += " - "
        if (!(sameDayStart && sameDayEnd)) conversionText += "${displayDate(localDateEnd, DateFormat.SHORT_DAY_DATE)}, "
        conversionText += formatMinutes(localMinuteEnd)
    }
    return conversionText
}

/**
 * Gets a list of available tag colours that are not already taken by existing tags.
 *
 * @param existingTags list of existing EventTag objects to check for taken colours
 * @param currentTag if given, this tag's colour will not be kept in the returned list
 * @return list of Color objects representing the available tag colours
 */
fun getAvailableTagColours(existingTags: List<EventTag>, currentTag: EventTag ?= null): List<Color> {
    val takenColours = existingTags.map { it.colour }.toSet()
    return TAG_COLOURS.filter { it.toColorLong() !in takenColours || (currentTag != null && it.toColorLong() == currentTag.colour) }
}
