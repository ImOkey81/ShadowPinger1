package com.example.shadow.core.ping

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_TIMEOUT_SECONDS = 1

data class PingResult(
    val ip: String,
    val isReachable: Boolean,
    val timeMs: Long?,
    val errorMessage: String? = null,
)

interface IcmpPingEngine {
    suspend fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult
}

class Icmp4aPingEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val bridge: Icmp4aBridge = Icmp4aBridge(),
    private val systemPingFallback: SystemPingFallback = SystemPingFallback(),
) : IcmpPingEngine {
    override suspend fun ping(ip: String, timeoutMs: Int, retries: Int): PingResult =
        withContext(dispatcher) {
            val attempts = (retries.coerceAtLeast(0) + 1)
            var lastResult: PingResult = PingResult(ip, false, null, "No attempts executed")
            repeat(attempts) { attempt ->
                val result = bridge.ping(ip, timeoutMs)
                    ?: systemPingFallback.ping(ip, timeoutMs)
                lastResult = result
                if (result.isReachable) {
                    return@withContext result
                }
                if (attempt < attempts - 1) {
                    kotlinx.coroutines.delay(timeoutMs.milliseconds)
                }
            }
            lastResult
        }
}

class Icmp4aBridge(
    private val classNames: List<String> = listOf(
        "com.github.jraf.android.icmp4a.Icmp4a",
        "com.github.jraf.icmp4a.Icmp4a",
        "icmp4a.Icmp4a",
    ),
) {
    fun ping(ip: String, timeoutMs: Int): PingResult? {
        val icmpClass = classNames.firstNotNullOfOrNull { name ->
            runCatching { Class.forName(name) }.getOrNull()
        } ?: return null

        return runCatching {
            val instance = icmpClass.getDeclaredConstructor().newInstance()
            val pingMethod = icmpClass.methods.firstOrNull { method ->
                method.name == "ping" && method.parameterTypes.size == 1
            } ?: return@runCatching null

            val status = pingMethod.invoke(instance, ip) ?: return@runCatching null
            val reachable = extractBoolean(status, "isReachable")
            val latency = extractLong(status, "timeMs")
            PingResult(ip = ip, isReachable = reachable, timeMs = latency)
        }.getOrNull()
    }

    private fun extractBoolean(target: Any, property: String): Boolean {
        val method = target.javaClass.methods.firstOrNull { it.name == property }
        if (method != null) {
            return (method.invoke(target) as? Boolean) ?: false
        }
        val field = target.javaClass.declaredFields.firstOrNull { it.name == property }
        if (field != null) {
            field.isAccessible = true
            return (field.get(target) as? Boolean) ?: false
        }
        return false
    }

    private fun extractLong(target: Any, property: String): Long? {
        val methodName = "get" + property.replaceFirstChar { it.titlecase() }
        val method = target.javaClass.methods.firstOrNull { it.name == methodName }
        if (method != null) {
            return (method.invoke(target) as? Number)?.toLong()
        }
        val field = target.javaClass.declaredFields.firstOrNull { it.name == property }
        if (field != null) {
            field.isAccessible = true
            return (field.get(target) as? Number)?.toLong()
        }
        return null
    }
}

class SystemPingFallback {
    fun ping(ip: String, timeoutMs: Int): PingResult {
        val timeoutSeconds = ceil(timeoutMs / 1000.0).toInt().coerceAtLeast(MIN_TIMEOUT_SECONDS)
        return runCatching {
            val process = ProcessBuilder(
                "ping",
                "-c",
                "1",
                "-W",
                timeoutSeconds.toString(),
                ip,
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            val latency = parseLatencyMs(output)
            PingResult(
                ip = ip,
                isReachable = exitCode == 0 && latency != null,
                timeMs = latency,
                errorMessage = if (exitCode == 0) null else output.trim(),
            )
        }.getOrElse { error ->
            PingResult(ip = ip, isReachable = false, timeMs = null, errorMessage = error.message)
        }
    }

    private fun parseLatencyMs(output: String): Long? {
        val regex = Regex("time=([0-9.]+) ms")
        val match = regex.find(output) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.toLong()
    }
}
