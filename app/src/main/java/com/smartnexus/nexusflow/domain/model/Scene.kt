package com.smartnexus.nexusflow.domain.model

data class Scene(
    val id: String,
    val deviceId: String,
    val name: String,
    val icon: String,
    val isFavorite: Boolean,
    val showOnHome: Boolean,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val relayStates: List<SceneRelayState> = emptyList()
)

data class SceneRelayState(
    val id: String,
    val sceneId: String,
    val relayId: String,
    val isOn: Boolean
)
