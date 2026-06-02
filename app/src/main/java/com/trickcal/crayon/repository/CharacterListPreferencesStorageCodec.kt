package com.trickcal.crayon.repository

import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.BrowseMode
import com.trickcal.crayon.model.CharacterDisplayMode
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.CharacterListPreferences
import com.trickcal.crayon.model.PersonalityType

internal data class StoredCharacterListPreferences(
    val browseMode: String? = null,
    val displayMode: String? = null,
    val nameQuery: String? = null,
    val selectedTiers: Set<String>? = null,
    val selectedAttributes: Set<String>? = null,
    val selectedPersonality: String? = null,
)

internal object CharacterListPreferencesStorageCodec {
    fun encode(preferences: CharacterListPreferences): StoredCharacterListPreferences =
        StoredCharacterListPreferences(
            browseMode = preferences.browseMode.name,
            displayMode = preferences.displayMode.name,
            nameQuery = preferences.filter.nameQuery.takeIf { it.isNotBlank() },
            selectedTiers = preferences.filter.selectedTiers.mapTo(linkedSetOf()) { it.name }
                .takeIf { it.isNotEmpty() },
            selectedAttributes = preferences.filter.selectedAttributes.mapTo(linkedSetOf()) { it.name }
                .takeIf { it.isNotEmpty() },
            selectedPersonality = preferences.filter.selectedPersonality?.name,
        )

    fun decode(stored: StoredCharacterListPreferences): CharacterListPreferences =
        CharacterListPreferences(
            filter = CharacterFilter(
                nameQuery = stored.nameQuery.orEmpty(),
                selectedTiers = decodeEnumSet(stored.selectedTiers),
                selectedAttributes = decodeEnumSet(stored.selectedAttributes),
                selectedPersonality = decodeEnum<PersonalityType>(stored.selectedPersonality),
            ),
            browseMode = decodeEnum<BrowseMode>(stored.browseMode) ?: BrowseMode.DETAIL,
            displayMode = decodeEnum<CharacterDisplayMode>(stored.displayMode) ?: CharacterDisplayMode.DETAIL,
        )

    private inline fun <reified T : Enum<T>> decodeEnum(value: String?): T? =
        enumValues<T>().firstOrNull { entry -> entry.name == value }

    private inline fun <reified T : Enum<T>> decodeEnumSet(values: Set<String>?): Set<T> =
        values
            .orEmpty()
            .mapNotNullTo(linkedSetOf()) { value -> decodeEnum<T>(value) }
}
