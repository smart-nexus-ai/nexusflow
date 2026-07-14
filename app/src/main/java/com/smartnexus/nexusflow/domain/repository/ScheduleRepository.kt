package com.smartnexus.nexusflow.domain.repository

import com.smartnexus.nexusflow.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getSchedulesForDevice(deviceId: String): Flow<List<Schedule>>
    suspend fun insertSchedule(schedule: Schedule): Result<Unit>
    suspend fun deleteSchedule(id: String): Result<Unit>
    suspend fun updateScheduleEnabled(id: String, isEnabled: Boolean): Result<Unit>
}
