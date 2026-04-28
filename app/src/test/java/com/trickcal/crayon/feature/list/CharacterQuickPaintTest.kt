package com.trickcal.crayon.feature.list

import com.trickcal.crayon.data.local.BoardLayerAssembler
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterQuickPaintTest {
    @Test
    fun resolveQuickPaintTarget_unlocksAllMatchingSlotsWhenNothingIsLit() {
        val character = duplicateAttackCharacter()

        val target = character.resolveQuickPaintTarget(
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(AttributeType.ATTACK),
            ),
            litSlots = emptySet(),
        )

        assertNotNull(target)
        assertEquals(
            setOf(
                "quick_first_attack_0",
                "quick_first_attack_1",
            ),
            target?.slotIds,
        )
        assertTrue(target?.shouldUnlock == true)
    }

    @Test
    fun resolveQuickPaintTarget_locksAllMatchingSlotsWhenAnyMatchingSlotIsLit() {
        val character = duplicateAttackCharacter()

        val target = character.resolveQuickPaintTarget(
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST),
                selectedAttributes = setOf(AttributeType.ATTACK),
            ),
            litSlots = setOf("quick_first_attack_1"),
        )

        assertNotNull(target)
        assertEquals(
            setOf(
                "quick_first_attack_0",
                "quick_first_attack_1",
            ),
            target?.slotIds,
        )
        assertFalse(target?.shouldUnlock == true)
    }

    @Test
    fun resolveQuickPaintTarget_returnsNullWhenSelectionIsNotSingleTierAndSingleAttribute() {
        val character = duplicateAttackCharacter()

        val target = character.resolveQuickPaintTarget(
            filter = CharacterFilter(
                selectedTiers = setOf(BoardTier.FIRST, BoardTier.SECOND),
                selectedAttributes = setOf(AttributeType.ATTACK),
            ),
            litSlots = emptySet(),
        )

        assertNull(target)
    }
}

private fun duplicateAttackCharacter(): CharacterProfile =
    CharacterProfile(
        id = "quick",
        name = "Quick",
        avatarKey = "avatar_quick",
        personality = PersonalityType.FIRE,
        layers = listOf(
            BoardLayerAssembler.assemble(
                characterId = "quick",
                tier = BoardTier.FIRST,
                rows = listOf("AA", "H."),
            ),
            BoardLayerAssembler.assemble(
                characterId = "quick",
                tier = BoardTier.SECOND,
                rows = listOf("D"),
            ),
            BoardLayerAssembler.assemble(
                characterId = "quick",
                tier = BoardTier.THIRD,
                rows = listOf("K"),
            ),
        ),
    )
