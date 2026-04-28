package com.trickcal.crayon.domain.settings

import com.trickcal.crayon.model.PreparedProgressImport
import com.trickcal.crayon.model.ProgressConfig
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
import kotlinx.serialization.json.longOrNull

object ProgressConfigCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(config: ProgressConfig): String {
        val payload = buildJsonObject {
            put("version", JsonPrimitive(config.version))
            put("exportedAt", JsonPrimitive(config.exportedAt))
            put(
                "litSlotIds",
                JsonArray(config.litSlotIds.sorted().map(::JsonPrimitive)),
            )
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    fun decode(
        raw: String,
        validSlotIds: Set<String>,
    ): PreparedProgressImport {
        val root = runCatching {
            json.parseToJsonElement(raw).jsonObject
        }.getOrElse {
            throw IllegalArgumentException("配置文件不是有效的 JSON。")
        }

        val version = root["version"]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("配置文件缺少 version。")
        if (version != ProgressConfig.CURRENT_VERSION) {
            throw IllegalArgumentException("不支持的配置版本：$version。")
        }

        root["exportedAt"]?.jsonPrimitive?.longOrNull
            ?: throw IllegalArgumentException("配置文件缺少 exportedAt。")

        val sourceSlotIds = root["litSlotIds"]?.jsonArray?.mapNotNull { element ->
            element.jsonPrimitive.contentOrNull
        } ?: throw IllegalArgumentException("配置文件缺少 litSlotIds。")

        val filteredSlotIds = sourceSlotIds
            .filter { it in validSlotIds }
            .toSet()

        return PreparedProgressImport(
            sourceSlotCount = sourceSlotIds.size,
            ignoredSlotCount = sourceSlotIds.size - filteredSlotIds.size,
            slotIds = filteredSlotIds,
        )
    }
}
