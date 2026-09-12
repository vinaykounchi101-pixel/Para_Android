package com.paradox.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.core.ui.components.BottomNavDestination
import com.paradox.app.core.ui.components.ParadoxBottomNavBar
import com.paradox.app.core.ui.components.StateContainer
import com.paradox.app.domain.model.BudgetHealth
import com.paradox.app.domain.model.DashboardSummary
import com.paradox.app.domain.model.Expense
import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
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
    onNavigateToCapture: (String) -> Unit = {},
    onNavigateToAskParadox: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToEngagement: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            ParadoxBottomNavBar(
                currentDestination = BottomNavDestination.DASHBOARD,
                onNavigateToDestination = { destination ->
                    when (destination) {
                        BottomNavDestination.DASHBOARD -> { /* Current screen */ }
                        BottomNavDestination.LEDGER -> onNavigateToLedger()
                        BottomNavDestination.BUDGETS -> onNavigateToBudgets()
                        BottomNavDestination.INSIGHTS -> onNavigateToInsights()
                        BottomNavDestination.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Native App Header per Stitch design
            StitchHeader(
                onSearchClick = onNavigateToLedger,
                onNotificationsClick = onNavigateToInsights,
                onProfileClick = onNavigateToSettings
            )

            StateContainer(
                isLoading = uiState.isLoading,
                data = uiState.summary,
                errorMessage = uiState.errorMessage
            ) { summary ->
                DashboardContent(
                    summary = summary,
                    selectedYearMonth = uiState.selectedYearMonth,
                    availableMonths = uiState.availableMonths,
                    onSelectMonth = viewModel::selectMonth,
                    onNavigateToLedger = onNavigateToLedger,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToExpenseDetail = onNavigateToExpenseDetail,
                    onNavigateToIncome = onNavigateToIncome,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToRecurring = onNavigateToRecurring,
                    onNavigateToSavingsGoals = onNavigateToSavingsGoals,
                    onNavigateToExport = onNavigateToExport,
                    onNavigateToCapture = onNavigateToCapture,
                    onNavigateToAskParadox = onNavigateToAskParadox,
                    onNavigateToInsights = onNavigateToInsights,
                    onNavigateToEngagement = onNavigateToEngagement,
                    onNavigateToDebts = onNavigateToDebts
                )
            }
        }
    }
}

