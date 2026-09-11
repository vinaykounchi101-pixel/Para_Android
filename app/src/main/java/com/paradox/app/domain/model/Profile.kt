package com.paradox.app.domain.model

import java.time.Instant

enum class AuthType {
    PIN,
    PASSWORD,
    PATTERN
}

data class Profile(
    val id: String,
    val name: String,
    val primaryAuthType: AuthType,
    val biometricEnabled: Boolean,
    val createdAt: Instant = Instant.now()
)
