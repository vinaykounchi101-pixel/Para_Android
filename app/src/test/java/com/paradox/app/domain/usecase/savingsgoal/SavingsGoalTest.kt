package com.paradox.app.domain.usecase.savingsgoal

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SavingsGoalTest {

    @Test
    fun `progressPercentage calculates accurate ratio and clamps to 100`() {
        val goal = SavingsGoal(
            id = "goal_1",
            profileId = "prof_1",
            name = "Laptop",
            targetAmount = Money(BigDecimal("100000.00"), "INR"),
            currentAmount = Money(BigDecimal("25000.00"), "INR"),
            targetDate = LocalDate.now().plusMonths(3)
        )

        assertEquals(25.0, goal.progressPercentage(), 0.01)
        assertEquals(BigDecimal("75000.00"), goal.remainingAmount().amount)
        assertFalse(goal.isAchieved())

        val completedGoal = goal.copy(currentAmount = Money(BigDecimal("120000.00"), "INR"))
        assertEquals(100.0, completedGoal.progressPercentage(), 0.01)
        assertEquals(BigDecimal.ZERO, completedGoal.remainingAmount().amount)
        assertTrue(completedGoal.isAchieved())
    }
}
