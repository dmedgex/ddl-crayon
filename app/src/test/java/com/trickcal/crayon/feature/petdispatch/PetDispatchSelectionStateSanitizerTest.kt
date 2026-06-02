package com.trickcal.crayon.feature.petdispatch

import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchRarity
import com.trickcal.crayon.model.PetDispatchRegion
import com.trickcal.crayon.model.PetDispatchSelectionState
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.model.PetDispatchSkill
import com.trickcal.crayon.model.PetDispatchSkillLevel
import com.trickcal.crayon.model.PetDispatchTask
import org.junit.Assert.assertEquals
import org.junit.Test

class PetDispatchSelectionStateSanitizerTest {
    @Test
    fun sanitize_filtersMissingRegionAndPets_andPreservesSelectedTab() {
        val sanitized = PetDispatchSelectionStateSanitizer.sanitize(
            regions = listOf(
                PetDispatchRegion(
                    name = "RegionA",
                    tasks = listOf(PetDispatchTask(id = 1, area = "Area", task = "Task", bonusSkills = listOf("SkillA"))),
                ),
                PetDispatchRegion(
                    name = "RegionB",
                    tasks = emptyList(),
                ),
            ),
            pets = listOf(
                pet(id = 1, name = "PetA"),
                pet(id = 3, name = "PetC"),
            ),
            selection = PetDispatchSelectionState(
                selectedRegionName = "MissingRegion",
                selectedTaskCount = 9,
                selectedOwnedPetIds = linkedSetOf(1, 2, 3),
                selectedFarmPetIds = linkedSetOf(3, 4),
                selectedTab = PetDispatchSelectionTab.FARM,
            ),
        )

        assertEquals("RegionA", sanitized.selectedRegionName)
        assertEquals(5, sanitized.selectedTaskCount)
        assertEquals(linkedSetOf(1, 3), sanitized.selectedOwnedPetIds)
        assertEquals(linkedSetOf(3), sanitized.selectedFarmPetIds)
        assertEquals(PetDispatchSelectionTab.FARM, sanitized.selectedTab)
    }

    @Test
    fun sanitize_defaultsToFirstRegionWhenSelectionIsBlank() {
        val sanitized = PetDispatchSelectionStateSanitizer.sanitize(
            regions = listOf(PetDispatchRegion(name = "RegionA", tasks = emptyList())),
            pets = emptyList(),
            selection = PetDispatchSelectionState(selectedRegionName = " "),
        )

        assertEquals("RegionA", sanitized.selectedRegionName)
        assertEquals(PetDispatchSelectionTab.OWNED, sanitized.selectedTab)
    }

    private fun pet(id: Int, name: String): PetDispatchPet =
        PetDispatchPet(
            id = id,
            name = name,
            rarity = PetDispatchRarity.COMMON,
            baseScore = 2,
            skills = listOf(PetDispatchSkill(name = "SkillA", level = PetDispatchSkillLevel.C)),
            imageAssetName = "$name.png",
        )
}
