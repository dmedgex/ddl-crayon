package com.trickcal.crayon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.R
import com.trickcal.crayon.model.AttributeType

@Composable
fun UnlockableCellIcon(
    attributeType: AttributeType,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    UnlockableCellVisual(
        attributeType = attributeType,
        isUnlocked = isUnlocked,
        modifier = modifier,
        size = size,
    )
}

@Composable
fun UnlockableCellToggle(
    attributeType: AttributeType,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    UnlockableCellVisual(
        attributeType = attributeType,
        isUnlocked = isUnlocked,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        size = size,
    )
}

@Composable
fun AttributeCellFilterStrip(
    selectedAttributes: Set<AttributeType>,
    onToggleAttribute: (AttributeType) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    imageScale: Float = 0.9f,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - spacing * (AttributeType.entries.size - 1)) / AttributeType.entries.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttributeType.entries.forEach { attributeType ->
                BoxWithConstraints(
                    modifier = Modifier
                        .width(cellWidth)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleAttribute(attributeType) },
                    contentAlignment = Alignment.Center,
                ) {
                    UnlockableCellIcon(
                        attributeType = attributeType,
                        isUnlocked = attributeType in selectedAttributes,
                        size = maxWidth * imageScale,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockableCellVisual(
    attributeType: AttributeType,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val context = LocalContext.current
    val drawableResId = remember(attributeType) {
        unlockableCellDrawableRes(attributeType)
    }
    val imageBitmap = remember(drawableResId, context) {
        DrawableImageBitmapCache.getOrLoad(
            context = context,
            drawableResId = drawableResId,
            maxDimensionPx = 128,
        )
    }
    val alphaValue = if (isUnlocked) 1f else 0.4f
    val shape = RoundedCornerShape(6.dp)
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = attributeType.displayName,
            modifier = modifier
                .size(size)
                .alpha(alphaValue),
            contentScale = ContentScale.Fit,
        )
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .alpha(alphaValue)
            .clip(shape)
            .background(attributeTint(attributeType).copy(alpha = 0.16f))
            .border(
                width = 1.dp,
                color = attributeTint(attributeType).copy(alpha = 0.6f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AttributeGlyph(
            attributeType = attributeType,
            highlighted = isUnlocked,
        )
    }
}

private fun unlockableCellDrawableRes(attributeType: AttributeType): Int =
    when (attributeType) {
        AttributeType.ATTACK -> R.drawable.cell_attack
        AttributeType.CRIT -> R.drawable.cell_crit
        AttributeType.HEALTH -> R.drawable.cell_health
        AttributeType.DEFENSE -> R.drawable.cell_defense
        AttributeType.CRIT_RESIST -> R.drawable.cell_crit_resist
    }
