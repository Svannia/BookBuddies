package com.appbuddies.bookbuddies.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.toColorLong
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appbuddies.bookbuddies.R
import com.appbuddies.bookbuddies.data.CalendarEvent
import com.appbuddies.bookbuddies.data.EventTag
import com.appbuddies.bookbuddies.datastore.CalendarRepository
import com.appbuddies.bookbuddies.ui.theme.MediumGrey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.core.content.edit
import com.appbuddies.bookbuddies.ui.theme.Cerulean
import com.appbuddies.bookbuddies.ui.theme.Coral
import com.appbuddies.bookbuddies.ui.theme.Emerald
import com.appbuddies.bookbuddies.ui.theme.Rose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.Calendar

class CalendarViewModel(private val repository: CalendarRepository, app: Application) : ViewModel() {
    val allEvents = repository.allEvents
    val allEventTags = repository.allEventTags

    /**
     * Fetches an observable FlowState of an event given its unique ID.
     *
     * @param uid event ID
     * @return Flow for the event
     */
    fun getEventFlowById(uid: String): Flow<CalendarEvent> {
        Timber.tag("CalendarVM").d("Recovering event flow with ID $uid")
        return allEvents.map { list -> list.find { it.uid == uid }}.filterNotNull()
    }

    // remember screen through navigation
    var savedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    var savedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1

    // store user preference to show read books progress in calendar or not
    private val _showReadBooks = MutableStateFlow(true)
    val showReadBooks: StateFlow<Boolean> = _showReadBooks
    /**
     * Toggles the visibility of read books in the calendar.
     */
    fun toggleShowReadBooks() {
        _showReadBooks.value = !_showReadBooks.value
        Timber.tag("CalendarVM").d("Toggled showReadBooks to ${_showReadBooks.value}")
    }

    // check for first launch to add example tags
    private val prefs = app.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    private val defaultTag = app.getString(R.string.tag_default) to MediumGrey
    private val exampleTagNames = listOf(
        app.getString(R.string.tag_bookRelease) to Rose,
        app.getString(R.string.tag_specialSale) to Cerulean,
        app.getString(R.string.tag_authorEvent) to Emerald,
        app.getString(R.string.tag_arcDeadline) to Coral
    )

    /**
     * WARNING: not consistency-safe, only use for testing/debugging.
     */
    fun resetTags() {
        viewModelScope.launch {
            Timber.tag("CalendarVM").d("First launch, inserting example tags.")
            // clear old existing tags just in case
            repository.deleteAllTags()
            // add basic default tag
            repository.insertEventTag(EventTag(
                name = defaultTag.first,
                colour = defaultTag.second.toColorLong(),
                isDefault = true
            ))
            // add example tags
            exampleTagNames.forEach { (name, colour) ->
                repository.insertEventTag(EventTag(name = name, colour = colour.toColorLong()))
            }
            prefs.edit { putBoolean("default_tags_inserted", true) }
        }
    }

    init {
        if (!prefs.getBoolean("default_tags_inserted", false)) {
            resetTags()
        }
    }

    // CALENDAR EVENTS

    /**
     * Inserts a new calendar event. If it already exists, it will be replaced.
     *
     * @param event CalendarEvent object to insert
     */
    fun insertEvent(event: CalendarEvent) = viewModelScope.launch {
        repository.insertEvent(event)
        Timber.tag("CalendarVM").d("Inserted event ${event.title}")
    }

    /**
     * Deletes a calendar event.
     *
     * @param context needed to cancel reminder notification
     * @param event CalendarEvent object to delete
     */
    fun deleteEvent(event: CalendarEvent) = viewModelScope.launch {
        repository.deleteEvent(event)
        Timber.tag("CalendarVM").d("Deleted event ${event.title}")
    }

    // EVENT TAGS

    /**
     * Inserts a new event tag. If it already exists, it will be replaced.
     *
     * @param tag EventTag object to insert
     */
    fun insertTag(tag: EventTag) = viewModelScope.launch {
        repository.insertEventTag(tag)
        Timber.tag("CalendarVM").d("Inserted tag ${tag.name}")
    }

    /**
     * Deletes an event tag.
     *
     * @param tag EventTag object to delete
     */
    fun deleteTag(tag: EventTag) = viewModelScope.launch {
        if (tag.isDefault) {
            Timber.tag("CalendarVM").d("Attempted to delete default tag, operation aborted.")
            return@launch
        }
        // assign default tag to all events that had this tag
        val defaultTag = repository.getDefaultTag()
        repository.updateEventsTag(oldTagUid = tag.uid, newTagUid = defaultTag.uid)

        repository.deleteEventTag(tag)
        Timber.tag("CalendarVM").d("Deleted tag ${tag.name}")
    }
}

class CalendarViewModelFactory(private val repository: CalendarRepository, private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
