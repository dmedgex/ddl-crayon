package com.trickcal.crayon.feature.settings

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.model.ThemeMode
import com.trickcal.crayon.ui.components.AvatarBadge
import com.trickcal.crayon.ui.components.SectionTitle

private const val AUTHOR_NAME = "梦之边缘X"
private const val AUTHOR_AVATAR_KEY = "author_avatar"
private const val AUTHOR_BIO = "个人制作与持续维护中，欢迎体验并反馈建议。"
private const val AUTHOR_HOME_URL = "https://space.bilibili.com/378862730"

@Composable
fun SettingsPage(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onCheckUpdateRequested: () -> Unit,
    onOpenPetDispatch: () -> Unit,
    onOpenDamageCalculator: () -> Unit,
    onOpenBattlePowerCalculator: () -> Unit,
    buildExportFileName: () -> String,
    onExportRequested: (Context, Uri) -> Unit,
    onImportRequested: (Context, Uri) -> Unit,
    onDismissImportConfirm: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isLabDialogVisible by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.1"
        }.getOrDefault("1.0.1")
    }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                onExportRequested(context, uri)
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onImportRequested(context, uri)
            }
        }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(title = "设置")
        }

        item {
            SettingsPageSectionCard(
                title = "主题",
                description = "切换白天和夜晚显示模式。",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        if (uiState.themeMode == mode) {
                            Button(
                                onClick = { onThemeModeSelected(mode) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(mode.displayName)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onThemeModeSelected(mode) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(mode.displayName)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsPageSectionCard(
                title = "配置",
                description = "仅包含金蜡笔格子解锁进度。",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(buildExportFileName()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导出配置")
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导入配置")
                    }
                }
            }
        }

        item {
            SettingsPageSectionCard(
                title = "版本更新",
                description = "当前版本 v$versionName，可手动检查是否有新版本。",
            ) {
                Button(
                    onClick = onCheckUpdateRequested,
                    enabled = !uiState.isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isCheckingUpdate) "检查中..." else "检查更新")
                }
            }
        }

        item {
            SettingsPageSectionCard(
                title = "实验室",
                description = "体验仍在测试中的实验功能。",
            ) {
                Button(
                    onClick = { isLabDialogVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("查看实验功能")
                }
            }
        }

        item {
            AuthorCard(
                authorName = AUTHOR_NAME,
                avatarKey = AUTHOR_AVATAR_KEY,
                bio = AUTHOR_BIO,
                onClick = { uriHandler.openUri(AUTHOR_HOME_URL) },
            )
        }
    }

    val pendingImport = uiState.pendingImport
    if (pendingImport != null) {
        AlertDialog(
            onDismissRequest = onDismissImportConfirm,
            title = {
                Text("确认导入配置")
            },
            text = {
                val message =
                    if (pendingImport.isClearOperation) {
                        "将覆盖当前进度，并清空全部已解锁金蜡笔格子。"
                    } else {
                        buildString {
                            append("将覆盖当前进度，并导入 ${pendingImport.slotIds.size} 个已解锁金蜡笔格子。")
                            if (pendingImport.ignoredSlotCount > 0) {
                                append(" 已忽略 ${pendingImport.ignoredSlotCount} 个未知格子。")
                            }
                        }
                    }
                Text(message)
            },
            confirmButton = {
                Button(onClick = onConfirmImport) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissImportConfirm) {
                    Text("取消")
                }
            },
        )
    }

    if (isLabDialogVisible) {
        AlertDialog(
            onDismissRequest = { isLabDialogVisible = false },
            title = {
                Text("实验室")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "选择想体验的实验功能。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = {
                            isLabDialogVisible = false
                            onOpenPetDispatch()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("农场宠物派遣计算器Beta")
                    }
                    OutlinedButton(
                        onClick = {
                            isLabDialogVisible = false
                            onOpenDamageCalculator()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("伤害计算器Beta")
                    }
                    OutlinedButton(
                        onClick = {
                            isLabDialogVisible = false
                            onOpenBattlePowerCalculator()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("战斗力计算器Beta")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isLabDialogVisible = false }) {
                    Text("取消")
                }
            },
        )
    }

    val availableUpdate = uiState.availableUpdate
    if (availableUpdate != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdateDialog,
            title = {
                Text("发现新版本 v${availableUpdate.versionName}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前版本 v$versionName")
                    val releaseNotes = availableUpdate.releaseNotes
                    if (!releaseNotes.isNullOrBlank()) {
                        Text(releaseNotes)
                    }
                }
            },
            confirmButton = {
                if (!availableUpdate.updateUrl.isNullOrBlank()) {
                    Button(
                        onClick = {
                            uriHandler.openUri(availableUpdate.updateUrl)
                            onDismissUpdateDialog()
                        },
                    ) {
                        Text("跳转更新")
                    }
                } else {
                    Button(onClick = onDismissUpdateDialog) {
                        Text("知道了")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissUpdateDialog) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SettingsPageSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun AuthorCard(
    authorName: String,
    avatarKey: String,
    bio: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "作者",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AvatarBadge(
                    name = authorName,
                    avatarKey = avatarKey,
                    size = 72.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
