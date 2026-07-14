package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme

data class PowerConsumptionSegment(
    val label: String,
    val percentage: Int,
    val color: Color
)

@Composable
fun PowerUsageCard(
    energyKwh: Double = 12.6,
    costRs: Double = 101.20,
    startDate: String = "05 Jun 2026",
    runningFor: String = "6d 8h 42m",
    lastUpdated: String = "Just now",
    consumptionSegments: List<PowerConsumptionSegment> = emptyList(),
    onResetClick: () -> Unit = {},
    onViewDetailsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Billing Cycle?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will reset all accumulated power consumption and start a new billing cycle from this moment. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetClick()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Metric 1 (kWh) Colors
    val greenBoxBg = if (isDark) Color(0xFF062C1B) else Color(0xFFF0FDF4)
    val greenBoxBorder = if (isDark) Color(0xFF166534) else Color(0xFFBBF7D0)
    val greenIconBg = if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
    val greenAccent = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)

    // Metric 2 (Cost) Colors
    val amberBoxBg = if (isDark) Color(0xFF3B2303) else Color(0xFFFFFBEB)
    val amberBoxBorder = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
    val amberIconBg = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
    val amberAccent = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    // Inner Info Container
    val innerContainerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val innerContainerBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    // Consumption Segments (Fan, TV, Light, Router, Others)
    val activeSegments = if (consumptionSegments.isNotEmpty()) {
        consumptionSegments
    } else {
        listOf(
            PowerConsumptionSegment("Fan", 41, Color(0xFF10B981)),
            PowerConsumptionSegment("TV", 24, Color(0xFF3B82F6)),
            PowerConsumptionSegment("Light", 19, Color(0xFFF59E0B)),
            PowerConsumptionSegment("Router", 10, Color(0xFF8B5CF6)),
            PowerConsumptionSegment("Others", 6, Color(0xFF9CA3AF))
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Power Usage",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Estimated Pill Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Estimated",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Usage based on relay runtime and configured power",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Power Usage Info",
                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Main Metrics Cards (Current Cycle Usage & Estimated Cost)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric 1: Current Cycle Usage (kWh)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = greenBoxBg),
                    border = BorderStroke(1.dp, greenBoxBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(greenIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = greenAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f", energyKwh),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kWh",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Current Cycle Usage",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                    }
                }

                // Metric 2: Estimated Cost (Rs)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = amberBoxBg),
                    border = BorderStroke(1.dp, amberBoxBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(amberIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    tint = amberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = String.format("%.2f ₹", costRs),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = textPrimary
                            )
                        }

                        Text(
                            text = "Estimated Cost",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                    }
                }
            }

            // 3. Cycle Info Grid (Started Date, Running Time, Last Updated)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = innerContainerBg),
                border = BorderStroke(1.dp, innerContainerBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CycleMetricCol(
                        icon = Icons.Default.CalendarMonth,
                        label = "Cycle Started",
                        value = startDate,
                        isDark = isDark
                    )
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(innerContainerBorder)
                    )
                    CycleMetricCol(
                        icon = Icons.Default.Schedule,
                        label = "Cycle Running For",
                        value = runningFor,
                        isDark = isDark
                    )
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(innerContainerBorder)
                    )
                    CycleMetricCol(
                        icon = Icons.Default.Autorenew,
                        label = "Last Updated",
                        value = lastUpdated,
                        isDark = isDark
                    )
                }
            }

            // 4. Top Consumption Breakdown Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = innerContainerBg),
                border = BorderStroke(1.dp, innerContainerBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Top Consumption",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = textPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onViewDetailsClick)
                        ) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Multi-Segment Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        activeSegments.forEach { segment ->
                            Box(
                                modifier = Modifier
                                    .weight(segment.percentage.toFloat())
                                    .fillMaxWidth()
                                    .background(segment.color)
                            )
                        }
                    }

                    // Legend items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        activeSegments.forEach { segment ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(segment.color)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = segment.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = textSecondary
                                    )
                                }
                                Text(
                                    text = "${segment.percentage}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF3C1313) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Reset Billing Cycle",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Starts a new cycle from current runtime",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Reset",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 6. Footer Disclaimer Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Device tracks only runtime. Power usage is estimated.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun CycleMetricCol(
    icon: ImageVector,
    label: String,
    value: String,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        )
    }
}

@Preview(name = "Power Usage Card - Light Mode", showBackground = true)
@Composable
private fun PowerUsageCardLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            PowerUsageCard()
        }
    }
}

@Preview(name = "Power Usage Card - Dark Mode", showBackground = true)
@Composable
private fun PowerUsageCardDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            PowerUsageCard()
        }
    }
}
