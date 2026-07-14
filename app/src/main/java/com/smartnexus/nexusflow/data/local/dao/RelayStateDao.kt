package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.RelayStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayStateDao {
    @Query("SELECT * FROM relay_states WHERE device_id = :deviceId ORDER BY relay_id ASC")
    fun getRelayStatesForDeviceFlow(deviceId: String): Flow<List<RelayStateEntity>>

    @Query("SELECT * FROM relay_states WHERE device_id = :deviceId ORDER BY relay_id ASC")
    fun getRelayStatesForDevice(deviceId: String): List<RelayStateEntity>

    @Query("SELECT * FROM relay_states WHERE id = :id LIMIT 1")
    fun getRelayStateById(id: String): RelayStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelayStates(relays: List<RelayStateEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelayState(relay: RelayStateEntity): Long

    @Query("UPDATE relay_states SET is_on = :isOn, is_pending = :isPending, updated_at = :updatedAt WHERE id = :id")
    fun updateRelayState(id: String, isOn: Boolean, isPending: Boolean, updatedAt: Long): Int

    @Query("UPDATE relay_states SET name = :newName, type = :type, power_watts = :powerWatts, updated_at = :updatedAt WHERE id = :id")
    fun updateRelayConfig(id: String, newName: String, type: String?, powerWatts: Int?, updatedAt: Long): Int
}
