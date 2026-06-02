package com.trickcal.crayon.domain.petdispatch

import com.trickcal.crayon.model.PetDispatchAssignedPet
import com.trickcal.crayon.model.PetDispatchAssignment
import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchRegion
import com.trickcal.crayon.model.PetDispatchResult
import com.trickcal.crayon.model.PetDispatchRewardTier
import com.trickcal.crayon.model.PetDispatchTask
import com.trickcal.crayon.model.PetDispatchTierSummary

object PetDispatchSolver {
    private const val maxPetsPerTask = 3
    private const val maxBorrowedPerTask = 1
    private const val maxTotalBorrowed = 3

    fun calculate(
        region: PetDispatchRegion,
        taskCount: Int,
        ownedPets: List<PetDispatchPet>,
        farmPets: List<PetDispatchPet>,
    ): PetDispatchResult {
        if (ownedPets.isEmpty()) {
            return failure("请选择至少一只拥有的宠物。")
        }
        if (taskCount !in 1..5) {
            return failure("请选择有效的任务数量。")
        }

        val availablePets = ownedPets.map { CandidatePet(pet = it, isBorrowed = false) } +
            farmPets.map { CandidatePet(pet = it, isBorrowed = true) }
        val validTasks = region.tasks.filter { it.task.isNotBlank() }
        val taskCombinations = generateTaskCombinations(validTasks, taskCount)
        if (taskCombinations.isEmpty()) {
            return failure(
                "无法生成${taskCount}个任务组合，该区域只有${validTasks.size}个有效任务，请选择较小的任务数量。",
            )
        }

        val petTaskScores = precomputePetTaskScores(availablePets, region.tasks)
        val preparedCombinations = taskCombinations.map { taskCombination ->
            val taskEntries = buildTaskEntries(taskCombination, availablePets, petTaskScores)
            val upperBound = getCombinationUpperBound(taskEntries)
            PreparedCombination(
                taskEntries = taskEntries,
                allSpecialPossible = upperBound.first,
                upperBound = upperBound.second,
            )
        }.sortedWith(
            compareByDescending<PreparedCombination> { it.allSpecialPossible }
                .thenByDescending { it.upperBound },
        )

        var overallBest: Solution? = null
        for (prepared in preparedCombinations) {
            val currentBest = overallBest
            if (currentBest != null && prepared.upperBound <= currentBest.objective) {
                continue
            }

            val allSpecial = searchAllSpecial(prepared.taskEntries)
            if (allSpecial != null) {
                overallBest = allSpecial
                break
            }

            val candidate = searchBestAssignment(prepared.taskEntries)
            if (candidate != null && (overallBest == null || candidate.objective > overallBest.objective)) {
                overallBest = candidate
            }
        }

        val solution = overallBest
            ?: return failure("没有找到有效的派遣方案。")

        return PetDispatchResult(
            isSuccess = true,
            totalScore = solution.totalScore,
            borrowedCount = solution.borrowedCount,
            totalPets = solution.totalPets,
            taskCount = solution.assignments.size,
            allSpecial = solution.allSpecial,
            tierSummary = solution.tierSummary,
            assignments = solution.assignments,
        )
    }

    fun buildTextReport(
        regionName: String,
        result: PetDispatchResult,
    ): String {
        if (!result.isSuccess) {
            return result.errorMessage ?: "计算失败。"
        }

        val lines = mutableListOf<String>()
        lines += "===== 最优派遣方案结果 ====="
        lines += "计算完成！方案计算总耗时：${"%.2f".format(result.calculationTimeMs / 1000f)} 秒"
        lines += "派遣区域：$regionName"
        lines += "执行任务数量：${result.taskCount}"
        lines += "总得分：${result.totalScore}"
        lines += "借用宠物数量：${result.borrowedCount}"
        lines += "总使用宠物数量：${result.totalPets}"

        result.assignments.forEachIndexed { index, assignment ->
            lines += ""
            lines += "--- 任务${index + 1} ---"
            lines += "任务名称：${assignment.task.task}"
            lines += "任务区域：${assignment.task.area}"
            lines += "加成特性：${assignment.task.bonusSkills.ifEmpty { listOf("无") }.joinToString()}"
            lines += "推荐派遣宠物：${assignment.team.joinToString { pet -> pet.name + if (pet.isBorrowed) "（借）" else "" }}"
            lines += "任务得分：${assignment.score}"
            lines += "预计奖励等级：${assignment.rewardTier.displayName}"
        }

        return lines.joinToString(separator = "\n")
    }

