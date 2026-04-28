package com.trickcal.crayon.repository

import com.trickcal.crayon.data.room.PaintProgressDao
import com.trickcal.crayon.data.room.PaintProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaintProgressRepository(
    private val paintProgressDao: PaintProgressDao,
) {
    fun observeLitSlots(): Flow<Set<String>> =
        paintProgressDao.observeAll().map { entities ->
            entities.mapTo(linkedSetOf()) { it.slotId }
        }

    suspend fun setSlotLit(slotId: String, isLit: Boolean) {
        if (isLit) {
            paintProgressDao.insert(PaintProgressEntity(slotId = slotId))
        } else {
            paintProgressDao.deleteBySlotId(slotId)
        }
    }

    suspend fun setSlotsLit(slotIds: Set<String>, isLit: Boolean) {
        if (slotIds.isEmpty()) {
            return
        }
        if (isLit) {
            paintProgressDao.insertAll(
                slotIds.map { slotId -> PaintProgressEntity(slotId = slotId) },
            )
        } else {
            paintProgressDao.deleteBySlotIds(slotIds.toList())
        }
    }

    suspend fun toggleSlot(slotId: String) {
        val exists = paintProgressDao.getBySlotId(slotId) != null
        setSlotLit(slotId = slotId, isLit = !exists)
    }

    suspend fun getLitSlots(): Set<String> =
        paintProgressDao.getAll().mapTo(linkedSetOf()) { it.slotId }

    suspend fun replaceProgress(slotIds: Set<String>) {
        paintProgressDao.replaceAll(
            slotIds.map { slotId -> PaintProgressEntity(slotId = slotId) },
        )
    }
}
