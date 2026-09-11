package com.paradox.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paradox.app.core.ui.theme.ParadoxTheme
import com.paradox.app.domain.model.BudgetHealth

@Composable
fun ParadoxProgressBar(
    progress: Float, // 0.0 to 1.0+
    health: BudgetHealth,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = clampedProgress, label = "progress")

    val fillColor = when (health) {
        BudgetHealth.ON_TRACK -> MaterialTheme.colorScheme.primary
        BudgetHealth.NEAR_LIMIT -> ParadoxTheme.colors.warning
        BudgetHealth.OVER_BUDGET -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(ParadoxTheme.colors.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(fillColor)
        )
    }
}