@Composable
private fun StitchHeader(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Paradox Vault",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Paradox",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "VAULT ENCRYPTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    selectedYearMonth: YearMonth,
    availableMonths: List<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToCapture: (String) -> Unit,
    onNavigateToAskParadox: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToEngagement: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Subheader: Local Device Storage & Period Picker Dropdown Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Local Device Storage",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DashboardMonthDropdown(
                selectedYearMonth = selectedYearMonth,
                availableMonths = availableMonths,
                onSelectMonth = onSelectMonth
            )
        }

        // Hero Card: Month Total Spend with Editorial Typography
        StitchHeroCard(
            summary = summary,
            selectedYearMonth = selectedYearMonth,
            onCardClick = onNavigateToBudgets
        )

        // Safe-to-Spend Tile (FR-P4-002)
        StitchSafeToSpendTile(
            summary = summary,
            selectedYearMonth = selectedYearMonth,
            onClick = onNavigateToInsights
        )

        // AI & Assisted Capture Quick Access Bar
        QuickActionPillRow(
            onNavigateToAskParadox = onNavigateToAskParadox,
            onNavigateToInsights = onNavigateToInsights,
            onNavigateToCapture = onNavigateToCapture,
            onNavigateToEngagement = onNavigateToEngagement,
            onNavigateToDebts = onNavigateToDebts,
            onNavigateToIncome = onNavigateToIncome,
            onNavigateToAccounts = onNavigateToAccounts,
            onNavigateToRecurring = onNavigateToRecurring,
            onNavigateToSavingsGoals = onNavigateToSavingsGoals,
            onNavigateToExport = onNavigateToExport
        )

        // Spending Velocity Bar Chart
        StitchSpendingVelocityCard(
            summary = summary,
            onClick = onNavigateToInsights
        )

        // Top Categories Section (Pastel Tactile Cards)
        StitchTopCategoriesSection(
            summary = summary,
            onViewAllClick = onNavigateToLedger
        )

        // Recent Transactions Section
        StitchRecentTransactionsSection(
            recentExpenses = summary.recentExpenses,
            onSeeAllClick = onNavigateToLedger,
            onExpenseClick = onNavigateToExpenseDetail
        )

        // Privacy Statement
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "On-device processing • No cloud telemetry",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun StitchHeroCard(
    summary: DashboardSummary,
    selectedYearMonth: YearMonth,
    onCardClick: () -> Unit
) {
    val overallBudget = summary.overallBudgetStatus

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    val headerLabel = if (selectedYearMonth == YearMonth.now()) {
                        "MONTH TOTAL SPEND"
                    } else {
                        "${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()} SPEND"
                    }
                    Text(
                        text = headerLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val formatted = CurrencyFormatter.format(summary.totalSpentThisMonth)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status Badge with glowing dot
                val health = overallBudget?.health ?: BudgetHealth.ON_TRACK
                val (badgeBg, dotColor, textColor, text) = when (health) {
                    BudgetHealth.ON_TRACK -> Quad(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        Color(0xFF628E75),
                        Color(0xFF2D6847),
                        "On Track"
                    )
                    BudgetHealth.NEAR_LIMIT -> Quad(
                        Color(0xFFFBF0E0),
                        Color(0xFFB8864E),
                        Color(0xFF8A5A23),
                        "Near Limit"
                    )
                    BudgetHealth.OVER_BUDGET -> Quad(
                        Color(0xFFFBE6E5),
                        Color(0xFFBA6D68),
                        Color(0xFF8A2E2A),
                        "Over Budget"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Progress Bar Section
            val budgetAmount = overallBudget?.budget?.limit ?: Money.ZERO_INR
            val spentAmount = summary.totalSpentThisMonth
            val pctUsed = if (budgetAmount.isPositive()) {
                val ratio = spentAmount.amount.divide(budgetAmount.amount, 4, RoundingMode.HALF_EVEN)
                (ratio.multiply(BigDecimal(100)).toFloat()).coerceIn(0f, 100f)
            } else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = pctUsed / 100f,
                animationSpec = tween(1000),
                label = "progress"
            )

            val trackColor = when {
                pctUsed >= 100f -> Color(0xFFBA6D68)
                pctUsed >= 80f -> Color(0xFFB8864E)
                else -> MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(trackColor)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val availableText = if (overallBudget != null) {
                    "${CurrencyFormatter.format(overallBudget.remaining)} available"
                } else "No limit set"

                Text(
                    text = availableText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val budgetText = if (overallBudget != null) {
                    "Budget ${CurrencyFormatter.format(overallBudget.budget.limit)}"
                } else ""

                Text(
                    text = budgetText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer cycle metadata
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val cycleElapsedText = if (selectedYearMonth == YearMonth.now()) {
                    val dayOfMonth = LocalDate.now().dayOfMonth
                    val totalDays = LocalDate.now().lengthOfMonth()
                    val cycleElapsedPct = ((dayOfMonth.toFloat() / totalDays.toFloat()) * 100).toInt()
                    "$cycleElapsedPct% of cycle elapsed"
                } else if (selectedYearMonth.isBefore(YearMonth.now())) {
                    "Month cycle completed"
                } else {
                    "Cycle not started"
                }

                Text(
                    text = cycleElapsedText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${pctUsed.toInt()}% budget used",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun StitchSafeToSpendTile(
    summary: DashboardSummary,
    selectedYearMonth: YearMonth,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Safe-to-Spend Today",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val safeToSpend = summary.safeToSpendToday
                    Text(
                        text = if (safeToSpend != null) CurrencyFormatter.format(safeToSpend) else "₹0.00",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ day",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "RECOMMENDED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                val daysLeftText = if (selectedYearMonth == YearMonth.now()) {
                    val daysLeft = LocalDate.now().lengthOfMonth() - LocalDate.now().dayOfMonth
                    "$daysLeft days left"
                } else if (selectedYearMonth.isBefore(YearMonth.now())) {
                    "Cycle ended"
                } else {
                    "${selectedYearMonth.lengthOfMonth()} days in month"
                }
                Text(
                    text = daysLeftText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StitchSpendingVelocityCard(
    summary: DashboardSummary,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val velocityItems = summary.dailyVelocity.takeLast(7)
    val maxSpend = velocityItems.maxOfOrNull { it.totalSpent.amount.toDouble() }?.takeIf { it > 0 } ?: 0.0

    // Compute stats
    val nonZeroSpends = velocityItems.filter { it.totalSpent.amount > BigDecimal.ZERO }
    val avgSpendAmount = if (velocityItems.isNotEmpty()) {
        velocityItems.map { it.totalSpent.amount }.reduce { acc, b -> acc.add(b) }
            .divide(BigDecimal(velocityItems.size), 0, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    val peakDayItem = if (nonZeroSpends.isNotEmpty()) {
        velocityItems.maxByOrNull { it.totalSpent.amount }
    } else null

    val subtitleText = if (avgSpendAmount > BigDecimal.ZERO && peakDayItem != null) {
        "Daily avg ${CurrencyFormatter.format(Money(avgSpendAmount, "INR"))} • Peak ${peakDayItem.dayLabel.take(3)} ${CurrencyFormatter.format(peakDayItem.totalSpent)}"
    } else {
        "Daily avg ₹0 • No spend in last 7 days"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spending Velocity",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Last 7 Days",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Minimal Bar Chart with Multi-color Muted Pastels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (velocityItems.isEmpty()) {
                    val defaultLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    defaultLabels.forEach { label ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.62f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    velocityItems.forEachIndexed { index, day ->
                        val isToday = index == velocityItems.size - 1
                        val isPeak = peakDayItem != null && day.date == peakDayItem.date && day.totalSpent.amount > BigDecimal.ZERO
                        val spendAmount = day.totalSpent.amount.toDouble()

                        // Calculate proportional height
                        val fraction = if (maxSpend > 0 && spendAmount > 0) {
                            (spendAmount / maxSpend).toFloat().coerceIn(0.12f, 1f)
                        } else {
                            0.06f
                        }

                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(durationMillis = 600),
                            label = "velocityBar_${day.date}"
                        )

                        // Stitch Palette
                        val barColor = when {
                            isPeak -> MaterialTheme.colorScheme.primary
                            isToday && spendAmount > 0 -> if (isDark) Color(0xFF86B29B) else Color(0xFF628E75)
                            spendAmount == 0.0 -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                            index % 3 == 0 -> if (isDark) Color(0xFF89A8C7).copy(alpha = 0.35f) else Color(0xFFDCE8F2)
                            index % 3 == 1 -> if (isDark) Color(0xFFD2B8DF).copy(alpha = 0.35f) else Color(0xFFEFE6F5)
                            else -> if (isDark) Color(0xFF86B29B).copy(alpha = 0.35f) else Color(0xFFE1EFE7)
                        }

                        val labelColor = when {
                            isPeak -> MaterialTheme.colorScheme.primary
                            isToday -> if (isDark) Color(0xFF86B29B) else Color(0xFF628E75)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Bar container with bottom alignment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.62f)
                                        .fillMaxHeight(animatedFraction)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = day.dayLabel.take(1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isPeak || isToday) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = labelColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPillRow(
    onNavigateToAskParadox: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToCapture: (String) -> Unit,
    onNavigateToEngagement: () -> Unit,
    onNavigateToDebts: () -> Unit = {},
    onNavigateToIncome: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickPill(label = "✨ Ask Paradox", onClick = onNavigateToAskParadox)
        QuickPill(label = "🧠 Intelligence", onClick = onNavigateToInsights)
        QuickPill(label = "🤝 Debts & Udhaar", onClick = onNavigateToDebts)
        QuickPill(label = "📷 Scan Receipt", onClick = { onNavigateToCapture("OCR") })
        QuickPill(label = "🎤 Voice Entry", onClick = { onNavigateToCapture("VOICE") })
        QuickPill(label = "📊 Monthly Digest", onClick = onNavigateToEngagement)
        QuickPill(label = "💰 Incomes", onClick = onNavigateToIncome)
        QuickPill(label = "🏦 Accounts", onClick = onNavigateToAccounts)
        QuickPill(label = "🔁 Subscriptions", onClick = onNavigateToRecurring)
        QuickPill(label = "🎯 Savings Goals", onClick = onNavigateToSavingsGoals)
        QuickPill(label = "📄 Export PDF/CSV", onClick = onNavigateToExport)
    }
}

@Composable
private fun QuickPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StitchTopCategoriesSection(
    summary: DashboardSummary,
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top Categories",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "View all",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        if (summary.categoryBreakdown.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No expenses recorded this cycle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pastelBgs = listOf(Color(0xFFFFF6F0), Color(0xFFF8F4FA), Color(0xFFF2F6FA))
                val borderColors = listOf(Color(0xFFF5E5DA), Color(0xFFE9E0F0), Color(0xFFDCE5EF))
                val iconBgs = listOf(Color(0xFFFCE5D8), Color(0xFFEFE6F5), Color(0xFFDCE8F2))
                val textTints = listOf(Color(0xFFA65B32), Color(0xFF695876), Color(0xFF5B7C99))

                summary.categoryBreakdown.take(4).forEachIndexed { index, cat ->
                    val colorIdx = index % pastelBgs.size
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = pastelBgs[colorIdx],
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColors[colorIdx]),
                        modifier = Modifier
                            .width(136.dp)
                            .clickable { onViewAllClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(iconBgs[colorIdx]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = textTints[colorIdx],
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "${cat.percentageOfTotal.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = textTints[colorIdx]
                                )
                            }

                            Column {
                                Text(
                                    text = cat.category.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = CurrencyFormatter.format(cat.totalSpent),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchRecentTransactionsSection(
    recentExpenses: List<Expense>,
    onSeeAllClick: () -> Unit,
    onExpenseClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        if (recentExpenses.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No recent transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    recentExpenses.take(5).forEachIndexed { index, expense ->
                        StitchTransactionRow(
                            expense = expense,
                            onClick = { onExpenseClick(expense.id) }
                        )

                        if (index < recentExpenses.take(5).size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchTransactionRow(
    expense: Expense,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val dateStr = expense.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CurrencyFormatter.format(expense.money),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = expense.source.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardMonthDropdown(
    selectedYearMonth: YearMonth,
    availableMonths: List<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val formattedSelected = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            shadowElevation = if (expanded) 2.dp else 0.5.dp,
            modifier = Modifier.clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = formattedSelected,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    ),
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Select period",
                    tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            val currentYearMonth = YearMonth.now()

            availableMonths.forEach { ym ->
                val isSelected = ym == selectedYearMonth
                val isCurrent = ym == currentYearMonth
                val label = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Current",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectMonth(ym)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                    )
                )
            }
        }
    }
}

