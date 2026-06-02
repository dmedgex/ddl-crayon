package com.trickcal.crayon.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CharacterBoardAssetSnapshotTest {
    @Test
    fun assetJson_containsKyarotAndParsesSuccessfully() {
        val assetFile = File("src/main/assets/character_boards.json")
        val jsonText = assetFile.readText(Charsets.UTF_8)

        val characters = CharacterBoardJsonParser.parse(jsonText)
        val kyarot = characters.firstOrNull { it.id == "kyarot" }

        assertNotNull("Expected kyarot to be present in character_boards.json", kyarot)
        assertEquals("avatar_kyarot", kyarot?.avatarKey)
        assertEquals("卡洛特", kyarot?.name)
    }
}
