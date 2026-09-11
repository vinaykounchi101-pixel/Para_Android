package com.paradox.app.domain.repository

import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(profileId: String): Profile?
    fun observeProfileById(profileId: String): Flow<Profile?>
    suspend fun createProfile(name: String, authType: AuthType, credentialPlainText: String, biometricEnabled: Boolean): Profile
    suspend fun verifyCredential(profileId: String, credentialPlainText: String): Boolean
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profileId: String)
    suspend fun getProfileCount(): Int
}
