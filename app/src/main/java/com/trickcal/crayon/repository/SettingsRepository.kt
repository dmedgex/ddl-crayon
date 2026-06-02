package com.trickcal.crayon.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trickcal.crayon.model.CharacterListPreferences
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

    fun observeCharacterListPreferences(): Flow<CharacterListPreferences> =
        context.settingsDataStore.data.map { preferences ->
            CharacterListPreferencesStorageCodec.decode(
                StoredCharacterListPreferences(
                    browseMode = preferences[Keys.characterListBrowseMode],
                    displayMode = preferences[Keys.characterListDisplayMode],
                    nameQuery = preferences[Keys.characterListNameQuery],
                    selectedTiers = preferences[Keys.characterListSelectedTiers],
                    selectedAttributes = preferences[Keys.characterListSelectedAttributes],
                    selectedPersonality = preferences[Keys.characterListSelectedPersonality],
                ),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.themeMode] = mode.name
        }
    }

    suspend fun setCharacterListPreferences(characterListPreferences: CharacterListPreferences) {
        val stored = CharacterListPreferencesStorageCodec.encode(characterListPreferences)
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.characterListBrowseMode] = checkNotNull(stored.browseMode)
            preferences[Keys.characterListDisplayMode] = checkNotNull(stored.displayMode)

            updateStringPreference(
                preferences = preferences,
                key = Keys.characterListNameQuery,
                value = stored.nameQuery,
            )
            updateStringSetPreference(
                preferences = preferences,
                key = Keys.characterListSelectedTiers,
                value = stored.selectedTiers,
            )
            updateStringSetPreference(
                preferences = preferences,
                key = Keys.characterListSelectedAttributes,
                value = stored.selectedAttributes,
            )
            updateStringPreference(
                preferences = preferences,
                key = Keys.characterListSelectedPersonality,
                value = stored.selectedPersonality,
            )
        }
    }

    private fun updateStringPreference(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }

    private fun updateStringSetPreference(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>,
        value: Set<String>?,
    ) {
        if (value == null) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val characterListBrowseMode = stringPreferencesKey("character_list_browse_mode")
        val characterListDisplayMode = stringPreferencesKey("character_list_display_mode")
        val characterListNameQuery = stringPreferencesKey("character_list_name_query")
        val characterListSelectedTiers = stringSetPreferencesKey("character_list_selected_tiers")
        val characterListSelectedAttributes = stringSetPreferencesKey("character_list_selected_attributes")
        val characterListSelectedPersonality = stringPreferencesKey("character_list_selected_personality")
    }
}
