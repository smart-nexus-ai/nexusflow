package com.smartnexus.nexusflow.domain.model

data class Device(
    val id: String,
    val ownerUid: String,
    val name: String,
    val hardwareId: String,
    val relayCount: Int,
    val firmwareVersion: String,
    val lastSeen: Long,
    val createdAt: Long
)
