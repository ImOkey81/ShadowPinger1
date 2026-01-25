package com.example.shadow.core.telephony

data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val phoneNumber: String?,
    val carrierName: String,
    val slotIndex: Int,
    val isEmbedded: Boolean,
    val simUid: String,
)
