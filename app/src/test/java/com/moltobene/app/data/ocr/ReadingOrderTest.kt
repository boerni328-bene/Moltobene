package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingOrderTest {

    /** Eine Zeile, deren linke obere Ecke bei ([left], [top]) liegt; Schrift 20 Punkte hoch. */
    private fun line(text: String, left: Float, top: Float, width: Float = text.length * 10f) =
        TextLine(TextBox(left + width / 2, top + 10f, width, 20f, 0f), text)

    @Test
    fun zweiSpaltenUnterEinerUeberschrift() {
        val lines = listOf(
            line("Apfelkuchen vom Blech", 20f, 0f, 400f),
            line("Zutaten", 20f, 26f),
            line("Zubereitung", 300f, 26f),
            line("500 g Mehl", 20f, 50f),
            line("Mehl und Zucker mischen,", 300f, 50f, 260f),
            line("200 g Zucker", 20f, 74f),
            line("dann die Butter dazu.", 300f, 74f, 260f),
            line("4 Äpfel", 20f, 98f),
            line("Äpfel darauf verteilen.", 300f, 110f, 260f),
        )
        // Erst die ganze linke Spalte, dann die rechte; der größere Abstand vor dem letzten Schritt bleibt ein Absatz.
        assertEquals(
            "Apfelkuchen vom Blech\n\n" +
                "Zutaten\n500 g Mehl\n200 g Zucker\n4 Äpfel\n\n" +
                "Zubereitung\nMehl und Zucker mischen,\ndann die Butter dazu.\n\nÄpfel darauf verteilen.",
            ReadingOrder.text(lines),
        )
    }

    @Test
    fun tabelleMitMengeRechtsBleibtZeilenweise() {
        val lines = listOf(
            line("Hackfleisch", 20f, 0f),
            line("500 q", 300f, 12f),
            line("gemischt", 20f, 24f),
            line("Zwiebel", 20f, 60f),
            line("1 Stk.", 300f, 60f),
            line("Salz", 20f, 96f),
            line("etwas", 300f, 96f),
        )
        assertEquals(listOf("500 g Hackfleisch gemischt\n1 Stk. Zwiebel\netwas Salz"), ReadingOrder.blocks(lines))
    }

    @Test
    fun schraegesFotoWirdGeradeGelesen() {
        // Alles um 5° gedreht: Die Zeilen bleiben in ihrer Reihenfolge.
        val angle = 0.087f
        val lines = listOf("Erste Zeile", "Zweite Zeile", "Dritte Zeile").mapIndexed { index, text ->
            val y = index * 24f
            TextLine(TextBox(200f - y * angle, 100f + y, 300f, 20f, angle), text)
        }
        assertEquals("Erste Zeile\nZweite Zeile\nDritte Zeile", ReadingOrder.text(lines))
    }

    @Test
    fun tabellenzeileWirdZurZutat() {
        assertEquals("500 g Hackfleisch gemischt", RowText.compose("Hackfleisch\ngemischt", "500 q"))
        assertEquals("2 Stk. Knoblauchzehen", RowText.compose("Knoblauchzehen", "2 Sik."))
        assertEquals("etwas Salz, Pfeffer", RowText.compose("Salz, Pfeffer", "etwas"))
        assertEquals("Für die Fülle:", RowText.compose("Für die Fülle:", null))
        assertEquals("Vanillezucker", RowText.compose("Vanille-\nzucker", ""))
    }

    @Test
    fun ohneZeilenKeinText() {
        assertEquals(emptyList<String>(), ReadingOrder.blocks(emptyList()))
    }
}
