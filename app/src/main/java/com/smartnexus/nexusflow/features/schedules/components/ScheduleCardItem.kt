package com.smartnexus.nexusflow.features.schedules.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.components.DeviceAnimationDisplay
import com.smartnexus.nexusflow.domain.model.DeviceType
import com.smartnexus.nexusflow.features.schedules.ScheduleItem

@Composable
fun ScheduleCardItem(
    item: ScheduleItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val activeDaysSet = item.daysActive.toSet()

    val (iconBgColor, activeTextColor) = when (item.deviceType) {
        DeviceType.LIGHT -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
        DeviceType.FAN -> Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
        DeviceType.AC -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB))
        DeviceType.HUMIDIFIER -> Pair(Color(0xFFDCFCE7), Color(0xFF059669))
        DeviceType.TV -> Pair(Color(0xFFF3E8FF), Color(0xFF9333EA))
        DeviceType.SWITCH -> Pair(Color(0xFFD1FAE5), Color(0xFF10B981))
        DeviceType.LIVING_ROOM -> Pair(Color(0xFFEEF2FF), Color(0xFF4F46E5))
        DeviceType.BEDROOM -> Pair(Color(0xFFEEF2FF), Color(0xFF4F46E5))
        DeviceType.KITCHEN -> Pair(Color(0xFFFFEDD5), Color(0xFFEA580C))
        DeviceType.OUTDOOR -> Pair(Color(0xFFDCFCE7), Color(0xFF059669))
        DeviceType.BATHROOM -> Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row (Icon + Device Name + Action Pill + Switch + Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Icon Box (Using app/src/main/assets/animations via DeviceAnimationDisplay)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    DeviceAnimationDisplay(
                        isOn = item.isEnabled,
                        deviceType = item.deviceType,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Action Badge Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.relayName}, ${item.deviceName}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ACTION ON/OFF Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isTurnOn) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6))
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTION: ${item.action}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (item.isTurnOn) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                        )
                    }
                }

                // Switch Toggle
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4F46E5),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
                    )
                )

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Schedule") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Schedule Pattern Info Row (Clock icon)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = item.pattern,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Days Active Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allDays.forEach { day ->
                    val isActive = activeDaysSet.contains(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) (if (isDark) Color(0xFF4338CA) else Color(0xFFC7D2FE)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6)),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                            color = if (isActive) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF9CA3AF))
                        )
                    }
                }
            }

            // Time Range Pill (For Duration Schedules like AC)
            if (!item.timeRange.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.timeRange,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                        )
                    }
                }
            }

            // Footer Next Trigger Row (Calendar icon)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Next: ${item.nextTrigger}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
