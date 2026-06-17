package com.trickcal.crayon.domain.damage

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class DamageCalculatorFieldDefinition(
    val id: String,
    val label: String,
    val defaultValue: String,
    val description: String,
)

data class DamageCalculatorParsedInputs(
    val attackInput: Double,
    val defenseInput: Double,
    val inputCoefficient: Double,
    val addDamageCoefficient: Double,
    val effectDamage: Double,
    val personalityBonus: Double,
    val typeDamageBase: Double,
    val skillDamageRate: Double,
    val criticalStat: Double,
    val criticalResist: Double,
    val criticalRateBonus: Double,
    val criticalRateReceiveBonus: Double,
    val criticalMultStat: Double,
    val criticalMultResist: Double,
    val criticalMultBonus: Double,
    val criticalMultReceiveBonus: Double,
    val isCritical: Double,
    val endDamageBase: Double,
    val endDamageExtra: Double,
)

data class DamageCalculatorConstants(
    val constDamageA: Double = 60.0,
    val constDamageB: Double = 60.0,
    val constDamageC: Double = 60.0,
    val constDamageD: Double = 78.0,
    val constDamageE: Double = 60.0,
    val constCriticalA: Double = 0.3,
    val constCriticalB: Double = 0.5,
    val constCriticalC: Double = 300.0,
    val constCriticalD: Double = 0.5,
    val constCriticalE: Double = 100.0,
    val constCriticalMultA: Double = 0.75,
    val constCriticalMultB: Double = 0.85,
    val constCriticalMultC: Double = 300.0,
    val constCriticalMultD: Double = 1.1,
    val constCriticalMultE: Double = 100.0,
    val limitDamageConstMin: Double = 0.05,
    val limitDamageConstMax: Double = 1.5,
    val limitCriticalRateMin: Double = 0.05,
    val limitCriticalRateMax: Double = 0.75,
    val limitCriticalMultRateMin: Double = 0.2,
    val limitCriticalMultRateMax: Double = 1.5,
    val limitDamagePercentMin: Double = 0.2,
    val globalAttackMin: Double = 1.0,
    val globalDefenseMin: Double = 0.0,
)

data class DamageCalculationRow(
    val id: String,
    val label: String,
    val value: Double,
    val description: String,
)

data class DamageCalculationResult(
    val rows: List<DamageCalculationRow>,
) {
    fun rowValue(id: String): Double =
        checkNotNull(rows.firstOrNull { it.id == id }) {
            "Unknown damage calculation row id: $id"
        }.value
}

object DamageCalculatorEngine {
    val inputDefinitions: List<DamageCalculatorFieldDefinition> = listOf(
        DamageCalculatorFieldDefinition(
            id = "AttackInput",
            label = "攻击属性原始值",
            defaultValue = "1200",
            description = "物攻或魔攻的最终计算值",
        ),
        DamageCalculatorFieldDefinition(
            id = "DefenseInput",
            label = "防御属性原始值",
            defaultValue = "800",
            description = "物防或魔防的最终计算值",
        ),
        DamageCalculatorFieldDefinition(
            id = "InputCoefficient",
            label = "输入技能系数",
            defaultValue = "250",
            description = "技能原始倍率，按百分制填写",
        ),
        DamageCalculatorFieldDefinition(
            id = "AddDamageCoefficient",
            label = "追加伤害系数",
            defaultValue = "0",
            description = "非零时额外叠加的伤害系数",
        ),
        DamageCalculatorFieldDefinition(
            id = "EffectDamage",
            label = "效果倍率",
            defaultValue = "1.1",
            description = "当前 effect 段的倍率",
        ),
        DamageCalculatorFieldDefinition(
            id = "PersonalityBonus",
            label = "性格或克制加成",
            defaultValue = "0.2",
            description = "以 0.2 表示 +20%",
        ),
        DamageCalculatorFieldDefinition(
            id = "TypeDamageBase",
            label = "普通伤害基础项",
            defaultValue = "1",
            description = "物伤或魔伤的基础乘区",
        ),
        DamageCalculatorFieldDefinition(
            id = "SkillDamageRate",
            label = "技能专属伤害加成",
            defaultValue = "0.25",
            description = "对应 SkillDamageRate 或同类项",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalStat",
            label = "暴击属性",
            defaultValue = "150",
            description = "攻击方暴击数值",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalResist",
            label = "暴抗属性",
            defaultValue = "100",
            description = "防守方暴抗数值",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalRateBonus",
            label = "额外暴击率加成",
            defaultValue = "0",
            description = "百分比值，例如 10 表示 +10%",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalRateReceiveBonus",
            label = "额外受暴击率加成",
            defaultValue = "0",
            description = "来自目标身上的受暴击率变化",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalMultStat",
            label = "暴伤属性",
            defaultValue = "140",
            description = "攻击方暴伤数值",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalMultResist",
            label = "暴伤抗性",
            defaultValue = "100",
            description = "防守方暴伤抗性数值",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalMultBonus",
            label = "额外暴伤加成",
            defaultValue = "0",
            description = "百分比值，例如 20 表示 +20%",
        ),
        DamageCalculatorFieldDefinition(
            id = "CriticalMultReceiveBonus",
            label = "额外受暴伤加成",
            defaultValue = "0",
            description = "来自目标身上的受暴伤变化",
        ),
        DamageCalculatorFieldDefinition(
            id = "IsCritical",
            label = "是否暴击",
            defaultValue = "1",
            description = "1 表示暴击，0 表示不暴击",
        ),
        DamageCalculatorFieldDefinition(
            id = "EndDamageBase",
            label = "终伤基础项",
            defaultValue = "0.1",
            description = "以 0.1 表示 +10% 终伤",
        ),
        DamageCalculatorFieldDefinition(
            id = "EndDamageExtra",
            label = "终伤附加项",
            defaultValue = "5",
            description = "百分制附加终伤，例如 5 表示 +5%",
        ),
    )

