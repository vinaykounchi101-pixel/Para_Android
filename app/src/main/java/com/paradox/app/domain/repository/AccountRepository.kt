package com.paradox.app.domain.repository

import com.paradox.app.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(profileId: String): Flow<List<Account>>
    suspend fun getAccountById(profileId: String, accountId: String): Account?
    fun observeAccountById(profileId: String, accountId: String): Flow<Account?>
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(profileId: String, accountId: String)
    suspend fun seedDefaultAccounts(profileId: String)
}
