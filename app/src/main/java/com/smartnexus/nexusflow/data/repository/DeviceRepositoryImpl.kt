package com.smartnexus.nexusflow.data.repository

import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import com.smartnexus.nexusflow.data.local.entity.DeviceEntity
import com.smartnexus.nexusflow.data.local.entity.RelayStateEntity
import com.smartnexus.nexusflow.domain.model.Device
import com.smartnexus.nexusflow.domain.model.RelayState
import com.smartnexus.nexusflow.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val relayStateDao: RelayStateDao
) : DeviceRepository {

    override fun getDevices(): Flow<List<Device>> {
        return deviceDao.getDevicesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDeviceById(id: String): Flow<Device?> {
        // Wrap query flow or convert nullable entity
        return deviceDao.getDevicesFlow().map { list ->
            list.find { it.id == id }?.toDomain()
        }
    }

    override suspend fun insertDevice(device: Device): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            deviceDao.insertDevice(device.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDevice(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            deviceDao.deleteDeviceById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRelayStates(deviceId: String): Flow<List<RelayState>> {
        return relayStateDao.getRelayStatesForDeviceFlow(deviceId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun updateRelayState(id: String, isOn: Boolean, isPending: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            relayStateDao.updateRelayState(id, isOn, isPending, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper extensions
    private fun DeviceEntity.toDomain() = Device(
        id = id,
        ownerUid = ownerUid,
        name = name,
        hardwareId = hardwareId,
        relayCount = relayCount,
        firmwareVersion = firmwareVersion,
        lastSeen = lastSeen,
        createdAt = createdAt
    )

    private fun Device.toEntity() = DeviceEntity(
        id = id,
        ownerUid = ownerUid,
        name = name,
        hardwareId = hardwareId,
        relayCount = relayCount,
        firmwareVersion = firmwareVersion,
        lastSeen = lastSeen,
        createdAt = createdAt,
        isSynced = false
    )

    private fun RelayStateEntity.toDomain() = RelayState(
        id = id,
        deviceId = deviceId,
        relayId = relayId,
        name = name,
        powerWatts = powerWatts,
        isOn = isOn,
        isPending = isPending,
        updatedAt = updatedAt
    )
}
