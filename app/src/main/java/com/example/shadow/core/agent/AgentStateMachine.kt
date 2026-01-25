package com.example.shadow.core.agent

import android.util.Log

class AgentStateMachine(private val repository: AgentRepository) {
    private val allowedTransitions: Map<AgentState, Set<AgentState>> = mapOf(
        AgentState.INIT to setOf(AgentState.REGISTERED),
        AgentState.REGISTERED to setOf(AgentState.AUTHORIZED),
        AgentState.AUTHORIZED to setOf(AgentState.PERMISSIONS_GRANTED),
        AgentState.PERMISSIONS_GRANTED to setOf(AgentState.SIMS_MAPPED),
        AgentState.SIMS_MAPPED to setOf(AgentState.KAFKA_REGISTERED),
        AgentState.KAFKA_REGISTERED to setOf(AgentState.IDLE),
        AgentState.IDLE to setOf(AgentState.TESTING),
        AgentState.TESTING to setOf(AgentState.REPORTING),
        AgentState.REPORTING to setOf(AgentState.IDLE),
    )

    var currentState: AgentState = repository.loadState()
        private set

    fun transition(target: AgentState): Boolean {
        val allowed = allowedTransitions[currentState].orEmpty()
        if (target !in allowed) {
            Log.w(TAG, "Invalid transition ${currentState.name} -> ${target.name}")
            return false
        }
        Log.i(TAG, "Transition ${currentState.name} -> ${target.name}")
        currentState = target
        repository.saveState(target)
        return true
    }

    companion object {
        private const val TAG = "AgentStateMachine"
    }
}
