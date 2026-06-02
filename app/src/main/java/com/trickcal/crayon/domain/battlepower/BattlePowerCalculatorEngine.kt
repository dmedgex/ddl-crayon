package com.trickcal.crayon.domain.battlepower

import kotlin.math.abs

data class BattlePowerCalculatorFieldDefinition(
    val id: String,
    val label: String,
    val defaultValue: String,
    val description: String,
)

data class BattlePowerCalculatorParsedInputs(
    val attackType: String,
    val attackPhysic: Double,
    val attackMagic: Double,
    val defensePhysic: Double,
    val defenseMagic: Double,
    val hp: Double,
    val criticalRate: Double,
    val criticalMult: Double,
    val criticalResist: Double,
    val criticalMultResist: Double,
    val attackSpeed: Double,
    val spSkillLevel: Double,
    val ultimateSkillLevel: Double,
    val passiveSkillLevel: Double,
    val activeSkillValueA: Double,
    val ultimateSkillValueA: Double,
    val passiveValueA: Double,
    val weightValueA: Double,
    val asideGrade: Double,
    val asideValueA: Double,
)

data class BattlePowerCalculatorConstants(
    val attackWeight301: Double = 2.1,
    val defenseWeight302: Double = 0.7,
    val hpWeight303: Double = 0.08,
    val criticalRateWeight304: Double = 0.7,
    val criticalMultWeight305: Double = 0.7,
    val criticalResistWeight306: Double = 0.7,
    val criticalMultResistWeight307: Double = 0.7,
    val attackSpeedWeight308: Double = 0.6,
)

data class BattlePowerCalculationRow(
    val id: String,
    val label: String,
    val value: Double,
    val description: String,
)

data class BattlePowerCalculationResult(
    val rows: List<BattlePowerCalculationRow>,
) {
    fun rowValue(rowId: String): Double =
        rows.first { it.id == rowId }.value
}

object BattlePowerCalculatorEngine {
    val inputDefinitions: List<BattlePowerCalculatorFieldDefinition> = listOf(
        BattlePowerCalculatorFieldDefinition(
            id = "AttackPhysic",
            label = "物理攻击",
            defaultValue = "53080",
            description = "角色当前最终物理攻击值，已计入等级、装备、养成等加成后的结果",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "AttackMagic",
            label = "魔法攻击",
            defaultValue = "4262",
            description = "角色当前最终魔法攻击值，已计入等级、装备、养成等加成后的结果",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "DefensePhysic",
            label = "物理防御",
            defaultValue = "57104",
            description = "角色当前最终物理防御值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "DefenseMagic",
            label = "魔法防御",
            defaultValue = "57179",
            description = "角色当前最终魔法防御值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "Hp",
            label = "生命值",
            defaultValue = "553870",
            description = "角色当前最终生命值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "CriticalRate",
            label = "暴击率",
            defaultValue = "23099",
            description = "角色当前最终暴击率数值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "CriticalMult",
            label = "暴击伤害",
            defaultValue = "22942",
            description = "角色当前最终暴击伤害数值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "CriticalResist",
            label = "暴击抗性",
            defaultValue = "21198",
            description = "角色当前最终暴击抗性数值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "CriticalMultResist",
            label = "暴伤抗性",
            defaultValue = "21207",
            description = "角色当前最终暴伤抗性数值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "AttackSpeed",
            label = "攻击速度",
            defaultValue = "100",
            description = "角色当前最终攻击速度数值",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "SpSkillLevel",
            label = "低年级技能等级",
            defaultValue = "12",
            description = "角色当前低年级技能等级",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "UltimateSkillLevel",
            label = "高年级技能等级",
            defaultValue = "7",
            description = "角色当前高年级技能等级",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "PassiveSkillLevel",
            label = "被动技能等级",
            defaultValue = "7",
            description = "角色当前被动技能等级",
        ),
        BattlePowerCalculatorFieldDefinition(
            id = "AsideGrade",
            label = "坨格等级",
            defaultValue = "2",
            description = "坨格等级，大于等于 2 时坨格系数生效",
        ),
    )