    private fun failure(message: String): PetDispatchResult =
        PetDispatchResult(
            isSuccess = false,
            errorMessage = message,
            textReport = message,
        )

    private fun precomputePetTaskScores(
        availablePets: List<CandidatePet>,
        tasks: List<PetDispatchTask>,
    ): Map<Int, Map<Int, Int>> =
        availablePets
            .map(CandidatePet::pet)
            .distinctBy(PetDispatchPet::id)
            .associate { pet ->
                pet.id to tasks.associate { task ->
                    task.id to scorePetForTask(pet, task)
                }
            }

    private fun scorePetForTask(
        pet: PetDispatchPet,
        task: PetDispatchTask,
    ): Int {
        val total = pet.skills
            .filter { skill -> skill.name in task.bonusSkills }
            .sumOf { skill -> skill.score }
        return if (total != 0) total else pet.baseScore
    }

    private fun buildTaskEntries(
        taskCombination: List<PetDispatchTask>,
        availablePets: List<CandidatePet>,
        petTaskScores: Map<Int, Map<Int, Int>>,
    ): List<TaskEntry> =
        taskCombination.mapIndexed { index, task ->
            buildTaskCandidates(
                task = task,
                originalIndex = index,
                availablePets = availablePets,
                petTaskScores = petTaskScores,
            )
        }

    private fun buildTaskCandidates(
        task: PetDispatchTask,
        originalIndex: Int,
        availablePets: List<CandidatePet>,
        petTaskScores: Map<Int, Map<Int, Int>>,
    ): TaskEntry {
        val bestBySignature = linkedMapOf<CandidateSignature, TaskCandidate>()

        fun register(team: List<CandidatePet>) {
            val borrowedCount = team.count(CandidatePet::isBorrowed)
            if (borrowedCount > maxBorrowedPerTask) {
                return
            }

            val petNames = team.map { it.pet.name }
            if (petNames.size != petNames.toSet().size) {
                return
            }

            val ownedMask = team
                .filterNot(CandidatePet::isBorrowed)
                .fold(0L) { mask, candidatePet -> mask or candidatePet.pet.id.toOwnedMask() }
            val score = team.sumOf { pet -> petTaskScores.getValue(pet.pet.id).getValue(task.id) }
            val rewardTier = PetDispatchRewardTier.fromScore(score)
            val candidate = TaskCandidate(
                team = team,
                ownedMask = ownedMask,
                borrowedCount = borrowedCount,
                petCount = team.size,
                score = score,
                rewardTier = rewardTier,
                tierSummary = PetDispatchTierSummary.forTier(rewardTier),
            )

            val signature = CandidateSignature(
                ownedMask = ownedMask,
                borrowedCount = borrowedCount,
                petCount = team.size,
            )
            val existing = bestBySignature[signature]
            if (existing == null || isCandidateBetter(candidate, existing)) {
                bestBySignature[signature] = candidate
            }
        }

        for (firstIndex in availablePets.indices) {
            register(listOf(availablePets[firstIndex]))
            for (secondIndex in firstIndex + 1 until availablePets.size) {
                register(listOf(availablePets[firstIndex], availablePets[secondIndex]))
                for (thirdIndex in secondIndex + 1 until availablePets.size) {
                    register(
                        listOf(
                            availablePets[firstIndex],
                            availablePets[secondIndex],
                            availablePets[thirdIndex],
                        ),
                    )
                }
            }
        }

        val candidates = bestBySignature.values.sortedWith(
            compareByDescending<TaskCandidate> { it.rewardTier.rank }
                .thenBy(TaskCandidate::petCount)
                .thenBy(TaskCandidate::borrowedCount)
                .thenByDescending(TaskCandidate::score),
        )
        val specialCandidates = candidates
            .filter { it.rewardTier == PetDispatchRewardTier.SPECIAL }
            .sortedWith(
                compareBy<TaskCandidate>(TaskCandidate::petCount)
                    .thenBy(TaskCandidate::borrowedCount)
                    .thenByDescending(TaskCandidate::score),
            )

        val bestRewardRank = candidates.maxOfOrNull { it.rewardTier.rank } ?: 0
        return TaskEntry(
            task = task,
            originalIndex = originalIndex,
            candidates = candidates,
            specialCandidates = specialCandidates,
            bestTierSummary = PetDispatchTierSummary.forTier(bestTierFromRank(bestRewardRank)),
            bestRewardRank = bestRewardRank,
            maxScore = candidates.maxOfOrNull(TaskCandidate::score) ?: 0,
            minPetCount = candidates.minOfOrNull(TaskCandidate::petCount) ?: 0,
            minBorrowed = candidates.minOfOrNull(TaskCandidate::borrowedCount) ?: 0,
        )
    }

