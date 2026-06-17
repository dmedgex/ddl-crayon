package com.trickcal.crayon.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import com.trickcal.crayon.data.local.PetDispatchCatalogAssetLoader
import com.trickcal.crayon.model.PetDispatchCatalog
import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchRarity
import com.trickcal.crayon.model.PetDispatchSelectionState
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.model.PetDispatchSkill
import com.trickcal.crayon.model.PetDispatchSkillLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.io.OutputStream

class PetDispatchRepository(
    private val context: Context,
) {
    private val loadMutex = Mutex()
    @Volatile
    private var cachedCatalog: PetDispatchCatalog? = null

    suspend fun loadCatalog(): PetDispatchCatalog {
        cachedCatalog?.let { return it }
        return loadMutex.withLock {
            cachedCatalog?.let { return@withLock it }
            val loadedCatalog = withContext(Dispatchers.IO) {
                val assetCatalog = PetDispatchCatalogAssetLoader.load(context.applicationContext)
                assetCatalog.copy(pets = assetCatalog.pets + loadCustomPets())
            }
            cachedCatalog = loadedCatalog
            loadedCatalog
        }
    }

    suspend fun addCustomPet(request: CreateCustomPetRequest) {
        withContext(Dispatchers.IO) {
            val currentPets = loadCustomPets()
            val nextId = ((currentPets.maxOfOrNull { it.id } ?: CUSTOM_PET_ID_START - 1) + 1)
                .coerceAtLeast(CUSTOM_PET_ID_START)
            val imageName = "custom_pet_$nextId.png"
            val imageAssetName = request.imageUri?.let { uri ->
                saveSquarePng(uri = uri, sizePx = 256, fileName = imageName)
                CUSTOM_IMAGE_PREFIX + imageName
            } ?: "$DRAWABLE_IMAGE_PREFIX$DEFAULT_IMAGE_NAME"
            val levels = levelsForRarity(request.rarity)
            val pet = PetDispatchPet(
                id = nextId,
                name = request.name.trim(),
                rarity = request.rarity,
                baseScore = request.rarity.baseScore,
                skills = listOf(
                    PetDispatchSkill(request.firstSkill, levels.first),
                    PetDispatchSkill(request.secondSkill, levels.second),
                ),
                imageAssetName = imageAssetName,
                isCustom = true,
            )
            saveCustomPets(currentPets + pet)
            cachedCatalog = null
        }
    }

    suspend fun deleteCustomPet(petId: Int) {
        withContext(Dispatchers.IO) {
            val currentPets = loadCustomPets()
            val removedPet = currentPets.firstOrNull { it.id == petId } ?: return@withContext
            saveCustomPets(currentPets.filterNot { it.id == petId })
            removedPet.imageAssetName.removePrefix(CUSTOM_IMAGE_PREFIX)
                .takeIf(String::isNotBlank)
                ?.let { imageName -> customImageFile(imageName).delete() }
            cachedCatalog = null
        }
    }

    suspend fun exportCustomPets(outputStream: OutputStream?) {
        if (outputStream == null) {
            error("未能创建导出文件。")
        }
        withContext(Dispatchers.IO) {
            val pets = loadCustomPets()
            outputStream.use { stream ->
                stream.write(encodeCustomPetsWithImages(pets).toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    suspend fun importCustomPets(inputStream: InputStream?) {
        if (inputStream == null) {
            error("未能读取导入文件。")
        }
        withContext(Dispatchers.IO) {
            val raw = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val imported = decodeCustomPetsWithImages(raw)
            saveCustomPets(imported)
            cachedCatalog = null
        }
    }

    suspend fun exportPetConfig(
        outputStream: OutputStream?,
        selection: PetDispatchSelectionState,
    ) {
        if (outputStream == null) {
            error("未能创建导出文件。")
        }
        withContext(Dispatchers.IO) {
            val root = buildJsonObject {
                put("version", JsonPrimitive(1))
                put(
                    "selection",
                    buildJsonObject {
                        put("selectedOwnedPetIds", JsonArray(selection.selectedOwnedPetIds.sorted().map(::JsonPrimitive)))
                        put("selectedFarmPetIds", JsonArray(selection.selectedFarmPetIds.sorted().map(::JsonPrimitive)))
                        put("selectedTab", JsonPrimitive(selection.selectedTab.name))
                    },
                )
                put("customPets", encodeCustomPetsWithImagesArray(loadCustomPets()))
            }
            outputStream.use { stream ->
                stream.write(json.encodeToString(JsonObject.serializer(), root).toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    suspend fun importPetConfig(inputStream: InputStream?): PetDispatchSelectionState {
        if (inputStream == null) {
            error("未能读取导入文件。")
        }
        return withContext(Dispatchers.IO) {
            val raw = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val root = json.parseToJsonElement(raw).jsonObject
            val customPetsPayload = root["customPets"]?.jsonArray
                ?: root["pets"]?.jsonArray
                ?: JsonArray(emptyList())
            replaceCustomPetsFromPayload(customPetsPayload)
            val selectionJson = root["selection"]?.jsonObject
            PetDispatchSelectionState(
                selectedOwnedPetIds = selectionJson?.get("selectedOwnedPetIds")?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.intOrNull }
                    ?.toSet()
                    .orEmpty(),
                selectedFarmPetIds = selectionJson?.get("selectedFarmPetIds")?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.intOrNull }
                    ?.toSet()
                    .orEmpty(),
                selectedTab = PetDispatchSelectionTab.fromStorageValue(
                    selectionJson?.get("selectedTab")?.jsonPrimitive?.contentOrNull,
                ),
            )
        }
    }

    companion object {
        const val CUSTOM_IMAGE_PREFIX = "custom:"
        const val DRAWABLE_IMAGE_PREFIX = "drawable:"
        const val DEFAULT_IMAGE_NAME = "none"
        private const val CUSTOM_PET_ID_START = 10_000
        private const val CUSTOM_PET_FILE_NAME = "custom_pet_dispatch_pets.json"
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        fun levelsForRarity(rarity: PetDispatchRarity): Pair<PetDispatchSkillLevel, PetDispatchSkillLevel> =
            when (rarity) {
                PetDispatchRarity.COMMON -> PetDispatchSkillLevel.C to PetDispatchSkillLevel.C
                PetDispatchRarity.ADVANCED -> PetDispatchSkillLevel.B to PetDispatchSkillLevel.C
                PetDispatchRarity.RARE -> PetDispatchSkillLevel.A to PetDispatchSkillLevel.C
                PetDispatchRarity.LEGENDARY -> PetDispatchSkillLevel.S to PetDispatchSkillLevel.B
            }
    }

    private fun loadCustomPets(): List<PetDispatchPet> {
        val file = customPetsFile()
        if (!file.exists()) {
            return emptyList()
        }
        return decodeCustomPets(file.readText(Charsets.UTF_8))
    }

    private fun saveCustomPets(pets: List<PetDispatchPet>) {
        customPetsFile().writeText(encodeCustomPets(pets), Charsets.UTF_8)
    }

    private fun encodeCustomPets(pets: List<PetDispatchPet>): String {
        val root = buildJsonObject {
            put("version", JsonPrimitive(1))
            put("pets", JsonArray(pets.map(::encodePet)))
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    private fun decodeCustomPets(raw: String): List<PetDispatchPet> {
        val root = json.parseToJsonElement(raw).jsonObject
        return root["pets"]?.jsonArray.orEmpty().map { element ->
            decodePet(element.jsonObject)
        }
    }

    private fun encodeCustomPetsWithImages(pets: List<PetDispatchPet>): String {
        val root = buildJsonObject {
            put("version", JsonPrimitive(1))
            put("pets", encodeCustomPetsWithImagesArray(pets))
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    private fun decodeCustomPetsWithImages(raw: String): List<PetDispatchPet> {
        val root = json.parseToJsonElement(raw).jsonObject
        return decodeCustomPetsWithImagesArray(root["pets"]?.jsonArray ?: root["customPets"]?.jsonArray ?: JsonArray(emptyList()))
    }

    private fun encodeCustomPetsWithImagesArray(pets: List<PetDispatchPet>): JsonArray =
        JsonArray(
            pets.map { pet ->
                val imageName = pet.imageAssetName.removePrefix(CUSTOM_IMAGE_PREFIX)
                val imageBase64 =
                    if (pet.imageAssetName.startsWith(CUSTOM_IMAGE_PREFIX)) {
                        customImageFile(imageName)
                            .takeIf { it.exists() }
                            ?.readBytes()
                            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            .orEmpty()
                    } else {
                        ""
                    }
                buildJsonObject {
                    put("pet", encodePet(pet))
                    put("imageBase64", JsonPrimitive(imageBase64))
                }
            },
        )

    private fun replaceCustomPetsFromPayload(payload: JsonArray) {
        val imported = decodeCustomPetsWithImagesArray(payload)
        saveCustomPets(imported)
        cachedCatalog = null
    }

    private fun decodeCustomPetsWithImagesArray(payload: JsonArray): List<PetDispatchPet> =
        payload.map { element ->
            val item = element.jsonObject
            val pet = decodePet(item.getValue("pet").jsonObject)
            val imageName = pet.imageAssetName.removePrefix(CUSTOM_IMAGE_PREFIX)
            if (pet.imageAssetName.startsWith(CUSTOM_IMAGE_PREFIX)) {
                item["imageBase64"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf(String::isNotBlank)
                    ?.let { Base64.decode(it, Base64.DEFAULT) }
                    ?.let { customImageFile(imageName).writeBytes(it) }
            }
            pet.copy(isCustom = true)
        }

    private fun encodePet(pet: PetDispatchPet): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(pet.id))
            put("name", JsonPrimitive(pet.name))
            put("rarity", JsonPrimitive(pet.rarity.name))
            put("imageAssetName", JsonPrimitive(pet.imageAssetName))
            put(
                "skills",
                JsonArray(
                    pet.skills.map { skill ->
                        buildJsonObject {
                            put("name", JsonPrimitive(skill.name))
                            put("level", JsonPrimitive(skill.level.name))
                        }
                    },
                ),
            )
        }

    private fun decodePet(petJson: JsonObject): PetDispatchPet {
        val rarity = PetDispatchRarity.valueOf(petJson.getValue("rarity").jsonPrimitive.content)
        return PetDispatchPet(
            id = petJson.getValue("id").jsonPrimitive.intOrNull ?: error("自定义宠物缺少 id。"),
            name = petJson.getValue("name").jsonPrimitive.content,
            rarity = rarity,
            baseScore = rarity.baseScore,
            skills = petJson.getValue("skills").jsonArray.map { skillElement ->
                val skillJson = skillElement.jsonObject
                PetDispatchSkill(
                    name = skillJson.getValue("name").jsonPrimitive.content,
                    level = PetDispatchSkillLevel.valueOf(skillJson.getValue("level").jsonPrimitive.content),
                )
            },
            imageAssetName = petJson.getValue("imageAssetName").jsonPrimitive.content,
            isCustom = true,
        )
    }

    private fun saveSquarePng(uri: Uri, sizePx: Int, fileName: String) {
        val source = context.contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream) ?: error("无法读取宠物图片。")
        }
        val square = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(square)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val scale = maxOf(sizePx.toFloat() / source.width, sizePx.toFloat() / source.height)
        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (sizePx - scaledWidth) / 2f
        val top = (sizePx - scaledHeight) / 2f
        canvas.drawBitmap(source, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), paint)
        customImageFile(fileName).outputStream().use { output ->
            square.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun customPetsFile() =
        java.io.File(context.filesDir, CUSTOM_PET_FILE_NAME)

    private fun customImageFile(fileName: String): java.io.File {
        val dir = java.io.File(context.filesDir, "pet_dispatch_custom_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return java.io.File(dir, fileName)
    }
}

data class CreateCustomPetRequest(
    val name: String,
    val rarity: PetDispatchRarity,
    val firstSkill: String,
    val secondSkill: String,
    val imageUri: Uri?,
)
