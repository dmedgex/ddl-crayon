package com.trickcal.crayon.repository

import com.trickcal.crayon.model.PetDispatchSelectionState
import com.trickcal.crayon.model.PetDispatchSelectionTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetDispatchSelectionStorageCodecTest {
    @Test
    fun encodeAndDecode_roundTripsSelectionState() {
        val state = PetDispatchSelectionState(
            selectedRegionName = "RegionA",
            selectedTaskCount = 4,
            selectedOwnedPetIds = linkedSetOf(1, 3, 5),
            selectedFarmPetIds = linkedSetOf(2, 4),
            selectedTab = PetDispatchSelectionTab.FARM,
        )

        val encoded = PetDispatchSelectionStorageCodec.encode(state)
        val decoded = PetDispatchSelectionStorageCodec.decode(encoded)

        assertEquals(state, decoded)
    }

    @Test
    fun decode_fallsBackToDefaultsWhenStoredValuesAreMissingOrInvalid() {
        val decoded = PetDispatchSelectionStorageCodec.decode(
            StoredPetDispatchSelectionState(
                selectedRegionName = "",
                selectedTaskCount = null,
                selectedOwnedPetIds = linkedSetOf("1", "oops"),
                selectedFarmPetIds = linkedSetOf("2", "bad"),
                selectedTab = "BROKEN",
            ),
        )

        assertNull(decoded.selectedRegionName)
        assertEquals(1, decoded.selectedTaskCount)
        assertEquals(linkedSetOf(1), decoded.selectedOwnedPetIds)
        assertEquals(linkedSetOf(2), decoded.selectedFarmPetIds)
        assertEquals(PetDispatchSelectionTab.OWNED, decoded.selectedTab)
    }
}
