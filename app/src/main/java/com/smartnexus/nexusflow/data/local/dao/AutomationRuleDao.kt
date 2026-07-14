package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartnexus.nexusflow.data.local.entity.AutomationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {
    @Query("SELECT * FROM automation_rules WHERE device_id = :deviceId ORDER BY created_at DESC")
    fun getAutomationRulesForDeviceFlow(deviceId: String): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE device_id = :deviceId ORDER BY created_at DESC")
    fun getAutomationRulesForDevice(deviceId: String): List<AutomationRuleEntity>

    @Query("SELECT * FROM automation_rules WHERE id = :id LIMIT 1")
    fun getAutomationRuleById(id: String): AutomationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAutomationRule(rule: AutomationRuleEntity): Long

    @Delete
    fun deleteAutomationRule(rule: AutomationRuleEntity): Int

    @Query("UPDATE automation_rules SET is_enabled = :isEnabled, is_synced = :isSynced WHERE id = :id")
    fun updateRuleEnabledState(id: String, isEnabled: Boolean, isSynced: Boolean): Int
}
