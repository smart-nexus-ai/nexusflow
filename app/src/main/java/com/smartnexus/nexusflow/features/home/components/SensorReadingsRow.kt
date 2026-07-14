package com.smartnexus.nexusflow.features.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartnexus.nexusflow.core.components.SensorMetricCard
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme

data class SensorItemData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val unit: String,
    val comfortLevel: String
)

@Composable
fun SensorReadingsRow(
    sensors: List<SensorItemData>,
    lastUpdatedText: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Environmental Sensors",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = lastUpdatedText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sensors.forEach { sensor ->
                val isTemp = sensor.label.contains("Temp", ignoreCase = true)
                val isHumidex = sensor.label.contains("Humidex", ignoreCase = true)

                // Derive colors matching the style guidelines
                val accentColor = when {
                    isHumidex -> Color(0xFF0284C7)
                    isTemp -> Color(0xFF10B981)
                    else -> Color(0xFFF97316)
                }

                val bgColor = if (isDark) {
                    when {
                        isHumidex -> Color(0xFF0F172A)
                        isTemp -> Color(0xFF064E3B)
                        else -> Color(0xFF3B2303)
                    }
                } else {
                    when {
                        isHumidex -> Color(0xFFEFF6FF)
                        isTemp -> Color(0xFFECFDF5)
                        else -> Color(0xFFFFFBEB)
                    }
                }

                SensorMetricCard(
                    icon = sensor.icon,
                    label = sensor.label,
                    value = "${sensor.value}${sensor.unit}",
                    status = sensor.comfortLevel,
                    accentColor = accentColor,
                    bgColor = bgColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SensorReadingsRowPreview() {
    NexusFlowTheme {
        SensorReadingsRow(
            sensors = listOf(
                SensorItemData(Icons.Default.Thermostat, "Temperature", "28.5", "°C", "Comfortable"),
                SensorItemData(Icons.Default.WaterDrop, "Humidity", "65", "%", "Normal"),
                SensorItemData(Icons.Default.SentimentSatisfiedAlt, "Humidex", "28.7", "°C", "Comfortable")
            ),
            lastUpdatedText = "Updated 5 seconds ago"
        )
    }
}
