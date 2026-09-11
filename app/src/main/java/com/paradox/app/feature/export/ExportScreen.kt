package com.paradox.app.feature.export

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradox.app.core.ui.components.ParadoxButton
import com.paradox.app.core.ui.components.ParadoxCard
import com.paradox.app.core.ui.components.ParadoxFilterChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExportEvent.ShareFile -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        event.file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Paradox Statement"))
                }
                is ExportEvent.ShowToast -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Statement",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Info Card
            ParadoxCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Offline & Private Data Export",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Export your financial records into clean CSV spreadsheets or formatted PDF statements directly on device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Export Format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParadoxFilterChip(
                        label = "PDF Statement",
                        selected = state.selectedFormat == ExportFormat.PDF,
                        onClick = { viewModel.selectFormat(ExportFormat.PDF) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "PDF"
                            )
                        }
                    )
                    ParadoxFilterChip(
                        label = "CSV Spreadsheet",
                        selected = state.selectedFormat == ExportFormat.CSV,
                        onClick = { viewModel.selectFormat(ExportFormat.CSV) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.TableChart,
                                contentDescription = "CSV"
                            )
                        }
                    )
                }
            }

            // Date Range
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportDateRange.values().forEach { range ->
                        ParadoxFilterChip(
                            label = range.displayName,
                            selected = state.selectedRange == range,
                            onClick = { viewModel.selectRange(range) }
                        )
                    }
                }
            }

            // Transaction Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Included Records",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportType.values().forEach { type ->
                        val label = when (type) {
                            ExportType.ALL -> "Income & Expenses"
                            ExportType.EXPENSES_ONLY -> "Expenses Only"
                            ExportType.INCOME_ONLY -> "Income Only"
                        }
                        ParadoxFilterChip(
                            label = label,
                            selected = state.selectedType == type,
                            onClick = { viewModel.selectType(type) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Action Button
            ParadoxButton(
                text = if (state.isExporting) "Generating File..." else "Generate & Share File",
                onClick = { viewModel.exportAndShare() },
                enabled = !state.isExporting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
