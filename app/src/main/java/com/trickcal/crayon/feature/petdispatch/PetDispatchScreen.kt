package com.trickcal.crayon.feature.petdispatch

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
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
import com.trickcal.crayon.model.PetDispatchRarity
import com.trickcal.crayon.model.PetDispatchRegion
import com.trickcal.crayon.model.PetDispatchResult
import com.trickcal.crayon.model.PetDispatchTask
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.repository.CreateCustomPetRequest
import com.trickcal.crayon.repository.PetDispatchRepository
import com.trickcal.crayon.ui.components.AssetImageBitmapCache
import com.trickcal.crayon.ui.components.DrawableImageBitmapCache
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.FileImageBitmapCache
import com.trickcal.crayon.ui.components.ImageUriPreview
import com.trickcal.crayon.ui.components.MetricCard
import com.trickcal.crayon.ui.components.SectionTitle
import com.trickcal.crayon.ui.components.resolveDrawableResId
import java.io.InputStream
import java.io.OutputStream

@Composable
fun PetDispatchScreen(
    uiState: PetDispatchUiState,
    onRetryLoad: () -> Unit,
    onOpenPetConfig: () -> Unit,
    onOpenRegionConfig: () -> Unit,
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
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SectionTitle(
                        title = "选择地区、任务数量和宠物后，计算当前最优派遣方案。",
                    )
                }
                item {
                    PetDispatchSummaryCard(
                        title = "宠物配置",
                        message = "自有宠物 ${uiState.selectedOwnedPetIds.size} 只，农场宠物 ${uiState.selectedFarmPetIds.size} 只",
                        onClick = onOpenPetConfig,
                    )
                }
                item {
                    PetDispatchSummaryCard(
                        title = "派遣地区配置",
                        message = if (uiState.selectedRegionName.isBlank()) {
                            "尚未选择派遣地区"
                        } else {
                            "${uiState.selectedRegionName}，任务数 ${uiState.selectedTaskCount}"
                        },
                        onClick = onOpenRegionConfig,
                    )
                }
                item {
                    Button(
                        onClick = onCalculate,
                        enabled = !uiState.isCalculating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.isCalculating) "计算中..." else "计算最优派遣方案")
                    }
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
fun PetDispatchPetConfigScreen(
    uiState: PetDispatchUiState,
    onToggleOwnedPet: (Int) -> Unit,
    onToggleFarmPet: (Int) -> Unit,
    onSelectTab: (PetDispatchSelectionTab) -> Unit,
    onAddCustomPet: (CreateCustomPetRequest) -> Unit,
    onExportPetConfig: (OutputStream?) -> Unit,
    onImportPetConfig: (InputStream?) -> Unit,
    onOpenCustomPetEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                onExportPetConfig(context.contentResolver.openOutputStream(uri))
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onImportPetConfig(context.contentResolver.openInputStream(uri))
            }
        }
    var showCreatePetDialog by remember { mutableStateOf(false) }
    val showingOwnedPets = uiState.selectedTab == PetDispatchSelectionTab.OWNED

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 118.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Text("导入宠物配置")
                        }
                        OutlinedButton(
                            onClick = { exportLauncher.launch("trickcal-pet-dispatch-config.json") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Text("导出宠物配置")
                        }
                    }
                    if (uiState.customPetEnabled) {
                        Button(
                            onClick = onOpenCustomPetEditor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("编辑自定义新增宠物")
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(title = "宠物配置")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ToggleButton(
                            label = "自有宠物 ${uiState.selectedOwnedPetIds.size}",
                            selected = showingOwnedPets,
                            onClick = { onSelectTab(PetDispatchSelectionTab.OWNED) },
                            modifier = Modifier.weight(1f),
                        )
                        ToggleButton(
                            label = "农场宠物 ${uiState.selectedFarmPetIds.size}",
                            selected = !showingOwnedPets,
                            onClick = { onSelectTab(PetDispatchSelectionTab.FARM) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
                onDelete = null,
                onClick = {
                    if (showingOwnedPets) {
                        onToggleOwnedPet(pet.id)
                    } else {
                        onToggleFarmPet(pet.id)
                    }
                },
            )
        }

        if (uiState.customPetEnabled) {
            item(key = "custom-pet-add") {
                AddCustomPetCard(onClick = { showCreatePetDialog = true })
            }
        }
    }

    if (showCreatePetDialog) {
        CreateCustomPetDialog(
            onDismiss = { showCreatePetDialog = false },
            onConfirm = { request ->
                showCreatePetDialog = false
                onAddCustomPet(request)
            },
        )
    }
}

@Composable
fun PetDispatchRegionConfigScreen(
    uiState: PetDispatchUiState,
    onSelectRegion: (String) -> Unit,
    onSelectTaskCount: (Int) -> Unit,
    onTaskBonusSkillChange: (Int, Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PetDispatchControlsCard(
                regionName = uiState.selectedRegionName,
                regionOptions = uiState.regions.map { it.name },
                selectedTaskCount = uiState.selectedTaskCount,
                selectedRegion = uiState.selectedRegion,
                onSelectRegion = onSelectRegion,
                onSelectTaskCount = onSelectTaskCount,
                onTaskBonusSkillChange = onTaskBonusSkillChange,
            )
        }
    }
}

