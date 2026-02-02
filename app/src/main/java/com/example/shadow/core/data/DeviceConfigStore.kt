package com.example.shadow.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.deviceConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "device_config",
)

class DeviceConfigStore(private val context: Context) {
    private val dataStore = context.deviceConfigDataStore

    val deviceToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_DEVICE_TOKEN]
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = dataStore.data.first()[KEY_DEVICE_ID]
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val created = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_ID] = created
        }
        return created
    }

    suspend fun setDeviceToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_TOKEN] = token
        }
    }

    companion object {
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_DEVICE_TOKEN = stringPreferencesKey("device_token")
    }
}
