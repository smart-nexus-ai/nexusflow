package com.smartnexus.nexusflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smartnexus.nexusflow.data.local.entity.SceneEntity
import com.smartnexus.nexusflow.data.local.entity.SceneRelayStateEntity
import kotlinx.coroutines.flow.Flow

data class SceneWithRelayStates(
    @androidx.room.Embedded val scene: SceneEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "scene_id"
    )
    val relayStates: List<SceneRelayStateEntity>
)

@Dao
abstract class SceneDao {
    @Query("SELECT * FROM scenes")
    abstract fun getAllScenes(): List<SceneEntity>

    @Transaction
    @Query("SELECT * FROM scenes ORDER BY created_at DESC")
    abstract fun getAllScenesWithRelayStatesFlow(): Flow<List<SceneWithRelayStates>>

    @Transaction
    @Query("SELECT * FROM scenes WHERE device_id = :deviceId ORDER BY created_at DESC")
    abstract fun getScenesWithRelayStatesForDeviceFlow(deviceId: String): Flow<List<SceneWithRelayStates>>

    @Transaction
    @Query("SELECT * FROM scenes WHERE device_id = :deviceId ORDER BY created_at DESC")
    abstract fun getScenesWithRelayStatesForDevice(deviceId: String): List<SceneWithRelayStates>

    @Transaction
    @Query("SELECT * FROM scenes WHERE id = :id LIMIT 1")
    abstract fun getSceneWithRelayStatesById(id: String): SceneWithRelayStates?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertSceneIgnore(scene: SceneEntity): Long

    @androidx.room.Update
    abstract fun updateScene(scene: SceneEntity): Int

    @Transaction
    open fun insertScene(scene: SceneEntity): Long {
        val id = insertSceneIgnore(scene)
        return if (id == -1L) {
            updateScene(scene)
            1L
        } else {
            id
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSceneRelayStates(relayStates: List<SceneRelayStateEntity>): List<Long>

    @Query("DELETE FROM scene_relay_states WHERE scene_id = :sceneId")
    abstract fun deleteRelayStatesForScene(sceneId: String): Int

    @Transaction
    open fun insertSceneWithRelayStates(scene: SceneEntity, relayStates: List<SceneRelayStateEntity>): Boolean {
        insertScene(scene)
        deleteRelayStatesForScene(scene.id)
        insertSceneRelayStates(relayStates)
        return true
    }

    @Delete
    abstract fun deleteScene(scene: SceneEntity): Int

    @Transaction
    open fun deleteSceneWithRelayStates(scene: SceneEntity): Boolean {
        deleteRelayStatesForScene(scene.id)
        deleteScene(scene)
        return true
    }
}
