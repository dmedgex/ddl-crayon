package com.trickcal.crayon.feature.damage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DamageCalculatorViewModelTest {
    @Test
    fun initialState_containsDefaultInputsAndConstants() {
        val viewModel = DamageCalculatorViewModel()
        val state = viewModel.uiState.value

        assertEquals(19, state.inputs.size)
        assertEquals(24, state.constants.size)
        assertTrue(state.outputs.isEmpty())
        assertFalse(state.isMoreSettingsExpanded)
        assertNull(state.errorMessage)
    }

    @Test
    fun toggleMoreSettings_updatesExpandedFlag() {
        val viewModel = DamageCalculatorViewModel()

        viewModel.toggleMoreSettings()
        assertTrue(viewModel.uiState.value.isMoreSettingsExpanded)

        viewModel.toggleMoreSettings()
        assertFalse(viewModel.uiState.value.isMoreSettingsExpanded)
    }

    @Test
    fun calculate_afterEditingValidInput_producesOrderedOutputs() {
        val viewModel = DamageCalculatorViewModel()

        viewModel.updateInput(inputId = "AttackInput", rawValue = "1500")
        viewModel.calculate()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(17, state.outputs.size)
        assertEquals("attack", state.outputs.first().id)
        assertEquals("final_damage", state.outputs.last().id)
        assertNotNull(state.finalDamage)
    }

    @Test
    fun calculate_withInvalidInput_setsFieldErrorAndClearsOutputs() {
        val viewModel = DamageCalculatorViewModel()

        viewModel.updateInput(inputId = "AttackInput", rawValue = "")
        viewModel.calculate()

        val state = viewModel.uiState.value
        val attackInput = state.inputs.first { it.id == "AttackInput" }
        assertEquals("请输入有效数字", attackInput.errorMessage)
        assertTrue(state.outputs.isEmpty())
        assertEquals("请先修正输入参数中的错误。", state.errorMessage)
    }

    @Test
    fun updateInput_clearsPreviousOutputsUntilNextCalculation() {
        val viewModel = DamageCalculatorViewModel()

        viewModel.calculate()
        assertTrue(viewModel.uiState.value.outputs.isNotEmpty())

        viewModel.updateInput(inputId = "AttackInput", rawValue = "1300")

        val state = viewModel.uiState.value
        assertTrue(state.outputs.isEmpty())
        assertNull(state.errorMessage)
    }
}
