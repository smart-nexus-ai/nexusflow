package com.smartnexus.nexusflow.data.remote

import android.util.Log
import com.google.firebase.database.*
import com.smartnexus.nexusflow.data.local.dao.*
import com.smartnexus.nexusflow.data.local.entity.*
import com.smartnexus.nexusflow.data.remote.firebase.FirebaseAuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeListenerManager @Inject constructor(
    private val authService: FirebaseAuthService,
    private val deviceDao: DeviceDao,
    private val relayStateDao: RelayStateDao,
    private val automationRuleDao: AutomationRuleDao,
    private val scheduleDao: ScheduleDao,
    private val sceneDao: SceneDao,
    private val relayRuntimeDao: RelayRuntimeDao,
    private val billingCycleDao: BillingCycleDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    
    // Track active database listeners by their reference paths to avoid leaks
    private val activeListeners = mutableMapOf<String, ValueEventListener>()

    fun startSync() {
        if (syncJob != null) return
        syncJob = scope.launch {
            authService.currentUserFlow.collectLatest { user ->
                cleanupAllListeners()
                if (user != null) {
                    Log.d("RealtimeListenerManager", "User logged in: ${user.uid}, starting RTDB sync...")
                    observeUserDevicesList(user.uid)
                    observeUserScenes(user.uid)
                } else {
                    Log.d("RealtimeListenerManager", "User logged out, stopping RTDB sync...")
                }
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        cleanupAllListeners()
    }

    private fun observeUserDevicesList(ownerUid: String) {
        val ref = FirebaseDatabase.getInstance().getReference("users/$ownerUid/devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentDeviceIds = mutableSetOf<String>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val deviceId = child.key ?: continue
                        currentDeviceIds.add(deviceId)
                    }
                }

                // Sync listeners for active devices
                manageDeviceListeners(currentDeviceIds, ownerUid)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "Failed to sync user devices list", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["users/$ownerUid/devices"] = listener
    }

    private fun manageDeviceListeners(currentDeviceIds: Set<String>, ownerUid: String) {
        // Unregister listeners for devices that are no longer paired
        val prefixDevices = "devices/"
        val prefixStates = "device_states/"
        val prefixSchedules = "schedules/"
        val prefixRules = "automation_rules/"

        val keysToRemove = mutableListOf<String>()
        for (path in activeListeners.keys) {
            val deviceId = when {
                path.startsWith(prefixDevices) -> path.substringAfter(prefixDevices)
                path.startsWith(prefixStates) -> path.substringAfter(prefixStates)
                path.startsWith(prefixSchedules) -> path.substringAfter(prefixSchedules)
                path.startsWith(prefixRules) -> path.substringAfter(prefixRules)
                else -> null
            }
            if (deviceId != null && !currentDeviceIds.contains(deviceId)) {
                val ref = FirebaseDatabase.getInstance().getReference(path)
                activeListeners[path]?.let { ref.removeEventListener(it) }
                keysToRemove.add(path)
            }
        }
        keysToRemove.forEach { activeListeners.remove(it) }

        // Start listeners for newly discovered devices
        for (deviceId in currentDeviceIds) {
            val devPath = "devices/$deviceId"
            if (!activeListeners.containsKey(devPath)) {
                observeDeviceMetadata(deviceId, ownerUid)
            }
            val statePath = "device_states/$deviceId"
            if (!activeListeners.containsKey(statePath)) {
                observeDeviceStates(deviceId)
            }
            val schedulePath = "schedules/$deviceId"
            if (!activeListeners.containsKey(schedulePath)) {
                observeSchedules(deviceId)
            }
            val rulesPath = "automation_rules/$deviceId"
            if (!activeListeners.containsKey(rulesPath)) {
                observeAutomationRules(deviceId)
            }
        }
    }

    private fun observeDeviceMetadata(deviceId: String, ownerUid: String) {
        val ref = FirebaseDatabase.getInstance().getReference("devices/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val name = snapshot.child("name").getValue(String::class.java) ?: "Device"
                val deviceType = snapshot.child("deviceType").getValue(String::class.java)
                    ?: snapshot.child("device_type").getValue(String::class.java) ?: "DEFAULT"
                val hardwareId = snapshot.child("hardwareId").getValue(String::class.java)
                    ?: snapshot.child("hardware_id").getValue(String::class.java) ?: ""
                val relayCount = snapshot.child("relayCount").getValue(Int::class.java)
                    ?: snapshot.child("relay_count").getValue(Int::class.java) ?: 6
                val firmwareVersion = snapshot.child("firmwareVersion").getValue(String::class.java)
                    ?: snapshot.child("firmware_version").getValue(String::class.java) ?: "1.0.0"
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                    ?: snapshot.child("last_seen").getValue(Long::class.java) ?: System.currentTimeMillis()
                val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                    ?: snapshot.child("created_at").getValue(Long::class.java) ?: System.currentTimeMillis()

                scope.launch {
                    val existing = deviceDao.getDeviceById(deviceId)
                    val deviceEntity = DeviceEntity(
                        id = deviceId,
                        ownerUid = ownerUid,
                        name = name,
                        hardwareId = hardwareId,
                        relayCount = relayCount,
                        firmwareVersion = firmwareVersion,
                        lastSeen = lastSeen,
                        createdAt = createdAt,
                        isSynced = true,
                        temperature = existing?.temperature ?: 25.0f,
                        humidity = existing?.humidity ?: 60.0f,
                        humidex = existing?.humidex ?: 25.0f,
                        uptimeSeconds = existing?.uptimeSeconds ?: 0L,
                        deviceType = deviceType
                    )
                    deviceDao.insertDevice(deviceEntity)

                    // Parse dynamic relay configuration if present in metadata
                    val relayConfigSnap = snapshot.child("relay_config")
                    if (relayConfigSnap.exists()) {
                        for (relaySnap in relayConfigSnap.children) {
                            val relayId = relaySnap.key ?: continue // e.g. "relay_1"
                            val relayName = relaySnap.child("name").getValue(String::class.java) ?: "Relay"
                            val powerWatts = relaySnap.child("powerWatts").getValue(Int::class.java)
                                ?: relaySnap.child("power_watts").getValue(Int::class.java) ?: 50
                            val relayType = relaySnap.child("type").getValue(String::class.java) ?: "switch"
                            
                            val compositeId = "${deviceId}_$relayId"
                            val existingRelay = relayStateDao.getRelayStateById(compositeId)
                            val relayEntity = RelayStateEntity(
                                id = compositeId,
                                deviceId = deviceId,
                                relayId = relayId,
                                name = relayName,
                                powerWatts = powerWatts,
                                type = relayType,
                                isOn = existingRelay?.isOn ?: false,
                                isPending = existingRelay?.isPending ?: false,
                                updatedAt = System.currentTimeMillis()
                            )
                            relayStateDao.insertRelayState(relayEntity)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "observeDeviceMetadata cancelled for $deviceId: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["devices/$deviceId"] = listener
    }

    private fun observeDeviceStates(deviceId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("device_states/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                // 1. Update sensors and uptime on the device
                val sensorsSnap = snapshot.child("sensors")
                val uptimeSec = snapshot.child("uptime_seconds").getValue(Long::class.java)
                    ?: snapshot.child("uptimeSeconds").getValue(Long::class.java)

                if (sensorsSnap.exists() || uptimeSec != null) {
                    val temp = sensorsSnap.child("temperature").getValue(Float::class.java) ?: 25.0f
                    val hum = sensorsSnap.child("humidity").getValue(Float::class.java) ?: 60.0f
                    val hdx = sensorsSnap.child("humidex").getValue(Float::class.java) ?: 25.0f
                    scope.launch {
                        val existing = deviceDao.getDeviceById(deviceId)
                        if (existing != null) {
                            deviceDao.insertDevice(existing.copy(
                                temperature = temp,
                                humidity = hum,
                                humidex = hdx,
                                uptimeSeconds = uptimeSec ?: existing.uptimeSeconds
                            ))
                        }
                    }
                }

                // 2. Update relays
                val relaysSnap = snapshot.child("relays")
                if (relaysSnap.exists()) {
                    for (relaySnap in relaysSnap.children) {
                        val relayId = relaySnap.key ?: continue
                        val isOn = relaySnap.getValue(Boolean::class.java) ?: false
                        scope.launch {
                            val compositeId = "${deviceId}_$relayId"
                            val existingRelay = relayStateDao.getRelayStateById(compositeId)
                            if (existingRelay != null) {
                                relayStateDao.insertRelayState(existingRelay.copy(
                                    isOn = isOn,
                                    isPending = false,
                                    updatedAt = System.currentTimeMillis()
                                ))
                            } else {
                                 val relayEntity = RelayStateEntity(
                                    id = compositeId,
                                    deviceId = deviceId,
                                    relayId = relayId,
                                    name = relayId.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    powerWatts = 50,
                                    type = "switch",
                                    isOn = isOn,
                                    isPending = false,
                                    updatedAt = System.currentTimeMillis()
                                )
                                relayStateDao.insertRelayState(relayEntity)
                            }
                        }
                    }
                }

                // 3. Update runtimes
                val runtimeSnap = snapshot.child("runtime")
                if (runtimeSnap.exists()) {
                    for (relaySnap in runtimeSnap.children) {
                        val relayId = relaySnap.key ?: continue
                        val lifetimeMin = relaySnap.child("lifetimeMinutes").getValue(Long::class.java) ?: 0L
                        scope.launch {
                            val compositeId = "${deviceId}_$relayId"
                            val runtimeEntity = RelayRuntimeEntity(
                                relayId = compositeId,
                                deviceId = deviceId,
                                lifetimeMinutes = lifetimeMin,
                                lastUpdated = System.currentTimeMillis(),
                                isSynced = true
                            )
                            relayRuntimeDao.insertRuntime(runtimeEntity)
                        }
                    }
                }

                // 4. Update billing cycle reset state
                val billingCycleSnap = snapshot.child("billing_cycle")
                if (billingCycleSnap.exists()) {
                    val startDate = billingCycleSnap.child("startDate").getValue(String::class.java) ?: "01 Jul 2026"
                    val startRuntime = billingCycleSnap.child("startRuntime").getValue(Long::class.java) ?: 0L
                    scope.launch {
                        val existing = billingCycleDao.getLatestBillingCycle(deviceId)
                        if (existing == null || existing.startDate != startDate || existing.startRuntime != startRuntime) {
                            billingCycleDao.insertBillingCycle(
                                BillingCycleEntity(
                                    id = existing?.id ?: "cycle_${System.currentTimeMillis()}",
                                    deviceId = deviceId,
                                    startDate = startDate,
                                    startRuntime = startRuntime
                                )
                            )
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "observeDeviceStates cancelled for $deviceId: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["device_states/$deviceId"] = listener
    }

    private fun observeSchedules(deviceId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("schedules/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val localSchedules = scheduleDao.getSchedulesForDevice(deviceId)
                    val remoteIds = mutableSetOf<String>()
                    
                    if (snapshot.exists()) {
                        for (scheduleSnap in snapshot.children) {
                            val scheduleId = scheduleSnap.key ?: continue
                            remoteIds.add(scheduleId)
                            
                             val relayId = scheduleSnap.child("relayId").getValue(String::class.java) ?: "relay_1"
                             val action = scheduleSnap.child("action").getValue(Boolean::class.java) ?: false
                             val isEnabled = scheduleSnap.child("isEnabled").getValue(Boolean::class.java) ?: true
                             val startTime = scheduleSnap.child("startTime").getValue(Long::class.java)?.toInt() ?: 0
                             val endTime = scheduleSnap.child("endTime").getValue(Long::class.java)?.toInt()?.takeIf { it >= 0 }
                             val endDate = scheduleSnap.child("endDate").getValue(String::class.java)
                             val scheduleType = scheduleSnap.child("scheduleType").getValue(String::class.java)
                                 ?: if (endTime != null) "Duration Window" else "Specific Time"
                             
                             // Parse daysOfWeek
                             val daysOfWeek = mutableListOf<Int>()
                             val daysOfWeekSnap = scheduleSnap.child("daysOfWeek")
                             if (daysOfWeekSnap.exists()) {
                                 for (daySnap in daysOfWeekSnap.children) {
                                     val day = daySnap.getValue(Int::class.java) ?: continue
                                     daysOfWeek.add(day)
                                 }
                             }
                             
                             val scheduleEntity = ScheduleEntity(
                                 id = scheduleId,
                                 deviceId = deviceId,
                                 relayId = relayId,
                                 action = action,
                                 startTime = startTime,
                                 endTime = endTime,
                                 daysOfWeek = daysOfWeek,
                                 isEnabled = isEnabled,
                                 createdAt = scheduleSnap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                                 isSynced = true,
                                 endDate = endDate,
                                 scheduleType = scheduleType
                             )
                             scheduleDao.insertSchedule(scheduleEntity)
                        }
                    }
                    
                    // Remove local schedules that were deleted on remote
                    for (localSch in localSchedules) {
                        if (!remoteIds.contains(localSch.id)) {
                            scheduleDao.deleteSchedule(localSch)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "observeSchedules cancelled for $deviceId: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["schedules/$deviceId"] = listener
    }

    private fun observeAutomationRules(deviceId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("automation_rules/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val localRules = automationRuleDao.getAutomationRulesForDevice(deviceId)
                    val remoteIds = mutableSetOf<String>()
                    
                    if (snapshot.exists()) {
                        for (ruleSnap in snapshot.children) {
                            val ruleId = ruleSnap.key ?: continue
                            remoteIds.add(ruleId)
                            
                            val relayId = ruleSnap.child("relayId").getValue(String::class.java) ?: "relay_1"
                            val sensorMode = ruleSnap.child("sensorMode").getValue(String::class.java) ?: "temperature"
                            val logicalOperator = ruleSnap.child("logicalOperator").getValue(String::class.java)
                            val tempCondition = ruleSnap.child("tempCondition").getValue(String::class.java)
                            val tempThreshold = ruleSnap.child("tempThreshold").getValue(Double::class.java)
                            val humidityCondition = ruleSnap.child("humidityCondition").getValue(String::class.java)
                            val humidityThreshold = ruleSnap.child("humidityThreshold").getValue(Double::class.java)
                            val action = ruleSnap.child("action").getValue(Boolean::class.java) ?: false
                            val isEnabled = ruleSnap.child("isEnabled").getValue(Boolean::class.java) ?: true
                            val lastTriggeredAt = ruleSnap.child("lastTriggeredAt").getValue(Long::class.java)
                            val ruleCreatedAt = ruleSnap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            
                            val ruleEntity = AutomationRuleEntity(
                                id = ruleId,
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
                                createdAt = ruleCreatedAt,
                                isSynced = true
                            )
                            automationRuleDao.insertAutomationRule(ruleEntity)
                        }
                    }
                    
                    // Remove local rules that were deleted on remote
                    for (localRule in localRules) {
                        if (!remoteIds.contains(localRule.id)) {
                            automationRuleDao.deleteAutomationRule(localRule)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "observeAutomationRules cancelled for $deviceId: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["automation_rules/$deviceId"] = listener
    }

    private fun observeUserScenes(ownerUid: String) {
        val ref = FirebaseDatabase.getInstance().getReference("scenes/$ownerUid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val localScenes = sceneDao.getAllScenes()
                    val remoteIds = mutableSetOf<String>()

                    if (snapshot.exists()) {
                        for (sceneSnap in snapshot.children) {
                            val sceneId = sceneSnap.key ?: continue
                            remoteIds.add(sceneId)

                            val deviceId = sceneSnap.child("deviceId").getValue(String::class.java) ?: ""
                            if (deviceDao.getDeviceById(deviceId) == null) {
                                Log.w("RealtimeListenerManager", "Skipping scene $sceneId because device $deviceId does not exist locally")
                                continue
                            }
                            val name = sceneSnap.child("name").getValue(String::class.java) ?: "Scene"
                            val icon = sceneSnap.child("icon").getValue(String::class.java) ?: "default"
                            val isFavorite = sceneSnap.child("isFavorite").getValue(Boolean::class.java) ?: false
                            val showOnHome = sceneSnap.child("showOnHome").getValue(Boolean::class.java) ?: false
                            val createdAt = sceneSnap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val lastUsedAt = sceneSnap.child("lastUsedAt").getValue(Long::class.java)

                            val sceneEntity = SceneEntity(
                                id = sceneId,
                                deviceId = deviceId,
                                name = name,
                                icon = icon,
                                isFavorite = isFavorite,
                                showOnHome = showOnHome,
                                createdAt = createdAt,
                                lastUsedAt = lastUsedAt,
                                isSynced = true
                            )
                            sceneDao.insertScene(sceneEntity)

                            // Parse child scene relay states
                            val relaysSnap = sceneSnap.child("relays")
                            if (relaysSnap.exists()) {
                                val sceneRelayStates = mutableListOf<SceneRelayStateEntity>()
                                for (relaySnap in relaysSnap.children) {
                                    val relayId = relaySnap.key ?: continue
                                    val isOn = relaySnap.child("isOn").getValue(Boolean::class.java) ?: false
                                    sceneRelayStates.add(
                                        SceneRelayStateEntity(
                                            id = "${sceneId}_$relayId",
                                            sceneId = sceneId,
                                            relayId = relayId,
                                            isOn = isOn
                                        )
                                    )
                                }
                                sceneDao.insertSceneRelayStates(sceneRelayStates)
                            }
                        }
                    }

                    // Remove local scenes that were deleted on remote
                    for (localScene in localScenes) {
                        if (!remoteIds.contains(localScene.id)) {
                            sceneDao.deleteScene(localScene)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeListenerManager", "observeUserScenes cancelled for $ownerUid: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(listener)
        activeListeners["scenes/$ownerUid"] = listener
    }

    private fun cleanupAllListeners() {
        for ((path, listener) in activeListeners) {
            val ref = FirebaseDatabase.getInstance().getReference(path)
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
    }
}
