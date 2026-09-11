package com.paradox.app.domain.model

enum class PaymentMethodType {
    CASH,
    UPI,
    DEBIT_CARD,
    CREDIT_CARD,
    BANK_ACCOUNT,
    WALLET,
    CUSTOM
}

data class PaymentMethod(
    val id: String,
    val profileId: String,
    val type: PaymentMethodType,
    val label: String,
    val isCustom: Boolean = false
) {
    companion object {
        fun createStarterPaymentMethods(profileId: String): List<PaymentMethod> {
            return listOf(
                PaymentMethod(
                    id = "${profileId}_pm_upi",
                    profileId = profileId,
                    type = PaymentMethodType.UPI,
                    label = "UPI",
                    isCustom = false
                ),
                PaymentMethod(
                    id = "${profileId}_pm_cash",
                    profileId = profileId,
                    type = PaymentMethodType.CASH,
                    label = "Cash",
                    isCustom = false
                ),
                PaymentMethod(
                    id = "${profileId}_pm_card",
                    profileId = profileId,
                    type = PaymentMethodType.DEBIT_CARD,
                    label = "Debit Card",
                    isCustom = false
                ),
                PaymentMethod(
                    id = "${profileId}_pm_credit",
                    profileId = profileId,
                    type = PaymentMethodType.CREDIT_CARD,
                    label = "Credit Card",
                    isCustom = false
                ),
                PaymentMethod(
                    id = "${profileId}_pm_bank",
                    profileId = profileId,
                    type = PaymentMethodType.BANK_ACCOUNT,
                    label = "Net Banking",
                    isCustom = false
                )
            )
        }
    }
}
