package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getDevices(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    fun getDeviceById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDeviceIgnore(device: DeviceEntity): Long

    @androidx.room.Update
    fun updateDevice(device: DeviceEntity): Int

    @androidx.room.Transaction
    fun insertDevice(device: DeviceEntity): Long {
        val id = insertDeviceIgnore(device)
        return if (id == -1L) {
            updateDevice(device)
            1L
        } else {
            id
        }
    }

    @Delete
    fun deleteDevice(device: DeviceEntity): Int

    @Query("DELETE FROM devices WHERE id = :id")
    fun deleteDeviceById(id: String): Int
}
