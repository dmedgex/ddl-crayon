package com.trickcal.crayon.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PetDispatchCatalogJsonParserTest {
    @Test
    fun parse_generatedCatalog_containsExpectedPetAndRegionCounts() {
        val catalog = PetDispatchCatalogJsonParser.parse(loadGeneratedCatalogJson())

        assertEquals(33, catalog.pets.size)
        assertEquals(6, catalog.regions.size)
        assertEquals(33, catalog.pets.map { it.imageAssetName }.distinct().size)

        val regionsByName = catalog.regions.associateBy { it.name }
        assertEquals(5, regionsByName.getValue("仙灵王国").tasks.size)
        assertEquals(5, regionsByName.getValue("兽人村").tasks.size)
        assertEquals(5, regionsByName.getValue("魔女王国").tasks.size)
        assertTrue(regionsByName.getValue("莫纳蒂尔").tasks.isEmpty())
        assertTrue(regionsByName.getValue("自然灵之地").tasks.isEmpty())
        assertTrue(regionsByName.getValue("幽灵村").tasks.isEmpty())
    }
}

internal fun loadGeneratedCatalogJson(): String {
    val candidates = listOf(
        File("app/src/main/assets/pet_dispatch/catalog.json"),
        File("src/main/assets/pet_dispatch/catalog.json"),
    )
    val catalogFile = candidates.firstOrNull(File::exists)
        ?: error("Unable to locate generated pet dispatch catalog.json from test runtime.")
    return catalogFile.readText(Charsets.UTF_8)
}
