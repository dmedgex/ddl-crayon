package com.trickcal.crayon.feature.damage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trickcal.crayon.ui.components.EmptyStateCard
import com.trickcal.crayon.ui.components.MetricCard
import com.trickcal.crayon.ui.components.SectionTitle

@Composable
fun DamageCalculatorScreen(
    uiState: DamageCalculatorUiState,
    onInputChange: (String, String) -> Unit,
    onToggleMoreSettings: () -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultRows = buildDamageResultRows(uiState.outputs)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "伤害计算器Beta",
                subtitle = "填写攻击、防御、技能和暴击相关参数后，按固定公式计算最终伤害。",
            )
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
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
            ) {
                Text("计算伤害")
            }
        }

        if (uiState.outputs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "尚未生成结果",
                    message = "填写或确认参数后，点击“计算伤害”查看完整输出。",
                )
            }
        } else {
            uiState.finalDamage?.let { finalDamage ->
                item {
                    MetricCard(
                        title = "最终伤害",
                        value = finalDamage.valueText,
                    )
                }
            }

            if (resultRows.isNotEmpty()) {
                item {
                    DamageResultsCard(rows = resultRows)
                }
            }
        }
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
    fields: List<DamageCalculatorInputValue>,
    onInputChange: (String, String) -> Unit,
) {
    ResponsiveFieldGrid(
        count = fields.size,
    ) { index ->
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
    constants: List<DamageCalculatorConstantValue>,
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
private fun ReadOnlyFieldGrid(fields: List<DamageCalculatorConstantValue>) {
    ResponsiveFieldGrid(
        count = fields.size,
    ) { index ->
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
private fun DamageResultsCard(rows: List<DamageResultRow>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val cardWidth = compactResultCardWidth(maxWidth)
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
                        DamageResultLine(row = row)
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
private fun DamageResultLine(row: DamageResultRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = row.valueText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal data class DamageResultRow(
    val id: String,
    val label: String,
    val valueText: String,
)

internal fun buildDamageResultRows(outputs: List<DamageCalculatorOutputRow>): List<DamageResultRow> =
    outputs
        .filterNot { it.id == "final_damage" }
        .map { row ->
            DamageResultRow(
                id = row.id,
                label = damageDisplayLabel(row),
                valueText = row.valueText,
            )
        }

internal fun compactResultCardWidth(width: Dp): Dp =
    (width * 0.92f).coerceAtMost(440.dp)

private fun damageDisplayLabel(row: DamageCalculatorOutputRow): String =
    when (row.id) {
        "x" -> "攻防差值x"
        "xc" -> "暴击差值xc"
        "xm" -> "暴伤差值xm"
        else -> row.label
    }
