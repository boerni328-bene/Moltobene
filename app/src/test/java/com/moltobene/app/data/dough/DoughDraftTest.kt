package com.moltobene.app.data.dough

import com.moltobene.app.data.AmountScaling
import com.moltobene.app.data.RecipeYield
import com.moltobene.app.data.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Teigrechner → Rezept (#69): Das Ergebnis muss sich umrechnen, lesen und teilen lassen. */
class DoughDraftTest {

    private object German : DoughTexts {
        override fun title(type: DoughType, preferment: Preferment) =
            if (preferment == Preferment.NONE) "Pizzateig" else "Pizzateig mit ${preferment.key}"

        override fun heading(kind: SectionKind, preferment: Preferment) =
            if (kind == SectionKind.PREFERMENT) "Für den Vorteig (${preferment.key})" else "Für den Hauptteig"

        override fun ingredient(role: LineRole, yeastKind: YeastKind) = when (role) {
            LineRole.FLOUR -> "Mehl"
            LineRole.WATER -> "Wasser"
            LineRole.SALT -> "Salz"
            LineRole.OIL -> "Öl"
            LineRole.YEAST -> "Frischhefe"
        }

        override fun servingsUnit(kind: YieldKind, pieceGrams: String) = "Teigkugeln (je $pieceGrams g)"
    }

    private val biga = LeavenedInput(
        DoughType.PIZZA, 6, 250.0, 60.0, 2.5, 0.0, YeastKind.FRESH, 0.1, Preferment.BIGA, 40.0, 45.0,
    )

    private fun draft(input: LeavenedInput, language: String) =
        DoughDraft.from(DoughCalculator.calculate(input) as DoughResult.Success, input, German, language)

    @Test
    fun zeilenMitMengeVornUndAnteilInKlammern() {
        val draft = draft(biga, "de")
        assertEquals("Pizzateig mit biga", draft.title)
        assertEquals(
            listOf(
                "Für den Vorteig (biga)",
                "369 g Mehl (40 %)",
                "166 g Wasser (18 %)",
                "0,9 g Frischhefe (0,1 %)",
                "Für den Hauptteig",
                "554 g Mehl (60 %)",
                "387 g Wasser (42 %)",
                "23 g Salz (2,5 %)",
            ),
            draft.ingredients.map { it.text },
        )
        assertTrue(draft.ingredients[0].isHeading)
        assertFalse(draft.ingredients[1].isHeading)
        assertEquals(6, draft.servings)
        assertEquals("Teigkugeln (je 250 g)", draft.servingsUnit)
    }

    @Test
    fun ohneTausenderpunktUndMitPunktImEnglischen() {
        val input = biga.copy(count = 12, preferment = Preferment.NONE)
        val english = draft(input, "en").ingredients.map { it.text }
        // 12 × 250 g sind 3 kg Teig: Mehl über 1.000 g, trotzdem „1845“ und nicht „1.779“.
        assertTrue(english.toString(), english.first().startsWith("1845 g Mehl"))
        assertTrue(english.toString(), english.any { it.contains(".") && it.contains("%)") })
        val german = draft(input, "de").ingredients.map { it.text }
        assertTrue(german.toString(), german.first().startsWith("1845 g Mehl"))
    }

    @Test
    fun entwurfIstEinRezeptMitQuelleEigenes() {
        val recipe = draft(biga, "de").toRecipe("00000000-0000-4000-8000-000000000001", now = 5L)
        assertTrue(recipe.isDraft)
        assertEquals("Pizzateig mit biga", recipe.title)
        assertEquals(SourceType.OWN, recipe.source?.type)
        assertTrue(recipe.steps.isEmpty())
        assertEquals("de", recipe.language)
    }

    @Test
    fun portionsangabeWirdVonDerLeseStelleVerstanden() {
        val draft = draft(biga, "de")
        val parsed = RecipeYield.parse("${draft.servings} ${draft.servingsUnit}")!!
        assertEquals(6, parsed.count)
        assertEquals("Teigkugeln (je 250 g)", parsed.unit)
        assertTrue(RecipeYield.hasSize(draft.servingsUnit))
    }

    /** „Umrechnen …“ rechnet jede Zeile des Entwurfs um und lässt Prozente in Klammern in Ruhe. */
    @Test
    fun umrechnenKommtMitDemEntwurfZurecht() {
        for (language in listOf("de", "en")) {
            val lines = draft(biga, language).ingredients.filter { !it.isHeading }.map { it.text }
            for (factor in listOf(1.0 / 3, 0.5, 1.5, 2.0, 3.0)) {
                for (line in lines) {
                    val original = AmountScaling.leadingAmount(line, language)!!
                    val scaled = AmountScaling.scaleText(line, factor, language)
                    val after = AmountScaling.leadingAmount(scaled.removePrefix("≈ "), language)!!
                    // Gerundet wird auf übliche Küchenwerte; die Abweichung bleibt klein.
                    assertEquals("$line × $factor → $scaled", original.value * factor, after.value, original.value * factor * 0.1 + 0.6)
                    assertTrue(scaled.endsWith("%)"))
                }
            }
        }
    }
}
