package com.trickcal.crayon.feature.damage

import androidx.lifecycle.ViewModel
import com.trickcal.crayon.domain.damage.DamageCalculationRow
import com.trickcal.crayon.domain.damage.DamageCalculatorEngine
import com.trickcal.crayon.domain.damage.DamageCalculatorFieldDefinition
import com.trickcal.crayon.domain.damage.DamageCalculatorParsedInputs
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DamageCalculatorInputValue(
    val id: String,
    val label: String,
    val rawValue: String,
    val description: String,
    val errorMessage: String? = null,
)

data class DamageCalculatorConstantValue(
    val id: String,
    val label: String,
    val value: String,
    val description: String,
)

data class DamageCalculatorOutputRow(
    val id: String,
    val label: String,
    val valueText: String,
    val description: String,
)

data class DamageCalculatorUiState(
    val inputs: List<DamageCalculatorInputValue> = emptyList(),
    val constants: List<DamageCalculatorConstantValue> = emptyList(),
    val outputs: List<DamageCalculatorOutputRow> = emptyList(),
    val errorMessage: String? = null,
    val isMoreSettingsExpanded: Boolean = false,
) {
    val finalDamage: DamageCalculatorOutputRow?
        get() = outputs.firstOrNull { it.id == "final_damage" }
}

class DamageCalculatorViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildInitialUiState())

    val uiState: StateFlow<DamageCalculatorUiState> = mutableUiState.asStateFlow()

    fun updateInput(
        inputId: String,
        rawValue: String,
    ) {
        mutableUiState.update { state ->
            state.copy(
                inputs = state.inputs.map { input ->
                    if (input.id == inputId) {
                        input.copy(rawValue = rawValue, errorMessage = null)
                    } else {
                        input
                    }
                },
                outputs = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun toggleMoreSettings() {
        mutableUiState.update { state ->
            state.copy(isMoreSettingsExpanded = !state.isMoreSettingsExpanded)
        }
    }

    fun calculate() {
        val state = mutableUiState.value
        val parseResult = parseInputs(state.inputs)
        if (parseResult.parsedInputs == null) {
            mutableUiState.value =
                state.copy(
                    inputs = parseResult.normalizedInputs,
                    outputs = emptyList(),
                    errorMessage = "请先修正输入参数中的错误。",
                )
            return
        }

        val result = DamageCalculatorEngine.calculate(parseResult.parsedInputs)
        mutableUiState.value =
            state.copy(
                inputs = parseResult.normalizedInputs,
                outputs = result.rows.map(::toUiRow),
                errorMessage = null,
            )
    }

    private fun parseInputs(inputs: List<DamageCalculatorInputValue>): ParseInputsResult {
        val parsedValues = linkedMapOf<String, Double>()
        val normalizedInputs =
            inputs.map { input ->
                val parsedValue = input.rawValue.trim().toDoubleOrNull()
                if (parsedValue == null) {
                    input.copy(errorMessage = "请输入有效数字")
                } else {
                    parsedValues[input.id] = parsedValue
                    input.copy(errorMessage = null)
                }
            }

        if (normalizedInputs.any { it.errorMessage != null }) {
            return ParseInputsResult(normalizedInputs = normalizedInputs, parsedInputs = null)
        }

        return ParseInputsResult(
            normalizedInputs = normalizedInputs,
            parsedInputs = DamageCalculatorParsedInputs(
                attackInput = parsedValues.getValue("AttackInput"),
                defenseInput = parsedValues.getValue("DefenseInput"),
                inputCoefficient = parsedValues.getValue("InputCoefficient"),
                addDamageCoefficient = parsedValues.getValue("AddDamageCoefficient"),
                effectDamage = parsedValues.getValue("EffectDamage"),
                personalityBonus = parsedValues.getValue("PersonalityBonus"),
                typeDamageBase = parsedValues.getValue("TypeDamageBase"),
                skillDamageRate = parsedValues.getValue("SkillDamageRate"),
                criticalStat = parsedValues.getValue("CriticalStat"),
                criticalResist = parsedValues.getValue("CriticalResist"),
                criticalRateBonus = parsedValues.getValue("CriticalRateBonus"),
                criticalRateReceiveBonus = parsedValues.getValue("CriticalRateReceiveBonus"),
                criticalMultStat = parsedValues.getValue("CriticalMultStat"),
                criticalMultResist = parsedValues.getValue("CriticalMultResist"),
                criticalMultBonus = parsedValues.getValue("CriticalMultBonus"),
                criticalMultReceiveBonus = parsedValues.getValue("CriticalMultReceiveBonus"),
                isCritical = parsedValues.getValue("IsCritical"),
                endDamageBase = parsedValues.getValue("EndDamageBase"),
                endDamageExtra = parsedValues.getValue("EndDamageExtra"),
            ),
        )
    }

    private fun toUiRow(row: DamageCalculationRow): DamageCalculatorOutputRow =
        DamageCalculatorOutputRow(
            id = row.id,
            label = row.label,
            valueText = formatNumericValue(row.value),
            description = row.description,
        )

    private fun buildInitialUiState(): DamageCalculatorUiState =
        DamageCalculatorUiState(
            inputs = DamageCalculatorEngine.inputDefinitions.map(::toInputValue),
            constants = DamageCalculatorEngine.constantDefinitions.map(::toConstantValue),
        )

    private fun toInputValue(definition: DamageCalculatorFieldDefinition): DamageCalculatorInputValue =
        DamageCalculatorInputValue(
            id = definition.id,
            label = definition.label,
            rawValue = definition.defaultValue,
            description = definition.description,
        )

    private fun toConstantValue(definition: DamageCalculatorFieldDefinition): DamageCalculatorConstantValue =
        DamageCalculatorConstantValue(
            id = definition.id,
            label = definition.label,
            value = definition.defaultValue,
            description = definition.description,
        )

    private fun formatNumericValue(value: Double): String {
        if (value == 0.0) {
            return "0"
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private data class ParseInputsResult(
        val normalizedInputs: List<DamageCalculatorInputValue>,
        val parsedInputs: DamageCalculatorParsedInputs?,
    )
}
