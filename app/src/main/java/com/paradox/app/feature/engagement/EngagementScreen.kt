package com.paradox.app.feature.engagement

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paradox.app.feature.insights.MetricRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSplit: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: EngagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Digest & Growth", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSplit) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Split Expense Calculator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Financial Vibe Card
                uiState.digest?.vibe?.let { vibe ->
                    item {
                        com.paradox.app.core.ui.components.AiFeatureDisabledContainer(
                            isAiEnabled = uiState.isAiEnabled,
                            featureName = "Financial Vibe & Roast",
                            onNavigateToSettings = onNavigateToSettings
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("MONTHLY FINANCIAL VIBE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(vibe.vibeEmoji, fontSize = 28.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(vibe.vibeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(vibe.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(vibe.summary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Monthly Digest Card
                uiState.digest?.let { d ->
                    item {
                        com.paradox.app.core.ui.components.AiFeatureDisabledContainer(
                            isAiEnabled = uiState.isAiEnabled,
                            featureName = "Monthly AI Digest",
                            onNavigateToSettings = onNavigateToSettings
                        ) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Monthly Highlights (${d.monthYearLabel})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(d.highlightSentence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    MetricRow("Total Income Recorded", "₹${d.totalEarned.amount}")
                                    MetricRow("Total Expenses Logged", "₹${d.totalSpent.amount}")
                                    MetricRow("Net Savings Generated", "₹${d.netSaved.amount} (${d.savingsRatePct.toInt()}%)")
                                    MetricRow("Top Expense Category", "${d.topCategory} (₹${d.topCategoryAmount.amount})")
                                    MetricRow("Largest Single Expense", "${d.biggestExpenseTitle} (₹${d.biggestExpenseAmount.amount})")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(d.positiveEncouragement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Logging Streak & Consistency
                uiState.streak?.let { streak ->
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tracking Habits & Consistency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${streak.currentStreakDays}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                        Text("Current Streak", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${streak.longestStreakDays}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Best Streak", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${streak.totalExpensesLogged}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text("Total Entries", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Milestones & Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(streak.badges) { badge ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (badge.icon) {
                                        "flag" -> Icons.Default.Flag
                                        "bolt" -> Icons.Default.Bolt
                                        "military_tech" -> Icons.Default.MilitaryTech
                                        else -> Icons.Default.WorkspacePremium
                                    },
                                    contentDescription = null,
                                    tint = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        badge.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        badge.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (badge.isUnlocked) 1f else 0.5f)
                                    )
                                }
                                if (badge.isUnlocked) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
