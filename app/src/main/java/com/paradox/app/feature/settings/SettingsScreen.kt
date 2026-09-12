package com.paradox.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Layers
import com.paradox.app.feature.quickball.QuickBallOverlayService
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyOff
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.paradox.app.core.ui.components.BottomNavDestination
import com.paradox.app.core.ui.components.ConfirmDeleteDialog
import com.paradox.app.core.ui.components.ParadoxBottomNavBar
import com.paradox.app.core.ui.theme.ThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToEngagement: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = onNavigateBack,
    onNavigateToLedger: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToUnlock: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.NavigateToUnlock -> onNavigateToUnlock()
                SettingsEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                is SettingsEvent.ShowToast -> Unit
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmDeleteDialog(
            title = "Delete Profile & Vault Data",
            message = "Are you sure you want to permanently erase this profile? All expenses, categories, wallets, and local database keys will be destroyed.",
            onConfirm = viewModel::deleteProfile,
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    if (uiState.showApiKeyDialog) {
        ApiKeyInputDialog(
            title = "Enable AI Features",
            errorMessage = uiState.apiKeyInputError,
            onConfirm = viewModel::saveNewApiKey,
            onDismiss = viewModel::closeApiKeyDialog
        )
    }

    if (uiState.showEditKeyDialog) {
        ApiKeyInputDialog(
            title = "Edit API Key",
            errorMessage = uiState.apiKeyInputError,
            onConfirm = viewModel::editApiKey,
            onDismiss = viewModel::closeEditKeyDialog
        )
    }

    if (uiState.showRemoveKeyDialog) {
        ConfirmDeleteDialog(
            title = "Remove API Key",
            message = "Are you sure you want to delete your stored API key? AI features will be disabled until a new key is configured.",
            onConfirm = viewModel::removeApiKey,
            onDismiss = viewModel::closeRemoveKeyDialog
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionBottomSheet(
            currentLanguage = uiState.appLanguage,
            onSelectLanguage = { lang ->
                viewModel.setAppLanguage(lang)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    Scaffold(
        bottomBar = {
            ParadoxBottomNavBar(
                currentDestination = BottomNavDestination.SETTINGS,
                onNavigateToDestination = { destination ->
                    when (destination) {
                        BottomNavDestination.DASHBOARD -> onNavigateToDashboard()
                        BottomNavDestination.LEDGER -> onNavigateToLedger()
                        BottomNavDestination.BUDGETS -> onNavigateToBudgets()
                        BottomNavDestination.INSIGHTS -> onNavigateToInsights()
                        BottomNavDestination.SETTINGS -> { /* Current */ }
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Vault",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.activeProfile?.name ?: "Personal Vault",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SQLCipher 256-bit Encrypted",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = { viewModel.signOut() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Appearance & Pastel Themes Group
            SettingsGroup(title = "Appearance & Pastel Themes") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Theme Mode Selector (System / Light / Dark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple("SYSTEM", "Auto", Icons.Outlined.BrightnessAuto),
                            Triple("LIGHT", "Light", Icons.Outlined.LightMode),
                            Triple("DARK", "Dark", Icons.Outlined.DarkMode)
                        )
                        modes.forEach { (mode, label, icon) ->
                            val isSelected = uiState.themeMode.equals(mode, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Pastel Palettes Section Header
                    Text(
                        text = "Curated Pastel Palettes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val palettes = ThemePalette.entries
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        palettes.chunked(2).forEach { rowPalettes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPalettes.forEach { palette ->
                                    val isSelected = uiState.themePalette.equals(palette.id, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setThemePalette(palette.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Dual Color Preview Swatch
                                                Row(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .background(palette.previewColor)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .background(palette.previewSecondary)
                                                    )
                                                }

                                                Text(
                                                    text = palette.displayName,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Assistive Quick Ball Group
            val context = LocalContext.current
            SettingsGroup(title = "Assistive Quick Ball") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Edge-docked floating shortcut ball for instant 1-tap capture & copilot access.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val modes = listOf(
                        Triple("OFF", "Disabled", "No floating ball"),
                        Triple("IN_APP", "In-App Only (Recommended)", "Zero permissions • Floats only within Paradox"),
                        Triple("SYSTEM_WIDE", "System-Wide Floating", "Floats over all apps (GPay, Swiggy, etc.)")
                    )

                    modes.forEach { (mode, title, desc) ->
                        val isSelected = uiState.quickBallMode.equals(mode, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (mode == "SYSTEM_WIDE") {
                                        viewModel.setQuickBallMode("SYSTEM_WIDE")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } else {
                                            QuickBallOverlayService.start(context)
                                        }
                                    } else {
                                        QuickBallOverlayService.stop(context)
                                        viewModel.setQuickBallMode(mode)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Security & Vault Group
            SettingsGroup(title = "Security & Access") {
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Sign Out",
                    subtitle = "Securely end session and return to login page",
                    onClick = viewModel::signOut
                )

                SettingsActionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Lock Vault Now",
                    subtitle = "Instantly secure the app with PIN, Password, or Pattern",
                    onClick = viewModel::lockVault
                )

                SettingsActionRow(
                    icon = Icons.Outlined.PersonAdd,
                    title = "Create New Profile",
                    subtitle = "Set up an additional local encrypted ledger",
                    onClick = onNavigateToOnboarding
                )

                if (uiState.canUseBiometric) {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = "Use fingerprint or face unlock",
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = viewModel::toggleBiometric
                    )
                }

                SettingsActionRow(
                    icon = Icons.Outlined.Security,
                    title = "Encrypted Vault Backup",
                    subtitle = "Export or restore password-protected vault",
                    onClick = onNavigateToBackup
                )
            }

            // Financial Management Group
            SettingsGroup(title = "Financial Management") {
                SettingsActionRow(
                    icon = Icons.Outlined.Category,
                    title = "Categories",
                    subtitle = "Manage expense & income categories",
                    onClick = onNavigateToCategories
                )
                SettingsActionRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Wallets & Accounts",
                    subtitle = "Cash, banks, UPI, and digital cards",
                    onClick = onNavigateToAccounts
                )
                SettingsActionRow(
                    icon = Icons.Outlined.Autorenew,
                    title = "Recurring & Subscriptions",
                    subtitle = "Track regular billing cycles",
                    onClick = onNavigateToRecurring
                )
                SettingsActionRow(
                    icon = Icons.Outlined.Savings,
                    title = "Savings Goals",
                    subtitle = "Set targets & track contributions",
                    onClick = onNavigateToSavingsGoals
                )
                SettingsActionRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Debts & Udhaar",
                    subtitle = "Track money lent, borrowed & settlements",
                    onClick = onNavigateToDebts
                )
                SettingsActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Export Reports",
                    subtitle = "Generate RFC-4180 CSV & vector PDFs",
                    onClick = onNavigateToExport
                )
            }

            // AI Features & Intelligence
            SettingsGroup(title = "AI Features & Intelligence") {
                SettingsSwitchRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "AI Features",
                    subtitle = if (uiState.isAiEnabled) "Enabled • Grounded queries, insights & suggestions" else "Disabled • Offline local computations only",
                    checked = uiState.isAiEnabled,
                    onCheckedChange = viewModel::toggleAiFeatures
                )

                SettingsActionRow(
                    icon = Icons.Outlined.Key,
                    title = "API Configuration",
                    subtitle = if (uiState.hasApiKey) "Configured (••••••••)" else "Not configured — Tap to setup",
                    onClick = {
                        if (uiState.hasApiKey) viewModel.openEditKeyDialog() else viewModel.openApiKeyDialog()
                    }
                )

                if (uiState.hasApiKey) {
                    SettingsActionRow(
                        icon = Icons.Outlined.Edit,
                        title = "Edit API Key",
                        subtitle = "Update or replace existing encrypted key",
                        onClick = viewModel::openEditKeyDialog
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.KeyOff,
                        title = "Remove API Key",
                        subtitle = "Securely delete stored key from Keystore",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = viewModel::openRemoveKeyDialog
                    )
                }
            }

            // Cloud & Preferences
            SettingsGroup(title = "Ecosystem & Sync") {
                SettingsActionRow(
                    icon = Icons.Outlined.CloudSync,
                    title = "Offline Sync Queue",
                    subtitle = "Manage sync engine and outbox records",
                    onClick = onNavigateToSync
                )
                val activeLanguageLabel = when (uiState.appLanguage) {
                    "hi" -> "Active: हिन्दी (Hindi)"
                    "mr" -> "Active: मराठी (Marathi)"
                    "hi-Latn" -> "Active: Hinglish (AI Mix)"
                    else -> "Active: English (EN)"
                }
                SettingsActionRow(
                    icon = Icons.Outlined.Translate,
                    title = "Language & Localization",
                    subtitle = activeLanguageLabel,
                    onClick = { showLanguageSheet = true }
                )
            }

            // Danger Zone
            SettingsGroup(title = "Danger Zone") {
                SettingsActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Destroy Local Vault",
                    subtitle = "Permanently delete profile and database",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteConfirmation = true }
                )
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private data class LanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val subtitle: String
)

private val AppLanguages = listOf(
    LanguageOption("en", "English", "English (US / India)", "Default universal financial vocabulary"),
    LanguageOption("hi", "हिन्दी", "Hindi", "भारतीय क्षेत्रीय भाषा"),
    LanguageOption("mr", "मराठी", "Marathi", "महाराष्ट्र प्रादेशिक भाषा"),
    LanguageOption("hi-Latn", "Hinglish", "Conversational AI Mix", "Voice, OCR & Minglish natural input")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionBottomSheet(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Language & Localization",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose preferred app dialect & AI voice assistant",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            AppLanguages.forEach { lang ->
                val isSelected = lang.code == currentLanguage

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLanguage(lang.code) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = lang.code.uppercase().take(2),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lang.nativeName} (${lang.englishName})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lang.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyInputDialog(
    title: String,
    initialKey: String = "",
    errorMessage: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initialKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your personal Gemini API key. Your key is securely encrypted with hardware-backed AES-256-GCM directly in Android KeyStore. It is never logged or exposed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Paste your API key here") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                "Obtain free key from Google AI Studio (aistudio.google.com)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide key" else "Show key")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(apiKey) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

