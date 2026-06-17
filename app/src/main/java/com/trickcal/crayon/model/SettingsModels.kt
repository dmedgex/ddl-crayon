package com.trickcal.crayon.model

import kotlinx.serialization.json.JsonArray

data class ProgressConfig(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val litSlotIds: List<String>,
    val customApostles: JsonArray = JsonArray(emptyList()),
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

data class PreparedProgressImport(
    val sourceSlotCount: Int,
    val ignoredSlotCount: Int,
    val slotIds: Set<String>,
    val customApostles: JsonArray? = null,
)

enum class ThemeMode(
    val displayName: String,
) {
    LIGHT(displayName = "白天"),
    DARK(displayName = "夜晚"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: LIGHT
    }
}
