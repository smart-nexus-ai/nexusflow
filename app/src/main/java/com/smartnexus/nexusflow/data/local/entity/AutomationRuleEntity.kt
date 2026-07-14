package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "automation_rules",
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
data class AutomationRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "relay_id") val relayId: String,
    @ColumnInfo(name = "sensor_mode") val sensorMode: String, // "temperature", "humidity", "both"
    @ColumnInfo(name = "logical_operator") val logicalOperator: String?, // "and", "or", null
    @ColumnInfo(name = "temp_condition") val tempCondition: String?, // "above", "below", null
    @ColumnInfo(name = "temp_threshold") val tempThreshold: Double?,
    @ColumnInfo(name = "humidity_condition") val humidityCondition: String?, // "above", "below", null
    @ColumnInfo(name = "humidity_threshold") val humidityThreshold: Double?,
    val action: Boolean, // true = ON, false = OFF
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "last_triggered_at") val lastTriggeredAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean
)
