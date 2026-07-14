package com.smartnexus.nexusflow.domain.model

data class Schedule(
    val id: String,
    val deviceId: String,
    val relayId: String,
    val action: Boolean,
    val startTime: Int, // seconds from midnight
    val endTime: Int?, // seconds from midnight for duration schedule
    val daysOfWeek: List<Int>, // 1 = Sun, 2 = Mon, ...
    val isEnabled: Boolean,
    val createdAt: Long
)
