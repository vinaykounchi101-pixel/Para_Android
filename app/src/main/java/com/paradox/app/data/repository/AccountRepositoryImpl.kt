package com.paradox.app.data.repository

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.dao.AccountDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAccounts(profileId: String): Flow<List<Account>> {
        return accountDao.getAccountsByProfile(profileId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAccountById(profileId: String, accountId: String): Account? {
        return accountDao.getAccountById(profileId, accountId)?.toDomain()
    }

    override fun observeAccountById(profileId: String, accountId: String): Flow<Account?> {
        return accountDao.observeAccountById(profileId, accountId).map { it?.toDomain() }
    }

    override suspend fun addAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(profileId: String, accountId: String) {
        accountDao.deleteAccountById(profileId, accountId)
    }

    override suspend fun seedDefaultAccounts(profileId: String) {
        if (accountDao.getAccountCount(profileId) > 0) return

        val defaultAccounts = listOf(
            Account(
                id = "acc_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                name = "Primary Cash",
                type = AccountType.CASH,
                initialBalance = Money(BigDecimal.ZERO, "INR"),
                colorHex = "#10B981",
                iconName = "payments",
                isDefault = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Account(
                id = "acc_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                name = "Main Bank",
                type = AccountType.BANK,
                initialBalance = Money(BigDecimal.ZERO, "INR"),
                colorHex = "#3B82F6",
                iconName = "account_balance",
                isDefault = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Account(
                id = "acc_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                name = "UPI / Wallet",
                type = AccountType.DIGITAL_WALLET,
                initialBalance = Money(BigDecimal.ZERO, "INR"),
                colorHex = "#8B5CF6",
                iconName = "account_balance_wallet",
                isDefault = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        accountDao.insertAccounts(defaultAccounts.map { it.toEntity() })
    }
}
