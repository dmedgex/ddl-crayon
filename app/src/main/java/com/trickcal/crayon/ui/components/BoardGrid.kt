package com.trickcal.crayon.ui.components

import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.R
import com.trickcal.crayon.model.BoardCellType
import com.trickcal.crayon.model.BoardLayerSpec

@Composable
fun BoardGrid(
    layer: BoardLayerSpec,
    litSlots: Set<String>,
    modifier: Modifier = Modifier,
) {
    val spacing = if (layer.columns >= 6) 4.dp else 8.dp
    val cellShape = RoundedCornerShape(if (layer.columns >= 6) 10.dp else 12.dp)
    val visibleRows = layer.cells
        .chunked(layer.columns)
        .filter { rowCells -> rowCells.any { cell -> cell.type != BoardCellType.EMPTY } }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        visibleRows.forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                rowCells.forEach { cell ->
                    val slot = layer.slotForCell(cell)
                    val isUnlocked = cell.slotId in litSlots
                    if (cell.type == BoardCellType.EMPTY) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                        return@forEach
                    }
                    val borderColor = when {
                        slot != null && isUnlocked -> attributeTint(slot.attributeType)
                        slot != null -> attributeTint(slot.attributeType).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(cellShape)
                            .border(
                                width = if (isUnlocked && slot != null) 2.dp else 1.dp,
                                color = borderColor,
                                shape = cellShape,
                            ),
                    ) {
                        BoardCellVisual(
                            type = cell.type,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (slot != null && isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(attributeTint(slot.attributeType).copy(alpha = 0.18f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCellVisual(
    type: BoardCellType,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawableRes = cellDrawableRes(type)
    val canUseRasterPainter = remember(drawableRes, context) {
        context.resources.isRasterDrawable(drawableRes)
    }
    val painter = if (canUseRasterPainter) {
        runCatching { painterResource(id = drawableRes) }.getOrNull()
    } else {
        null
    }

    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = type.name,
            modifier = modifier,
            contentScale = ContentScale.FillBounds,
        )
        return
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        cellFallbackColor(type),
                        cellFallbackColor(type).copy(alpha = 0.82f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = cellFallbackBorderColor(type),
                shape = RoundedCornerShape(0.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (type == BoardCellType.START || type == BoardCellType.END) {
            Text(
                text = if (type == BoardCellType.START) "起" else "终",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Resources.isRasterDrawable(drawableRes: Int): Boolean {
    val typedValue = TypedValue()
    getValue(drawableRes, typedValue, true)
    val filePath = typedValue.string?.toString()?.lowercase() ?: return false
    return filePath.endsWith(".png") ||
        filePath.endsWith(".jpg") ||
        filePath.endsWith(".jpeg") ||
        filePath.endsWith(".webp")
}

private fun cellFallbackColor(type: BoardCellType): Color =
    when (type) {
        BoardCellType.EMPTY -> Color.Transparent
        BoardCellType.WHITE -> Color(0xFFFFF9F0)
        BoardCellType.PURPLE -> Color(0xFFE8DDF8)
        BoardCellType.START -> Color(0xFF6AA67D)
        BoardCellType.END -> Color(0xFFD96C59)
        BoardCellType.ATTACK -> Color(0xFFF19A84)
        BoardCellType.CRIT -> Color(0xFFF2B46C)
        BoardCellType.HEALTH -> Color(0xFF7BC67E)
        BoardCellType.DEFENSE -> Color(0xFF7A9AF8)
        BoardCellType.CRIT_RESIST -> Color(0xFFB18AE8)
        BoardCellType.PURPLE_ATTACK -> Color(0xFFE0B8EF)
        BoardCellType.PURPLE_CRIT -> Color(0xFFDAB0F3)
        BoardCellType.PURPLE_HEALTH -> Color(0xFFBED4EB)
        BoardCellType.PURPLE_DEFENSE -> Color(0xFFC6C2F2)
        BoardCellType.PURPLE_CRIT_RESIST -> Color(0xFFD9C1F1)
    }

private fun cellFallbackBorderColor(type: BoardCellType): Color =
    when (type) {
        BoardCellType.WHITE -> Color(0xFFD5C8B6)
        BoardCellType.PURPLE,
        BoardCellType.PURPLE_ATTACK,
        BoardCellType.PURPLE_CRIT,
        BoardCellType.PURPLE_HEALTH,
        BoardCellType.PURPLE_DEFENSE,
        BoardCellType.PURPLE_CRIT_RESIST -> Color(0xFF9B7AB8)
        BoardCellType.START,
        BoardCellType.END,
        BoardCellType.ATTACK,
        BoardCellType.CRIT,
        BoardCellType.HEALTH,
        BoardCellType.DEFENSE,
        BoardCellType.CRIT_RESIST -> Color(0xFF6F5B4B)
        BoardCellType.EMPTY -> Color.Transparent
    }

private fun cellDrawableRes(type: BoardCellType): Int =
    when (type) {
        BoardCellType.EMPTY -> error("Empty cell does not have a drawable.")
        BoardCellType.WHITE -> R.drawable.cell_white
        BoardCellType.PURPLE -> R.drawable.cell_purple
        BoardCellType.START -> R.drawable.cell_start
        BoardCellType.END -> R.drawable.cell_end
        BoardCellType.ATTACK -> R.drawable.cell_attack
        BoardCellType.CRIT -> R.drawable.cell_crit
        BoardCellType.HEALTH -> R.drawable.cell_health
        BoardCellType.DEFENSE -> R.drawable.cell_defense
        BoardCellType.CRIT_RESIST -> R.drawable.cell_crit_resist
        BoardCellType.PURPLE_ATTACK -> R.drawable.cell_purple_attack
        BoardCellType.PURPLE_CRIT -> R.drawable.cell_purple_crit
        BoardCellType.PURPLE_HEALTH -> R.drawable.cell_purple_health
        BoardCellType.PURPLE_DEFENSE -> R.drawable.cell_purple_defense
        BoardCellType.PURPLE_CRIT_RESIST -> R.drawable.cell_purple_crit_resist
    }
