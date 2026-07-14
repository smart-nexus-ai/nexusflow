package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme

@Composable
fun ConnectionStatusIndicator(
    bleConnected: Boolean,
    wifiOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BLE Badge
        StatusBadge(
            icon = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth Status",
            isActive = bleConnected,
            activeColor = Color(0xFF3B82F6) // Brand Blue
        )

        Spacer(modifier = Modifier.width(8.dp))

        // WiFi Badge
        StatusBadge(
            icon = Icons.Default.Wifi,
            contentDescription = "WiFi Status",
            isActive = wifiOnline,
            activeColor = Color(0xFF10B981) // Brand Green
        )
    }
}

@Composable
private fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (isActive) activeColor else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectionStatusIndicatorPreview() {
    NexusFlowTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ConnectionStatusIndicator(bleConnected = true, wifiOnline = true)
            Spacer(modifier = Modifier.width(16.dp))
            ConnectionStatusIndicator(bleConnected = false, wifiOnline = false)
        }
    }
}
