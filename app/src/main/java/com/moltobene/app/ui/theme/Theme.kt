package com.moltobene.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.moltobene.app.data.Palette

/**
 * Markenfarben aus dem Logo; nur für den Schriftzug MOLTOBENE (`ui/components/AppTitle`). Sie bleiben in jeder
 * Farbwelt gleich, denn sie gehören zum Logo.
 */
@Immutable
data class BrandColors(val blue: Color)

private val LocalBrandColors = staticCompositionLocalOf { BrandColors(BrandBlueLight) }

/** Zugriff auf die Markenfarben, passend zu Hell- und Dunkelmodus – nie feste Werte im Code. */
object MoltobeneTheme {
    val brand: BrandColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandColors.current
}

/**
 * @param darkTheme dunkel oder hell, wie in den Einstellungen gewählt („Wie das Handy“ folgt dem Handy)
 * @param palette Farbwelt aus den Einstellungen; Standard ist „Schiefer“
 */
@Composable
fun MoltobeneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: Palette = Palette.SLATE,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBrandColors provides BrandColors(if (darkTheme) BrandBlueDark else BrandBlueLight)) {
        MaterialTheme(
            colorScheme = colorSchemeOf(palette, darkTheme),
            typography = MoltobeneTypography,
            shapes = MoltobeneShapes,
            content = content
        )
    }
}
