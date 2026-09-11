package com.paradox.app.core.security

import android.util.Base64
import com.paradox.app.core.common.Constants
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialHasher @Inject constructor() {

    fun hashCredential(credential: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = pbkdf2(credential.toCharArray(), salt, Constants.PBKDF2_ITERATIONS, Constants.HASH_KEY_LENGTH)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return "$saltB64:$hashB64"
    }

    fun verifyCredential(credential: String, storedHashWithSalt: String): Boolean {
        val parts = storedHashWithSalt.split(":")
        if (parts.size != 2) return false

        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)

        val computedHash = pbkdf2(credential.toCharArray(), salt, Constants.PBKDF2_ITERATIONS, Constants.HASH_KEY_LENGTH)
        return computedHash.contentEquals(expectedHash)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }
}
