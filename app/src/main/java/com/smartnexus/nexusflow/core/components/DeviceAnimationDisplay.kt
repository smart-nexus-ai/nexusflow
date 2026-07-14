package com.smartnexus.nexusflow.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartnexus.nexusflow.domain.model.DeviceType

@Composable
fun DeviceAnimationDisplay(
    isOn: Boolean,
    deviceType: DeviceType,
    modifier: Modifier = Modifier
) {
    // 1. Color transitions
    val activeColor = when (deviceType) {
        DeviceType.LIGHT -> Color(0xFFF59E0B) // Amber
        DeviceType.FAN -> Color(0xFF3B82F6) // Blue
        DeviceType.AC -> Color(0xFF10B981) // Emerald
        DeviceType.HUMIDIFIER -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF6366F1) // Indigo/Purple
    }
    
    val tintColor by animateColorAsState(
        targetValue = if (isOn) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "icon_color_animation"
    )

    // 2. Infinite rotation animation (for FAN)
    val infiniteTransition = rememberInfiniteTransition(label = "icon_infinite_animations")
    
    val rotationAngle by if (deviceType == DeviceType.FAN && isOn) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fan_rotation"
        )
    } else {
        rememberTransitionState(0f)
    }

    // 3. Pulse / glow animation (for LIGHT, HUMIDIFIER, AC)
    val pulseAlpha by if (isOn && (deviceType == DeviceType.LIGHT || deviceType == DeviceType.HUMIDIFIER || deviceType == DeviceType.AC)) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon_pulse"
        )
    } else {
        rememberTransitionState(1.0f)
    }

    val pulseScale by if (isOn && deviceType == DeviceType.LIGHT) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon_scale"
        )
    } else {
        rememberTransitionState(1.0f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = deviceType.icon,
            contentDescription = deviceType.displayName,
            tint = tintColor,
            modifier = Modifier
                .size(34.dp)
                .rotate(if (deviceType == DeviceType.FAN) rotationAngle else 0f)
                .scale(pulseScale)
                .alpha(pulseAlpha)
        )
    }
}

@Composable
private fun rememberTransitionState(value: Float): androidx.compose.runtime.State<Float> {
    return androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(value) }
}
