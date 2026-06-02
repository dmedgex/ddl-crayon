package com.trickcal.crayon.data.local

import android.content.Context
import com.trickcal.crayon.model.BattlePowerCharacterCatalog
import com.trickcal.crayon.model.BattlePowerCharacterProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BattlePowerCharacterCatalogAssetLoader {
    private const val ASSET_FILE_NAME = "battle_power/character_coefficients.json"

    fun load(context: Context): BattlePowerCharacterCatalog {
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        return BattlePowerCharacterCatalogJsonParser.parse(json)
    }
}

object BattlePowerCharacterCatalogJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): BattlePowerCharacterCatalog {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val characters = root.requireArray("characters").map { element ->
            parseCharacter(element.jsonObject)
        }
        val characterUids = characters.map(BattlePowerCharacterProfile::uid)
        require(characterUids.size == characterUids.toSet().size) {
            "Battle power character catalog contains duplicate uids."
        }
        val characterNames = characters.map(BattlePowerCharacterProfile::name)
        require(characterNames.size == characterNames.toSet().size) {
            "Battle power character catalog contains duplicate character names."
        }

        val defaultCharacterUid = root.requireInt("defaultCharacterUid")
        require(characters.any { it.uid == defaultCharacterUid }) {
            "Default battle power character uid $defaultCharacterUid is missing from catalog."
        }

        return BattlePowerCharacterCatalog(
            defaultCharacterUid = defaultCharacterUid,
            characters = characters.sortedBy(BattlePowerCharacterProfile::uid),
        )
    }

    private fun parseCharacter(characterJson: JsonObject): BattlePowerCharacterProfile =
        BattlePowerCharacterProfile(
            uid = characterJson.requireInt("uid"),
            name = characterJson.requireString("name"),
            resourceName = characterJson.requireString("resourceName"),
            attackType = characterJson.requireString("attackType"),
            nameKey = characterJson.requireString("nameKey"),
            activeSkillValueA = characterJson.requireDouble("activeSkillValueA"),
            ultimateSkillValueA = characterJson.requireDouble("ultimateSkillValueA"),
            passiveValueA = characterJson.requireDouble("passiveValueA"),
            weightValueA = characterJson.requireDouble("weightValueA"),
            asideValueA = characterJson.requireDouble("asideValueA"),
        )

    private fun JsonObject.requireArray(key: String): JsonArray =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonArray

    private fun JsonObject.requireString(key: String): String =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonPrimitive.content

    private fun JsonObject.requireInt(key: String): Int =
        requireString(key).toInt()

    private fun JsonObject.requireDouble(key: String): Double =
        requireString(key).toDouble()
}
