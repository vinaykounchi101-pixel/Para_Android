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
import com.paradox.app.feature.askparadox.AskParadoxScreen
import com.paradox.app.feature.backup.BackupScreen
import com.paradox.app.feature.budget.BudgetListScreen
import com.paradox.app.feature.category.CategoryManagementScreen
import com.paradox.app.feature.dashboard.DashboardScreen
import com.paradox.app.feature.debt.DebtScreen
import com.paradox.app.feature.engagement.EngagementScreen
import com.paradox.app.feature.engagement.SplitExpenseScreen
import com.paradox.app.feature.expense.AddEditExpenseScreen
import com.paradox.app.feature.expense.ExpenseDetailScreen
import com.paradox.app.feature.expense.LedgerScreen
import com.paradox.app.feature.export.ExportScreen
import com.paradox.app.feature.income.AddEditIncomeScreen
import com.paradox.app.feature.income.IncomeListScreen
import com.paradox.app.feature.insights.InsightsHubScreen
import com.paradox.app.feature.onboarding.OnboardingScreen
import com.paradox.app.feature.profile.UnlockScreen
import com.paradox.app.feature.recurring.AddEditRecurringScreen
import com.paradox.app.feature.recurring.RecurringExpensesScreen
import com.paradox.app.feature.savingsgoal.AddEditSavingsGoalScreen
import com.paradox.app.feature.savingsgoal.SavingsGoalsScreen
import com.paradox.app.feature.settings.SettingsScreen
import com.paradox.app.feature.sync.SyncSettingsScreen

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
                },
                onNavigateToCapture = { mode ->
                    navController.navigate(Screen.Capture.createRoute(mode))
                },
                onNavigateToAskParadox = {
                    navController.navigate(Screen.AskParadox.route)
                },
                onNavigateToInsights = {
                    navController.navigate(Screen.InsightsHub.route)
                },
                onNavigateToEngagement = {
                    navController.navigate(Screen.Engagement.route)
                },
                onNavigateToDebts = {
                    navController.navigate(Screen.Debts.route)
                }
            )
        }

        composable(Screen.Ledger.route) {
            LedgerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onNavigateToExpenseDetail = { expenseId ->
                    navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                },
                onNavigateToInsights = {
                    navController.navigate(Screen.InsightsHub.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Budgets.route) {
            BudgetListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLedger = {
                    navController.navigate(Screen.Ledger.route)
                },
                onNavigateToInsights = {
                    navController.navigate(Screen.InsightsHub.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
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
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToSync = { navController.navigate(Screen.SyncSettings.route) },
                onNavigateToEngagement = { navController.navigate(Screen.Engagement.route) },
                onNavigateToInsights = { navController.navigate(Screen.InsightsHub.route) },
                onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLedger = {
                    navController.navigate(Screen.Ledger.route)
                },
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                },
                onNavigateToUnlock = {
                    navController.navigate(Screen.Unlock.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route)
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

        // Phase 3 Assisted Capture
        composable(
            route = Screen.Capture.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "QUICK_ADD"
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: "QUICK_ADD"
            val mode = try {
                com.paradox.app.feature.capture.CaptureMode.valueOf(modeStr)
            } catch (_: Exception) {
                com.paradox.app.feature.capture.CaptureMode.QUICK_ADD
            }
            com.paradox.app.feature.capture.CaptureHubScreen(
                initialMode = mode,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Phase 4 Routes
        composable(Screen.AskParadox.route) {
            AskParadoxScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InsightsHub.route) {
            InsightsHubScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAskParadox = { navController.navigate(Screen.AskParadox.route) },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLedger = {
                    navController.navigate(Screen.Ledger.route)
                },
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Phase 5 Routes
        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SyncSettings.route) {
            SyncSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Phase 6 Routes
        composable(Screen.Engagement.route) {
            EngagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSplit = { navController.navigate(Screen.SplitExpense.route) }
            )
        }

        composable(Screen.SplitExpense.route) {
            SplitExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Debts & Udhaar Ledger
        composable(Screen.Debts.route) {
            DebtScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
