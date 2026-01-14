package com.example.shadow.core.contracts

data class ChunkResult(
    val jobId: String,
    val hwid: String,
    val operator: String,
    val subnet: String,
    val chunkId: Int,
    val range: IpRange,
    val results: List<IpResult>,
)

data class IpRange(
    val from: Long,
    val to: Long,
)

data class IpResult(
    val ip: Long,
    val status: String,
    val latency: Long?,
)
