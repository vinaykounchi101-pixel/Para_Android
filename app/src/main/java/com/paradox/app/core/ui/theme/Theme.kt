package com.paradox.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun getPastelColorScheme(palette: ThemePalette, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (palette) {
            ThemePalette.SLATE -> darkColorScheme(
                primary = SlateDarkPrimary,
                onPrimary = SlateDarkOnPrimary,
                primaryContainer = SlateDarkPrimaryContainer,
                onPrimaryContainer = SlateDarkOnPrimaryContainer,
                secondary = SlateDarkSecondary,
                onSecondary = SlateDarkOnSecondary,
                secondaryContainer = SlateDarkSecondaryContainer,
                onSecondaryContainer = SlateDarkOnSecondaryContainer,
                tertiary = SlateDarkTertiary,
                onTertiary = SlateDarkOnTertiary,
                tertiaryContainer = SlateDarkTertiaryContainer,
                onTertiaryContainer = SlateDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = SlateDarkSurface,
                onBackground = SlateDarkOnSurface,
                surface = SlateDarkSurface,
                onSurface = SlateDarkOnSurface,
                surfaceVariant = SlateDarkSurfaceContainer,
                onSurfaceVariant = SlateDarkOnSurfaceVariant,
                outline = SlateDarkOutline,
                outlineVariant = SlateDarkOutlineVariant
            )
            ThemePalette.SAGE -> darkColorScheme(
                primary = SageDarkPrimary,
                onPrimary = SageDarkOnPrimary,
                primaryContainer = SageDarkPrimaryContainer,
                onPrimaryContainer = SageDarkOnPrimaryContainer,
                secondary = SageDarkSecondary,
                onSecondary = SageDarkOnSecondary,
                secondaryContainer = SageDarkSecondaryContainer,
                onSecondaryContainer = SageDarkOnSecondaryContainer,
                tertiary = SageDarkTertiary,
                onTertiary = SageDarkOnTertiary,
                tertiaryContainer = SageDarkTertiaryContainer,
                onTertiaryContainer = SageDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = SageDarkSurface,
                onBackground = SageDarkOnSurface,
                surface = SageDarkSurface,
                onSurface = SageDarkOnSurface,
                surfaceVariant = SageDarkSurfaceContainer,
                onSurfaceVariant = SageDarkOnSurfaceVariant,
                outline = SageDarkOutline,
                outlineVariant = SageDarkOutlineVariant
            )
            ThemePalette.LILAC -> darkColorScheme(
                primary = LilacDarkPrimary,
                onPrimary = LilacDarkOnPrimary,
                primaryContainer = LilacDarkPrimaryContainer,
                onPrimaryContainer = LilacDarkOnPrimaryContainer,
                secondary = LilacDarkSecondary,
                onSecondary = LilacDarkOnSecondary,
                secondaryContainer = LilacDarkSecondaryContainer,
                onSecondaryContainer = LilacDarkOnSecondaryContainer,
                tertiary = LilacDarkTertiary,
                onTertiary = LilacDarkOnTertiary,
                tertiaryContainer = LilacDarkTertiaryContainer,
                onTertiaryContainer = LilacDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = LilacDarkSurface,
                onBackground = LilacDarkOnSurface,
                surface = LilacDarkSurface,
                onSurface = LilacDarkOnSurface,
                surfaceVariant = LilacDarkSurfaceContainer,
                onSurfaceVariant = LilacDarkOnSurfaceVariant,
                outline = LilacDarkOutline,
                outlineVariant = LilacDarkOutlineVariant
            )
            ThemePalette.TERRACOTTA -> darkColorScheme(
                primary = TerracottaDarkPrimary,
                onPrimary = TerracottaDarkOnPrimary,
                primaryContainer = TerracottaDarkPrimaryContainer,
                onPrimaryContainer = TerracottaDarkOnPrimaryContainer,
                secondary = TerracottaDarkSecondary,
                onSecondary = TerracottaDarkOnSecondary,
                secondaryContainer = TerracottaDarkSecondaryContainer,
                onSecondaryContainer = TerracottaDarkOnSecondaryContainer,
                tertiary = TerracottaDarkTertiary,
                onTertiary = TerracottaDarkOnTertiary,
                tertiaryContainer = TerracottaDarkTertiaryContainer,
                onTertiaryContainer = TerracottaDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = TerracottaDarkSurface,
                onBackground = TerracottaDarkOnSurface,
                surface = TerracottaDarkSurface,
                onSurface = TerracottaDarkOnSurface,
                surfaceVariant = TerracottaDarkSurfaceContainer,
                onSurfaceVariant = TerracottaDarkOnSurfaceVariant,
                outline = TerracottaDarkOutline,
                outlineVariant = TerracottaDarkOutlineVariant
            )
            ThemePalette.MATCHA -> darkColorScheme(
                primary = MatchaDarkPrimary,
                onPrimary = MatchaDarkOnPrimary,
                primaryContainer = MatchaDarkPrimaryContainer,
                onPrimaryContainer = MatchaDarkOnPrimaryContainer,
                secondary = MatchaDarkSecondary,
                onSecondary = MatchaDarkOnSecondary,
                secondaryContainer = MatchaDarkSecondaryContainer,
                onSecondaryContainer = MatchaDarkOnSecondaryContainer,
                tertiary = MatchaDarkTertiary,
                onTertiary = MatchaDarkOnTertiary,
                tertiaryContainer = MatchaDarkTertiaryContainer,
                onTertiaryContainer = MatchaDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = MatchaDarkSurface,
                onBackground = MatchaDarkOnSurface,
                surface = MatchaDarkSurface,
                onSurface = MatchaDarkOnSurface,
                surfaceVariant = MatchaDarkSurfaceContainer,
                onSurfaceVariant = MatchaDarkOnSurfaceVariant,
                outline = MatchaDarkOutline,
                outlineVariant = MatchaDarkOutlineVariant
            )
            ThemePalette.ROSE -> darkColorScheme(
                primary = RoseDarkPrimary,
                onPrimary = RoseDarkOnPrimary,
                primaryContainer = RoseDarkPrimaryContainer,
                onPrimaryContainer = RoseDarkOnPrimaryContainer,
                secondary = RoseDarkSecondary,
                onSecondary = RoseDarkOnSecondary,
                secondaryContainer = RoseDarkSecondaryContainer,
                onSecondaryContainer = RoseDarkOnSecondaryContainer,
                tertiary = RoseDarkTertiary,
                onTertiary = RoseDarkOnTertiary,
                tertiaryContainer = RoseDarkTertiaryContainer,
                onTertiaryContainer = RoseDarkOnTertiaryContainer,
                error = DarkError,
                onError = DarkOnError,
                errorContainer = DarkErrorContainer,
                onErrorContainer = DarkOnErrorContainer,
                background = RoseDarkSurface,
                onBackground = RoseDarkOnSurface,
                surface = RoseDarkSurface,
                onSurface = RoseDarkOnSurface,
                surfaceVariant = RoseDarkSurfaceContainer,
                onSurfaceVariant = RoseDarkOnSurfaceVariant,
                outline = RoseDarkOutline,
                outlineVariant = RoseDarkOutlineVariant
            )
        }
    } else {
        when (palette) {
            ThemePalette.SLATE -> lightColorScheme(
                primary = SlateLightPrimary,
                onPrimary = SlateLightOnPrimary,
                primaryContainer = SlateLightPrimaryContainer,
                onPrimaryContainer = SlateLightOnPrimaryContainer,
                secondary = SlateLightSecondary,
                onSecondary = SlateLightOnSecondary,
                secondaryContainer = SlateLightSecondaryContainer,
                onSecondaryContainer = SlateLightOnSecondaryContainer,
                tertiary = SlateLightTertiary,
                onTertiary = SlateLightOnTertiary,
                tertiaryContainer = SlateLightTertiaryContainer,
                onTertiaryContainer = SlateLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = SlateLightSurface,
                onBackground = SlateLightOnSurface,
                surface = SlateLightSurface,
                onSurface = SlateLightOnSurface,
                surfaceVariant = SlateLightSurfaceContainer,
                onSurfaceVariant = SlateLightOnSurfaceVariant,
                outline = SlateLightOutline,
                outlineVariant = SlateLightOutlineVariant
            )
            ThemePalette.SAGE -> lightColorScheme(
                primary = SageLightPrimary,
                onPrimary = SageLightOnPrimary,
                primaryContainer = SageLightPrimaryContainer,
                onPrimaryContainer = SageLightOnPrimaryContainer,
                secondary = SageLightSecondary,
                onSecondary = SageLightOnSecondary,
                secondaryContainer = SageLightSecondaryContainer,
                onSecondaryContainer = SageLightOnSecondaryContainer,
                tertiary = SageLightTertiary,
                onTertiary = SageLightOnTertiary,
                tertiaryContainer = SageLightTertiaryContainer,
                onTertiaryContainer = SageLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = SageLightSurface,
                onBackground = SageLightOnSurface,
                surface = SageLightSurface,
                onSurface = SageLightOnSurface,
                surfaceVariant = SageLightSurfaceContainer,
                onSurfaceVariant = SageLightOnSurfaceVariant,
                outline = SageLightOutline,
                outlineVariant = SageLightOutlineVariant
            )
            ThemePalette.LILAC -> lightColorScheme(
                primary = LilacLightPrimary,
                onPrimary = LilacLightOnPrimary,
                primaryContainer = LilacLightPrimaryContainer,
                onPrimaryContainer = LilacLightOnPrimaryContainer,
                secondary = LilacLightSecondary,
                onSecondary = LilacLightOnSecondary,
                secondaryContainer = LilacLightSecondaryContainer,
                onSecondaryContainer = LilacLightOnSecondaryContainer,
                tertiary = LilacLightTertiary,
                onTertiary = LilacLightOnTertiary,
                tertiaryContainer = LilacLightTertiaryContainer,
                onTertiaryContainer = LilacLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = LilacLightSurface,
                onBackground = LilacLightOnSurface,
                surface = LilacLightSurface,
                onSurface = LilacLightOnSurface,
                surfaceVariant = LilacLightSurfaceContainer,
                onSurfaceVariant = LilacLightOnSurfaceVariant,
                outline = LilacLightOutline,
                outlineVariant = LilacLightOutlineVariant
            )
            ThemePalette.TERRACOTTA -> lightColorScheme(
                primary = TerracottaLightPrimary,
                onPrimary = TerracottaLightOnPrimary,
                primaryContainer = TerracottaLightPrimaryContainer,
                onPrimaryContainer = TerracottaLightOnPrimaryContainer,
                secondary = TerracottaLightSecondary,
                onSecondary = TerracottaLightOnSecondary,
                secondaryContainer = TerracottaLightSecondaryContainer,
                onSecondaryContainer = TerracottaLightOnSecondaryContainer,
                tertiary = TerracottaLightTertiary,
                onTertiary = TerracottaLightOnTertiary,
                tertiaryContainer = TerracottaLightTertiaryContainer,
                onTertiaryContainer = TerracottaLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = TerracottaLightSurface,
                onBackground = TerracottaLightOnSurface,
                surface = TerracottaLightSurface,
                onSurface = TerracottaLightOnSurface,
                surfaceVariant = TerracottaLightSurfaceContainer,
                onSurfaceVariant = TerracottaLightOnSurfaceVariant,
                outline = TerracottaLightOutline,
                outlineVariant = TerracottaLightOutlineVariant
            )
            ThemePalette.MATCHA -> lightColorScheme(
                primary = MatchaLightPrimary,
                onPrimary = MatchaLightOnPrimary,
                primaryContainer = MatchaLightPrimaryContainer,
                onPrimaryContainer = MatchaLightOnPrimaryContainer,
                secondary = MatchaLightSecondary,
                onSecondary = MatchaLightOnSecondary,
                secondaryContainer = MatchaLightSecondaryContainer,
                onSecondaryContainer = MatchaLightOnSecondaryContainer,
                tertiary = MatchaLightTertiary,
                onTertiary = MatchaLightOnTertiary,
                tertiaryContainer = MatchaLightTertiaryContainer,
                onTertiaryContainer = MatchaLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = MatchaLightSurface,
                onBackground = MatchaLightOnSurface,
                surface = MatchaLightSurface,
                onSurface = MatchaLightOnSurface,
                surfaceVariant = MatchaLightSurfaceContainer,
                onSurfaceVariant = MatchaLightOnSurfaceVariant,
                outline = MatchaLightOutline,
                outlineVariant = MatchaLightOutlineVariant
            )
            ThemePalette.ROSE -> lightColorScheme(
                primary = RoseLightPrimary,
                onPrimary = RoseLightOnPrimary,
                primaryContainer = RoseLightPrimaryContainer,
                onPrimaryContainer = RoseLightOnPrimaryContainer,
                secondary = RoseLightSecondary,
                onSecondary = RoseLightOnSecondary,
                secondaryContainer = RoseLightSecondaryContainer,
                onSecondaryContainer = RoseLightOnSecondaryContainer,
                tertiary = RoseLightTertiary,
                onTertiary = RoseLightOnTertiary,
                tertiaryContainer = RoseLightTertiaryContainer,
                onTertiaryContainer = RoseLightOnTertiaryContainer,
                error = LightError,
                onError = LightOnError,
                errorContainer = LightErrorContainer,
                onErrorContainer = LightOnErrorContainer,
                background = RoseLightSurface,
                onBackground = RoseLightOnSurface,
                surface = RoseLightSurface,
                onSurface = RoseLightOnSurface,
                surfaceVariant = RoseLightSurfaceContainer,
                onSurfaceVariant = RoseLightOnSurfaceVariant,
                outline = RoseLightOutline,
                outlineVariant = RoseLightOutlineVariant
            )
        }
    }
}

