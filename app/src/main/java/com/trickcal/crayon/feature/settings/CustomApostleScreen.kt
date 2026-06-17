package com.trickcal.crayon.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trickcal.crayon.model.PersonalityType
import com.trickcal.crayon.repository.CreateCustomApostleRequest
import com.trickcal.crayon.repository.CustomApostleCrayonChoice
import com.trickcal.crayon.repository.CustomApostleRace
import com.trickcal.crayon.repository.CustomApostleRepository
import com.trickcal.crayon.ui.components.AvatarBadge
import com.trickcal.crayon.ui.components.ImageUriPreview
import com.trickcal.crayon.ui.components.SectionTitle
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomApostleScreen(
    repository: CustomApostleRepository,
    modifier: Modifier = Modifier,
) {
    val customApostles by repository.profiles.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf(PersonalityType.LIGHT) }
    var race by remember { mutableStateOf(CustomApostleRace.FAIRY) }
    var firstLayerChoice by remember { mutableStateOf(CustomApostleCrayonChoice.entries.first()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            imageUri = uri
        }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ImageUriPreview(
                            imageUri = imageUri,
                            size = 104.dp,
                            circular = true,
                        )
                        OutlinedButton(onClick = { imageLauncher.launch(arrayOf("image/*")) }) {
                            Text(if (imageUri == null) "上传使徒图片" else "更换使徒图片")
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("使徒名字") },
                    )
                    OptionButtonGroup(
                        label = "属性",
                        options = PersonalityType.entries.map { it.displayName to it },
                        selected = personality,
                        onSelect = { personality = it },
                    )
                    OptionButtonGroup(
                        label = "种族",
                        options = CustomApostleRace.entries.map { it.displayName to it },
                        selected = race,
                        onSelect = { race = it },
                    )
                    OptionButtonGroup(
                        label = "第一层蜡笔格子",
                        options = CustomApostleCrayonChoice.entries.map { it.displayName to it },
                        selected = firstLayerChoice,
                        onSelect = { firstLayerChoice = it },
                    )
                    Text(
                        text = "第二层：${firstLayerChoice.secondLayer.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "第三层：${firstLayerChoice.thirdLayer.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.addApostle(
                                    CreateCustomApostleRequest(
                                        name = name,
                                        personality = personality,
                                        race = race,
                                        crayonChoice = firstLayerChoice,
                                        imageUri = imageUri,
                                    ),
                                )
                                name = ""
                                imageUri = null
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("确认新增")
                    }
                }
            }
        }
        if (customApostles.isNotEmpty()) {
            item {
                SectionTitle(title = "已新增使徒")
            }
            items(customApostles, key = { it.id }) { apostle ->
                Card {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarBadge(name = apostle.name, avatarKey = apostle.avatarKey)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(apostle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(apostle.personality.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.deleteApostle(apostle.id)
                                }
                            },
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionButtonGroup(
    label: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (text, value) ->
                if (value == selected) {
                    Button(onClick = { onSelect(value) }) {
                        Text(text)
                    }
                } else {
                    OutlinedButton(onClick = { onSelect(value) }) {
                        Text(text)
                    }
                }
            }
        }
    }
}
