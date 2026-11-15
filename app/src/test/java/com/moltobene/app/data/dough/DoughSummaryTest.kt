package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kurzfassungen im Teigrechner (Entwurf 5 vom 04.11.2026): Gewicht des Vorteigs, Zeile für den Hauptteig, Hefe gesamt. */
class DoughSummaryTest {

    private fun pizza(
        preferment: Preferment,
        prefermentFlour: Double = 30.0,
        prefermentWater: Double = 50.0,
        yeast: Double = 0.2,
        type: DoughType = DoughType.PIZZA,
    ) = DoughCalculator.calculate(
        LeavenedInput(type, 4, 250.0, 60.0, 2.8, 0.0, YeastKind.FRESH, yeast, preferment, prefermentFlour, prefermentWater),
    ) as DoughResult.Success

    @Test
    fun bigaWiegtMehlWasserUndHefe() {
        val result = pizza(Preferment.BIGA)
        val flour = result.flourGrams
        // 30 % des Mehls, davon 50 % Wasser, dazu die ganze Hefe (0,2 % vom Mehl).
        assertEquals(flour * 0.30 + flour * 0.30 * 0.50 + flour * 0.002, DoughSummary.prefermentGrams(result), 1e-9)
        assertEquals(277.3, DoughSummary.prefermentGrams(result), 0.05)
    }

    @Test
    fun hauptteigMitBigaOhneHefe() {
        val result = pizza(Preferment.BIGA)
        val lines = DoughSummary.mainLines(result)
        // Bei Biga steckt die ganze Hefe im Vorteig; zum Kneten kommen Mehl, Wasser und Salz dazu.
        assertEquals(listOf(LineRole.FLOUR, LineRole.WATER, LineRole.SALT), lines.map { it.role })
        assertEquals(result.flourGrams * 0.70, lines.first { it.role == LineRole.FLOUR }.grams, 1e-9)
        assertEquals(result.flourGrams * (0.60 - 0.15), lines.first { it.role == LineRole.WATER }.grams, 1e-9)
    }

    @Test
    fun hauptteigMitSauerteigNenntDieHefeZuerst() {
        val result = pizza(Preferment.SOURDOUGH, prefermentFlour = 20.0, prefermentWater = 100.0, yeast = 0.5, type = DoughType.BREAD)
        val lines = DoughSummary.mainLines(result)
        assertEquals(LineRole.YEAST, lines.first().role)
        assertEquals(result.flourGrams * 0.005, lines.first().grams, 1e-9)
        // Der Sauerteig selbst ist nur Mehl und Wasser.
        assertEquals(result.flourGrams * 0.20 * 2, DoughSummary.prefermentGrams(result), 1e-9)
    }

    @Test
    fun ohneVorteigGibtEsKeineHauptteigZeile() {
        val result = pizza(Preferment.NONE)
        assertTrue(DoughSummary.mainLines(result).isEmpty())
        assertEquals(0.0, DoughSummary.prefermentGrams(result), 0.0)
    }

    @Test
    fun hefeGesamtGleichObImVorteigOderImHauptteig() {
        listOf(Preferment.NONE, Preferment.BIGA, Preferment.POOLISH, Preferment.SOURDOUGH).forEach { preferment ->
            val result = pizza(preferment, prefermentWater = 100.0)
            assertEquals(preferment.key, result.flourGrams * 0.002, DoughSummary.yeastGrams(result), 0.05)
        }
    }

    @Test
    fun zusaetzlicheHefeStehtZuerstImHauptteig() {
        val input = LeavenedInput(
            DoughType.PIZZA, 4, 250.0, 60.0, 2.8, 0.0, YeastKind.FRESH, 0.2, Preferment.BIGA, 30.0, 50.0, mainYeast = 0.1,
        )
        val result = DoughCalculator.calculate(input) as DoughResult.Success
        val lines = DoughSummary.mainLines(result)
        assertEquals(listOf(LineRole.YEAST, LineRole.FLOUR, LineRole.WATER, LineRole.SALT), lines.map { it.role })
        assertEquals(result.flourGrams * 0.001, lines.first().grams, 1e-9)
        assertEquals(result.flourGrams * 0.003, DoughSummary.yeastGrams(result), 0.1)
    }

    @Test
    fun hefeGesamtIstDieSummeDerGerundetenZeilen() {
        // Rückmeldung vom 15.11.2026: 1,24 g im Vorteig und 0,64 g im Hauptteig stehen als 1,2 g und 0,6 g im Rezept;
        // gesamt muss dann 1,8 g heißen, nicht 1,9 g (gerundet aus 1,88 g).
        val result = DoughResult.Success(
            listOf(
                DoughSection(SectionKind.PREFERMENT, Preferment.BIGA, listOf(DoughLine(LineRole.FLOUR, 180.0), DoughLine(LineRole.YEAST, 1.24))),
                DoughSection(SectionKind.MAIN, Preferment.BIGA, listOf(DoughLine(LineRole.YEAST, 0.64), DoughLine(LineRole.FLOUR, 420.0))),
            ),
            DoughYield(4, YieldKind.BALL, 250.0),
            1000.0,
            600.0,
        )
        assertEquals(1.8, DoughSummary.yeastGrams(result), 1e-9)
        assertEquals("1,8", DoughNumbers.grams(DoughSummary.yeastGrams(result), LineRole.YEAST, "de"))
        // Sehr kleine Mengen bleiben, wie sie sind.
        assertEquals(0.03, DoughNumbers.rounded(0.03, LineRole.YEAST), 1e-9)
        assertEquals("0,03", DoughNumbers.grams(0.03, LineRole.YEAST, "de"))
    }
}
