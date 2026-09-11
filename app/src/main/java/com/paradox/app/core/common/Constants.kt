package com.paradox.app.core.common

object Constants {
    const val DATABASE_NAME = "paradox_vault.db"
    const val PREFERENCES_NAME = "paradox_preferences"

    // Default Starter Currencies
    const val DEFAULT_CURRENCY = "INR"
    val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP")

    // Budget Guardrail Defaults
    const val DEFAULT_BUDGET_NEAR_LIMIT_PERCENT = 80

    // Security
    const val KEYSTORE_ALIAS_MASTER = "paradox_vault_master_key"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val PBKDF2_ITERATIONS = 12000
    const val HASH_KEY_LENGTH = 256
}