    private fun isCandidateBetter(
        candidate: TaskCandidate,
        existing: TaskCandidate,
    ): Boolean =
        compareValuesBy(
            candidate,
            existing,
            { it.rewardTier.rank },
            TaskCandidate::score,
        ) > 0

    private fun searchAllSpecial(taskEntries: List<TaskEntry>): Solution? {
        if (taskEntries.any { it.specialCandidates.isEmpty() }) {
            return null
        }

        val orderedTasks = getTaskSearchOrder(taskEntries)
        val chosenPairs = arrayOfNulls<ChosenPair>(orderedTasks.size)
        val failedStates = mutableSetOf<SearchState>()

        fun dfs(
            taskIndex: Int,
            usedOwnedMask: Long,
            borrowedTotal: Int,
        ): Boolean {
            if (taskIndex == orderedTasks.size) {
                return true
            }

            val state = SearchState(taskIndex, usedOwnedMask, borrowedTotal)
            if (!failedStates.add(state)) {
                return false
            }

            val taskEntry = orderedTasks[taskIndex]
            for (candidate in taskEntry.specialCandidates) {
                if (borrowedTotal + candidate.borrowedCount > maxTotalBorrowed) {
                    continue
                }
                if ((candidate.ownedMask and usedOwnedMask) != 0L) {
                    continue
                }

                chosenPairs[taskIndex] = ChosenPair(taskEntry, candidate)
                if (dfs(taskIndex + 1, usedOwnedMask or candidate.ownedMask, borrowedTotal + candidate.borrowedCount)) {
                    return true
                }
            }

            return false
        }

        if (!dfs(0, 0L, 0)) {
            return null
        }

        return buildSolution(
            taskCount = taskEntries.size,
            chosenPairs = chosenPairs.filterNotNull(),
        )
    }

    private fun searchBestAssignment(taskEntries: List<TaskEntry>): Solution? {
        if (taskEntries.any { it.candidates.isEmpty() }) {
            return null
        }

        val orderedTasks = getTaskSearchOrder(taskEntries)
        val taskCount = orderedTasks.size
        val suffixBestTierSummaries = Array(taskCount + 1) { PetDispatchTierSummary() }
        val suffixMinBorrowed = IntArray(taskCount + 1)
        val suffixMinPets = IntArray(taskCount + 1)
        val suffixMaxScore = IntArray(taskCount + 1)

        for (index in taskCount - 1 downTo 0) {
            val taskEntry = orderedTasks[index]
            suffixBestTierSummaries[index] = taskEntry.bestTierSummary + suffixBestTierSummaries[index + 1]
            suffixMinBorrowed[index] = taskEntry.minBorrowed + suffixMinBorrowed[index + 1]
            suffixMinPets[index] = taskEntry.minPetCount + suffixMinPets[index + 1]
            suffixMaxScore[index] = taskEntry.maxScore + suffixMaxScore[index + 1]
        }

        val chosenPairs = arrayOfNulls<ChosenPair>(taskCount)
        val prefixCache = mutableMapOf<SearchState, PrefixObjective>()
        var bestSolution: Solution? = null

        fun dfs(
            taskIndex: Int,
            usedOwnedMask: Long,
            borrowedTotal: Int,
            tierSummary: PetDispatchTierSummary,
            totalPets: Int,
            totalScore: Int,
        ) {
            val optimisticTierSummary = tierSummary + suffixBestTierSummaries[taskIndex]
            val optimisticObjective = Objective(
                tierSummary = optimisticTierSummary,
                borrowedCount = borrowedTotal + suffixMinBorrowed[taskIndex],
                totalPets = totalPets + suffixMinPets[taskIndex],
                totalScore = totalScore + suffixMaxScore[taskIndex],
            )
            val currentBest = bestSolution
            if (currentBest != null && optimisticObjective <= currentBest.objective) {
                return
            }

            val state = SearchState(taskIndex, usedOwnedMask, borrowedTotal)
            val prefixObjective = PrefixObjective(
                tierSummary = tierSummary,
                totalPets = totalPets,
                totalScore = totalScore,
            )
            val cachedPrefix = prefixCache[state]
            if (cachedPrefix != null && prefixObjective <= cachedPrefix) {
                return
            }
            prefixCache[state] = prefixObjective

            if (taskIndex == taskCount) {
                val solution = buildSolution(
                    taskCount = taskCount,
                    chosenPairs = chosenPairs.filterNotNull(),
                )
                if (bestSolution == null || solution.objective > bestSolution!!.objective) {
                    bestSolution = solution
                }
                return
            }

            val taskEntry = orderedTasks[taskIndex]
            for (candidate in taskEntry.candidates) {
                val nextBorrowedTotal = borrowedTotal + candidate.borrowedCount
                if (nextBorrowedTotal > maxTotalBorrowed) {
                    continue
                }
                if ((candidate.ownedMask and usedOwnedMask) != 0L) {
                    continue
                }

                chosenPairs[taskIndex] = ChosenPair(taskEntry, candidate)
                dfs(
                    taskIndex = taskIndex + 1,
                    usedOwnedMask = usedOwnedMask or candidate.ownedMask,
                    borrowedTotal = nextBorrowedTotal,
                    tierSummary = tierSummary + candidate.tierSummary,
                    totalPets = totalPets + candidate.petCount,
                    totalScore = totalScore + candidate.score,
                )
            }
        }

        dfs(
            taskIndex = 0,
            usedOwnedMask = 0L,
            borrowedTotal = 0,
            tierSummary = PetDispatchTierSummary(),
            totalPets = 0,
            totalScore = 0,
        )
        return bestSolution
    }

