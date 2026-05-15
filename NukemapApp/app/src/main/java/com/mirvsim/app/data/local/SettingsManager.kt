package com.mirvsim.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "nukemap_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val MAP_TILE_SOURCE = stringPreferencesKey("map_tile_source")
        val AUTO_LAUNCH_PRESET = booleanPreferencesKey("auto_launch_preset")
        val POPUP_ENABLED = booleanPreferencesKey("popup_enabled")
        val RING_ANIMATION = booleanPreferencesKey("ring_animation")
    }

    val settingsFlow: Flow<androidx.datastore.preferences.core.Preferences> = context.dataStore.data


    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[IS_DARK_THEME] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setTileSource(source: String) {
        context.dataStore.edit { it[MAP_TILE_SOURCE] = source }
    }

    suspend fun setAutoLaunchPreset(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_LAUNCH_PRESET] = enabled }
    }

    suspend fun setPopupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[POPUP_ENABLED] = enabled }
    }

    suspend fun setRingAnimation(enabled: Boolean) {
        context.dataStore.edit { it[RING_ANIMATION] = enabled }
    }
}
