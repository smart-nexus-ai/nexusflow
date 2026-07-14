package com.smartnexus.nexusflow.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleWifiProvisioner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    // UUIDs must exactly match BLEManager.cpp firmware definitions
    private val SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    private val READ_INFO_CHAR_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    private val WRITE_CHAR_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
    private val STATUS_CHAR_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val _provisioningState = MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    val provisioningState = _provisioningState.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var pendingSsid: String? = null
    private var pendingPass: String? = null
    private var pendingPin: String? = null
    private var pendingDeviceId: String? = null

    sealed class ProvisioningStatus {
        object Idle : ProvisioningStatus()
        object Connecting : ProvisioningStatus()
        object Connected : ProvisioningStatus()
        data class MetadataRead(
            val hardwareId: String,
            val relayCount: Int,
            val deviceName: String,
            val firmwareVersion: String
        ) : ProvisioningStatus()
        object SendingCredentials : ProvisioningStatus()
        object PinVerified : ProvisioningStatus()
        object WiFiConnecting : ProvisioningStatus()
        object Success : ProvisioningStatus()
        data class Error(val message: String) : ProvisioningStatus()
    }

    @SuppressLint("MissingPermission")
    fun connectAndReadInfo(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress) ?: run {
            _provisioningState.value = ProvisioningStatus.Error("Device not found")
            return
        }

        cleanup()
        _provisioningState.value = ProvisioningStatus.Connecting

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d("BleWifiProvisioner", "Connection state change: status=$status, newState=$newState")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        _provisioningState.value = ProvisioningStatus.Connected
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        _provisioningState.value = ProvisioningStatus.Idle
                        cleanup()
                    }
                } else {
                    _provisioningState.value = ProvisioningStatus.Error("Connection failed: status $status")
                    cleanup()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d("BleWifiProvisioner", "Services discovered: status=$status")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    val readChar = service?.getCharacteristic(READ_INFO_CHAR_UUID)
                    if (readChar != null) {
                        gatt.readCharacteristic(readChar)
                    } else {
                        _provisioningState.value = ProvisioningStatus.Error("Device metadata characteristic not found")
                        cleanup()
                    }
                } else {
                    _provisioningState.value = ProvisioningStatus.Error("Service discovery failed")
                    cleanup()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val jsonStr = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("BleWifiProvisioner", "Metadata read: $jsonStr")
                    try {
                        val json = JSONObject(jsonStr)
                        val hardwareId = json.getString("hardwareId")
                        val deviceName = json.getString("deviceName")
                        val firmwareVersion = json.optString("firmware", json.optString("firmwareVersion", "v1.0.0"))
                        val relayCount = json.getInt("relayCount")
                        
                        _provisioningState.value = ProvisioningStatus.MetadataRead(
                            hardwareId = hardwareId,
                            relayCount = relayCount,
                            deviceName = deviceName,
                            firmwareVersion = firmwareVersion
                        )
                    } catch (e: Exception) {
                        _provisioningState.value = ProvisioningStatus.Error("Failed to parse metadata")
                        cleanup()
                    }
                } else {
                    _provisioningState.value = ProvisioningStatus.Error("Failed to read device info")
                    cleanup()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d("BleWifiProvisioner", "Characteristic write status: $status")
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == WRITE_CHAR_UUID) {
                    // Subscribe to status notifications
                    val service = gatt.getService(SERVICE_UUID)
                    val statusChar = service?.getCharacteristic(STATUS_CHAR_UUID)
                    if (statusChar != null) {
                        gatt.setCharacteristicNotification(statusChar, true)
                        val descriptor = statusChar.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        } else {
                            Log.e("BleWifiProvisioner", "CCCD descriptor not found for status notification")
                        }
                    } else {
                        Log.e("BleWifiProvisioner", "Status characteristic not found")
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    _provisioningState.value = ProvisioningStatus.Error("Failed to write WiFi credentials")
                    cleanup()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == STATUS_CHAR_UUID) {
                    val statusJson = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("BleWifiProvisioner", "Status notification: $statusJson")
                    try {
                        val json = JSONObject(statusJson)
                        val status = json.getString("status")
                        when (status) {
                            "pin_verified" -> _provisioningState.value = ProvisioningStatus.PinVerified
                            "pin_failed" -> {
                                _provisioningState.value = ProvisioningStatus.Error("PIN verification failed on device")
                                cleanup()
                            }
                            "wifi_connecting" -> {
                                Log.d("BleWifiProvisioner", "WiFi connecting, treating as Success immediately")
                                _provisioningState.value = ProvisioningStatus.Success
                                cleanup()
                            }
                            "wifi_failed" -> {
                                val err = json.optString("error", "Unknown WiFi error")
                                _provisioningState.value = ProvisioningStatus.Error("WiFi Connection Failed: $err")
                                cleanup()
                            }
                            "registered" -> {
                                _provisioningState.value = ProvisioningStatus.Success
                                cleanup()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BleWifiProvisioner", "Failed to parse status notification", e)
                    }
                }
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun provision(ssid: String, password: String, pin: String, deviceId: String, ownerId: String) {
        val gatt = bluetoothGatt
        if (gatt == null) {
            _provisioningState.value = ProvisioningStatus.Error("No active BLE connection")
            return
        }

        _provisioningState.value = ProvisioningStatus.SendingCredentials

        val service = gatt.getService(SERVICE_UUID)
        val writeChar = service?.getCharacteristic(WRITE_CHAR_UUID)
        if (writeChar != null) {
            val json = JSONObject().apply {
                put("ssid", ssid)
                put("password", password)
                put("pin", pin)
                put("deviceId", deviceId)
                put("ownerId", ownerId)
            }.toString()

            writeChar.value = json.toByteArray(Charsets.UTF_8)
            val success = gatt.writeCharacteristic(writeChar)
            if (!success) {
                _provisioningState.value = ProvisioningStatus.Error("Failed to initiate BLE write")
            }
        } else {
            _provisioningState.value = ProvisioningStatus.Error("WiFi provisioning characteristic not found")
        }
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
