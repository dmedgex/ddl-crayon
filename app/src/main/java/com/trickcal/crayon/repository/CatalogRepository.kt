package com.trickcal.crayon.repository

import com.trickcal.crayon.model.CharacterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class CatalogRepository(
    private val characters: List<CharacterProfile>,
    private val customApostleRepository: CustomApostleRepository? = null,
) {
    fun observeCharacters(): Flow<List<CharacterProfile>> =
        customApostleRepository?.profiles?.let { customProfiles ->
            combine(flowOf(characters), customProfiles) { staticCharacters, customCharacters ->
                staticCharacters + customCharacters
            }
        } ?: flowOf(characters)

    fun getCharacter(characterId: String): CharacterProfile? =
        characters.firstOrNull { it.id == characterId }
            ?: customApostleRepository?.profiles?.value?.firstOrNull { it.id == characterId }

    fun getAllSlotIds(): Set<String> =
        (characters + customApostleRepository?.profiles?.value.orEmpty()).flatMapTo(linkedSetOf()) { character ->
            character.allAttributeSlots().map { slot -> slot.id }
        }
}
