package com.shadowpinger.core

data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val phoneNumber: String?,
    val carrierName: String,
    val slotIndex: Int,
    val isEmbedded: Boolean,
)

sealed class SimSwitchResult {
    data class Success(val subscriptionId: Int) : SimSwitchResult()

    data class Failure(
        val reason: String,
        val isPermissionIssue: Boolean,
    ) : SimSwitchResult()
}

class SimManager(
    private val simCards: List<SimInfo> = emptyList(),
) {
    fun getAllSimCards(): List<SimInfo> = simCards

    fun switchDataSubscription(targetSubscriptionId: Int): SimSwitchResult {
        val targetSim = simCards.firstOrNull { it.subscriptionId == targetSubscriptionId }
            ?: return SimSwitchResult.Failure("Target SIM not found", isPermissionIssue = false)

        return SimSwitchResult.Success(targetSim.subscriptionId)
    }
}
