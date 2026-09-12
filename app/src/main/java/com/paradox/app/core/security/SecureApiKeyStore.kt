package com.paradox.app.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun hasApiKey(): Boolean {
        val encKey = prefs.getString(PREF_ENCRYPTED_KEY, null)
        val iv = prefs.getString(PREF_KEY_IV, null)
        return !encKey.isNullOrBlank() && !iv.isNullOrBlank()
    }

    @Synchronized
    fun getApiKey(): String? {
        val encKeyB64 = prefs.getString(PREF_ENCRYPTED_KEY, null) ?: return null
        val ivB64 = prefs.getString(PREF_KEY_IV, null) ?: return null

        return try {
            val cipherText = Base64.decode(encKeyB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val decryptedBytes = keystoreManager.decrypt(cipherText, iv)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun saveApiKey(apiKey: String): Result<Unit> {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("API Key cannot be empty"))
        }

        return try {
            val (cipherText, iv) = keystoreManager.encrypt(trimmed.toByteArray(Charsets.UTF_8))
            prefs.edit(commit = true) {
                putString(PREF_ENCRYPTED_KEY, Base64.encodeToString(cipherText, Base64.NO_WRAP))
                putString(PREF_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Synchronized
    fun removeApiKey(): Result<Unit> {
        return try {
            prefs.edit(commit = true) {
                remove(PREF_ENCRYPTED_KEY)
                remove(PREF_KEY_IV)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "paradox_ai_sec"
        private const val PREF_ENCRYPTED_KEY = "enc_ai_api_key"
        private const val PREF_KEY_IV = "ai_api_key_iv"
    }
}
