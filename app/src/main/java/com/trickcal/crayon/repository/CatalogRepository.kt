package com.trickcal.crayon.repository

import com.trickcal.crayon.model.CharacterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CatalogRepository(
    private val characters: List<CharacterProfile>,
) {
    fun observeCharacters(): Flow<List<CharacterProfile>> = flowOf(characters)

    fun getCharacter(characterId: String): CharacterProfile? =
        characters.firstOrNull { it.id == characterId }

    fun getAllSlotIds(): Set<String> =
        characters.flatMapTo(linkedSetOf()) { character ->
            character.allAttributeSlots().map { slot -> slot.id }
        }
}
