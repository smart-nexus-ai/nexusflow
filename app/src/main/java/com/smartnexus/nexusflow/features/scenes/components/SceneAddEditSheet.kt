package com.smartnexus.nexusflow.features.scenes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.features.scenes.SceneRelayOption

data class SceneFormData(
    val id: String? = null,
    val name: String = "",
    val iconName: String = "WbSunny",
    val relayStates: Map<String, Boolean> = emptyMap()
)

val sceneAvailableIcons = mapOf(
    "WbSunny" to Icons.Default.WbSunny,
    "Movie" to Icons.Default.Movie,
    "Bedtime" to Icons.Default.Bedtime,
    "AutoAwesome" to Icons.Default.AutoAwesome,
    "Home" to Icons.Default.Home,
    "Lightbulb" to Icons.Default.Lightbulb
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SceneAddEditSheet(
    initialData: SceneFormData?,
    onDismiss: () -> Unit,
    onSave: (SceneFormData) -> Unit,
    availableRelays: List<SceneRelayOption>,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sceneName by remember { mutableStateOf(initialData?.name ?: "") }
    var selectedIconName by remember { mutableStateOf(initialData?.iconName ?: "WbSunny") }
    var addRelayDropdownExpanded by remember { mutableStateOf(false) }

    val relayStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            putAll(
                initialData?.relayStates ?: mapOf(
                    "Living Light, Living Room" to true,
                    "Ceiling Fan, Living Room" to true,
                    "Bedroom Fan, Bedroom" to true
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialData?.id == null) "Create Preset Scene" else "Edit Preset Scene",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Scene Name Input
            OutlinedTextField(
                value = sceneName,
                onValueChange = { sceneName = it },
                label = { Text("Scene Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            // Icon Picker
            Column {
                Text(
                    text = "Select Scene Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sceneAvailableIcons.forEach { (key, iconVector) ->
                        val isSelected = selectedIconName == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedIconName = key },
                            label = {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = key,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Target Relay States Toggles & Add Relay
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preset Relay Actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Box {
                        Button(
                            onClick = { addRelayDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Relay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        DropdownMenu(
                            expanded = addRelayDropdownExpanded,
                            onDismissRequest = { addRelayDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 280.dp)
                        ) {
                            if (availableRelays.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No relays available (0 devices connected)", color = Color.Gray, fontSize = 13.sp) },
                                    onClick = { addRelayDropdownExpanded = false }
                                )
                            } else {
                                availableRelays.forEach { option ->
                                    val keyName = "${option.relayName}, ${option.deviceName}"
                                    val alreadyAdded = relayStates.containsKey(keyName)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$keyName (${option.relayId})${if (alreadyAdded) " ✓" else ""}",
                                                fontWeight = if (alreadyAdded) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (alreadyAdded) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            addRelayDropdownExpanded = false
                                            if (!alreadyAdded) {
                                                relayStates[keyName] = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (relayStates.isEmpty()) {
                    Text(
                        text = "No relays added yet. Tap '+ Add Relay' to add relays.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    relayStates.keys.forEach { relayLabel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = relayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (relayStates[relayLabel] == true) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (relayStates[relayLabel] == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = relayStates[relayLabel] == true,
                                    onCheckedChange = { isChecked -> relayStates[relayLabel] = isChecked }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { relayStates.remove(relayLabel) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Relay",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "Cancel")
                }

                Button(
                    onClick = {
                        if (sceneName.isNotBlank()) {
                            onSave(
                                SceneFormData(
                                    id = initialData?.id,
                                    name = sceneName.trim(),
                                    iconName = selectedIconName,
                                    relayStates = relayStates.toMap()
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = sceneName.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Save Scene", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SceneAddEditSheetPreview() {
    NexusFlowTheme(darkTheme = true) {
        SceneAddEditSheet(
            initialData = null,
            onDismiss = {},
            onSave = {},
            availableRelays = emptyList()
        )
    }
}
