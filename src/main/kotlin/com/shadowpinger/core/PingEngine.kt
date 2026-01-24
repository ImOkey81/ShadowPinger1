package com.shadowpinger.core

data class PingResult(
    val ip: String,
    val isReachable: Boolean,
    val timeMs: Long?,
)

interface PingEngine {
    fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult
}

class PlaceholderPingEngine : PingEngine {
    override fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult {
        return PingResult(
            ip = ip,
            isReachable = false,
            timeMs = null,
        )
    }
}
