package com.trickcal.crayon.model

data class OverviewStatistics(
    val totalGoldCrayons: Int,
    val litColoredSlots: Int,
    val totalColoredSlots: Int,
    val totalBonusPercent: Int,
    val bonusByAttribute: Map<AttributeType, Int>,
)

data class AttributeStatistics(
    val attributeType: AttributeType,
    val litCount: Int,
    val totalCount: Int,
    val totalBonusPercent: Int,
    val goldCrayonCost: Int,
)

data class TierAttributeStatistics(
    val attributeType: AttributeType,
    val litCount: Int,
    val totalCount: Int,
    val totalBonusPercent: Int,
    val goldCrayonCost: Int,
)

data class TierStatistics(
    val tier: BoardTier,
    val litCount: Int,
    val totalCount: Int,
    val totalBonusPercent: Int,
    val goldCrayonCost: Int,
    val attributeStatistics: List<TierAttributeStatistics>,
)

data class StatisticsSnapshot(
    val overview: OverviewStatistics,
    val attributeStatistics: List<AttributeStatistics>,
    val tierStatistics: List<TierStatistics>,
) {
    companion object {
        fun empty(): StatisticsSnapshot {
            val emptyAttributeStats = AttributeType.entries.map { attribute ->
                AttributeStatistics(
                    attributeType = attribute,
                    litCount = 0,
                    totalCount = 0,
                    totalBonusPercent = 0,
                    goldCrayonCost = 0,
                )
            }
            val emptyTierStats = BoardTier.entries.map { tier ->
                TierStatistics(
                    tier = tier,
                    litCount = 0,
                    totalCount = 0,
                    totalBonusPercent = 0,
                    goldCrayonCost = 0,
                    attributeStatistics = AttributeType.entries.map { attribute ->
                        TierAttributeStatistics(
                            attributeType = attribute,
                            litCount = 0,
                            totalCount = 0,
                            totalBonusPercent = 0,
                            goldCrayonCost = 0,
                        )
                    },
                )
            }
            return StatisticsSnapshot(
                overview = OverviewStatistics(
                    totalGoldCrayons = 0,
                    litColoredSlots = 0,
                    totalColoredSlots = 0,
                    totalBonusPercent = 0,
                    bonusByAttribute = AttributeType.entries.associateWith { 0 },
                ),
                attributeStatistics = emptyAttributeStats,
                tierStatistics = emptyTierStats,
            )
        }
    }
}
