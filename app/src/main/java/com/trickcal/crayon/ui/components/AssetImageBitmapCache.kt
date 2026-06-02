package com.trickcal.crayon.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal object AssetImageBitmapCache {
    private val bitmapCache = object : LruCache<String, ImageBitmap>(64) {}

    fun getOrLoad(
        context: Context,
        assetPath: String,
        maxDimensionPx: Int = 256,
    ): ImageBitmap? {
        if (assetPath.isBlank()) {
            return null
        }

        synchronized(bitmapCache) {
            bitmapCache.get(assetPath)
        }?.let { return it }

        val imageBitmap = runCatching {
            context.assets.open(assetPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@runCatching null
                val scaledBitmap = bitmap.scaleDownIfNeeded(maxDimensionPx)
                scaledBitmap.asImageBitmap()
            }
        }.getOrNull() ?: return null

        synchronized(bitmapCache) {
            bitmapCache.put(assetPath, imageBitmap)
        }
        return imageBitmap
    }
}

private fun Bitmap.scaleDownIfNeeded(maxDimensionPx: Int): Bitmap {
    val maxDimension = maxOf(width, height)
    if (maxDimension <= maxDimensionPx) {
        return this
    }
    val scale = maxDimensionPx.toFloat() / maxDimension.toFloat()
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}
