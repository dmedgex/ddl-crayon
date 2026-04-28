package com.trickcal.crayon.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaintProgressDao {
    @Query("SELECT * FROM paint_progress")
    fun observeAll(): Flow<List<PaintProgressEntity>>

    @Query("SELECT * FROM paint_progress")
    suspend fun getAll(): List<PaintProgressEntity>

    @Query("SELECT * FROM paint_progress WHERE slotId = :slotId LIMIT 1")
    suspend fun getBySlotId(slotId: String): PaintProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PaintProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaintProgressEntity>)

    @Query("DELETE FROM paint_progress WHERE slotId = :slotId")
    suspend fun deleteBySlotId(slotId: String)

    @Query("DELETE FROM paint_progress WHERE slotId IN (:slotIds)")
    suspend fun deleteBySlotIds(slotIds: List<String>)

    @Query("DELETE FROM paint_progress")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<PaintProgressEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            insertAll(entities)
        }
    }
}
