package com.paradox.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.paradox.app.R
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Paradox Obsidian Quick Ball
 * An assistive, edge-docked floating action ball that expands into a radial
 * menu for ultra-fast transaction capture and conversational copilot access.
 */
@Composable
fun QuickBallView(
    onQuickAdd: () -> Unit,
    onVoiceEntry: () -> Unit,
    onScanReceipt: () -> Unit,
    onAskParadox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isIdle by remember { mutableStateOf(false) }

    // Position tracking
    var isDockedLeft by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(600f) }
    val animatedOffsetX = remember { Animatable(0f) }

    // Idle timer (auto-dim and edge tuck after 3.5s of inactivity)
    LaunchedEffect(isExpanded, isDragging, isIdle) {
        if (!isExpanded && !isDragging) {
            delay(3500)
            isIdle = true
        } else {
            isIdle = false
        }
    }

    val ballAlpha by animateFloatAsState(
        targetValue = when {
            isExpanded -> 1f
            isDragging -> 0.95f
            isIdle -> 0.40f
            else -> 0.85f
        },
        animationSpec = tween(400),
        label = "ballAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val ballSizePx = with(density) { 48.dp.toPx() }

        // Background scrim when menu is expanded
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = false
                        }
                    )
            )
        }

        // Floating Ball Container
        Box(
            modifier = Modifier
                .offset {
                    val x = if (isDockedLeft) {
                        if (isIdle && !isExpanded) -ballSizePx * 0.4f else 8f
                    } else {
                        if (isIdle && !isExpanded) (screenWidthPx - ballSizePx * 0.6f) else (screenWidthPx - ballSizePx - 8f)
                    }
                    IntOffset(x.roundToInt(), offsetY.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            isIdle = false
                            if (isExpanded) isExpanded = false
                        },
                        onDragEnd = {
                            isDragging = false
                            // Magnetic snap to nearest edge
                            isDockedLeft = animatedOffsetX.value < (screenWidthPx / 2)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY = (offsetY + dragAmount.y).coerceIn(100f, screenHeightPx - ballSizePx - 200f)
                            coroutineScope.launch {
                                animatedOffsetX.snapTo(if (isDockedLeft) 0f else screenWidthPx - ballSizePx)
                            }
                        }
                    )
                }
        ) {
            val expansionProgress by animateFloatAsState(
                targetValue = if (isExpanded) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "inAppRadialExpansion"
            )

            // Radial Curved Action Buttons
            if (isExpanded || expansionProgress > 0.01f) {
                // 1. Quick Add (Top Arc ~ -55 deg) - Minimal Light Mint
                InAppRadialCurvedItem(
                    icon = Icons.Default.Edit,
                    label = "Quick Add",
                    surfaceColor = Color(0xFFF0FDF4),
                    borderColor = Color(0xFF86EFAC),
                    textColor = Color(0xFF14532D),
                    badgeBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    targetOffsetX = if (isDockedLeft) 56.dp else (-142).dp,
                    targetOffsetY = (-76).dp,
                    progress = expansionProgress,
                    isDockedLeft = isDockedLeft,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = false
                        onQuickAdd()
                    }
                )

                // 2. Voice Entry (Upper Middle Arc ~ -18 deg) - Minimal Light Sky Cyan
                InAppRadialCurvedItem(
                    icon = Icons.Default.Mic,
                    label = "Voice Entry",
                    surfaceColor = Color(0xFFF0F9FF),
                    borderColor = Color(0xFF7DD3FC),
                    textColor = Color(0xFF0C4A6E),
                    badgeBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    targetOffsetX = if (isDockedLeft) 72.dp else (-162).dp,
                    targetOffsetY = (-26).dp,
                    progress = expansionProgress,
                    isDockedLeft = isDockedLeft,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = false
                        onVoiceEntry()
                    }
                )

                // 3. Scan Receipt (Lower Middle Arc ~ +18 deg) - Minimal Light Periwinkle Blue
                InAppRadialCurvedItem(
                    icon = Icons.Default.CameraAlt,
                    label = "Scan Receipt",
                    surfaceColor = Color(0xFFEEF2FF),
                    borderColor = Color(0xFF93C5FD),
                    textColor = Color(0xFF1E3A8A),
                    badgeBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    targetOffsetX = if (isDockedLeft) 72.dp else (-162).dp,
                    targetOffsetY = 26.dp,
                    progress = expansionProgress,
                    isDockedLeft = isDockedLeft,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = false
                        onScanReceipt()
                    }
                )

                // 4. Ask Paradox AI (Bottom Arc ~ +55 deg) - Minimal Light Lavender Lilac
                InAppRadialCurvedItem(
                    icon = Icons.Default.AutoAwesome,
                    label = "Ask Paradox",
                    surfaceColor = Color(0xFFFAF5FF),
                    borderColor = Color(0xFFC4B5FD),
                    textColor = Color(0xFF581C87),
                    badgeBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7C3AED),
                    targetOffsetX = if (isDockedLeft) 56.dp else (-142).dp,
                    targetOffsetY = 76.dp,
                    progress = expansionProgress,
                    isDockedLeft = isDockedLeft,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = false
                        onAskParadox()
                    }
                )
            }

            // Central Orb / Ball
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .alpha(ballAlpha)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(
                        1.5.dp,
                        Color(0xFFCBD5E1),
                        CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = !isExpanded
                        isIdle = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.paradox_logo),
                    contentDescription = "Paradox Quick Ball",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )

                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f * expansionProgress)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppRadialCurvedItem(
    icon: ImageVector,
    label: String,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    badgeBg: Color,
    iconTint: Color,
    targetOffsetX: androidx.compose.ui.unit.Dp,
    targetOffsetY: androidx.compose.ui.unit.Dp,
    progress: Float,
    isDockedLeft: Boolean,
    onClick: () -> Unit
) {
    val currentOffsetX = targetOffsetX * progress
    val currentOffsetY = targetOffsetY * progress
    val scale = (0.3f + 0.7f * progress).coerceIn(0f, 1f)
    val alpha = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .offset(x = currentOffsetX, y = currentOffsetY)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isDockedLeft) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeBg)
                        .border(1.dp, borderColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (isDockedLeft) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
