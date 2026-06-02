package com.trickcal.crayon.feature.battlepower

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.model.BattlePowerCharacterProfile
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.MetricCard
import com.trickcal.crayon.ui.components.SectionTitle
import java.math.BigDecimal

@Composable
fun BattlePowerCalculatorScreen(
    uiState: BattlePowerCalculatorUiState,
    onCharacterQueryChange: (String) -> Unit,
    onCharacterSelected: (Int) -> Unit,
    onInputChange: (String, String) -> Unit,
    onToggleMoreSettings: () -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultRows = buildBattlePowerResultRows(uiState.outputs)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "战斗力计算器Beta",
                subtitle = "选择使徒后，按角色系数、最终属性和固定权重计算当前战斗力。",
            )
        }

        uiState.loadErrorMessage?.let { message ->
            item { ErrorCard(message = message) }
        }

        uiState.errorMessage?.let { message ->
            item { ErrorCard(message = message) }
        }

        item {
            CharacterSelectorCard(
                query = uiState.characterQuery,
                selectedCharacter = uiState.selectedCharacter,
                suggestions = uiState.characterSuggestions,
                onQueryChange = onCharacterQueryChange,
                onCharacterSelected = onCharacterSelected,
            )
        }

        item {
            CharacterCoefficientsCard(character = uiState.selectedCharacter)
        }

        item {
            ParameterSectionCard(
                title = "输入参数",
                subtitle = "以下参数可手动修改，点击底部按钮后重新计算。",
            ) {
                EditableFieldGrid(
                    fields = uiState.inputs,
                    onInputChange = onInputChange,
                )
            }
        }

        item {
            MoreSettingsCard(
                constants = uiState.constants,
                expanded = uiState.isMoreSettingsExpanded,
                onToggle = onToggleMoreSettings,
            )
        }

        item {
            Button(
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.loadErrorMessage == null,
            ) {
                Text("计算战斗力")
            }
        }

        if (uiState.outputs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "尚未生成结果",
                    message = "确认使徒和输入参数后，点击“计算战斗力”查看输出。",
                )
            }
        } else {
            uiState.power?.let { power ->
                item {
                    MetricCard(
                        title = "战斗力",
                        value = power.valueText,
                    )
                }
            }

            if (resultRows.isNotEmpty()) {
                item {
                    BattlePowerResultsCard(rows = resultRows)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CharacterSelectorCard(
    query: String,
    selectedCharacter: BattlePowerCharacterProfile?,
    suggestions: List<BattlePowerCharacterProfile>,
    onQueryChange: (String) -> Unit,
    onCharacterSelected: (Int) -> Unit,
) {
    ParameterSectionCard(
        title = "使徒选择",
        subtitle = "输入使徒名后，在下方候选中点选一个角色，系统会自动带入攻击类型和角色系数。",
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("使徒名")
            },
            supportingText = {
                Text(
                    if (selectedCharacter != null) {
                        "当前已选：${selectedCharacter.name}"
                    } else {
                        "请输入使徒名并从候选列表中选择。"
                    },
                )
            },
            singleLine = true,
        )

        AnimatedVisibility(visible = query.isNotBlank() && suggestions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Column {
                    suggestions.forEachIndexed { index, character ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCharacterSelected(character.uid) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = character.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${character.attackType} / ${character.resourceName} / ${character.uid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = { onCharacterSelected(character.uid) }) {
                                Text("选择")
                            }
                        }
                        if (index != suggestions.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = query.isNotBlank() && selectedCharacter == null && suggestions.isEmpty(),
        ) {
            Text(
                text = "没有找到匹配的使徒，请继续修改关键词。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CharacterCoefficientsCard(character: BattlePowerCharacterProfile?) {
    ParameterSectionCard(
        title = "角色系数",
        subtitle = "以下内容由所选使徒自动带入，不需要手动输入。",
    ) {
        if (character == null) {
            Text(
                text = "尚未选中使徒。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CharacterCoefficientLine(label = "攻击类型", value = character.attackType)
            CharacterCoefficientLine(label = "低年级技能系数", value = formatReadonlyValue(character.activeSkillValueA))
            CharacterCoefficientLine(label = "高年级技能系数", value = formatReadonlyValue(character.ultimateSkillValueA))
            CharacterCoefficientLine(label = "被动技能系数", value = formatReadonlyValue(character.passiveValueA))
            CharacterCoefficientLine(label = "角色固定系数", value = formatReadonlyValue(character.weightValueA))
            CharacterCoefficientLine(label = "坨格系数", value = formatReadonlyValue(character.asideValueA))
        }
    }
}

@Composable
private fun CharacterCoefficientLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ParameterSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(
                title = title,
                subtitle = subtitle,
            )
            content()
        }
    }
}

@Composable
private fun EditableFieldGrid(
    fields: List<BattlePowerCalculatorInputValue>,
    onInputChange: (String, String) -> Unit,
) {
    ResponsiveFieldGrid(count = fields.size) { index ->
        val field = fields[index]
        OutlinedTextField(
            value = field.rawValue,
            onValueChange = { value -> onInputChange(field.id, value) },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(field.label)
            },
            supportingText = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(field.description)
                    field.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            isError = field.errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

@Composable
private fun MoreSettingsCard(
    constants: List<BattlePowerCalculatorConstantValue>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "更多设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "运行时常量已固定，只展示默认值供核对。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起更多设置" else "展开更多设置",
                )
            }

            AnimatedVisibility(visible = expanded) {
                ReadOnlyFieldGrid(fields = constants)
            }
        }
    }
}

@Composable
private fun ReadOnlyFieldGrid(fields: List<BattlePowerCalculatorConstantValue>) {
    ResponsiveFieldGrid(count = fields.size) { index ->
        val field = fields[index]
        OutlinedTextField(
            value = field.value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(field.label)
            },
            supportingText = {
                Text(field.description)
            },
            readOnly = true,
            enabled = false,
            singleLine = true,
        )
    }
}

@Composable
private fun ResponsiveFieldGrid(
    count: Int,
    content: @Composable (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 720.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until count)
                .toList()
                .chunked(columns)
                .forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        rowIndices.forEach { index ->
                            Column(modifier = Modifier.weight(1f)) {
                                content(index)
                            }
                        }
                        repeat(columns - rowIndices.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
        }
    }
}

@Composable
private fun BattlePowerResultsCard(rows: List<BattlePowerResultRow>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val cardWidth = compactBattlePowerResultCardWidth(maxWidth)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.width(cardWidth),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(
                        title = "计算表",
                        subtitle = "左侧显示变量，右侧显示当前计算值。",
                    )
                    rows.forEachIndexed { index, row ->
                        BattlePowerResultLine(row = row)
                        if (index != rows.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattlePowerResultLine(row: BattlePowerResultRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = row.valueText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal data class BattlePowerResultRow(
    val id: String,
    val label: String,
    val valueText: String,
)

internal fun buildBattlePowerResultRows(outputs: List<BattlePowerCalculatorOutputRow>): List<BattlePowerResultRow> =
    outputs
        .filterNot { it.id == "power" }
        .map { row ->
            BattlePowerResultRow(
                id = row.id,
                label = row.label,
                valueText = row.valueText,
            )
        }

internal fun compactBattlePowerResultCardWidth(width: Dp): Dp =
    (width * 0.92f).coerceAtMost(440.dp)

private fun formatReadonlyValue(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
