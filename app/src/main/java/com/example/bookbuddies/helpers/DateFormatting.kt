package com.example.bookbuddies.helpers

import android.content.Context
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.DateFormat
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

/**
 * Parses a date string into a Long (milliseconds since Unix epoch).
 *
 * @param dateStr the date string to parse
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @return the parsed date in milliseconds since Unix epoch, or null if parsing failed
 */
fun parseDate(dateStr: String, isError: (Boolean) -> Unit): Long? {
    val str = dateStr.trim()
    if (dateStr.isBlank()) return null

    return try {
        when {
            str.matches(Regex("""\d{4}$""")) -> { // yyyy
                val year = Year.parse(str)
                year.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}$""")) -> { // yyyy-MM
                val ym = YearMonth.parse(str)
                ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> { // yyyy-MM-dd or longer
                try {
                    val local = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
                    local.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                } catch (_: DateTimeParseException) {
                    // fallback for datetime strings with time
                    val ldt = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
                    ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
            }
            str.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> { // dd/mm/yyyy
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}/\d{2}/\d{2}""")) -> { // yyyy/mm/dd
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""[A-Za-z]+ \d{1,2}, \d{4}""")) -> { // MMMM d, yyyy
                val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
                val locale = LocalDate.parse(str, formatter)
                locale.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            else -> {
                Timber.tag("BookImport").e("Unknown date format: $str")
                isError(true)
                null
            }
        }
    } catch (e: DateTimeParseException) {
        Timber.tag("BookImport").e("Failed to parse date: $str with error $e")
        isError(true)
        null
    }
}

/**
 * Parses a date string into a Long (milliseconds since epoch).
 * This is specifically for a book's dateAdded field which also stores hour and minutes.
 *
 * @param dateStr the date string to parse
 * @param isError lambda that returns true if an error occurred while running the function, and a string with error details
 * @return the parsed date in milliseconds since Unix epoch, or null if parsing failed
 */
fun parseAddedDate(dateStr: String, isError: (Boolean) -> Unit): Long? {
    val str = dateStr.trim()
    if (str.isBlank()) return null

    return try {
        when {
            str.matches(Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}(:\d{2})?""")) -> { // dd/MM/yyyy HH:mm
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}/\d{2}/\d{2} \d{2}:\d{2}(:\d{2})?""")) -> { // yyyy/MM/dd HH:mm
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{2}-\d{2}-\d{4} \d{2}:\d{2}(:\d{2})?""")) -> { // dd-MM-yyyy HH:mm
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            str.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}(:\d{2})?""")) -> { // yyyy-MM-dd HH:mm
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]", Locale.getDefault())
                val localDateTime = LocalDateTime.parse(str, formatter)
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            else -> {
                Timber.tag("BookImport").d("Unknown added date format: $str")
                isError(true)
                null
            }
        }
    } catch (e: DateTimeParseException) {
        Timber.tag("BookImport").e("Failed to parse added date: $str with error $e")
        isError(true)
        null
    }
}

fun formatEventTime(event: CalendarEvent): String {
    return if (event.allDay) {
        // all day event only on current day
        if (event.dateStart == event.dateEnd || event.dateEnd <= 0L) {
            "All day"
        } else {
            // all day event that spans multiple days
            "${displayDate(event.dateStart, DateFormat.DAY_MONTH)} " +
                    "- ${displayDate(event.dateEnd, DateFormat.DAY_MONTH)}"
        }
    } else {
        // no end date -> just display start time
        if (event.dateEnd <= 0L) {
            formatMinutes(event.minuteStart)
        } else {
            val sameDay = run {
                val start = Calendar.getInstance().apply { timeInMillis = event.dateStart }
                val end = Calendar.getInstance().apply { timeInMillis = event.dateEnd }
                start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR) &&
                        start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
            }
            // same day -> just display times
            if (sameDay) {
                "${formatMinutes(event.minuteStart)} - ${formatMinutes(event.minuteEnd)}"
            } else {
                // multiple days with times
                "${displayDate(event.dateStart, DateFormat.DAY_MONTH)}, " +
                        "${formatMinutes(event.minuteStart)} " +
                        "- ${displayDate(event.dateEnd, DateFormat.DAY_MONTH)}, " +
                        formatMinutes(event.minuteEnd)
            }
        }
    }
}

fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

fun formatReminderTime(context: Context, hasReminder: Boolean, reminderTime: Long): String {
    if (!hasReminder) return context.getString(R.string.reminder_none)
    return when (reminderTime) {
        0L -> context.getString(R.string.reminder_onTime)
        10 * 60 * 1000L -> context.getString(R.string.reminder_10before)
        60 * 60 * 1000L -> context.getString(R.string.reminder_1hbefore)
        24 * 60 * 60 * 1000L -> context.getString(R.string.reminder_1dbefore)
        else -> {
            val totalMinutes = reminderTime / (60 * 1000L)
            if (totalMinutes % 60 == 0L) {
                context.getString(R.string.reminder_customHours, totalMinutes / 60)
            } else {
                context.getString(R.string.reminder_customMinutes, totalMinutes)
            }
        }
    }
}