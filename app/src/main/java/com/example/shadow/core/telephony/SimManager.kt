package com.example.shadow.core.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

class SimManager(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun getAllSimCards(): List<SimInfo> {
        val subscriptionManager = context.getSystemService(
            Context.TELEPHONY_SUBSCRIPTION_SERVICE
        ) as SubscriptionManager

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

    fun switchDataSubscription(targetSubscriptionId: Int): SimSwitchResult {
        val subscriptionManager = context.getSystemService(
            Context.TELEPHONY_SUBSCRIPTION_SERVICE
        ) as SubscriptionManager

        val targetSim = subscriptionManager.getActiveSubscriptionInfo(targetSubscriptionId)
            ?: return SimSwitchResult.Failure("Target SIM not found", isPermissionIssue = false)

        return try {
            val method = SubscriptionManager::class.java.getDeclaredMethod(
                "setDefaultDataSubscriptionId",
                Int::class.javaPrimitiveType,
            )
            method.invoke(subscriptionManager, targetSubscriptionId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telephonyManager = context.getSystemService(
                    Context.TELEPHONY_SERVICE
                ) as TelephonyManager
                telephonyManager.createForSubscriptionId(targetSubscriptionId).setDataEnabled(true)
            }
            SimSwitchResult.Success(targetSim.subscriptionId)
        } catch (exception: ReflectiveOperationException) {
            SimSwitchResult.Failure(
                reason = "Failed to switch data subscription: ${exception.message}",
                isPermissionIssue = false,
            )
        } catch (exception: SecurityException) {
            SimSwitchResult.Failure(
                reason = "Permission denied. Requires system app signature.",
                isPermissionIssue = true,
            )
        } catch (exception: IllegalArgumentException) {
            SimSwitchResult.Failure(
                reason = "Failed to switch data subscription: ${exception.message}",
                isPermissionIssue = false,
            )
        }
    }
}
