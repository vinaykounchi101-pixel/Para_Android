package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.time.Instant
import java.time.LocalDate

enum class DebtType {
    LENT,      // "You'll Get" (Receivable)
    BORROWED   // "You Owe" (Payable)
}

enum class DebtStatus {
    ACTIVE,
    SETTLED
}

data class Debt(
    val id: String,
    val profileId: String,
    val personName: String,
    val personContactNumber: String? = null,
    val debtType: DebtType,
    val initialAmount: Money,
    val remainingAmount: Money,
    val dueDate: LocalDate? = null,
    val notes: String? = null,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val reminderEnabled: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class DebtRepayment(
    val id: String,
    val debtId: String,
    val profileId: String,
    val amount: Money,
    val repaymentDate: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

data class DebtSummary(
    val totalReceivable: Money, // Total Lent Active
    val totalPayable: Money,    // Total Borrowed Active
    val netBalance: Money       // Receivable - Payable
)
