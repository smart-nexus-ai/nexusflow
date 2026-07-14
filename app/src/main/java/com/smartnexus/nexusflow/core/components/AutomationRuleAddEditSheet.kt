package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.features.home.components.AutomationRuleItemData
import com.smartnexus.nexusflow.features.home.components.RelayItemData

enum class SensorSelectionMode {
    TEMPERATURE_ONLY,
    HUMIDITY_ONLY,
    BOTH
}

enum class LogicalOperator {
    AND,
    OR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRuleAddEditSheet(
    initialRule: AutomationRuleItemData? = null,
    availableRelays: List<RelayItemData> = emptyList(),
    existingRules: List<AutomationRuleItemData> = emptyList(),
    onDismiss: () -> Unit,
    onSaveRule: (AutomationRuleItemData) -> Unit,
    onDeleteRule: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEditMode = initialRule != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()

    // ── Form state ────────────────────────────────────────────────────────────
    var sensorMode by remember {
        mutableStateOf(
            when {
                initialRule?.description?.contains("Temp", ignoreCase = true) == true
                    && initialRule.description.contains("Humidity", ignoreCase = true) -> SensorSelectionMode.BOTH
                initialRule?.description?.contains("Humidity", ignoreCase = true) == true -> SensorSelectionMode.HUMIDITY_ONLY
                else -> SensorSelectionMode.TEMPERATURE_ONLY
            }
        )
    }

    var logicalOp by remember {
        mutableStateOf(
            if (initialRule?.description?.contains(" OR ", ignoreCase = true) == true)
                LogicalOperator.OR else LogicalOperator.AND
        )
    }

    var tempCondition by remember {
        mutableStateOf(
            if (initialRule?.description?.contains("Temp <") == true
                || initialRule?.subtitle?.contains("temperature drops below", ignoreCase = true) == true)
                "Below" else "Above"
        )
    }
    var tempThreshold by remember { mutableFloatStateOf(30.0f) }

    var humidityCondition by remember {
        mutableStateOf(
            if (initialRule?.description?.contains("Humidity <") == true
                || initialRule?.subtitle?.contains("humidity drops below", ignoreCase = true) == true)
                "Below" else "Above"
        )
    }
    var humidityThreshold by remember { mutableFloatStateOf(60.0f) }

    val configuredRelayIds = remember(existingRules, initialRule) {
        existingRules.filterNot { it.id == initialRule?.id }.map { it.targetRelayId }.toSet()
    }

    // Default to the next available relay (not configured yet)
    val defaultSelectedRelay = remember(availableRelays, initialRule, configuredRelayIds) {
        if (initialRule != null && availableRelays.any { it.id == initialRule.targetRelayId }) {
            availableRelays.first { it.id == initialRule.targetRelayId }
        } else {
            availableRelays.firstOrNull { !configuredRelayIds.contains(it.id) }
        }
    }

    var selectedRelay by remember { mutableStateOf(defaultSelectedRelay) }
    var actionOnTrigger by remember {
        mutableStateOf(initialRule?.description?.contains("OFF", ignoreCase = true) != true)
    }
    var isEnabled by remember { mutableStateOf(initialRule?.isEnabled ?: true) }

    // Check if limit is reached for adding new rule
    val noRelaysAvailable = remember(availableRelays, configuredRelayIds) {
        availableRelays.all { configuredRelayIds.contains(it.id) }
    }

    // ── Change-detection for unsaved-changes dialog ────────────────────────
    val hasChanges by remember {
        derivedStateOf {
            if (!isEditMode) {
                // For new rule: any non-default state counts as a change
                selectedRelay != defaultSelectedRelay
                    || sensorMode != SensorSelectionMode.TEMPERATURE_ONLY
                    || tempCondition != "Above"
                    || tempThreshold != 30.0f
                    || humidityThreshold != 60.0f
                    || !actionOnTrigger
                    || !isEnabled
            } else true  // edit mode always asks on close if changes exist (assume always has changes or simplify to true)
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog   by remember { mutableStateOf(false) }

    // ── Helper: build the save result ─────────────────────────────────────
    fun buildRule(): AutomationRuleItemData? {
        val relay = selectedRelay ?: return null
        val relayName = relay.name
        val actionStr = if (actionOnTrigger) "ON" else "OFF"
        val ruleId    = initialRule?.id ?: "rule_${System.currentTimeMillis()}"

        return when (sensorMode) {
            SensorSelectionMode.TEMPERATURE_ONLY -> {
                val sym  = if (tempCondition == "Above") ">" else "<"
                val text = if (tempCondition == "Above") "rises above" else "drops below"
                AutomationRuleItemData(
                    id = ruleId,
                    description = "$relayName $actionStr if Temp $sym ${tempThreshold.toInt()}°C",
                    subtitle = "When temperature $text ${tempThreshold.toInt()}°C",
                    isEnabled = isEnabled,
                    iconType = "snowflake",
                    targetRelayId = relay.id
                )
            }
            SensorSelectionMode.HUMIDITY_ONLY -> {
                val sym  = if (humidityCondition == "Above") ">" else "<"
                val text = if (humidityCondition == "Above") "rises above" else "drops below"
                AutomationRuleItemData(
                    id = ruleId,
                    description = "$relayName $actionStr if Humidity $sym ${humidityThreshold.toInt()}%",
                    subtitle = "When humidity $text ${humidityThreshold.toInt()}%",
                    isEnabled = isEnabled,
                    iconType = "wind",
                    targetRelayId = relay.id
                )
            }
            SensorSelectionMode.BOTH -> {
                val tSym  = if (tempCondition == "Above") ">" else "<"
                val hSym  = if (humidityCondition == "Above") ">" else "<"
                val opStr = if (logicalOp == LogicalOperator.AND) "AND" else "OR"
                AutomationRuleItemData(
                    id = ruleId,
                    description = "$relayName $actionStr if Temp $tSym ${tempThreshold.toInt()}°C $opStr Humidity $hSym ${humidityThreshold.toInt()}%",
                    subtitle = "When Temp $tSym ${tempThreshold.toInt()}°C $opStr Humidity $hSym ${humidityThreshold.toInt()}%",
                    isEnabled = isEnabled,
                    iconType = "snowflake",
                    targetRelayId = relay.id
                )
            }
        }
    }

    val canSave = selectedRelay != null && !noRelaysAvailable

    // ── Discard-changes confirmation dialog ───────────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text  = { Text("You have unsaved changes. What would you like to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        val rule = buildRule()
                        if (rule != null) { onSaveRule(rule) }
                        showDiscardDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    enabled = canSave
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { showDiscardDialog = false; onDismiss() }) {
                        Text("Discard", color = Color(0xFFEF4444))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ── Delete-rule confirmation dialog ───────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule?", fontWeight = FontWeight.Bold) },
            text  = { Text("This automation rule will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRule?.invoke(initialRule!!.id)
                        showDeleteDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (hasChanges) showDiscardDialog = true else onDismiss()
        },
        sheetState     = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier       = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Header: title + Save icon button ─────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector  = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint         = Color(0xFF6366F1),
                        modifier     = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text  = if (isEditMode) "Edit Automation Rule" else "Add Automation Rule",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Save icon button at the top right
                IconButton(
                    onClick  = {
                        val rule = buildRule()
                        if (rule != null) { onSaveRule(rule); onDismiss() }
                    },
                    enabled  = canSave,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (canSave) Color(0xFF4F46E5) else Color(0xFF4F46E5).copy(alpha = 0.3f)
                        )
                ) {
                    Icon(
                        imageVector  = Icons.Default.Save,
                        contentDescription = "Save Rule",
                        tint         = Color.White,
                        modifier     = Modifier.size(20.dp)
                    )
                }
            }

            // ── Max Limit Warning Banner ──────────────────────────────────
            if (noRelaysAvailable && !isEditMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF452B1A) else Color(0xFFFEF3C7)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD97706))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Max Limit Reached",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                            )
                            Text(
                                text = "Each relay channel supports exactly 1 rule. All channels are already configured.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color(0xFFFCD34D).copy(alpha = 0.8f) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // ── Section 1: Sensor Selection ───────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = "1. Select Sensor(s)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        modifier    = Modifier.weight(1f),
                        title       = "Temp (°C)",
                        icon        = Icons.Default.Thermostat,
                        isSelected  = sensorMode == SensorSelectionMode.TEMPERATURE_ONLY,
                        activeColor = Color(0xFFF59E0B),
                        onClick     = { sensorMode = SensorSelectionMode.TEMPERATURE_ONLY }
                    )
                    SelectableChip(
                        modifier    = Modifier.weight(1f),
                        title       = "Humidity (%)",
                        icon        = Icons.Default.WaterDrop,
                        isSelected  = sensorMode == SensorSelectionMode.HUMIDITY_ONLY,
                        activeColor = Color(0xFF0284C7),
                        onClick     = { sensorMode = SensorSelectionMode.HUMIDITY_ONLY }
                    )
                    SelectableChip(
                        modifier    = Modifier.weight(1.2f),
                        title       = "Both",
                        icon        = Icons.Default.AutoAwesome,
                        isSelected  = sensorMode == SensorSelectionMode.BOTH,
                        activeColor = Color(0xFF6366F1),
                        onClick     = { sensorMode = SensorSelectionMode.BOTH }
                    )
                }

                if (sensorMode == SensorSelectionMode.BOTH) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "Condition Combination Operator:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SelectablePill(Modifier.weight(1f), "AND (Both must meet)", logicalOp == LogicalOperator.AND) { logicalOp = LogicalOperator.AND }
                        SelectablePill(Modifier.weight(1f), "OR (Either can meet)",  logicalOp == LogicalOperator.OR)  { logicalOp = LogicalOperator.OR  }
                    }
                }
            }

            // ── Section 2A: Temperature Threshold ────────────────────────
            if (sensorMode == SensorSelectionMode.TEMPERATURE_ONLY || sensorMode == SensorSelectionMode.BOTH) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "2A. Temperature Threshold (°C)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = Color(0xFFD97706)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectablePill(Modifier.weight(1f), "Rises Above (>)", tempCondition == "Above") { tempCondition = "Above" }
                        SelectablePill(Modifier.weight(1f), "Drops Below (<)", tempCondition == "Below") { tempCondition = "Below" }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Temperature Target:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${tempThreshold.toInt()}°C", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD97706))
                    }
                    Slider(value = tempThreshold, onValueChange = { tempThreshold = it }, valueRange = 15.0f..45.0f, steps = 29, modifier = Modifier.fillMaxWidth())
                }
            }

            // ── Section 2B: Humidity Threshold ───────────────────────────
            if (sensorMode == SensorSelectionMode.HUMIDITY_ONLY || sensorMode == SensorSelectionMode.BOTH) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "2B. Humidity Threshold (%)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = Color(0xFF0284C7)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectablePill(Modifier.weight(1f), "Rises Above (>)", humidityCondition == "Above") { humidityCondition = "Above" }
                        SelectablePill(Modifier.weight(1f), "Drops Below (<)", humidityCondition == "Below") { humidityCondition = "Below" }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Humidity Target:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${humidityThreshold.toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0284C7))
                    }
                    Slider(value = humidityThreshold, onValueChange = { humidityThreshold = it }, valueRange = 20.0f..95.0f, steps = 74, modifier = Modifier.fillMaxWidth())
                }
            }

            // ── Section 3: Target Relay ───────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "3. Target Relay",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text  = "1 Rule / Relay",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = Color(0xFF4F46E5)
                    )
                }

                if (availableRelays.isEmpty()) {
                    Text(text = "No relays available", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableRelays) { relay ->
                            val isAlreadyUsed = configuredRelayIds.contains(relay.id)
                            val isSelected    = selectedRelay?.id == relay.id

                            Card(
                                modifier = if (isAlreadyUsed) Modifier else Modifier.clickable {
                                    selectedRelay = relay
                                },
                                shape  = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    when {
                                        isSelected    -> Color(0xFF4F46E5)
                                        isAlreadyUsed -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                        else          -> Color(0xFFD1D5DB)
                                    }
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isSelected    -> Color(0xFFEEF2FF)
                                        isAlreadyUsed -> if (isDark) Color(0xFF2C1E1E) else Color(0xFFFEF2F2)
                                        else          -> MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(
                                    modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text  = relay.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize   = 12.sp
                                            ),
                                            color = when {
                                                isAlreadyUsed -> Color(0xFFEF4444).copy(alpha = 0.8f)
                                                isSelected    -> Color(0xFF4F46E5)
                                                else          -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (isAlreadyUsed) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector  = Icons.Default.Block,
                                                contentDescription = "Already used",
                                                tint         = Color(0xFFEF4444),
                                                modifier     = Modifier.size(12.dp)
                                            )
                                        } else if (isSelected) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector  = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint         = Color(0xFF4F46E5),
                                                modifier     = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    // "Used" label for already-configured relays
                                    if (isAlreadyUsed) {
                                        Text(
                                            text  = "Rule Configured",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectablePill(Modifier.weight(1f), "Turn Relay ON ⚡", actionOnTrigger)  { actionOnTrigger = true  }
                    SelectablePill(Modifier.weight(1f), "Turn Relay OFF 🔴", !actionOnTrigger) { actionOnTrigger = false }
                }
            }

            // ── Enable Rule toggle — at the BOTTOM, dark-mode aware (Slate 800 for dark mode) ───────
            val enableToggleBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6)
            val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(enableToggleBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text  = "Enable Rule Immediately",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = textColor
                    )
                    Text(
                        text  = "Evaluates dynamically on sensor updates",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked         = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = Color(0xFF10B981),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // ── Delete card — only shown in edit mode at the bottom ───────────────────
            if (isEditMode && onDeleteRule != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete This Rule",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun SelectableChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) activeColor else Color(0xFFE5E7EB)),
        colors   = CardDefaults.cardColors(containerColor = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier          = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(28.dp).clip(CircleShape).background(activeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = activeColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 11.sp
                ),
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectablePill(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier         = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 12.sp
            ),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
