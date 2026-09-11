package com.paradox.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Unlock : Screen("unlock")
    data object Dashboard : Screen("dashboard")
    data object Ledger : Screen("ledger")
    data object Budgets : Screen("budgets")
    data object Categories : Screen("categories")
    data object Settings : Screen("settings")
    data object AddExpense : Screen("add_expense")
    data object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }
    data object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
}
