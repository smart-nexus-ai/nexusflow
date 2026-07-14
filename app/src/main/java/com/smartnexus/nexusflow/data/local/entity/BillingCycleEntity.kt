package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "billing_cycles",
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
data class BillingCycleEntity(
    @PrimaryKey val id: String, // e.g. UUID
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "start_runtime") val startRuntime: Long // in minutes
)
