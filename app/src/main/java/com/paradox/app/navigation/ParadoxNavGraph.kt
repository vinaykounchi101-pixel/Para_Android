package com.paradox.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.paradox.app.feature.account.AccountsScreen
import com.paradox.app.feature.account.AddEditAccountScreen
import com.paradox.app.feature.budget.BudgetListScreen
import com.paradox.app.feature.category.CategoryManagementScreen
import com.paradox.app.feature.dashboard.DashboardScreen
import com.paradox.app.feature.expense.AddEditExpenseScreen
import com.paradox.app.feature.expense.ExpenseDetailScreen
import com.paradox.app.feature.expense.LedgerScreen
import com.paradox.app.feature.export.ExportScreen
import com.paradox.app.feature.income.AddEditIncomeScreen
import com.paradox.app.feature.income.IncomeListScreen
import com.paradox.app.feature.onboarding.OnboardingScreen
import com.paradox.app.feature.profile.UnlockScreen
import com.paradox.app.feature.recurring.AddEditRecurringScreen
import com.paradox.app.feature.recurring.RecurringExpensesScreen
import com.paradox.app.feature.savingsgoal.AddEditSavingsGoalScreen
import com.paradox.app.feature.savingsgoal.SavingsGoalsScreen
import com.paradox.app.feature.settings.SettingsScreen

@Composable
fun ParadoxNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Unlock.route) {
            UnlockScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Unlock.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Unlock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddExpense.route)
                },
                onNavigateToLedger = {
                    navController.navigate(Screen.Ledger.route)
                },
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToExpenseDetail = { expenseId ->
                    navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                },
                onNavigateToIncome = {
                    navController.navigate(Screen.IncomeList.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToRecurring = {
                    navController.navigate(Screen.Recurring.route)
                },
                onNavigateToSavingsGoals = {
                    navController.navigate(Screen.SavingsGoals.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                }
            )
        }

        composable(Screen.Ledger.route) {
            LedgerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onNavigateToExpenseDetail = { expenseId ->
                    navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                }
            )
        }

        composable(Screen.Budgets.route) {
            BudgetListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Categories.route) {
            CategoryManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                onNavigateToSavingsGoals = { navController.navigate(Screen.SavingsGoals.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) },
                onNavigateToUnlock = {
                    navController.navigate(Screen.Unlock.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AddExpense.route) {
            AddEditExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            AddEditExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            ExpenseDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { expenseId ->
                    navController.navigate(Screen.EditExpense.createRoute(expenseId))
                }
            )
        }

        // Phase 2 composables
        composable(Screen.IncomeList.route) {
            IncomeListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddIncome = { navController.navigate(Screen.AddIncome.route) },
                onNavigateToEditIncome = { incomeId ->
                    navController.navigate(Screen.EditIncome.createRoute(incomeId))
                }
            )
        }

        composable(Screen.AddIncome.route) {
            AddEditIncomeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditIncome.route,
            arguments = listOf(navArgument("incomeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val incomeId = backStackEntry.arguments?.getString("incomeId")
            AddEditIncomeScreen(
                incomeId = incomeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddAccount = { navController.navigate(Screen.AddAccount.route) },
                onNavigateToEditAccount = { accountId ->
                    navController.navigate(Screen.EditAccount.createRoute(accountId))
                }
            )
        }

        composable(Screen.AddAccount.route) {
            AddEditAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")
            AddEditAccountScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recurring.route) {
            RecurringExpensesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddRecurring = { navController.navigate(Screen.AddRecurring.route) },
                onNavigateToEditRecurring = { recurringId ->
                    navController.navigate(Screen.EditRecurring.createRoute(recurringId))
                }
            )
        }

        composable(Screen.AddRecurring.route) {
            AddEditRecurringScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditRecurring.route,
            arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recurringId = backStackEntry.arguments?.getString("recurringId")
            AddEditRecurringScreen(
                recurringId = recurringId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddGoal = { navController.navigate(Screen.AddSavingsGoal.route) },
                onNavigateToEditGoal = { goalId ->
                    navController.navigate(Screen.EditSavingsGoal.createRoute(goalId))
                }
            )
        }

        composable(Screen.AddSavingsGoal.route) {
            AddEditSavingsGoalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditSavingsGoal.route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId")
            AddEditSavingsGoalScreen(
                goalId = goalId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
