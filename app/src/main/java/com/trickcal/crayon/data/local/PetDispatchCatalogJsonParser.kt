package com.trickcal.crayon.data.local

import android.content.Context
import com.trickcal.crayon.model.PetDispatchCatalog
import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchRarity
import com.trickcal.crayon.model.PetDispatchRegion
import com.trickcal.crayon.model.PetDispatchSkill
import com.trickcal.crayon.model.PetDispatchSkillLevel
import com.trickcal.crayon.model.PetDispatchTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PetDispatchCatalogAssetLoader {
    private const val ASSET_FILE_NAME = "pet_dispatch/catalog.json"

    fun load(context: Context): PetDispatchCatalog {
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        return PetDispatchCatalogJsonParser.parse(json)
    }
}

object PetDispatchCatalogJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): PetDispatchCatalog {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val pets = root.requireArray("pets").map { element ->
            parsePet(element.jsonObject)
        }
        val petIds = pets.map(PetDispatchPet::id)
        require(petIds.size == petIds.toSet().size) { "Pet dispatch catalog contains duplicate pet ids." }
        val petNames = pets.map(PetDispatchPet::name)
        require(petNames.size == petNames.toSet().size) { "Pet dispatch catalog contains duplicate pet names." }

        val regions = root.requireArray("regions").map { element ->
            parseRegion(element.jsonObject)
        }
        val regionNames = regions.map(PetDispatchRegion::name)
        require(regionNames.size == regionNames.toSet().size) {
            "Pet dispatch catalog contains duplicate region names."
        }

        return PetDispatchCatalog(
            pets = pets.sortedBy(PetDispatchPet::id),
            regions = regions,
        )
    }

    private fun parsePet(petJson: JsonObject): PetDispatchPet =
        PetDispatchPet(
            id = petJson.requireInt("id"),
            name = petJson.requireString("name"),
            rarity = PetDispatchRarity.valueOf(petJson.requireString("rarity")),
            baseScore = petJson.requireInt("baseScore"),
            skills = petJson.requireArray("skills").map { skillElement ->
                val skillJson = skillElement.jsonObject
                PetDispatchSkill(
                    name = skillJson.requireString("name"),
                    level = PetDispatchSkillLevel.valueOf(skillJson.requireString("level")),
                )
            },
            imageAssetName = petJson.requireString("imageAssetName"),
        )

    private fun parseRegion(regionJson: JsonObject): PetDispatchRegion =
        PetDispatchRegion(
            name = regionJson.requireString("name"),
            tasks = regionJson.requireArray("tasks").map { taskElement ->
                val taskJson = taskElement.jsonObject
                PetDispatchTask(
                    id = taskJson.requireInt("id"),
                    area = taskJson.requireString("area"),
                    task = taskJson.requireString("task"),
                    bonusSkills = taskJson.requireArray("bonusSkills").map { it.jsonPrimitive.content },
                )
            },
        )

    private fun JsonObject.requireArray(key: String): JsonArray =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonArray

    private fun JsonObject.requireString(key: String): String =
        requireNotNull(this[key]) { "Missing required field: $key" }.jsonPrimitive.content

    private fun JsonObject.requireInt(key: String): Int =
        requireString(key).toInt()
}
