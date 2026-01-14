package com.example.shadow.core.contracts

data class SubnetJob(
    val jobId: String,
    val createdAt: String,
    val ttlSeconds: Int,
    val subnets: List<String>,
    val mobileOperators: List<String>,
    val pingConfig: PingConfig,
)

data class PingConfig(
    val method: String,
    val timeoutMs: Int,
    val retries: Int,
    val concurrency: Int,
    val samplingRatio: Double,
)
