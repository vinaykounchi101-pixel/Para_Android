package com.paradox.app.core.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemePalette(
    val id: String,
    val displayName: String,
    val previewColor: Color,
    val previewSecondary: Color,
    val previewContainer: Color
) {
    SLATE("SLATE", "Ocean Slate", SlateLightPrimary, SlateLightSecondary, SlateLightPrimaryContainer),
    SAGE("SAGE", "Forest Sage", SageLightPrimary, SageLightSecondary, SageLightPrimaryContainer),
    LILAC("LILAC", "Twilight Lilac", LilacLightPrimary, LilacLightSecondary, LilacLightPrimaryContainer),
    TERRACOTTA("TERRACOTTA", "Warm Clay", TerracottaLightPrimary, TerracottaLightSecondary, TerracottaLightPrimaryContainer),
    MATCHA("MATCHA", "Matcha Tea", MatchaLightPrimary, MatchaLightSecondary, MatchaLightPrimaryContainer),
    ROSE("ROSE", "Vintage Rose", RoseLightPrimary, RoseLightSecondary, RoseLightPrimaryContainer);

    companion object {
        fun fromName(name: String?): ThemePalette {
            return entries.firstOrNull { it.id.equals(name, ignoreCase = true) } ?: SLATE
        }
    }
}
