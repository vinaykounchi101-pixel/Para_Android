package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.time.Instant

enum class AccountType(val displayName: String, val defaultIcon: String) {
    CASH("Cash", "payments"),
    BANK("Bank Account", "account_balance"),
    DEBIT_CARD("Debit Card", "credit_card"),
    CREDIT_CARD("Credit Card", "credit_card"),
    DIGITAL_WALLET("Digital Wallet", "account_balance_wallet"),
    SAVINGS("Savings Account", "savings"),
    CUSTOM("Custom", "wallet")
}

data class Account(
    val id: String,
    val profileId: String,
    val name: String,
    val type: AccountType,
    val currency: String = "INR",
    val initialBalance: Money = Money.zero(currency),
    val colorHex: String = "#3B82F6",
    val iconName: String = type.defaultIcon,
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class AccountWithBalance(
    val account: Account,
    val currentBalance: Money,
    val totalIncome: Money = Money.zero(account.currency),
    val totalExpense: Money = Money.zero(account.currency)
)
