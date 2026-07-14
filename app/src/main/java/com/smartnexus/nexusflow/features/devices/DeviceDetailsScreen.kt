package com.smartnexus.nexusflow.features.devices

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.smartnexus.nexusflow.core.components.DeviceAnimationDisplay
import com.smartnexus.nexusflow.core.components.PowerUsageCard
import com.smartnexus.nexusflow.core.components.SensorMetricCard
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.domain.model.DeviceType

@Composable
fun DeviceDetailsScreen(
    device: DeviceDetailsData,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onRestart: () -> Unit,
    onCheckUpdate: () -> Unit,
    onRemoveDevice: () -> Unit,
    onToggleChannel: (Int) -> Unit,
    onResetBillingCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()
    var expandedChannels by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteInputText by remember { mutableStateOf("") }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF)))
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Device Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "View and control your device",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(cardBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Device & Icon", color = textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = textSecondary, modifier = Modifier.size(22.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Restart Device", color = textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onRestart()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = textSecondary, modifier = Modifier.size(22.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Device", color = Color(0xFFEF4444)) },
                        onClick = {
                            menuExpanded = false
                            deleteInputText = ""
                            showDeleteConfirmDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                        }
                    )
                }
            }
        }

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card (Interactive Icon & Status Visual)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Device Icon Container
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF))
                                    .clickable { onRename() },
                                contentAlignment = Alignment.Center
                            ) {
                                DeviceAnimationDisplay(
                                    isOn = device.isOnline,
                                    deviceType = device.deviceType,
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "${device.channels} Channel Smart Relay",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = textSecondary
                                )
                            }
                        }

                        // Replaced ESP32 Badge with Status Badge (Online / Offline)
                        StatusPill(
                            icon = Icons.Default.CheckCircle,
                            text = if (device.isOnline) "Online" else "Offline",
                            color = if (device.isOnline) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else Color(0xFFEA580C),
                            bgColor = if (device.isOnline) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // System Metrics Grid (Kept 3rd Row)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(icon = Icons.Default.Wifi, label = "Wi-Fi Signal", value = device.wifiSignal, isDark = isDark)
                        MetricItem(icon = Icons.Default.Bluetooth, label = "BLE Status", value = device.bleStatus, isDark = isDark)
                        MetricItem(icon = Icons.Default.Schedule, label = "Last Seen", value = device.lastSeen, isDark = isDark)
                        MetricItem(icon = Icons.Default.AccessTime, label = "Uptime", value = device.uptime, isDark = isDark)
                    }
                }
            }

            // Live Sensors Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Sensors",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Updated just now",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SensorMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Thermostat,
                        label = "Temperature",
                        value = device.temperature,
                        status = "Comfortable",
                        accentColor = Color(0xFFF59E0B),
                        bgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB),
                        isDark = isDark
                    )
                    SensorMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = device.humidity,
                        status = "Normal",
                        accentColor = Color(0xFF0284C7),
                        bgColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF),
                        isDark = isDark
                    )
                    SensorMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SentimentSatisfiedAlt,
                        label = "Humidex",
                        value = device.humidex,
                        status = "Comfortable",
                        accentColor = Color(0xFF10B981),
                        bgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                        isDark = isDark
                    )
                }
            }

            // Power Usage Section (⭐ Estimated Power Usage Card)
            PowerUsageCard(
                energyKwh = device.energyKwh,
                costRs = device.costRs,
                startDate = device.startDate,
                runningFor = device.runningFor,
                lastUpdated = "Just now",
                consumptionSegments = device.consumptionSegments,
                onResetClick = {
                    onResetBillingCycle()
                    Toast.makeText(context, "Billing cycle reset! Starting new cycle from 0 kWh.", Toast.LENGTH_LONG).show()
                },
                onViewDetailsClick = {
                    Toast.makeText(context, "Consumption details per channel", Toast.LENGTH_SHORT).show()
                }
            )

            // Relay Channels Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Relay Channels (${device.relayChannels.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = textPrimary
                        )
                        Text(
                            text = "Tap to toggle state",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val visibleChannels = if (expandedChannels) device.relayChannels else device.relayChannels.take(5)

                    visibleChannels.forEachIndexed { index, channel ->
                        RelayChannelRowItem(
                            channel = channel,
                            onToggle = { onToggleChannel(channel.channelNumber) },
                            isDark = isDark
                        )

                        if (index < visibleChannels.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (device.relayChannels.size > 5) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                .clickable { expandedChannels = !expandedChannels }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (expandedChannels) "Show Less Channels" else "Show Channels 6 – ${device.relayChannels.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (expandedChannels) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Device Technical Info Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoGridRow(icon = Icons.Default.Info, label = "Hardware ID", value = device.hardwareId, isDark = isDark)
                            InfoGridRow(icon = Icons.Default.Wifi, label = "MAC Address", value = device.macAddress, isDark = isDark)
                            InfoGridRow(icon = Icons.Default.DeveloperBoard, label = "Firmware", value = device.firmware, isDark = isDark)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoGridRow(icon = Icons.Default.Info, label = "Device Type", value = device.deviceTypeLabel, isDark = isDark)
                            InfoGridRow(icon = Icons.Default.Schedule, label = "Added On", value = device.addedDate, isDark = isDark)
                            InfoGridRow(icon = Icons.Default.AccessTime, label = "Time Zone", value = device.timeZone, isDark = isDark)
                        }
                    }
                }
            }

            // Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButtonCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Edit,
                    label = "Rename",
                    bgColor = if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF),
                    contentColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    onClick = onRename
                )
                ActionButtonCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Refresh,
                    label = "Restart",
                    bgColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFE0F2FE),
                    contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    onClick = onRestart
                )
                ActionButtonCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudDownload,
                    label = "Update",
                    bgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5),
                    contentColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                    onClick = onCheckUpdate
                )
                ActionButtonCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Remove",
                    bgColor = if (isDark) Color(0xFF451212) else Color(0xFFFEE2E2),
                    contentColor = Color(0xFFEF4444),
                    onClick = {
                        deleteInputText = ""
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }

    // Strict Red Confirmation Dialog for Device Deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete ${device.name}?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Warning: Removing this device will permanently delete all its associated relay channels, schedules, and automation triggers. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "To confirm, type \"${device.name}\" below:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = deleteInputText,
                        onValueChange = { deleteInputText = it },
                        placeholder = { Text(device.name) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onRemoveDevice()
                    },
                    enabled = deleteInputText.trim() == device.name.trim(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.4f)
                    )
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun StatusPill(
    icon: ImageVector,
    text: String,
    color: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = color
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        )
    }
}



