package com.trickcal.crayon.data.local

import com.trickcal.crayon.model.AttributeSlotSpec
import com.trickcal.crayon.model.BoardCellSpec
import com.trickcal.crayon.model.BoardCellType
import com.trickcal.crayon.model.BoardLayerSpec
import com.trickcal.crayon.model.BoardLayoutBlueprint
import com.trickcal.crayon.model.BoardTier

object BoardLayerAssembler {
    fun assemble(
        characterId: String,
        tier: BoardTier,
        rows: List<String>,
    ): BoardLayerSpec {
        val layout = BoardLayoutBlueprint(rows = rows)
        val attributeSlots = mutableListOf<AttributeSlotSpec>()
        val cells = buildList {
            layout.rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, symbol ->
                    val type = BoardCellType.fromSymbol(symbol)
                    val slotId = if (type.isUnlockable) {
                        val attributeType = checkNotNull(type.attributeType)
                        val displayOrder = attributeSlots.size
                        val id = "${characterId}_${tier.name.lowercase()}_${attributeType.name.lowercase()}_$displayOrder"
                        attributeSlots += AttributeSlotSpec(
                            id = id,
                            tier = tier,
                            attributeType = attributeType,
                            displayOrder = displayOrder,
                        )
                        id
                    } else {
                        null
                    }
                    add(
                        BoardCellSpec(
                            index = size,
                            row = rowIndex,
                            column = columnIndex,
                            type = type,
                            slotId = slotId,
                        ),
                    )
                }
            }
        }
        return BoardLayerSpec(
            tier = tier,
            layout = layout,
            cells = cells,
            attributeSlots = attributeSlots,
        )
    }
}
