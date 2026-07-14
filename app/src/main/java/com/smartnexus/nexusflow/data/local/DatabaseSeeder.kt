package com.smartnexus.nexusflow.data.local

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.smartnexus.nexusflow.data.local.dao.*
import kotlinx.coroutines.tasks.await

object DatabaseSeeder {
    suspend fun seedIfEmpty(
        deviceDao: DeviceDao,
        relayStateDao: RelayStateDao,
        automationRuleDao: AutomationRuleDao,
        scheduleDao: ScheduleDao,
        sceneDao: SceneDao,
        relayRuntimeDao: RelayRuntimeDao? = null,
        billingCycleDao: BillingCycleDao? = null
    ) {
        // No-op: Disable automatic mock data seeding.
    }
}
