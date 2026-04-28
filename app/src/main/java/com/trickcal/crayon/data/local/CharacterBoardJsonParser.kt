package com.trickcal.crayon.data.local

import android.content.Context
import com.trickcal.crayon.model.BoardLayerSpec
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.PersonalityType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CharacterBoardAssetLoader {
    private const val ASSET_FILE_NAME = "character_boards.json"

    fun load(context: Context): List<CharacterProfile> {
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        return CharacterBoardJsonParser.parse(json)
    }
}

object CharacterBoardJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): List<CharacterProfile> {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val charactersJson = root.requireArray("characters")
        val seenIds = mutableSetOf<String>()

        val parsedCharacters = buildList<ParsedCharacter> {
            charactersJson.forEach { element ->
                val parsedCharacter = parseCharacter(element.jsonObject)
                check(seenIds.add(parsedCharacter.profile.id)) {
                    "Duplicate character id found in JSON: ${parsedCharacter.profile.id}"
                }
                add(parsedCharacter)
            }
        }

        return parsedCharacters
            .sortedWith(compareBy<ParsedCharacter> { it.sortOrder }.thenBy { it.profile.id })
            .map(ParsedCharacter::profile)
    }

    private fun parseCharacter(characterJson: JsonObject): ParsedCharacter {
        val id = characterJson.requireString("id")
        val name = characterJson.requireString("name")
        val avatarKey = characterJson.requireString("avatarKey").ifBlank { "avatar_$id" }
        val personality = PersonalityType.fromStoredValue(characterJson.requireString("personality"))
        val sortOrder = characterJson.requireInt("sortOrder")
        val layers = characterJson.requireArray("layers")
            .map { parseLayer(characterId = id, layerJson = it.jsonObject) }
            .sortedBy { it.tier.ordinal }

        require(layers.size == BoardTier.entries.size) {
            "Character $id must contain exactly ${BoardTier.entries.size} layers."
        }
        require(layers.map { it.tier }.toSet() == BoardTier.entries.toSet()) {
            "Character $id must provide tier 1, 2 and 3 exactly once."
        }

        return ParsedCharacter(
            sortOrder = sortOrder,
            profile = CharacterProfile(
                id = id,
                name = name,
                avatarKey = avatarKey,
                personality = personality,
                layers = layers,
            ),
        )
    }

    private fun parseLayer(
        characterId: String,
        layerJson: JsonObject,
    ): BoardLayerSpec {
        val tier = BoardTier.fromNumber(layerJson.requireInt("tier"))
        val rows = layerJson.requireArray("rows").map { it.jsonPrimitive.content }
        return BoardLayerAssembler.assemble(
            characterId = characterId,
            tier = tier,
            rows = rows,
        )
    }

    private fun JsonObject.requireArray(key: String): JsonArray =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonArray

    private fun JsonObject.requireString(key: String): String =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonPrimitive.content

    private fun JsonObject.requireInt(key: String): Int =
        requireString(key).toInt()

    private data class ParsedCharacter(
        val sortOrder: Int,
        val profile: CharacterProfile,
    )
}
