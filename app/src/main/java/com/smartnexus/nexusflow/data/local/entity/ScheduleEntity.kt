package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["device_id"])]
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "relay_id") val relayId: String,
    val action: Boolean, // true = ON, false = OFF
    @ColumnInfo(name = "start_time") val startTime: Int, // seconds since midnight
    @ColumnInfo(name = "end_time") val endTime: Int?, // optional
    @ColumnInfo(name = "days_of_week") val daysOfWeek: List<Int>, // converted to/from JSON string
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    @ColumnInfo(name = "schedule_type") val scheduleType: String = "Specific Time"
)
