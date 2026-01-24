package com.shadowpinger.app

import com.shadowpinger.core.PlaceholderPingEngine
import com.shadowpinger.core.PingEngine
import com.shadowpinger.core.PingResult

class AgentRunner(
    private val engineFactory: () -> PingEngine = { PlaceholderPingEngine() },
) {
    fun pingOnce(ip: String, timeoutMs: Int, retries: Int): PingResult {
        val engine = engineFactory()
        return engine.ping(ip, timeoutMs, retries)
    }
}
