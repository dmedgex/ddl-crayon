package com.trickcal.crayon.feature.list

import com.trickcal.crayon.data.local.DemoCharacterCatalog
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCardUiMapperTest {
    @Test
    fun toCardUi_preservesSlotOrderAndLitStatePerLayer() {
        val character = DemoCharacterCatalog.characters.first { it.id == "aiko" }
        val firstLayer = checkNotNull(character.layerForTier(BoardTier.FIRST))
        val litSlotId = firstLayer.attributeSlots.last().id

        val cardUi = character.toCardUi(setOf(litSlotId))
        val firstLayerSummary = cardUi.layerSummaries.first { it.tier == BoardTier.FIRST }

        assertEquals(firstLayer.attributeSlots.size, firstLayerSummary.slots.size)
        assertEquals(
            firstLayer.attributeSlots.map { it.id },
            firstLayerSummary.slots.map { it.slotId },
        )
        assertEquals(
            firstLayer.attributeSlots.map { it.attributeType },
            firstLayerSummary.slots.map { it.attributeType },
        )
        assertFalse(firstLayerSummary.slots.first().isLit)
        assertTrue(firstLayerSummary.slots.last().isLit)
        assertEquals(cardUi.totalCount, cardUi.layerSummaries.sumOf { it.slots.size })
        assertFalse(cardUi.isCompactDimmed)
    }

    @Test
    fun toCardUi_marksCompactCardDimmed_whenSingleTierAndSingleAttributeAreSelectedButUnlit() {
        val character = DemoCharacterCatalog.characters.first { it.id == "aiko" }
        val firstLayer = checkNotNull(character.layerForTier(BoardTier.FIRST))
        val targetSlot = firstLayer.attributeSlots.first()

        val cardUi = character.toCardUi(
            litSlots = emptySet(),
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(targetSlot.attributeType),
                selectedPersonality = PersonalityType.FIRE,
            ),
        )

        assertTrue(cardUi.isCompactDimmed)
    }

    @Test
    fun toCardUi_keepsCompactCardNormal_whenSingleTierAndSingleAttributeHaveBeenLit() {
        val character = DemoCharacterCatalog.characters.first { it.id == "aiko" }
        val firstLayer = checkNotNull(character.layerForTier(BoardTier.FIRST))
        val targetSlot = firstLayer.attributeSlots.first()

        val cardUi = character.toCardUi(
            litSlots = setOf(targetSlot.id),
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(targetSlot.attributeType),
            ),
        )

        assertFalse(cardUi.isCompactDimmed)
    }

    @Test
    fun toCardUi_doesNotDimCompactCard_whenMoreThanOneAttributeIsSelected() {
        val character = DemoCharacterCatalog.characters.first { it.id == "aiko" }

        val cardUi = character.toCardUi(
            litSlots = emptySet(),
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(AttributeType.ATTACK, AttributeType.CRIT),
            ),
        )

        assertFalse(cardUi.isCompactDimmed)
    }
}
