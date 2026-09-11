package com.paradox.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.core.ui.components.ParadoxCard
import com.paradox.app.core.ui.components.ParadoxProgressBar
import com.paradox.app.core.ui.components.ParadoxStatusChip
import com.paradox.app.core.ui.components.StateContainer
import com.paradox.app.core.ui.theme.ParadoxTheme
import com.paradox.app.domain.model.BudgetHealth
import com.paradox.app.domain.model.DashboardSummary
import com.paradox.app.domain.model.Expense
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToIncome: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToSavingsGoals: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Native App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Paradox",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "VAULT ENCRYPTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToLedger) {
                        Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = "Ledger", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToBudgets) {
                        Icon(imageVector = Icons.Outlined.AccountBalanceWallet, contentDescription = "Budgets", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            StateContainer(
                isLoading = uiState.isLoading,
                data = uiState.summary,
                errorMessage = uiState.errorMessage
            ) { summary ->
                DashboardContent(
                    summary = summary,
                    onNavigateToLedger = onNavigateToLedger,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToExpenseDetail = onNavigateToExpenseDetail,
                    onNavigateToIncome = onNavigateToIncome,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToRecurring = onNavigateToRecurring,
                    onNavigateToSavingsGoals = onNavigateToSavingsGoals,
                    onNavigateToExport = onNavigateToExport
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    onNavigateToLedger: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Quick Access Services Grid / Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.paradox.app.core.ui.components.ParadoxFilterChip(
                selected = false,
                onClick = onNavigateToIncome,
                label = "Income & Cash Flow"
            )
            com.paradox.app.core.ui.components.ParadoxFilterChip(
                selected = false,
                onClick = onNavigateToAccounts,
                label = "Wallets / Accounts"
            )
            com.paradox.app.core.ui.components.ParadoxFilterChip(
                selected = false,
                onClick = onNavigateToRecurring,
                label = "Subscriptions"
            )
            com.paradox.app.core.ui.components.ParadoxFilterChip(
                selected = false,
                onClick = onNavigateToSavingsGoals,
                label = "Savings Goals"
            )
            com.paradox.app.core.ui.components.ParadoxFilterChip(
                selected = false,
                onClick = onNavigateToExport,
                label = "Export"
            )
        }

        // Hero Spending Section
        ParadoxCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "MONTH TOTAL SPEND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.format(summary.totalSpentThisMonth),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val health = summary.overallBudgetStatus?.health ?: BudgetHealth.ON_TRACK
                ParadoxStatusChip(health = health)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Linear Gauge
            val progress = summary.overallBudgetStatus?.let { (it.percentageUsed / 100.0).toFloat() } ?: 0f
            ParadoxProgressBar(
                progress = progress,
                health = summary.overallBudgetStatus?.health ?: BudgetHealth.ON_TRACK,
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val remainingText = summary.overallBudgetStatus?.let {
                    if (it.remaining.isPositive()) "${CurrencyFormatter.format(it.remaining)} available"
                    else "Exceeded by ${CurrencyFormatter.format(it.remaining.copy(amount = it.remaining.amount.abs()))}"
                } ?: "No budget set"

                val limitText = summary.overallBudgetStatus?.let {
                    "Budget ${CurrencyFormatter.format(it.budget.limit)}"
                } ?: "Tap to set budget"

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = limitText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onNavigateToBudgets() }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Safe to Spend Tile
        if (summary.safeToSpendToday != null) {
            ParadoxCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Safe-to-Spend Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = CurrencyFormatter.format(summary.safeToSpendToday),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "RECOMMENDED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "Paced for month end",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Spending Velocity Minimal Bars
        if (summary.dailyVelocity.isNotEmpty()) {
            ParadoxCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "7-DAY SPENDING VELOCITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val maxSpend = summary.dailyVelocity.maxOfOrNull { it.totalSpent.amount } ?: java.math.BigDecimal.ONE
                val normalizedMax = if (maxSpend.compareTo(java.math.BigDecimal.ZERO) > 0) maxSpend else java.math.BigDecimal.ONE

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    summary.dailyVelocity.forEach { day ->
                        val ratio = (day.totalSpent.amount.divide(normalizedMax, 2, java.math.RoundingMode.HALF_EVEN)).toFloat().coerceIn(0.08f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (day.totalSpent.isPositive()) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = day.dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Category Breakdown
        if (summary.categoryBreakdown.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            summary.categoryBreakdown.take(4).forEach { catSummary ->
                ParadoxCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(catSummary.category.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = catSummary.category.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyFormatter.format(catSummary.totalSpent),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${catSummary.percentageOfTotal.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Recent Activity Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View All",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onNavigateToLedger() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (summary.recentExpenses.isEmpty()) {
            ParadoxCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No transactions recorded yet. Tap + below to add your first expense.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            summary.recentExpenses.forEach { expense ->
                RecentExpenseItem(
                    expense = expense,
                    onClick = { onNavigateToExpenseDetail(expense.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun RecentExpenseItem(
    expense: Expense,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    ParadoxCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.date.format(dateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expense.notes != null) {
                        Text(
                            text = " • ${expense.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = CurrencyFormatter.format(expense.money),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
