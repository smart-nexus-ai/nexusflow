package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.RelayRuntimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayRuntimeDao {
    @Query("SELECT * FROM relay_runtime WHERE device_id = :deviceId")
    fun getRuntimesForDeviceFlow(deviceId: String): Flow<List<RelayRuntimeEntity>>

    @Query("SELECT * FROM relay_runtime WHERE device_id = :deviceId")
    fun getRuntimesForDevice(deviceId: String): List<RelayRuntimeEntity>

    @Query("SELECT * FROM relay_runtime WHERE relay_id = :relayId LIMIT 1")
    fun getRuntimeByRelayId(relayId: String): RelayRuntimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRuntime(runtime: RelayRuntimeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRuntimes(runtimes: List<RelayRuntimeEntity>): List<Long>
}
