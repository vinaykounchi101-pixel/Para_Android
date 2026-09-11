package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.intelligence.AffordabilityRating
import com.paradox.app.domain.model.intelligence.PurchaseSimulationResult
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class SimulatePurchaseUseCase @Inject constructor(
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase
) {
    suspend operator fun invoke(profileId: String, purchaseAmount: Money): PurchaseSimulationResult {
        val safeToSpend = calculateSafeToSpendUseCase(profileId)
        val remainingBudget = safeToSpend.remainingBudget
        val daysRemaining = maxOf(1, safeToSpend.daysRemaining)

        val budgetImpactPct = if (remainingBudget.isPositive()) {
            purchaseAmount.amount.divide(remainingBudget.amount, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100))
        } else {
            BigDecimal("100.00")
        }

        val netAfterPurchase = remainingBudget.amount
            .subtract(safeToSpend.upcomingCommitments.amount)
            .subtract(safeToSpend.savingsCommitments.amount)
            .subtract(purchaseAmount.amount)

        val postPurchaseDailySafe = if (netAfterPurchase > BigDecimal.ZERO) {
            Money.of(netAfterPurchase.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_EVEN), purchaseAmount.currencyCode)
        } else {
            Money.zero(purchaseAmount.currencyCode)
        }

        val rating: AffordabilityRating
        val explanation: String
        val tradeOff: String

        when {
            purchaseAmount.amount > remainingBudget.amount -> {
                val excess = purchaseAmount.amount.subtract(remainingBudget.amount)
                rating = AffordabilityRating.DELAY_PURCHASE
                explanation = "This purchase exceeds your remaining monthly budget by ₹$excess."
                tradeOff = "Buying this now will require dipping into emergency savings or overdrawing other categories. Consider scheduling a dedicated savings goal."
            }
            budgetImpactPct >= BigDecimal("60.00") || postPurchaseDailySafe.isZero() -> {
                rating = AffordabilityRating.PROCEED_WITH_CAUTION
                explanation = "Affordable, but will absorb ${budgetImpactPct.toInt()}% of remaining budget with $daysRemaining days left."
                tradeOff = "Your safe daily spending will drop from ₹${safeToSpend.dailySafeAmount.amount} down to ₹${postPurchaseDailySafe.amount} per day."
            }
            else -> {
                rating = AffordabilityRating.SAFE_TO_BUY
                explanation = "Comfortably fits within your monthly budget limits."
                tradeOff = "Daily safe spending remains healthy at ₹${postPurchaseDailySafe.amount}/day (down from ₹${safeToSpend.dailySafeAmount.amount}/day)."
            }
        }

        return PurchaseSimulationResult(
            itemAmount = purchaseAmount,
            rating = rating,
            explanation = explanation,
            postPurchaseSafeToSpend = postPurchaseDailySafe,
            budgetImpactPct = budgetImpactPct,
            tradeOffAnalysis = tradeOff
        )
    }
}
