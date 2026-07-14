package com.smartnexus.nexusflow.features.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.domain.model.DeviceType
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.smartnexus.nexusflow.data.local.DatabaseSeeder
import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import com.smartnexus.nexusflow.data.local.dao.AutomationRuleDao
import com.smartnexus.nexusflow.data.local.dao.RelayRuntimeDao
import com.smartnexus.nexusflow.data.local.dao.BillingCycleDao
import com.smartnexus.nexusflow.data.local.dao.ScheduleDao
import com.smartnexus.nexusflow.data.local.dao.SceneDao
import com.smartnexus.nexusflow.data.local.entity.DeviceEntity
import com.smartnexus.nexusflow.data.local.entity.RelayStateEntity
import com.smartnexus.nexusflow.data.local.entity.BillingCycleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

data class RelayChannelItem(
    val channelNumber: Int,
    val channelLabel: String,
    val name: String,
    val isSystemOn: Boolean,
    val deviceType: DeviceType
)

data class DeviceDetailsData(
    val id: String,
    val name: String,
    val channels: Int,
    val isOnline: Boolean,
    val wifiConnected: Boolean,
    val bleNearby: Boolean,
    val deviceType: DeviceType = DeviceType.DEFAULT,
    val wifiSignal: String = "Excellent",
    val bleStatus: String = "Connected",
    val lastSeen: String = "Just now",
    val uptime: String = "2d 14h 32m",
    val temperature: String = "28.4°C",
    val humidity: String = "68%",
    val humidex: String = "28.7°C",
    val hardwareId: String = "SN-88CH-4F2A",
    val macAddress: String = "DC:4F:22:7A:4F:2A",
    val firmware: String = "v1.2.3",
    val deviceTypeLabel: String = "NexusFlow 8CH Pro",
    val addedDate: String = "12 Jun 2026, 10:30 AM",
    val timeZone: String = "Asia/Kolkata (GMT+5:30)",
    val relayChannels: List<RelayChannelItem> = emptyList(),
    val energyKwh: Double = 0.0,
    val costRs: Double = 0.0,
    val startDate: String = "01 Jul 2026",
    val runningFor: String = "0d 0h 0m",
    val consumptionSegments: List<com.smartnexus.nexusflow.core.components.PowerConsumptionSegment> = emptyList()
)

data class DeviceListItem(
    val id: String,
    val name: String,
    val channels: Int,
    val isOnline: Boolean,
    val lastSeenText: String,
    val bleConnected: Boolean,
    val wifiOnline: Boolean,
    val deviceType: DeviceType = DeviceType.DEFAULT,
    val iconBgType: String = "purple", // "purple" or "orange"
    val details: DeviceDetailsData
)

