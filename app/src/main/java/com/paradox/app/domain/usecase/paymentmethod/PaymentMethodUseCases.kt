package com.paradox.app.domain.usecase.paymentmethod

import com.paradox.app.core.common.Result
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.PaymentMethodType
import com.paradox.app.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetPaymentMethodsUseCase @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) {
    operator fun invoke(profileId: String): Flow<List<PaymentMethod>> = paymentMethodRepository.getPaymentMethods(profileId)
}

class CreatePaymentMethodUseCase @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) {
    suspend operator fun invoke(
        profileId: String,
        label: String,
        type: PaymentMethodType = PaymentMethodType.CUSTOM
    ): Result<PaymentMethod> {
        return try {
            if (label.isBlank()) {
                return Result.Error(IllegalArgumentException("Payment method label cannot be blank"))
            }

            val paymentMethod = PaymentMethod(
                id = "${profileId}_pm_" + UUID.randomUUID().toString().take(8),
                profileId = profileId,
                type = type,
                label = label.trim(),
                isCustom = true
            )

            paymentMethodRepository.addPaymentMethod(paymentMethod)
            Result.Success(paymentMethod)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeletePaymentMethodUseCase @Inject constructor(
    private val paymentMethodRepository: PaymentMethodRepository
) {
    suspend operator fun invoke(
        profileId: String,
        id: String,
        targetReassignId: String? = null
    ): Result<Unit> {
        return try {
            val dependentCount = paymentMethodRepository.getDependentExpenseCount(profileId, id)
            if (dependentCount > 0 && targetReassignId == null) {
                return Result.Error(
                    IllegalStateException("Cannot delete payment method with $dependentCount expenses without specifying a reassignment method")
                )
            }
            paymentMethodRepository.deletePaymentMethod(profileId, id, targetReassignId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
