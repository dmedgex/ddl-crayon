package com.trickcal.crayon.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal object FileImageBitmapCache {
    private val bitmapCache = object : LruCache<String, ImageBitmap>(32) {}

    fun getOrLoad(
        context: Context,
        relativePath: String,
        maxDimensionPx: Int = 256,
    ): ImageBitmap? {
        if (relativePath.isBlank()) {
            return null
        }
        synchronized(bitmapCache) {
            bitmapCache.get(relativePath)
        }?.let { return it }

        val file = File(context.filesDir, relativePath)
        if (!file.exists()) {
            return null
        }
        val imageBitmap = runCatching {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
            bitmap.scaleDownIfNeeded(maxDimensionPx).asImageBitmap()
        }.getOrNull() ?: return null

        synchronized(bitmapCache) {
            bitmapCache.put(relativePath, imageBitmap)
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
