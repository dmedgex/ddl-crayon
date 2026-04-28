package com.trickcal.crayon.data.local

import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardCellType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.PersonalityType

object DemoCharacterCatalog {
    val characters: List<CharacterProfile> = listOf(
        createCharacter(
            id = "aiko",
            name = "艾可",
            avatarKey = "avatar_aiko",
            personality = PersonalityType.LIGHT,
            firstLayer = listOf(AttributeType.HEALTH, AttributeType.ATTACK),
            secondLayer = listOf(AttributeType.CRIT, AttributeType.HEALTH, AttributeType.DEFENSE),
            thirdLayer = listOf(
                AttributeType.CRIT,
                AttributeType.HEALTH,
                AttributeType.DEFENSE,
                AttributeType.CRIT_RESIST,
            ),
        ),
        createCharacter(
            id = "roki",
            name = "洛琪",
            avatarKey = "avatar_roki",
            personality = PersonalityType.DARK,
            firstLayer = listOf(AttributeType.ATTACK, AttributeType.CRIT),
            secondLayer = listOf(AttributeType.ATTACK, AttributeType.DEFENSE, AttributeType.CRIT_RESIST),
            thirdLayer = listOf(AttributeType.HEALTH, AttributeType.ATTACK, AttributeType.CRIT),
        ),
        createCharacter(
            id = "tana",
            name = "塔娜",
            avatarKey = "avatar_tana",
            personality = PersonalityType.ICE,
            firstLayer = listOf(AttributeType.DEFENSE),
            secondLayer = listOf(AttributeType.HEALTH, AttributeType.DEFENSE),
            thirdLayer = listOf(AttributeType.HEALTH, AttributeType.DEFENSE, AttributeType.CRIT_RESIST),
        ),
        createCharacter(
            id = "shir",
            name = "希尔",
            avatarKey = "avatar_shir",
            personality = PersonalityType.FIRE,
            firstLayer = listOf(AttributeType.CRIT, AttributeType.HEALTH),
            secondLayer = listOf(AttributeType.CRIT, AttributeType.ATTACK),
            thirdLayer = listOf(AttributeType.ATTACK, AttributeType.DEFENSE, AttributeType.CRIT),
        ),
        createCharacter(
            id = "kaiser",
            name = "凯瑟",
            avatarKey = "avatar_kaiser",
            personality = PersonalityType.GRASS,
            firstLayer = listOf(AttributeType.HEALTH, AttributeType.DEFENSE),
            secondLayer = listOf(AttributeType.HEALTH, AttributeType.CRIT_RESIST, AttributeType.DEFENSE),
            thirdLayer = listOf(AttributeType.ATTACK, AttributeType.HEALTH, AttributeType.CRIT_RESIST),
        ),
        createCharacter(
            id = "flora",
            name = "芙萝拉",
            avatarKey = "avatar_flora",
            personality = PersonalityType.LIGHT,
            firstLayer = listOf(AttributeType.ATTACK),
            secondLayer = listOf(AttributeType.ATTACK, AttributeType.CRIT, AttributeType.HEALTH),
            thirdLayer = listOf(AttributeType.ATTACK, AttributeType.CRIT, AttributeType.HEALTH, AttributeType.DEFENSE),
        ),
        createCharacter(
            id = "noel",
            name = "诺艾尔",
            avatarKey = "avatar_noel",
            personality = PersonalityType.DARK,
            firstLayer = listOf(AttributeType.CRIT_RESIST, AttributeType.DEFENSE),
            secondLayer = listOf(AttributeType.HEALTH, AttributeType.CRIT_RESIST),
            thirdLayer = listOf(AttributeType.DEFENSE, AttributeType.CRIT_RESIST, AttributeType.HEALTH),
        ),
        createCharacter(
            id = "vega",
            name = "维嘉",
            avatarKey = "avatar_vega",
            personality = PersonalityType.ICE,
            firstLayer = listOf(AttributeType.CRIT),
            secondLayer = listOf(AttributeType.ATTACK, AttributeType.CRIT, AttributeType.DEFENSE),
            thirdLayer = listOf(AttributeType.CRIT, AttributeType.CRIT_RESIST, AttributeType.DEFENSE),
        ),
        createCharacter(
            id = "lia",
            name = "莉亚",
            avatarKey = "avatar_lia",
            personality = PersonalityType.FIRE,
            firstLayer = listOf(AttributeType.HEALTH, AttributeType.CRIT_RESIST),
            secondLayer = listOf(AttributeType.ATTACK, AttributeType.HEALTH),
            thirdLayer = listOf(
                AttributeType.ATTACK,
                AttributeType.CRIT,
                AttributeType.HEALTH,
                AttributeType.DEFENSE,
            ),
        ),
        createCharacter(
            id = "selene",
            name = "赛琳娜",
            avatarKey = "avatar_selene",
            personality = PersonalityType.GRASS,
            firstLayer = listOf(AttributeType.ATTACK, AttributeType.DEFENSE),
            secondLayer = listOf(AttributeType.CRIT, AttributeType.HEALTH, AttributeType.CRIT_RESIST),
            thirdLayer = listOf(
                AttributeType.ATTACK,
                AttributeType.CRIT,
                AttributeType.HEALTH,
                AttributeType.CRIT_RESIST,
            ),
        ),
    )

