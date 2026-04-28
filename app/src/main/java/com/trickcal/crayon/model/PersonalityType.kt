package com.trickcal.crayon.model

enum class PersonalityType(
    val displayName: String,
    val csvValue: String,
    val drawableName: String,
) {
    LIGHT(
        displayName = "光",
        csvValue = "光",
        drawableName = "personality_light",
    ),
    DARK(
        displayName = "暗",
        csvValue = "暗",
        drawableName = "personality_dark",
    ),
    ICE(
        displayName = "冰",
        csvValue = "冰",
        drawableName = "personality_ice",
    ),
    FIRE(
        displayName = "火",
        csvValue = "火",
        drawableName = "personality_fire",
    ),
    GRASS(
        displayName = "草",
        csvValue = "草",
        drawableName = "personality_grass",
    ),
    ;

    companion object {
        fun fromStoredValue(value: String): PersonalityType =
            entries.firstOrNull { type ->
                type.csvValue == value || type.name.equals(value, ignoreCase = true)
            } ?: error("Unsupported personality value: $value")
    }
}
