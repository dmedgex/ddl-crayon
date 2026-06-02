package com.trickcal.crayon.feature.battlepower

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BattlePowerCalculatorResultsTest {
    @Test
    fun buildBattlePowerResultRows_preservesOrderAndExcludesPower() {
        val rows = buildBattlePowerResultRows(sampleOutputs())

        assertEquals(
            listOf(
                "main_attack",
                "defense_sum",
                "aside_bonus",
                "base_score",
                "growth_multiplier",
            ),
            rows.map { it.id },
        )
        assertFalse(rows.any { it.id == "power" })
    }

    @Test
    fun compactBattlePowerResultCardWidth_returnsNarrowerCardAndCapsAtDesktopWidth() {
        assertEquals(331.2.dp, compactBattlePowerResultCardWidth(360.dp))
        assertEquals(440.dp, compactBattlePowerResultCardWidth(600.dp))
    }

    private fun sampleOutputs(): List<BattlePowerCalculatorOutputRow> =
        listOf(
            output("main_attack", "主攻击"),
            output("defense_sum", "双防合计"),
            output("aside_bonus", "坨格加成"),
            output("base_score", "基础分"),
            output("growth_multiplier", "成长乘区"),
            output("power", "战斗力"),
        )

    private fun output(
        id: String,
        label: String,
    ): BattlePowerCalculatorOutputRow =
        BattlePowerCalculatorOutputRow(
            id = id,
            label = label,
            valueText = id,
            description = "$label 说明",
        )
}
