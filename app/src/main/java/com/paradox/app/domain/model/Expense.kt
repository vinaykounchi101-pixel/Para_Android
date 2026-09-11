package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.time.Instant
import java.time.LocalDate

data class Expense(
    val id: String,
    val profileId: String,
    val title: String,
    val money: Money,
    val categoryId: String,
    val paymentMethodId: String,
    val date: LocalDate,
    val notes: String? = null,
    val recurringFlag: Boolean = false,
    val source: String = "MANUAL",
    val attachmentRef: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
