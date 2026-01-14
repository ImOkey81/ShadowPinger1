package com.example.shadow.core.contracts

data class ExecutionMetrics(
    val jobId: String,
    val hwid: String,
    val operator: String,
    val subnet: String,
    val metrics: ExecutionMetricsDetails,
    val timestamp: String,
)

data class ExecutionMetricsDetails(
    val ipsTotal: Int,
    val ipsTested: Int,
    val ipsUp: Int,
    val avgLatencyMs: Double,
    val p95LatencyMs: Double,
    val timeouts: Int,
    val errors: Int,
)
