package com.example.shadow.core.ping

data class PingResult(
    val ip: String,
    val isReachable: Boolean,
    val timeMs: Long?,
)

interface IcmpPingEngine {
    suspend fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult
}

class PlaceholderIcmpPingEngine : IcmpPingEngine {
    override suspend fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult {
        return PingResult(
            ip = ip,
            isReachable = false,
            timeMs = null,
        )
    }
}
