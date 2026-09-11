package com.paradox.app.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.paradox.app.core.util.VoiceState
import com.paradox.app.feature.capture.ocr.CameraScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureHubScreen(
    onNavigateBack: () -> Unit,
    initialMode: CaptureMode = CaptureMode.QUICK_ADD,
    sharedText: String? = null,
    sharedImageUri: Uri? = null,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialMode) {
        viewModel.setMode(initialMode)
    }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            viewModel.setMode(CaptureMode.QUICK_ADD)
            viewModel.onInputTextChanged(sharedText)
        }
    }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            viewModel.setMode(CaptureMode.OCR)
            viewModel.processImageUri(sharedImageUri)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Expense saved successfully!")
            viewModel.resetSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Permission launchers
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.startVoiceListening()
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUri(uri)
        }
    }

    // CSV Document Picker Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    viewModel.onCsvContentSelected(content)
                }
            } catch (e: Exception) {
                // Handle read failure
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assisted Capture") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedMode.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedMode == CaptureMode.QUICK_ADD,
                    onClick = { viewModel.setMode(CaptureMode.QUICK_ADD) },
                    text = { Text("Quick Add") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedMode == CaptureMode.VOICE,
                    onClick = { viewModel.setMode(CaptureMode.VOICE) },
                    text = { Text("Voice") },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedMode == CaptureMode.OCR,
                    onClick = { viewModel.setMode(CaptureMode.OCR) },
                    text = { Text("Scan Receipt") },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedMode == CaptureMode.CSV,
                    onClick = { viewModel.setMode(CaptureMode.CSV) },
                    text = { Text("CSV Import") },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) }
                )
            }

            // Mode Content & Candidate Preview
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode-specific Input Card
                item {
                    when (uiState.selectedMode) {
                        CaptureMode.QUICK_ADD -> {
                            QuickAddInputSection(
                                text = uiState.inputText,
                                onTextChanged = viewModel::onInputTextChanged
                            )
                        }
                        CaptureMode.VOICE -> {
                            VoiceCaptureSection(
                                voiceState = uiState.voiceState,
                                hasAudioPermission = hasAudioPermission,
                                onRequestPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                onStartListening = viewModel::startVoiceListening,
                                onStopListening = viewModel::stopVoiceListening
                            )
                        }
                        CaptureMode.OCR -> {
                            OcrScanSection(
                                hasCameraPermission = hasCameraPermission,
                                onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                onPickFromGallery = { galleryLauncher.launch("image/*") },
                                onLinesExtracted = viewModel::processCapturedLines
                            )
                        }
                        CaptureMode.CSV -> {
                            CsvImportSection(
                                onPickCsv = { csvLauncher.launch("text/*") },
                                preview = uiState.csvPreview,
                                isProcessing = uiState.isProcessing,
                                onCommit = viewModel::commitCsvImport
                            )
                        }
                    }
                }

                // Candidate Preview Card (Shown for Quick Add, Voice, and OCR when candidate parsed)
                if (uiState.selectedMode != CaptureMode.CSV && uiState.candidate != null) {
                    item {
                        CandidatePreviewCard(
                            candidate = uiState.candidate!!,
                            categories = uiState.categories,
                            paymentMethods = uiState.paymentMethods,
                            isProcessing = uiState.isProcessing,
                            onTitleChange = viewModel::updateCandidateTitle,
                            onAmountChange = viewModel::updateCandidateAmount,
                            onCategoryChange = viewModel::updateCandidateCategory,
                            onPaymentChange = viewModel::updateCandidatePaymentMethod,
                            onNotesChange = viewModel::updateCandidateNotes,
                            onSave = viewModel::saveCandidateExpense
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddInputSection(
    text: String,
    onTextChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Type Natural Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "e.g., \"Uber 250 cash yesterday\", \"Starbucks 450 card\", \"Groceries 1200 upi\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text("What did you spend on?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun VoiceCaptureSection(
    voiceState: VoiceState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val isListening = voiceState is VoiceState.Listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voice Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Speak naturally (e.g., \"Coffee 300 cash today\")",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Mic Button with pulsing animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (!hasAudioPermission) {
                            onRequestPermission()
                        } else {
                            if (isListening) onStopListening() else onStartListening()
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (voiceState) {
                is VoiceState.Listening -> Text("Listening...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                is VoiceState.Recognized -> Text("\"${voiceState.text}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                is VoiceState.Error -> Text(voiceState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                is VoiceState.Idle -> Text("Tap mic to speak", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OcrScanSection(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit,
    onLinesExtracted: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Receipt OCR Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan bills and receipts to extract merchant, total & date automatically",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission is required to scan receipts live.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestPermission) {
                        Text("Grant Camera Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onPickFromGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Gallery Instead")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    CameraScannerView(
                        onLinesExtracted = onLinesExtracted,
                        onPickFromGallery = onPickFromGallery
                    )
                }
            }
        }
    }
}

@Composable
private fun CsvImportSection(
    onPickCsv: () -> Unit,
    preview: com.paradox.app.domain.usecase.capture.CsvImportPreview?,
    isProcessing: Boolean,
    onCommit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CSV Statement Import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Import bank/card statements in CSV format with automatic column detection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickCsv,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select CSV File")
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (preview != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Import Summary: ${preview.validCandidates.size} valid rows (${preview.invalidRowCount} skipped)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Headers detected: ${preview.detectedHeaders.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview first 5 rows
                        preview.validCandidates.take(5).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(
                                    com.paradox.app.core.money.CurrencyFormatter.format(row.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCommit,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Commit ${preview.validCandidates.size} Transactions")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidatePreviewCard(
    candidate: EditableCandidate,
    categories: List<com.paradox.app.domain.model.Category>,
    paymentMethods: List<com.paradox.app.domain.model.PaymentMethod>,
    isProcessing: Boolean,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPaymentChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Parsed Transaction Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Duplicate Warning Banner
            if (candidate.duplicateWarning.isLikelyDuplicate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Duplicate Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = candidate.duplicateWarning.reason ?: "Possible duplicate transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Title
            OutlinedTextField(
                value = candidate.title,
                onValueChange = onTitleChange,
                label = { Text("Title / Merchant") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Amount
            OutlinedTextField(
                value = candidate.amountStr,
                onValueChange = onAmountChange,
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Category Dropdown
            var catExpanded by remember { mutableStateOf(false) }
            val selectedCat = categories.firstOrNull { it.id == candidate.categoryId }

            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCat?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                onCategoryChange(cat.id)
                                catExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Payment Method Dropdown
            var payExpanded by remember { mutableStateOf(false) }
            val selectedPay = paymentMethods.firstOrNull { it.id == candidate.paymentMethodId }

            ExposedDropdownMenuBox(
                expanded = payExpanded,
                onExpandedChange = { payExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPay?.label ?: "Select Payment Method",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = payExpanded,
                    onDismissRequest = { payExpanded = false }
                ) {
                    paymentMethods.forEach { pm ->
                        DropdownMenuItem(
                            text = { Text(pm.label) },
                            onClick = {
                                onPaymentChange(pm.id)
                                payExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Notes
            OutlinedTextField(
                value = candidate.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = onSave,
                enabled = !isProcessing && candidate.amountStr.isNotBlank() && candidate.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Save to Ledger")
                }
            }
        }
    }
}
