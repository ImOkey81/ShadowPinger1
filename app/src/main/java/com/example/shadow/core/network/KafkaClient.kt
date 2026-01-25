package com.example.shadow.core.network

import com.example.shadow.core.contracts.ChunkResult
import com.example.shadow.core.contracts.ClientErrorReport
import com.example.shadow.core.contracts.ExecutionMetrics
import com.example.shadow.core.contracts.FinalResult
import com.example.shadow.core.contracts.Heartbeat
import com.example.shadow.core.contracts.SubnetJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface KafkaClient {
    val jobs: Flow<SubnetJob>
    suspend fun sendHeartbeat(heartbeat: Heartbeat)
    suspend fun sendChunkResult(result: ChunkResult)
    suspend fun sendExecutionMetrics(metrics: ExecutionMetrics)
    suspend fun sendFinalResult(result: FinalResult)
    suspend fun sendClientError(error: ClientErrorReport)
}

class InMemoryKafkaClient : KafkaClient {
    private val jobsFlow = MutableSharedFlow<SubnetJob>(extraBufferCapacity = 16)

    override val jobs: Flow<SubnetJob> = jobsFlow.asSharedFlow()

    suspend fun emitJob(job: SubnetJob) {
        jobsFlow.emit(job)
    }

    override suspend fun sendHeartbeat(heartbeat: Heartbeat) {
        // TODO: Replace with Kafka producer implementation.
    }

    override suspend fun sendChunkResult(result: ChunkResult) {
        // TODO: Replace with Kafka producer implementation.
    }

    override suspend fun sendExecutionMetrics(metrics: ExecutionMetrics) {
        // TODO: Replace with Kafka producer implementation.
    }

    override suspend fun sendFinalResult(result: FinalResult) {
        // TODO: Replace with Kafka producer implementation.
    }

    override suspend fun sendClientError(error: ClientErrorReport) {
        // TODO: Replace with Kafka producer implementation.
    }
}
