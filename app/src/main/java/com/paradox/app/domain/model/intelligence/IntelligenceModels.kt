package com.paradox.app.domain.model.intelligence

import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class SafeToSpendStatus {
    HEALTHY,
    MODERATE,
    CAUTION,
    DANGER
}

data class SafeToSpendResult(
    val dailySafeAmount: Money,
    val remainingBudget: Money,
    val totalMonthlyBudget: Money,
    val daysRemaining: Int,
    val upcomingCommitments: Money,
    val savingsCommitments: Money,
    val burnRatePerDay: Money,
    val status: SafeToSpendStatus,
    val statusReason: String
)

data class FinancialHealthPillar(
    val name: String,
    val score: Int,
    val maxScore: Int = 20,
    val summary: String,
    val isHealthy: Boolean
)

data class ActionableTip(
    val id: String,
    val priority: Int,
    val title: String,
    val description: String,
    val potentialSavings: Money? = null
)

data class FinancialHealthScore(
    val overallScore: Int,
    val tier: String,
    val savingsDiscipline: FinancialHealthPillar,
    val budgetAdherence: FinancialHealthPillar,
    val spendingStability: FinancialHealthPillar,
    val cashCushion: FinancialHealthPillar,
    val leakControl: FinancialHealthPillar,
    val actionableTips: List<ActionableTip>
)

enum class LeakType {
    SUBSCRIPTION,
    MICRO_SPENDING,
    HIGH_FREQUENCY_MERCHANT,
    SURGE_CATEGORY
}

data class SpendingLeak(
    val id: String,
    val title: String,
    val type: LeakType,
    val totalImpact: Money,
    val occurrences: Int,
    val description: String,
    val recommendation: String
)

enum class AffordabilityRating {
    SAFE_TO_BUY,
    PROCEED_WITH_CAUTION,
    DELAY_PURCHASE
}

data class PurchaseSimulationResult(
    val itemAmount: Money,
    val rating: AffordabilityRating,
    val explanation: String,
    val postPurchaseSafeToSpend: Money,
    val budgetImpactPct: BigDecimal,
    val tradeOffAnalysis: String
)

data class FiftyThirtyTwentyRule(
    val needsAmount: Money,
    val needsPct: BigDecimal,
    val wantsAmount: Money,
    val wantsPct: BigDecimal,
    val savingsAmount: Money,
    val savingsPct: BigDecimal
)

data class SpendingForecast(
    val currentMonthSpent: Money,
    val projectedMonthEnd: Money,
    val dailyBurnRate: Money,
    val recommendedPace: Money,
    val fiftyThirtyTwenty: FiftyThirtyTwentyRule
)

data class GroundedSourceRef(
    val type: String,
    val title: String,
    val amount: Money? = null,
    val date: LocalDate? = null
)

data class GroundedChatMessage(
    val id: String,
    val isUser: Boolean,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val groundedSources: List<GroundedSourceRef> = emptyList()
)
