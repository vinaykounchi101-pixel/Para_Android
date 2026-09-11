package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val primaryAuthType: String, // "PIN", "PASSWORD", "PATTERN"
    val credentialHash: String,
    val biometricEnabled: Boolean,
    val createdAt: Instant = Instant.now()
)
