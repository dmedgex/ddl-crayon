package com.trickcal.crayon.feature.battlepower

import androidx.lifecycle.ViewModel
import com.trickcal.crayon.domain.battlepower.BattlePowerCalculationRow
import com.trickcal.crayon.domain.battlepower.BattlePowerCalculatorEngine
import com.trickcal.crayon.domain.battlepower.BattlePowerCalculatorFieldDefinition
import com.trickcal.crayon.domain.battlepower.BattlePowerCalculatorParsedInputs
import com.trickcal.crayon.model.BattlePowerCharacterCatalog
import com.trickcal.crayon.model.BattlePowerCharacterProfile
import com.trickcal.crayon.repository.BattlePowerCharacterRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BattlePowerCalculatorInputValue(
    val id: String,
    val label: String,
    val rawValue: String,
    val description: String,
    val errorMessage: String? = null,
)

data class BattlePowerCalculatorConstantValue(
    val id: String,
    val label: String,
    val value: String,
    val description: String,
)

data class BattlePowerCalculatorOutputRow(
    val id: String,
    val label: String,
    val valueText: String,
    val description: String,
)

data class BattlePowerCalculatorUiState(
    val characterQuery: String = "",
    val characterSuggestions: List<BattlePowerCharacterProfile> = emptyList(),
    val selectedCharacter: BattlePowerCharacterProfile? = null,
    val inputs: List<BattlePowerCalculatorInputValue> = emptyList(),
    val constants: List<BattlePowerCalculatorConstantValue> = emptyList(),
    val outputs: List<BattlePowerCalculatorOutputRow> = emptyList(),
    val errorMessage: String? = null,
    val loadErrorMessage: String? = null,
    val isMoreSettingsExpanded: Boolean = false,
) {
    val power: BattlePowerCalculatorOutputRow?
        get() = outputs.firstOrNull { it.id == "power" }
}

