package com.moltobene.app.data.ocr

import ai.onnxruntime.OrtSession
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Texterkennung von Anfang bis Ende mit den echten Modellen: Zeilen suchen, lesen, Lesereihenfolge und Aufteilung
 * in Titel, Portionen, Zutaten und Zubereitung. Die Prüfbilder sind selbst erzeugt: selbst geschriebene Rezepte,
 * als Handyfoto nachgestellt – schräg, gewölbt, mit Schatten (test/resources/ocr/foto_*.pgm.gz; der Originaltext
 * steht jeweils in der .txt-Datei daneben).
 */
class PaddleOcrTest {

    companion object {
        private lateinit var ocr: PaddleOcr

        @BeforeClass
        @JvmStatic
        fun open() {
            ocr = TestImages.openOcr()
        }

        @AfterClass
        @JvmStatic
        fun close() {
            ocr.close()
        }

        /** So viele Wörter des Originals müssen mindestens richtig gelesen sein. */
        private const val MIN_WORDS_RIGHT = 0.95
    }

    private fun read(name: String): String =
        OrtSession.RunOptions().use { options -> ReadingOrder.text(ocr.recognize(TestImages.loadGray(name), options)) }

    /** Anteil der Wörter des Originals, die genau so gelesen wurden (unabhängig von der Reihenfolge). */
    private fun wordsRight(original: String, read: String): Double {
        fun words(text: String) = text.split(Regex("\\s+")).map { it.trim('.', ',', ';', ':', '!', '?', '(', ')', '„', '“', '"') }
            .filter { it.isNotEmpty() }
        val found = words(read).groupingBy { it }.eachCount().toMutableMap()
        val expected = words(original)
        val hits = expected.count { word ->
            val left = found[word] ?: 0
            if (left > 0) found[word] = left - 1
            left > 0
        }
        return hits.toDouble() / expected.size
    }

    private fun original(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("ocr/$name")).readText(Charsets.UTF_8)

    @Test
    fun zweispaltigeKochbuchseite() {
        val text = read("foto_zweispaltig.pgm.gz")
        assertTrue(wordsRight(original("foto_zweispaltig.txt"), text) >= MIN_WORDS_RIGHT)
        val recipe = RecipeTextParser.parse(text, TextLanguage.detect(text))
        assertEquals("Apfelstrudel nach Omas Art", recipe.title)
        assertEquals(12, recipe.servings)
        // Erst die ganze Zutaten-Spalte, dann die Zubereitung – nichts vermischt.
        assertEquals(14, recipe.ingredients.size)
        assertTrue("250 g Mehl" in recipe.ingredients)
        assertTrue("100 g Zucker" in recipe.ingredients)
        assertTrue("Für die Füllung:" in recipe.ingredients)
        assertEquals(5, recipe.steps.size)
        assertTrue(recipe.steps.first().startsWith("Mehl, Salz, Wasser und Öl"))
        assertTrue(recipe.steps.last().endsWith("bestäuben."))
    }

    @Test
    fun schwierigesFotoMitSchattenUndWoelbung() {
        val text = read("foto_schwierig.pgm.gz")
        assertTrue(wordsRight(original("foto_schwierig.txt"), text) >= MIN_WORDS_RIGHT)
        val recipe = RecipeTextParser.parse(text, TextLanguage.detect(text))
        assertEquals("Kürbissuppe mit Ingwer", recipe.title)
        assertEquals(4, recipe.servings)
        assertEquals(10, recipe.ingredients.size)
        assertTrue("800 g Hokkaido-Kürbis" in recipe.ingredients)
        assertTrue("1 Stück Ingwer (ca. 3 cm)" in recipe.ingredients)
        assertEquals(4, recipe.steps.size)
        assertTrue(recipe.steps.first().startsWith("Den Kürbis waschen"))
    }

    @Test
    fun italienischesRezeptMitAkzenten() {
        val text = read("foto_italienisch.pgm.gz")
        assertTrue(wordsRight(original("foto_italienisch.txt"), text) >= MIN_WORDS_RIGHT)
        assertEquals("it", TextLanguage.detect(text))
        val recipe = RecipeTextParser.parse(text, "it")
        assertEquals("Risotto ai funghi porcini", recipe.title)
        assertEquals(4, recipe.servings)
        assertEquals(9, recipe.ingredients.size)
        assertTrue("1 l di brodo vegetale" in recipe.ingredients)
        assertEquals(4, recipe.steps.size)
        assertTrue(recipe.steps.last().endsWith("È più buono appena fatto."))
    }

    @Test
    fun dreiSpaltenNebenZutatenKarte() {
        // Zutaten-Karte schräg links, großer Titel über zwei Zeilen, drei schmale Spalten im Blocksatz, die mitten
        // im Satz umbrechen, und ein Nährwert-Kasten (nachgestellt wie auf einer Kochbuchseite).
        val text = read("foto_dreispaltig.pgm.gz")
        assertTrue(wordsRight(original("foto_dreispaltig.txt"), text) >= MIN_WORDS_RIGHT)
        val recipe = RecipeTextParser.parse(text, TextLanguage.detect(text))
        assertEquals("Hähnchenbrust mit Kräuterkruste auf buntem Ofengemüse", recipe.title)
        assertEquals(2, recipe.servings)
        assertEquals(12, recipe.ingredients.size)
        assertEquals("2 Hähnchenbrustfilets", recipe.ingredients.first())
        assertEquals("Pfeffer", recipe.ingredients.last())
        assertEquals(5, recipe.steps.size)
        assertTrue(recipe.steps[0].startsWith("Das Gemüse waschen"))
        // Die zweite Spalte setzt den zweiten Schritt fort, die dritte den vierten.
        assertTrue(recipe.steps[1].endsWith("im restlichen Öl von beiden Seiten kurz anbraten. Dann salzen, pfeffern und auf das Gemüse legen."))
        assertTrue(recipe.steps[3].startsWith("Alles im heißen Ofen etwa 25 Minuten garen"))
        assertTrue(recipe.steps[4].startsWith("Enthält pro Portion"))
    }

    @Test
    fun tabelleMitMengenRechts() {
        // Bildschirmfoto einer Rezept-App (selbst erzeugt): Zutat links, Menge rechts, Trennlinien dazwischen.
        val recipe = RecipeTextParser.parse(read("tabelle.pgm.gz"), "de")
        assertTrue("500 g Hackfleisch gemischt" in recipe.ingredients)
        assertTrue("1 Pr Oregano getrocknet" in recipe.ingredients)
        assertTrue("2 Stk. Knoblauchzehen" in recipe.ingredients)
        assertEquals(listOf("Die Zwiebel fein hacken und in Öl andünsten."), recipe.steps)
    }

    @Test
    fun leeresBildOhneText() {
        val blank = GrayImage(600, 800, ByteArray(600 * 800) { 255.toByte() })
        assertTrue(OrtSession.RunOptions().use { ocr.recognize(blank, it) }.isEmpty())
    }
}
