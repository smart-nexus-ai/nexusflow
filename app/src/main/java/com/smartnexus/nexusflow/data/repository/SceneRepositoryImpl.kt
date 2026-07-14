package com.smartnexus.nexusflow.data.repository

import com.smartnexus.nexusflow.data.local.dao.SceneDao
import com.smartnexus.nexusflow.data.local.entity.SceneEntity
import com.smartnexus.nexusflow.data.local.entity.SceneRelayStateEntity
import com.smartnexus.nexusflow.domain.model.Scene
import com.smartnexus.nexusflow.domain.model.SceneRelayState
import com.smartnexus.nexusflow.domain.repository.SceneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneRepositoryImpl @Inject constructor(
    private val sceneDao: SceneDao
) : SceneRepository {

    override fun getScenesForDevice(deviceId: String): Flow<List<Scene>> {
        return sceneDao.getScenesWithRelayStatesForDeviceFlow(deviceId).map { list ->
            list.map { sceneWithRelays ->
                sceneWithRelays.scene.toDomain(
                    sceneWithRelays.relayStates.map { it.toDomain() }
                )
            }
        }
    }

    override suspend fun insertScene(scene: Scene, relays: List<SceneRelayState>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sceneDao.insertSceneWithRelayStates(
                scene.toEntity(),
                relays.map { it.toEntity() }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteScene(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = sceneDao.getSceneWithRelayStatesById(id)
            if (existing != null) {
                sceneDao.deleteSceneWithRelayStates(existing.scene)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper extensions
    private fun SceneEntity.toDomain(relays: List<SceneRelayState>) = Scene(
        id = id,
        deviceId = deviceId,
        name = name,
        icon = icon,
        isFavorite = isFavorite,
        showOnHome = showOnHome,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        relayStates = relays
    )

    private fun Scene.toEntity() = SceneEntity(
        id = id,
        deviceId = deviceId,
        name = name,
        icon = icon,
        isFavorite = isFavorite,
        showOnHome = showOnHome,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isSynced = false
    )

    private fun SceneRelayStateEntity.toDomain() = SceneRelayState(
        id = id,
        sceneId = sceneId,
        relayId = relayId,
        isOn = isOn
    )

    private fun SceneRelayState.toEntity() = SceneRelayStateEntity(
        id = id,
        sceneId = sceneId,
        relayId = relayId,
        isOn = isOn
    )
}
