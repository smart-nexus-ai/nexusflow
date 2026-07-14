package com.smartnexus.nexusflow.domain.model

data class RelayState(
    val id: String,
    val deviceId: String,
    val relayId: String,
    val name: String,
    val powerWatts: Int?,
    val isOn: Boolean,
    val isPending: Boolean,
    val updatedAt: Long
)
