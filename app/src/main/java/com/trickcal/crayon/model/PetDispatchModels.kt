package com.trickcal.crayon.model

enum class PetDispatchRarity(
    val displayName: String,
    val baseScore: Int,
) {
    COMMON(displayName = "普通宠物", baseScore = 2),
    ADVANCED(displayName = "高级宠物", baseScore = 2),
    RARE(displayName = "稀有宠物", baseScore = 3),
    LEGENDARY(displayName = "传说宠物", baseScore = 5),
    ;
}

enum class PetDispatchSkillLevel(
    val score: Int,
) {
    C(score = 7),
    B(score = 12),
    A(score = 17),
    S(score = 22),
    ;
}

enum class PetDispatchRewardTier(
    val displayName: String,
    val rank: Int,
) {
    NONE(displayName = "无奖励", rank = 0),
    FOURTH(displayName = "四阶", rank = 1),
    THIRD(displayName = "三阶", rank = 2),
    SECOND(displayName = "二阶", rank = 3),
    FIRST(displayName = "一阶", rank = 4),
    SPECIAL(displayName = "特阶", rank = 5),
    ;

    companion object {
        fun fromScore(score: Int): PetDispatchRewardTier =
            when {
                score > 37 -> SPECIAL
                score > 25 -> FIRST
                score > 13 -> SECOND
                score > 5 -> THIRD
                score > 1 -> FOURTH
                else -> NONE
            }
    }
}

data class PetDispatchSkill(
    val name: String,
    val level: PetDispatchSkillLevel,
) {
    val score: Int
        get() = level.score
}

data class PetDispatchPet(
    val id: Int,
    val name: String,
    val rarity: PetDispatchRarity,
    val baseScore: Int,
    val skills: List<PetDispatchSkill>,
    val imageAssetName: String,
)

data class PetDispatchTask(
    val id: Int,
    val area: String,
    val task: String,
    val bonusSkills: List<String>,
)

data class PetDispatchRegion(
    val name: String,
    val tasks: List<PetDispatchTask>,
)

data class PetDispatchCatalog(
    val pets: List<PetDispatchPet>,
    val regions: List<PetDispatchRegion>,
)

enum class PetDispatchSelectionTab {
    OWNED,
    FARM,
    ;

    companion object {
        fun fromStorageValue(value: String?): PetDispatchSelectionTab =
            entries.firstOrNull { it.name == value } ?: OWNED
    }
}

data class PetDispatchSelectionState(
    val selectedRegionName: String? = null,
    val selectedTaskCount: Int = 1,
    val selectedOwnedPetIds: Set<Int> = emptySet(),
    val selectedFarmPetIds: Set<Int> = emptySet(),
    val selectedTab: PetDispatchSelectionTab = PetDispatchSelectionTab.OWNED,
)

data class PetDispatchAssignedPet(
    val id: Int,
    val name: String,
    val rarity: PetDispatchRarity,
    val skills: List<PetDispatchSkill>,
    val imageAssetName: String,
    val isBorrowed: Boolean,
)

data class PetDispatchAssignment(
    val task: PetDispatchTask,
    val team: List<PetDispatchAssignedPet>,
    val score: Int,
    val rewardTier: PetDispatchRewardTier,
)

data class PetDispatchTierSummary(
    val specialCount: Int = 0,
    val firstCount: Int = 0,
    val secondCount: Int = 0,
    val thirdCount: Int = 0,
    val fourthCount: Int = 0,
) {
    val allSpecialCount: Int
        get() = specialCount

    operator fun plus(other: PetDispatchTierSummary): PetDispatchTierSummary =
        PetDispatchTierSummary(
            specialCount = specialCount + other.specialCount,
            firstCount = firstCount + other.firstCount,
            secondCount = secondCount + other.secondCount,
            thirdCount = thirdCount + other.thirdCount,
            fourthCount = fourthCount + other.fourthCount,
        )

    companion object {
        fun forTier(tier: PetDispatchRewardTier): PetDispatchTierSummary =
            when (tier) {
                PetDispatchRewardTier.SPECIAL -> PetDispatchTierSummary(specialCount = 1)
                PetDispatchRewardTier.FIRST -> PetDispatchTierSummary(firstCount = 1)
                PetDispatchRewardTier.SECOND -> PetDispatchTierSummary(secondCount = 1)
                PetDispatchRewardTier.THIRD -> PetDispatchTierSummary(thirdCount = 1)
                PetDispatchRewardTier.FOURTH -> PetDispatchTierSummary(fourthCount = 1)
                PetDispatchRewardTier.NONE -> PetDispatchTierSummary()
            }
    }
}

data class PetDispatchResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val totalScore: Int = 0,
    val borrowedCount: Int = 0,
    val totalPets: Int = 0,
    val taskCount: Int = 0,
    val allSpecial: Boolean = false,
    val calculationTimeMs: Long = 0L,
    val tierSummary: PetDispatchTierSummary = PetDispatchTierSummary(),
    val assignments: List<PetDispatchAssignment> = emptyList(),
    val textReport: String = "",
)