    val constantDefinitions: List<DamageCalculatorFieldDefinition> = listOf(
        DamageCalculatorFieldDefinition("ConstDamage_A", "伤害常量A", "60", "TableData.GlobalWeight 运行时 raw 值"),
        DamageCalculatorFieldDefinition("ConstDamage_B", "伤害常量B", "60", "TableData.GlobalWeight 运行时 raw 值"),
        DamageCalculatorFieldDefinition("ConstDamage_C", "伤害常量C", "60", "TableData.GlobalWeight 运行时 raw 值"),
        DamageCalculatorFieldDefinition("ConstDamage_D", "伤害常量D", "78", "TableData.GlobalWeight 运行时 raw 值"),
        DamageCalculatorFieldDefinition("ConstDamage_E", "伤害常量E", "60", "TableData.GlobalWeight 运行时 raw 值"),
        DamageCalculatorFieldDefinition("ConstCritical_A", "暴击常量A", "0.3", "暴击率曲线常量"),
        DamageCalculatorFieldDefinition("ConstCritical_B", "暴击常量B", "0.5", "暴击率曲线常量"),
        DamageCalculatorFieldDefinition("ConstCritical_C", "暴击常量C", "300", "暴击率曲线常量"),
        DamageCalculatorFieldDefinition("ConstCritical_D", "暴击常量D", "0.5", "暴击率曲线常量"),
        DamageCalculatorFieldDefinition("ConstCritical_E", "暴击常量E", "100", "暴击率曲线常量"),
        DamageCalculatorFieldDefinition("ConstCriticalMult_A", "暴伤常量A", "0.75", "暴伤曲线常量"),
        DamageCalculatorFieldDefinition("ConstCriticalMult_B", "暴伤常量B", "0.85", "暴伤曲线常量"),
        DamageCalculatorFieldDefinition("ConstCriticalMult_C", "暴伤常量C", "300", "暴伤曲线常量"),
        DamageCalculatorFieldDefinition("ConstCriticalMult_D", "暴伤常量D", "1.1", "暴伤曲线常量"),
        DamageCalculatorFieldDefinition("ConstCriticalMult_E", "暴伤常量E", "100", "暴伤曲线常量"),
        DamageCalculatorFieldDefinition("LimitDamageConst_Min", "伤害系数下限", "0.05", "clamp 下限"),
        DamageCalculatorFieldDefinition("LimitDamageConst_Max", "伤害系数上限", "1.5", "clamp 上限"),
        DamageCalculatorFieldDefinition("LimitCriticalRate_Min", "暴击率下限", "0.05", "暴击率 clamp 下限"),
        DamageCalculatorFieldDefinition("LimitCriticalRate_Max", "暴击率上限", "0.75", "暴击率 clamp 上限"),
        DamageCalculatorFieldDefinition("LimitCriticalMultRate_Min", "暴伤倍率下限", "0.2", "暴伤倍率 clamp 下限"),
        DamageCalculatorFieldDefinition("LimitCriticalMultRate_Max", "暴伤倍率上限", "1.5", "暴伤倍率 clamp 上限"),
        DamageCalculatorFieldDefinition("LimitDamagePercent_Min", "最终伤害百分比下限", "0.2", "普通伤害乘区和终伤乘区的下限"),
        DamageCalculatorFieldDefinition("Global_AttackMin", "攻击下限", "1", "攻击属性最低保底"),
        DamageCalculatorFieldDefinition("Global_DefenseMin", "防御下限", "0", "防御属性最低保底"),
    )

