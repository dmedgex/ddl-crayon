package com.trickcal.crayon.repository

import android.content.Context
import com.trickcal.crayon.data.local.BattlePowerCharacterCatalogAssetLoader
import com.trickcal.crayon.model.BattlePowerCharacterCatalog

interface BattlePowerCharacterRepository {
    fun loadCatalog(): BattlePowerCharacterCatalog
}

class AssetBattlePowerCharacterRepository(
    private val context: Context,
) : BattlePowerCharacterRepository {
    @Volatile
    private var cachedCatalog: BattlePowerCharacterCatalog? = null

    override fun loadCatalog(): BattlePowerCharacterCatalog {
        cachedCatalog?.let { return it }
        return synchronized(this) {
            cachedCatalog?.let { return@synchronized it }
            val loadedCatalog = BattlePowerCharacterCatalogAssetLoader.load(context.applicationContext)
            cachedCatalog = loadedCatalog
            loadedCatalog
        }
    }
}
