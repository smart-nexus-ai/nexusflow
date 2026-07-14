package com.smartnexus.nexusflow.features.adddevice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.smartnexus.nexusflow.ble.BleScanner
import com.smartnexus.nexusflow.ble.BleWifiProvisioner
import com.smartnexus.nexusflow.features.adddevice.components.BleDiscoveredDevice
import com.smartnexus.nexusflow.features.adddevice.components.DeviceConfirmationData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDeviceUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 5,
    val discoveredDevices: List<BleDiscoveredDevice> = emptyList(),
    val isScanningBle: Boolean = false,
    val selectedDevice: BleDiscoveredDevice? = null,
    val pinInput: String = "",
    val pinError: String? = null,
    val availableNetworks: List<String> = emptyList(),
    val selectedSsid: String = "",
    val wifiPassword: String = "",
    val isWifiScanning: Boolean = false,
    val confirmationData: DeviceConfirmationData = DeviceConfirmationData(
        deviceName = "Smart Relay",
        hardwareId = "",
        channels = 4,
        firmware = "v1.2.5",
        deviceCountBadge = ""
    )
)

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleScanner: BleScanner,
    private val bleWifiProvisioner: BleWifiProvisioner
) : ViewModel() {

    companion object {
        // Mirrors Config.h RELAY_COUNT — update this if the hardware changes
        private const val RELAY_COUNT_DEFAULT = 6
    }

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Collect real BLE scanning results
        viewModelScope.launch {
            bleScanner.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanningBle = isScanning) }
            }
        }
        viewModelScope.launch {
            bleWifiProvisioner.provisioningState.collect { status ->
                Log.d("AddDeviceViewModel", "Provisioning status update: $status")
                when (status) {
                    is BleWifiProvisioner.ProvisioningStatus.Connecting -> {
                        _uiState.update { it.copy(isWifiScanning = true) }
                    }
                    is BleWifiProvisioner.ProvisioningStatus.MetadataRead -> {
                        val friendlyName = when (status.relayCount) {
                            8 -> "Living Room 2"
                            6 -> "Bedroom 2"
                            else -> "Guestroom 2"
                        }
                        val deviceTypeName = when (status.relayCount) {
                            8 -> "Smart Pro Relay (8CH)"
                            6 -> "Smart Pro Relay (6CH)"
                            else -> "Smart Standard Relay (4CH)"
                        }
                        _uiState.update { state ->
                            state.copy(
                                currentStep = 3,
                                isWifiScanning = false,
                                pinError = null,
                                confirmationData = DeviceConfirmationData(
                                    deviceName = deviceTypeName,
                                    hardwareId = status.hardwareId,
                                    channels = status.relayCount,
                                    firmware = status.firmwareVersion,
                                    deviceCountBadge = "Ready for pairing"
                                )
                            )
                        }
                    }
                    is BleWifiProvisioner.ProvisioningStatus.PinVerified -> {
                        _uiState.update { it.copy(pinError = null) }
                    }
                    is BleWifiProvisioner.ProvisioningStatus.WiFiConnecting -> {
                        _uiState.update { it.copy(isWifiScanning = true) }
                    }
                    is BleWifiProvisioner.ProvisioningStatus.Success -> {
                        _uiState.update { it.copy(currentStep = 5, isWifiScanning = false) }
                    }
                    is BleWifiProvisioner.ProvisioningStatus.Error -> {
                        _uiState.update { state ->
                            state.copy(pinError = status.message, isWifiScanning = false)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startBleScan() {
        bleScanner.startScan()
    }

    private fun stopBleScan() {
        bleScanner.stopScan()
    }

    private fun scanWifiNetworks() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return

            _uiState.update { it.copy(isWifiScanning = true) }

            // Read any already-cached results first
            val cachedSsids = wifiManager.scanResults
                .mapNotNull { it.SSID.takeIf { ssid -> ssid.isNotBlank() } }
                .distinct()
            if (cachedSsids.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        availableNetworks = cachedSsids,
                        selectedSsid = cachedSsids.first(),
                        isWifiScanning = false
                    )
                }
                return
            }

            // Register a one-shot receiver to collect fresh scan results
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    ctx.unregisterReceiver(this)
                    val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    } else true

                    val ssids = wifiManager.scanResults
                        .mapNotNull { it.SSID.takeIf { ssid -> ssid.isNotBlank() } }
                        .distinct()

                    _uiState.update {
                        it.copy(
                            availableNetworks = if (ssids.isNotEmpty()) ssids else it.availableNetworks,
                            selectedSsid = if (ssids.isNotEmpty()) ssids.first() else it.selectedSsid,
                            isWifiScanning = false
                        )
                    }
                }
            }

            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, filter)

            @Suppress("DEPRECATION")
            val started = wifiManager.startScan()
            if (!started) {
                // Scan throttled by Android — unregister and fallback to cached
                context.unregisterReceiver(receiver)
                _uiState.update { it.copy(isWifiScanning = false) }
            }
        } catch (e: Exception) {
            Log.e("AddDeviceViewModel", "Failed to scan WiFi networks", e)
            _uiState.update { it.copy(isWifiScanning = false) }
        }
    }

    fun nextStep() {
        val currentState = _uiState.value
        if (currentState.currentStep < currentState.totalSteps) {
            val nextStep = currentState.currentStep + 1
            _uiState.update { it.copy(currentStep = nextStep) }
            
            when (nextStep) {
                2 -> startBleScan()
                3 -> stopBleScan()
                4 -> { /* Manual SSID entry, no scan needed */ }
                5 -> provisionAndRegisterDevice()
            }
        }
    }

    /**
     * Derives the expected provisioning PIN for a device, matching the ESP32 firmware logic in
     * BLEManager.cpp::verifyPIN().
     *
     * Firmware device name format: "NexusFlow-XXXX" where XXXX = last 4 hex digits of eFUSE MAC.
     * Hardware ID format: "SN-8{relay_count}CH-XXXX" (used internally but NOT the BLE name).
     *
     * Algorithm (matches firmware exactly):
     *   prefix = relay_count==6 → "9", relay_count==8 → "5", relay_count==4 → "8", else → "1"
     *   pin = strtol(prefix + XXXX, base16).toString()
     *
     * We support both name formats for robustness.
     */
    internal fun calculateExpectedPin(deviceName: String, relayCount: Int = -1): String {
        // Format 1: "NexusFlow-XXXX" — the actual BLE advertisement name
        val nexusFlowRegex = "NexusFlow-([0-9A-Fa-f]{4})".toRegex()
        val nexusFlowMatch = nexusFlowRegex.find(deviceName)
        if (nexusFlowMatch != null) {
            val suffixHex = nexusFlowMatch.groupValues[1]
            // Infer relay count from relayCount param or from Hardware ID if available
            val channels = if (relayCount > 0) relayCount else {
                // Try to read relay count from the selected device's hardware ID in state
                val hwId = _uiState.value.confirmationData.hardwareId
                when {
                    hwId.contains("8CH") -> 8
                    hwId.contains("6CH") -> 6
                    else -> 4
                }
            }
            val prefix = when (channels) {
                6 -> "9"
                8 -> "5"
                4 -> "8"
                else -> "3"
            }
            return try {
                (prefix + suffixHex).toLong(16).toString()
            } catch (e: Exception) {
                "123456"
            }
        }

        // Format 2: "SN-{X}CH-XXXX" — Hardware ID format (fallback)
        val snRegex = "SN-(\\d)CH-([0-9A-Fa-f]{4})".toRegex()
        val snMatch = snRegex.find(deviceName)
        if (snMatch != null) {
            val channelCount = snMatch.groupValues[1].toInt()
            val suffixHex = snMatch.groupValues[2]
            val prefix = when (channelCount) {
                6 -> "9"
                8 -> "5"
                4 -> "8"
                else -> "3"
            }
            return try {
                (prefix + suffixHex).toLong(16).toString()
            } catch (e: Exception) {
                "123456"
            }
        }

        return "123456"
    }

    private fun provisionAndRegisterDevice() {
        val currentState = _uiState.value
        val device = currentState.selectedDevice ?: return
        val channels = currentState.confirmationData.channels
        val hardwareId = currentState.confirmationData.hardwareId
        val expectedPin = calculateExpectedPin(hardwareId, relayCount = channels)
        val deviceId = hardwareId.lowercase().replace("-", "_")

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        _uiState.update { it.copy(isWifiScanning = true, pinError = null) }

        bleWifiProvisioner.provision(
            ssid = currentState.selectedSsid,
            password = currentState.wifiPassword,
            pin = expectedPin,
            deviceId = deviceId,
            ownerId = uid
        )
    }

    fun previousStep(): Boolean {
        return if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
            true
        } else {
            false
        }
    }

    fun selectBleDevice(device: BleDiscoveredDevice) {
        _uiState.update { state ->
            state.copy(
                selectedDevice = device,
                pinInput = "",
                pinError = null,
                isWifiScanning = true
            )
        }
        stopBleScan()
        bleWifiProvisioner.connectAndReadInfo(device.macAddress)
    }

    fun updatePin(newPin: String) {
        _uiState.update { it.copy(pinInput = newPin, pinError = null) }
    }

    fun submitPin() {
        val currentState = _uiState.value
        val device = currentState.selectedDevice
        if (device == null) {
            _uiState.update { it.copy(pinError = "No device selected.") }
            return
        }

        val hardwareId = currentState.confirmationData.hardwareId
        val channels = currentState.confirmationData.channels
        val expectedPin = calculateExpectedPin(hardwareId, relayCount = channels)
        if (currentState.pinInput == expectedPin) {
            _uiState.update { it.copy(pinError = null, currentStep = 4) }
        } else {
            _uiState.update { it.copy(pinError = "Invalid PIN. Check label and try again.") }
        }
    }

    fun updateSsid(ssid: String) {
        _uiState.update { it.copy(selectedSsid = ssid) }
    }

    fun updatePassword(pass: String) {
        _uiState.update { it.copy(wifiPassword = pass) }
    }

    fun resetWizard() {
        _uiState.update { AddDeviceUiState(currentStep = 1) }
    }

    override fun onCleared() {
        super.onCleared()
        bleWifiProvisioner.cleanup()
    }
}
