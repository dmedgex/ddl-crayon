package com.trickcal.crayon.data.local

import com.trickcal.crayon.model.BoardCellType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterBoardJsonParserTest {
    @Test
    fun parse_buildsUnlockableAndDisplayOnlyCellsFromRows() {
        val characters = CharacterBoardJsonParser.parse(
            jsonText =
                """
                {
                  "characters": [
                    {
                      "id": "test_hero",
                      "name": "测试角色",
                      "avatarKey": "avatar_test_hero",
                      "personality": "LIGHT",
                      "sortOrder": 1,
                      "layers": [
                        {"tier": 1, "rows": ["..SPW..", ".WaPW..", "WPAKEP."]},
                        {"tier": 2, "rows": ["..SPW..", ".WHdW..", "WPKREP."]},
                        {"tier": 3, "rows": ["..SPW..", ".WARW..", "WPhDEP."]}
                      ]
                    }
                  ]
                }
                """.trimIndent(),
        )

        val character = characters.single()
        val firstLayer = character.layerForTier(BoardTier.FIRST)!!
        assertEquals(PersonalityType.LIGHT, character.personality)
        assertEquals(2, firstLayer.attributeSlots.size)
        assertEquals(1, firstLayer.displayOnlyAttributes().size)
        assertNull(firstLayer.cells.first { it.type == BoardCellType.PURPLE_ATTACK }.slotId)
    }

    @Test(expected = IllegalStateException::class)
    fun parse_rejectsDuplicateCharacterIds() {
        CharacterBoardJsonParser.parse(
            jsonText =
                """
                {
                  "characters": [
                    {
                      "id": "dup",
                      "name": "角色一",
                      "avatarKey": "avatar_dup_1",
                      "personality": "DARK",
                      "sortOrder": 1,
                      "layers": [
                        {"tier": 1, "rows": ["..SPW..", ".WAPW..", "WPPAEP."]},
                        {"tier": 2, "rows": ["..SPW..", ".WHPW..", "WPPHEP."]},
                        {"tier": 3, "rows": ["..SPW..", ".WDPW..", "WPPDEP."]}
                      ]
                    },
                    {
                      "id": "dup",
                      "name": "角色二",
                      "avatarKey": "avatar_dup_2",
                      "personality": "LIGHT",
                      "sortOrder": 2,
                      "layers": [
                        {"tier": 1, "rows": ["..SPW..", ".WAPW..", "WPPAEP."]},
                        {"tier": 2, "rows": ["..SPW..", ".WHPW..", "WPPHEP."]},
                        {"tier": 3, "rows": ["..SPW..", ".WDPW..", "WPPDEP."]}
                      ]
                    }
                  ]
                }
                """.trimIndent(),
        )
    }
}
