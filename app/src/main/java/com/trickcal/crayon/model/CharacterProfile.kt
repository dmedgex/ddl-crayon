package com.trickcal.crayon.model

data class CharacterProfile(
    val id: String,
    val name: String,
    val avatarKey: String,
    val personality: PersonalityType,
    val layers: List<BoardLayerSpec>,
) {
    val avatarSeed: Int
        get() = avatarKey.hashCode() and Int.MAX_VALUE

    init {
        require(layers.size == 3) { "Character must provide exactly 3 board layers." }
    }

    fun layerForTier(tier: BoardTier): BoardLayerSpec? = layers.firstOrNull { it.tier == tier }

    fun allAttributeSlots(): List<AttributeSlotSpec> = layers.flatMap { it.attributeSlots }

    fun litSlotCount(litSlots: Set<String>): Int = allAttributeSlots().count { it.id in litSlots }
}
