package com.example.bookbuddies.viewModels

import android.app.Application
import androidx.compose.ui.graphics.toColorLong
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookbuddies.R
import com.example.bookbuddies.data.CalendarEvent
import com.example.bookbuddies.data.EventTag
import com.example.bookbuddies.datastore.CalendarRepository
import com.example.bookbuddies.ui.theme.DifferentPurple
import com.example.bookbuddies.ui.theme.LightGreen
import com.example.bookbuddies.ui.theme.LightRed
import com.example.bookbuddies.ui.theme.MediumBlue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class CalendarViewModel(private val repository: CalendarRepository, app: Application) : ViewModel() {
    val allEvents = repository.allEvents
    val allEventTags = repository.allEventTags

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

    // default tags (inserted if there are no tags)
    private val defaultTagNames = listOf(
        app.getString(R.string.tag_bookRelease) to DifferentPurple,
        app.getString(R.string.tag_specialSale) to MediumBlue,
        app.getString(R.string.tag_authorEvent) to LightGreen,
        app.getString(R.string.tag_arcDeadline) to LightRed
    )
    init {
        viewModelScope.launch {
            val tags = allEventTags.first()
            if (tags.isEmpty()) {
                Timber.tag("CalendarVM").d("No tags found, inserting defaults.")
                defaultTagNames.forEach { (name, colour) ->
                    repository.insertEventTag(EventTag(name = name, colour = colour.toColorLong()))
                }
            }
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