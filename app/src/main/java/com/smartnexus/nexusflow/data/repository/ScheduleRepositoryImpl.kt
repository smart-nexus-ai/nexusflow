package com.smartnexus.nexusflow.data.repository

import com.smartnexus.nexusflow.data.local.dao.ScheduleDao
import com.smartnexus.nexusflow.data.local.entity.ScheduleEntity
import com.smartnexus.nexusflow.domain.model.Schedule
import com.smartnexus.nexusflow.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {

    override fun getSchedulesForDevice(deviceId: String): Flow<List<Schedule>> {
        return scheduleDao.getSchedulesForDeviceFlow(deviceId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertSchedule(schedule: Schedule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.insertSchedule(schedule.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSchedule(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schedule = scheduleDao.getScheduleById(id)
            if (schedule != null) {
                scheduleDao.deleteSchedule(schedule)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateScheduleEnabled(id: String, isEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.updateScheduleEnabledState(id, isEnabled, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper extensions
    private fun ScheduleEntity.toDomain() = Schedule(
        id = id,
        deviceId = deviceId,
        relayId = relayId,
        action = action,
        startTime = startTime,
        endTime = endTime,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun Schedule.toEntity() = ScheduleEntity(
        id = id,
        deviceId = deviceId,
        relayId = relayId,
        action = action,
        startTime = startTime,
        endTime = endTime,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        createdAt = createdAt,
        isSynced = false
    )
}
