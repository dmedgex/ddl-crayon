package com.trickcal.crayon.feature.list

import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile

enum class BatchPaintAction(
    val displayName: String,
    val confirmButtonLabel: String,
    val previewVerb: String,
) {
    UNLOCK(
        displayName = "\u6279\u91cf\u89e3\u9501",
        confirmButtonLabel = "\u786e\u8ba4\u6279\u91cf\u89e3\u9501",
        previewVerb = "\u5c06\u89e3\u9501",
    ),
    LOCK(
        displayName = "\u6279\u91cf\u53d6\u6d88",
        confirmButtonLabel = "\u786e\u8ba4\u6279\u91cf\u53d6\u6d88",
        previewVerb = "\u5c06\u53d6\u6d88",
    ),
}

data class BatchPaintSelection(
    val selectedTiers: Set<BoardTier> = emptySet(),
    val selectedAttributes: Set<AttributeType> = emptySet(),
    val action: BatchPaintAction = BatchPaintAction.UNLOCK,
)

data class BatchPaintPreview(
    val matchedCharacterCount: Int = 0,
    val matchedSlotCount: Int = 0,
    val slotIds: Set<String> = emptySet(),
)

data class BatchPaintDialogUiState(
    val isVisible: Boolean = false,
    val selection: BatchPaintSelection = BatchPaintSelection(),
    val preview: BatchPaintPreview = BatchPaintPreview(),
) {
    val canExecute: Boolean
        get() = selection.selectedTiers.isNotEmpty() &&
            selection.selectedAttributes.isNotEmpty() &&
            preview.slotIds.isNotEmpty()
}

object BatchPaintPlanner {
    fun buildPreview(
        characters: List<CharacterProfile>,
        litSlots: Set<String>,
        selection: BatchPaintSelection,
    ): BatchPaintPreview {
        if (selection.selectedTiers.isEmpty() || selection.selectedAttributes.isEmpty()) {
            return BatchPaintPreview()
        }

        val slotIdsByCharacter = characters.mapNotNull { character ->
            val effectiveSlotIds = character.layers
                .filter { it.tier in selection.selectedTiers }
                .flatMap { layer ->
                    layer.attributeSlots.filter { slot ->
                        slot.attributeType in selection.selectedAttributes
                    }
                }
                .map { it.id }
                .filterTo(linkedSetOf()) { slotId ->
                    when (selection.action) {
                        BatchPaintAction.UNLOCK -> slotId !in litSlots
                        BatchPaintAction.LOCK -> slotId in litSlots
                    }
                }

            effectiveSlotIds.takeIf { it.isNotEmpty() }?.let { character.id to it }
        }

        val slotIds = slotIdsByCharacter
            .flatMapTo(linkedSetOf()) { (_, ids) -> ids }

        return BatchPaintPreview(
            matchedCharacterCount = slotIdsByCharacter.size,
            matchedSlotCount = slotIds.size,
            slotIds = slotIds,
        )
    }
}
