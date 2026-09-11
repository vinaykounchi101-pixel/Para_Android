package com.paradox.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Interactive 3x3 Pattern Lock View for setting and verifying pattern credentials.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit,
    onPatternReset: (() -> Unit)? = null
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    val activeColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val lineColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val activeRingColor = activeColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedDots.clear()
                            val dotIndex = getDotIndexAt(offset, size.width.toFloat(), size.height.toFloat())
                            if (dotIndex != -1) {
                                selectedDots.add(dotIndex)
                            }
                            currentTouchPosition = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val offset = change.position
                            currentTouchPosition = offset
                            val dotIndex = getDotIndexAt(offset, size.width.toFloat(), size.height.toFloat())
                            if (dotIndex != -1 && !selectedDots.contains(dotIndex)) {
                                selectedDots.add(dotIndex)
                            }
                        },
                        onDragEnd = {
                            currentTouchPosition = null
                            if (selectedDots.isNotEmpty()) {
                                onPatternComplete(selectedDots.toList())
                            }
                        },
                        onDragCancel = {
                            currentTouchPosition = null
                            selectedDots.clear()
                            onPatternReset?.invoke()
                        }
                    )
                }
        ) {
            val cellWidth = size.width / 3f
            val cellHeight = size.height / 3f
            val dotRadius = 8.dp.toPx()
            val activeDotRadius = 10.dp.toPx()
            val ringRadius = 24.dp.toPx()
            val strokeWidth = 5.dp.toPx()

            // Calculate dot centers (3x3 grid: 0..8)
            val dotCenters = List(9) { index ->
                val col = index % 3
                val row = index / 3
                Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            // Draw connecting lines between selected dots
            if (selectedDots.isNotEmpty()) {
                val path = Path().apply {
                    val firstCenter = dotCenters[selectedDots.first()]
                    moveTo(firstCenter.x, firstCenter.y)
                    for (i in 1 until selectedDots.size) {
                        val center = dotCenters[selectedDots[i]]
                        lineTo(center.x, center.y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw line to current touch position if dragging
                currentTouchPosition?.let { touchPos ->
                    val lastCenter = dotCenters[selectedDots.last()]
                    drawLine(
                        color = lineColor,
                        start = lastCenter,
                        end = touchPos,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw all 9 dots
            dotCenters.forEachIndexed { index, center ->
                val isSelected = selectedDots.contains(index)
                if (isSelected) {
                    // Outer glow ring
                    drawCircle(
                        color = activeRingColor,
                        radius = ringRadius,
                        center = center
                    )
                    // Inner active dot
                    drawCircle(
                        color = activeColor,
                        radius = activeDotRadius,
                        center = center
                    )
                } else {
                    // Inactive base dot
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = center
                    )
                }
            }
        }
    }
}

private fun getDotIndexAt(offset: Offset, width: Float, height: Float): Int {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val hitRadius = cellWidth * 0.45f

    for (row in 0..2) {
        for (col in 0..2) {
            val centerX = col * cellWidth + cellWidth / 2f
            val centerY = row * cellHeight + cellHeight / 2f
            val distance = hypot(offset.x - centerX, offset.y - centerY)
            if (distance <= hitRadius) {
                return row * 3 + col
            }
        }
    }
    return -1
}
