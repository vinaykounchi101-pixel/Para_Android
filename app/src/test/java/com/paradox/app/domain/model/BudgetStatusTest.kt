package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetStatusTest {

    @Test
    fun `budget status is ON_TRACK when spending below 80 percent`() {
        val budget = Budget(
            id = "b1",
            profileId = "p1",
            type = BudgetType.MONTHLY,
            limit = Money.of("10000.00", "INR"),
            thresholdPct = 80
        )
        val spent = Money.of("5000.00", "INR") // 50%

        val status = BudgetStatus.calculate(budget, spent)

        assertEquals(BudgetHealth.ON_TRACK, status.health)
        assertEquals(50.0, status.percentageUsed, 0.01)
        assertEquals(Money.of("5000.00", "INR"), status.remaining)
    }

    @Test
    fun `budget status is NEAR_LIMIT when spending reaches 80 percent threshold`() {
        val budget = Budget(
            id = "b1",
            profileId = "p1",
            type = BudgetType.MONTHLY,
            limit = Money.of("10000.00", "INR"),
            thresholdPct = 80
        )
        val spent = Money.of("8500.00", "INR") // 85%

        val status = BudgetStatus.calculate(budget, spent)

        assertEquals(BudgetHealth.NEAR_LIMIT, status.health)
        assertEquals(85.0, status.percentageUsed, 0.01)
        assertEquals(Money.of("1500.00", "INR"), status.remaining)
    }

    @Test
    fun `budget status is OVER_BUDGET when spending exceeds 100 percent`() {
        val budget = Budget(
            id = "b1",
            profileId = "p1",
            type = BudgetType.MONTHLY,
            limit = Money.of("10000.00", "INR"),
            thresholdPct = 80
        )
        val spent = Money.of("11500.00", "INR") // 115%

        val status = BudgetStatus.calculate(budget, spent)

        assertEquals(BudgetHealth.OVER_BUDGET, status.health)
        assertEquals(115.0, status.percentageUsed, 0.01)
        assertEquals(Money.of("-1500.00", "INR"), status.remaining)
    }
}
