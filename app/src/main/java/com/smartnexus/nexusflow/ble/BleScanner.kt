package com.smartnexus.nexusflow.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smartnexus.nexusflow.features.adddevice.components.BleDiscoveredDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val _discoveredDevices = MutableStateFlow<List<BleDiscoveredDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            
            // Match any SN- or NexusFlow- device name
            if (name.startsWith("SN-") || name.startsWith("NexusFlow-")) {
                val mac = device.address
                val rssi = "${result.rssi} dBm"
                
                val currentList = _discoveredDevices.value
                if (currentList.none { it.macAddress == mac }) {
                    _discoveredDevices.value = currentList + BleDiscoveredDevice(name, mac, rssi)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (_isScanning.value) return

        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        try {
            scanner.startScan(scanCallback)
        } catch (e: Exception) {
            Log.e("BleScanner", "Failed to start BLE scan", e)
            _isScanning.value = false
            return
        }

        // Auto stop scan after 10 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, 10000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        _isScanning.value = false
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e("BleScanner", "Failed to stop BLE scan", e)
        }
    }
}