    private fun buildSolution(
        taskCount: Int,
        chosenPairs: List<ChosenPair>,
    ): Solution {
        val assignments = arrayOfNulls<PetDispatchAssignment>(taskCount)
        var totalScore = 0
        var borrowedCount = 0
        var totalPets = 0
        var tierSummary = PetDispatchTierSummary()

        for ((taskEntry, candidate) in chosenPairs) {
            val assignment = PetDispatchAssignment(
                task = taskEntry.task,
                team = candidate.team.map { pet ->
                    PetDispatchAssignedPet(
                        id = pet.pet.id,
                        name = pet.pet.name,
                        rarity = pet.pet.rarity,
                        skills = pet.pet.skills,
                        imageAssetName = pet.pet.imageAssetName,
                        isBorrowed = pet.isBorrowed,
                    )
                },
                score = candidate.score,
                rewardTier = candidate.rewardTier,
            )
            assignments[taskEntry.originalIndex] = assignment
            totalScore += candidate.score
            borrowedCount += candidate.borrowedCount
            totalPets += candidate.petCount
            tierSummary += candidate.tierSummary
        }

        return Solution(
            assignments = assignments.filterNotNull(),
            totalScore = totalScore,
            borrowedCount = borrowedCount,
            totalPets = totalPets,
            tierSummary = tierSummary,
            objective = Objective(
                tierSummary = tierSummary,
                borrowedCount = borrowedCount,
                totalPets = totalPets,
                totalScore = totalScore,
            ),
            allSpecial = tierSummary.specialCount == taskCount,
        )
    }

    private fun getTaskSearchOrder(taskEntries: List<TaskEntry>): List<TaskEntry> =
        taskEntries.sortedWith(
            compareBy<TaskEntry> { it.specialCandidates.size }
                .thenBy { it.candidates.size }
                .thenByDescending { it.bestRewardRank }
                .thenByDescending { it.maxScore },
        )

    private fun generateTaskCombinations(
        validTasks: List<PetDispatchTask>,
        taskCount: Int,
    ): List<List<PetDispatchTask>> {
        if (validTasks.size < taskCount) {
            return emptyList()
        }

        val sortedTasks = validTasks.sortedByDescending { it.bonusSkills.size }
        val combinations = mutableListOf<List<PetDispatchTask>>()

        fun dfs(
            startIndex: Int,
            current: MutableList<PetDispatchTask>,
        ) {
            if (current.size == taskCount) {
                combinations += current.toList()
                return
            }
            val remainingSlots = taskCount - current.size
            val maxStart = sortedTasks.size - remainingSlots
            for (index in startIndex..maxStart) {
                current += sortedTasks[index]
                dfs(index + 1, current)
                current.removeAt(current.lastIndex)
            }
        }

        dfs(0, mutableListOf())
        return combinations
    }

