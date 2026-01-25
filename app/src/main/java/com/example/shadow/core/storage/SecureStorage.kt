package com.example.shadow.core.storage

import android.content.Context
import android.content.SharedPreferences

class SecureStorage(context: Context, prefsName: String) {
    private val prefs: SharedPreferences = createPrefs(context, prefsName)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    private fun createPrefs(context: Context, prefsName: String): SharedPreferences {
        return runCatching {
            val masterKeyClass = Class.forName("androidx.security.crypto.MasterKey")
            val masterKeyBuilderClass = Class.forName("androidx.security.crypto.MasterKey$Builder")
            val keySchemeClass = Class.forName("androidx.security.crypto.MasterKey$KeyScheme")
            val keyScheme = keySchemeClass.getField("AES256_GCM").get(null)
            val masterKeyBuilder = masterKeyBuilderClass
                .getConstructor(Context::class.java)
                .newInstance(context)
            val builderMethod = masterKeyBuilderClass.getMethod("setKeyScheme", keySchemeClass)
            builderMethod.invoke(masterKeyBuilder, keyScheme)
            val buildMethod = masterKeyBuilderClass.getMethod("build")
            val masterKey = buildMethod.invoke(masterKeyBuilder)

            val encryptedPrefsClass = Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
            val prefKeySchemeClass = Class.forName(
                "androidx.security.crypto.EncryptedSharedPreferences$PrefKeyEncryptionScheme"
            )
            val prefValueSchemeClass = Class.forName(
                "androidx.security.crypto.EncryptedSharedPreferences$PrefValueEncryptionScheme"
            )
            val keyScheme = prefKeySchemeClass.getField("AES256_SIV").get(null)
            val valueScheme = prefValueSchemeClass.getField("AES256_GCM").get(null)
            val createMethod = encryptedPrefsClass.getMethod(
                "create",
                String::class.java,
                String::class.java,
                Context::class.java,
                masterKeyClass,
                prefKeySchemeClass,
                prefValueSchemeClass,
            )
            createMethod.invoke(
                null,
                prefsName,
                prefsName,
                context,
                masterKey,
                keyScheme,
                valueScheme,
            ) as SharedPreferences
        }.getOrElse {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }
    }
}
