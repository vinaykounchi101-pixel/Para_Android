package com.paradox.app.domain.usecase.account

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(profileId: String): Flow<List<Account>> {
        return accountRepository.getAccounts(profileId)
    }

    suspend fun seedDefaultsIfNeeded(profileId: String) {
        accountRepository.seedDefaultAccounts(profileId)
    }
}

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        profileId: String,
        name: String,
        type: AccountType,
        currency: String = "INR",
        initialBalance: Money = Money.zero(currency),
        colorHex: String = "#3B82F6",
        iconName: String = type.defaultIcon,
        isDefault: Boolean = false
    ): Result<Account> {
        return try {
            if (name.isBlank()) {
                return Result.Error(IllegalArgumentException("Account name cannot be blank"))
            }

            val account = Account(
                id = "acc_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                name = name.trim(),
                type = type,
                currency = currency,
                initialBalance = initialBalance,
                colorHex = colorHex,
                iconName = iconName,
                isDefault = isDefault,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            accountRepository.addAccount(account)
            Result.Success(account)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        return try {
            if (account.name.isBlank()) {
                return Result.Error(IllegalArgumentException("Account name cannot be blank"))
            }
            accountRepository.updateAccount(account.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(profileId: String, accountId: String): Result<Unit> {
        return try {
            accountRepository.deleteAccount(profileId, accountId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
