package com.trickcal.crayon.model

enum class BoardCellType(
    val symbol: Char,
    val attributeType: AttributeType? = null,
) {
    EMPTY(symbol = '.'),
    WHITE(symbol = 'W'),
    PURPLE(symbol = 'P'),
    START(symbol = 'S'),
    END(symbol = 'E'),
    ATTACK(symbol = 'A', attributeType = AttributeType.ATTACK),
    CRIT(symbol = 'K', attributeType = AttributeType.CRIT),
    HEALTH(symbol = 'H', attributeType = AttributeType.HEALTH),
    DEFENSE(symbol = 'D', attributeType = AttributeType.DEFENSE),
    CRIT_RESIST(symbol = 'R', attributeType = AttributeType.CRIT_RESIST),
    PURPLE_ATTACK(symbol = 'a', attributeType = AttributeType.ATTACK),
    PURPLE_CRIT(symbol = 'k', attributeType = AttributeType.CRIT),
    PURPLE_HEALTH(symbol = 'h', attributeType = AttributeType.HEALTH),
    PURPLE_DEFENSE(symbol = 'd', attributeType = AttributeType.DEFENSE),
    PURPLE_CRIT_RESIST(symbol = 'r', attributeType = AttributeType.CRIT_RESIST),
    ;

    val isUnlockable: Boolean
        get() = attributeType != null && symbol.isUpperCase()

    val isFilterable: Boolean
        get() = isUnlockable

    val isCounted: Boolean
        get() = isUnlockable

    val isDisplayOnlyAttribute: Boolean
        get() = attributeType != null && symbol.isLowerCase()

    companion object {
        private val bySymbol = entries.associateBy(BoardCellType::symbol)

        fun fromSymbol(symbol: Char): BoardCellType =
            checkNotNull(bySymbol[symbol]) {
                "Unsupported board symbol: $symbol"
            }
    }
}

data class BoardLayoutBlueprint(
    val rows: List<String>,
) {
    val rowCount: Int = rows.size
    val columnCount: Int = rows.maxOfOrNull { it.length } ?: 0

    init {
        require(rows.isNotEmpty()) { "Board layout rows must not be empty." }
        require(rowCount <= 13) { "Board row count must not exceed 13." }
        require(columnCount in 1..7) { "Board column count must be between 1 and 7." }
        require(rows.all { it.length == columnCount }) {
            "Each board layout row must have the same column count."
        }
        require(rows.all { row -> row.all { symbol -> symbol in supportedSymbols } }) {
            "Board layout only supports '.', 'W', 'P', 'S', 'E', 'A', 'K', 'H', 'D', 'R', 'a', 'k', 'h', 'd', 'r'."
        }
    }

    fun unlockableCellCount(): Int =
        rows.sumOf { row ->
            row.count { symbol -> BoardCellType.fromSymbol(symbol).isUnlockable }
        }

    fun displayOnlyAttributeCount(): Int =
        rows.sumOf { row ->
            row.count { symbol -> BoardCellType.fromSymbol(symbol).isDisplayOnlyAttribute }
        }

    companion object {
        private val supportedSymbols = BoardCellType.entries.map { it.symbol }.toSet()
    }
}

data class AttributeSlotSpec(
    val id: String,
    val tier: BoardTier,
    val attributeType: AttributeType,
    val displayOrder: Int,
)

data class BoardCellSpec(
    val index: Int,
    val row: Int,
    val column: Int,
    val type: BoardCellType,
    val slotId: String? = null,
)

data class BoardLayerSpec(
    val tier: BoardTier,
    val layout: BoardLayoutBlueprint,
    val cells: List<BoardCellSpec>,
    val attributeSlots: List<AttributeSlotSpec>,
) {
    val columns: Int = layout.columnCount
    val rows: Int = layout.rowCount

    init {
        require(cells.isNotEmpty()) { "Board cells must not be empty." }
        require(attributeSlots.isNotEmpty()) { "Each board layer must contain at least one attribute slot." }
        require(layout.unlockableCellCount() == attributeSlots.size) {
            "Attribute slot count must match unlockable cell count in layout."
        }
    }

    fun slotForCell(cell: BoardCellSpec): AttributeSlotSpec? {
        val cellSlotId = cell.slotId ?: return null
        return attributeSlots.firstOrNull { it.id == cellSlotId }
    }

    fun displayOnlyAttributes(): List<AttributeType> =
        cells.mapNotNull { cell ->
            cell.type.attributeType?.takeIf { cell.type.isDisplayOnlyAttribute }
        }
}