    private fun createCharacter(
        id: String,
        name: String,
        avatarKey: String,
        personality: PersonalityType,
        firstLayer: List<AttributeType>,
        secondLayer: List<AttributeType>,
        thirdLayer: List<AttributeType>,
    ): CharacterProfile {
        val layerAttributes = listOf(firstLayer, secondLayer, thirdLayer)
        val baseSeed = avatarKey.hashCode() and Int.MAX_VALUE
        val layers = BoardTier.entries.mapIndexed { tierIndex, tier ->
            BoardLayerAssembler.assemble(
                characterId = id,
                tier = tier,
                rows = layoutRowsFor(
                    attributes = layerAttributes[tierIndex],
                    patternSeed = baseSeed + tierIndex,
                ),
            )
        }
        return CharacterProfile(
            id = id,
            name = name,
            avatarKey = avatarKey,
            personality = personality,
            layers = layers,
        )
    }

    private fun layoutRowsFor(
        attributes: List<AttributeType>,
        patternSeed: Int,
    ): List<String> =
        applyAttributes(
            templateRows = templatesFor(attributes.size).let { templates ->
                templates[patternSeed % templates.size]
            },
            attributes = attributes,
        )

    private fun templatesFor(slotCount: Int): List<List<String>> =
        when (slotCount) {
            1 -> listOf(
                listOf(
                    "..SPW..",
                    ".WPPW..",
                    "WPP*EP.",
                    ".WPPW..",
                    "..WWW..",
                ),
                listOf(
                    "..WWW..",
                    ".WPSW..",
                    "WPP*EP.",
                    ".WPPW..",
                    "..WWW..",
                ),
            )

            2 -> listOf(
                listOf(
                    "..SPW..",
                    ".W*PW..",
                    "WPP*EP.",
                    ".WPPW..",
                    "..WWW..",
                ),
                listOf(
                    "..WWW..",
                    ".WPSW..",
                    "W**PEP.",
                    ".WPPW..",
                    "..WWW..",
                ),
                listOf(
                    "..SPW..",
                    ".WPPW..",
                    "W**PEP.",
                    ".WPPW..",
                    "..WWW..",
                ),
            )

            3 -> listOf(
                listOf(
                    "..SPW..",
                    ".W*PW..",
                    "W**PEP.",
                    ".WPPW..",
                    "..WWW..",
                ),
                listOf(
                    "..WWW..",
                    ".WPSW..",
                    "W**PEP.",
                    ".W*PW..",
                    "..WWW..",
                ),
                listOf(
                    "..SPW..",
                    ".W*PW..",
                    "W**PEP.",
                    ".WPPW..",
                    "..WWW..",
                ),
            )

            4 -> listOf(
                listOf(
                    "..SPW..",
                    ".W*PW..",
                    "W**PEP.",
                    ".W*PW..",
                    "..WWW..",
                ),
                listOf(
                    "..WWW..",
                    ".WPSW..",
                    "W**PEP.",
                    ".W**W..",
                    "..WWW..",
                ),
                listOf(
                    "..SPW..",
                    ".W*PW..",
                    "W**PEP.",
                    ".W*PW..",
                    "..WWW..",
                ),
            )

            else -> error("Unsupported unlockable slot count: $slotCount")
        }

    private fun applyAttributes(
        templateRows: List<String>,
        attributes: List<AttributeType>,
    ): List<String> {
        var attributeIndex = 0
        return templateRows.map { row ->
            buildString(row.length) {
                row.forEach { symbol ->
                    if (symbol == '*') {
                        append(attributeSymbol(attributes[attributeIndex++]))
                    } else {
                        append(symbol)
                    }
                }
            }
        }
    }

    private fun attributeSymbol(attributeType: AttributeType): Char =
        when (attributeType) {
            AttributeType.ATTACK -> BoardCellType.ATTACK.symbol
            AttributeType.CRIT -> BoardCellType.CRIT.symbol
            AttributeType.HEALTH -> BoardCellType.HEALTH.symbol
            AttributeType.DEFENSE -> BoardCellType.DEFENSE.symbol
            AttributeType.CRIT_RESIST -> BoardCellType.CRIT_RESIST.symbol
        }
}
