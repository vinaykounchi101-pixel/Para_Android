package com.paradox.app.domain.repository

import com.paradox.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun getPaymentMethods(profileId: String): Flow<List<PaymentMethod>>
    suspend fun getPaymentMethodById(profileId: String, id: String): PaymentMethod?
    suspend fun addPaymentMethod(paymentMethod: PaymentMethod)
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod)
    suspend fun deletePaymentMethod(profileId: String, id: String, targetReassignId: String? = null)
    suspend fun getDependentExpenseCount(profileId: String, paymentMethodId: String): Int
}
