package com.paradox.app.feature.savingsgoal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.ui.components.ParadoxButton
import com.paradox.app.core.ui.components.ParadoxTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val GoalColors = listOf(
    "#10B981", // Emerald
    "#3B82F6", // Blue
    "#8B5CF6", // Purple
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#06B6D4", // Cyan
    "#E11D48", // Rose
    "#64748B"  // Slate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsGoalScreen(
    goalId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: SavingsGoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.editState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(goalId) {
        viewModel.initAddEdit(goalId)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SavingsGoalEvent.NavigateBack -> onNavigateBack()
                is SavingsGoalEvent.ShowToast -> Unit
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.targetDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.onTargetDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Savings Goal" else "Create Savings Goal",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            ParadoxTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = "Goal Name",
                placeholder = "e.g. Emergency Fund, Goa Trip, New MacBook",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Savings,
                        contentDescription = "Goal Name"
                    )
                },
                isError = uiState.errorMessage != null && uiState.name.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Target Amount
            ParadoxTextField(
                value = uiState.targetAmountInput,
                onValueChange = { viewModel.onTargetAmountChange(it) },
                label = "Target Amount",
                placeholder = "50000.00",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyRupee,
                        contentDescription = "Target Amount"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.errorMessage != null && uiState.targetAmountInput.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Target Date
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Target Completion Date",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Date"
                            )
                            Text(
                                text = uiState.targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Theme Color
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Badge Color",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = uiState.colorHex.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onColorChange(hex) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            ParadoxButton(
                text = if (uiState.isEditMode) "Save Changes" else "Create Goal",
                onClick = { viewModel.saveGoal() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
