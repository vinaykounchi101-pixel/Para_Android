package com.paradox.app.data.repository

import com.paradox.app.data.local.dao.PaymentMethodDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentMethodRepositoryImpl @Inject constructor(
    private val paymentMethodDao: PaymentMethodDao
) : PaymentMethodRepository {

    override fun getPaymentMethods(profileId: String): Flow<List<PaymentMethod>> {
        return paymentMethodDao.getPaymentMethodsByProfile(profileId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPaymentMethodById(profileId: String, id: String): PaymentMethod? {
        return paymentMethodDao.getPaymentMethodById(profileId, id)?.toDomain()
    }

    override suspend fun addPaymentMethod(paymentMethod: PaymentMethod) {
        paymentMethodDao.insertPaymentMethod(paymentMethod.toEntity())
    }

    override suspend fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        paymentMethodDao.updatePaymentMethod(paymentMethod.toEntity())
    }

    override suspend fun deletePaymentMethod(profileId: String, id: String, targetReassignId: String?) {
        if (targetReassignId != null) {
            paymentMethodDao.reassignExpensesPaymentMethod(profileId, id, targetReassignId)
        }
        val entity = paymentMethodDao.getPaymentMethodById(profileId, id)
        if (entity != null) {
            paymentMethodDao.deletePaymentMethod(entity)
        }
    }

    override suspend fun getDependentExpenseCount(profileId: String, paymentMethodId: String): Int {
        return paymentMethodDao.getDependentExpenseCount(profileId, paymentMethodId)
    }
}
