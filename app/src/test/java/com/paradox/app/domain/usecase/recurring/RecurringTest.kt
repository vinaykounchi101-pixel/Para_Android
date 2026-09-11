package com.paradox.app.domain.usecase.recurring

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringTest {

    @Test
    fun `monthlyNormalizedAmount calculates accurately across frequencies`() {
        val monthly = RecurringExpense(
            id = "rec_1",
            profileId = "prof_1",
            title = "Netflix",
            amount = Money(BigDecimal("649.00"), "INR"),
            categoryId = "cat_1",
            paymentMethodId = "pm_1",
            frequency = RecurringFrequency.MONTHLY,
            startDate = LocalDate.now(),
            nextDueDate = LocalDate.now().plusMonths(1)
        )
        assertEquals(BigDecimal("649.00"), monthly.monthlyNormalizedAmount().amount)

        val yearly = monthly.copy(
            frequency = RecurringFrequency.YEARLY,
            amount = Money(BigDecimal("12000.00"), "INR")
        )
        // 12000 * 0.0833 = 999.60
        assertEquals(BigDecimal("999.60"), yearly.monthlyNormalizedAmount().amount)
    }
}
