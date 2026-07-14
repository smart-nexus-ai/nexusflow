package com.smartnexus.nexusflow.features.devices

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.components.DevDebugConfig
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.features.devices.components.DeviceCard
import com.smartnexus.nexusflow.features.devices.components.DeviceContextMenuSheet
import com.smartnexus.nexusflow.features.devices.components.DeviceNameEditSheet

@Composable
fun DevicesScreen(
    onNavigateToAddDevice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debugDeviceCount by DevDebugConfig.deviceCount.collectAsState()
    val context = LocalContext.current

    var deletingDevice by remember { mutableStateOf<DeviceListItem?>(null) }
    var deleteConfirmInput by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf("Last Used") }

    val effectiveDevices = if (debugDeviceCount == 0) emptyList() else uiState.devices
    val activeDetails = uiState.activeDetailsDevice

    // If a device details screen is open, render DeviceDetailsScreen
    if (activeDetails != null) {
        BackHandler { viewModel.onCloseDeviceDetails() }
        val currentDeviceListItem = effectiveDevices.firstOrNull { it.id == activeDetails.id }
        DeviceDetailsScreen(
            device = activeDetails,
            onBack = viewModel::onCloseDeviceDetails,
            onRename = {
                currentDeviceListItem?.let { viewModel.onOpenEditDevice(it) }
            },
            onRestart = {
                Toast.makeText(context, "Restarting ${activeDetails.name}...", Toast.LENGTH_SHORT).show()
            },
            onCheckUpdate = {
                Toast.makeText(context, "${activeDetails.name} is up to date", Toast.LENGTH_SHORT).show()
            },
            onRemoveDevice = {
                viewModel.onDeleteDevice(activeDetails.id)
                viewModel.onCloseDeviceDetails()
                Toast.makeText(context, "Device removed", Toast.LENGTH_SHORT).show()
            },
            onToggleChannel = viewModel::onToggleRelayChannel,
            onResetBillingCycle = { viewModel.onResetBillingCycle(activeDetails.id) },
            modifier = modifier
        )

        // Render edit sheet over details screen if active
        val editingDev = uiState.editingDevice
        if (editingDev != null) {
            DeviceNameEditSheet(
                initialName = editingDev.name,
                initialDeviceType = editingDev.deviceType,
                onDismiss = viewModel::onCloseEditDevice,
                onSave = { newName, newType ->
                    viewModel.onSaveDeviceChanges(editingDev.id, newName, newType)
                    Toast.makeText(context, "Device updated", Toast.LENGTH_SHORT).show()
                }
            )
        }
        return
    }

    DevicesScreenContent(
        uiState = uiState,
        effectiveDevices = effectiveDevices,
        selectedSortOption = selectedSortOption,
        onSortOptionSelected = { selectedSortOption = it },
        onNavigateToAddDevice = onNavigateToAddDevice,
        onOpenDeviceDetails = viewModel::onOpenDeviceDetails,
        onOpenContextMenu = viewModel::onOpenContextMenu,
        modifier = modifier
    )

    // Context Menu Bottom Sheet
    val menuDevice = uiState.selectedDeviceForMenu
    if (menuDevice != null) {
        DeviceContextMenuSheet(
            device = menuDevice,
            onDismiss = viewModel::onCloseContextMenu,
            onOpenDetails = {
                viewModel.onCloseContextMenu()
                viewModel.onOpenDeviceDetails(menuDevice.details)
            },
            onRename = {
                viewModel.onOpenEditDevice(menuDevice)
            },
            onDelete = {
                val target = menuDevice
                viewModel.onCloseContextMenu()
                deleteConfirmInput = ""
                deletingDevice = target
            }
        )
    }

    // Strict Red Confirmation Dialog for Device Deletion from List
    val devToDelete = deletingDevice
    if (devToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingDevice = null },
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
                    text = "Delete ${devToDelete.name}?",
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
                        text = "To confirm, type \"${devToDelete.name}\" below:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it },
                        placeholder = { Text(devToDelete.name) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteDevice(devToDelete.id)
                        deletingDevice = null
                        Toast.makeText(context, "Device removed", Toast.LENGTH_SHORT).show()
                    },
                    enabled = deleteConfirmInput.trim() == devToDelete.name.trim(),
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
                    onClick = { deletingDevice = null }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Device Rename & Icon Edit Sheet
    val editingDev = uiState.editingDevice
    if (editingDev != null) {
        DeviceNameEditSheet(
            initialName = editingDev.name,
            initialDeviceType = editingDev.deviceType,
            onDismiss = viewModel::onCloseEditDevice,
            onSave = { newName, newType ->
                viewModel.onSaveDeviceChanges(editingDev.id, newName, newType)
                Toast.makeText(context, "Device updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun DevicesScreenContent(
    uiState: DevicesUiState,
    effectiveDevices: List<DeviceListItem>,
    selectedSortOption: String,
    onSortOptionSelected: (String) -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onOpenDeviceDetails: (DeviceDetailsData) -> Unit,
    onOpenContextMenu: (DeviceListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val connectedCount = effectiveDevices.count { it.isOnline }
    val totalCount = effectiveDevices.size
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val sortedDevices = when (selectedSortOption) {
        "A-Z" -> effectiveDevices.sortedBy { it.name }
        "Z-A" -> effectiveDevices.sortedByDescending { it.name }
        "Last Seen" -> effectiveDevices.sortedBy { it.details.lastSeen }
        else -> effectiveDevices // "Last Used"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar Header (Removed Search Button, Updated Filter Menu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Devices",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage your smart relay devices",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { filterMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sort Devices",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf("Last Used", "A-Z", "Z-A", "Last Seen").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontWeight = if (selectedSortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortOptionSelected(option)
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Stats Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFC7D2FE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$connectedCount / $totalCount Active",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (connectedCount > 0) "All systems operational" else "No devices online",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF4338CA)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Wi-Fi",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BLE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Devices List
            if (sortedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Devices Added Yet",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pair your ESP32 NexusFlow smart relay device via Wi-Fi or BLE to start controlling appliances.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToAddDevice,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pair New Device", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(sortedDevices, key = { it.id }) { device ->
                        DeviceCard(
                            item = device,
                            onClickCard = { onOpenDeviceDetails(device.details) },
                            onOpenMenu = { onOpenContextMenu(device) }
                        )
                    }

                    item {
                        // Add New Device Dashed Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onNavigateToAddDevice() }
                                .drawWithContent {
                                    drawContent()
                                    drawRoundRect(
                                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        style = Stroke(
                                            width = 3f,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(16f, 12f),
                                                0f
                                            )
                                        )
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFAF5FF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pair Additional NexusFlow Device",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
