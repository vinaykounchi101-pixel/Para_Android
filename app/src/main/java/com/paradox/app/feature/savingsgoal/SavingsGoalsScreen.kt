package com.paradox.app.feature.savingsgoal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.core.ui.components.ConfirmDeleteDialog
import com.paradox.app.core.ui.components.ParadoxCard
import com.paradox.app.core.ui.components.ParadoxProgressBar
import com.paradox.app.core.ui.components.ParadoxTextField
import com.paradox.app.core.ui.components.StateContainer
import com.paradox.app.domain.model.BudgetHealth
import com.paradox.app.domain.model.SavingsGoal
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddGoal: () -> Unit,
    onNavigateToEditGoal: (String) -> Unit,
    viewModel: SavingsGoalViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToContribute by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddGoal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Goal"
                )
            }
        }
    ) { innerPadding ->
        StateContainer(
            isLoading = state.isLoading,
            data = state.goals,
            emptyMessage = "No savings goals yet. Set targets for emergency funds, vacations, or major purchases.",
            isEmpty = { it.isEmpty() },
            errorMessage = state.errorMessage,
            onRetry = { },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { _ ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Targets",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(state.goals, key = { it.id }) { goal ->
                    SavingsGoalItem(
                        goal = goal,
                        onContribute = { goalToContribute = goal },
                        onEdit = { onNavigateToEditGoal(goal.id) },
                        onDelete = { goalToDelete = goal }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Contribute Dialog
    goalToContribute?.let { goal ->
        ContributeDialog(
            goal = goal,
            onDismiss = { goalToContribute = null },
            onConfirm = { amount ->
                viewModel.contributeToGoal(goal.id, amount)
                goalToContribute = null
            }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete Savings Goal?",
            message = "Are you sure you want to delete \"${goal.name}\"? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteGoal(goal.id)
                goalToDelete = null
            },
            onDismiss = { goalToDelete = null }
        )
    }
}

@Composable
private fun SavingsGoalItem(
    goal: SavingsGoal,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progressPct = goal.progressPercentage()
    val isAchieved = goal.isAchieved()
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    ParadoxCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon + Name + Category + Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAchieved) Icons.Default.CheckCircle else Icons.Default.Savings,
                            contentDescription = goal.name,
                            tint = goalColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: ${goal.targetDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress bar
            val health = when {
                isAchieved -> BudgetHealth.ON_TRACK
                progressPct >= 75.0 -> BudgetHealth.ON_TRACK
                progressPct >= 25.0 -> BudgetHealth.NEAR_LIMIT
                else -> BudgetHealth.OVER_BUDGET
            }
            ParadoxProgressBar(
                progress = (progressPct / 100.0).toFloat(),
                health = health,
                modifier = Modifier.fillMaxWidth()
            )

            // Numbers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(goal.currentAmount),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = goalColor
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(progressPct)}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(goal.targetAmount),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Quick Contribute Button
            if (!isAchieved) {
                OutlinedButton(
                    onClick = onContribute,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Contribute",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Add Contribution")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 Goal Completed!",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    onConfirm: (BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add to ${goal.name}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How much would you like to save toward this goal?",
                    style = MaterialTheme.typography.bodyMedium
                )
                ParadoxTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        error = null
                    },
                    label = "Amount",
                    placeholder = "500.00",
                    isError = error != null,
                    errorMessage = error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val decimal = amountInput.toBigDecimalOrNull()
                    if (decimal == null || decimal <= BigDecimal.ZERO) {
                        error = "Enter a valid amount"
                    } else {
                        onConfirm(decimal)
                    }
                }
            ) {
                Text("Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
