package com.trickcal.crayon.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val avatarPalettes = listOf(
    listOf(Color(0xFFEFA07A), Color(0xFFD96B52)),
    listOf(Color(0xFF85C1B4), Color(0xFF5E9980)),
    listOf(Color(0xFFA6B7F6), Color(0xFF7768AE)),
    listOf(Color(0xFFF2CC8F), Color(0xFFEDAE49)),
    listOf(Color(0xFFEEA5B2), Color(0xFFD2647F)),
)

@Composable
fun AvatarBadge(
    name: String,
    avatarKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    fillBounds: Boolean = false,
) {
    val context = LocalContext.current
    val avatarResId = remember(avatarKey, context.packageName) {
        context.resolveDrawableResId(avatarKey)
    }
    val imageBitmap = remember(avatarResId, context) {
        DrawableImageBitmapCache.getOrLoad(
            context = context,
            drawableResId = avatarResId,
            maxDimensionPx = 256,
        )
    }
    val shape = RoundedCornerShape(18.dp)
    val seed = avatarKey.hashCode() and Int.MAX_VALUE
    val palette = avatarPalettes[seed % avatarPalettes.size]
    val baseModifier = if (fillBounds) {
        modifier
            .fillMaxSize()
            .clip(shape)
    } else {
        modifier
            .size(size)
            .clip(shape)
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = name,
            modifier = baseModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = baseModifier
                .background(
                    Brush.linearGradient(colors = palette),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.takeLast(1),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
