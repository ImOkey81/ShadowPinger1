package com.example.shadow.core.contracts

data class FinalResult(
    val jobId: String,
    val hwid: String,
    val finishedAt: String,
    val summary: FinalSummary,
    val operators: Map<String, OperatorSummary>,
)

data class FinalSummary(
    val totalIpsTested: Int,
    val totalIpsUp: Int,
    val avgLatencyMs: Double,
)

data class OperatorSummary(
    val subnets: Map<String, SubnetSummary>,
)

data class SubnetSummary(
    val availableHosts: List<String>,
    val totalAvailableHosts: Int,
)
