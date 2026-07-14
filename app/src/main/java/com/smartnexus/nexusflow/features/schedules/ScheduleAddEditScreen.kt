package com.smartnexus.nexusflow.features.schedules

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.components.DevDebugConfig
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.domain.model.DeviceType

val all22ScheduleRelays = listOf(
    // Living Room (8 channels) -> Index 1
    Triple("Living Light", "Living Room", Pair("CH 1", 1)),
    Triple("Ceiling Fan", "Living Room", Pair("CH 2", 1)),
    Triple("TV Unit", "Living Room", Pair("CH 3", 1)),
    Triple("LED Strip", "Living Room", Pair("CH 4", 1)),
    Triple("Spare Switch", "Living Room", Pair("CH 5", 1)),
    Triple("Balcony Light", "Living Room", Pair("CH 6", 1)),
    Triple("Air Purifier", "Living Room", Pair("CH 7", 1)),
    Triple("Accent Lamp", "Living Room", Pair("CH 8", 1)),

    // Bedroom (6 channels) -> Index 2
    Triple("Bedroom Light", "Bedroom", Pair("CH 1", 2)),
    Triple("Nightstand Lamp", "Bedroom", Pair("CH 2", 2)),
    Triple("Bedroom Fan", "Bedroom", Pair("CH 3", 2)),
    Triple("Air Conditioner", "Bedroom", Pair("CH 4", 2)),
    Triple("Humidifier", "Bedroom", Pair("CH 5", 2)),
    Triple("Wardrobe Light", "Bedroom", Pair("CH 6", 2)),

    // Kitchen (4 channels) -> Index 3
    Triple("Main Ceiling Light", "Kitchen", Pair("CH 1", 3)),
    Triple("Exhaust Fan", "Kitchen", Pair("CH 2", 3)),
    Triple("Cabinet Strip", "Kitchen", Pair("CH 3", 3)),
    Triple("Water Purifier", "Kitchen", Pair("CH 4", 3)),

    // Guestroom (4 channels) -> Index 4
    Triple("Guest Light", "Guestroom", Pair("CH 1", 4)),
    Triple("Guest Fan", "Guestroom", Pair("CH 2", 4)),
    Triple("Desk Lamp", "Guestroom", Pair("CH 3", 4)),
    Triple("Charger Outlet", "Guestroom", Pair("CH 4", 4))
)

