package com.smartnexus.nexusflow.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

enum class DeviceType(
    val displayName: String,
    val icon: ImageVector
) {
    // Relay Appliance Icons
    LIGHT("Light", Icons.Default.Lightbulb),
    FAN("Fan", Icons.Default.Air),
    AC("AC Unit", Icons.Default.AcUnit),
    HUMIDIFIER("Humidifier", Icons.Default.WaterDrop),
    TV("Television", Icons.Default.Tv),
    SWITCH("Switch", Icons.Default.Power),

    // Room / Device Icons
    LIVING_ROOM("Living Room", Icons.Default.Weekend),
    BEDROOM("Bedroom", Icons.Default.Bed),
    KITCHEN("Kitchen", Icons.Default.Kitchen),
    OUTDOOR("Outdoor", Icons.Default.Yard),
    BATHROOM("Bathroom", Icons.Default.Bathtub),
    DEFAULT("Default Device", Icons.Default.Devices);

    companion object {
        val relayApplianceTypes = listOf(LIGHT, FAN, AC, HUMIDIFIER, TV, SWITCH)
        val roomDeviceTypes = listOf(LIVING_ROOM, BEDROOM, KITCHEN, OUTDOOR, BATHROOM, DEFAULT)

        fun fromName(name: String): DeviceType {
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                return when {
                    name.contains("light", ignoreCase = true) -> LIGHT
                    name.contains("fan", ignoreCase = true) -> FAN
                    name.contains("ac", ignoreCase = true) || name.contains("conditioner", ignoreCase = true) -> AC
                    name.contains("humid", ignoreCase = true) -> HUMIDIFIER
                    name.contains("tv", ignoreCase = true) -> TV
                    name.contains("switch", ignoreCase = true) -> SWITCH
                    name.contains("living", ignoreCase = true) || name.contains("hall", ignoreCase = true) -> LIVING_ROOM
                    name.contains("bed", ignoreCase = true) -> BEDROOM
                    name.contains("kitchen", ignoreCase = true) -> KITCHEN
                    name.contains("outdoor", ignoreCase = true) || name.contains("garden", ignoreCase = true) -> OUTDOOR
                    name.contains("bath", ignoreCase = true) -> BATHROOM
                    else -> DEFAULT
                }
            }
        }
    }
}
