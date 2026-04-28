package com.trickcal.crayon.repository

import com.trickcal.crayon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long?,
    val downloadUrl: String?,
    val releaseNotes: String?,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult

    data class Failure(val message: String) : UpdateCheckResult
}

class AppUpdateRepository {
    suspend fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val apiUrl = BuildConfig.UPDATE_API_URL.trim()
        if (apiUrl.isBlank()) {
            return@withContext UpdateCheckResult.Failure("未配置更新检测地址")
        }

        runCatching {
            val payload = fetchPayload(apiUrl)
            val updateInfo = parseUpdateInfo(payload)
            if (hasNewVersion(updateInfo, currentVersionCode, currentVersionName)) {
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate
            }
        }.getOrElse { error ->
            UpdateCheckResult.Failure(error.message ?: "检查更新失败")
        }
    }

    private fun fetchPayload(apiUrl: String): JSONObject {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TrickcalCrayon/${BuildConfig.VERSION_NAME}")
        }

        return connection.useConnection { http ->
            val responseCode = http.responseCode
            if (responseCode !in 200..299) {
                error("更新接口返回异常状态码：$responseCode")
            }
            val body = http.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        }
    }

    private fun parseUpdateInfo(payload: JSONObject): AppUpdateInfo {
        val versionName = payload.firstNonBlank("versionName", "version_name", "tag_name", "name")
            ?: error("更新数据缺少版本号字段")
        val versionCode = payload.firstLong("versionCode", "version_code")
        val downloadUrl = payload.firstNonBlank(
            "downloadUrl",
            "download_url",
            "pageUrl",
            "page_url",
            "html_url",
        ) ?: BuildConfig.UPDATE_PAGE_URL.takeIf { it.isNotBlank() }
        val releaseNotes = payload.firstNonBlank("releaseNotes", "release_notes", "body")

        return AppUpdateInfo(
            versionName = versionName,
            versionCode = versionCode,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
        )
    }

    private fun hasNewVersion(
        remote: AppUpdateInfo,
        currentVersionCode: Int,
        currentVersionName: String,
    ): Boolean {
        val remoteVersionCode = remote.versionCode
        if (remoteVersionCode != null) {
            return remoteVersionCode > currentVersionCode
        }
        return compareVersionNames(remote.versionName, currentVersionName) > 0
    }

    private fun compareVersionNames(remoteVersion: String, currentVersion: String): Int {
        val remoteParts = normalizeVersionName(remoteVersion).split(".", "-", "_")
        val currentParts = normalizeVersionName(currentVersion).split(".", "-", "_")
        val maxSize = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until maxSize) {
            val remotePart = remoteParts.getOrNull(index).orEmpty()
            val currentPart = currentParts.getOrNull(index).orEmpty()
            val remoteNumber = remotePart.toIntOrNull()
            val currentNumber = currentPart.toIntOrNull()
            val result = when {
                remoteNumber != null || currentNumber != null ->
                    (remoteNumber ?: 0).compareTo(currentNumber ?: 0)

                else -> remotePart.compareTo(currentPart, ignoreCase = true)
            }
            if (result != 0) {
                return result
            }
        }
        return 0
    }

    private fun normalizeVersionName(versionName: String): String =
        versionName.trim().removePrefix("v").removePrefix("V")

    private fun JSONObject.firstNonBlank(vararg keys: String): String? {
        keys.forEach { key ->
            val value = optString(key).trim()
            if (value.isNotEmpty()) {
                return value
            }
        }
        return null
    }

    private fun JSONObject.firstLong(vararg keys: String): Long? {
        keys.forEach { key ->
            if (!has(key)) {
                return@forEach
            }
            when (val value = opt(key)) {
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }
        return null
    }
}

private inline fun <T> HttpURLConnection.useConnection(block: (HttpURLConnection) -> T): T =
    try {
        block(this)
    } finally {
        disconnect()
    }
