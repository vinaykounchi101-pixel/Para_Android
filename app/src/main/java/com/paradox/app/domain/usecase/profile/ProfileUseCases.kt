package com.paradox.app.domain.usecase.profile

import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Profile
import com.paradox.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionDataStore: SessionDataStore
) {
    suspend operator fun invoke(
        name: String,
        authType: AuthType,
        credentialPlainText: String,
        biometricEnabled: Boolean
    ): Result<Profile> {
        return try {
            if (name.isBlank()) {
                return Result.Error(IllegalArgumentException("Profile name cannot be blank"))
            }
            if (credentialPlainText.isBlank()) {
                return Result.Error(IllegalArgumentException("Credential cannot be blank"))
            }
            if (authType == AuthType.PIN && credentialPlainText.length < 4) {
                return Result.Error(IllegalArgumentException("PIN must be at least 4 digits"))
            }

            val profile = profileRepository.createProfile(
                name = name,
                authType = authType,
                credentialPlainText = credentialPlainText,
                biometricEnabled = biometricEnabled
            )

            sessionDataStore.setActiveProfileId(profile.id)
            sessionDataStore.setAppLocked(false)

            Result.Success(profile)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UnlockProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionDataStore: SessionDataStore
) {
    suspend operator fun invoke(profileId: String, credentialPlainText: String): Result<Boolean> {
        return try {
            val verified = profileRepository.verifyCredential(profileId, credentialPlainText)
            if (verified) {
                sessionDataStore.setActiveProfileId(profileId)
                sessionDataStore.setAppLocked(false)
                Result.Success(true)
            } else {
                Result.Error(IllegalArgumentException("Incorrect credential"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun unlockViaBiometric(profileId: String): Result<Boolean> {
        return try {
            val profile = profileRepository.getProfileById(profileId)
            if (profile != null && profile.biometricEnabled) {
                sessionDataStore.setActiveProfileId(profileId)
                sessionDataStore.setAppLocked(false)
                Result.Success(true)
            } else {
                Result.Error(IllegalStateException("Biometric unlock not enabled for this profile"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class GetProfilesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(): Flow<List<Profile>> = profileRepository.getAllProfiles()
}

class SwitchProfileUseCase @Inject constructor(
    private val sessionDataStore: SessionDataStore
) {
    suspend operator fun invoke(profileId: String) {
        sessionDataStore.setActiveProfileId(profileId)
        sessionDataStore.setAppLocked(true) // Always require re-authentication on profile switch
    }
}

class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionDataStore: SessionDataStore
) {
    suspend operator fun invoke(profileId: String): Result<Unit> {
        return try {
            profileRepository.deleteProfile(profileId)
            sessionDataStore.setActiveProfileId(null)
            sessionDataStore.setAppLocked(true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
