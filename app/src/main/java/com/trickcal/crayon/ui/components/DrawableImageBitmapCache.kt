package com.trickcal.crayon.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal object DrawableImageBitmapCache {
    private val bitmapCache = object : LruCache<Int, ImageBitmap>(48) {}

    fun getOrLoad(
        context: Context,
        drawableResId: Int,
        maxDimensionPx: Int = 256,
    ): ImageBitmap? {
        if (drawableResId == 0) {
            return null
        }
        val cacheKey = drawableResId * 10_000 + maxDimensionPx
        synchronized(bitmapCache) {
            bitmapCache.get(cacheKey)
        }?.let { return it }

        val imageBitmap = AppCompatResources.getDrawable(context, drawableResId)
            ?.toImageBitmapOrNull(maxDimensionPx)
            ?: return null

        synchronized(bitmapCache) {
            bitmapCache.put(cacheKey, imageBitmap)
        }
        return imageBitmap
    }
}

internal fun Context.resolveDrawableResId(name: String): Int =
    name
        .takeIf(String::isNotBlank)
        ?.let { key -> resources.getIdentifier(key, "drawable", packageName) }
        ?: 0

private fun Drawable.toImageBitmapOrNull(maxDimensionPx: Int) =
    runCatching {
        val intrinsicWidth = intrinsicWidth.takeIf { it > 0 } ?: maxDimensionPx
        val intrinsicHeight = intrinsicHeight.takeIf { it > 0 } ?: maxDimensionPx
        val scale = minOf(
            1f,
            maxDimensionPx.toFloat() / intrinsicWidth,
            maxDimensionPx.toFloat() / intrinsicHeight,
        )
        val width = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()
