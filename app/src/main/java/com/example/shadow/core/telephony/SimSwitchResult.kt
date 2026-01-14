package com.example.shadow.core.telephony

sealed class SimSwitchResult {
    data class Success(val subscriptionId: Int) : SimSwitchResult()
    data class Failure(val reason: String, val isPermissionIssue: Boolean) : SimSwitchResult()
}