@Composable
fun ScheduleAddEditScreen(
    initialData: AddEditScheduleData?,
    existingScheduledRelays: Set<String> = emptySet(),
    availableRelays: List<Triple<String, String, Pair<String, Int>>>,
    onDismiss: () -> Unit,
    onSave: (AddEditScheduleData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    // Pick 1st unused relay by default when adding a new schedule
    val unusedRelay = availableRelays.firstOrNull { option ->
        val key = "${option.first}, ${option.second}"
        !existingScheduledRelays.contains(key)
    } ?: availableRelays.firstOrNull()

    val initialAction = when (initialData?.action) {
        "OFF", "TURN OFF" -> "TURN OFF"
        else -> "TURN ON"
    }

    val initialScheduleType = when (initialData?.scheduleType) {
        "Duration", "Duration Window" -> "Duration Window"
        else -> "Specific Time"
    }

    val initialRelayName = initialData?.relayName?.takeIf { it.isNotEmpty() } ?: unusedRelay?.first ?: "Living Light"
    val initialDeviceName = initialData?.deviceName?.takeIf { it.isNotEmpty() } ?: unusedRelay?.second ?: "Living Room"
    val initialRelayId = initialData?.relayId?.takeIf { it.isNotEmpty() } ?: unusedRelay?.third?.first ?: "CH 1"
    val initialStartTime24 = initialData?.startTime ?: initialData?.time ?: "07:00"
    val initialEndTime24 = initialData?.endTime ?: "17:00"
    val initialSelectedDays = initialData?.daysOfWeek?.toList() ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val initialSelectedRepeat = initialData?.repeatPattern ?: "Every weekday (Mon - Fri)"
    val initialNotifyWhenExecuted = initialData?.notifyWhenExecuted ?: true
    val initialEnableSchedule = initialData?.isEnabled ?: true
    val initialSetEndDate = initialData?.hasEndDate ?: false
    val initialSelectedEndDateText = initialData?.endDate ?: "Jul 15, 2026"
    val initialDeviceType = initialData?.deviceType ?: DeviceType.LIGHT

    var selectedRelayName by remember(initialRelayName) { mutableStateOf(initialRelayName) }
    var selectedDeviceName by remember(initialDeviceName) { mutableStateOf(initialDeviceName) }
    var selectedRelayId by remember(initialRelayId) { mutableStateOf(initialRelayId) }
    var selectedDeviceType by remember(initialDeviceType) { mutableStateOf(initialDeviceType) }

    var selectedAction by remember { mutableStateOf(initialAction) }
    var selectedScheduleType by remember { mutableStateOf(initialScheduleType) }
    var startTime24 by remember { mutableStateOf(initialStartTime24) }
    var endTime24 by remember { mutableStateOf(initialEndTime24) }

    var selectedDays by remember { mutableStateOf(initialSelectedDays) }
    var selectedRepeat by remember { mutableStateOf(initialSelectedRepeat) }

    var notifyWhenExecuted by remember { mutableStateOf(initialNotifyWhenExecuted) }
    var enableSchedule by remember { mutableStateOf(initialEnableSchedule) }
    var setEndDate by remember { mutableStateOf(initialSetEndDate) }
    var selectedEndDateText by remember { mutableStateOf(initialSelectedEndDateText) }

    var endDatePickerExpanded by remember { mutableStateOf(false) }
    var deviceDropdownExpanded by remember { mutableStateOf(false) }
    var repeatDropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDiscardDialog by remember { mutableStateOf(false) }

    val repeatOptions = listOf(
        "Every weekday (Mon - Fri)",
        "Every day",
        "Custom days",
        "Once"
    )

    val endDateOptions = listOf(
        "In 1 Week (Jul 10, 2026)",
        "In 2 Weeks (Jul 17, 2026)",
        "In 1 Month (Aug 3, 2026)",
        "In 3 Months (Oct 3, 2026)",
        "End of Month (Jul 31, 2026)",
        "End of Year (Dec 31, 2026)"
    )

    // Calculate if form has unsaved modifications
    val isDirty = initialData?.id == null ||
        selectedRelayName != initialRelayName ||
        selectedDeviceName != initialDeviceName ||
        selectedRelayId != initialRelayId ||
        selectedAction != initialAction ||
        selectedScheduleType != initialScheduleType ||
        startTime24 != initialStartTime24 ||
        endTime24 != initialEndTime24 ||
        selectedDays != initialSelectedDays ||
        selectedRepeat != initialSelectedRepeat ||
        notifyWhenExecuted != initialNotifyWhenExecuted ||
        enableSchedule != initialEnableSchedule ||
        setEndDate != initialSetEndDate ||
        (setEndDate && selectedEndDateText != initialSelectedEndDateText)

    fun handleSave() {
        val scheduleData = AddEditScheduleData(
            id = initialData?.id,
            relayName = selectedRelayName,
            deviceName = selectedDeviceName,
            relayId = selectedRelayId,
            action = selectedAction,
            scheduleType = selectedScheduleType,
            time = startTime24,
            startTime = startTime24,
            endTime = endTime24,
            daysOfWeek = selectedDays.toSet(),
            repeatPattern = selectedRepeat,
            notifyWhenExecuted = notifyWhenExecuted,
            isEnabled = enableSchedule,
            hasEndDate = setEndDate,
            endDate = if (setEndDate) selectedEndDateText else null,
            deviceType = selectedDeviceType
        )
        onSave(scheduleData)
        onDismiss()
    }

    fun handleBackPress() {
        if (isDirty && initialData != null) {
            showConfirmDiscardDialog = true
        } else if (initialData == null && (selectedRelayName != initialRelayName || startTime24 != "07:00")) {
            showConfirmDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler {
        handleBackPress()
    }

    fun openStartTimePicker() {
        val initialHour = startTime24.substringBefore(":").toIntOrNull() ?: 7
        val initialMinute = startTime24.substringAfter(":").toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute ->
                startTime24 = String.format("%02d:%02d", hour, minute)
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    fun openEndTimePicker() {
        val initialHour = endTime24.substringBefore(":").toIntOrNull() ?: 17
        val initialMinute = endTime24.substringAfter(":").toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute ->
                endTime24 = String.format("%02d:%02d", hour, minute)
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { handleBackPress() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (initialData?.id == null) "Add Schedule" else "Edit Schedule",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Create automated timer or schedule",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }

                // Single ✓ Save Icon in Top Bar
                IconButton(
                    onClick = { handleSave() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save Schedule",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Step 1: Select Device & Relay (1 Schedule Per Device Limit)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "👤  1. Select Device (1 Schedule Max per Relay)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deviceDropdownExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Devices,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "$selectedRelayName, $selectedDeviceName",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = textPrimary
                                        )
                                        Text(
                                            text = selectedRelayId,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = textSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textSecondary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = deviceDropdownExpanded,
                            onDismissRequest = { deviceDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .heightIn(max = 280.dp)
                                .background(cardBg)
                        ) {
                            if (availableRelays.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No relays available (0 devices connected)", color = textSecondary) },
                                    onClick = { deviceDropdownExpanded = false }
                                )
                            } else {
                                availableRelays.forEach { option ->
                                    val relayKey = "${option.first}, ${option.second}"
                                    val isAlreadyScheduled = existingScheduledRelays.contains(relayKey) && (initialData == null || "${initialData.relayName}, ${initialData.deviceName}" != relayKey)

                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = relayKey,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isAlreadyScheduled) Color(0xFFEF4444) else textPrimary
                                                )
                                                Text(
                                                    text = if (isAlreadyScheduled) "${option.third.first} • Already Scheduled (Unavailable)" else option.third.first,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isAlreadyScheduled) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    color = if (isAlreadyScheduled) Color(0xFFEF4444).copy(alpha = 0.85f) else textSecondary
                                                )
                                            }
                                        },
                                        enabled = !isAlreadyScheduled,
                                        onClick = {
                                            if (!isAlreadyScheduled) {
                                                selectedRelayName = option.first
                                                selectedDeviceName = option.second
                                                selectedRelayId = option.third.first
                                                selectedDeviceType = when (option.first) {
                                                    "Living Light", "LED Strip", "Balcony Light", "Accent Lamp", "Bedroom Light", "Nightstand Lamp", "Wardrobe Light", "Main Ceiling Light", "Cabinet Strip", "Guest Light", "Desk Lamp" -> DeviceType.LIGHT
                                                    "Ceiling Fan", "Bedroom Fan", "Exhaust Fan", "Guest Fan" -> DeviceType.FAN
                                                    "Air Conditioner" -> DeviceType.AC
                                                    "TV Unit" -> DeviceType.TV
                                                    "Air Purifier", "Humidifier" -> DeviceType.HUMIDIFIER
                                                    else -> DeviceType.SWITCH
                                                }
                                                deviceDropdownExpanded = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: Select Action (TURN ON / TURN OFF - Preselected)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚡  2. Action",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isOnSelected = selectedAction == "TURN ON"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAction = "TURN ON" },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOnSelected) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else cardBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isOnSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isOnSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TURN ON",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isOnSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else textPrimary
                                )
                            }
                        }

                        val isOffSelected = selectedAction == "TURN OFF"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAction = "TURN OFF" },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOffSelected) (if (isDark) Color(0xFF451212) else Color(0xFFFEE2E2)) else cardBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isOffSelected) Color(0xFFEF4444) else cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isOffSelected) Color(0xFFEF4444) else textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TURN OFF",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isOffSelected) Color(0xFFEF4444) else textPrimary
                                )
                            }
                        }
                    }
                }

                // Step 3: Trigger Type (Specific Time vs Duration Window - Preselected)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⏰  3. Trigger Type",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isSpecific = selectedScheduleType == "Specific Time"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedScheduleType = "Specific Time" },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSpecific) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else cardBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSpecific) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else cardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isSpecific) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Specific Time",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSpecific) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else textPrimary
                                )
                            }
                        }

                        val isDuration = selectedScheduleType == "Duration Window"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedScheduleType = "Duration Window" },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDuration) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else cardBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isDuration) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else cardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (isDuration) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Duration Window",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDuration) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else textPrimary
                                )
                            }
                        }
                    }
                }

                // Step 4: Time Selection
                if (selectedScheduleType == "Specific Time") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🕒  4. Execution Time",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textPrimary
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openStartTimePicker() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Scheduled Time", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = textSecondary)
                                        Text(
                                            text = startTime24,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                                            color = textPrimary
                                        )
                                    }
                                }
                                Text("Change", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5))
                            }
                        }
                    }
                } else {
                    // Duration Window
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🕒  4. Time Window",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openStartTimePicker() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Start Time", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = startTime24,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                        color = textPrimary
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openEndTimePicker() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("End Time", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = endTime24,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 5: Repeat Pattern & Days Selection
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📅  5. Repeat & Days",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { repeatDropdownExpanded = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectedRepeat,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = textPrimary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textSecondary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = repeatDropdownExpanded,
                            onDismissRequest = { repeatDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .background(cardBg)
                        ) {
                            repeatOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = textPrimary) },
                                    onClick = {
                                        selectedRepeat = option
                                        when (option) {
                                            "Every weekday (Mon - Fri)" -> selectedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                                            "Every day" -> selectedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                            "Once" -> selectedDays = emptyList()
                                        }
                                        repeatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Days of week multi-select pills (Single Row)
                    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allDays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF4F46E5) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                                    )
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            selectedDays - day
                                        } else {
                                            selectedDays + day
                                        }
                                        if (selectedDays.size == 7) selectedRepeat = "Every day"
                                        else if (selectedDays == listOf("Mon", "Tue", "Wed", "Thu", "Fri")) selectedRepeat = "Every weekday (Mon - Fri)"
                                        else if (selectedDays.isEmpty()) selectedRepeat = "Once"
                                        else selectedRepeat = "Custom days"
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = if (isSelected) Color.White else textPrimary
                                )
                            }
                        }
                    }
                }

                // Step 6: Advanced Options Toggles
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚙️  6. Advanced Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textPrimary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // Notify Execution
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Execution Notification", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                                        Text("Send phone alert when triggered", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                                    }
                                }
                                Switch(
                                    checked = notifyWhenExecuted,
                                    onCheckedChange = { notifyWhenExecuted = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                                )
                            }

                            // Enable Schedule
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Enable Schedule", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                                        Text("Schedule will be active immediately", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                                    }
                                }
                                Switch(
                                    checked = enableSchedule,
                                    onCheckedChange = { enableSchedule = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                                )
                            }

                            // End Date Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Set End Date", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                                        Text("Automatically stop schedule after date", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                                    }
                                }
                                Switch(
                                    checked = setEndDate,
                                    onCheckedChange = { setEndDate = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4F46E5))
                                )
                            }

                            if (setEndDate) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { endDatePickerExpanded = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("End Date: $selectedEndDateText", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textPrimary)
                                            Text("Change", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = endDatePickerExpanded,
                                        onDismissRequest = { endDatePickerExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(cardBg)
                                    ) {
                                        endDateOptions.forEach { dateText ->
                                            DropdownMenuItem(
                                                text = { Text(dateText, color = textPrimary) },
                                                onClick = {
                                                    selectedEndDateText = dateText
                                                    endDatePickerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Unsaved Changes
    if (showConfirmDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDiscardDialog = false },
            title = {
                Text(
                    text = "Unsaved Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. Do you want to save or discard them?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDiscardDialog = false
                        handleSave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showConfirmDiscardDialog = false
                            onDismiss()
                        }
                    ) {
                        Text("Discard", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showConfirmDiscardDialog = false }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
