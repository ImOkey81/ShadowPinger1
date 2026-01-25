package com.example.shadow.core.agent

import com.example.shadow.core.contracts.ClientErrorReport
import com.example.shadow.core.contracts.Heartbeat
import com.example.shadow.core.jobs.JobExecutor
import com.example.shadow.core.monitor.BatteryMonitor
import com.example.shadow.core.monitor.NetworkMonitor
import com.example.shadow.core.network.KafkaClient
import com.example.shadow.core.telephony.Operator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

class AgentCoordinator(
    private val hwid: String,
    private val stateMachine: AgentStateMachine,
    private val kafkaClient: KafkaClient,
    private val jobExecutor: JobExecutor,
    private val batteryMonitor: BatteryMonitor,
    private val networkMonitor: NetworkMonitor,
) {
    private val _status = MutableStateFlow(
        AgentStatus(
            state = stateMachine.currentState,
            activeOperator = null,
            activeSimLabel = null,
            jobId = null,
            currentIp = null,
            progress = AgentProgress(0, 0, 0),
            lastErrors = emptyList(),
        )
    )
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    private var heartbeatJob: Job? = null
    private var jobListener: Job? = null

    fun start(scope: CoroutineScope, operatorMappings: Map<Operator, String>) {
        batteryMonitor.start()
        networkMonitor.start()
        heartbeatJob = scope.launch(Dispatchers.IO) { sendHeartbeats() }
        jobListener = scope.launch(Dispatchers.IO) {
            kafkaClient.jobs.collect { job ->
                if (!stateMachine.transition(AgentState.TESTING)) return@collect
                val updatedProgress = _status.value.progress.copy(subnetsTotal = job.subnets.size)
                _status.value = _status.value.copy(
                    state = stateMachine.currentState,
                    jobId = job.jobId,
                    progress = updatedProgress,
                )
                val finalResult = jobExecutor.execute(
                    job = job,
                    hwid = hwid,
                    operatorMappings = operatorMappings,
                    onChunkResult = { kafkaClient.sendChunkResult(it) },
                    onExecutionMetrics = { kafkaClient.sendExecutionMetrics(it) },
                )
                stateMachine.transition(AgentState.REPORTING)
                _status.value = _status.value.copy(state = stateMachine.currentState)
                kafkaClient.sendFinalResult(finalResult)
                stateMachine.transition(AgentState.IDLE)
                _status.value = _status.value.copy(
                    state = stateMachine.currentState,
                    jobId = null,
                    progress = AgentProgress(0, 0, 0),
                )
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        jobListener?.cancel()
        batteryMonitor.stop()
        networkMonitor.stop()
    }

    private suspend fun sendHeartbeats() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val statusSnapshot = _status.value
            kafkaClient.sendHeartbeat(
                Heartbeat(
                    hwid = hwid,
                    timestamp = Instant.now().toString(),
                    state = statusSnapshot.state.name,
                    batteryLevel = batteryMonitor.batteryLevel.value,
                    networkType = networkMonitor.networkType.value,
                    activeSim = statusSnapshot.activeOperator?.name ?: "",
                    currentJobId = statusSnapshot.jobId,
                    progress = com.example.shadow.core.contracts.HeartbeatProgress(
                        subnetsTotal = statusSnapshot.progress.subnetsTotal,
                        subnetsCompleted = statusSnapshot.progress.subnetsCompleted,
                        ipsTested = statusSnapshot.progress.ipsTested,
                    ),
                )
            )
            delay(HEARTBEAT_INTERVAL_MS)
        }
    }

    suspend fun reportError(errorType: String, message: String, fatal: Boolean) {
        kafkaClient.sendClientError(
            ClientErrorReport(
                hwid = hwid,
                jobId = _status.value.jobId,
                errorType = errorType,
                message = message,
                fatal = fatal,
                timestamp = Instant.now().toString(),
            )
        )
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 45_000L
    }
}
