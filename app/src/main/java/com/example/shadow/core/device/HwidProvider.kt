package com.example.shadow.core.device

import android.content.Context
import java.util.UUID

class HwidProvider(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        val existing = prefs.getString(KEY_HWID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_HWID, created).apply()
        return created
    }

    companion object {
        private const val PREFS_NAME = "shadow_agent"
        private const val KEY_HWID = "hwid"
    }
}
