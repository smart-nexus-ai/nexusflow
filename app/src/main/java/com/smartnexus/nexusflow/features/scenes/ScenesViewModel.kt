package com.smartnexus.nexusflow.features.scenes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.smartnexus.nexusflow.data.local.DatabaseSeeder
import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import com.smartnexus.nexusflow.data.local.dao.SceneDao
import com.smartnexus.nexusflow.data.local.entity.SceneEntity
import com.smartnexus.nexusflow.data.local.entity.SceneRelayStateEntity
import com.smartnexus.nexusflow.domain.model.DeviceType
import com.smartnexus.nexusflow.features.scenes.components.SceneFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SceneDeviceItem(
    val relayName: String,
    val deviceName: String,
    val relayId: String, // e.g. "CH 1"
    val state: String, // "ON" or "OFF"
    val deviceType: DeviceType
)

data class SceneItem(
    val id: String,
    val name: String,
    val iconName: String,
    val actionCount: Int,
    val summaryText: String,
    val description: String,
    val lastUsedText: String,
    val createdOnText: String,
    val isFavorite: Boolean = false,
    val showOnHome: Boolean = true,
    val isActivating: Boolean = false,
    val devices: List<SceneDeviceItem> = emptyList(),
    val formData: SceneFormData = SceneFormData(id = id, name = name, iconName = iconName)
)

data class ScenesUiState(
    val scenes: List<SceneItem> = emptyList(),
    val selectedFilter: String = "all", // "all", "favorites", "last_used", "a_z", "z_a"
    val isAddEditSheetOpen: Boolean = false,
    val editingScene: SceneFormData? = null,
    val activeDetailsSceneId: String? = null,
    val hasDevices: Boolean = false,
    val availableRelays: List<SceneRelayOption> = emptyList()
)

