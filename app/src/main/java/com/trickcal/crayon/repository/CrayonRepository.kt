package com.trickcal.crayon.repository

import com.trickcal.crayon.domain.statistics.StatisticsCalculator
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.ProgressConfig
import com.trickcal.crayon.model.StatisticsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CrayonRepository(
    private val catalogRepository: CatalogRepository,
    private val paintProgressRepository: PaintProgressRepository,
) {
    fun observeCharacters(): Flow<List<CharacterProfile>> = catalogRepository.observeCharacters()

    fun observeCharacter(characterId: String): Flow<CharacterProfile?> =
        observeCharacters().map { characters ->
            characters.firstOrNull { it.id == characterId }
        }

    fun observeLitSlots(): Flow<Set<String>> = paintProgressRepository.observeLitSlots()

    fun observeStatistics(): Flow<StatisticsSnapshot> =
        combine(observeCharacters(), observeLitSlots()) { characters, litSlots ->
            StatisticsCalculator.calculate(characters = characters, litSlots = litSlots)
        }

    suspend fun toggleSlot(slotId: String) {
        paintProgressRepository.toggleSlot(slotId)
    }

    suspend fun setSlotLit(slotId: String, isLit: Boolean) {
        paintProgressRepository.setSlotLit(slotId, isLit)
    }

    suspend fun setSlotsLit(slotIds: Set<String>, isLit: Boolean) {
        paintProgressRepository.setSlotsLit(slotIds, isLit)
    }

    suspend fun exportProgress(): ProgressConfig =
        ProgressConfig(
            exportedAt = System.currentTimeMillis(),
            litSlotIds = paintProgressRepository.getLitSlots().sorted(),
        )

    fun getValidSlotIds(): Set<String> = catalogRepository.getAllSlotIds()

    suspend fun replaceProgress(slotIds: Set<String>) {
        paintProgressRepository.replaceProgress(slotIds.intersect(getValidSlotIds()))
    }

    fun getCharacter(characterId: String): CharacterProfile? =
        catalogRepository.getCharacter(characterId)
}
