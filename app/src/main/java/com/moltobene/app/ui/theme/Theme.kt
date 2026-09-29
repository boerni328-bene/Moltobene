package com.moltobene.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Eigene Palette „Schiefer“ statt der Farben vom Hintergrundbild (Vision: eigene ruhige Farbpalette).
private val LightColors = lightColorScheme(
    primary = SlatePrimaryLight,
    onPrimary = SlateOnPrimaryLight,
    primaryContainer = SlatePrimaryContainerLight,
    onPrimaryContainer = SlateOnPrimaryContainerLight,
    inversePrimary = SlatePrimaryDark,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = SlateTertiaryLight,
    onTertiary = SlateOnTertiaryLight,
    tertiaryContainer = SlateTertiaryContainerLight,
    onTertiaryContainer = SlateOnTertiaryContainerLight,
    error = SlateErrorLight,
    onError = SlateOnErrorLight,
    errorContainer = SlateErrorContainerLight,
    onErrorContainer = SlateOnErrorContainerLight,
    background = SlateBackgroundLight,
    onBackground = SlateOnBackgroundLight,
    surface = SlateBackgroundLight,
    onSurface = SlateOnBackgroundLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    surfaceTint = SlatePrimaryLight,
    outline = SlateOutlineLight,
    outlineVariant = SlateOutlineVariantLight,
    inverseSurface = SlateInverseSurfaceLight,
    inverseOnSurface = SlateInverseOnSurfaceLight,
    surfaceBright = SlateBackgroundLight,
    surfaceDim = SlateSurfaceDimLight,
    surfaceContainerLowest = SlateSurfaceContainerLowestLight,
    surfaceContainerLow = SlateSurfaceContainerLowLight,
    surfaceContainer = SlateSurfaceContainerLight,
    surfaceContainerHigh = SlateSurfaceContainerHighLight,
    surfaceContainerHighest = SlateSurfaceContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = SlatePrimaryDark,
    onPrimary = SlateOnPrimaryDark,
    primaryContainer = SlatePrimaryContainerDark,
    onPrimaryContainer = SlateOnPrimaryContainerDark,
    inversePrimary = SlatePrimaryLight,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = SlateTertiaryDark,
    onTertiary = SlateOnTertiaryDark,
    tertiaryContainer = SlateTertiaryContainerDark,
    onTertiaryContainer = SlateOnTertiaryContainerDark,
    error = SlateErrorDark,
    onError = SlateOnErrorDark,
    errorContainer = SlateErrorContainerDark,
    onErrorContainer = SlateOnErrorContainerDark,
    background = SlateBackgroundDark,
    onBackground = SlateOnBackgroundDark,
    surface = SlateBackgroundDark,
    onSurface = SlateOnBackgroundDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    surfaceTint = SlatePrimaryDark,
    outline = SlateOutlineDark,
    outlineVariant = SlateOutlineVariantDark,
    inverseSurface = SlateInverseSurfaceDark,
    inverseOnSurface = SlateInverseOnSurfaceDark,
    surfaceBright = SlateSurfaceBrightDark,
    surfaceDim = SlateBackgroundDark,
    surfaceContainerLowest = SlateSurfaceContainerLowestDark,
    surfaceContainerLow = SlateSurfaceContainerLowDark,
    surfaceContainer = SlateSurfaceContainerDark,
    surfaceContainerHigh = SlateSurfaceContainerHighDark,
    surfaceContainerHighest = SlateSurfaceContainerHighestDark,
)

@Composable
fun MoltobeneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MoltobeneTypography,
        shapes = MoltobeneShapes,
        content = content
    )
}
