package com.trickcal.crayon.domain.damage

import org.junit.Assert.assertEquals
import org.junit.Test

class DamageCalculatorEngineTest {
    @Test
    fun calculate_matchesWorkbookSnapshotForDefaultInputs() {
        val result = DamageCalculatorEngine.calculate(defaultInputs())

        assertEquals(1200.0, result.rowValue("attack"), 0.0)
        assertEquals(800.0, result.rowValue("defense"), 0.0)
        assertEquals(106.153846153846, result.rowValue("damage_curve"), 1e-12)
        assertEquals(0.371428571428571, result.rowValue("critical_rate"), 1e-12)
        assertEquals(0.85, result.rowValue("critical_mult_rate"), 1e-12)
        assertEquals(9721.0, result.rowValue("intermediate_damage"), 0.0)
        assertEquals(11179.0, result.rowValue("final_damage"), 0.0)
    }

    @Test
    fun calculate_usesNonCriticalMultiplierWhenIsCriticalIsZero() {
        val result = DamageCalculatorEngine.calculate(defaultInputs().copy(isCritical = 0.0))

        assertEquals(1.0, result.rowValue("critical_multiplier"), 0.0)
    }

    @Test
    fun calculate_appliesAttackAndDefenseFloors() {
        val result = DamageCalculatorEngine.calculate(
            defaultInputs().copy(
                attackInput = -10.0,
                defenseInput = -20.0,
            ),
        )

        assertEquals(1.0, result.rowValue("attack"), 0.0)
        assertEquals(0.0, result.rowValue("defense"), 0.0)
    }

    @Test
    fun calculate_clampsDamageCurveMultiplierToConfiguredRange() {
        val minClampedResult = DamageCalculatorEngine.calculate(
            inputs = defaultInputs(),
            constants = DamageCalculatorConstants(
                limitDamageConstMin = 1.1,
                limitDamageConstMax = 1.5,
            ),
        )
        val maxClampedResult = DamageCalculatorEngine.calculate(
            inputs = defaultInputs(),
            constants = DamageCalculatorConstants(limitDamageConstMax = 0.5),
        )

        assertEquals(1320.0, minClampedResult.rowValue("base_attack_damage"), 1e-12)
        assertEquals(600.0, maxClampedResult.rowValue("base_attack_damage"), 1e-12)
    }

    @Test
    fun calculate_clampsCriticalRateAndCriticalMultiplierRanges() {
        val upperClampedResult = DamageCalculatorEngine.calculate(
            defaultInputs().copy(
                criticalStat = 100_000.0,
                criticalResist = 1.0,
                criticalRateBonus = 1_000.0,
                criticalMultStat = 100_000.0,
                criticalMultResist = 1.0,
                criticalMultBonus = 1_000.0,
            ),
        )
        val lowerClampedResult = DamageCalculatorEngine.calculate(
            defaultInputs().copy(
                criticalStat = 0.0,
                criticalResist = 100_000.0,
                criticalRateBonus = -1_000.0,
                criticalMultStat = 0.0,
                criticalMultResist = 100_000.0,
                criticalMultBonus = -1_000.0,
            ),
        )

        assertEquals(0.75, upperClampedResult.rowValue("critical_rate"), 1e-12)
        assertEquals(1.5, upperClampedResult.rowValue("critical_mult_rate"), 1e-12)
        assertEquals(0.05, lowerClampedResult.rowValue("critical_rate"), 1e-12)
        assertEquals(0.2, lowerClampedResult.rowValue("critical_mult_rate"), 1e-12)
    }

    @Test
    fun calculate_clampsNormalAndEndDamageMultipliersToMinimum() {
        val result = DamageCalculatorEngine.calculate(
            defaultInputs().copy(
                typeDamageBase = -10.0,
                skillDamageRate = 0.0,
                endDamageBase = -10.0,
                endDamageExtra = 0.0,
            ),
        )

        assertEquals(0.2, result.rowValue("normal_damage_multiplier"), 1e-12)
        assertEquals(0.2, result.rowValue("end_damage_multiplier"), 1e-12)
    }

    private fun defaultInputs(): DamageCalculatorParsedInputs =
        DamageCalculatorParsedInputs(
            attackInput = 1200.0,
            defenseInput = 800.0,
            inputCoefficient = 250.0,
            addDamageCoefficient = 0.0,
            effectDamage = 1.1,
            personalityBonus = 0.2,
            typeDamageBase = 1.0,
            skillDamageRate = 0.25,
            criticalStat = 150.0,
            criticalResist = 100.0,
            criticalRateBonus = 0.0,
            criticalRateReceiveBonus = 0.0,
            criticalMultStat = 140.0,
            criticalMultResist = 100.0,
            criticalMultBonus = 0.0,
            criticalMultReceiveBonus = 0.0,
            isCritical = 1.0,
            endDamageBase = 0.1,
            endDamageExtra = 5.0,
        )
}
