package com.smartnexus.nexusflow.features.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.domain.model.DeviceType
import com.smartnexus.nexusflow.data.local.DatabaseSeeder
import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.data.local.dao.RelayStateDao
import com.smartnexus.nexusflow.data.local.dao.AutomationRuleDao
import com.smartnexus.nexusflow.data.local.dao.ScheduleDao
import com.smartnexus.nexusflow.data.local.dao.SceneDao
import com.smartnexus.nexusflow.data.local.entity.ScheduleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ScheduleItem(
    val id: String,
    val relayName: String,
    val deviceName: String,
    val relayId: String,
    val action: String,
    val pattern: String,
    val nextTrigger: String,
    val daysActive: List<String>,
    val isEnabled: Boolean,
    val deviceType: DeviceType,
    val deviceIndex: Int = 1,
    val timeRange: String? = null,
    val isTurnOn: Boolean = action.equals("ON", ignoreCase = true)
)

data class AddEditScheduleData(
    val id: String? = null,
    val deviceId: String = "",
    val relayName: String = "Living Light",
    val deviceName: String = "Living Room",
    val relayId: String = "CH 1",
    val action: String = "ON",
    val scheduleType: String = "Time", // "Time" or "Duration"
    val time: String = "07:00 AM",
    val period: String = "AM",
    val startTime: String = "14:00",
    val endTime: String = "17:00",
    val daysOfWeek: Set<String> = setOf("MON", "TUE", "WED", "THU", "FRI"),
    val repeatPattern: String = "Every weekday (Mon - Fri)",
    val notifyWhenExecuted: Boolean = true,
    val isEnabled: Boolean = true,
    val hasEndDate: Boolean = false,
    val endDate: String? = null,
    val deviceType: DeviceType = DeviceType.LIGHT
)

