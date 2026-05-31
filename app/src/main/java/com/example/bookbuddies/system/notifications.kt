package com.example.bookbuddies.system

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.bookbuddies.MainActivity
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.datastore.DatabaseProvider
import com.example.bookbuddies.helpers.TIMEZONES
import com.example.bookbuddies.helpers.Timezone
import com.example.bookbuddies.helpers.convertToLocal
import com.example.bookbuddies.helpers.getLocalTimezone
import com.example.bookbuddies.helpers.getTimezoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag("Debug").d("onReceive called")
        if (intent.component?.packageName != context.packageName) return
        val title = intent.getStringExtra("title") ?: return
        val eventID = intent.getStringExtra("eventID") ?: return
        val reminderTime = intent.getLongExtra("reminderTime", 0L)
        val expandedText = when {
            reminderTime <= 0L -> context.getString(R.string.notif_atTime)
            else -> {
                val totalMinutes = reminderTime / (60 * 1000L)
                if (totalMinutes < 60) context.getString(R.string.notif_minutesBefore, totalMinutes)
                else context.getString(R.string.notif_hoursBefore, totalMinutes / 60)
            }
        }


        val channelId = "bookbuddies_events"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("eventID", eventID)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventID.hashCode(), openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(expandedText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(title.hashCode(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Timber.tag("Debug").d("Boot completed, rescheduling notifications")

        // get the database and reschedule all events with reminders
        val db = DatabaseProvider.getDatabase(context)
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            db.calendarEventDao().getAllEvents().first()
                .filter { it.reminder && it.reminderTime >= 0L }
                .forEach { scheduleEventNotification(context, it) }
        }
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
    Timber.tag("Debug").d("scheduleEventNotification called for '${event.title}', reminder=${event.reminder}, reminderTime=${event.reminderTime}")
    if (!event.reminder || event.reminderTime < 0L) {
        Timber.tag("Debug").d("Skipping - no reminder set")
        return
    }

    // get event start time in local timezone
    val eventTimezone = TIMEZONES.find { it.label == event.timezone }
        ?: Timezone(event.timezone, event.timezone)
    val localOffset = getLocalTimezone().offset
    val (localDate, localMinutes) = if (getTimezoneOffset(event.timezone) != localOffset) {
        convertToLocal(event.dateStart, event.minuteStart, eventTimezone)
    } else {
        event.dateStart to event.minuteStart
    }

    // compute absolute trigger time (rounded down to nearest minute)
    val eventStartMillis = localDate + localMinutes * 60 * 1000L
    val triggerMillis = ((eventStartMillis - event.reminderTime) / 60_000L) * 60_000L
    Timber.tag("Debug").d("eventStartMillis=$eventStartMillis triggerMillis=$triggerMillis now=${System.currentTimeMillis()} diff=${triggerMillis - System.currentTimeMillis()}ms")

    // already past
    if (triggerMillis <= System.currentTimeMillis()) {
        Timber.tag("Debug").d("Skipping - trigger time is in the past")
        return
    }

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", event.title)
        putExtra("eventID", event.uid)
        putExtra("reminderTime", event.reminderTime)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        event.uid.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
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