package com.trickcal.crayon.domain.petdispatch

import com.trickcal.crayon.data.local.PetDispatchCatalogJsonParser
import com.trickcal.crayon.data.local.loadGeneratedCatalogJson
import com.trickcal.crayon.model.PetDispatchAssignment
import com.trickcal.crayon.model.PetDispatchCatalog
import com.trickcal.crayon.model.PetDispatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetDispatchSolverTest {
    private val catalog: PetDispatchCatalog by lazy {
        PetDispatchCatalogJsonParser.parse(loadGeneratedCatalogJson())
    }

    @Test
    fun calculate_matchesReferenceForFairyKingdomFiveTasks() {
        val result = calculateAllOwned(regionName = "仙灵王国", taskCount = 5)

        assertSuccessfulSummary(
            result = result,
            totalScore = 210,
            borrowedCount = 0,
            totalPets = 10,
            taskCount = 5,
            allSpecial = true,
        )
        assertAssignments(
            result.assignments,
            expected = listOf(
                "洗澡" to "青龙/迷你斯皮奇",
                "偷吃胡萝卜" to "小黑/偶像",
                "闻闻面包的香味" to "迷你刻耳柏洛斯/跳跳鲸",
                "假扮玩偶" to "迷你市长大人/迷你黄油",
                "用宝箱玩捉迷藏" to "迷你纳塔/迷你柯米",
            ),
        )
    }

    @Test
    fun calculate_matchesReferenceForOrcVillageFiveTasks() {
        val result = calculateAllOwned(regionName = "兽人村", taskCount = 5)

        assertSuccessfulSummary(
            result = result,
            totalScore = 214,
            borrowedCount = 0,
            totalPets = 10,
            taskCount = 5,
            allSpecial = true,
        )
        assertAssignments(
            result.assignments,
            expected = listOf(
                "在训练场运动" to "迷你莉1莉/迷你猫咪",
                "眺望兽人村" to "迷你车市长大人/迷你艾鲁",
                "在柯米的被窝里睡午觉" to "小黑/偶像",
                "在没有黄油的家里玩耍" to "青龙/迷你斯皮奇",
                "在牧场里假装是羊" to "迷你市长大人/迷你黄油",
            ),
        )
    }

    @Test
    fun calculate_matchesReferenceForWitchKingdomFiveTasks() {
        val result = calculateAllOwned(regionName = "魔女王国", taskCount = 5)

        assertSuccessfulSummary(
            result = result,
            totalScore = 212,
            borrowedCount = 0,
            totalPets = 10,
            taskCount = 5,
            allSpecial = true,
        )
        assertAssignments(
            result.assignments,
            expected = listOf(
                "偷拿归还的书籍" to "偶像/宝贝河马",
                "给植物施肥" to "迷你刻耳柏洛斯/跳跳鲸",
                "获得并喝下药水" to "迷你市长大人/迷你黄油",
                "配送炸胡萝卜" to "青龙/迷你斯皮奇",
                "训练" to "迷你车市长大人/迷你艾鲁",
            ),
        )
    }

    @Test
    fun calculate_reportsWhenRegionHasNoUsableTasks() {
        val result = calculateAllOwned(regionName = "莫纳蒂尔", taskCount = 3)

        assertFalse(result.isSuccess)
        assertEquals(
            "无法生成3个任务组合，该区域只有0个有效任务，请选择较小的任务数量。",
            result.errorMessage,
        )
    }

    @Test
    fun calculate_matchesReferenceForBorrowedPetScenario() {
        val result = calculate(
            regionName = "仙灵王国",
            taskCount = 3,
            ownedIds = setOf(1, 2, 3, 4, 5, 6),
            farmIds = setOf(7, 8, 9, 10, 11, 12),
        )

        assertSuccessfulSummary(
            result = result,
            totalScore = 109,
            borrowedCount = 3,
            totalPets = 9,
            taskCount = 3,
            allSpecial = false,
        )
        assertEquals(2, result.tierSummary.specialCount)
        assertEquals(1, result.tierSummary.firstCount)
        assertAssignments(
            result.assignments,
            expected = listOf(
                "洗澡" to "迷你车市长大人/鼠克/奇克金（借）",
                "闻闻面包的香味" to "莎兔/迷你刻耳柏洛斯/羊驼（借）",
                "用宝箱玩捉迷藏" to "迷你莉1莉/迷你猫咪/厚毛猬（借）",
            ),
        )
    }

    @Test
    fun calculate_doesNotThrowAcrossRandomSelections() {
        val random = Random(42)
        val allPetIds = catalog.pets.map { it.id }
        repeat(200) { index ->
            val region = catalog.regions.random(random)
            val taskCount = random.nextInt(1, 6)
            val ownedIds = allPetIds.shuffled(random).take(random.nextInt(1, allPetIds.size + 1)).toSet()
            val farmIds = allPetIds.shuffled(random).take(random.nextInt(0, allPetIds.size + 1)).toSet()

            runCatching {
                calculate(
                    regionName = region.name,
                    taskCount = taskCount,
                    ownedIds = ownedIds,
                    farmIds = farmIds,
                )
            }.onFailure { error ->
                throw AssertionError(
                    "Unexpected solver exception at iteration=$index, region=${region.name}, taskCount=$taskCount, " +
                        "owned=${ownedIds.size}, farm=${farmIds.size}",
                    error,
                )
            }
        }
    }

    @Test
    fun calculate_doesNotThrowWhenOwnedAndFarmSelectionsFullyOverlap() {
        val allPetIds = catalog.pets.map { it.id }.toSet()
        val regions = catalog.regions.filter { it.tasks.isNotEmpty() }

        regions.forEach { region ->
            val result = runCatching {
                calculate(
                    regionName = region.name,
                    taskCount = region.tasks.size,
                    ownedIds = allPetIds,
                    farmIds = allPetIds,
                )
            }.getOrElse { error ->
                throw AssertionError(
                    "Unexpected solver exception for fully overlapping selections in region=${region.name}",
                    error,
                )
            }

            assertTrue("Expected success for region=${region.name}", result.isSuccess)
        }
    }

    private fun calculateAllOwned(
        regionName: String,
        taskCount: Int,
    ): PetDispatchResult =
        calculate(
            regionName = regionName,
            taskCount = taskCount,
            ownedIds = catalog.pets.map { it.id }.toSet(),
            farmIds = emptySet(),
        )

    private fun calculate(
        regionName: String,
        taskCount: Int,
        ownedIds: Set<Int>,
        farmIds: Set<Int>,
    ): PetDispatchResult {
        val region = catalog.regions.first { it.name == regionName }
        val ownedPets = catalog.pets.filter { it.id in ownedIds }
        val farmPets = catalog.pets.filter { it.id in farmIds }
        return PetDispatchSolver.calculate(
            region = region,
            taskCount = taskCount,
            ownedPets = ownedPets,
            farmPets = farmPets,
        )
    }

    private fun assertSuccessfulSummary(
        result: PetDispatchResult,
        totalScore: Int,
        borrowedCount: Int,
        totalPets: Int,
        taskCount: Int,
        allSpecial: Boolean,
    ) {
        assertTrue(result.isSuccess)
        assertEquals(totalScore, result.totalScore)
        assertEquals(borrowedCount, result.borrowedCount)
        assertEquals(totalPets, result.totalPets)
        assertEquals(taskCount, result.taskCount)
        assertEquals(allSpecial, result.allSpecial)
    }

    private fun assertAssignments(
        assignments: List<PetDispatchAssignment>,
        expected: List<Pair<String, String>>,
    ) {
        assertEquals(expected.size, assignments.size)
        val actual = assignments.map { assignment ->
            assignment.task.task to assignment.team.joinToString(separator = "/") { pet ->
                pet.name + if (pet.isBorrowed) "（借）" else ""
            }
        }
        assertEquals(expected, actual)
    }
}
