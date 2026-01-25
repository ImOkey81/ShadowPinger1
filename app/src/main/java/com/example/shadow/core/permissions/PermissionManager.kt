package com.example.shadow.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionType {
    FOREGROUND_SERVICE,
    BATTERY_OPTIMIZATION,
    MOBILE_NETWORK,
    SIM_ACCESS,
}

class PermissionManager(private val context: Context) {
    fun isGranted(type: PermissionType): Boolean {
        return when (type) {
            PermissionType.FOREGROUND_SERVICE -> true
            PermissionType.BATTERY_OPTIMIZATION -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
            PermissionType.MOBILE_NETWORK -> true
            PermissionType.SIM_ACCESS -> ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun batteryOptimizationIntent(): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
    }
}
