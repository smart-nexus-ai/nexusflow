package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_queue")
data class QueuedCommandEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "relay_id") val relayId: String,
    val action: Boolean, // true = ON, false = OFF
    val source: String, // "app", "scene", "schedule"
    @ColumnInfo(name = "queued_at") val queuedAt: Long,
    @ColumnInfo(name = "retry_count") val retryCount: Int
)
