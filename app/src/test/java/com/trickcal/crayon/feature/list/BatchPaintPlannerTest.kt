package com.trickcal.crayon.feature.list

import com.trickcal.crayon.data.local.DemoCharacterCatalog
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchPaintPlannerTest {
    private val aiko = DemoCharacterCatalog.characters.first { it.id == "aiko" }
    private val selene = DemoCharacterCatalog.characters.first { it.id == "selene" }

    @Test
    fun unlockPreview_matchesOnlySameTierAndAttribute_andSkipsAlreadyLitSlots() {
        val preview = BatchPaintPlanner.buildPreview(
            characters = listOf(aiko, selene),
            litSlots = setOf(aiko.findSlotId(BoardTier.FIRST, AttributeType.ATTACK)),
            selection = BatchPaintSelection(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(AttributeType.ATTACK),
                action = BatchPaintAction.UNLOCK,
            ),
        )

        assertEquals(1, preview.matchedCharacterCount)
        assertEquals(1, preview.matchedSlotCount)
        assertEquals(
            setOf(selene.findSlotId(BoardTier.FIRST, AttributeType.ATTACK)),
            preview.slotIds,
        )
    }

    @Test
    fun lockPreview_targetsOnlyCurrentlyLitSlotsWithinSelectedConditions() {
        val seleneAttack = selene.findSlotId(BoardTier.FIRST, AttributeType.ATTACK)
        val seleneDefense = selene.findSlotId(BoardTier.FIRST, AttributeType.DEFENSE)

        val preview = BatchPaintPlanner.buildPreview(
            characters = listOf(aiko, selene),
            litSlots = setOf(seleneAttack, seleneDefense),
            selection = BatchPaintSelection(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(AttributeType.ATTACK, AttributeType.DEFENSE),
                action = BatchPaintAction.LOCK,
            ),
        )

        assertEquals(1, preview.matchedCharacterCount)
        assertEquals(2, preview.matchedSlotCount)
        assertTrue(preview.slotIds.containsAll(setOf(seleneAttack, seleneDefense)))
    }
}

private fun CharacterProfile.findSlotId(
    tier: BoardTier,
    attributeType: AttributeType,
): String =
    checkNotNull(layerForTier(tier))
        .attributeSlots
        .first { it.attributeType == attributeType }
        .id