data class CustomThemeColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color
)

fun getCustomColors(palette: ThemePalette, darkTheme: Boolean): CustomThemeColors {
    return if (darkTheme) {
        val (low, mid, high, highest) = when (palette) {
            ThemePalette.SLATE -> Quad(SlateDarkSurfaceContainerLow, SlateDarkSurfaceContainer, SlateDarkSurfaceContainerHigh, SlateDarkSurfaceContainerHighest)
            ThemePalette.SAGE -> Quad(SageDarkSurfaceContainerLow, SageDarkSurfaceContainer, SageDarkSurfaceContainerHigh, SageDarkSurfaceContainerHighest)
            ThemePalette.LILAC -> Quad(LilacDarkSurfaceContainerLow, LilacDarkSurfaceContainer, LilacDarkSurfaceContainerHigh, LilacDarkSurfaceContainerHighest)
            ThemePalette.TERRACOTTA -> Quad(TerracottaDarkSurfaceContainerLow, TerracottaDarkSurfaceContainer, TerracottaDarkSurfaceContainerHigh, TerracottaDarkSurfaceContainerHighest)
            ThemePalette.MATCHA -> Quad(MatchaDarkSurfaceContainerLow, MatchaDarkSurfaceContainer, MatchaDarkSurfaceContainerHigh, MatchaDarkSurfaceContainerHighest)
            ThemePalette.ROSE -> Quad(RoseDarkSurfaceContainerLow, RoseDarkSurfaceContainer, RoseDarkSurfaceContainerHigh, RoseDarkSurfaceContainerHighest)
        }
        CustomThemeColors(
            warning = DarkWarning,
            warningContainer = DarkWarningContainer,
            onWarningContainer = DarkOnWarningContainer,
            success = DarkSuccess,
            successContainer = DarkSuccessContainer,
            onSuccessContainer = DarkOnSuccessContainer,
            surfaceContainerLow = low,
            surfaceContainer = mid,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest
        )
    } else {
        val (low, mid, high, highest) = when (palette) {
            ThemePalette.SLATE -> Quad(SlateLightSurfaceContainerLow, SlateLightSurfaceContainer, SlateLightSurfaceContainerHigh, SlateLightSurfaceContainerHighest)
            ThemePalette.SAGE -> Quad(SageLightSurfaceContainerLow, SageLightSurfaceContainer, SageLightSurfaceContainerHigh, SageLightSurfaceContainerHighest)
            ThemePalette.LILAC -> Quad(LilacLightSurfaceContainerLow, LilacLightSurfaceContainer, LilacLightSurfaceContainerHigh, LilacLightSurfaceContainerHighest)
            ThemePalette.TERRACOTTA -> Quad(TerracottaLightSurfaceContainerLow, TerracottaLightSurfaceContainer, TerracottaLightSurfaceContainerHigh, TerracottaLightSurfaceContainerHighest)
            ThemePalette.MATCHA -> Quad(MatchaLightSurfaceContainerLow, MatchaLightSurfaceContainer, MatchaLightSurfaceContainerHigh, MatchaLightSurfaceContainerHighest)
            ThemePalette.ROSE -> Quad(RoseLightSurfaceContainerLow, RoseLightSurfaceContainer, RoseLightSurfaceContainerHigh, RoseLightSurfaceContainerHighest)
        }
        CustomThemeColors(
            warning = LightWarning,
            warningContainer = LightWarningContainer,
            onWarningContainer = LightOnWarningContainer,
            success = LightSuccess,
            successContainer = LightSuccessContainer,
            onSuccessContainer = LightOnSuccessContainer,
            surfaceContainerLow = low,
            surfaceContainer = mid,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

val LocalCustomColors = staticCompositionLocalOf { getCustomColors(ThemePalette.SLATE, false) }

object ParadoxTheme {
    val colors: CustomThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}

@Composable
fun ParadoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.SLATE,
    content: @Composable () -> Unit
) {
    val colorScheme = getPastelColorScheme(palette, darkTheme)
    val customColors = getCustomColors(palette, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
