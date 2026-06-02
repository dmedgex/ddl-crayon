package com.trickcal.crayon.feature.damage

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DamageCalculatorDashboardTest {
    @Test
    fun buildDamageResultRows_preservesOrderAndExcludesFinalDamage() {
        val rows = buildDamageResultRows(sampleOutputs())

        assertEquals(
            listOf(
                "attack",
                "defense",
                "x",
                "damage_curve",
                "base_attack_damage",
                "skill_total_coefficient",
                "xc",
                "critical_rate_base",
                "critical_rate",
                "xm",
                "critical_mult_base",
                "critical_mult_rate",
                "critical_multiplier",
                "normal_damage_multiplier",
                "end_damage_multiplier",
                "intermediate_damage",
            ),
            rows.map { it.id },
        )
        assertFalse(rows.any { it.id == "final_damage" })
    }

    @Test
    fun buildDamageResultRows_usesDocumentLikeLabelsForXRelatedFields() {
        val rows = buildDamageResultRows(sampleOutputs())

        assertEquals("攻防差值x", rows.first { it.id == "x" }.label)
        assertEquals("暴击差值xc", rows.first { it.id == "xc" }.label)
        assertEquals("暴伤差值xm", rows.first { it.id == "xm" }.label)
    }

    @Test
    fun compactResultCardWidth_returnsNarrowerCardAndCapsAtDesktopWidth() {
        assertEquals(331.2.dp, compactResultCardWidth(360.dp))
        assertEquals(440.dp, compactResultCardWidth(600.dp))
    }

    private fun sampleOutputs(): List<DamageCalculatorOutputRow> =
        listOf(
            output("attack", "攻击属性"),
            output("defense", "防御属性"),
            output("x", "x"),
            output("damage_curve", "伤害曲线(x)"),
            output("base_attack_damage", "基础攻击伤害"),
            output("skill_total_coefficient", "技能总系数"),
            output("xc", "xc"),
            output("critical_rate_base", "暴击率基础值"),
            output("critical_rate", "暴击率"),
            output("xm", "xm"),
            output("critical_mult_base", "暴伤基础值"),
            output("critical_mult_rate", "暴伤倍率"),
            output("critical_multiplier", "暴击乘区"),
            output("normal_damage_multiplier", "普通伤害乘区"),
            output("end_damage_multiplier", "终伤乘区"),
            output("intermediate_damage", "中间伤害"),
            output("final_damage", "最终伤害"),
        )

    private fun output(
        id: String,
        label: String,
    ): DamageCalculatorOutputRow =
        DamageCalculatorOutputRow(
            id = id,
            label = label,
            valueText = id,
            description = "$label 说明",
        )
}