    fun calculate(
        inputs: DamageCalculatorParsedInputs,
        constants: DamageCalculatorConstants = DamageCalculatorConstants(),
    ): DamageCalculationResult {
        val attack = max(inputs.attackInput, constants.globalAttackMin)
        val defense = max(inputs.defenseInput, constants.globalDefenseMin)
        val x = ((attack - defense / 2.0) / max(defense / 2.0, 1.0)) * 100.0
        val damageCurve = piecewiseCurve(
            delta = x,
            base = constants.constDamageA,
            positiveScale = constants.constDamageB,
            positiveOffset = constants.constDamageC,
            negativeScale = constants.constDamageD,
            negativeOffset = constants.constDamageE,
        )
        val baseAttackDamage =
            attack * clamp(damageCurve * 0.01, constants.limitDamageConstMin, constants.limitDamageConstMax)
        val skillTotalCoefficient = inputs.inputCoefficient + inputs.addDamageCoefficient
        val xc = ((inputs.criticalStat - inputs.criticalResist) / max(inputs.criticalResist, 1.0)) * 100.0
        val criticalRateBase = piecewiseCurve(
            delta = xc,
            base = constants.constCriticalA,
            positiveScale = constants.constCriticalB,
            positiveOffset = constants.constCriticalC,
            negativeScale = constants.constCriticalD,
            negativeOffset = constants.constCriticalE,
        )
        val criticalRate = clamp(
            criticalRateBase + 0.01 * (inputs.criticalRateBonus + inputs.criticalRateReceiveBonus),
            constants.limitCriticalRateMin,
            constants.limitCriticalRateMax,
        )
        val xm = ((inputs.criticalMultStat - inputs.criticalMultResist) / max(inputs.criticalMultResist, 1.0)) * 100.0
        val criticalMultBase = piecewiseCurve(
            delta = xm,
            base = constants.constCriticalMultA,
            positiveScale = constants.constCriticalMultB,
            positiveOffset = constants.constCriticalMultC,
            negativeScale = constants.constCriticalMultD,
            negativeOffset = constants.constCriticalMultE,
        )
        val criticalMultRate = clamp(
            criticalMultBase + 0.01 * (inputs.criticalMultBonus + inputs.criticalMultReceiveBonus),
            constants.limitCriticalMultRateMin,
            constants.limitCriticalMultRateMax,
        )
        val criticalMultiplier = if (inputs.isCritical == 1.0) 1.0 + criticalMultRate else 1.0
        val normalDamageMultiplier = max(inputs.typeDamageBase + inputs.skillDamageRate, constants.limitDamagePercentMin)
        val endDamageMultiplier = max(1.0 + inputs.endDamageBase + 0.01 * inputs.endDamageExtra, constants.limitDamagePercentMin)
        val intermediateDamage = truncate(
            baseAttackDamage *
                0.01 *
                criticalMultiplier *
                skillTotalCoefficient *
                inputs.effectDamage *
                (1.0 + inputs.personalityBonus) *
                normalDamageMultiplier,
        )
        val finalDamage = truncate(intermediateDamage * endDamageMultiplier)

        return DamageCalculationResult(
            rows = listOf(
                DamageCalculationRow("attack", "攻击属性", attack, "套用攻击下限"),
                DamageCalculationRow("defense", "防御属性", defense, "套用防御下限"),
                DamageCalculationRow("x", "x", x, "攻防差值"),
                DamageCalculationRow("damage_curve", "伤害曲线(x)", damageCurve, "分段伤害曲线"),
                DamageCalculationRow("base_attack_damage", "基础攻击伤害", baseAttackDamage, "攻击属性乘以被 clamp 的伤害曲线"),
                DamageCalculationRow("skill_total_coefficient", "技能总系数", skillTotalCoefficient, "输入技能系数 + 追加伤害系数"),
                DamageCalculationRow("xc", "xc", xc, "暴击差值"),
                DamageCalculationRow("critical_rate_base", "暴击率基础值", criticalRateBase, "暴击率分段曲线"),
                DamageCalculationRow("critical_rate", "暴击率", criticalRate, "暴击率 clamp 后值"),
                DamageCalculationRow("xm", "xm", xm, "暴伤差值"),
                DamageCalculationRow("critical_mult_base", "暴伤基础值", criticalMultBase, "暴伤分段曲线"),
                DamageCalculationRow("critical_mult_rate", "暴伤倍率", criticalMultRate, "暴伤倍率 clamp 后值"),
                DamageCalculationRow("critical_multiplier", "暴击乘区", criticalMultiplier, "1 表示暴击"),
                DamageCalculationRow("normal_damage_multiplier", "普通伤害乘区", normalDamageMultiplier, "物伤或魔伤乘区"),
                DamageCalculationRow("end_damage_multiplier", "终伤乘区", endDamageMultiplier, "终伤基础项 + 附加项"),
                DamageCalculationRow("intermediate_damage", "中间伤害", intermediateDamage, "未叠终伤乘区前的截断值"),
                DamageCalculationRow("final_damage", "最终伤害", finalDamage, "最终截断结果"),
            ),
        )
    }

    private fun clamp(
        value: Double,
        minValue: Double,
        maxValue: Double,
    ): Double = value.coerceIn(minimumValue = minValue, maximumValue = maxValue)

    private fun piecewiseCurve(
        delta: Double,
        base: Double,
        positiveScale: Double,
        positiveOffset: Double,
        negativeScale: Double,
        negativeOffset: Double,
    ): Double =
        if (delta < 0.0) {
            base + delta * negativeScale / (negativeOffset - delta)
        } else {
            base + delta * positiveScale / (delta + positiveOffset)
        }

    private fun truncate(value: Double): Double =
        if (value >= 0.0) {
            floor(value)
        } else {
            ceil(value)
        }
}
