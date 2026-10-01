package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Bereiche zuordnen (#39): Rahmen mit Bezeichnung und das Zusammensetzen des Rezepts daraus. */
class AreaFrameTest {

    @Test
    fun rahmenUeberstehenDasSpeichern() {
        val frames = listOf(
            AreaFrame(CropArea(0f, 0f, 0.5f, 1f), AreaKind.INGREDIENTS),
            AreaFrame(CropArea(0.5f, 0.1f, 1f, 0.9f), AreaKind.STEPS),
            AreaFrame(CropArea(0f, 0f, 1f, 0.1f), AreaKind.TITLE),
        )
        assertEquals(frames, AreaFrame.decodeAll(AreaFrame.encodeAll(frames)))
        // Gespeichert wird die feste englische Kennung, nie ein übersetzter Text.
        assertEquals("ingredients@0.0,0.0,0.5,1.0", frames.first().encode())
    }

    @Test
    fun aeltererBereichOhneArtGiltAlsAlles() {
        assertEquals(listOf(AreaFrame(CropArea(0.1f, 0.2f, 0.9f, 0.8f))), AreaFrame.decodeAll("0.1,0.2,0.9,0.8"))
        assertEquals(listOf(AreaFrame.WHOLE_PAGE), AreaFrame.decodeAll(""))
        assertEquals(AreaKind.ALL, AreaKind.fromKey("unbekannt"))
    }

    @Test
    fun neuerRahmenBekommtDieFehlendeArt() {
        assertEquals(AreaKind.INGREDIENTS, AreaFrame.next(listOf(AreaFrame.WHOLE_PAGE)).kind)
        val withIngredients = listOf(AreaFrame(CropArea.WHOLE_PAGE, AreaKind.INGREDIENTS))
        assertEquals(AreaKind.STEPS, AreaFrame.next(withIngredients).kind)
        val all = AreaKind.entries.map { AreaFrame(CropArea.WHOLE_PAGE, it) }
        assertEquals(AreaKind.ALL, AreaFrame.next(all).kind)
    }

    @Test
    fun umfassenderBereich() {
        assertEquals(
            CropArea(0.1f, 0.05f, 0.9f, 0.8f),
            boundsOf(listOf(CropArea(0.1f, 0.2f, 0.5f, 0.8f), CropArea(0.4f, 0.05f, 0.9f, 0.3f))),
        )
        assertEquals(CropArea.WHOLE_PAGE, boundsOf(emptyList()))
    }

    @Test
    fun bezeichneteBereicheWerdenWeissUebermalt() {
        val image = GrayImage(4, 2, ByteArray(8))
        val blanked = image.whiten(listOf(CropArea(0.5f, 0f, 1f, 0.5f)))
        assertEquals(listOf(0, 0, -1, -1, 0, 0, 0, 0), blanked.pixels.map { it.toInt() })
        // Das Original bleibt unverändert.
        assertEquals(List(8) { 0 }, image.pixels.map { it.toInt() })
        assertSame(image, image.whiten(emptyList()))
    }

    @Test
    fun bezeichneteBereicheGehenOhneRatenInIhrFeld() {
        val recipe = RecipeTextParser.parseParts(
            listOf(
                AreaKind.TITLE to "Torta di mele\ndella nonna",
                AreaKind.INGREDIENTS to "Ingredienti per 6 persone\n200 g di farina\nZucchero 100 g",
                // Ohne Bezeichnung sähe dieser lange Satz wie ein Schritt aus, die Zahl wie eine Zutat.
                AreaKind.STEPS to "Preparazione\nMescolate la farina con lo zucchero e\naggiungete le uova.\n\nCuocete per 40 minuti.",
            ),
            language = "it",
        )
        assertEquals("Torta di mele della nonna", recipe.title)
        assertEquals(6, recipe.servings)
        assertEquals(listOf("200 g di farina", "100 g Zucchero"), recipe.ingredients)
        assertEquals(listOf("Mescolate la farina con lo zucchero e aggiungete le uova.", "Cuocete per 40 minuti."), recipe.steps)
    }

    @Test
    fun alles_UndBezeichneteBereicheZusammen() {
        val recipe = RecipeTextParser.parseParts(
            listOf(
                AreaKind.ALL to "Apfelkuchen\nZutaten\n200 g Mehl",
                AreaKind.STEPS to "Den Ofen auf 180 Grad vorheizen.",
            ),
            language = "de",
        )
        assertEquals("Apfelkuchen", recipe.title)
        assertEquals(listOf("200 g Mehl"), recipe.ingredients)
        assertEquals(listOf("Den Ofen auf 180 Grad vorheizen."), recipe.steps)
        assertNull(RecipeTextParser.parseParts(emptyList()).title)
    }
}
