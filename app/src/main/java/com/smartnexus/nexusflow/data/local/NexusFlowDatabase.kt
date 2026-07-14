package com.smartnexus.nexusflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartnexus.nexusflow.data.local.dao.*
import com.smartnexus.nexusflow.data.local.entity.*

@Database(
    entities = [
        AppConfigEntity::class,
        DeviceEntity::class,
        RelayStateEntity::class,
        ScheduleEntity::class,
        SceneEntity::class,
        SceneRelayStateEntity::class,
        AutomationRuleEntity::class,
        QueuedCommandEntity::class,
        RelayRuntimeEntity::class,
        BillingCycleEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NexusFlowDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun deviceDao(): DeviceDao
    abstract fun relayStateDao(): RelayStateDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sceneDao(): SceneDao
    abstract fun automationRuleDao(): AutomationRuleDao
    abstract fun commandQueueDao(): CommandQueueDao
    abstract fun relayRuntimeDao(): RelayRuntimeDao
    abstract fun billingCycleDao(): BillingCycleDao
}
