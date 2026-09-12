package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllDebts(profileId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND debtType = :debtType ORDER BY createdAt DESC")
    fun getDebtsByType(profileId: String, debtType: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveDebts(profileId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND status = 'SETTLED' ORDER BY updatedAt DESC")
    fun getSettledDebts(profileId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getDebtById(profileId: String, id: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE profileId = :profileId AND id = :id LIMIT 1")
    fun observeDebtById(profileId: String, id: String): Flow<DebtEntity?>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM debts WHERE profileId = :profileId AND debtType = 'LENT' AND status = 'ACTIVE'")
    fun observeTotalReceivable(profileId: String): Flow<BigDecimal?>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM debts WHERE profileId = :profileId AND debtType = 'BORROWED' AND status = 'ACTIVE'")
    fun observeTotalPayable(profileId: String): Flow<BigDecimal?>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM debts WHERE profileId = :profileId AND debtType = 'LENT' AND status = 'ACTIVE'")
    suspend fun getTotalReceivableSync(profileId: String): BigDecimal?

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM debts WHERE profileId = :profileId AND debtType = 'BORROWED' AND status = 'ACTIVE'")
    suspend fun getTotalPayableSync(profileId: String): BigDecimal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("UPDATE debts SET remainingAmount = :remainingAmount, status = :status, updatedAt = :updatedAt WHERE profileId = :profileId AND id = :id")
    suspend fun updateRemainingAndStatus(profileId: String, id: String, remainingAmount: BigDecimal, status: String, updatedAt: Instant = Instant.now())

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE profileId = :profileId AND id = :id")
    suspend fun deleteDebtById(profileId: String, id: String)
}
