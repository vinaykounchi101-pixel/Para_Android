package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId ORDER BY label ASC")
    fun getPaymentMethodsByProfile(profileId: String): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getPaymentMethodById(profileId: String, id: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(paymentMethods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Query("SELECT COUNT(*) FROM expenses WHERE profileId = :profileId AND paymentMethodId = :paymentMethodId")
    suspend fun getDependentExpenseCount(profileId: String, paymentMethodId: String): Int

    @Query("UPDATE expenses SET paymentMethodId = :targetPaymentMethodId WHERE profileId = :profileId AND paymentMethodId = :sourcePaymentMethodId")
    suspend fun reassignExpensesPaymentMethod(profileId: String, sourcePaymentMethodId: String, targetPaymentMethodId: String)
}
