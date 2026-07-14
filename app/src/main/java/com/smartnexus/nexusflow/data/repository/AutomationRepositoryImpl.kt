package com.smartnexus.nexusflow.data.repository

import com.smartnexus.nexusflow.data.local.dao.AutomationRuleDao
import com.smartnexus.nexusflow.data.local.entity.AutomationRuleEntity
import com.smartnexus.nexusflow.domain.model.AutomationRule
import com.smartnexus.nexusflow.domain.repository.AutomationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepositoryImpl @Inject constructor(
    private val automationRuleDao: AutomationRuleDao
) : AutomationRepository {

    override fun getRulesForDevice(deviceId: String): Flow<List<AutomationRule>> {
        return automationRuleDao.getAutomationRulesForDeviceFlow(deviceId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertRule(rule: AutomationRule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Enforce 1-relay-1-rule constraint: check if another rule already has this target relay
            val deviceRules = automationRuleDao.getAutomationRulesForDevice(rule.deviceId)
            val alreadyConfigured = deviceRules.any { it.relayId == rule.relayId && it.id != rule.id }
            if (alreadyConfigured) {
                return@withContext Result.failure(IllegalStateException("A rule is already configured for this relay. Max 1 rule per relay."))
            }

            automationRuleDao.insertAutomationRule(rule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRule(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rule = automationRuleDao.getAutomationRuleById(id)
            if (rule != null) {
                automationRuleDao.deleteAutomationRule(rule)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRuleEnabled(id: String, isEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            automationRuleDao.updateRuleEnabledState(id, isEnabled, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper extensions
    private fun AutomationRuleEntity.toDomain() = AutomationRule(
        id = id,
        deviceId = deviceId,
        relayId = relayId,
        sensorMode = sensorMode,
        logicalOperator = logicalOperator,
        tempCondition = tempCondition,
        tempThreshold = tempThreshold,
        humidityCondition = humidityCondition,
        humidityThreshold = humidityThreshold,
        action = action,
        isEnabled = isEnabled,
        lastTriggeredAt = lastTriggeredAt,
        createdAt = createdAt
    )

    private fun AutomationRule.toEntity() = AutomationRuleEntity(
        id = id,
        deviceId = deviceId,
        relayId = relayId,
        sensorMode = sensorMode,
        logicalOperator = logicalOperator,
        tempCondition = tempCondition,
        tempThreshold = tempThreshold,
        humidityCondition = humidityCondition,
        humidityThreshold = humidityThreshold,
        action = action,
        isEnabled = isEnabled,
        lastTriggeredAt = lastTriggeredAt,
        createdAt = createdAt,
        isSynced = false
    )
}
