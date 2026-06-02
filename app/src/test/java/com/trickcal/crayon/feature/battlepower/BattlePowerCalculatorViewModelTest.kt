package com.trickcal.crayon.feature.battlepower

import com.trickcal.crayon.model.BattlePowerCharacterCatalog
import com.trickcal.crayon.model.BattlePowerCharacterProfile
import com.trickcal.crayon.repository.BattlePowerCharacterRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattlePowerCalculatorViewModelTest {
    @Test
    fun initialState_containsDefaultCharacterInputsAndConstants() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())
        val state = viewModel.uiState.value

        assertEquals("斯诺基", state.characterQuery)
        assertEquals("斯诺基", state.selectedCharacter?.name)
        assertEquals(14, state.inputs.size)
        assertEquals(8, state.constants.size)
        assertTrue(state.outputs.isEmpty())
        assertFalse(state.isMoreSettingsExpanded)
        assertNull(state.errorMessage)
    }

    @Test
    fun updateCharacterQuery_clearsSelectionUntilCharacterIsPickedAgain() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())

        viewModel.updateCharacterQuery("达雅")

        val state = viewModel.uiState.value
        assertNull(state.selectedCharacter)
        assertEquals(1, state.characterSuggestions.size)
        assertEquals("达雅", state.characterSuggestions.first().name)
    }

    @Test
    fun selectCharacter_updatesCharacterAndClearsSuggestions() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())
        viewModel.updateCharacterQuery("达雅")

        viewModel.selectCharacter(10001)

        val state = viewModel.uiState.value
        assertEquals("达雅", state.characterQuery)
        assertEquals(10001, state.selectedCharacter?.uid)
        assertTrue(state.characterSuggestions.isEmpty())
    }

    @Test
    fun toggleMoreSettings_updatesExpandedFlag() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())

        viewModel.toggleMoreSettings()
        assertTrue(viewModel.uiState.value.isMoreSettingsExpanded)

        viewModel.toggleMoreSettings()
        assertFalse(viewModel.uiState.value.isMoreSettingsExpanded)
    }

    @Test
    fun calculate_afterEditingValidInput_producesOrderedOutputs() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())

        viewModel.updateInput(inputId = "AttackPhysic", rawValue = "60000")
        viewModel.calculate()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(6, state.outputs.size)
        assertEquals("main_attack", state.outputs.first().id)
        assertEquals("power", state.outputs.last().id)
        assertNotNull(state.power)
    }

    @Test
    fun calculate_withoutSelectedCharacter_setsErrorAndClearsOutputs() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())
        viewModel.updateCharacterQuery("不存在")

        viewModel.calculate()

        val state = viewModel.uiState.value
        assertEquals("请先选择一个使徒。", state.errorMessage)
        assertTrue(state.outputs.isEmpty())
    }

    @Test
    fun calculate_withInvalidInput_setsFieldErrorAndClearsOutputs() {
        val viewModel = BattlePowerCalculatorViewModel(fakeRepository())

        viewModel.updateInput(inputId = "AttackPhysic", rawValue = "")
        viewModel.calculate()

        val state = viewModel.uiState.value
        val attackInput = state.inputs.first { it.id == "AttackPhysic" }
        assertEquals("请输入有效数字", attackInput.errorMessage)
        assertTrue(state.outputs.isEmpty())
        assertEquals("请先修正输入参数中的错误。", state.errorMessage)
    }

    private fun fakeRepository(): BattlePowerCharacterRepository =
        object : BattlePowerCharacterRepository {
            override fun loadCatalog(): BattlePowerCharacterCatalog =
                BattlePowerCharacterCatalog(
                    defaultCharacterUid = 10081,
                    characters = listOf(
                        character(
                            uid = 10001,
                            name = "达雅",
                            attackType = "魔法",
                            resourceName = "Daya",
                            weightValueA = 0.349999994039536,
                            asideValueA = 0.0,
                        ),
                        character(
                            uid = 10075,
                            name = "莉纽阿",
                            attackType = "物理",
                            resourceName = "RenewaAwaken",
                            weightValueA = 0.5,
                            asideValueA = 0.2,
                        ),
                        character(
                            uid = 10081,
                            name = "斯诺基",
                            attackType = "物理",
                            resourceName = "Snorky",
                            weightValueA = 0.340000003576279,
                            asideValueA = 0.699999988079071,
                        ),
                    ),
                )
        }

    private fun character(
        uid: Int,
        name: String,
        attackType: String,
        resourceName: String,
        weightValueA: Double,
        asideValueA: Double,
    ): BattlePowerCharacterProfile =
        BattlePowerCharacterProfile(
            uid = uid,
            name = name,
            resourceName = resourceName,
            attackType = attackType,
            nameKey = "Hero_Name_$resourceName",
            activeSkillValueA = 0.02,
            ultimateSkillValueA = 0.02,
            passiveValueA = 0.02,
            weightValueA = weightValueA,
            asideValueA = asideValueA,
        )
}
