package com.trickcal.crayon.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.ui.components.AvatarBadge
import com.trickcal.crayon.ui.components.BoardGrid
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.SectionTitle

@Composable
fun CharacterDetailScreen(
    uiState: CharacterDetailUiState,
    modifier: Modifier = Modifier,
) {
    val character = uiState.character
    if (character == null) {
        EmptyStateCard(
            modifier = modifier.padding(16.dp),
            title = "使徒不存在",
            message = "未找到对应使徒数据，请返回使徒列表后重新进入。",
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarBadge(
                            name = character.name,
                            avatarKey = character.avatarKey,
                            size = 72.dp,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "当前已解锁 ${character.litSlotCount(uiState.litSlots)} / ${character.allAttributeSlots().size}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        items(character.layers, key = { it.tier.name }) { layer ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(
                        title = layer.tier.displayName,
                        subtitle = "每解锁 1 个金蜡笔格子 +${layer.tier.bonusPercentPerSlot}% ，消耗 ${layer.tier.goldCrayonCostPerSlot} 根金蜡笔",
                    )
                    BoardGrid(
                        layer = layer,
                        litSlots = uiState.litSlots,
                    )
                }
            }
        }
    }
}
