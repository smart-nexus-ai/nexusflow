package com.smartnexus.nexusflow.features.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.features.home.components.AutomationRuleItemData
import com.smartnexus.nexusflow.features.home.components.RelayItemData
import com.smartnexus.nexusflow.features.home.components.SensorItemData
import com.smartnexus.nexusflow.domain.model.DeviceType
import com.smartnexus.nexusflow.data.local.DatabaseSeeder
import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import com.smartnexus.nexusflow.data.local.dao.AutomationRuleDao
import com.smartnexus.nexusflow.data.local.dao.ScheduleDao
import com.smartnexus.nexusflow.data.local.dao.SceneDao
import com.smartnexus.nexusflow.data.local.entity.AutomationRuleEntity
import com.smartnexus.nexusflow.data.local.entity.DeviceEntity
import com.smartnexus.nexusflow.data.local.entity.RelayStateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class HomeUiState(
    val rooms: List<String> = listOf("Living Room", "Kitchen Room"),
    val selectedRoom: String = "Living Room",
    val relays: List<RelayItemData> = emptyList(),
    val sensors: List<SensorItemData> = listOf(
        SensorItemData(Icons.Default.Thermostat, "Temperature", "28.5", "°C", "Comfortable"),
        SensorItemData(Icons.Default.WaterDrop, "Humidity", "65", "%", "Normal"),
        SensorItemData(Icons.Default.SentimentSatisfiedAlt, "Humidex", "28.7", "°C", "Comfortable")
    ),
    val automationRules: List<AutomationRuleItemData> = emptyList(),
    val isRulesExpanded: Boolean = true,
    val selectedRelayForEdit: RelayItemData? = null,
    val selectedRuleForDetails: AutomationRuleItemData? = null,
    val showAddEditRuleSheet: Boolean = false,
    val selectedRuleForEdit: AutomationRuleItemData? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceDao: DeviceDao,
    private val relayStateDao: RelayStateDao,
    private val automationRuleDao: AutomationRuleDao,
    private val scheduleDao: ScheduleDao,
    private val sceneDao: SceneDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val selectedRoomFlow = MutableStateFlow("")
    init {
        viewModelScope.launch {
            // Seed database with mock data if empty
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DatabaseSeeder.seedIfEmpty(deviceDao, relayStateDao, automationRuleDao, scheduleDao, sceneDao)
            }

            // Combine active devices and selected room dynamically
            val devicesAndRoomFlow = combine(
                deviceDao.getDevicesFlow(),
                selectedRoomFlow
            ) { devices, room -> devices to room }

            // Switch map to the relays and rules of the active device ID
            devicesAndRoomFlow.flatMapLatest { (dbDevices, room) ->
                val roomsList = dbDevices.map { it.name }
                val activeRoom = if (roomsList.contains(room)) {
                    room
                } else {
                    roomsList.firstOrNull() ?: ""
                }
                val activeDevice = dbDevices.find { it.name == activeRoom }
                val deviceId = activeDevice?.id ?: ""

                if (deviceId.isNotEmpty()) {
                    combine(
                        relayStateDao.getRelayStatesForDeviceFlow(deviceId),
                        automationRuleDao.getAutomationRulesForDeviceFlow(deviceId)
                    ) { dbRelays, dbRules ->
                        ItemResult(activeDevice, dbDevices, dbRelays, dbRules)
                    }
                } else {
                    flowOf(ItemResult(null, dbDevices, emptyList(), emptyList()))
                }
            }.collect { result ->
                val activeDevice = result.activeDevice
                val dbDevices = result.dbDevices
                val dbRelays = result.dbRelays
                val dbRules = result.dbRules

                Log.d("HomeViewModelDebug", "Devices: ${dbDevices.map { "${it.id}:${it.name}" }}, SelectedRoomFlow: ${selectedRoomFlow.value}, ActiveRoom: ${activeDevice?.name}, DeviceID: ${activeDevice?.id}, Relays: ${dbRelays.size}")

                val roomsList = dbDevices.map { it.name }
                val room = selectedRoomFlow.value
                val activeRoom = if (roomsList.contains(room)) room else (roomsList.firstOrNull() ?: "")

                val updatedSensors = if (activeDevice != null) {
                    val tempComfort = when {
                        activeDevice.temperature < 20f -> "Cool"
                        activeDevice.temperature > 30f -> "Warm"
                        else -> "Comfortable"
                    }
                    val humComfort = when {
                        activeDevice.humidity < 40f -> "Dry"
                        activeDevice.humidity > 70f -> "Humid"
                        else -> "Normal"
                    }
                    val humidexComfort = when {
                        activeDevice.humidex < 29f -> "Comfortable"
                        activeDevice.humidex < 39f -> "Slight Discomfort"
                        else -> "Extreme Discomfort"
                    }
                    listOf(
                        SensorItemData(Icons.Default.Thermostat, "Temperature", String.format(java.util.Locale.US, "%.1f", activeDevice.temperature), "°C", tempComfort),
                        SensorItemData(Icons.Default.WaterDrop, "Humidity", String.format(java.util.Locale.US, "%.0f", activeDevice.humidity), "%", humComfort),
                        SensorItemData(Icons.Default.SentimentSatisfiedAlt, "Humidex", String.format(java.util.Locale.US, "%.1f", activeDevice.humidex), "°C", humidexComfort)
                    )
                } else {
                    listOf(
                        SensorItemData(Icons.Default.Thermostat, "Temperature", "--", "°C", "--"),
                        SensorItemData(Icons.Default.WaterDrop, "Humidity", "--", "%", "--"),
                        SensorItemData(Icons.Default.SentimentSatisfiedAlt, "Humidex", "--", "°C", "--")
                    )
                }

                val relays = dbRelays.map { relay ->
                    RelayItemData(
                        id = relay.id,
                        name = relay.name,
                        isOn = relay.isOn,
                        isPending = relay.isPending,
                        deviceType = DeviceType.fromName(relay.type ?: relay.name),
                        powerWatts = relay.powerWatts
                    )
                }

                val rules = dbRules.map { rule ->
                    val relayName = dbRelays.find { it.relayId == rule.relayId }?.name ?: rule.relayId
                    toItemData(rule, relayName)
                }

                _uiState.update { state ->
                    state.copy(
                        rooms = roomsList,
                        selectedRoom = activeRoom,
                        sensors = updatedSensors,
                        relays = relays,
                        automationRules = rules
                    )
                }

                if (room != activeRoom) {
                    selectedRoomFlow.value = activeRoom
                }
            }
        }
    }
    fun onRoomSelected(room: String) {
        selectedRoomFlow.value = room
    }

    fun onToggleRelay(relayId: String, newState: Boolean) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                relayStateDao.updateRelayState(
                    id = relayId,
                    isOn = newState,
                    isPending = true,
                    updatedAt = System.currentTimeMillis()
                )
                
                // Sync relay state change directly to device_states relays node
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val relayState = relayStateDao.getRelayStateById(relayId)
                    if (relayState != null) {
                        val deviceId = relayState.deviceId
                        val channelId = relayState.relayId
                        
                        FirebaseDatabase.getInstance()
                            .getReference("device_states/$deviceId/relays/$channelId")
                            .setValue(newState)
                    }
                }
            }
        }
    }

    fun onToggleRulesExpand() {
        _uiState.value = _uiState.value.copy(isRulesExpanded = !_uiState.value.isRulesExpanded)
    }

    fun onToggleRule(ruleId: String, newState: Boolean) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                automationRuleDao.updateRuleEnabledState(ruleId, newState, false)
                
                // Sync rule enabled/disabled state to Firebase RTDB
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val rule = automationRuleDao.getAutomationRuleById(ruleId)
                    if (rule != null) {
                        FirebaseDatabase.getInstance()
                            .getReference("automation_rules/${rule.deviceId}/${rule.id}/isEnabled")
                            .setValue(newState)
                    }
                }
            }
        }
    }

    fun onOpenRelayEdit(relay: RelayItemData) {
        _uiState.value = _uiState.value.copy(selectedRelayForEdit = relay)
    }

    fun onCloseRelayEdit() {
        _uiState.value = _uiState.value.copy(selectedRelayForEdit = null)
    }

    fun onSaveRelayConfig(relayId: String, newName: String, newDeviceType: DeviceType, powerWatts: Int = 50) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val typeStr = when (newDeviceType) {
                    DeviceType.LIGHT -> "light"
                    DeviceType.FAN -> "fan"
                    DeviceType.AC -> "ac"
                    DeviceType.HUMIDIFIER -> "humidifier"
                    DeviceType.TV -> "tv"
                    else -> "switch"
                }

                relayStateDao.updateRelayConfig(
                    id = relayId,
                    newName = newName,
                    type = typeStr,
                    powerWatts = powerWatts,
                    updatedAt = System.currentTimeMillis()
                )

                // Sync the modified configuration to Firebase devices/{deviceId}/relay_config/{relayId}
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val relayState = relayStateDao.getRelayStateById(relayId)
                    if (relayState != null) {
                        val deviceId = relayState.deviceId
                        val channelId = relayState.relayId
                        
                        // Map DeviceType back to standard JSON name
                        val typeStr = when (newDeviceType) {
                            DeviceType.LIGHT -> "light"
                            DeviceType.FAN -> "fan"
                            DeviceType.AC -> "ac"
                            DeviceType.HUMIDIFIER -> "humidifier"
                            DeviceType.TV -> "tv"
                            else -> "switch"
                        }
                        
                        val iconStr = when (newDeviceType) {
                            DeviceType.LIGHT -> "Lightbulb"
                            DeviceType.FAN -> "Air"
                            DeviceType.AC -> "AcUnit"
                            DeviceType.HUMIDIFIER -> "WaterDrop"
                            DeviceType.TV -> "Tv"
                            else -> "Power"
                        }

                        val configPayload = mapOf(
                            "name" to newName,
                            "type" to typeStr,
                            "icon" to iconStr,
                            "powerWatts" to powerWatts
                        )

                        FirebaseDatabase.getInstance()
                            .getReference("devices/$deviceId/relay_config/$channelId")
                            .setValue(configPayload)
                    }
                }
            }
            _uiState.update { it.copy(selectedRelayForEdit = null) }
        }
    }

    fun onOpenRuleDetails(rule: AutomationRuleItemData) {
        _uiState.value = _uiState.value.copy(selectedRuleForDetails = rule)
    }

    fun onCloseRuleDetails() {
        _uiState.value = _uiState.value.copy(selectedRuleForDetails = null)
    }

    fun onOpenAddRule() {
        _uiState.value = _uiState.value.copy(
            showAddEditRuleSheet = true,
            selectedRuleForEdit = null
        )
    }

    fun onOpenEditRule(rule: AutomationRuleItemData) {
        _uiState.value = _uiState.value.copy(
            showAddEditRuleSheet = true,
            selectedRuleForEdit = rule,
            selectedRuleForDetails = null
        )
    }

    fun onCloseAddEditRule() {
        _uiState.value = _uiState.value.copy(
            showAddEditRuleSheet = false,
            selectedRuleForEdit = null
        )
    }

    fun onSaveRule(rule: AutomationRuleItemData) {
        viewModelScope.launch {
            val deviceId = getDeviceIdForRoom(_uiState.value.selectedRoom)
            val entity = parseRuleItemDataToEntity(rule, deviceId)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                automationRuleDao.insertAutomationRule(entity)
                
                // Sync rule to Firebase
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("automation_rules/$deviceId/${entity.id}")
                        .setValue(mapOf(
                            "relayId" to entity.relayId,
                            "sensorMode" to entity.sensorMode,
                            "logicalOperator" to entity.logicalOperator,
                            "tempCondition" to entity.tempCondition,
                            "tempThreshold" to entity.tempThreshold,
                            "humidityCondition" to entity.humidityCondition,
                            "humidityThreshold" to entity.humidityThreshold,
                            "action" to entity.action,
                            "isEnabled" to entity.isEnabled,
                            "createdAt" to entity.createdAt
                        ))
                }
            }
            _uiState.update { state ->
                state.copy(
                    showAddEditRuleSheet = false,
                    selectedRuleForEdit = null,
                    selectedRuleForDetails = null
                )
            }
        }
    }

    fun onDeleteRule(ruleId: String) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val rule = automationRuleDao.getAutomationRuleById(ruleId)
                if (rule != null) {
                    automationRuleDao.deleteAutomationRule(rule)
                    
                    // Sync rule deletion to Firebase
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        FirebaseDatabase.getInstance()
                            .getReference("automation_rules/${rule.deviceId}/$ruleId")
                            .removeValue()
                    }
                }
            }
            _uiState.update { state ->
                state.copy(selectedRuleForDetails = null)
            }
        }
    }

    private suspend fun getDeviceIdForRoom(room: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            deviceDao.getDevicesFlow().first().find { it.name == room }?.id ?: ""
        }
    }

    private fun toItemData(entity: AutomationRuleEntity, relayName: String): AutomationRuleItemData {
        val actionStr = if (entity.action) "ON" else "OFF"
        val description = when (entity.sensorMode) {
            "temperature" -> {
                val condSymbol = if (entity.tempCondition == "above") ">" else "<"
                "$relayName $actionStr if Temp $condSymbol ${entity.tempThreshold?.toInt() ?: 30}°C"
            }
            "humidity" -> {
                val condSymbol = if (entity.humidityCondition == "above") ">" else "<"
                "$relayName $actionStr if Humidity $condSymbol ${entity.humidityThreshold?.toInt() ?: 60}%"
            }
            else -> {
                val tSym = if (entity.tempCondition == "above") ">" else "<"
                val hSym = if (entity.humidityCondition == "above") ">" else "<"
                val opStr = entity.logicalOperator?.uppercase() ?: "AND"
                "$relayName $actionStr if Temp $tSym ${entity.tempThreshold?.toInt() ?: 30}°C $opStr Humidity $hSym ${entity.humidityThreshold?.toInt() ?: 60}%"
            }
        }

        val subtitle = when (entity.sensorMode) {
            "temperature" -> {
                val condText = if (entity.tempCondition == "above") "rises above" else "drops below"
                "When temperature $condText ${entity.tempThreshold?.toInt() ?: 30}°C"
            }
            "humidity" -> {
                val condText = if (entity.humidityCondition == "above") "rises above" else "drops below"
                "When humidity $condText ${entity.humidityThreshold?.toInt() ?: 60}%"
            }
            else -> {
                val tSym = if (entity.tempCondition == "above") ">" else "<"
                val hSym = if (entity.humidityCondition == "above") ">" else "<"
                val opStr = entity.logicalOperator?.uppercase() ?: "AND"
                "When Temp $tSym ${entity.tempThreshold?.toInt() ?: 30}°C $opStr Humidity $hSym ${entity.humidityThreshold?.toInt() ?: 60}%"
            }
        }

        return AutomationRuleItemData(
            id = entity.id,
            description = description,
            subtitle = subtitle,
            isEnabled = entity.isEnabled,
            iconType = if (entity.sensorMode == "humidity") "wind" else "snowflake",
            targetRelayId = entity.relayId
        )
    }

    private fun parseRuleItemDataToEntity(item: AutomationRuleItemData, deviceId: String): AutomationRuleEntity {
        val description = item.description
        val action = if (description.contains(" ON ", ignoreCase = true)) true else false

        val sensorMode = when {
            description.contains("Temp", ignoreCase = true) && description.contains("Hum", ignoreCase = true) -> "both"
            description.contains("Humidity", ignoreCase = true) -> "humidity"
            else -> "temperature"
        }

        val logicalOperator = when {
            description.contains(" AND ", ignoreCase = true) -> "and"
            description.contains(" OR ", ignoreCase = true) -> "or"
            else -> null
        }

        val tempCondition = when {
            description.contains("Temp >") -> "above"
            description.contains("Temp <") -> "below"
            else -> null
        }
        val tempThreshold = if (tempCondition != null) {
            val pattern = "Temp [><] (\\d+)".toRegex()
            pattern.find(description)?.groupValues?.get(1)?.toDoubleOrNull() ?: 30.0
        } else null

        val humidityCondition = when {
            description.contains("Humidity >") || description.contains("Hum >") -> "above"
            description.contains("Humidity <") || description.contains("Hum <") -> "below"
            else -> null
        }
        val humidityThreshold = if (humidityCondition != null) {
            val pattern = "Hum(?:idity)? [><] (\\d+)".toRegex()
            pattern.find(description)?.groupValues?.get(1)?.toDoubleOrNull() ?: 60.0
        } else null

        val relayId = item.targetRelayId.substringAfterLast("_")
        val fullRelayId = if (relayId.startsWith("relay_")) relayId else "relay_$relayId"

        return AutomationRuleEntity(
            id = item.id,
            deviceId = deviceId,
            relayId = fullRelayId,
            sensorMode = sensorMode,
            logicalOperator = logicalOperator,
            tempCondition = tempCondition,
            tempThreshold = tempThreshold,
            humidityCondition = humidityCondition,
            humidityThreshold = humidityThreshold,
            action = action,
            isEnabled = item.isEnabled,
            lastTriggeredAt = null,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
    }
}

private data class ItemResult(
    val activeDevice: DeviceEntity?,
    val dbDevices: List<DeviceEntity>,
    val dbRelays: List<RelayStateEntity>,
    val dbRules: List<AutomationRuleEntity>
)