@HiltViewModel
class ScenesViewModel @Inject constructor(
    private val deviceDao: DeviceDao,
    private val sceneDao: SceneDao,
    private val relayStateDao: RelayStateDao,
    private val automationRuleDao: com.smartnexus.nexusflow.data.local.dao.AutomationRuleDao,
    private val scheduleDao: com.smartnexus.nexusflow.data.local.dao.ScheduleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScenesUiState())
    val uiState = _uiState.asStateFlow()

    private val _activationSuccessEvent = MutableSharedFlow<String>()
    val activationSuccessEvent = _activationSuccessEvent.asSharedFlow()

    init {
        // Sync scene modifications reactively to Firebase RTDB
        viewModelScope.launch {
            sceneDao.getAllScenesWithRelayStatesFlow().collect { scenesWithRelays ->
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val uid = currentUser.uid
                    val dbRef = FirebaseDatabase.getInstance().getReference("scenes/$uid")
                    
                    val scenesMap = scenesWithRelays.associate { sceneWithRelay ->
                        val scene = sceneWithRelay.scene
                        val relays = sceneWithRelay.relayStates
                        
                        scene.id to mapOf(
                            "id" to scene.id,
                            "deviceId" to scene.deviceId,
                            "name" to scene.name,
                            "icon" to scene.icon,
                            "isFavorite" to scene.isFavorite,
                            "showOnHome" to scene.showOnHome,
                            "createdAt" to scene.createdAt,
                            "lastUsedAt" to scene.lastUsedAt,
                            "relays" to relays.associate { r ->
                                r.relayId to mapOf(
                                    "relayId" to r.relayId,
                                    "isOn" to r.isOn
                                )
                            }
                        )
                    }

                    try {
                        dbRef.setValue(scenesMap)
                    } catch (e: Exception) {
                        Log.e("ScenesViewModel", "Failed to sync scenes to Firebase RTDB", e)
                    }
                }
            }
        }

        viewModelScope.launch {
            // Seed database with mock data if empty
            withContext(Dispatchers.IO) {
                DatabaseSeeder.seedIfEmpty(deviceDao, relayStateDao, automationRuleDao, scheduleDao, sceneDao)
            }

            // Stream scenes and map them to UI items reactively
            combine(
                deviceDao.getDevicesFlow(),
                sceneDao.getAllScenesWithRelayStatesFlow()
            ) { devices, scenes ->
                devices to scenes
            }.collectLatest { (dbDevices, dbScenes) ->
                val realRelaysList = mutableListOf<SceneRelayOption>()
                val dbRelays = withContext(Dispatchers.IO) {
                    dbDevices.flatMapIndexed { devIdx, dev ->
                        relayStateDao.getRelayStatesForDevice(dev.id).map { relay ->
                            val chNum = relay.relayId.substringAfterLast("_").toIntOrNull() ?: 1
                            realRelaysList.add(
                                SceneRelayOption(
                                    relayName = relay.name,
                                    deviceName = dev.name,
                                    relayId = "CH $chNum",
                                    deviceType = DeviceType.fromName(relay.name),
                                    deviceIndex = devIdx + 1
                                )
                            )
                            relay
                        }
                    }
                }

                val mappedScenes = dbScenes.map { sceneWithRelays ->
                    val scene = sceneWithRelays.scene
                    val device = dbDevices.find { it.id == scene.deviceId }
                    val deviceName = device?.name ?: "Living Room"

                    val devicesList = sceneWithRelays.relayStates.map { stateEntity ->
                        val fullRelayId = "${scene.deviceId}_${stateEntity.relayId}"
                        val relay = dbRelays.find { it.id == fullRelayId }
                        val relayName = relay?.name ?: stateEntity.relayId

                        val chNumber = stateEntity.relayId.substringAfterLast("_").toIntOrNull() ?: 1
                        val uiRelayId = "CH $chNumber"

                        SceneDeviceItem(
                            relayName = relayName,
                            deviceName = deviceName,
                            relayId = uiRelayId,
                            state = if (stateEntity.isOn) "ON" else "OFF",
                            deviceType = DeviceType.fromName(relayName)
                        )
                    }

                    val summary = devicesList.joinToString(", ") { "${it.relayName} ${it.state}" }

                    val diff = System.currentTimeMillis() - (scene.lastUsedAt ?: 0L)
                    val lastUsed = if (scene.lastUsedAt == null) "Never" else {
                        when {
                            diff < 60000L -> "Just now"
                            diff < 3600000L -> "${diff / 60000L} mins ago"
                            diff < 86400000L -> "${diff / 3600000L} hrs ago"
                            else -> "${diff / 86400000L} days ago"
                        }
                    }

                    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    val createdOn = dateFormat.format(java.util.Date(scene.createdAt))

                    val formDataRelayStates = devicesList.associate { "${it.relayName}, ${it.deviceName}" to (it.state == "ON") }

                    SceneItem(
                        id = scene.id,
                        name = scene.name,
                        iconName = scene.icon,
                        actionCount = devicesList.size,
                        summaryText = summary.ifEmpty { "Tap + Add Relay to configure scene actions" },
                        description = "Custom smart automation scene",
                        lastUsedText = lastUsed,
                        createdOnText = createdOn,
                        isFavorite = scene.isFavorite,
                        showOnHome = scene.showOnHome,
                        isActivating = false,
                        devices = devicesList,
                        formData = SceneFormData(
                            id = scene.id,
                            name = scene.name,
                            iconName = scene.icon,
                            relayStates = formDataRelayStates
                        )
                    )
                }

                _uiState.update { it.copy(scenes = mappedScenes, hasDevices = dbDevices.isNotEmpty(), availableRelays = realRelaysList) }
            }
        }
    }

    fun onFilterChanged(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onToggleFavorite(sceneId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sceneWithRelays = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (sceneWithRelays != null) {
                    val updatedScene = sceneWithRelays.scene.copy(isFavorite = !sceneWithRelays.scene.isFavorite)
                    sceneDao.insertScene(updatedScene)
                }
            }
        }
    }

    fun onToggleShowOnHome(sceneId: String, show: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sceneWithRelays = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (sceneWithRelays != null) {
                    val updatedScene = sceneWithRelays.scene.copy(showOnHome = show)
                    sceneDao.insertScene(updatedScene)
                }
            }
        }
    }

    fun onActivateScene(sceneId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                val updated = state.scenes.map { if (it.id == sceneId) it.copy(isActivating = true) else it }
                state.copy(scenes = updated)
            }

            withContext(Dispatchers.IO) {
                val sceneWithRelays = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (sceneWithRelays != null) {
                    // Update last used timestamp
                    val updatedScene = sceneWithRelays.scene.copy(lastUsedAt = System.currentTimeMillis())
                    sceneDao.insertScene(updatedScene)

                    // Apply relay states
                    val deviceId = sceneWithRelays.scene.deviceId
                    sceneWithRelays.relayStates.forEach { sceneRelay ->
                        val fullRelayId = "${deviceId}_${sceneRelay.relayId}"
                        relayStateDao.updateRelayState(
                            id = fullRelayId,
                            isOn = sceneRelay.isOn,
                            isPending = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        // Push directly to Firebase device_states
                        FirebaseDatabase.getInstance()
                            .getReference("device_states/$deviceId/relays/${sceneRelay.relayId}")
                            .setValue(sceneRelay.isOn)
                    }
                }
            }

            delay(1200)

            _uiState.update { state ->
                val updated = state.scenes.map { if (it.id == sceneId) it.copy(isActivating = false) else it }
                state.copy(scenes = updated)
            }

            val sceneName = _uiState.value.scenes.find { it.id == sceneId }?.name ?: "Scene"
            _activationSuccessEvent.emit("$sceneName activated successfully!")
        }
    }

    fun onOpenSceneDetails(sceneId: String) {
        _uiState.update { it.copy(activeDetailsSceneId = sceneId) }
    }

    fun onCloseSceneDetails() {
        _uiState.update { it.copy(activeDetailsSceneId = null) }
    }

    fun onToggleSceneRelay(sceneId: String, relayId: String, relayName: String) {
        val dbRelayId = "relay_${relayId.replace("CH ", "").trim()}"
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (existing != null) {
                    val currentRelayState = existing.relayStates.find { it.relayId == dbRelayId }
                    if (currentRelayState != null) {
                        val updatedRelayState = currentRelayState.copy(isOn = !currentRelayState.isOn)
                        sceneDao.insertSceneRelayStates(listOf(updatedRelayState))
                    }
                }
            }
        }
    }

    fun onRemoveRelayFromScene(sceneId: String, relayId: String, relayName: String) {
        val dbRelayId = "relay_${relayId.replace("CH ", "").trim()}"
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (existing != null) {
                    val filteredStates = existing.relayStates.filterNot { it.relayId == dbRelayId }
                    sceneDao.insertSceneWithRelayStates(existing.scene, filteredStates)
                }
            }
        }
    }

    fun onAddRelayToScene(sceneId: String, relayName: String, deviceName: String, relayId: String, deviceType: DeviceType) {
        val dbRelayId = "relay_${relayId.replace("CH ", "").trim()}"
        val compositeId = "${sceneId}_${dbRelayId}"
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (existing != null) {
                    val alreadyExists = existing.relayStates.any { it.relayId == dbRelayId }
                    if (!alreadyExists) {
                        val newState = SceneRelayStateEntity(
                            id = compositeId,
                            sceneId = sceneId,
                            relayId = dbRelayId,
                            isOn = true
                        )
                        sceneDao.insertSceneRelayStates(listOf(newState))
                    }
                }
            }
        }
    }

    fun onOpenAddScene() {
        viewModelScope.launch {
            val currentScenes = _uiState.value.scenes
            if (currentScenes.size >= 6) return@launch

            val baseName = "Default Scene"
            val existingNames = currentScenes.map { it.name }.toSet()
            var uniqueName = baseName
            var counter = 2
            while (existingNames.contains(uniqueName)) {
                uniqueName = "$baseName $counter"
                counter++
            }

            val firstDevice = withContext(Dispatchers.IO) {
                deviceDao.getDevicesFlow().first().firstOrNull()
            }
            val newId = "scene_${System.currentTimeMillis()}"
            val newScene = SceneEntity(
                id = newId,
                deviceId = firstDevice?.id ?: "nf_lr8", // Fallback to Living Room if no device paired
                name = uniqueName,
                icon = "WbSunny",
                isFavorite = false,
                showOnHome = true,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = null,
                isSynced = false
            )

            withContext(Dispatchers.IO) {
                sceneDao.insertScene(newScene)
            }

            _uiState.update { it.copy(activeDetailsSceneId = newId) }
        }
    }

    fun onEditSceneHeader(sceneId: String, newName: String, newIconName: String, newDescription: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (existing != null) {
                    val updated = existing.scene.copy(
                        name = newName,
                        icon = newIconName
                    )
                    sceneDao.insertScene(updated)
                }
            }
        }
    }

    fun onOpenEditScene(item: SceneItem) {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = true,
                editingScene = item.formData
            )
        }
    }

    fun onCloseAddEditSheet() {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = false,
                editingScene = null
            )
        }
    }

    fun onSaveScene(data: SceneFormData) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sceneId = data.id ?: "scene_${System.currentTimeMillis()}"
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)

                val dbDevices = deviceDao.getDevicesFlow().first()
                val dbRelays = dbDevices.flatMap { relayStateDao.getRelayStatesForDevice(it.id) }

                val relayStates = data.relayStates.mapNotNull { (key, isOn) ->
                    val parts = key.split(",")
                    val relayName = parts.getOrNull(0)?.trim() ?: return@mapNotNull null
                    val roomName = parts.getOrNull(1)?.trim() ?: return@mapNotNull null

                    val dev = dbDevices.find { it.name.equals(roomName, ignoreCase = true) } ?: return@mapNotNull null
                    val relay = dbRelays.find { it.deviceId == dev.id && it.name.equals(relayName, ignoreCase = true) } ?: return@mapNotNull null

                    SceneRelayStateEntity(
                        id = "${sceneId}_${relay.relayId}",
                        sceneId = sceneId,
                        relayId = relay.relayId,
                        isOn = isOn
                    )
                }

                val determinedDeviceId = relayStates.firstOrNull()?.let { rs ->
                    dbRelays.find { it.relayId == rs.relayId }?.deviceId
                } ?: dbDevices.firstOrNull()?.id ?: "nf_lr8"

                val sceneEntity = if (existing != null) {
                    existing.scene.copy(
                        name = data.name,
                        icon = data.iconName
                    )
                } else {
                    SceneEntity(
                        id = sceneId,
                        deviceId = determinedDeviceId,
                        name = data.name,
                        icon = data.iconName,
                        isFavorite = false,
                        showOnHome = true,
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = null,
                        isSynced = false
                    )
                }

                sceneDao.insertSceneWithRelayStates(sceneEntity, relayStates)
            }

            _uiState.update {
                it.copy(
                    isAddEditSheetOpen = false,
                    editingScene = null
                )
            }
        }
    }

    fun onDeleteScene(sceneId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = sceneDao.getSceneWithRelayStatesById(sceneId)
                if (existing != null) {
                    sceneDao.deleteSceneWithRelayStates(existing.scene)
                }
            }
            _uiState.update {
                it.copy(
                    activeDetailsSceneId = if (it.activeDetailsSceneId == sceneId) null else it.activeDetailsSceneId
                )
            }
        }
    }
}
