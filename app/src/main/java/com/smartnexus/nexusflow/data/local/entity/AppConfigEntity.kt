package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "maintenance_mode") val maintenanceMode: Boolean,
    @ColumnInfo(name = "maintenance_message") val maintenanceMessage: String?,
    @ColumnInfo(name = "latest_version_code") val latestVersionCode: Int,
    @ColumnInfo(name = "update_type") val updateType: String?,
    @ColumnInfo(name = "terms_url") val termsUrl: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long
)
