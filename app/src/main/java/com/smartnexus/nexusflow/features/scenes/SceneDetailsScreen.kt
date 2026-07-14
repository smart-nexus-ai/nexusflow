package com.smartnexus.nexusflow.features.scenes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.components.DeviceAnimationDisplay
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.domain.model.DeviceType

data class SceneRelayOption(
    val relayName: String,
    val deviceName: String,
    val relayId: String,
    val deviceType: DeviceType,
    val deviceIndex: Int
)

val all22SceneRelays = listOf(
    // Living Room (8 channels) -> Index 1
    SceneRelayOption("Living Light", "Living Room", "CH 1", DeviceType.LIGHT, 1),
    SceneRelayOption("Ceiling Fan", "Living Room", "CH 2", DeviceType.FAN, 1),
    SceneRelayOption("TV Unit", "Living Room", "CH 3", DeviceType.TV, 1),
    SceneRelayOption("LED Strip", "Living Room", "CH 4", DeviceType.LIGHT, 1),
    SceneRelayOption("Spare Switch", "Living Room", "CH 5", DeviceType.SWITCH, 1),
    SceneRelayOption("Balcony Light", "Living Room", "CH 6", DeviceType.LIGHT, 1),
    SceneRelayOption("Air Purifier", "Living Room", "CH 7", DeviceType.HUMIDIFIER, 1),
    SceneRelayOption("Accent Lamp", "Living Room", "CH 8", DeviceType.LIGHT, 1),

    // Bedroom (6 channels) -> Index 2
    SceneRelayOption("Bedroom Light", "Bedroom", "CH 1", DeviceType.LIGHT, 2),
    SceneRelayOption("Nightstand Lamp", "Bedroom", "CH 2", DeviceType.LIGHT, 2),
    SceneRelayOption("Bedroom Fan", "Bedroom", "CH 3", DeviceType.FAN, 2),
    SceneRelayOption("Air Conditioner", "Bedroom", "CH 4", DeviceType.AC, 2),
    SceneRelayOption("Humidifier", "Bedroom", "CH 5", DeviceType.HUMIDIFIER, 2),
    SceneRelayOption("Wardrobe Light", "Bedroom", "CH 6", DeviceType.LIGHT, 2),

    // Kitchen (4 channels) -> Index 3
    SceneRelayOption("Main Ceiling Light", "Kitchen", "CH 1", DeviceType.LIGHT, 3),
    SceneRelayOption("Exhaust Fan", "Kitchen", "CH 2", DeviceType.FAN, 3),
    SceneRelayOption("Cabinet Strip", "Kitchen", "CH 3", DeviceType.LIGHT, 3),
    SceneRelayOption("Water Purifier", "Kitchen", "CH 4", DeviceType.SWITCH, 3),

    // Guestroom (4 channels) -> Index 4
    SceneRelayOption("Guest Light", "Guestroom", "CH 1", DeviceType.LIGHT, 4),
    SceneRelayOption("Guest Fan", "Guestroom", "CH 2", DeviceType.FAN, 4),
    SceneRelayOption("Desk Lamp", "Guestroom", "CH 3", DeviceType.LIGHT, 4),
    SceneRelayOption("Charger Outlet", "Guestroom", "CH 4", DeviceType.SWITCH, 4)
)

