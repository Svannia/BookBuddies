package com.appbuddies.bookbuddies.data

import android.content.Context
import androidx.compose.ui.graphics.toColorLong
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.helpers.getLocalTimezone
import com.appbuddies.bookbuddies.ui.theme.Beige
import com.appbuddies.bookbuddies.ui.theme.Brown
import com.appbuddies.bookbuddies.ui.theme.Cerulean
import com.appbuddies.bookbuddies.ui.theme.Coral
import com.appbuddies.bookbuddies.ui.theme.DarkBlue
import com.appbuddies.bookbuddies.ui.theme.DarkGreen
import com.appbuddies.bookbuddies.ui.theme.Emerald
import com.appbuddies.bookbuddies.ui.theme.LightBlue
import com.appbuddies.bookbuddies.ui.theme.LightGreen
import com.appbuddies.bookbuddies.ui.theme.LightRed
import com.appbuddies.bookbuddies.ui.theme.Maroon
import com.appbuddies.bookbuddies.ui.theme.Mauve
import com.appbuddies.bookbuddies.ui.theme.MediumGrey
import com.appbuddies.bookbuddies.ui.theme.Orange
import com.appbuddies.bookbuddies.ui.theme.PaleYellow
import com.appbuddies.bookbuddies.ui.theme.PastelOrange
import com.appbuddies.bookbuddies.ui.theme.Pink
import com.appbuddies.bookbuddies.ui.theme.Purple
import com.appbuddies.bookbuddies.ui.theme.Rose
import com.appbuddies.bookbuddies.ui.theme.Teal
import com.appbuddies.bookbuddies.ui.theme.Violet
import com.appbuddies.bookbuddies.ui.theme.Yellow
import timber.log.Timber
import java.util.UUID

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    val title: String,
    val allDay: Boolean,
    val timezone: String,
    val dateStart: Long,
    val dateEnd: Long,
    val minuteStart: Int, // minutes since midnight
    val minuteEnd: Int,
    val location: String,
    val notes: String,
    val tag: String
) {
    companion object {
        /**
         * Creates an empty CalendarEvent data object.
         *
         * @return empty CalendarEvent data object.
         */
        fun empty(dateStart: Long): CalendarEvent {


            return CalendarEvent(UUID.randomUUID().toString().replace("-", ""),
                "", false, getLocalTimezone().label, dateStart, 0L, 12*60, 0,
                "", "", ""
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

val TAG_COLOURS = listOf(
    Rose, Pink, Violet, Mauve, Purple, DarkBlue, Cerulean, LightBlue, Teal,
    LightGreen, Emerald, DarkGreen, Brown, Beige, Maroon,
    LightRed, Coral, Orange, PastelOrange, Yellow, PaleYellow
)

/**
 * Finds the EventTag associated with a tag ID.
 *
 * @param tagID the ID of the tag to find
 * @param tags the list of EventTags to search through
 * @return the EventTag associated with the CalendarEvent, or the default tag if not found
 */
fun getTagForEvent(context: Context, tagID: String, tags: List<EventTag>): EventTag {
    return tags.find { it.uid == tagID }
        ?: tags.find { it.isDefault }
        ?: run {
            Timber.tag("Error").w("No tag found for tag ID $tagID and no default tag exists. Using hardcoded fallback.")
            EventTag(name = context.getString(R.string.tag_default), colour = MediumGrey.toColorLong(), isDefault = true)
        }
}
