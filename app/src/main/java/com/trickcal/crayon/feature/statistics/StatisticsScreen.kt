package com.trickcal.crayon.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.R
import com.trickcal.crayon.model.AttributeStatistics
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.TierAttributeStatistics
import com.trickcal.crayon.model.TierStatistics
import com.trickcal.crayon.ui.components.AttributeBonusText
import com.trickcal.crayon.ui.components.AttributeGlyph
import com.trickcal.crayon.ui.components.MetricCard
import com.trickcal.crayon.ui.components.SectionTitle

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.snapshot
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "总览",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "已使用金蜡笔",
                    value = snapshot.overview.totalGoldCrayons.toString(),
                    valueImageRes = R.drawable.crayon_gold,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "已解锁金蜡笔格子",
                    value = "${snapshot.overview.litColoredSlots} / ${snapshot.overview.totalColoredSlots}",
                    valueImageRes = R.drawable.cell,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "当前总加成情况",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "累计加成：${snapshot.overview.totalBonusPercent}%",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    AttributeType.entries.forEach { attribute ->
                        AttributeBonusText(
                            attributeType = attribute,
                            bonusPercent = snapshot.overview.bonusByAttribute[attribute] ?: 0,
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "按属性统计",
            )
        }

        items(snapshot.attributeStatistics, key = { it.attributeType.name }) { item ->
            AttributeStatCard(
                item = item,
                tierStatistics = snapshot.tierStatistics,
            )
        }
    }
}

@Composable
private fun AttributeStatCard(
    item: AttributeStatistics,
    tierStatistics: List<TierStatistics>,
) {
    var expanded by rememberSaveable(item.attributeType.name) { mutableStateOf(false) }
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AttributeGlyph(
                    attributeType = item.attributeType,
                    highlighted = item.litCount > 0,
                    iconSize = 28.dp,
                    padding = 8.dp,
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.attributeType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "已解锁 ${item.litCount} / ${item.totalCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${item.totalBonusPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${item.goldCrayonCost} 金蜡笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180)),
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tierStatistics.forEach { tierStats ->
                        val tierAttributeStats = tierStats.attributeStatistics.first {
                            it.attributeType == item.attributeType
                        }
                        TierAttributeDetailLine(
                            tier = tierStats.tier,
                            item = tierAttributeStats,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierAttributeDetailLine(
    tier: com.trickcal.crayon.model.BoardTier,
    item: TierAttributeStatistics,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tier.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.9f),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "已解锁 ${item.litCount} / ${item.totalCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2.4f),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "+${item.totalBonusPercent}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.0f),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "${item.goldCrayonCost} 金蜡笔",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
        )
    }
}
