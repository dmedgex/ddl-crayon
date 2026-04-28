package com.trickcal.crayon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trickcal.crayon.BuildConfig
import com.trickcal.crayon.domain.settings.ProgressConfigCodec
import com.trickcal.crayon.model.PreparedProgressImport
import com.trickcal.crayon.model.ThemeMode
import com.trickcal.crayon.repository.AppUpdateRepository
import com.trickcal.crayon.repository.CrayonRepository
import com.trickcal.crayon.repository.SettingsRepository
import com.trickcal.crayon.repository.UpdateCheckResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class PendingImportUiState(
    val slotIds: Set<String>,
    val sourceSlotCount: Int,
    val ignoredSlotCount: Int,
) {
    val isClearOperation: Boolean
        get() = sourceSlotCount == 0
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val pendingImport: PendingImportUiState? = null,
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AvailableUpdateUiState? = null,
)

data class AvailableUpdateUiState(
    val versionName: String,
    val downloadUrl: String?,
    val releaseNotes: String?,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val crayonRepository: CrayonRepository,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {
    private val pendingImportState = MutableStateFlow<PendingImportUiState?>(null)
    private val isCheckingUpdateState = MutableStateFlow(false)
    private val availableUpdateState = MutableStateFlow<AvailableUpdateUiState?>(null)
    private val _messages = MutableSharedFlow<String>()

    val messages = _messages.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.observeThemeMode(),
            pendingImportState,
            isCheckingUpdateState,
            availableUpdateState,
        ) { themeMode, pendingImport, isCheckingUpdate, availableUpdate ->
            SettingsUiState(
                themeMode = themeMode,
                pendingImport = pendingImport,
                isCheckingUpdate = isCheckingUpdate,
                availableUpdate = availableUpdate,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    fun buildExportFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return "trickcal-crayon-progress-$timestamp.json"
    }

    fun exportToStream(outputStream: OutputStream?) {
        if (outputStream == null) {
            emitMessage("未能创建导出文件。")
            return
        }
        viewModelScope.launch {
            runCatching {
                val config = crayonRepository.exportProgress()
                val content = ProgressConfigCodec.encode(config)
                outputStream.use { stream ->
                    stream.write(content.toByteArray(StandardCharsets.UTF_8))
                    stream.flush()
                }
            }.onSuccess {
                emitMessage("配置导出成功。")
            }.onFailure {
                emitMessage("配置导出失败。")
            }
        }
    }

    fun prepareImport(inputStream: InputStream?) {
        if (inputStream == null) {
            emitMessage("未能读取导入文件。")
            return
        }
        viewModelScope.launch {
            runCatching {
                val raw = inputStream.use { stream ->
                    stream.readBytes().toString(StandardCharsets.UTF_8)
                }
                val prepared = ProgressConfigCodec.decode(
                    raw = raw,
                    validSlotIds = crayonRepository.getValidSlotIds(),
                )
                handlePreparedImport(prepared)
            }.onFailure { error ->
                emitMessage(error.message ?: "导入配置失败。")
            }
        }
    }

    fun dismissImportConfirm() {
        pendingImportState.value = null
    }

    fun checkForUpdate() {
        if (isCheckingUpdateState.value) {
            return
        }
        viewModelScope.launch {
            isCheckingUpdateState.value = true
            when (val result = appUpdateRepository.checkForUpdate(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdateState.value = AvailableUpdateUiState(
                        versionName = result.updateInfo.versionName,
                        downloadUrl = result.updateInfo.downloadUrl,
                        releaseNotes = result.updateInfo.releaseNotes,
                    )
                }

                UpdateCheckResult.UpToDate -> {
                    emitMessage("当前已经是最新版本")
                }

                is UpdateCheckResult.Failure -> {
                    emitMessage(result.message)
                }
            }
            isCheckingUpdateState.value = false
        }
    }

    fun dismissUpdateDialog() {
        availableUpdateState.value = null
    }

    fun confirmImport() {
        val pendingImport = pendingImportState.value ?: return
        viewModelScope.launch {
            crayonRepository.replaceProgress(pendingImport.slotIds)
            pendingImportState.value = null
            emitMessage(
                if (pendingImport.isClearOperation) {
                    "已清空当前解锁进度。"
                } else {
                    "已导入 ${pendingImport.slotIds.size} 个已解锁金蜡笔格子。"
                },
            )
        }
    }

    private fun handlePreparedImport(prepared: PreparedProgressImport) {
        when {
            prepared.sourceSlotCount == 0 -> {
                pendingImportState.value = PendingImportUiState(
                    slotIds = emptySet(),
                    sourceSlotCount = 0,
                    ignoredSlotCount = 0,
                )
            }

            prepared.slotIds.isEmpty() -> {
                emitMessage("导入文件中没有可用的解锁进度。")
            }

            else -> {
                pendingImportState.value = PendingImportUiState(
                    slotIds = prepared.slotIds,
                    sourceSlotCount = prepared.sourceSlotCount,
                    ignoredSlotCount = prepared.ignoredSlotCount,
                )
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
