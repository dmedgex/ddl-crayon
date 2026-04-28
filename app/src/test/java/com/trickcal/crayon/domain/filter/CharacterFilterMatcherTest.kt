package com.trickcal.crayon.domain.filter

import com.trickcal.crayon.data.local.DemoCharacterCatalog
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterFilterMatcherTest {
    private val characters = DemoCharacterCatalog.characters

    @Test
    fun emptyFilter_returnsAllCharacters() {
        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, CharacterFilter())
        }

        assertEquals(characters.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun firstLayerWithHealth_matchesOnlyCharactersWhoseFirstLayerContainsHealth() {
        val filter = CharacterFilter(
            selectedTiers = setOf(BoardTier.FIRST),
            selectedAttributes = setOf(AttributeType.HEALTH),
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("aiko", "shir", "kaiser", "lia"),
            result.map { it.id }.toSet(),
        )
    }

    @Test
    fun attributeOnlyFilter_checksAcrossAllLayers() {
        val filter = CharacterFilter(
            selectedAttributes = setOf(AttributeType.CRIT_RESIST),
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("aiko", "roki", "tana", "kaiser", "noel", "vega", "lia", "selene"),
            result.map { it.id }.toSet(),
        )
    }

    @Test
    fun multipleTiersAndAttributes_requireAllSelectedAttributesInSameCandidateLayer() {
        val filter = CharacterFilter(
            selectedTiers = setOf(BoardTier.FIRST, BoardTier.SECOND),
            selectedAttributes = setOf(AttributeType.ATTACK, AttributeType.CRIT),
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("roki", "shir", "flora", "vega"),
            result.map { it.id }.toSet(),
        )
    }

    @Test
    fun multipleAttributesWithoutTierFilter_requireOneLayerToContainAllAttributes() {
        val filter = CharacterFilter(
            selectedAttributes = setOf(AttributeType.HEALTH, AttributeType.CRIT_RESIST),
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("aiko", "tana", "kaiser", "noel", "lia", "selene"),
            result.map { it.id }.toSet(),
        )
    }

    @Test
    fun nameQuery_matchesCharactersByPartialNameIgnoringCase() {
        val filter = CharacterFilter(
            nameQuery = "洛",
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("roki"),
            result.map { it.id }.toSet(),
        )
    }

    @Test
    fun selectedPersonality_requiresExactSinglePersonalityMatch() {
        val filter = CharacterFilter(
            selectedPersonality = PersonalityType.GRASS,
        )

        val result = characters.filter { character ->
            CharacterFilterMatcher.matches(character, filter)
        }

        assertEquals(
            setOf("kaiser", "selene"),
            result.map { it.id }.toSet(),
        )
    }
}
