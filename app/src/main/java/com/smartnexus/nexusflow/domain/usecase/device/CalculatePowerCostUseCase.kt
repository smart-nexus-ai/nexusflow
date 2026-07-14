package com.smartnexus.nexusflow.domain.usecase.device

import com.smartnexus.nexusflow.data.local.dao.BillingCycleDao
import com.smartnexus.nexusflow.data.local.dao.RelayRuntimeDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

data class PowerCostResult(
    val energyKwh: Double,
    val costRs: Double,
    val startDateText: String,
    val runningForText: String
)

class CalculatePowerCostUseCase @Inject constructor(
    private val billingCycleDao: BillingCycleDao,
    private val relayRuntimeDao: RelayRuntimeDao,
    private val relayStateDao: RelayStateDao
) {
    operator fun invoke(deviceId: String, tariffRate: Double = 8.0): Flow<PowerCostResult> {
        return combine(
            billingCycleDao.getLatestBillingCycleFlow(deviceId),
            relayRuntimeDao.getRuntimesForDeviceFlow(deviceId),
            relayStateDao.getRelayStatesForDeviceFlow(deviceId)
        ) { latestCycle, runtimes, relayStates ->
            val startRuntime = latestCycle?.startRuntime ?: 0L
            val startDate = latestCycle?.startDate ?: "01 Jun 2026"
            
            // Calculate total accumulated lifetime minutes on the device
            val totalLifetimeMinutes = runtimes.sumOf { it.lifetimeMinutes }
            
            // Proportional starting consumption factor based on cycle start point
            val startRatio = if (totalLifetimeMinutes > 0) {
                startRuntime.toDouble() / totalLifetimeMinutes
            } else 0.0
            
            var totalCycleKwh = 0.0
            
            runtimes.forEach { runtime ->
                val state = relayStates.find { it.relayId == runtime.relayId }
                val powerWatts = state?.powerWatts ?: 50 // default to 50W if unconfigured
                
                // Absolute total energy consumed by this relay in its lifetime (in kWh)
                val lifetimeKwh = (runtime.lifetimeMinutes / 60.0) * (powerWatts / 1000.0)
                
                // Subtract proportional offset from before this cycle started
                val cycleKwh = lifetimeKwh * (1.0 - startRatio)
                if (cycleKwh > 0.0) {
                    totalCycleKwh += cycleKwh
                }
            }
            
            val totalCost = totalCycleKwh * tariffRate
            
            // Running time display
            val cycleStartMillis = latestCycle?.let {
                // Parse date or fallback to 6 days ago (mock date mapping helper)
                try {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    sdf.parse(it.startDate)?.time
                } catch (e: Exception) {
                    null
                }
            } ?: (System.currentTimeMillis() - 6 * 24 * 3600 * 1000L)
            
            val diff = System.currentTimeMillis() - cycleStartMillis
            val days = diff / (24 * 3600 * 1000L)
            val hours = (diff % (24 * 3600 * 1000L)) / (3600 * 1000L)
            val minutes = (diff % (3600 * 1000L)) / (60 * 1000L)
            
            PowerCostResult(
                energyKwh = totalCycleKwh,
                costRs = totalCost,
                startDateText = startDate,
                runningForText = "${days}d ${hours}h ${minutes}m"
            )
        }.flowOn(Dispatchers.Default)
    }
}
