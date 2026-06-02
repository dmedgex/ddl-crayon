package com.trickcal.crayon.feature.petdispatch

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trickcal.crayon.model.PetDispatchAssignment
import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchResult
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.ui.components.AssetImageBitmapCache
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.MetricCard
import com.trickcal.crayon.ui.components.SectionTitle

@Composable
fun PetDispatchScreen(
    uiState: PetDispatchUiState,
    onRetryLoad: () -> Unit,
    onSelectRegion: (String) -> Unit,
    onSelectTaskCount: (Int) -> Unit,
    onToggleOwnedPet: (Int) -> Unit,
    onToggleFarmPet: (Int) -> Unit,
    onSelectTab: (PetDispatchSelectionTab) -> Unit,
    onCalculate: () -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.loadError != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EmptyStateCard(
                    title = "加载失败",
                    message = uiState.loadError,
                )
                Button(
                    onClick = onRetryLoad,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("重新加载")
                }
            }
        }

        else -> {
            val showingOwnedPets = uiState.selectedTab == PetDispatchSelectionTab.OWNED

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 118.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(
                        title = "农场宠物派遣计算器Beta",
                        subtitle = "选择地区、任务数量和宠物后，计算当前最优派遣方案。",
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    PetDispatchControlsCard(
                        regionName = uiState.selectedRegionName,
                        regionOptions = uiState.regions.map { it.name },
                        selectedTaskCount = uiState.selectedTaskCount,
                        onSelectRegion = onSelectRegion,
                        onSelectTaskCount = onSelectTaskCount,
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SectionTitle(title = "宠物选择")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (showingOwnedPets) {
                                    Button(
                                        onClick = { onSelectTab(PetDispatchSelectionTab.OWNED) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("自有宠物 ${uiState.selectedOwnedPetIds.size}")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onSelectTab(PetDispatchSelectionTab.OWNED) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("自有宠物 ${uiState.selectedOwnedPetIds.size}")
                                    }
                                }
                                if (showingOwnedPets) {
                                    OutlinedButton(
                                        onClick = { onSelectTab(PetDispatchSelectionTab.FARM) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("农场宠物 ${uiState.selectedFarmPetIds.size}")
                                    }
                                } else {
                                    Button(
                                        onClick = { onSelectTab(PetDispatchSelectionTab.FARM) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("农场宠物 ${uiState.selectedFarmPetIds.size}")
                                    }
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = onCalculate,
                        enabled = !uiState.isCalculating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.isCalculating) "计算中..." else "计算最优派遣方案")
                    }
                }

                gridItems(
                    items = uiState.pets,
                    key = { pet -> "${if (showingOwnedPets) "owned" else "farm"}-${pet.id}" },
                ) { pet ->
                    val isSelected =
                        if (showingOwnedPets) {
                            pet.id in uiState.selectedOwnedPetIds
                        } else {
                            pet.id in uiState.selectedFarmPetIds
                        }
                    PetDispatchPetCard(
                        pet = pet,
                        isSelected = isSelected,
                        onClick = {
                            if (showingOwnedPets) {
                                onToggleOwnedPet(pet.id)
                            } else {
                                onToggleFarmPet(pet.id)
                            }
                        },
                    )
                }
            }

            uiState.result?.let { result ->
                PetDispatchResultDialog(
                    result = result,
                    regionName = uiState.selectedRegionName,
                    onDismiss = onDismissResult,
                )
            }
        }
    }
}

