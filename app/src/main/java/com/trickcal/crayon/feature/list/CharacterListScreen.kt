package com.trickcal.crayon.feature.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.BrowseMode
import com.trickcal.crayon.model.CharacterDisplayMode
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.PersonalityType
import com.trickcal.crayon.ui.components.AttributeCellFilterStrip
import com.trickcal.crayon.ui.components.AvatarBadge
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.PersonalityFilterStrip
import com.trickcal.crayon.ui.components.UnlockableCellIcon
import com.trickcal.crayon.ui.components.UnlockableCellToggle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterListScreen(
    uiState: CharacterListUiState,
    onNameQueryChange: (String) -> Unit,
    onToggleTier: (BoardTier) -> Unit,
    onToggleAttribute: (AttributeType) -> Unit,
    onTogglePersonality: (PersonalityType) -> Unit,
    onClearFilters: () -> Unit,
    onModeSelected: (BrowseMode) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onOpenPaintSheet: (String) -> Unit,
    onDismissPaintSheet: () -> Unit,
    onOpenBatchPaintDialog: () -> Unit,
    onDismissBatchPaintDialog: () -> Unit,
    onToggleBatchTier: (BoardTier) -> Unit,
    onToggleBatchAttribute: (AttributeType) -> Unit,
    onSetBatchPaintAction: (BatchPaintAction) -> Unit,
    onApplyBatchPaint: () -> Unit,
    onToggleSlot: (String) -> Unit,
    onQuickPaintCharacter: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactColumns = if (maxWidth >= 420.dp) 5 else 4
        val gridColumns =
            if (uiState.displayMode == CharacterDisplayMode.DETAIL) {
                2
            } else {
                compactColumns
            }
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterSection(
                    uiState = uiState,
                    onNameQueryChange = onNameQueryChange,
                    onToggleTier = onToggleTier,
                    onToggleAttribute = onToggleAttribute,
                    onTogglePersonality = onTogglePersonality,
                    onClearFilters = onClearFilters,
                    onModeSelected = onModeSelected,
                    onToggleDisplayMode = onToggleDisplayMode,
                    onOpenBatchPaintDialog = onOpenBatchPaintDialog,
                )
            }

            if (uiState.characters.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateCard(
                        title = "没有匹配使徒",
                        message = "当前筛选条件下没有命中的使徒，试试清空筛选后再查看。",
                    )
                }
            } else {
                items(uiState.characters, key = { it.id }) { item ->
                    val canQuickPaintFromCompact =
                        uiState.displayMode == CharacterDisplayMode.COMPACT &&
                            uiState.browseMode == BrowseMode.PAINT &&
                            uiState.filter.selectedTiers.size == 1 &&
                            uiState.filter.selectedAttributes.size == 1
                    val onCharacterClick = {
                        if (canQuickPaintFromCompact) {
                            onQuickPaintCharacter(item.id)
                        } else if (uiState.browseMode == BrowseMode.DETAIL) {
                            onNavigateToDetail(item.id)
                        } else {
                            onOpenPaintSheet(item.id)
                        }
                    }
                    if (uiState.displayMode == CharacterDisplayMode.DETAIL) {
                        CharacterCard(
                            item = item,
                            browseMode = uiState.browseMode,
                            onClick = onCharacterClick,
                        )
                    } else {
                        CompactCharacterCard(
                            item = item,
                            onClick = onCharacterClick,
                        )
                    }
                }
            }
        }
    }

    val selectedCharacter = uiState.selectedCharacter
    if (selectedCharacter != null) {
        Dialog(
            onDismissRequest = onDismissPaintSheet,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 420.dp)
                ) {
                    PaintSheetContent(
                        character = selectedCharacter,
                        litSlots = uiState.litSlots,
                        onToggleSlot = onToggleSlot,
                        onDismiss = onDismissPaintSheet,
                    )
                }
            }
        }
    }

    if (uiState.batchPaintDialog.isVisible) {
        Dialog(
            onDismissRequest = onDismissBatchPaintDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 420.dp)
                        .fillMaxHeight(0.78f),
                ) {
                    BatchPaintDialogContent(
                        dialogState = uiState.batchPaintDialog,
                        onDismiss = onDismissBatchPaintDialog,
                        onToggleTier = onToggleBatchTier,
                        onToggleAttribute = onToggleBatchAttribute,
                        onSetAction = onSetBatchPaintAction,
                        onApply = onApplyBatchPaint,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    uiState: CharacterListUiState,
    onNameQueryChange: (String) -> Unit,
    onToggleTier: (BoardTier) -> Unit,
    onToggleAttribute: (AttributeType) -> Unit,
    onTogglePersonality: (PersonalityType) -> Unit,
    onClearFilters: () -> Unit,
    onModeSelected: (BrowseMode) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onOpenBatchPaintDialog: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            OutlinedTextField(
                value = uiState.filter.nameQuery,
                onValueChange = onNameQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("搜索使徒名字")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索",
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClearFilters) {
                        Text("清空筛选")
                    }
                    OutlinedButton(onClick = onOpenBatchPaintDialog) {
                        Text("一键涂色")
                    }
                    ModeToggleButton(
                        label = uiState.displayMode.displayName,
                        selected = uiState.displayMode == CharacterDisplayMode.COMPACT,
                        onClick = onToggleDisplayMode,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterSectionTitle(text = "模式切换")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BrowseMode.entries.forEach { mode ->
                        ModeToggleButton(
                            label = mode.displayName,
                            selected = uiState.browseMode == mode,
                            onClick = { onModeSelected(mode) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterSectionTitle(text = "筛选")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BoardTier.entries.forEach { tier ->
                        SelectablePillButton(
                            label = tier.displayName,
                            selected = tier in uiState.filter.selectedTiers,
                            onClick = { onToggleTier(tier) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AttributeCellFilterStrip(
                    selectedAttributes = uiState.filter.selectedAttributes,
                    onToggleAttribute = onToggleAttribute,
                )
                PersonalityFilterStrip(
                    selectedPersonality = uiState.filter.selectedPersonality,
                    onTogglePersonality = onTogglePersonality,
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ModeToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(label)
        }
    }
}

@Composable
private fun SelectablePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (selected) 4.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CharacterCard(
    item: CharacterCardUiModel,
    browseMode: BrowseMode,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarBadge(
                    name = item.name,
                    avatarKey = item.avatarKey,
                    size = 44.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "进度 ${item.litCount}/${item.totalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (browseMode == BrowseMode.DETAIL) {
                        Icons.AutoMirrored.Filled.ArrowForward
                    } else {
                        Icons.Filled.Edit
                    },
                    contentDescription = browseMode.displayName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.layerSummaries.forEach { summary ->
                    LayerSummaryBlock(summary)
                }
            }
        }
    }
}

@Composable
private fun CompactCharacterCard(
    item: CharacterCardUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        ) {
            AvatarBadge(
                name = item.name,
                avatarKey = item.avatarKey,
                modifier = Modifier.fillMaxSize(),
                fillBounds = true,
            )
            if (item.isCompactDimmed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerSummaryBlock(
    summary: LayerSummaryUiModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = summary.tier.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            summary.slots.forEach { slot ->
                UnlockableCellIcon(
                    attributeType = slot.attributeType,
                    isUnlocked = slot.isLit,
                    size = 22.dp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaintSheetContent(
    character: CharacterProfile,
    litSlots: Set<String>,
    onToggleSlot: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                name = character.name,
                avatarKey = character.avatarKey,
                size = 44.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            character.layers.forEach { layer ->
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = layer.tier.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "进度 ${layer.attributeSlots.count { it.id in litSlots }} / ${layer.attributeSlots.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            layer.attributeSlots.forEach { slot ->
                                UnlockableCellToggle(
                                    attributeType = slot.attributeType,
                                    isUnlocked = slot.id in litSlots,
                                    onClick = { onToggleSlot(slot.id) },
                                    size = 38.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchPaintDialogContent(
    dialogState: BatchPaintDialogUiState,
    onDismiss: () -> Unit,
    onToggleTier: (BoardTier) -> Unit,
    onToggleAttribute: (AttributeType) -> Unit,
    onSetAction: (BatchPaintAction) -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "一键涂色",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "操作类型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BatchPaintAction.entries.forEach { action ->
                            SelectablePillButton(
                                modifier = Modifier.weight(1f),
                                label = action.displayName,
                                selected = dialogState.selection.action == action,
                                onClick = { onSetAction(action) },
                            )
                        }
                    }
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "层数",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BoardTier.entries.forEach { tier ->
                            SelectablePillButton(
                                modifier = Modifier.weight(1f),
                                label = tier.displayName,
                                selected = tier in dialogState.selection.selectedTiers,
                                onClick = { onToggleTier(tier) },
                            )
                        }
                    }

                    Text(
                        text = "格子属性",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AttributeCellFilterStrip(
                        selectedAttributes = dialogState.selection.selectedAttributes,
                        onToggleAttribute = onToggleAttribute,
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "预览",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (
                            dialogState.selection.selectedTiers.isEmpty() ||
                            dialogState.selection.selectedAttributes.isEmpty()
                        ) {
                            "请至少选择一个层数和一个格子属性。"
                        } else if (dialogState.preview.matchedSlotCount == 0) {
                            "当前条件下没有可执行的金蜡笔格子。"
                        } else {
                            "${dialogState.selection.action.previewVerb} ${dialogState.preview.matchedCharacterCount} 个使徒的 ${dialogState.preview.matchedSlotCount} 个金蜡笔格子"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onApply,
            enabled = dialogState.canExecute,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
        ) {
            Text(dialogState.selection.action.confirmButtonLabel)
        }
    }
}
