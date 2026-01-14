package com.example.shadow.core.agent

enum class AgentState {
    INIT,
    REGISTERED,
    AUTHORIZED,
    PERMISSIONS_GRANTED,
    SIMS_MAPPED,
    KAFKA_REGISTERED,
    IDLE,
    TESTING,
    REPORTING,
}
