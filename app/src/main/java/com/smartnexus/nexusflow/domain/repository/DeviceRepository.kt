package com.smartnexus.nexusflow.domain.repository

import com.smartnexus.nexusflow.domain.model.Device
import com.smartnexus.nexusflow.domain.model.RelayState
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevices(): Flow<List<Device>>
    fun getDeviceById(id: String): Flow<Device?>
    suspend fun insertDevice(device: Device): Result<Unit>
    suspend fun deleteDevice(id: String): Result<Unit>
    fun getRelayStates(deviceId: String): Flow<List<RelayState>>
    suspend fun updateRelayState(id: String, isOn: Boolean, isPending: Boolean): Result<Unit>
}
