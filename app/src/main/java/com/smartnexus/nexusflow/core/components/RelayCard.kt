package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.domain.model.DeviceType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RelayCard(
    name: String,
    isOn: Boolean,
    isPending: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deviceType: DeviceType = DeviceType.fromName(name),
    // Long-press opens the relay rename / icon / power sheet
    onLongClick: (() -> Unit)? = null
) {
    val activeColor   = Color(0xFF10B981)
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled     = !isPending,
                onClick     = { onToggle(!isOn) },
                onLongClick = onLongClick
            ),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isOn) {
            BorderStroke(1.5.dp, activeColor.copy(alpha = 0.65f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Animation / spinner ──────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(56.dp)
            ) {
                if (isPending) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp,
                        color       = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Reuse the proven DeviceAnimationDisplay — avoids
                    // re-implementing Lottie composition lifecycle
                    DeviceAnimationDisplay(
                        isOn       = isOn,
                        deviceType = deviceType,
                        modifier   = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ── Device name ──────────────────────────────────────────────────
            Text(
                text      = name,
                style     = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    lineHeight = 15.sp
                ),
                color     = MaterialTheme.colorScheme.onSurface,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // ── Status label ─────────────────────────────────────────────────
            Text(
                text  = when {
                    isPending -> "…"
                    isOn      -> "ON"
                    else      -> "OFF"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp
                ),
                color = when {
                    isPending -> inactiveColor
                    isOn      -> activeColor
                    else      -> inactiveColor
                }
            )
        }
    }
}

// ─── Preview helpers ──────────────────────────────────────────────────────────

class RelayPreviewParameterProvider : PreviewParameterProvider<RelayState> {
    override val values = sequenceOf(
        RelayState("Living Light",  DeviceType.LIGHT,      isOn = true,  isPending = false),
        RelayState("Ceiling Fan",   DeviceType.FAN,        isOn = true,  isPending = false),
        RelayState("TV Unit",       DeviceType.TV,         isOn = false, isPending = false),
        RelayState("LED Strip",     DeviceType.LIGHT,      isOn = true,  isPending = false),
        RelayState("Spare Switch",  DeviceType.SWITCH,     isOn = false, isPending = true),
        RelayState("Air Purifier",  DeviceType.HUMIDIFIER, isOn = true,  isPending = false),
    )
}

data class RelayState(
    val name: String,
    val deviceType: DeviceType,
    val isOn: Boolean,
    val isPending: Boolean
)

@Preview(showBackground = true)
@Composable
fun RelayCardLightPreview(
    @PreviewParameter(RelayPreviewParameterProvider::class) state: RelayState
) {
    NexusFlowTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            RelayCard(
                name        = state.name,
                isOn        = state.isOn,
                isPending   = state.isPending,
                deviceType  = state.deviceType,
                onToggle    = {},
                onLongClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RelayCardDarkPreview(
    @PreviewParameter(RelayPreviewParameterProvider::class) state: RelayState
) {
    NexusFlowTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            RelayCard(
                name        = state.name,
                isOn        = state.isOn,
                isPending   = state.isPending,
                deviceType  = state.deviceType,
                onToggle    = {},
                onLongClick = {}
            )
        }
    }
}
