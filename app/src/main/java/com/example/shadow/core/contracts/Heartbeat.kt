package com.example.shadow.core.contracts

data class Heartbeat(
    val hwid: String,
    val timestamp: String,
    val state: String,
    val batteryLevel: Double,
    val networkType: String,
    val activeSim: String,
    val currentJobId: String?,
    val progress: HeartbeatProgress,
)

data class HeartbeatProgress(
    val subnetsTotal: Int,
    val subnetsCompleted: Int,
    val ipsTested: Int,
)
