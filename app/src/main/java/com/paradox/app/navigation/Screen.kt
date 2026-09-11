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

    // Phase 2 Routes
    data object IncomeList : Screen("income_list")
    data object AddIncome : Screen("add_income")
    data object EditIncome : Screen("edit_income/{incomeId}") {
        fun createRoute(incomeId: String) = "edit_income/$incomeId"
    }

    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account")
    data object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: String) = "edit_account/$accountId"
    }

    data object Recurring : Screen("recurring")
    data object AddRecurring : Screen("add_recurring")
    data object EditRecurring : Screen("edit_recurring/{recurringId}") {
        fun createRoute(recurringId: String) = "edit_recurring/$recurringId"
    }

    data object SavingsGoals : Screen("savings_goals")
    data object AddSavingsGoal : Screen("add_savings_goal")
    data object EditSavingsGoal : Screen("edit_savings_goal/{goalId}") {
        fun createRoute(goalId: String) = "edit_savings_goal/$goalId"
    }

    data object Export : Screen("export")
}
