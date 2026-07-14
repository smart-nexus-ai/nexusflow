package com.smartnexus.nexusflow.domain.model

data class AutomationRule(
    val id: String,
    val deviceId: String,
    val relayId: String,
    val sensorMode: String, // "temperature", "humidity", "both"
    val logicalOperator: String?, // "and", "or"
    val tempCondition: String?, // "above", "below"
    val tempThreshold: Double?,
    val humidityCondition: String?, // "above", "below"
    val humidityThreshold: Double?,
    val action: Boolean, // true = ON, false = OFF
    val isEnabled: Boolean,
    val lastTriggeredAt: Long?,
    val createdAt: Long
)
