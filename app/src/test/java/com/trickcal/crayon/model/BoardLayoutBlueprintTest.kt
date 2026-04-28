package com.trickcal.crayon.model

import com.trickcal.crayon.data.local.BoardLayerAssembler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardLayoutBlueprintTest {
    @Test
    fun unlockableCellCount_onlyCountsUppercaseAttributeCells() {
        val blueprint = BoardLayoutBlueprint(
            rows = listOf(
                "..SPW..",
                ".WaKR..",
                ".hdPE..",
            ),
        )

        assertEquals(2, blueprint.unlockableCellCount())
    }

    @Test
    fun displayOnlyAttributeCount_onlyCountsLowercasePurpleAttributeCells() {
        val blueprint = BoardLayoutBlueprint(
            rows = listOf(
                "..SPW..",
                ".WaKR..",
                ".hdrE..",
            ),
        )

        assertEquals(4, blueprint.displayOnlyAttributeCount())
    }

    @Test
    fun startEndAndPurpleDisplayCells_doNotGenerateSlotIds() {
        val layer = BoardLayerAssembler.assemble(
            characterId = "test",
            tier = BoardTier.FIRST,
            rows = listOf(
                "..SPW..",
                ".WaPW..",
                "WPAKEP.",
                ".WPrW..",
            ),
        )

        val startCell = layer.cells.first { it.type == BoardCellType.START }
        val endCell = layer.cells.first { it.type == BoardCellType.END }
        val purpleAttackCell = layer.cells.first { it.type == BoardCellType.PURPLE_ATTACK }
        val purpleCritResistCell = layer.cells.first { it.type == BoardCellType.PURPLE_CRIT_RESIST }

        assertNull(startCell.slotId)
        assertNull(endCell.slotId)
        assertNull(purpleAttackCell.slotId)
        assertNull(purpleCritResistCell.slotId)
        assertEquals(2, layer.attributeSlots.size)
        assertEquals(2, layer.displayOnlyAttributes().size)
    }
}
