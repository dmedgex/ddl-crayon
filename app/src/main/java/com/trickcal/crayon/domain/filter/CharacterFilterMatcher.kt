package com.trickcal.crayon.domain.filter

import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.FilterMatchMode

object CharacterFilterMatcher {
    fun matches(
        character: CharacterProfile,
        filter: CharacterFilter,
    ): Boolean {
        if (filter.nameQuery.isNotBlank() &&
            !character.name.contains(filter.nameQuery.trim(), ignoreCase = true)
        ) {
            return false
        }
        if (filter.selectedPersonality != null &&
            character.personality != filter.selectedPersonality
        ) {
            return false
        }
        return when (filter.matchMode) {
            FilterMatchMode.ANY_SELECTED_TIER_AND_ANY_SELECTED_ATTRIBUTE_SAME_LAYER ->
                matchesAnyTierAnyAttribute(character, filter)

            FilterMatchMode.STRICT_ALL_SELECTED_VALUES ->
                matchesStrict(character, filter)
        }
    }

    private fun matchesAnyTierAnyAttribute(
        character: CharacterProfile,
        filter: CharacterFilter,
    ): Boolean {
        val candidateLayers = if (filter.selectedTiers.isEmpty()) {
            character.layers
        } else {
            character.layers.filter { it.tier in filter.selectedTiers }
        }
        if (candidateLayers.isEmpty()) {
            return false
        }
        if (filter.selectedAttributes.isEmpty()) {
            return candidateLayers.any { it.attributeSlots.isNotEmpty() }
        }
        return candidateLayers.any { layer ->
            filter.selectedAttributes.all { attribute ->
                layer.attributeSlots.any { it.attributeType == attribute }
            }
        }
    }

    private fun matchesStrict(
        character: CharacterProfile,
        filter: CharacterFilter,
    ): Boolean {
        if (filter.selectedTiers.isEmpty() && filter.selectedAttributes.isEmpty()) {
            return true
        }

        val selectedTiersSatisfied = if (filter.selectedTiers.isEmpty()) {
            true
        } else {
            filter.selectedTiers.all { tier ->
                matchesTierAgainstAttributes(
                    character = character,
                    tier = tier,
                    selectedAttributes = filter.selectedAttributes,
                )
            }
        }

        val selectedAttributesSatisfied = if (filter.selectedAttributes.isEmpty()) {
            true
        } else {
            filter.selectedAttributes.all { attribute ->
                character.layers.any { layer ->
                    layer.attributeSlots.any { it.attributeType == attribute }
                }
            }
        }

        return selectedTiersSatisfied && selectedAttributesSatisfied
    }

    private fun matchesTierAgainstAttributes(
        character: CharacterProfile,
        tier: BoardTier,
        selectedAttributes: Set<AttributeType>,
    ): Boolean {
        val layer = character.layerForTier(tier) ?: return false
        if (selectedAttributes.isEmpty()) {
            return layer.attributeSlots.isNotEmpty()
        }
        return selectedAttributes.all { attribute ->
            layer.attributeSlots.any { it.attributeType == attribute }
        }
    }
}
