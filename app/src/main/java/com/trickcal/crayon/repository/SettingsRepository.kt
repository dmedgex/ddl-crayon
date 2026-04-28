package com.trickcal.crayon.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trickcal.crayon.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    fun observeThemeMode(): Flow<ThemeMode> =
        context.settingsDataStore.data.map { preferences ->
            ThemeMode.fromStorage(preferences[Keys.themeMode])
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.themeMode] = mode.name
        }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
    }
}
