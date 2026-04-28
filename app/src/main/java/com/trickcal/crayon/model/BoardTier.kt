package com.trickcal.crayon.model

enum class BoardTier(
    val displayName: String,
    val bonusPercentPerSlot: Int,
    val goldCrayonCostPerSlot: Int,
) {
    FIRST(displayName = "第1层", bonusPercentPerSlot = 3, goldCrayonCostPerSlot = 2),
    SECOND(displayName = "第2层", bonusPercentPerSlot = 4, goldCrayonCostPerSlot = 4),
    THIRD(displayName = "第3层", bonusPercentPerSlot = 5, goldCrayonCostPerSlot = 6),
    ;

    companion object {
        fun fromNumber(value: Int): BoardTier =
            when (value) {
                1 -> FIRST
                2 -> SECOND
                3 -> THIRD
                else -> error("Unsupported board tier value: $value")
            }
    }
}