@Composable
private fun RelayChannelRowItem(
    channel: RelayChannelItem,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (channel.isSystemOn) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Lottie Relay Display
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (channel.isSystemOn) (if (isDark) Color(0xFF2E2A52) else Color(0xFFE0E7FF)) else (if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0))),
                contentAlignment = Alignment.Center
            ) {
                DeviceAnimationDisplay(
                    isOn = channel.isSystemOn,
                    deviceType = channel.deviceType,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = channel.channelLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }
                Text(
                    text = if (channel.isSystemOn) "Status: Active" else "Status: Inactive",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (channel.isSystemOn) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (channel.isSystemOn) {
                            if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                        } else {
                            if (isDark) Color(0xFF374151) else Color(0xFFF1F5F9)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (channel.isSystemOn) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = if (channel.isSystemOn) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoGridRow(
    icon: ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun ActionButtonCard(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = contentColor
            )
        }
    }
}

@Preview(name = "Device Details - Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DeviceDetailsLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        Surface {
            DeviceDetailsScreen(
                device = DeviceDetailsData(
                    id = "nf_lr8",
                    name = "Living Room",
                    channels = 8,
                    isOnline = true,
                    wifiConnected = true,
                    bleNearby = true,
                    deviceType = DeviceType.LIVING_ROOM,
                    relayChannels = listOf(
                        RelayChannelItem(1, "CH 1", "Living Light", true, DeviceType.LIGHT),
                        RelayChannelItem(2, "CH 2", "Ceiling Fan", true, DeviceType.FAN),
                        RelayChannelItem(3, "CH 3", "TV Unit", false, DeviceType.TV)
                    )
                ),
                onBack = {},
                onRename = {},
                onRestart = {},
                onCheckUpdate = {},
                onRemoveDevice = {},
                onToggleChannel = {},
                onResetBillingCycle = {}
            )
        }
    }
}

@Preview(name = "Device Details - Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeviceDetailsDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface {
            DeviceDetailsScreen(
                device = DeviceDetailsData(
                    id = "nf_lr8",
                    name = "Living Room",
                    channels = 8,
                    isOnline = true,
                    wifiConnected = true,
                    bleNearby = true,
                    deviceType = DeviceType.LIVING_ROOM,
                    relayChannels = listOf(
                        RelayChannelItem(1, "CH 1", "Living Light", true, DeviceType.LIGHT),
                        RelayChannelItem(2, "CH 2", "Ceiling Fan", true, DeviceType.FAN),
                        RelayChannelItem(3, "CH 3", "TV Unit", false, DeviceType.TV)
                    )
                ),
                onBack = {},
                onRename = {},
                onRestart = {},
                onCheckUpdate = {},
                onRemoveDevice = {},
                onToggleChannel = {},
                onResetBillingCycle = {}
            )
        }
    }
}