    private fun getCombinationUpperBound(taskEntries: List<TaskEntry>): Pair<Boolean, Objective> {
        var tierSummary = PetDispatchTierSummary()
        var minBorrowed = 0
        var minPets = 0
        var maxScore = 0
        var allSpecialPossible = true

        taskEntries.forEach { taskEntry ->
            tierSummary += taskEntry.bestTierSummary
            minBorrowed += taskEntry.minBorrowed
            minPets += taskEntry.minPetCount
            maxScore += taskEntry.maxScore
            if (taskEntry.bestRewardRank != PetDispatchRewardTier.SPECIAL.rank) {
                allSpecialPossible = false
            }
        }

        return allSpecialPossible to Objective(
            tierSummary = tierSummary,
            borrowedCount = minBorrowed,
            totalPets = minPets,
            totalScore = maxScore,
        )
    }

    private fun bestTierFromRank(rank: Int): PetDispatchRewardTier =
        PetDispatchRewardTier.entries.firstOrNull { it.rank == rank } ?: PetDispatchRewardTier.NONE

    private data class CandidatePet(
        val pet: PetDispatchPet,
        val isBorrowed: Boolean,
    )

    private data class CandidateSignature(
        val ownedMask: Long,
        val borrowedCount: Int,
        val petCount: Int,
    )

    private data class TaskCandidate(
        val team: List<CandidatePet>,
        val ownedMask: Long,
        val borrowedCount: Int,
        val petCount: Int,
        val score: Int,
        val rewardTier: PetDispatchRewardTier,
        val tierSummary: PetDispatchTierSummary,
    )

    private data class TaskEntry(
        val task: PetDispatchTask,
        val originalIndex: Int,
        val candidates: List<TaskCandidate>,
        val specialCandidates: List<TaskCandidate>,
        val bestTierSummary: PetDispatchTierSummary,
        val bestRewardRank: Int,
        val maxScore: Int,
        val minPetCount: Int,
        val minBorrowed: Int,
    )

    private data class ChosenPair(
        val taskEntry: TaskEntry,
        val candidate: TaskCandidate,
    )

    private data class PreparedCombination(
        val taskEntries: List<TaskEntry>,
        val allSpecialPossible: Boolean,
        val upperBound: Objective,
    )

    private data class Solution(
        val assignments: List<PetDispatchAssignment>,
        val totalScore: Int,
        val borrowedCount: Int,
        val totalPets: Int,
        val tierSummary: PetDispatchTierSummary,
        val objective: Objective,
        val allSpecial: Boolean,
    )

    private data class SearchState(
        val taskIndex: Int,
        val usedOwnedMask: Long,
        val borrowedTotal: Int,
    )

    private data class PrefixObjective(
        val tierSummary: PetDispatchTierSummary,
        val totalPets: Int,
        val totalScore: Int,
    ) : Comparable<PrefixObjective> {
        override fun compareTo(other: PrefixObjective): Int =
            compareValuesBy(
                this,
                other,
                PrefixObjective::tierSummarySpecial,
                PrefixObjective::tierSummaryFirst,
                PrefixObjective::tierSummarySecond,
                PrefixObjective::tierSummaryThird,
                PrefixObjective::tierSummaryFourth,
                { -it.totalPets },
                PrefixObjective::totalScore,
            )

        private val tierSummarySpecial: Int
            get() = tierSummary.specialCount
        private val tierSummaryFirst: Int
            get() = tierSummary.firstCount
        private val tierSummarySecond: Int
            get() = tierSummary.secondCount
        private val tierSummaryThird: Int
            get() = tierSummary.thirdCount
        private val tierSummaryFourth: Int
            get() = tierSummary.fourthCount
    }

    private data class Objective(
        val tierSummary: PetDispatchTierSummary,
        val borrowedCount: Int,
        val totalPets: Int,
        val totalScore: Int,
    ) : Comparable<Objective> {
        override fun compareTo(other: Objective): Int =
            compareValuesBy(
                this,
                other,
                Objective::tierSummarySpecial,
                Objective::tierSummaryFirst,
                Objective::tierSummarySecond,
                Objective::tierSummaryThird,
                Objective::tierSummaryFourth,
                { -it.borrowedCount },
                { -it.totalPets },
                Objective::totalScore,
            )

        private val tierSummarySpecial: Int
            get() = tierSummary.specialCount
        private val tierSummaryFirst: Int
            get() = tierSummary.firstCount
        private val tierSummarySecond: Int
            get() = tierSummary.secondCount
        private val tierSummaryThird: Int
            get() = tierSummary.thirdCount
        private val tierSummaryFourth: Int
            get() = tierSummary.fourthCount
    }
}

private fun Int.toOwnedMask(): Long = 1L shl (this - 1)
