package com.smartnexus.nexusflow.features.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.components.AutomationRuleAddEditSheet
import com.smartnexus.nexusflow.core.components.EmptyState
import com.smartnexus.nexusflow.core.components.RelayNameEditSheet
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.components.DevDebugConfig
import com.smartnexus.nexusflow.features.home.components.AutomationRuleItemData
import com.smartnexus.nexusflow.features.home.components.AutomationRulesSection
import com.smartnexus.nexusflow.core.components.DevicePickerBar
import com.smartnexus.nexusflow.features.home.components.RelayGrid
import com.smartnexus.nexusflow.features.home.components.RelayItemData
import com.smartnexus.nexusflow.features.home.components.SensorReadingsRow
import com.smartnexus.nexusflow.core.navigation.Screen

@Composable
fun HomeScreen(
    onNavigateToTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceCount by DevDebugConfig.deviceCount.collectAsState()
    val bleConnected by DevDebugConfig.bleConnected.collectAsState()
    val wifiOnline by DevDebugConfig.wifiOnline.collectAsState()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            deviceCount = deviceCount,
            bleConnected = bleConnected,
            wifiOnline = wifiOnline,
            onRoomSelected = viewModel::onRoomSelected,
            onToggleRelay = viewModel::onToggleRelay,
            onToggleRulesExpand = viewModel::onToggleRulesExpand,
            onToggleRule = viewModel::onToggleRule,
            onRelayClick = viewModel::onOpenRelayEdit,
            onEditRuleClick = viewModel::onOpenEditRule,
            onAddRuleClick = viewModel::onOpenAddRule,
            onNavigateToTab = onNavigateToTab,
            modifier = Modifier.fillMaxSize()
        )

        // Relay Name Edit Sheet
        if (uiState.selectedRelayForEdit != null) {
            val target = uiState.selectedRelayForEdit!!
            RelayNameEditSheet(
                initialName = target.name,
                initialDeviceType = target.deviceType,
                initialPowerWatts = target.powerWatts,
                onDismiss = viewModel::onCloseRelayEdit,
                onSave = { newName, newDeviceType, powerWatts ->
                    viewModel.onSaveRelayConfig(target.id, newName, newDeviceType, powerWatts)
                    Toast.makeText(context, "Relay updated with ${powerWatts}W rating", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Automation Rule Add / Edit Sheet (directly opened on click / long press)
        if (uiState.showAddEditRuleSheet) {
            AutomationRuleAddEditSheet(
                initialRule = uiState.selectedRuleForEdit,
                availableRelays = uiState.relays,
                existingRules = uiState.automationRules,
                onDismiss = viewModel::onCloseAddEditRule,
                onSaveRule = { savedRule ->
                    viewModel.onSaveRule(savedRule)
                    Toast.makeText(context, "Automation rule saved successfully", Toast.LENGTH_SHORT).show()
                },
                onDeleteRule = { ruleId ->
                    viewModel.onDeleteRule(ruleId)
                    Toast.makeText(context, "Automation rule deleted successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    deviceCount: Int,
    bleConnected: Boolean,
    wifiOnline: Boolean,
    onRoomSelected: (String) -> Unit,
    onToggleRelay: (String, Boolean) -> Unit,
    onToggleRulesExpand: () -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onRelayClick: (RelayItemData) -> Unit,
    onEditRuleClick: (AutomationRuleItemData) -> Unit,
    onAddRuleClick: () -> Unit = {},
    onNavigateToTab: (Screen) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

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
            // Top Bar Header (Room Selector + Avatar + Connection Indicators)
            DevicePickerBar(
                selectedRoom = uiState.selectedRoom,
                rooms = uiState.rooms,
                onRoomSelected = onRoomSelected,
                bleConnected = bleConnected,
                wifiOnline = wifiOnline
            )

            if (uiState.rooms.isEmpty()) {
                // Empty state when 0 devices are configured
                EmptyState(
                    icon = Icons.Default.Devices,
                    title = "No Devices Found",
                    description = "You haven't paired any NexusFlow smart relays yet. Tap below to pair your first device.",
                    buttonText = "Add Device",
                    onButtonClick = { onNavigateToTab(Screen.Devices) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Scrollable Dashboard Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Action Navigation Bar (Scenes & Schedules Pill Buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToTab(Screen.Scenes) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scenes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigateToTab(Screen.Schedules) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Schedules",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Relay Controls Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Relay Controls",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val onCount = uiState.relays.count { it.isOn }
                        val offCount = uiState.relays.size - onCount
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$onCount On",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "$offCount Off",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Relay Grid - 3x2+2 layout for 8 channels
                    RelayGrid(
                        relays = uiState.relays,
                        onToggleRelay = onToggleRelay,
                        onRelayClick = onRelayClick,
                        layoutType = if (uiState.relays.size == 8)
                            com.smartnexus.nexusflow.features.home.components.RelayLayoutType.THREE_BY_TWO_PLUS_TWO
                        else
                            com.smartnexus.nexusflow.features.home.components.RelayLayoutType.AUTO
                    )

                    // Environmental Sensor Readouts (Temperature, Humidity, Humidex)
                    SensorReadingsRow(
                        sensors = uiState.sensors,
                        lastUpdatedText = "Updated 5s ago"
                    )

                    // Automation Rules Section
                    AutomationRulesSection(
                        rules = uiState.automationRules,
                        isExpanded = uiState.isRulesExpanded,
                        onToggleExpand = onToggleRulesExpand,
                        onToggleRule = onToggleRule,
                        onEditRuleClick = onEditRuleClick,
                        onAddRuleClick = onAddRuleClick
                    )

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface {
            HomeContent(
                uiState = HomeUiState(),
                deviceCount = 2,
                bleConnected = true,
                wifiOnline = true,
                onRoomSelected = {},
                onToggleRelay = { _, _ -> },
                onToggleRulesExpand = {},
                onToggleRule = { _, _ -> },
                onRelayClick = {},
                onEditRuleClick = {},
                onAddRuleClick = {},
                onNavigateToTab = {}
            )
        }
    }
}
