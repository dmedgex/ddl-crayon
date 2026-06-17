package com.trickcal.crayon.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.R

@Composable
fun ImageUriPreview(
    imageUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    circular: Boolean = false,
) {
    val context = LocalContext.current
    val bitmap = remember(context, imageUri) {
        if (imageUri == null) {
            DrawableImageBitmapCache.getOrLoad(context, R.drawable.none, maxDimensionPx = 256)
        } else {
            runCatching {
                context.contentResolver.openInputStream(imageUri).use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                    decoded.scaleDownIfNeeded(256).asImageBitmap()
                }
            }.getOrNull()
        }
    }
    val shape: Shape = if (circular) CircleShape else RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFFEDE7DF)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun Bitmap.scaleDownIfNeeded(maxDimensionPx: Int): Bitmap {
    val maxDimension = maxOf(width, height)
    if (maxDimension <= maxDimensionPx) {
        return this
    }
    val scale = maxDimensionPx.toFloat() / maxDimension
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
