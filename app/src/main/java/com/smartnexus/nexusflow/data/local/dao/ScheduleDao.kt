package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY start_time ASC")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE device_id = :deviceId ORDER BY start_time ASC")
    fun getSchedulesForDeviceFlow(deviceId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE device_id = :deviceId ORDER BY start_time ASC")
    fun getSchedulesForDevice(deviceId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    fun getScheduleById(id: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSchedule(schedule: ScheduleEntity): Long

    @Delete
    fun deleteSchedule(schedule: ScheduleEntity): Int

    @Query("UPDATE schedules SET is_enabled = :isEnabled, is_synced = :isSynced WHERE id = :id")
    fun updateScheduleEnabledState(id: String, isEnabled: Boolean, isSynced: Boolean): Int
}
