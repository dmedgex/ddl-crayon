package com.trickcal.crayon.domain.statistics

import com.trickcal.crayon.data.local.DemoCharacterCatalog
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsCalculatorTest {
    private val characters = DemoCharacterCatalog.characters

    @Test
    fun singleCharacterMixedTierLighting_followsBonusAndGoldRules() {
        val character = characters.first { it.id == "aiko" }
        val litSlots = setOf(
            character.findSlotId(BoardTier.FIRST, AttributeType.HEALTH),
            character.findSlotId(BoardTier.SECOND, AttributeType.DEFENSE),
            character.findSlotId(BoardTier.THIRD, AttributeType.CRIT),
        )

        val snapshot = StatisticsCalculator.calculate(
            characters = listOf(character),
            litSlots = litSlots,
        )

        assertEquals(3, snapshot.overview.litColoredSlots)
        assertEquals(9, snapshot.overview.totalColoredSlots)
        assertEquals(12, snapshot.overview.totalGoldCrayons)
        assertEquals(12, snapshot.overview.totalBonusPercent)
        assertEquals(3, snapshot.overview.bonusByAttribute[AttributeType.HEALTH])
        assertEquals(4, snapshot.overview.bonusByAttribute[AttributeType.DEFENSE])
        assertEquals(5, snapshot.overview.bonusByAttribute[AttributeType.CRIT])

        val firstTier = snapshot.tierStatistics.first { it.tier == BoardTier.FIRST }
        val secondTier = snapshot.tierStatistics.first { it.tier == BoardTier.SECOND }
        val thirdTier = snapshot.tierStatistics.first { it.tier == BoardTier.THIRD }
        assertEquals(1, firstTier.litCount)
        assertEquals(2, firstTier.totalCount)
        assertEquals(3, firstTier.totalBonusPercent)
        assertEquals(2, firstTier.goldCrayonCost)
        assertEquals(1, secondTier.litCount)
        assertEquals(3, secondTier.totalCount)
        assertEquals(4, secondTier.totalBonusPercent)
        assertEquals(4, secondTier.goldCrayonCost)
        assertEquals(
            3,
            secondTier.attributeStatistics.sumOf { it.totalCount },
        )
        val secondTierDefense = secondTier.attributeStatistics.first {
            it.attributeType == AttributeType.DEFENSE
        }
        assertEquals(1, secondTierDefense.litCount)
        assertEquals(1, secondTierDefense.totalCount)
        assertEquals(4, secondTierDefense.totalBonusPercent)
        assertEquals(4, secondTierDefense.goldCrayonCost)
        assertEquals(1, thirdTier.litCount)
        assertEquals(4, thirdTier.totalCount)
        assertEquals(5, thirdTier.totalBonusPercent)
        assertEquals(6, thirdTier.goldCrayonCost)
        val firstTierHealth = firstTier.attributeStatistics.first {
            it.attributeType == AttributeType.HEALTH
        }
        assertEquals(1, firstTierHealth.litCount)
        assertEquals(1, firstTierHealth.totalCount)
        assertEquals(3, firstTierHealth.totalBonusPercent)
        assertEquals(2, firstTierHealth.goldCrayonCost)
        val thirdTierCrit = thirdTier.attributeStatistics.first {
            it.attributeType == AttributeType.CRIT
        }
        assertEquals(1, thirdTierCrit.litCount)
        assertEquals(1, thirdTierCrit.totalCount)
        assertEquals(5, thirdTierCrit.totalBonusPercent)
        assertEquals(6, thirdTierCrit.goldCrayonCost)
    }

    @Test
    fun sameAttributeAcrossDifferentTiers_accumulatesCorrectly() {
        val aiko = characters.first { it.id == "aiko" }
        val selene = characters.first { it.id == "selene" }
        val litSlots = setOf(
            aiko.findSlotId(BoardTier.FIRST, AttributeType.HEALTH),
            aiko.findSlotId(BoardTier.SECOND, AttributeType.HEALTH),
            selene.findSlotId(BoardTier.THIRD, AttributeType.HEALTH),
        )

        val snapshot = StatisticsCalculator.calculate(
            characters = characters,
            litSlots = litSlots,
        )

        val healthStats = snapshot.attributeStatistics.first {
            it.attributeType == AttributeType.HEALTH
        }

        assertEquals(3, healthStats.litCount)
        assertEquals(19, healthStats.totalCount)
        assertEquals(12, healthStats.totalBonusPercent)
        assertEquals(12, healthStats.goldCrayonCost)
    }

    @Test
    fun emptyProgress_hasExpectedTotalsAndConsistentBreakdown() {
        val snapshot = StatisticsCalculator.calculate(
            characters = characters,
            litSlots = emptySet(),
        )

        assertEquals(0, snapshot.overview.litColoredSlots)
        assertEquals(77, snapshot.overview.totalColoredSlots)
        assertEquals(0, snapshot.overview.totalGoldCrayons)
        assertEquals(0, snapshot.overview.totalBonusPercent)
        assertEquals(
            snapshot.overview.totalColoredSlots,
            snapshot.attributeStatistics.sumOf { it.totalCount },
        )
        assertEquals(
            snapshot.overview.totalColoredSlots,
            snapshot.tierStatistics.sumOf { it.totalCount },
        )
        assertEquals(
            snapshot.overview.totalColoredSlots,
            snapshot.tierStatistics.sumOf { tierStats ->
                tierStats.attributeStatistics.sumOf { it.totalCount }
            },
        )
    }
}

private fun CharacterProfile.findSlotId(
    tier: BoardTier,
    attributeType: AttributeType,
): String =
    checkNotNull(layerForTier(tier))
        .attributeSlots
        .first { it.attributeType == attributeType }
        .id