data class DevicesUiState(
    val devices: List<DeviceListItem> = emptyList(),
    val maxDeviceLimit: Int = 4,
    val selectedDeviceForMenu: DeviceListItem? = null,
    val editingDevice: DeviceListItem? = null,
    val activeDetailsDevice: DeviceDetailsData? = null
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceDao: DeviceDao,
    private val relayStateDao: RelayStateDao,
    private val automationRuleDao: AutomationRuleDao,
    private val relayRuntimeDao: RelayRuntimeDao,
    private val billingCycleDao: BillingCycleDao,
    private val scheduleDao: ScheduleDao,
    private val sceneDao: SceneDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed database with mock data if empty
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DatabaseSeeder.seedIfEmpty(deviceDao, relayStateDao, automationRuleDao, scheduleDao, sceneDao, relayRuntimeDao, billingCycleDao)
            }

            // Stream devices and their channel states reactively
            combine(
                deviceDao.getDevicesFlow(),
                relayStateDao.getRelayStatesForDeviceFlow("nf_lr8") // dummy combine to trigger updates on any relay change
            ) { dbDevices, _ ->
                dbDevices.filter { it.id != "nf_kt4" }
            }.collectLatest { dbDevices ->
                val listItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dbDevices.map { dev ->
                        val relays = relayStateDao.getRelayStatesForDevice(dev.id)
                        val details = toDetailsData(dev, relays)
                        val isOnline = isDeviceOnline(dev.lastSeen)
                        DeviceListItem(
                            id = dev.id,
                            name = dev.name,
                            channels = dev.relayCount,
                            isOnline = isOnline,
                            lastSeenText = getLastSeenText(dev.lastSeen),
                            bleConnected = isOnline,
                            wifiOnline = isOnline,
                            deviceType = DeviceType.fromName(dev.deviceType),
                            iconBgType = if (dev.deviceType.contains("Kitchen", ignoreCase = true) || dev.name.contains("Kitchen")) "orange" else "purple",
                            details = details
                        )
                    }
                }

                _uiState.update { state ->
                    val updatedActiveDetails = state.activeDetailsDevice?.let { active ->
                        listItems.find { it.id == active.id }?.details
                    }
                    state.copy(
                        devices = listItems,
                        activeDetailsDevice = updatedActiveDetails
                    )
                }
            }
        }
    }

    fun onOpenContextMenu(device: DeviceListItem) {
        _uiState.update { it.copy(selectedDeviceForMenu = device) }
    }

    fun onCloseContextMenu() {
        _uiState.update { it.copy(selectedDeviceForMenu = null) }
    }

    fun onOpenEditDevice(device: DeviceListItem) {
        _uiState.update {
            it.copy(
                selectedDeviceForMenu = null,
                editingDevice = device
            )
        }
    }

    fun onCloseEditDevice() {
        _uiState.update { it.copy(editingDevice = null) }
    }

    fun onSaveDeviceChanges(deviceId: String, newName: String, newType: DeviceType) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val dev = deviceDao.getDeviceById(deviceId)
                if (dev != null) {
                    deviceDao.insertDevice(dev.copy(name = newName, deviceType = newType.name))
                    
                    // Sync the name and type/icon to Firebase Realtime Database
                    val ref = FirebaseDatabase.getInstance().getReference("devices/$deviceId")
                    ref.child("name").setValue(newName)
                    ref.child("deviceType").setValue(newType.name)
                }
            }
            _uiState.update { it.copy(editingDevice = null) }
        }
    }

    fun onDeleteDevice(deviceId: String) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                deviceDao.deleteDeviceById(deviceId)
            }
            _uiState.update {
                it.copy(
                    selectedDeviceForMenu = null,
                    activeDetailsDevice = if (it.activeDetailsDevice?.id == deviceId) null else it.activeDetailsDevice
                )
            }
        }
    }

    fun onOpenDeviceDetails(deviceDetails: DeviceDetailsData) {
        _uiState.update { it.copy(activeDetailsDevice = deviceDetails) }
    }

    fun onCloseDeviceDetails() {
        _uiState.update { it.copy(activeDetailsDevice = null) }
    }

    fun onToggleRelayChannel(channelNumber: Int) {
        viewModelScope.launch {
            val currentDetails = _uiState.value.activeDetailsDevice ?: return@launch
            val channelIndex = channelNumber - 1
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val relays = relayStateDao.getRelayStatesForDevice(currentDetails.id)
                if (channelIndex in relays.indices) {
                    val targetRelay = relays[channelIndex]
                    relayStateDao.updateRelayState(
                        id = targetRelay.id,
                        isOn = !targetRelay.isOn,
                        isPending = false,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun onResetBillingCycle(deviceId: String) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val todayDate = dateFormat.format(Date())
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val runtimes = relayRuntimeDao.getRuntimesForDevice(deviceId)
                val totalLifetimeMinutes = runtimes.sumOf { it.lifetimeMinutes }
                billingCycleDao.insertBillingCycle(
                    BillingCycleEntity(
                        id = "cycle_${System.currentTimeMillis()}",
                        deviceId = deviceId,
                        startDate = todayDate,
                        startRuntime = totalLifetimeMinutes
                    )
                )

                // Sync to Firebase
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("device_states/$deviceId/billing_cycle")
                        .setValue(mapOf(
                            "startDate" to todayDate,
                            "startRuntime" to totalLifetimeMinutes
                        ))
                }
            }
        }
    }

    private fun toDetailsData(dev: DeviceEntity, relays: List<RelayStateEntity>): DeviceDetailsData {
        val isOnline = isDeviceOnline(dev.lastSeen)
        val formattedMac = when (dev.hardwareId) {
            "SN-8CH-A448" -> "3C:8A:1F:A8:A4:48"
            "SN-6CH-DBAC" -> "D4:E9:F4:BA:DB:AC"
            "SN-4CH-D398" -> "30:C6:F7:2F:D3:98"
            else -> {
                val macHex = dev.hardwareId.substringAfterLast("-", "4F2A")
                "DC:4F:22:7A:${macHex.chunked(2).joinToString(":")}"
            }
        }

        // Calculate power usage dynamically from DB
        val runtimes = relayRuntimeDao.getRuntimesForDevice(dev.id)
        val latestCycle = billingCycleDao.getLatestBillingCycle(dev.id)
        
        val startRuntime = latestCycle?.startRuntime ?: 0L
        val cycleStartDate = latestCycle?.startDate ?: "01 Jul 2026"
        
        val totalLifetimeMinutes = runtimes.sumOf { it.lifetimeMinutes }
        val startRatio = if (totalLifetimeMinutes > 0) {
            (startRuntime.toDouble() / totalLifetimeMinutes).coerceAtMost(1.0)
        } else 0.0
        
        var totalCycleKwh = 0.0
        val relayKwhList = runtimes.map { runtime ->
            val state = relays.find { it.id == runtime.relayId }
            val powerWatts = state?.powerWatts ?: 50
            val lifetimeKwh = (runtime.lifetimeMinutes / 60.0) * (powerWatts / 1000.0)
            val cycleKwh = lifetimeKwh * (1.0 - startRatio)
            val netKwh = if (cycleKwh > 0.0) cycleKwh else 0.0
            totalCycleKwh += netKwh
            val friendlyName = state?.name ?: ("CH " + runtime.relayId.substringAfterLast("_"))
            Pair(friendlyName, netKwh)
        }
        
        val totalCost = totalCycleKwh * 8.0 // ₹ 8 per kWh
        
        val colorsList = listOf(
            Color(0xFF10B981), // Emerald
            Color(0xFF3B82F6), // Blue
            Color(0xFFF59E0B), // Amber
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEF4444), // Red
            Color(0xFF9CA3AF)  // Grey
        )
        
        // Calculate Top 3 + Others segment list
        val sortedKwhList = relayKwhList.sortedByDescending { it.second }
        val segments = if (totalCycleKwh > 0) {
            val top3 = sortedKwhList.take(3)
            val othersSum = sortedKwhList.drop(3).sumOf { it.second }
            val finalSegments = mutableListOf<com.smartnexus.nexusflow.core.components.PowerConsumptionSegment>()
            
            top3.forEachIndexed { idx, pair ->
                val pct = ((pair.second / totalCycleKwh) * 100).toInt()
                if (pct > 0) {
                    val color = colorsList.getOrElse(idx) { Color(0xFF9CA3AF) }
                    finalSegments.add(com.smartnexus.nexusflow.core.components.PowerConsumptionSegment(pair.first, pct, color))
                }
            }
            if (othersSum > 0) {
                val othersPct = ((othersSum / totalCycleKwh) * 100).toInt()
                if (othersPct > 0) {
                    finalSegments.add(com.smartnexus.nexusflow.core.components.PowerConsumptionSegment("Others", othersPct, Color(0xFF9CA3AF)))
                }
            }
            finalSegments
        } else {
            val top3 = relays.take(3)
            val othersCount = relays.size - 3
            val finalSegments = mutableListOf<com.smartnexus.nexusflow.core.components.PowerConsumptionSegment>()
            val basePct = if (relays.isNotEmpty()) 100 / (top3.size + if (othersCount > 0) 1 else 0) else 100
            
            top3.forEachIndexed { idx, relay ->
                val color = colorsList.getOrElse(idx) { Color(0xFF9CA3AF) }
                finalSegments.add(com.smartnexus.nexusflow.core.components.PowerConsumptionSegment(relay.name, basePct, color))
            }
            if (othersCount > 0) {
                finalSegments.add(com.smartnexus.nexusflow.core.components.PowerConsumptionSegment("Others", basePct, Color(0xFF9CA3AF)))
            }
            finalSegments
        }
        
        val cycleStartMillis = latestCycle?.let {
            try {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(it.startDate)?.time
            } catch (e: Exception) {
                null
            }
        } ?: (System.currentTimeMillis() - 6 * 24 * 3600 * 1000L)
        
        val diff = System.currentTimeMillis() - cycleStartMillis
        val days = diff / (24 * 3600 * 1000L)
        val hours = (diff % (24 * 3600 * 1000L)) / (3600 * 1000L)
        val minutes = (diff % (3600 * 1000L)) / (60 * 1000L)
        val runningForText = "${days}d ${hours}h ${minutes}m"

        return DeviceDetailsData(
            id = dev.id,
            name = dev.name,
            channels = dev.relayCount,
            isOnline = isOnline,
            wifiConnected = isOnline,
            bleNearby = isOnline,
            deviceType = DeviceType.fromName(dev.name),
            wifiSignal = if (isOnline) "Excellent" else "Offline",
            bleStatus = if (isOnline) "Connected" else "Disconnected",
            lastSeen = getLastSeenText(dev.lastSeen),
            uptime = if (isOnline && dev.uptimeSeconds > 0) {
                val upSec = dev.uptimeSeconds
                val upDays = upSec / (24 * 3600)
                val upHours = (upSec % (24 * 3600)) / 3600
                val upMins = (upSec % 3600) / 60
                "${upDays}d ${upHours}h ${upMins}m"
            } else if (isOnline) {
                "0d 00h 00m"
            } else {
                "Offline"
            },
            temperature = if (isOnline) String.format(Locale.US, "%.1f°C", dev.temperature) else "-- °C",
            humidity = if (isOnline) String.format(Locale.US, "%.0f%%", dev.humidity) else "-- %",
            humidex = if (isOnline) String.format(Locale.US, "%.1f°C", dev.humidex) else "-- °C",
            hardwareId = dev.hardwareId,
            macAddress = formattedMac,
            firmware = dev.firmwareVersion,
            deviceTypeLabel = if (dev.relayCount == 8) "NexusFlow 8CH Pro" else if (dev.relayCount == 6) "NexusFlow 6CH Pro" else "NexusFlow 4CH Standard",
            addedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dev.createdAt)),
            timeZone = "Asia/Kolkata (GMT+5:30)",
            relayChannels = relays.mapIndexed { idx, relay ->
                RelayChannelItem(
                    channelNumber = idx + 1,
                    channelLabel = "CH ${idx + 1}",
                    name = relay.name,
                    isSystemOn = relay.isOn,
                    deviceType = DeviceType.fromName(relay.name)
                )
            },
            energyKwh = totalCycleKwh,
            costRs = totalCost,
            startDate = cycleStartDate,
            runningFor = runningForText,
            consumptionSegments = segments
        )
    }

    private fun isDeviceOnline(lastSeen: Long): Boolean {
        return System.currentTimeMillis() - lastSeen < 5 * 60 * 1000L
    }

    private fun getLastSeenText(lastSeen: Long): String {
        val diffSec = (System.currentTimeMillis() - lastSeen) / 1000L
        return when {
            diffSec < 60 -> "Just now"
            diffSec < 3600 -> "${diffSec / 60} min ago"
            diffSec < 86400 -> "${diffSec / 3600} hrs ago"
            else -> "${diffSec / 86400} days ago"
        }
    }
}
