package com.paradox.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.paradox.app.core.common.Constants
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        if (!keyStore.containsAlias(Constants.KEYSTORE_ALIAS_MASTER)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                Constants.KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                Constants.KEYSTORE_ALIAS_MASTER,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    fun encrypt(plainTextBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = keyStore.getKey(Constants.KEYSTORE_ALIAS_MASTER, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainTextBytes)
        return Pair(cipherText, iv)
    }

    fun decrypt(cipherText: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = keyStore.getKey(Constants.KEYSTORE_ALIAS_MASTER, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherText)
    }
}
