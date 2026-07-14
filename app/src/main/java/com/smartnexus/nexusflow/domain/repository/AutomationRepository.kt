package com.smartnexus.nexusflow.domain.repository

import com.smartnexus.nexusflow.domain.model.AutomationRule
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun getRulesForDevice(deviceId: String): Flow<List<AutomationRule>>
    suspend fun insertRule(rule: AutomationRule): Result<Unit>
    suspend fun deleteRule(id: String): Result<Unit>
    suspend fun updateRuleEnabled(id: String, isEnabled: Boolean): Result<Unit>
}
