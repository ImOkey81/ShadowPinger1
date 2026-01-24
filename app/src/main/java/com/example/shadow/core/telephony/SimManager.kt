package com.example.shadow.core.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

class SimManager(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    fun getAllSimCards(): List<SimInfo> {
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as SubscriptionManager

        val infos: List<SubscriptionInfo> =
            subscriptionManager.activeSubscriptionInfoList ?: emptyList()

        return infos.map { info ->
            SimInfo(
                subscriptionId = info.subscriptionId,
                displayName = info.displayName?.toString().orEmpty(),
                phoneNumber = info.number,
                carrierName = info.carrierName?.toString().orEmpty(),
                slotIndex = info.simSlotIndex,
                isEmbedded = info.isEmbedded,
            )
        }
    }

    /**
     * Возвращает TelephonyManager для конкретной SIM
     * (это единственное, что разрешено обычному приложению)
     */
    fun getTelephonyManagerForSim(subscriptionId: Int): TelephonyManager {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.createForSubscriptionId(subscriptionId)
    }
}
