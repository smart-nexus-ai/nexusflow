package com.smartnexus.nexusflow.features.schedules

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.components.EmptyState
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.features.schedules.components.ScheduleCardItem
import com.smartnexus.nexusflow.core.navigation.Screen

@Composable
fun SchedulesScreen(
    onNavigateToTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchedulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.hasDevices) {
        EmptyState(
            icon = Icons.Default.Devices,
            title = "No Devices Connected",
            description = "You need to pair a NexusFlow smart relay device first before you can schedule timers.",
            buttonText = "Add Device",
            onButtonClick = { onNavigateToTab(Screen.Devices) },
            modifier = modifier.fillMaxSize()
        )
    } else if (uiState.isAddEditSheetOpen) {
        val existingScheduledRelays = uiState.schedules.map { "${it.relayName}, ${it.deviceName}" }.toSet()
        ScheduleAddEditScreen(
            initialData = uiState.editingSchedule,
            existingScheduledRelays = existingScheduledRelays,
            availableRelays = uiState.availableRelays,
            onDismiss = viewModel::onCloseAddEditSheet,
            onSave = viewModel::onSaveSchedule,
            modifier = modifier
        )
    } else {
        SchedulesScreenContent(
            uiState = uiState,
            effectiveSchedules = uiState.schedules,
            onOpenAddSchedule = viewModel::onOpenAddSchedule,
            onFilterChanged = viewModel::onFilterChanged,
            onToggleSchedule = viewModel::onToggleSchedule,
            onEditSchedule = viewModel::onOpenEditSchedule,
            onDeleteSchedule = viewModel::onDeleteSchedule,
            modifier = modifier
        )
    }
}

@Composable
fun SchedulesScreenContent(
    uiState: SchedulesUiState,
    effectiveSchedules: List<ScheduleItem>,
    onOpenAddSchedule: () -> Unit,
    onFilterChanged: (String) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var selectedDeviceFilters by remember { mutableStateOf<Set<String>>(emptySet()) } // Empty = ALL

    val filteredSchedules = effectiveSchedules.filter { item ->
        val matchesDevice = selectedDeviceFilters.isEmpty() || selectedDeviceFilters.contains(item.deviceName)
        val matchesTab = when (uiState.selectedFilter) {
            "active" -> item.isEnabled
            "inactive" -> !item.isEnabled
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            item.relayName.contains(searchQuery, ignoreCase = true) ||
            item.deviceName.contains(searchQuery, ignoreCase = true) ||
            item.pattern.contains(searchQuery, ignoreCase = true) ||
            item.nextTrigger.contains(searchQuery, ignoreCase = true)

        matchesDevice && matchesTab && matchesSearch
    }

    val activeCount = effectiveSchedules.count { it.isEnabled }
    val inactiveCount = effectiveSchedules.count { !it.isEnabled }

    fun handleAddScheduleClick() {
        val existingSet = effectiveSchedules.map { "${it.relayName}, ${it.deviceName}" }.toSet()
        val unusedRelays = uiState.availableRelays.filterNot { option ->
            existingSet.contains("${option.first}, ${option.second}")
        }

        if (unusedRelays.isEmpty()) {
            Toast.makeText(context, "Schedule capacity reached: All available relays are already scheduled", Toast.LENGTH_SHORT).show()
        } else {
            onOpenAddSchedule()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row (Standardized Position & Padding matching Devices/Scenes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Schedules",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage automated device timings",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSearchActive) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)))
                            .clickable {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Multi-Device Filter Button (Select Multiple Devices)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selectedDeviceFilters.isNotEmpty()) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)))
                                .clickable { filterMenuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter Devices",
                                tint = if (selectedDeviceFilters.isNotEmpty()) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Filter by Devices",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedDeviceFilters.isEmpty(),
                                            onCheckedChange = { selectedDeviceFilters = emptySet() }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("All Devices", fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = { selectedDeviceFilters = emptySet() }
                            )

                            listOf("Living Room", "Bedroom", "Kitchen", "Guestroom").forEach { deviceName ->
                                val isChecked = selectedDeviceFilters.contains(deviceName)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedDeviceFilters = if (checked) selectedDeviceFilters + deviceName else selectedDeviceFilters - deviceName
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(deviceName)
                                        }
                                    },
                                    onClick = {
                                        selectedDeviceFilters = if (isChecked) selectedDeviceFilters - deviceName else selectedDeviceFilters + deviceName
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar Input (Shown when Search Button Active)
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by relay, device or time...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Filter Pills Row (All Schedules, Active, Inactive)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: All Schedules
                item {
                    val isSelected = uiState.selectedFilter == "all"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("all") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All (${effectiveSchedules.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 2: Active
                item {
                    val isSelected = uiState.selectedFilter == "active"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("active") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Active ($activeCount)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 3: Inactive
                item {
                    val isSelected = uiState.selectedFilter == "inactive"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("inactive") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF451212) else Color(0xFFFEE2E2)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFEF4444) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFEF4444) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Inactive ($inactiveCount)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) Color(0xFFEF4444) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Schedules List
            if (filteredSchedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "No Schedules Found",
                        description = "Automate your smart relays by creating time-based schedules",
                        buttonText = "Add Schedule",
                        onButtonClick = { handleAddScheduleClick() }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        ScheduleCardItem(
                            item = schedule,
                            onToggle = { isChecked -> onToggleSchedule(schedule.id, isChecked) },
                            onEdit = { onEditSchedule(schedule) },
                            onDelete = { onDeleteSchedule(schedule.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { handleAddScheduleClick() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            containerColor = Color(0xFF4F46E5),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Schedule"
            )
        }
    }
}
