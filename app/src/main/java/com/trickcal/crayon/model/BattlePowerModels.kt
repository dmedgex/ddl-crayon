package com.trickcal.crayon.model

data class BattlePowerCharacterCatalog(
    val defaultCharacterUid: Int,
    val characters: List<BattlePowerCharacterProfile>,
)

data class BattlePowerCharacterProfile(
    val uid: Int,
    val name: String,
    val resourceName: String,
    val attackType: String,
    val nameKey: String,
    val activeSkillValueA: Double,
    val ultimateSkillValueA: Double,
    val passiveValueA: Double,
    val weightValueA: Double,
    val asideValueA: Double,
)
