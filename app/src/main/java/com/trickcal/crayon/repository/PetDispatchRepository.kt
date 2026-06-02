package com.trickcal.crayon.repository

import android.content.Context
import com.trickcal.crayon.data.local.PetDispatchCatalogAssetLoader
import com.trickcal.crayon.model.PetDispatchCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PetDispatchRepository(
    private val context: Context,
) {
    private val loadMutex = Mutex()
    @Volatile
    private var cachedCatalog: PetDispatchCatalog? = null

    suspend fun loadCatalog(): PetDispatchCatalog {
        cachedCatalog?.let { return it }
        return loadMutex.withLock {
            cachedCatalog?.let { return@withLock it }
            val loadedCatalog = withContext(Dispatchers.IO) {
                PetDispatchCatalogAssetLoader.load(context.applicationContext)
            }
            cachedCatalog = loadedCatalog
            loadedCatalog
        }
    }
}
