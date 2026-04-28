package com.trickcal.crayon.domain.statistics

import com.trickcal.crayon.model.AttributeStatistics
import com.trickcal.crayon.model.AttributeSlotSpec
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.OverviewStatistics
import com.trickcal.crayon.model.StatisticsSnapshot
import com.trickcal.crayon.model.TierAttributeStatistics
import com.trickcal.crayon.model.TierStatistics

object StatisticsCalculator {
    fun calculate(
        characters: List<CharacterProfile>,
        litSlots: Set<String>,
    ): StatisticsSnapshot {
        val slotEntries = characters.flatMap { character ->
            character.layers.flatMap { layer ->
                layer.attributeSlots.map { slot ->
                    SlotEntry(
                        characterId = character.id,
                        tier = layer.tier,
                        slot = slot,
                        isLit = slot.id in litSlots,
                    )
                }
            }
        }

        val overview = OverviewStatistics(
            totalGoldCrayons = slotEntries.filter { it.isLit }.sumOf { it.tier.goldCrayonCostPerSlot },
            litColoredSlots = slotEntries.count { it.isLit },
            totalColoredSlots = slotEntries.size,
            totalBonusPercent = slotEntries.filter { it.isLit }.sumOf { it.tier.bonusPercentPerSlot },
            bonusByAttribute = AttributeType.entries.associateWith { attribute ->
                slotEntries.filter { it.isLit && it.slot.attributeType == attribute }
                    .sumOf { it.tier.bonusPercentPerSlot }
            },
        )

        val attributeStatistics = AttributeType.entries.map { attribute ->
            val attributeEntries = slotEntries.filter { it.slot.attributeType == attribute }
            AttributeStatistics(
                attributeType = attribute,
                litCount = attributeEntries.count { it.isLit },
                totalCount = attributeEntries.size,
                totalBonusPercent = attributeEntries.filter { it.isLit }.sumOf { it.tier.bonusPercentPerSlot },
                goldCrayonCost = attributeEntries.filter { it.isLit }.sumOf { it.tier.goldCrayonCostPerSlot },
            )
        }

        val tierStatistics = BoardTier.entries.map { tier ->
            val tierEntries = slotEntries.filter { it.tier == tier }
            TierStatistics(
                tier = tier,
                litCount = tierEntries.count { it.isLit },
                totalCount = tierEntries.size,
                totalBonusPercent = tierEntries.filter { it.isLit }.sumOf { it.tier.bonusPercentPerSlot },
                goldCrayonCost = tierEntries.filter { it.isLit }.sumOf { it.tier.goldCrayonCostPerSlot },
                attributeStatistics = AttributeType.entries.map { attribute ->
                    val attributeEntries = tierEntries.filter { it.slot.attributeType == attribute }
                    TierAttributeStatistics(
                        attributeType = attribute,
                        litCount = attributeEntries.count { it.isLit },
                        totalCount = attributeEntries.size,
                        totalBonusPercent = attributeEntries
                            .filter { it.isLit }
                            .sumOf { it.tier.bonusPercentPerSlot },
                        goldCrayonCost = attributeEntries
                            .filter { it.isLit }
                            .sumOf { it.tier.goldCrayonCostPerSlot },
                    )
                },
            )
        }

        return StatisticsSnapshot(
            overview = overview,
            attributeStatistics = attributeStatistics,
            tierStatistics = tierStatistics,
        )
    }

    private data class SlotEntry(
        val characterId: String,
        val tier: BoardTier,
        val slot: AttributeSlotSpec,
        val isLit: Boolean,
    )
}
