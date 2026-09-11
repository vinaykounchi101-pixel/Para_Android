package com.paradox.app.feature.recurring

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.ui.components.ParadoxButton
import com.paradox.app.core.ui.components.ParadoxFilterChip
import com.paradox.app.core.ui.components.ParadoxTextField
import com.paradox.app.domain.model.RecurringFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringScreen(
    recurringId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.editState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(recurringId) {
        viewModel.initAddEdit(recurringId)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                RecurringEvent.NavigateBack -> onNavigateBack()
                is RecurringEvent.ShowToast -> Unit
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate
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
                            viewModel.onStartDateSelected(selectedDate)
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
                        text = if (uiState.isEditMode) "Edit Subscription" else "Add Subscription",
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
            // Title
            ParadoxTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = "Subscription Name",
                placeholder = "e.g. Netflix, Spotify, Gym, Rent",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Title,
                        contentDescription = "Title"
                    )
                },
                isError = uiState.errorMessage != null && uiState.title.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Amount
            ParadoxTextField(
                value = uiState.amountInput,
                onValueChange = { viewModel.onAmountChange(it) },
                label = "Billing Amount",
                placeholder = "0.00",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyRupee,
                        contentDescription = "Amount"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.errorMessage != null && uiState.amountInput.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Billing Frequency",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurringFrequency.values().forEach { freq ->
                        ParadoxFilterChip(
                            selected = uiState.frequency == freq,
                            onClick = { viewModel.onFrequencySelected(freq) },
                            label = freq.displayName
                        )
                    }
                }
            }

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { category ->
                        ParadoxFilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { viewModel.onCategorySelected(category.id) },
                            label = category.name
                        )
                    }
                }
            }

            // Payment Method Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.paymentMethods.forEach { method ->
                        ParadoxFilterChip(
                            selected = uiState.selectedPaymentMethodId == method.id,
                            onClick = { viewModel.onPaymentMethodSelected(method.id) },
                            label = method.label
                        )
                    }
                }
            }

            // Start / Due Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Start / Next Due Date",
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
                                text = uiState.startDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")),
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

            // Notes
            ParadoxTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = "Notes (Optional)",
                placeholder = "Add cancellation terms, account email, etc.",
                singleLine = false,
                maxLines = 4,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Notes"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Auto-renewal switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Subscription",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Include in monthly commitment calculations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = { viewModel.onActiveToggled(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            ParadoxButton(
                text = if (uiState.isEditMode) "Save Changes" else "Add Subscription",
                onClick = { viewModel.saveRecurring() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
