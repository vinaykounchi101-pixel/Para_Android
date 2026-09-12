package com.paradox.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wraps any AI-powered component.
 * When [isAiEnabled] is true, renders [content] normally.
 * When [isAiEnabled] is false, keeps [content] in its exact layout position but applies
 * a frosted/blurred and semi-transparent disabled style, with an overlay lock badge/card
 * prompting the user to enable AI in Settings.
 */
@Composable
fun AiFeatureDisabledContainer(
    isAiEnabled: Boolean,
    modifier: Modifier = Modifier,
    featureName: String = "AI Feature",
    blurRadius: Dp = 6.dp,
    disabledAlpha: Float = 0.35f,
    compact: Boolean = false,
    onNavigateToSettings: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (isAiEnabled) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            // Blurred, non-interactive background content in exact layout position
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(blurRadius)
                    .alpha(disabledAlpha)
            ) {
                content()
            }

            // Transparent overlay that intercepts touches and displays the Lock Pill/Badge
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigateToSettings?.invoke() }
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (compact) {
                    // Sleek, compact badge overlay for small tiles/pills
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        ),
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable { onNavigateToSettings?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "AI Feature Disabled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AI Disabled • Tap to Enable",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Full-featured glassmorphic lock card for larger sections / screens
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Disabled",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "$featureName Disabled",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Turn on AI Features in Settings to unlock grounded calculations and insights.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (onNavigateToSettings != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Button(
                                    onClick = onNavigateToSettings,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Enable in Settings",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
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
