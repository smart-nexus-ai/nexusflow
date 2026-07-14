package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_uid") val ownerUid: String,
    val name: String,
    @ColumnInfo(name = "hardware_id") val hardwareId: String,
    @ColumnInfo(name = "relay_count") val relayCount: Int,
    @ColumnInfo(name = "firmware_version") val firmwareVersion: String,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean,
    @ColumnInfo(name = "uptime_seconds") val uptimeSeconds: Long = 0L,
    @ColumnInfo(name = "device_type") val deviceType: String = "DEFAULT",
    val temperature: Float = 25.0f,
    val humidity: Float = 60.0f,
    val humidex: Float = 25.0f
)
