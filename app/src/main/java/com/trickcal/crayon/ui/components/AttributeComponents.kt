package com.trickcal.crayon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.model.AttributeType

fun attributeTint(attributeType: AttributeType): Color =
    when (attributeType) {
        AttributeType.ATTACK -> Color(0xFFE76F51)
        AttributeType.CRIT -> Color(0xFFF4A261)
        AttributeType.HEALTH -> Color(0xFF4CAF50)
        AttributeType.DEFENSE -> Color(0xFF4C6EF5)
        AttributeType.CRIT_RESIST -> Color(0xFF9C6ADE)
    }

fun attributeIcon(attributeType: AttributeType): ImageVector =
    when (attributeType) {
        AttributeType.ATTACK -> Icons.Filled.FlashOn
        AttributeType.CRIT -> Icons.Filled.Star
        AttributeType.HEALTH -> Icons.Filled.Favorite
        AttributeType.DEFENSE -> Icons.Filled.Security
        AttributeType.CRIT_RESIST -> Icons.Filled.VerifiedUser
    }

fun attributeIconDrawableName(attributeType: AttributeType): String =
    when (attributeType) {
        AttributeType.ATTACK -> "icon_attack"
        AttributeType.CRIT -> "icon_crit"
        AttributeType.HEALTH -> "icon_health"
        AttributeType.DEFENSE -> "icon_defense"
        AttributeType.CRIT_RESIST -> "icon_crit_resist"
    }

@Composable
fun AttributeGlyph(
    attributeType: AttributeType,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    padding: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val tint = attributeTint(attributeType)
    val context = LocalContext.current
    val drawableResId = remember(attributeType, context.packageName) {
        context.resolveDrawableResId(attributeIconDrawableName(attributeType))
    }
    val imageBitmap = remember(drawableResId, context) {
        DrawableImageBitmapCache.getOrLoad(
            context = context,
            drawableResId = drawableResId,
            maxDimensionPx = 96,
        )
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (highlighted) tint.copy(alpha = 0.18f) else Color.Transparent)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = attributeType.displayName,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                imageVector = attributeIcon(attributeType),
                contentDescription = attributeType.displayName,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun AttributeBadgeChip(
    attributeType: AttributeType,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val tint = attributeTint(attributeType)
    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = if (selected) tint else MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (selected) 4.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
            )
            .then(clickableModifier),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                AttributeGlyph(attributeType = attributeType, highlighted = selected)
            }
            if (showLabel) {
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = attributeType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
fun AttributeBonusText(
    attributeType: AttributeType,
    bonusPercent: Int,
    modifier: Modifier = Modifier,
) {
    if (bonusPercent <= 0) {
        return
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AttributeGlyph(attributeType = attributeType)
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = "${attributeType.displayName} +${bonusPercent}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
