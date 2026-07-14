package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.BillingCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillingCycleDao {
    @Query("SELECT * FROM billing_cycles WHERE device_id = :deviceId ORDER BY start_date DESC")
    fun getBillingCyclesForDeviceFlow(deviceId: String): Flow<List<BillingCycleEntity>>

    @Query("SELECT * FROM billing_cycles WHERE device_id = :deviceId ORDER BY start_date DESC")
    fun getBillingCyclesForDevice(deviceId: String): List<BillingCycleEntity>

    @Query("SELECT * FROM billing_cycles WHERE device_id = :deviceId ORDER BY start_date DESC LIMIT 1")
    fun getLatestBillingCycleFlow(deviceId: String): Flow<BillingCycleEntity?>

    @Query("SELECT * FROM billing_cycles WHERE device_id = :deviceId ORDER BY start_date DESC LIMIT 1")
    fun getLatestBillingCycle(deviceId: String): BillingCycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBillingCycle(cycle: BillingCycleEntity): Long

    @Query("DELETE FROM billing_cycles WHERE device_id = :deviceId")
    fun deleteBillingCyclesForDevice(deviceId: String): Int
}
