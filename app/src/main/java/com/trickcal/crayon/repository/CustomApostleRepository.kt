package com.trickcal.crayon.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import com.trickcal.crayon.data.local.BoardLayerAssembler
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.PersonalityType
import com.trickcal.crayon.ui.components.CUSTOM_AVATAR_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.io.OutputStream

class CustomApostleRepository(
    private val context: Context,
) {
    private val mutableProfiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<CharacterProfile>> = mutableProfiles

    suspend fun addApostle(request: CreateCustomApostleRequest) {
        withContext(Dispatchers.IO) {
            val currentRecords = loadRecords()
            val id = "custom_${System.currentTimeMillis()}"
            val avatarFileName = "$id.png"
            val avatarKey = request.imageUri?.let {
                saveSquarePng(uri = it, sizePx = 210, fileName = avatarFileName)
                CUSTOM_AVATAR_PREFIX + avatarFileName
            } ?: DEFAULT_AVATAR_KEY
            val record = CustomApostleRecord(
                id = id,
                name = request.name.trim(),
                avatarFileName = avatarKey,
                personality = request.personality.name,
                race = request.race.name,
                crayonChoice = request.crayonChoice.name,
            )
            saveRecords(currentRecords + record)
            mutableProfiles.value = loadProfiles()
        }
    }

    suspend fun deleteApostle(characterId: String): Set<String> =
        withContext(Dispatchers.IO) {
            val currentRecords = loadRecords()
            val removed = currentRecords.firstOrNull { it.id == characterId } ?: return@withContext emptySet()
            val removedProfile = removed.toProfile()
            saveRecords(currentRecords.filterNot { it.id == characterId })
            removed.avatarFileName
                .takeIf { it.startsWith(CUSTOM_AVATAR_PREFIX) }
                ?.removePrefix(CUSTOM_AVATAR_PREFIX)
                ?.let { imageFile(it).delete() }
            mutableProfiles.value = loadProfiles()
            removedProfile.allAttributeSlots().mapTo(linkedSetOf()) { it.id }
        }

    fun exportPayload(): JsonArray =
        JsonArray(
            loadRecords().map { record ->
                val imageBase64 =
                    if (record.avatarFileName.startsWith(CUSTOM_AVATAR_PREFIX)) {
                        imageFile(record.avatarFileName.removePrefix(CUSTOM_AVATAR_PREFIX))
                            .takeIf { it.exists() }
                            ?.readBytes()
                            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            .orEmpty()
                    } else {
                        ""
                    }
                buildJsonObject {
                    put("record", record.toJson())
                    put("imageBase64", JsonPrimitive(imageBase64))
                }
            },
        )

    suspend fun replaceFromPayload(payload: JsonArray) {
        withContext(Dispatchers.IO) {
            val records = payload.map { element ->
                val item = element.jsonObject
                val record = CustomApostleRecord.fromJson(item.getValue("record").jsonObject)
                item["imageBase64"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf(String::isNotBlank)
                    ?.let { Base64.decode(it, Base64.DEFAULT) }
                    ?.takeIf { record.avatarFileName.startsWith(CUSTOM_AVATAR_PREFIX) }
                    ?.let { imageFile(record.avatarFileName.removePrefix(CUSTOM_AVATAR_PREFIX)).writeBytes(it) }
                record
            }
            saveRecords(records)
            mutableProfiles.value = loadProfiles()
        }
    }

    private fun loadProfiles(): List<CharacterProfile> =
        loadRecords().map { it.toProfile() }

    private fun loadRecords(): List<CustomApostleRecord> {
        val file = recordsFile()
        if (!file.exists()) {
            return emptyList()
        }
        return json.parseToJsonElement(file.readText(Charsets.UTF_8))
            .jsonObject["apostles"]
            ?.jsonArray
            .orEmpty()
            .map { CustomApostleRecord.fromJson(it.jsonObject) }
    }

    private fun saveRecords(records: List<CustomApostleRecord>) {
        val root = buildJsonObject {
            put("version", JsonPrimitive(1))
            put("apostles", JsonArray(records.map { it.toJson() }))
        }
        recordsFile().writeText(json.encodeToString(JsonObject.serializer(), root), Charsets.UTF_8)
    }

    private fun CustomApostleRecord.toProfile(): CharacterProfile {
        val race = CustomApostleRace.valueOf(race)
        return CharacterProfile(
            id = id,
            name = name,
            avatarKey = avatarFileName,
            personality = PersonalityType.valueOf(personality),
            layers = race.rows.mapIndexed { index, rows ->
                val crayonChoice = CustomApostleCrayonChoice.valueOf(crayonChoice)
                BoardLayerAssembler.assemble(
                    characterId = id,
                    tier = BoardTier.entries[index],
                    rows = rows.rewriteUnlockableAttributes(crayonChoice.layerAttributes(index)),
                )
            },
            isCustom = true,
        )
    }

    private fun saveSquarePng(uri: Uri, sizePx: Int, fileName: String) {
        val source = context.contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream) ?: error("无法读取使徒图片。")
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
        imageFile(fileName).outputStream().use { output ->
            square.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun recordsFile() = java.io.File(context.filesDir, "custom_apostles.json")

    private fun imageFile(fileName: String): java.io.File {
        val dir = java.io.File(context.filesDir, "custom_apostle_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return java.io.File(dir, fileName)
    }

    companion object {
        private const val DEFAULT_AVATAR_KEY = "none"
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
}

data class CreateCustomApostleRequest(
    val name: String,
    val personality: PersonalityType,
    val race: CustomApostleRace,
    val crayonChoice: CustomApostleCrayonChoice,
    val imageUri: Uri?,
)

data class CustomApostleRecord(
    val id: String,
    val name: String,
    val avatarFileName: String,
    val personality: String,
    val race: String,
    val crayonChoice: String = CustomApostleCrayonChoice.ATTACK_DEFENSE.name,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(id))
            put("name", JsonPrimitive(name))
            put("avatarFileName", JsonPrimitive(avatarFileName))
            put("personality", JsonPrimitive(personality))
            put("race", JsonPrimitive(race))
            put("crayonChoice", JsonPrimitive(crayonChoice))
        }

    companion object {
        fun fromJson(json: JsonObject): CustomApostleRecord =
            CustomApostleRecord(
                id = json.getValue("id").jsonPrimitive.content,
                name = json.getValue("name").jsonPrimitive.content,
                avatarFileName = json.getValue("avatarFileName").jsonPrimitive.content,
                personality = json.getValue("personality").jsonPrimitive.content,
                race = json.getValue("race").jsonPrimitive.content,
                crayonChoice = json["crayonChoice"]?.jsonPrimitive?.content ?: CustomApostleCrayonChoice.ATTACK_DEFENSE.name,
            )
    }
}

enum class CustomApostleCrayonChoice(
    val displayName: String,
    val firstLayer: List<AttributeType>,
    val secondLayer: List<AttributeType>,
    val thirdLayer: List<AttributeType>,
) {
    ATTACK_DEFENSE(
        "攻击，防御",
        listOf(AttributeType.ATTACK, AttributeType.DEFENSE),
        listOf(AttributeType.ATTACK, AttributeType.DEFENSE, AttributeType.HEALTH),
        listOf(AttributeType.ATTACK, AttributeType.HEALTH, AttributeType.CRIT, AttributeType.CRIT_RESIST),
    ),
    ATTACK_HEALTH(
        "攻击，生命",
        listOf(AttributeType.ATTACK, AttributeType.HEALTH),
        listOf(AttributeType.ATTACK, AttributeType.DEFENSE, AttributeType.CRIT_RESIST),
        listOf(AttributeType.ATTACK, AttributeType.DEFENSE, AttributeType.CRIT, AttributeType.CRIT_RESIST),
    ),
    DEFENSE_CRIT_RESIST(
        "防御，暴击抗性",
        listOf(AttributeType.DEFENSE, AttributeType.CRIT_RESIST),
        listOf(AttributeType.ATTACK, AttributeType.HEALTH, AttributeType.CRIT),
        listOf(AttributeType.ATTACK, AttributeType.HEALTH, AttributeType.DEFENSE, AttributeType.CRIT_RESIST),
    ),
    HEALTH_CRIT(
        "生命，暴击",
        listOf(AttributeType.HEALTH, AttributeType.CRIT),
        listOf(AttributeType.DEFENSE, AttributeType.CRIT, AttributeType.CRIT_RESIST),
        listOf(AttributeType.ATTACK, AttributeType.HEALTH, AttributeType.DEFENSE, AttributeType.CRIT),
    ),
    CRIT_CRIT_RESIST(
        "暴击，暴击抗性",
        listOf(AttributeType.CRIT, AttributeType.CRIT_RESIST),
        listOf(AttributeType.HEALTH, AttributeType.DEFENSE, AttributeType.CRIT),
        listOf(AttributeType.DEFENSE, AttributeType.HEALTH, AttributeType.CRIT, AttributeType.CRIT_RESIST),
    );

    fun layerAttributes(index: Int): List<AttributeType> =
        when (index) {
            0 -> firstLayer
            1 -> secondLayer
            else -> thirdLayer
        }
}

private fun List<String>.rewriteUnlockableAttributes(attributes: List<AttributeType>): List<String> {
    var index = 0
    return map { row ->
        buildString {
            row.forEach { symbol ->
                if (symbol in UppercaseAttributeSymbols) {
                    append(attributes[index % attributes.size].boardSymbol)
                    index += 1
                } else {
                    append(symbol)
                }
            }
        }
    }
}

private val UppercaseAttributeSymbols = setOf('A', 'K', 'H', 'D', 'R')

private val AttributeType.boardSymbol: Char
    get() = when (this) {
        AttributeType.ATTACK -> 'A'
        AttributeType.CRIT -> 'K'
        AttributeType.HEALTH -> 'H'
        AttributeType.DEFENSE -> 'D'
        AttributeType.CRIT_RESIST -> 'R'
    }

enum class CustomApostleRace(
    val displayName: String,
    val rows: List<List<String>>,
) {
    FAIRY("精灵", listOf(
        listOf("...E...", ".kWWWD.", ".A.W.r.", ".W.W.W.", ".W.W.W.", ".W.W.W.", ".dWWWa.", ".W.W.W.", ".W.W.W.", ".W.W.W.", ".W.W.h.", ".WWWWW.", "...S..."),
        listOf("...E...", "...W...", "...W...", "...W...", "...W...", "..WWW..", "..a.W..", "..H.W..", ".KkWW..", ".d..W..", ".h..W..", ".RrWW..", "...W..."),
        listOf(".......", "...H...", "..WrK..", "..W.d..", "..W.W..", "..W.W..", ".RW.W..", ".hW.a..", "..W.D..", "..W.k..", "..WWW..", "...W...", "...W..."),
    )),
    CELESTIAL("仙灵", listOf(
        listOf("...E...", "WWWWWWW", "Wk.r.dW", "WK....W", "W.....W", "W.....W", "W.....a", "WWWWWhR", ".W...W.", ".W...W.", ".W...W.", ".WWWWW.", "...S..."),
        listOf("...E...", "..aW...", "..WW...", "..W....", "..W....", "..W....", "HhWWrK.", "..W....", "..W....", "..WrR..", "..W.d..", "..WWW..", "...W..."),
        listOf(".......", "..Hk...", "...W...", "...W...", "...W...", "...WW..", ".KhWWW.", ".RaWdD.", "..rWW..", "...WW..", "...W...", "...W...", "...W..."),
    )),
    WITCH("魔女", listOf(
        listOf("...E...", ".dWWWW.", ".WW.rW.", ".k...W.", ".W...W.", ".aW.WW.", ".DWhWW.", "..WWW..", ".WWWWW.", ".W.W.W.", ".W.W.W.", ".W.W.A.", "...S..."),
        listOf("...E...", "...W...", "...W...", "...W...", ".DrW...", "..WWWh.", "..W.kA.", "..W....", "..W....", "..W....", "..W.dH.", "..WWWa.", "...W..."),
        listOf(".......", "...A...", "...h...", "...W...", "...WW..", "...WW..", ".WWW...", "dW.W...", "H..W...", "rW.W...", "Kk.W...", ".RaW...", "...W..."),
    )),
    BEAST("兽人", listOf(
        listOf("...E...", "WWWWWWW", "H.kW..r", "...WW..", "...WW..", "...WWd.", "...WWa.", ".WWWW..", "hWW....", "WW.KW..", "WW..W..", ".WWWW..", "...S..."),
        listOf("...E...", "...W...", "...W...", "...W...", "...W.K.", "...WWk.", "..hWWa.", "..WWrR.", ".dWW...", ".D.W...", "...W...", "...W...", "...W..."),
        listOf(".......", ".D.....", ".d.....", ".W..A..", ".W..h..", ".W..W..", ".WWWWH.", ".W..ar.", ".W..K..", ".WWWk..", "...W...", "...W...", "...W..."),
    )),
    NATURE("自然灵", listOf(
        listOf("..E....", "A.Wr..a", "k.W...W", "WWW..WW", "..W.WW.", "..WWWW.", "H...W..", "dW..WWW", ".WW.W..", "WWW.WWh", "..WWW..", "...W...", "...S..."),
        listOf("..E....", "..WW...", "..W....", "..W....", "..W....", "..W....", "aWW....", "..W.R..", "..WWd..", "DkW....", ".hW....", "..WWr..", "..W.A.."),
        listOf(".......", "..K....", "..r....", "h.W....", "WWWWa..", "..WWR..", "..W....", "dWWWk..", "D.W.A..", "..W....", "..W....", "..W....", "..W...."),
    )),
    GHOST("幽灵", listOf(
        listOf("...E...", ".rWWWa.", "kWW.WWW", "W.....W", "W.....W", "W.....W", "W.....W", "WWW.WWW", "..W.W..", "..W.W..", ".Wh.WW.", "HdK.WW.", "....S.."),
        listOf("...E...", "...W...", "...W.K.", "...W.h.", "...WWW.", "...W...", "...W...", "kWWW...", "R..W...", "...W...", "...W...", "..aW...", "..rWdD."),
        listOf(".......", "...D...", "...r...", "..WW...", "dWWWWhA", "...W...", "...W...", ".H.W.K.", ".aWWWk.", "...W...", "...W...", "...W...", "...W..."),
    )),
    DRAGON("龙族", listOf(
        listOf("..E....", "WWW....", "WWW....", "WW.....", "WW.....", "WW..Ra.", "WW...WW", "WW...WW", "hWW..Wk", "DdW..W.", ".WWWWr.", "..WWW..", "...S..."),
        listOf("..E....", "..W....", "..W....", "..W....", "..W....", "WrWWk..", "h.W.A..", "H.W.K..", "aWWWd..", "..W....", "..W....", "..W....", "..W...."),
        listOf(".......", "..R....", "..r....", ".DkA...", "dWWWW..", "WW..Wh.", "W..Ha..", "W......", "W......", "WW.....", ".W.....", ".WW....", "..W...."),
    )),
}
