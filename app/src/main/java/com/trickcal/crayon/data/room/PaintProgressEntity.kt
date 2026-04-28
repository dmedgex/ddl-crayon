package com.trickcal.crayon.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paint_progress")
data class PaintProgressEntity(
    @PrimaryKey val slotId: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
