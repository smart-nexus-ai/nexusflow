package com.smartnexus.nexusflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scene_relay_states",
    foreignKeys = [
        ForeignKey(
            entity = SceneEntity::class,
            parentColumns = ["id"],
            childColumns = ["scene_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scene_id"])]
)
data class SceneRelayStateEntity(
    @PrimaryKey val id: String, // Composite: "{sceneId}_{relayId}"
    @ColumnInfo(name = "scene_id") val sceneId: String,
    @ColumnInfo(name = "relay_id") val relayId: String,
    @ColumnInfo(name = "is_on") val isOn: Boolean
)
