package com.example.shadow.core.storage

import android.content.Context

class AuthTokenStore(context: Context) {
    private val storage = SecureStorage(context, PREFS_NAME)

    fun saveToken(token: String) {
        storage.putString(KEY_TOKEN, token)
    }

    fun loadToken(): String? = storage.getString(KEY_TOKEN)

    companion object {
        private const val PREFS_NAME = "shadow_auth"
        private const val KEY_TOKEN = "auth_token"
    }
}
