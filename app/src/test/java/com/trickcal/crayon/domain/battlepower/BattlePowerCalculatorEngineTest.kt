package com.trickcal.crayon.domain.battlepower

import org.junit.Assert.assertEquals
import org.junit.Test

class BattlePowerCalculatorEngineTest {
    @Test
    fun calculate_matchesWorkbookSnapshotForDefaultInputs() {
        val result = BattlePowerCalculatorEngine.calculate(defaultInputs())

        assertEquals(53080.0, result.rowValue("main_attack"), 0.0)
        assertEquals(114283.0, result.rowValue("defense_sum"), 0.0)
        assertEquals(0.699999988079071, result.rowValue("aside_bonus"), 1e-12)
        assertEquals(297747.9, result.rowValue("base_score"), 1e-9)
        assertEquals(2.55999999165535, result.rowValue("growth_multiplier"), 1e-12)
        assertEquals(762234.0, result.rowValue("power"), 0.0)
    }

    @Test
    fun calculate_usesMagicAttackWhenAttackTypeIsMagic() {
        val result = BattlePowerCalculatorEngine.calculate(
            defaultInputs().copy(attackType = "魔法"),
        )

        assertEquals(4262.0, result.rowValue("main_attack"), 0.0)
    }

    @Test
    fun calculate_ignoresAsideCoefficientWhenAsideGradeBelowTwo() {
        val result = BattlePowerCalculatorEngine.calculate(
            defaultInputs().copy(asideGrade = 1.0),
        )

        assertEquals(0.0, result.rowValue("aside_bonus"), 0.0)
    }

    @Test
    fun calculate_truncatesFinalPowerTowardZero() {
        val result = BattlePowerCalculatorEngine.calculate(
            defaultInputs().copy(
                attackPhysic = 1.0,
                attackMagic = 1.0,
                defensePhysic = 0.0,
                defenseMagic = 0.0,
                hp = 0.0,
                criticalRate = 0.0,
                criticalMult = 0.0,
                criticalResist = 0.0,
                criticalMultResist = 0.0,
                attackSpeed = 0.0,
                spSkillLevel = 0.0,
                ultimateSkillLevel = 0.0,
                passiveSkillLevel = 0.0,
                weightValueA = 0.0,
                asideGrade = 0.0,
                asideValueA = 0.0,
            ),
            constants = BattlePowerCalculatorConstants(attackWeight301 = 1.9),
        )

        assertEquals(1.0, result.rowValue("power"), 0.0)
    }

    private fun defaultInputs(): BattlePowerCalculatorParsedInputs =
        BattlePowerCalculatorParsedInputs(
            attackType = "物理",
            attackPhysic = 53080.0,
            attackMagic = 4262.0,
            defensePhysic = 57104.0,
            defenseMagic = 57179.0,
            hp = 553870.0,
            criticalRate = 23099.0,
            criticalMult = 22942.0,
            criticalResist = 21198.0,
            criticalMultResist = 21207.0,
            attackSpeed = 100.0,
            spSkillLevel = 12.0,
            ultimateSkillLevel = 7.0,
            passiveSkillLevel = 7.0,
            activeSkillValueA = 0.02,
            ultimateSkillValueA = 0.02,
            passiveValueA = 0.02,
            weightValueA = 0.340000003576279,
            asideGrade = 2.0,
            asideValueA = 0.699999988079071,
        )
}