    val constantDefinitions: List<BattlePowerCalculatorFieldDefinition> = listOf(
        BattlePowerCalculatorFieldDefinition("AttackWeight_301", "主攻击权重", "2.1", "FormalInfo[301].ValueA，主攻击项权重"),
        BattlePowerCalculatorFieldDefinition("DefenseWeight_302", "防御权重", "0.7", "FormalInfo[302].ValueA，物理防御与魔法防御之和的权重"),
        BattlePowerCalculatorFieldDefinition("HpWeight_303", "生命权重", "0.08", "FormalInfo[303].ValueA，生命值项权重"),
        BattlePowerCalculatorFieldDefinition("CriticalRateWeight_304", "暴击率权重", "0.7", "FormalInfo[304].ValueA，暴击率项权重"),
        BattlePowerCalculatorFieldDefinition("CriticalMultWeight_305", "暴击伤害权重", "0.7", "FormalInfo[305].ValueA，暴击伤害项权重"),
        BattlePowerCalculatorFieldDefinition("CriticalResistWeight_306", "暴击抗性权重", "0.7", "FormalInfo[306].ValueA，暴击抗性项权重"),
        BattlePowerCalculatorFieldDefinition("CriticalMultResistWeight_307", "暴伤抗性权重", "0.7", "FormalInfo[307].ValueA，暴伤抗性项权重"),
        BattlePowerCalculatorFieldDefinition("AttackSpeedWeight_308", "攻击速度权重", "0.6", "FormalInfo[308].ValueA，攻击速度项权重"),
    )

    fun calculate(
        inputs: BattlePowerCalculatorParsedInputs,
        constants: BattlePowerCalculatorConstants = BattlePowerCalculatorConstants(),
    ): BattlePowerCalculationResult {
        val mainAttack =
            if (inputs.attackType == "物理") {
                inputs.attackPhysic
            } else {
                inputs.attackMagic
            }
        val defenseSum = inputs.defensePhysic + inputs.defenseMagic
        val asideBonus = if (inputs.asideGrade >= 2.0) inputs.asideValueA else 0.0
        val baseScore =
            mainAttack * constants.attackWeight301 +
                defenseSum * constants.defenseWeight302 +
                inputs.hp * constants.hpWeight303 +
                inputs.criticalRate * constants.criticalRateWeight304 +
                inputs.criticalMult * constants.criticalMultWeight305 +
                inputs.criticalResist * constants.criticalResistWeight306 +
                inputs.criticalMultResist * constants.criticalMultResistWeight307 +
                inputs.attackSpeed * constants.attackSpeedWeight308
        val growthMultiplier =
            1.0 +
                inputs.activeSkillValueA * inputs.spSkillLevel +
                inputs.ultimateSkillValueA * inputs.ultimateSkillLevel +
                inputs.passiveValueA * inputs.passiveSkillLevel +
                inputs.weightValueA +
                asideBonus
        val power = truncateTowardZero(baseScore * growthMultiplier)

        return BattlePowerCalculationResult(
            rows = listOf(
                BattlePowerCalculationRow(
                    id = "main_attack",
                    label = "主攻击",
                    value = mainAttack,
                    description = "攻击类型为“物理”时取物理攻击，为“魔法”时取魔法攻击",
                ),
                BattlePowerCalculationRow(
                    id = "defense_sum",
                    label = "双防合计",
                    value = defenseSum,
                    description = "物理防御与魔法防御之和",
                ),
                BattlePowerCalculationRow(
                    id = "aside_bonus",
                    label = "坨格加成",
                    value = asideBonus,
                    description = "坨格等级大于等于 2 时取旁支系数，否则为 0",
                ),
                BattlePowerCalculationRow(
                    id = "base_score",
                    label = "基础分",
                    value = baseScore,
                    description = "由主攻击、双防、生命、暴击率、暴击伤害、暴击抗性、暴伤抗性、攻击速度组成",
                ),
                BattlePowerCalculationRow(
                    id = "growth_multiplier",
                    label = "成长乘区",
                    value = growthMultiplier,
                    description = "由技能等级系数、角色固定系数、坨格加成组成",
                ),
                BattlePowerCalculationRow(
                    id = "power",
                    label = "战斗力",
                    value = power,
                    description = "最终结果，等于基础分乘成长乘区后取整",
                ),
            ),
        )
    }

    private fun truncateTowardZero(value: Double): Double =
        if (abs(value) < 1.0) {
            0.0
        } else {
            value.toLong().toDouble()
        }
}
