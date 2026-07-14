package com.smartnexus.nexusflow.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartnexus.nexusflow.core.components.RelayCard
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.domain.model.DeviceType

data class RelayItemData(
    val id: String,
    val name: String,
    val isOn: Boolean,
    val isPending: Boolean = false,
    val deviceType: DeviceType = DeviceType.fromName(name),
    val powerWatts: Int? = null,
    val valueText: String = if (isOn) "80%" else "0%",
    val valuePercentage: Float = if (isOn) 0.80f else 0.0f
)

@Composable
fun RelayGrid(
    relays: List<RelayItemData>,
    onToggleRelay: (String, Boolean) -> Unit,
    onRelayClick: (RelayItemData) -> Unit,
    modifier: Modifier = Modifier,
    useAnimations: Boolean = false,
    layoutType: RelayLayoutType = RelayLayoutType.AUTO
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            // 3x2+2 layout for 8 relays (Living Room)
            relays.size == 8 && layoutType == RelayLayoutType.THREE_BY_TWO_PLUS_TWO -> {
                // First 6 relays in 3x2 grid
                relays.take(6).chunked(3).forEach { rowRelays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowRelays.forEach { relay ->
                            RelayCardWrapper(
                                relay = relay,
                                onToggleRelay = onToggleRelay,
                                onRelayClick = onRelayClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    relays.takeLast(2).forEach { relay ->
                        RelayCardWrapper(
                            relay = relay,
                            onToggleRelay = onToggleRelay,
                            onRelayClick = onRelayClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3x2 layout for 6 relays (Bedroom)
            relays.size == 6 -> {
                relays.chunked(3).forEach { rowRelays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowRelays.forEach { relay ->
                            RelayCardWrapper(
                                relay = relay,
                                onToggleRelay = onToggleRelay,
                                onRelayClick = onRelayClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Default chunks
            else -> {
                val columnsPerRow = when {
                    relays.size <= 2 -> 1
                    relays.size <= 4 -> 2
                    else -> 3
                }
                relays.chunked(columnsPerRow).forEach { rowRelays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowRelays.forEach { relay ->
                            RelayCardWrapper(
                                relay = relay,
                                onToggleRelay = onToggleRelay,
                                onRelayClick = onRelayClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty slots with spacer
                        if (rowRelays.size < columnsPerRow) {
                            repeat(columnsPerRow - rowRelays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelayCardWrapper(
    relay: RelayItemData,
    onToggleRelay: (String, Boolean) -> Unit,
    onRelayClick: (RelayItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    RelayCard(
        name        = relay.name,
        isOn        = relay.isOn,
        isPending   = relay.isPending,
        deviceType  = relay.deviceType,
        onToggle    = { newState -> onToggleRelay(relay.id, newState) },
        // Long-press opens relay rename / icon / power sheet
        onLongClick = { onRelayClick(relay) },
        modifier    = modifier
    )
}

enum class RelayLayoutType {
    AUTO,
    THREE_BY_TWO_PLUS_TWO,
    TWO_BY_TWO,
    THREE_BY_THREE
}

@Preview(showBackground = true)
@Composable
private fun RelayGridPreview() {
    NexusFlowTheme {
        RelayGrid(
            relays = listOf(
                RelayItemData("1", "Bedroom Light", isOn = true, deviceType = DeviceType.LIGHT),
                RelayItemData("2", "Ceiling Fan", isOn = false, deviceType = DeviceType.FAN),
                RelayItemData("3", "AC Unit", isOn = true, deviceType = DeviceType.AC),
                RelayItemData("4", "Humidifier", isOn = false, deviceType = DeviceType.HUMIDIFIER),
                RelayItemData("5", "TV", isOn = true, deviceType = DeviceType.TV),
                RelayItemData("6", "Power Switch", isOn = false, deviceType = DeviceType.SWITCH)
            ),
            onToggleRelay = { _, _ -> },
            onRelayClick = {}
        )
    }
}
