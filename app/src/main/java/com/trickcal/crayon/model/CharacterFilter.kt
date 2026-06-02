package com.trickcal.crayon.model

enum class BrowseMode(
    val displayName: String,
) {
    DETAIL(displayName = "查看详情"),
    PAINT(displayName = "蜡笔涂色"),
}

enum class CharacterDisplayMode(
    val displayName: String,
) {
    DETAIL(displayName = "\u8be6\u60c5"),
    COMPACT(displayName = "\u6781\u7b80"),
}

enum class FilterMatchMode {
    ANY_SELECTED_TIER_AND_ANY_SELECTED_ATTRIBUTE_SAME_LAYER,
    STRICT_ALL_SELECTED_VALUES,
}

data class CharacterFilter(
    val nameQuery: String = "",
    val selectedTiers: Set<BoardTier> = emptySet(),
    val selectedAttributes: Set<AttributeType> = emptySet(),
    val selectedPersonality: PersonalityType? = null,
    val matchMode: FilterMatchMode =
        FilterMatchMode.ANY_SELECTED_TIER_AND_ANY_SELECTED_ATTRIBUTE_SAME_LAYER,
)

data class CharacterListPreferences(
    val filter: CharacterFilter = CharacterFilter(),
    val browseMode: BrowseMode = BrowseMode.DETAIL,
    val displayMode: CharacterDisplayMode = CharacterDisplayMode.DETAIL,
)