@Composable
fun CustomPetEditorScreen(
    uiState: PetDispatchUiState,
    onDeleteCustomPet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPetId by remember { mutableStateOf<Int?>(null) }
    val customPets = uiState.pets.filter { it.isCustom }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 118.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(
                        title = "编辑自定义新增宠物",
                        subtitle = "点击选择自定义宠物后，可以按删除键删除。",
                    )
                    Button(
                        onClick = {
                            selectedPetId?.let(onDeleteCustomPet)
                            selectedPetId = null
                        },
                        enabled = selectedPetId != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("删除")
                    }
                }
            }
        }
        if (customPets.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyStateCard(
                    title = "没有自定义宠物",
                    message = "返回宠物配置页面后点击加号新增宠物。",
                )
            }
        } else {
            gridItems(customPets, key = { it.id }) { pet ->
                PetDispatchPetCard(
                    pet = pet,
                    isSelected = selectedPetId == pet.id,
                    onDelete = null,
                    onClick = { selectedPetId = pet.id },
                )
            }
        }
    }
}

@Composable
private fun PetDispatchSummaryCard(
    title: String,
    message: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionTitle(title = title)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PetDispatchControlsCard(
    regionName: String,
    regionOptions: List<String>,
    selectedTaskCount: Int,
    selectedRegion: PetDispatchRegion?,
    onSelectRegion: (String) -> Unit,
    onSelectTaskCount: (Int) -> Unit,
    onTaskBonusSkillChange: (Int, Int, String) -> Unit,
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
            SectionTitle(title = "任务配置")
            selectedRegion?.tasks.orEmpty().forEach { task ->
                PetDispatchTaskConfigCard(
                    task = task,
                    onTaskBonusSkillChange = onTaskBonusSkillChange,
                )
            }
        }
    }
}

@Composable
private fun PetDispatchTaskConfigCard(
    task: PetDispatchTask,
    onTaskBonusSkillChange: (Int, Int, String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(task.task, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("任务区域：${task.area}", style = MaterialTheme.typography.bodyMedium)
            Text("加成特性", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) { index ->
                    SkillDropdownButton(
                        selectedSkill = task.bonusSkills.getOrNull(index) ?: "无",
                        onSelect = { skill -> onTaskBonusSkillChange(task.id, index, skill) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillDropdownButton(
    selectedSkill: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedLabel: String = selectedSkill.ifBlank { "无" },
    options: List<String> = PetSkillOptions,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selectedLabel)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { skill ->
                DropdownMenuItem(
                    text = { Text(skill) },
                    onClick = {
                        expanded = false
                        onSelect(skill)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@Composable
private fun AddCustomPetCard(
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "新增宠物",
                modifier = Modifier.fillMaxSize(0.35f),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CreateCustomPetDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreateCustomPetRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var rarity by remember { mutableStateOf(PetDispatchRarity.COMMON) }
    var firstSkill by remember { mutableStateOf("迟钝") }
    var secondSkill by remember { mutableStateOf("活泼") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            imageUri = uri
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增宠物") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImageUriPreview(
                        imageUri = imageUri,
                        size = 104.dp,
                    )
                    OutlinedButton(onClick = { imageLauncher.launch(arrayOf("image/*")) }) {
                        Text(if (imageUri == null) "上传宠物图片" else "更换宠物图片")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("宠物名字") },
                )
                RarityButtonGroup(
                    selected = rarity,
                    onSelect = { rarity = it },
                )
                val levels = PetDispatchRepository.levelsForRarity(rarity)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "特性：",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkillDropdownButton(
                            selectedSkill = firstSkill,
                            selectedLabel = "$firstSkill${levels.first.name}",
                            onSelect = { firstSkill = it },
                            options = PetTraitOptions,
                            modifier = Modifier.weight(1f),
                        )
                        SkillDropdownButton(
                            selectedSkill = secondSkill,
                            selectedLabel = "$secondSkill${levels.second.name}",
                            onSelect = { secondSkill = it },
                            options = PetTraitOptions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        CreateCustomPetRequest(
                            name = name,
                            rarity = rarity,
                            firstSkill = firstSkill,
                            secondSkill = secondSkill,
                            imageUri = imageUri,
                        ),
                    )
                },
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun RarityButtonGroup(
    selected: PetDispatchRarity,
    onSelect: (PetDispatchRarity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "稀有度：",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PetDispatchRarity.entries.forEach { rarity ->
                if (rarity == selected) {
                    Button(
                        onClick = { onSelect(rarity) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(rarity.displayName.removeSuffix("宠物"))
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(rarity) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(rarity.displayName.removeSuffix("宠物"))
                    }
                }
            }
        }
    }
}

private val PetSkillOptions = listOf("无", "迟钝", "活泼", "亲密", "敏锐", "体贴", "自信")
private val PetTraitOptions = listOf("迟钝", "活泼", "亲密", "敏锐", "体贴", "自信")

@Composable
private fun PetDispatchPetCard(
    pet: PetDispatchPet,
    isSelected: Boolean,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageBitmap = remember(context, pet.imageAssetName) {
        if (pet.imageAssetName.startsWith(PetDispatchRepository.CUSTOM_IMAGE_PREFIX)) {
            FileImageBitmapCache.getOrLoad(
                context = context,
                relativePath = "pet_dispatch_custom_images/${pet.imageAssetName.removePrefix(PetDispatchRepository.CUSTOM_IMAGE_PREFIX)}",
            )
        } else if (pet.imageAssetName.startsWith(PetDispatchRepository.DRAWABLE_IMAGE_PREFIX)) {
            DrawableImageBitmapCache.getOrLoad(
                context = context,
                drawableResId = context.resolveDrawableResId(
                    pet.imageAssetName.removePrefix(PetDispatchRepository.DRAWABLE_IMAGE_PREFIX),
                ),
            )
        } else {
            AssetImageBitmapCache.getOrLoad(
                context = context,
                assetPath = "pet_dispatch/images/${pet.imageAssetName}",
            )
        }
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
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            }
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