data class SchedulesUiState(
    val schedules: List<ScheduleItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddEditSheetOpen: Boolean = false,
    val editingSchedule: AddEditScheduleData? = null,
    val selectedFilter: String = "all", // "all", "active", "inactive"
    val searchQuery: String = "",
    val availableRelays: List<Triple<String, String, Pair<String, Int>>> = emptyList(),
    val hasDevices: Boolean = false
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val deviceDao: DeviceDao,
    private val relayStateDao: RelayStateDao,
    private val automationRuleDao: AutomationRuleDao,
    private val scheduleDao: ScheduleDao,
    private val sceneDao: SceneDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulesUiState())
    val uiState: StateFlow<SchedulesUiState> = _uiState.asStateFlow()

    private val dayAbbrevMap = mapOf(
        "Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7,
        "SUN" to 1, "MON" to 2, "TUE" to 3, "WED" to 4, "THU" to 5, "FRI" to 6, "SAT" to 7
    )
    private val dayIntMap = mapOf(
        1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
    )

    init {
        viewModelScope.launch {
            // Seed database with mock data if empty
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DatabaseSeeder.seedIfEmpty(deviceDao, relayStateDao, automationRuleDao, scheduleDao, sceneDao)
            }

            // Reactively stream all schedules and map them with device/relay names
            combine(
                deviceDao.getDevicesFlow(),
                scheduleDao.getAllSchedulesFlow()
            ) { devices, _ ->
                devices
            }.collectLatest { dbDevices ->
                val hasDevices = dbDevices.isNotEmpty()
                val realRelaysList = mutableListOf<Triple<String, String, Pair<String, Int>>>()
                val items = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val allSchedules = mutableListOf<ScheduleEntity>()
                    val allRelayStates = mutableMapOf<String, String>()

                    for ((idx, dev) in dbDevices.withIndex()) {
                        val schedules = scheduleDao.getSchedulesForDevice(dev.id)
                        allSchedules.addAll(schedules)

                        val relays = relayStateDao.getRelayStatesForDevice(dev.id)
                        for (relay in relays) {
                            allRelayStates["${dev.id}_${relay.relayId}"] = relay.name
                            val chNum = relay.relayId.substringAfterLast("_").toIntOrNull() ?: 1
                            realRelaysList.add(Triple(relay.name, dev.name, Pair("CH $chNum", idx + 1)))
                        }
                    }

                    allSchedules.map { sch ->
                        val dev = dbDevices.find { it.id == sch.deviceId }
                        val deviceName = dev?.name ?: "Unknown Device"
                        val relayKey = "${sch.deviceId}_${sch.relayId}"
                        val relayName = allRelayStates[relayKey] ?: sch.relayId

                        val daysList = sch.daysOfWeek.map { dayIntMap[it] ?: "Mon" }
                        val daysText = when {
                            daysList.size == 7 -> "Every day"
                            daysList.size == 5 && !daysList.contains("Sun") && !daysList.contains("Sat") -> "Every weekday"
                            daysList.size == 2 && daysList.contains("Sun") && daysList.contains("Sat") -> "Weekends"
                            daysList.isEmpty() -> "Once"
                            else -> daysList.joinToString(", ")
                        }

                        val patternText = if (sch.endTime != null) {
                            "$daysText at ${secondsToTimeString(sch.startTime, false)} – ${secondsToTimeString(sch.endTime, false)}"
                        } else {
                            "$daysText at ${secondsToTimeString(sch.startTime, true)}"
                        }

                        val chNumber = sch.relayId.substringAfterLast("_").toIntOrNull() ?: 1

                        ScheduleItem(
                            id = sch.id,
                            relayName = relayName,
                            deviceName = deviceName,
                            relayId = "CH $chNumber",
                            action = if (sch.action) "ON" else "OFF",
                            pattern = patternText,
                            nextTrigger = "Upcoming",
                            daysActive = daysList,
                            isEnabled = sch.isEnabled,
                            deviceType = DeviceType.fromName(relayName),
                            deviceIndex = getDeviceIndex(deviceName),
                            timeRange = if (sch.endTime != null) "${secondsToTimeString(sch.startTime, false)} ➔ ${secondsToTimeString(sch.endTime, false)}" else null
                        )
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        schedules = items,
                        availableRelays = realRelaysList,
                        hasDevices = hasDevices,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onOpenAddSchedule() {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = true,
                editingSchedule = null
            )
        }
    }

    fun onOpenEditSchedule(schedule: ScheduleItem) {
        viewModelScope.launch {
            val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val dbDevices = deviceDao.getDevicesFlow().first()
                val dev = dbDevices.find { it.name == schedule.deviceName } ?: return@withContext null
                val sch = scheduleDao.getScheduleById(schedule.id) ?: return@withContext null
                AddEditScheduleData(
                    id = schedule.id,
                    deviceId = dev.id,
                    relayName = schedule.relayName,
                    deviceName = schedule.deviceName,
                    relayId = schedule.relayId,
                    action = if (schedule.action.equals("ON", ignoreCase = true)) "TURN ON" else "TURN OFF",
                    scheduleType = sch.scheduleType,
                    time = secondsToTimeString(sch.startTime, true),
                    startTime = secondsToTimeString(sch.startTime, false),
                    endTime = if (sch.endTime != null) secondsToTimeString(sch.endTime, false) else "17:00",
                    daysOfWeek = schedule.daysActive.toSet(),
                    isEnabled = schedule.isEnabled,
                    hasEndDate = sch.endDate != null,
                    endDate = sch.endDate,
                    deviceType = schedule.deviceType
                )
            }
            if (data != null) {
                _uiState.update {
                    it.copy(
                        isAddEditSheetOpen = true,
                        editingSchedule = data
                    )
                }
            }
        }
    }

    fun onCloseAddEditSheet() {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = false,
                editingSchedule = null
            )
        }
    }

    fun onSaveSchedule(data: AddEditScheduleData) {
        viewModelScope.launch {
            val deviceId = getDeviceIdForRoom(data.deviceName)
            val channelNum = data.relayId.substringAfterLast(" ", "1").toIntOrNull() ?: 1
            val fullRelayId = "relay_$channelNum"

            val startSeconds = timeStringToSeconds(if (data.scheduleType.contains("Time", ignoreCase = true)) data.time else data.startTime)
            val endSeconds = if (data.scheduleType.contains("Duration", ignoreCase = true)) timeStringToSeconds(data.endTime) else null

            val daysInts = data.daysOfWeek.mapNotNull { dayAbbrevMap[it] }

            val scheduleId = data.id ?: "schedule_${System.currentTimeMillis()}"
            val entity = ScheduleEntity(
                id = scheduleId,
                deviceId = deviceId,
                relayId = fullRelayId,
                action = data.action.contains("ON", ignoreCase = true),
                startTime = startSeconds,
                endTime = endSeconds,
                daysOfWeek = daysInts,
                isEnabled = data.isEnabled,
                createdAt = System.currentTimeMillis(),
                isSynced = false,
                endDate = if (data.hasEndDate) data.endDate else null,
                scheduleType = data.scheduleType
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                scheduleDao.insertSchedule(entity)
                
                // Sync schedule to Firebase Realtime Database
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("schedules/$deviceId/$scheduleId")
                        .setValue(mapOf(
                            "id" to scheduleId,
                            "relayId" to fullRelayId,
                            "action" to entity.action,
                            "startTime" to startSeconds,
                            "endTime" to endSeconds,
                            "daysOfWeek" to daysInts,
                            "isEnabled" to entity.isEnabled,
                            "createdAt" to entity.createdAt,
                            "endDate" to entity.endDate,
                            "scheduleType" to entity.scheduleType
                        ))
                }
            }
            _uiState.update {
                it.copy(
                    isAddEditSheetOpen = false,
                    editingSchedule = null
                )
            }
        }
    }

    fun onToggleSchedule(scheduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                scheduleDao.updateScheduleEnabledState(scheduleId, isEnabled, false)
                
                // Sync status to Firebase Realtime Database
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val schedule = scheduleDao.getScheduleById(scheduleId)
                    if (schedule != null) {
                        FirebaseDatabase.getInstance()
                            .getReference("schedules/${schedule.deviceId}/$scheduleId/isEnabled")
                            .setValue(isEnabled)
                    }
                }
            }
        }
    }

    fun onDeleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val sch = scheduleDao.getScheduleById(scheduleId)
                if (sch != null) {
                    scheduleDao.deleteSchedule(sch)
                    
                    // Sync deletion to Firebase Realtime Database
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        FirebaseDatabase.getInstance()
                            .getReference("schedules/${sch.deviceId}/$scheduleId")
                            .removeValue()
                    }
                }
            }
        }
    }

    fun onFilterChanged(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private suspend fun getDeviceIdForRoom(room: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            deviceDao.getDevicesFlow().first().find { it.name == room }?.id ?: ""
        }
    }

    private fun getDeviceIndex(deviceName: String): Int {
        return when (deviceName) {
            "Living Room" -> 1
            "Bedroom" -> 2
            "Kitchen" -> 3
            "Guestroom" -> 4
            else -> 1
        }
    }

    private fun timeStringToSeconds(timeStr: String): Int {
        return try {
            val trimmed = timeStr.trim().uppercase()
            if (trimmed.contains("AM") || trimmed.contains("PM")) {
                val parts = trimmed.split(":")
                var hour = parts[0].toInt()
                val minPart = parts[1].split(" ")
                val min = minPart[0].toInt()
                val isPm = minPart[1].equals("PM", ignoreCase = true)
                if (isPm && hour != 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                hour * 3600 + min * 60
            } else {
                val parts = trimmed.split(":")
                val hour = parts[0].toInt()
                val min = parts[1].toInt()
                hour * 3600 + min * 60
            }
        } catch (e: Exception) {
            7 * 3600
        }
    }

    private fun secondsToTimeString(seconds: Int, amPmFormat: Boolean): String {
        val hour = seconds / 3600
        val min = (seconds % 3600) / 60
        return if (amPmFormat) {
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            val amPm = if (hour >= 12) "PM" else "AM"
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, min, amPm)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hour, min)
        }
    }


}
