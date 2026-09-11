package com.paradox.app.data.mapper

import com.paradox.app.data.local.entity.ProfileEntity
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Profile

fun ProfileEntity.toDomain(): Profile {
    val authType = try {
        AuthType.valueOf(primaryAuthType)
    } catch (_: Exception) {
        AuthType.PIN
    }
    return Profile(
        id = id,
        name = name,
        primaryAuthType = authType,
        biometricEnabled = biometricEnabled,
        createdAt = createdAt
    )
}

fun Profile.toEntity(credentialHash: String): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        primaryAuthType = primaryAuthType.name,
        credentialHash = credentialHash,
        biometricEnabled = biometricEnabled,
        createdAt = createdAt
    )
}
