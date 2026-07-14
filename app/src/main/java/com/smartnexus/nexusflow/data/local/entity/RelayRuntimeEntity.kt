package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relay_runtime",
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
data class RelayRuntimeEntity(
    @PrimaryKey @ColumnInfo(name = "relay_id") val relayId: String, // Composite: "{deviceId}_{relayId}"
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "lifetime_minutes") val lifetimeMinutes: Long,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean
)
