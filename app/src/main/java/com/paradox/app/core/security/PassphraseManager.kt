package com.paradox.app.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) {
    private val prefs = context.getSharedPreferences("paradox_vault_sec", Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val encryptedKeyB64 = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val ivB64 = prefs.getString(PREF_PASSPHRASE_IV, null)

        if (encryptedKeyB64 != null && ivB64 != null) {
            val cipherText = Base64.decode(encryptedKeyB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            return keystoreManager.decrypt(cipherText, iv)
        }

        // Generate 256-bit high-entropy random passphrase
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)

        val (cipherText, iv) = keystoreManager.encrypt(rawPassphrase)
        prefs.edit {
            putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(cipherText, Base64.NO_WRAP))
            putString(PREF_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
        }

        return rawPassphrase
    }

    companion object {
        private const val PREF_ENCRYPTED_PASSPHRASE = "enc_passphrase"
        private const val PREF_PASSPHRASE_IV = "passphrase_iv"
    }
}
