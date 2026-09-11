package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY isDefault DESC, createdAt ASC")
    fun getAccountsByProfile(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND id = :accountId LIMIT 1")
    suspend fun getAccountById(profileId: String, accountId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND id = :accountId LIMIT 1")
    fun observeAccountById(profileId: String, accountId: String): Flow<AccountEntity?>

    @Query("SELECT COUNT(*) FROM accounts WHERE profileId = :profileId")
    suspend fun getAccountCount(profileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE profileId = :profileId AND id = :accountId")
    suspend fun deleteAccountById(profileId: String, accountId: String)
}
