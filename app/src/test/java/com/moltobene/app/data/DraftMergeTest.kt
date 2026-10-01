package com.moltobene.app.data

import com.moltobene.app.data.ocr.ParsedRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftMergeTest {

    private fun parsed(
        title: String? = null,
        servings: Int? = null,
        unit: String? = null,
        ingredients: List<String> = emptyList(),
        steps: List<String> = emptyList(),
    ) = ParsedRecipe(title, servings, unit, ingredients, steps)

    @Test
    fun leereFelderWerdenAusgefuellt() {
        val draft = DraftMerge.merge(
            DraftText(),
            text = "Apfelkuchen\n12 Stück\n200 g Mehl\nBacken.",
            parsed = parsed("Apfelkuchen", 12, "Stück", listOf("200 g Mehl"), listOf("Backen.")),
            language = "de",
        )
        assertEquals(
            DraftText("Apfelkuchen", "12", "Stück", "200 g Mehl", "Backen.", "Apfelkuchen\n12 Stück\n200 g Mehl\nBacken.", "de"),
            draft,
        )
    }

    @Test
    fun vorhandeneAngabenBleiben() {
        val before = DraftText(title = "Omas Kuchen", servings = "8", servingsUnit = "", language = "de")
        val draft = DraftMerge.merge(before, "Torta di mele", parsed("Torta di mele", 6, "pezzi"), language = "it")
        assertEquals("Omas Kuchen", draft.title)
        assertEquals("8", draft.servings)
        // Die Angabe zu den Portionen war leer und wird ergänzt.
        assertEquals("pezzi", draft.servingsUnit)
        // Eine schon feststehende Sprache wird nicht überschrieben.
        assertEquals("de", draft.language)
    }

    @Test
    fun unklareSpracheWirdNichtGesetzt() {
        assertNull(DraftMerge.merge(DraftText(), "Salz", parsed(ingredients = listOf("Salz")), language = null).language)
    }

    @Test
    fun zeilenWerdenAuchErgaenztWennSieInEinerAnderenZeileVorkommen() {
        val draft = DraftMerge.merge(DraftText(ingredients = "1 TL Salz"), "Salz", parsed(ingredients = listOf("Salz")), null)
        assertEquals("1 TL Salz\nSalz", draft.ingredients)
        assertEquals("Salz", draft.originalText)
    }

    @Test
    fun dieselbeSeiteZweimalGelesenKommtNichtDoppelt() {
        val text = "Für den Teig:\n200 g Mehl\n1 Prise Salz"
        val page = parsed(ingredients = listOf("Für den Teig:", "200 g Mehl", "1 Prise Salz"), steps = listOf("Kneten."))
        val once = DraftMerge.merge(DraftText(), text, page, null)
        val twice = DraftMerge.merge(once, text, page, null)
        assertEquals(once, twice)
        // Groß-/Kleinschreibung und Leerzeichen am Rand spielen beim Vergleich keine Rolle.
        val similar = parsed(ingredients = listOf("für den teig:", " 200 g Mehl", "1 prise salz"))
        assertEquals(once.ingredients, DraftMerge.merge(once, text, similar, null).ingredients)
    }

    @Test
    fun wiederkehrendeZutatInNeuemAbschnittBleibtErhalten() {
        val first = DraftText(ingredients = "Für den Teig:\n200 g Mehl\n1 Prise Salz")
        val draft = DraftMerge.merge(first, "", parsed(ingredients = listOf("Für die Füllung:", "1 Prise Salz")), null)
        assertEquals("Für den Teig:\n200 g Mehl\n1 Prise Salz\nFür die Füllung:\n1 Prise Salz", draft.ingredients)
    }

    @Test
    fun originaltextWirdAlsAbsatzAngehaengt() {
        assertEquals("Seite 1\n\nSeite 2", DraftMerge.appendText("Seite 1", "Seite 2"))
        assertEquals("Seite 1\n\nSeite 2", DraftMerge.appendText("Seite 1\n\nSeite 2", "Seite 1"))
        assertEquals("Seite 1\n\nSeite 2", DraftMerge.appendText("Seite 1\n\nSeite 2", "Seite 2"))
        assertEquals("A\n\nB\n\nC", DraftMerge.appendText("A\n\nB\n\nC", "B"))
        // Nur ganze Absätze zählen: „Salz“ steckt zwar in „1 TL Salz“, ist aber neu.
        assertEquals("1 TL Salz\n\nSalz", DraftMerge.appendText("1 TL Salz", "Salz"))
        assertEquals("Seite 1", DraftMerge.appendText("", "Seite 1"))
        assertEquals("Seite 1", DraftMerge.appendText("Seite 1", ""))
    }

    @Test
    fun unsinnigePortionenWerdenNichtUebernommen() {
        assertEquals("", DraftMerge.merge(DraftText(), "", parsed(servings = 1500), null).servings)
    }
}