class BattlePowerCalculatorViewModel(
    repository: BattlePowerCharacterRepository,
) : ViewModel() {
    private val allCharacters: List<BattlePowerCharacterProfile>
    private val mutableUiState: MutableStateFlow<BattlePowerCalculatorUiState>

    init {
        val catalogResult = runCatching { repository.loadCatalog() }
        val catalog = catalogResult.getOrNull()
        allCharacters = catalog?.characters.orEmpty()
        mutableUiState = MutableStateFlow(buildInitialUiState(catalog))
    }

    val uiState: StateFlow<BattlePowerCalculatorUiState> = mutableUiState.asStateFlow()

    fun updateCharacterQuery(rawQuery: String) {
        mutableUiState.update { state ->
            val normalizedQuery = rawQuery.trim()
            val selectedCharacter = state.selectedCharacter?.takeIf { it.name == normalizedQuery }
            state.copy(
                characterQuery = rawQuery,
                characterSuggestions = buildCharacterSuggestions(rawQuery, selectedCharacter),
                selectedCharacter = selectedCharacter,
                outputs = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun selectCharacter(characterUid: Int) {
        val character = allCharacters.firstOrNull { it.uid == characterUid } ?: return
        mutableUiState.update { state ->
            state.copy(
                characterQuery = character.name,
                characterSuggestions = emptyList(),
                selectedCharacter = character,
                outputs = emptyList(),
                errorMessage = null,
            )
        }
    }

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
        val selectedCharacter = state.selectedCharacter
        if (selectedCharacter == null) {
            mutableUiState.value =
                state.copy(
                    outputs = emptyList(),
                    errorMessage = "请先选择一个使徒。",
                )
            return
        }

        val parseResult = parseInputs(state.inputs)
        if (parseResult.parsedValues == null) {
            mutableUiState.value =
                state.copy(
                    inputs = parseResult.normalizedInputs,
                    outputs = emptyList(),
                    errorMessage = "请先修正输入参数中的错误。",
                )
            return
        }

        val result =
            BattlePowerCalculatorEngine.calculate(
                inputs = BattlePowerCalculatorParsedInputs(
                    attackType = selectedCharacter.attackType,
                    attackPhysic = parseResult.parsedValues.getValue("AttackPhysic"),
                    attackMagic = parseResult.parsedValues.getValue("AttackMagic"),
                    defensePhysic = parseResult.parsedValues.getValue("DefensePhysic"),
                    defenseMagic = parseResult.parsedValues.getValue("DefenseMagic"),
                    hp = parseResult.parsedValues.getValue("Hp"),
                    criticalRate = parseResult.parsedValues.getValue("CriticalRate"),
                    criticalMult = parseResult.parsedValues.getValue("CriticalMult"),
                    criticalResist = parseResult.parsedValues.getValue("CriticalResist"),
                    criticalMultResist = parseResult.parsedValues.getValue("CriticalMultResist"),
                    attackSpeed = parseResult.parsedValues.getValue("AttackSpeed"),
                    spSkillLevel = parseResult.parsedValues.getValue("SpSkillLevel"),
                    ultimateSkillLevel = parseResult.parsedValues.getValue("UltimateSkillLevel"),
                    passiveSkillLevel = parseResult.parsedValues.getValue("PassiveSkillLevel"),
                    activeSkillValueA = selectedCharacter.activeSkillValueA,
                    ultimateSkillValueA = selectedCharacter.ultimateSkillValueA,
                    passiveValueA = selectedCharacter.passiveValueA,
                    weightValueA = selectedCharacter.weightValueA,
                    asideGrade = parseResult.parsedValues.getValue("AsideGrade"),
                    asideValueA = selectedCharacter.asideValueA,
                ),
            )

        mutableUiState.value =
            state.copy(
                inputs = parseResult.normalizedInputs,
                outputs = result.rows.map(::toUiRow),
                errorMessage = null,
            )
    }

    private fun buildInitialUiState(catalog: BattlePowerCharacterCatalog?): BattlePowerCalculatorUiState {
        if (catalog == null) {
            return BattlePowerCalculatorUiState(
                inputs = BattlePowerCalculatorEngine.inputDefinitions.map(::toInputValue),
                constants = BattlePowerCalculatorEngine.constantDefinitions.map(::toConstantValue),
                loadErrorMessage = "加载使徒系数失败，请检查 battle_power 资产是否存在。",
            )
        }

        val defaultCharacter =
            catalog.characters.firstOrNull { it.uid == catalog.defaultCharacterUid }
                ?: catalog.characters.first()
        return BattlePowerCalculatorUiState(
            characterQuery = defaultCharacter.name,
            selectedCharacter = defaultCharacter,
            inputs = BattlePowerCalculatorEngine.inputDefinitions.map(::toInputValue),
            constants = BattlePowerCalculatorEngine.constantDefinitions.map(::toConstantValue),
        )
    }

    private fun buildCharacterSuggestions(
        rawQuery: String,
        selectedCharacter: BattlePowerCharacterProfile?,
    ): List<BattlePowerCharacterProfile> {
        val query = rawQuery.trim()
        if (query.isBlank() || selectedCharacter?.name == query) {
            return emptyList()
        }
        return allCharacters.filter { character ->
            character.name.contains(query, ignoreCase = true) ||
                character.resourceName.contains(query, ignoreCase = true) ||
                character.uid.toString().contains(query)
        }
    }

    private fun parseInputs(inputs: List<BattlePowerCalculatorInputValue>): ParseInputsResult {
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

        return if (normalizedInputs.any { it.errorMessage != null }) {
            ParseInputsResult(
                normalizedInputs = normalizedInputs,
                parsedValues = null,
            )
        } else {
            ParseInputsResult(
                normalizedInputs = normalizedInputs,
                parsedValues = parsedValues,
            )
        }
    }

    private fun toUiRow(row: BattlePowerCalculationRow): BattlePowerCalculatorOutputRow =
        BattlePowerCalculatorOutputRow(
            id = row.id,
            label = row.label,
            valueText = formatNumericValue(row.value),
            description = row.description,
        )

    private fun toInputValue(definition: BattlePowerCalculatorFieldDefinition): BattlePowerCalculatorInputValue =
        BattlePowerCalculatorInputValue(
            id = definition.id,
            label = definition.label,
            rawValue = definition.defaultValue,
            description = definition.description,
        )

    private fun toConstantValue(definition: BattlePowerCalculatorFieldDefinition): BattlePowerCalculatorConstantValue =
        BattlePowerCalculatorConstantValue(
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
        val normalizedInputs: List<BattlePowerCalculatorInputValue>,
        val parsedValues: Map<String, Double>?,
    )
}