@Composable
fun SceneDetailsScreen(
    scene: SceneItem,
    onBackPress: () -> Unit,
    onEditScene: () -> Unit,
    onEditSceneHeader: (newName: String, newIconName: String, newDescription: String) -> Unit = { _, _, _ -> },
    onDeleteScene: () -> Unit,
    onActivateScene: () -> Unit,
    onToggleShowOnHome: (Boolean) -> Unit,
    onToggleSceneRelay: (relayId: String, relayName: String) -> Unit = { _, _ -> },
    onRemoveRelay: (relayId: String, relayName: String) -> Unit = { _, _ -> },
    onAddRelay: (relayName: String, deviceName: String, relayId: String, deviceType: DeviceType) -> Unit = { _, _, _, _ -> },
    availableRelays: List<SceneRelayOption>,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var addRelayMenuExpanded by remember { mutableStateOf(false) }
    var showEditHeaderDialog by remember { mutableStateOf(false) }

    val isDark = isAppInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackPress,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scene Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Control multiple devices with one tap",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                // Top Right Action Buttons (3 Dots Overflow Menu Only)
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Name & Icon") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showEditHeaderDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Scene", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                menuExpanded = false
                                onDeleteScene()
                            }
                        )
                    }
                }
            }

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFC7D2FE))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) Color(0xFF2E2A52) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val (heroBg, heroTint, heroIcon) = when (scene.iconName) {
                                        "Movie" -> Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Default.Movie)
                                        "Bedtime" -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Default.Bedtime)
                                        "AutoAwesome" -> Triple(Color(0xFFDCFCE7), Color(0xFF059669), Icons.Default.AutoAwesome)
                                        "Home" -> Triple(Color(0xFFFFEDD5), Color(0xFFEA580C), Icons.Default.Home)
                                        "Lightbulb" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Lightbulb)
                                        else -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.WbSunny)
                                    }
                                    Icon(
                                        imageVector = heroIcon,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else heroTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = scene.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${scene.devices.size} Devices • Automation",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF4338CA)
                                    )
                                }
                            }

                            if (scene.isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = "Favorite",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = scene.description.ifEmpty { "One tap automation scene for configured room appliances." },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF374151)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Last Used", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (isDark) Color(0xFF94A3B8) else Color.Gray)
                                        Text(scene.lastUsedText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Created", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (isDark) Color(0xFF94A3B8) else Color.Gray)
                                        Text(scene.createdOnText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B))
                                    }
                                }
                            }
                        }

                        // Big Activate Button
                        Button(
                            onClick = onActivateScene,
                            enabled = !scene.isActivating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            if (scene.isActivating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Activating Scene...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activate Scene Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }
                        }
                    }
                }

                // Scene Devices Section Header & Add Relay Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Devices in this Scene (${scene.devices.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF))
                                    .clickable { addRelayMenuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add Relay",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = addRelayMenuExpanded,
                                onDismissRequest = { addRelayMenuExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 280.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (availableRelays.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No relays available", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                                        onClick = { addRelayMenuExpanded = false }
                                    )
                                } else {
                                    availableRelays.forEach { option ->
                                        val alreadyAdded = scene.devices.any { it.relayId == option.relayId && it.relayName == option.relayName }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${option.relayName}, ${option.deviceName} (${option.relayId})${if (alreadyAdded) " ✓" else ""}",
                                                    fontWeight = if (alreadyAdded) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (alreadyAdded) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                addRelayMenuExpanded = false
                                                if (!alreadyAdded) {
                                                    onAddRelay(option.relayName, option.deviceName, option.relayId, option.deviceType)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        if (scene.devices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No relays assigned to this scene yet.\nTap '+ Add Relay' above to add relays.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                scene.devices.forEachIndexed { index, dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                DeviceAnimationDisplay(
                                                    isOn = dev.state == "ON",
                                                    deviceType = dev.deviceType,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${dev.relayName}, ${dev.deviceName}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Relay ${dev.relayId} • State: ${dev.state}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // State Toggle Pill (ON / OFF)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (dev.state == "ON") (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7))
                                                        else (if (isDark) Color(0xFF451212) else Color(0xFFFEE2E2))
                                                    )
                                                    .clickable { onToggleSceneRelay(dev.relayId, dev.relayName) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = dev.state,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (dev.state == "ON") (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else Color(0xFFEF4444)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Remove Relay Button
                                            IconButton(
                                                onClick = { onRemoveRelay(dev.relayId, dev.relayName) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove Relay",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (index < scene.devices.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Show on Dashboard Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Show on Dashboard",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Quick trigger from Home screen",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Switch(
                            checked = scene.showOnHome,
                            onCheckedChange = onToggleShowOnHome,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4F46E5)
                            )
                        )
                    }
                }
            }
        }
    }

    // Top-Level Edit Scene Name, Icon & Description Dialog
    if (showEditHeaderDialog) {
        var editedName by remember { mutableStateOf(scene.name) }
        var editedDescription by remember { mutableStateOf(scene.description) }
        var editedIcon by remember { mutableStateOf(scene.iconName) }
        val availableIcons = mapOf(
            "WbSunny" to Icons.Default.WbSunny,
            "Movie" to Icons.Default.Movie,
            "Bedtime" to Icons.Default.Bedtime,
            "AutoAwesome" to Icons.Default.AutoAwesome,
            "Home" to Icons.Default.Home,
            "Lightbulb" to Icons.Default.Lightbulb
        )

        AlertDialog(
            onDismissRequest = { showEditHeaderDialog = false },
            title = {
                Text("Edit Scene Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Scene Name", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Description", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Icon", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableIcons.forEach { (iconKey, vector) ->
                            val isSelected = editedIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4F46E5) else (if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)))
                                    .clickable { editedIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) Color.White else (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onEditSceneHeader(editedName, editedIcon, editedDescription)
                        }
                        showEditHeaderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHeaderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
