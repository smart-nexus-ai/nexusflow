package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relay_states",
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
data class RelayStateEntity(
    @PrimaryKey val id: String, // Composite: "{deviceId}_{relayId}"
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "relay_id") val relayId: String,
    val name: String,
    @ColumnInfo(name = "power_watts") val powerWatts: Int?,
    val type: String?,
    @ColumnInfo(name = "is_on") val isOn: Boolean,
    @ColumnInfo(name = "is_pending") val isPending: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
