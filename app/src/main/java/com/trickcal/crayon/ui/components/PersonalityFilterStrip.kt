package com.trickcal.crayon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.model.PersonalityType

@Composable
fun PersonalityFilterStrip(
    selectedPersonality: PersonalityType?,
    onTogglePersonality: (PersonalityType) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    imageScale: Float = 0.9f,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - spacing * (PersonalityType.entries.size - 1)) / PersonalityType.entries.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonalityType.entries.forEach { personality ->
                BoxWithConstraints(
                    modifier = Modifier
                        .width(cellWidth)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTogglePersonality(personality) },
                    contentAlignment = Alignment.Center,
                ) {
                    PersonalityIcon(
                        personality = personality,
                        isSelected = selectedPersonality == personality,
                        size = maxWidth * imageScale,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalityIcon(
    personality: PersonalityType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val context = LocalContext.current
    val drawableResId = remember(personality, context.packageName) {
        context.resolveDrawableResId(personality.drawableName)
    }
    val imageBitmap = remember(drawableResId, context) {
        DrawableImageBitmapCache.getOrLoad(
            context = context,
            drawableResId = drawableResId,
            maxDimensionPx = 128,
        )
    }
    val alphaValue = if (isSelected) 1f else 0.4f
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = personality.displayName,
            modifier = modifier.alpha(alphaValue),
            contentScale = ContentScale.Fit,
        )
        return
    }

    Box(
        modifier = modifier
            .alpha(alphaValue)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = personality.displayName,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
