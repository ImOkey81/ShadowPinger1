package com.example.shadow.core.telephony

class FakeSimProvider : SimProvider {
    override fun getAllSimCards(): List<SimInfo> {
        return listOf(
            SimInfo(
                subscriptionId = 1,
                displayName = "SIM 1",
                phoneNumber = "+10000000001",
                carrierName = "TestCarrier A",
                slotIndex = 0,
                isEmbedded = false,
            ),
            SimInfo(
                subscriptionId = 2,
                displayName = "eSIM",
                phoneNumber = "+10000000002",
                carrierName = "TestCarrier B",
                slotIndex = 1,
                isEmbedded = true,
            ),
        )
    }
}
