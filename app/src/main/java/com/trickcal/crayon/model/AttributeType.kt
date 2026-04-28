package com.trickcal.crayon.model

enum class AttributeType(
    val displayName: String,
    val shortName: String,
) {
    ATTACK(displayName = "攻击力", shortName = "攻"),
    CRIT(displayName = "暴击", shortName = "暴"),
    HEALTH(displayName = "生命值", shortName = "生"),
    DEFENSE(displayName = "防御力", shortName = "防"),
    CRIT_RESIST(displayName = "暴击抗性", shortName = "抗"),
}
