package com.example.bookbuddies.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val THEME = "theme"
private val Context.dataStore by preferencesDataStore(name = "settings")
/**
 * The DataStore contains locally-stored information.
 * Here it contains user preferences for the theme, stored in the DataStore under SETTINGS
 *
 * @property context used to access the DataStore
 */
class DataStoreManager(private val context: Context) {
    private val themeKey = stringPreferencesKey(THEME)

    // Theme preference
    val themeChoice: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[themeKey] ?: ThemeChoice.SYSTEM_DEFAULT.name
        }

    /**
     * Updates the DataStore with the user's preferred theme.
     *
     * @param themeChoice SYSTEM_DEFAULT, LIGHT or DARK
     */
    suspend fun setThemeChoice(themeChoice: ThemeChoice) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeChoice.name

        }
    }
}

enum class ThemeChoice {
    SYSTEM_DEFAULT, LIGHT, DARK
}