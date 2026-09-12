package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.paradox.app.data.local.entity.DebtRepaymentEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

@Dao
interface DebtRepaymentDao {

    @Query("SELECT * FROM debt_repayments WHERE profileId = :profileId AND debtId = :debtId ORDER BY repaymentDate DESC, createdAt DESC")
    fun getRepaymentsForDebt(profileId: String, debtId: String): Flow<List<DebtRepaymentEntity>>

    @Query("SELECT * FROM debt_repayments WHERE profileId = :profileId ORDER BY repaymentDate DESC")
    fun getAllRepayments(profileId: String): Flow<List<DebtRepaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: DebtRepaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayments(repayments: List<DebtRepaymentEntity>)

    @Delete
    suspend fun deleteRepayment(repayment: DebtRepaymentEntity)

    @Query("DELETE FROM debt_repayments WHERE profileId = :profileId AND id = :id")
    suspend fun deleteRepaymentById(profileId: String, id: String)

    @Query("DELETE FROM debt_repayments WHERE profileId = :profileId AND debtId = :debtId")
    suspend fun deleteRepaymentsForDebt(profileId: String, debtId: String)

    @Transaction
    suspend fun recordRepaymentAndUpdateDebt(
        repayment: DebtRepaymentEntity,
        debtDao: DebtDao
    ) {
        insertRepayment(repayment)
        val debt = debtDao.getDebtById(repayment.profileId, repayment.debtId)
        if (debt != null) {
            val newRemaining = (debt.remainingAmount - repayment.amount).max(BigDecimal.ZERO)
            val newStatus = if (newRemaining.compareTo(BigDecimal.ZERO) == 0) "SETTLED" else "ACTIVE"
            debtDao.updateRemainingAndStatus(
                profileId = repayment.profileId,
                id = repayment.debtId,
                remainingAmount = newRemaining,
                status = newStatus,
                updatedAt = Instant.now()
            )
        }
    }
}
