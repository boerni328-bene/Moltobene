package com.moltobene.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.moltobene.app.data.Palette
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Farbwelt ist hell und dunkel gut lesbar (WCAG AA): Text mindestens 4,5 : 1, Rahmen von Eingabefeldern und
 * der fette Schriftzug MOLTOBENE mindestens 3 : 1.
 */
class PalettesTest {

    @Test
    fun kontrasteInAllenFarbwelten() {
        val problems = mutableListOf<String>()
        for (palette in Palette.entries) {
            for (dark in listOf(false, true)) {
                val c = colorSchemeOf(palette, dark)
                val brand = if (dark) BrandBlueDark else BrandBlueLight
                val pairs = listOf(
                    Triple("Text auf Hauptfarbe", c.onPrimary to c.primary, TEXT),
                    Triple("Text auf hellem Feld der Hauptfarbe", c.onPrimaryContainer to c.primaryContainer, TEXT),
                    Triple("Text auf Auswahl", c.onSecondaryContainer to c.secondaryContainer, TEXT),
                    Triple("Text auf „Entwurf“", c.onTertiaryContainer to c.tertiaryContainer, TEXT),
                    Triple("Text", c.onSurface to c.surface, TEXT),
                    Triple("Nebentext", c.onSurfaceVariant to c.surface, TEXT),
                    Triple("Text auf Fläche", c.onSurface to c.surfaceContainerHighest, TEXT),
                    Triple("Schaltfläche ohne Rahmen", c.primary to c.surface, TEXT),
                    Triple("Fehlermeldung", c.error to c.surface, TEXT),
                    Triple("Text auf Fehlerfeld", c.onErrorContainer to c.errorContainer, TEXT),
                    Triple("Rahmen", c.outline to c.surface, LARGE),
                    Triple("Schriftzug", brand to c.background, LARGE),
                )
                for ((name, colors, minimum) in pairs) {
                    val ratio = contrast(colors.first, colors.second)
                    if (ratio < minimum) problems += "$palette ${if (dark) "dunkel" else "hell"}: $name ${"%.2f".format(ratio)}"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun hellIstHellUndDunkelIstDunkel() {
        for (palette in Palette.entries) {
            assertTrue("$palette hell", colorSchemeOf(palette, dark = false).background.luminance() > 0.8f)
            assertTrue("$palette dunkel", colorSchemeOf(palette, dark = true).background.luminance() < 0.05f)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb).toDouble()
    }

    private companion object {
        const val TEXT = 4.5
        const val LARGE = 3.0
    }
}
