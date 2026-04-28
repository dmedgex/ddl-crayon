package com.trickcal.crayon.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PaintProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CrayonDatabase : RoomDatabase() {
    abstract fun paintProgressDao(): PaintProgressDao
}
