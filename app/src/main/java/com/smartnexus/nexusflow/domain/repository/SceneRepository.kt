package com.smartnexus.nexusflow.domain.repository

import com.smartnexus.nexusflow.domain.model.Scene
import com.smartnexus.nexusflow.domain.model.SceneRelayState
import kotlinx.coroutines.flow.Flow

interface SceneRepository {
    fun getScenesForDevice(deviceId: String): Flow<List<Scene>>
    suspend fun insertScene(scene: Scene, relays: List<SceneRelayState>): Result<Unit>
    suspend fun deleteScene(id: String): Result<Unit>
}
