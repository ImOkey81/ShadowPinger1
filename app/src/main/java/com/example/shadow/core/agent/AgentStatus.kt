package com.example.shadow.core.agent

import com.example.shadow.core.telephony.Operator

data class AgentStatus(
    val state: AgentState,
    val activeOperator: Operator?,
    val activeSimLabel: String?,
    val jobId: String?,
    val currentIp: String?,
    val progress: AgentProgress,
    val lastErrors: List<String>,
)

data class AgentProgress(
    val subnetsTotal: Int,
    val subnetsCompleted: Int,
    val ipsTested: Int,
)
