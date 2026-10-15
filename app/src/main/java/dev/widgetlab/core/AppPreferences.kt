package dev.widgetlab.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("lab-settings")
class AppPreferences(private val context: Context) {
    private val key = booleanPreferencesKey("auto-racing-refresh")
    val automatic = context.settings.data.map { it[key] ?: true }
    suspend fun automatic(enabled: Boolean) { context.settings.edit { it[key] = enabled } }
}
