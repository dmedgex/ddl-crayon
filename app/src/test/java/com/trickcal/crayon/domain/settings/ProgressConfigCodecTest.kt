package com.trickcal.crayon.domain.settings

import com.trickcal.crayon.model.ProgressConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressConfigCodecTest {
    @Test
    fun encodeAndDecode_preservesValidSlotIds() {
        val config = ProgressConfig(
            exportedAt = 123456789L,
            litSlotIds = listOf("a_1", "b_2"),
        )

        val encoded = ProgressConfigCodec.encode(config)
        val decoded = ProgressConfigCodec.decode(
            raw = encoded,
            validSlotIds = setOf("a_1", "b_2", "c_3"),
        )

        assertEquals(2, decoded.sourceSlotCount)
        assertEquals(0, decoded.ignoredSlotCount)
        assertEquals(setOf("a_1", "b_2"), decoded.slotIds)
    }

    @Test
    fun decode_ignoresUnknownSlotIds() {
        val raw = """
            {
              "version": 1,
              "exportedAt": 42,
              "litSlotIds": ["known_1", "unknown_2", "known_3"]
            }
        """.trimIndent()

        val decoded = ProgressConfigCodec.decode(
            raw = raw,
            validSlotIds = setOf("known_1", "known_3"),
        )

        assertEquals(3, decoded.sourceSlotCount)
        assertEquals(1, decoded.ignoredSlotCount)
        assertEquals(setOf("known_1", "known_3"), decoded.slotIds)
    }

    @Test
    fun decode_supportsEmptyProgress() {
        val raw = """
            {
              "version": 1,
              "exportedAt": 42,
              "litSlotIds": []
            }
        """.trimIndent()

        val decoded = ProgressConfigCodec.decode(
            raw = raw,
            validSlotIds = emptySet(),
        )

        assertEquals(0, decoded.sourceSlotCount)
        assertEquals(0, decoded.ignoredSlotCount)
        assertTrue(decoded.slotIds.isEmpty())
    }
}
