package com.example.bookbuddies.system

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.helpers.TIMEZONES
import com.example.bookbuddies.helpers.Timezone
import com.example.bookbuddies.helpers.convertToLocal
import com.example.bookbuddies.helpers.getLocalTimezone
import com.example.bookbuddies.helpers.getTimezoneOffset
import timber.log.Timber

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val channelId = "bookbuddies_events"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setAutoCancel(true)
            .build()

        manager.notify(title.hashCode(), notification)
    }
}

/**
 * Schedules a notification at the chosen time before an event.
 * If the event's timezone is different, the notification's time is converted to local time.
 * No notification is scheduled if there is not set reminder or if the reminder's time is past already.
 *
 * @param context for Intent
 * @param event CalendarEvent object to schedule a notification for
 */
fun scheduleEventNotification(context: Context, event: CalendarEvent) {
    if (!event.reminder || event.reminderTime < 0L) return

    // get event start time in local timezone
    val eventTimezone = TIMEZONES.find { it.label == event.timezone }
        ?: Timezone(event.timezone, event.timezone)
    val localOffset = getLocalTimezone().offset
    val (localDate, localMinutes) = if (getTimezoneOffset(event.timezone) != localOffset) {
        convertToLocal(event.dateStart, event.minuteStart, eventTimezone)
    } else {
        event.dateStart to event.minuteStart
    }

    // compute absolute trigger time
    val eventStartMillis = localDate + localMinutes * 60 * 1000L
    val triggerMillis = eventStartMillis - event.reminderTime
    if (triggerMillis <= System.currentTimeMillis()) return  // already past

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", event.title)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        event.uid.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                // fallback to inexact alarm if permission not granted
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    } catch (e: SecurityException) {
        Timber.tag("Notifications").e("Failed to schedule exact alarm with error: $e")
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
}

/**
 * Fetches the existing scheduled notification for an event and cancels it.
 *
 * @param context for Intent
 * @param event CalendarEvent object to cancel the notification for
 */
fun cancelEventNotification(context: Context, event: CalendarEvent) {
    val intent = Intent(context, NotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        event.uid.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pendingIntent)
}