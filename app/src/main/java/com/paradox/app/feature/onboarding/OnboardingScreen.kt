package com.paradox.app.feature.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.ui.components.ParadoxButton
import com.paradox.app.core.ui.components.ParadoxCard
import com.paradox.app.core.ui.components.ParadoxTextField
import com.paradox.app.domain.model.AuthType

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToDashboard -> onNavigateToDashboard()
                is OnboardingEvent.ShowToast -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App Icon & Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Vault",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to Paradox",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your private, local-first encrypted financial companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            ParadoxTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = "Profile Name",
                placeholder = "e.g. Personal, Business",
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ParadoxTextField(
                value = uiState.credential,
                onValueChange = viewModel::onCredentialChanged,
                label = if (uiState.authType == AuthType.PIN) "Set 4-Digit Security PIN" else "Set Password",
                placeholder = if (uiState.authType == AuthType.PIN) "••••" else "Enter password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uiState.authType == AuthType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                ),
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ParadoxTextField(
                value = uiState.confirmCredential,
                onValueChange = viewModel::onConfirmCredentialChanged,
                label = "Confirm PIN / Password",
                placeholder = if (uiState.authType == AuthType.PIN) "••••" else "Confirm password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uiState.authType == AuthType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                ),
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.canEnableBiometrics) {
                ParadoxCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = "Biometric",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Enable Biometric Unlock",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Unlock with fingerprint or face sensor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = viewModel::onBiometricToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ParadoxButton(
                text = if (uiState.isLoading) "Creating Encrypted Vault..." else "Create Vault & Get Started",
                onClick = viewModel::createProfile,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
