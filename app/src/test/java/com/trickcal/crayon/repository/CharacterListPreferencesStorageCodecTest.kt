package com.trickcal.crayon.repository

import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.BrowseMode
import com.trickcal.crayon.model.CharacterDisplayMode
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.CharacterListPreferences
import com.trickcal.crayon.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterListPreferencesStorageCodecTest {
    @Test
    fun encodeAndDecode_roundTripsCharacterListPreferences() {
        val preferences = CharacterListPreferences(
            filter = CharacterFilter(
                nameQuery = "艾",
                selectedTiers = linkedSetOf(BoardTier.SECOND),
                selectedAttributes = linkedSetOf(AttributeType.ATTACK, AttributeType.CRIT),
                selectedPersonality = PersonalityType.FIRE,
            ),
            browseMode = BrowseMode.PAINT,
            displayMode = CharacterDisplayMode.COMPACT,
        )

        val encoded = CharacterListPreferencesStorageCodec.encode(preferences)
        val decoded = CharacterListPreferencesStorageCodec.decode(encoded)

        assertEquals(preferences, decoded)
    }

    @Test
    fun encode_omitsBlankAndEmptyOptionalFields() {
        val encoded = CharacterListPreferencesStorageCodec.encode(
            CharacterListPreferences(
                filter = CharacterFilter(nameQuery = ""),
            ),
        )

        assertEquals(BrowseMode.DETAIL.name, encoded.browseMode)
        assertEquals(CharacterDisplayMode.DETAIL.name, encoded.displayMode)
        assertNull(encoded.nameQuery)
        assertNull(encoded.selectedTiers)
        assertNull(encoded.selectedAttributes)
        assertNull(encoded.selectedPersonality)
    }

    @Test
    fun decode_fallsBackToDefaultsWhenStoredValuesAreInvalid() {
        val decoded = CharacterListPreferencesStorageCodec.decode(
            StoredCharacterListPreferences(
                browseMode = "BROKEN",
                displayMode = "UNKNOWN",
                nameQuery = null,
                selectedTiers = linkedSetOf("FIRST", "BAD_TIER"),
                selectedAttributes = linkedSetOf("ATTACK", "BAD_ATTR"),
                selectedPersonality = "NOPE",
            ),
        )

        assertEquals(
            CharacterListPreferences(
                filter = CharacterFilter(
                    selectedTiers = linkedSetOf(BoardTier.FIRST),
                    selectedAttributes = linkedSetOf(AttributeType.ATTACK),
                ),
                browseMode = BrowseMode.DETAIL,
                displayMode = CharacterDisplayMode.DETAIL,
            ),
            decoded,
        )
    }
}
