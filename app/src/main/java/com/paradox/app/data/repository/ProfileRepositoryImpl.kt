package com.paradox.app.data.repository

import com.paradox.app.core.security.CredentialHasher
import com.paradox.app.data.local.dao.CategoryDao
import com.paradox.app.data.local.dao.PaymentMethodDao
import com.paradox.app.data.local.dao.ProfileDao
import com.paradox.app.data.local.entity.ProfileEntity
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.Profile
import com.paradox.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val credentialHasher: CredentialHasher
) : ProfileRepository {

    override fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getProfileById(profileId: String): Profile? {
        return profileDao.getProfileById(profileId)?.toDomain()
    }

    override fun observeProfileById(profileId: String): Flow<Profile?> {
        return profileDao.observeProfileById(profileId).map { it?.toDomain() }
    }

    override suspend fun createProfile(
        name: String,
        authType: AuthType,
        credentialPlainText: String,
        biometricEnabled: Boolean
    ): Profile {
        val profileId = "prof_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val hash = credentialHasher.hashCredential(credentialPlainText)

        val entity = ProfileEntity(
            id = profileId,
            name = name.trim(),
            primaryAuthType = authType.name,
            credentialHash = hash,
            biometricEnabled = biometricEnabled,
            createdAt = Instant.now()
        )

        profileDao.insertProfile(entity)

        // Seed starter categories & payment methods scoped to this profile
        val starterCategories = Category.createStarterCategories(profileId).map { it.toEntity() }
        categoryDao.insertCategories(starterCategories)

        val starterPaymentMethods = PaymentMethod.createStarterPaymentMethods(profileId).map { it.toEntity() }
        paymentMethodDao.insertPaymentMethods(starterPaymentMethods)

        return entity.toDomain()
    }

    override suspend fun verifyCredential(profileId: String, credentialPlainText: String): Boolean {
        val profile = profileDao.getProfileById(profileId) ?: return false
        return credentialHasher.verifyCredential(credentialPlainText, profile.credentialHash)
    }

    override suspend fun updateProfile(profile: Profile) {
        val existing = profileDao.getProfileById(profile.id) ?: return
        profileDao.updateProfile(
            existing.copy(
                name = profile.name.trim(),
                biometricEnabled = profile.biometricEnabled
            )
        )
    }

    override suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfileById(profileId)
    }

    override suspend fun getProfileCount(): Int {
        return profileDao.getProfileCount()
    }
}
