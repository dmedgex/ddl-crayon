package com.trickcal.crayon.feature.settings

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.model.ThemeMode
import com.trickcal.crayon.ui.components.SectionTitle
import kotlinx.coroutines.flow.Flow

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    messages: Flow<String>,
    onThemeModeSelected: (ThemeMode) -> Unit,
    buildExportFileName: () -> String,
    onExportRequested: (Context, Uri) -> Unit,
    onImportRequested: (Context, Uri) -> Unit,
    onDismissImportConfirm: () -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(modifier = modifier) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionTitle(title = "设置")
            }

            item {
                SettingsSectionCard(
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
                SettingsSectionCard(
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
                SettingsSectionCard(
                    title = "关于",
                ) {
                    Text(
                        text = "署名",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "梦之边缘X",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
}

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
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
            },
        )
    }
}