@Composable
private fun PetDispatchControlsCard(
    regionName: String,
    regionOptions: List<String>,
    selectedTaskCount: Int,
    onSelectRegion: (String) -> Unit,
    onSelectTaskCount: (Int) -> Unit,
) {
    var regionMenuExpanded by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle(title = "派遣地区选择")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { regionMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (regionName.isBlank()) "选择派遣地区" else regionName)
                }
                DropdownMenu(
                    expanded = regionMenuExpanded,
                    onDismissRequest = { regionMenuExpanded = false },
                ) {
                    regionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                regionMenuExpanded = false
                                onSelectRegion(option)
                            },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "可派遣任务数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lazyItems((1..5).toList(), key = { count -> count }) { count ->
                        if (count == selectedTaskCount) {
                            Button(onClick = { onSelectTaskCount(count) }) {
                                Text(count.toString())
                            }
                        } else {
                            OutlinedButton(onClick = { onSelectTaskCount(count) }) {
                                Text(count.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetDispatchPetCard(
    pet: PetDispatchPet,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageBitmap = remember(context, pet.imageAssetName) {
        AssetImageBitmapCache.getOrLoad(
            context = context,
            assetPath = "pet_dispatch/images/${pet.imageAssetName}",
        )
    }
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(68.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "无图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AutoResizeSingleLineText(
                text = pet.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 1.1.em,
                    fontWeight = FontWeight.SemiBold,
                ),
                minFontSize = 7.sp,
                modifier = Modifier.fillMaxWidth(),
                color = contentColor,
                textAlign = TextAlign.Center,
            )
            AutoResizeSingleLineText(
                text = pet.rarity.displayName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 1.1.em,
                ),
                minFontSize = 8.sp,
                modifier = Modifier.fillMaxWidth(),
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AutoResizeSingleLineText(
                text = pet.skills.joinToString(separator = ", ") { skill ->
                    "${skill.name}(${skill.level.name})"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 1.1.em,
                ),
                minFontSize = 6.5.sp,
                modifier = Modifier.fillMaxWidth(),
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AutoResizeSingleLineText(
    text: String,
    style: TextStyle,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
) {
    var adjustedStyle by remember(text, style, minFontSize) { mutableStateOf(style) }
    var readyToDraw by remember(text, style, minFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = adjustedStyle,
        color = color,
        textAlign = textAlign,
        softWrap = false,
        maxLines = 1,
        onTextLayout = { layoutResult ->
            if (layoutResult.didOverflowWidth && adjustedStyle.fontSize > minFontSize) {
                val nextFontSize = (adjustedStyle.fontSize.value - 0.5f).coerceAtLeast(minFontSize.value).sp
                adjustedStyle = adjustedStyle.copy(fontSize = nextFontSize)
            } else {
                readyToDraw = true
            }
        },
    )
}

@Composable
private fun PetDispatchResultDialog(
    result: PetDispatchResult,
    regionName: String,
    onDismiss: () -> Unit,
) {
    var showTextReport by remember(result) { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(
                topBar = {
                    PetDispatchResultTopBar(
                        title = if (result.isSuccess) "派遣结果" else "计算失败",
                        showTextReport = showTextReport,
                        canToggle = result.isSuccess,
                        onToggle = { showTextReport = !showTextReport },
                        onDismiss = onDismiss,
                    )
                },
            ) { innerPadding ->
                if (!result.isSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                    ) {
                        EmptyStateCard(
                            title = "无法生成派遣方案",
                            message = result.errorMessage ?: "计算失败。",
                        )
                    }
                    return@Scaffold
                }

                if (showTextReport) {
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            Text(
                                text = result.textReport,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            SectionTitle(
                                title = regionName,
                                subtitle = buildString {
                                    append("共执行 ${result.taskCount} 个任务，")
                                    append(
                                        if (result.allSpecial) {
                                            "本次结果全部达到特阶。"
                                        } else {
                                            "可在下方查看每个任务的奖励等级。"
                                        },
                                    )
                                },
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetricCard(
                                    title = "总得分",
                                    value = result.totalScore.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                                MetricCard(
                                    title = "借用宠物",
                                    value = result.borrowedCount.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetricCard(
                                    title = "总使用宠物",
                                    value = result.totalPets.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                                MetricCard(
                                    title = "任务数量",
                                    value = result.taskCount.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        item {
                            MetricCard(
                                title = "计算耗时",
                                value = "${result.calculationTimeMs} ms",
                            )
                        }
                        lazyItems(result.assignments, key = { assignment -> assignment.task.id }) { assignment ->
                            PetDispatchAssignmentCard(assignment = assignment)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetDispatchResultTopBar(
    title: String,
    showTextReport: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "关闭",
                )
            }
        },
        actions = {
            if (canToggle) {
                TextButton(onClick = onToggle) {
                    Text(if (showTextReport) "查看卡片" else "查看文本")
                }
            }
        },
    )
}

@Composable
private fun PetDispatchAssignmentCard(
    assignment: PetDispatchAssignment,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = assignment.task.task,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "区域：${assignment.task.area}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "加成特性：${assignment.task.bonusSkills.ifEmpty { listOf("无") }.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "推荐宠物",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            assignment.team.forEach { pet ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = pet.name + if (pet.isBorrowed) "（借）" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResultMetaText(
                    title = "任务得分",
                    value = assignment.score.toString(),
                    modifier = Modifier.weight(1f),
                )
                ResultMetaText(
                    title = "预计奖励",
                    value = assignment.rewardTier.displayName,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResultMetaText(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
