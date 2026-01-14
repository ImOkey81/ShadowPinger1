package com.example.shadow.core.agent

import android.content.Context
import android.content.SharedPreferences

class AgentRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): AgentState {
        val stored = prefs.getString(KEY_STATE, AgentState.INIT.name) ?: AgentState.INIT.name
        return runCatching { AgentState.valueOf(stored) }.getOrDefault(AgentState.INIT)
    }

    fun saveState(state: AgentState) {
        prefs.edit().putString(KEY_STATE, state.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "shadow_agent"
        private const val KEY_STATE = "agent_state"
    }
}
