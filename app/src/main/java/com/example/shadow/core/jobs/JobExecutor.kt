package com.example.shadow.core.jobs

import com.example.shadow.core.contracts.ChunkResult
import com.example.shadow.core.contracts.ExecutionMetrics
import com.example.shadow.core.contracts.ExecutionMetricsDetails
import com.example.shadow.core.contracts.FinalResult
import com.example.shadow.core.contracts.FinalSummary
import com.example.shadow.core.contracts.IpRange
import com.example.shadow.core.contracts.IpResult
import com.example.shadow.core.contracts.OperatorSummary
import com.example.shadow.core.contracts.SubnetJob
import com.example.shadow.core.contracts.SubnetSummary
import com.example.shadow.core.ip.IpAddressUtils
import com.example.shadow.core.ip.IpSampling
import com.example.shadow.core.ping.IcmpPingEngine
import com.example.shadow.core.telephony.Operator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.roundToLong

class JobExecutor(
    private val pingEngine: IcmpPingEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun execute(
        job: SubnetJob,
        hwid: String,
        operatorMappings: Map<Operator, String>,
        onChunkResult: suspend (ChunkResult) -> Unit,
        onExecutionMetrics: suspend (ExecutionMetrics) -> Unit,
    ): FinalResult = coroutineScope {
        val operatorSummaries = mutableMapOf<String, OperatorSummary>()
        var totalIpsTested = 0
        var totalIpsUp = 0
        val latencyValues = mutableListOf<Long>()

        job.mobileOperators.forEach { operatorName ->
            val operator = runCatching { Operator.valueOf(operatorName) }.getOrNull()
            if (operator == null || operatorMappings[operator] == null) return@forEach

            val subnetSummaries = mutableMapOf<String, SubnetSummary>()
            job.subnets.forEach { subnet ->
                val subnetRange = IpAddressUtils.cidrToRange(subnet)
                val ips = IpAddressUtils.expandRange(subnetRange)
                val sampledIps = IpSampling.sample(ips, job.pingConfig.samplingRatio)
                val chunks = IpSampling.chunk(sampledIps, DEFAULT_CHUNK_SIZE)

                val subnetStats = SubnetStatsTracker(sampledIps.size)
                val semaphore = Semaphore(job.pingConfig.concurrency.coerceAtLeast(1))

                chunks.forEachIndexed { index, chunk ->
                    val results = chunk.map { ip ->
                        async(dispatcher) {
                            semaphore.withPermit {
                                val result = pingEngine.ping(
                                    ip = IpAddressUtils.longToIpv4(ip),
                                    timeoutMs = job.pingConfig.timeoutMs,
                                    retries = job.pingConfig.retries,
                                )
                                subnetStats.register(result)
                                IpResult(
                                    ip = ip,
                                    status = if (result.isReachable) "up" else "down",
                                    latency = result.timeMs,
                                )
                            }
                        }
                    }.awaitAll()

                    onChunkResult(
                        ChunkResult(
                            jobId = job.jobId,
                            hwid = hwid,
                            operator = operator.name,
                            subnet = subnet,
                            chunkId = index,
                            range = IpRange(from = chunk.first(), to = chunk.last()),
                            results = results,
                        )
                    )
                }

                val metrics = subnetStats.toExecutionMetrics(
                    jobId = job.jobId,
                    hwid = hwid,
                    operator = operator.name,
                    subnet = subnet,
                )
                onExecutionMetrics(metrics)

                totalIpsTested += subnetStats.ipsTested
                totalIpsUp += subnetStats.ipsUp
                latencyValues += subnetStats.latencies

                subnetSummaries[subnet] = SubnetSummary(
                    availableHosts = subnetStats.availableHosts.map(IpAddressUtils::longToIpv4),
                    totalAvailableHosts = subnetStats.availableHosts.size,
                )
            }
            operatorSummaries[operator.name] = OperatorSummary(subnets = subnetSummaries)
        }

        val avgLatency = if (latencyValues.isNotEmpty()) {
            latencyValues.average()
        } else {
            0.0
        }

        FinalResult(
            jobId = job.jobId,
            hwid = hwid,
            finishedAt = Instant.now().toString(),
            summary = FinalSummary(
                totalIpsTested = totalIpsTested,
                totalIpsUp = totalIpsUp,
                avgLatencyMs = avgLatency,
            ),
            operators = operatorSummaries,
        )
    }

    private class SubnetStatsTracker(private val ipsTotal: Int) {
        var ipsTested = 0
            private set
        var ipsUp = 0
            private set
        var timeouts = 0
            private set
        var errors = 0
            private set
        val latencies = mutableListOf<Long>()
        val availableHosts = mutableListOf<Long>()

        fun register(result: com.example.shadow.core.ping.PingResult) {
            ipsTested += 1
            if (result.isReachable) {
                ipsUp += 1
                result.timeMs?.let { latencies.add(it) }
                availableHosts.add(IpAddressUtils.ipv4ToLong(result.ip))
            } else if (result.errorMessage?.contains("timeout", ignoreCase = true) == true) {
                timeouts += 1
            } else if (result.errorMessage != null) {
                errors += 1
            }
        }

        fun toExecutionMetrics(
            jobId: String,
            hwid: String,
            operator: String,
            subnet: String,
        ): ExecutionMetrics {
            val avgLatency = if (latencies.isNotEmpty()) latencies.average() else 0.0
            val p95Latency = if (latencies.isNotEmpty()) percentile(latencies, 0.95) else 0.0
            return ExecutionMetrics(
                jobId = jobId,
                hwid = hwid,
                operator = operator,
                subnet = subnet,
                metrics = ExecutionMetricsDetails(
                    ipsTotal = ipsTotal,
                    ipsTested = ipsTested,
                    ipsUp = ipsUp,
                    avgLatencyMs = avgLatency,
                    p95LatencyMs = p95Latency,
                    timeouts = timeouts,
                    errors = errors,
                ),
                timestamp = Instant.now().toString(),
            )
        }

        private fun percentile(values: List<Long>, quantile: Double): Double {
            val sorted = values.sorted()
            val index = ceil(sorted.size * quantile).toInt().coerceAtMost(sorted.lastIndex)
            return sorted[index].toDouble()
        }
    }

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 256
    }
}
