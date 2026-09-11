package com.paradox.app.feature.account

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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.paradox.app.core.ui.components.ParadoxFilterChip
import com.paradox.app.core.ui.components.ParadoxTextField
import com.paradox.app.domain.model.AccountType

private val AccountColors = listOf(
    "#3B82F6", // Blue
    "#10B981", // Emerald
    "#8B5CF6", // Purple
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#06B6D4", // Cyan
    "#6366F1", // Indigo
    "#64748B"  // Slate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    accountId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        viewModel.initAddEdit(accountId)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                AccountEvent.NavigateBack -> onNavigateBack()
                is AccountEvent.ShowToast -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Account" else "Add Account",
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
            // Account Name
            ParadoxTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = "Account Name",
                placeholder = "e.g. HDFC Bank, PayTM Wallet, Cash",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = "Account Name"
                    )
                },
                isError = uiState.errorMessage != null,
                errorMessage = uiState.errorMessage,
                modifier = Modifier.fillMaxWidth()
            )

            // Account Type
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.values().forEach { type ->
                        ParadoxFilterChip(
                            label = type.displayName,
                            selected = uiState.type == type,
                            onClick = { viewModel.onTypeSelected(type) }
                        )
                    }
                }
            }

            // Initial Balance
            ParadoxTextField(
                value = uiState.initialBalanceInput,
                onValueChange = { viewModel.onInitialBalanceChange(it) },
                label = "Starting Balance",
                placeholder = "0.00",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyRupee,
                        contentDescription = "Balance"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Theme Color",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccountColors.forEach { hex ->
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

            // Default Account Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Set as Primary Account",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Used as the default payment source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isDefault,
                    onCheckedChange = { viewModel.onDefaultToggled(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            ParadoxButton(
                text = if (uiState.isEditMode) "Save Changes" else "Create Account",
                onClick = { viewModel.saveAccount() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
