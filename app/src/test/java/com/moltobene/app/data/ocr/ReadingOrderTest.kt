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
    fun dreiSchmaleSpaltenNebenZutatenKarte() {
        // Links eine Zutaten-Karte, rechts ein großer Titel über zwei Zeilen und drei Spalten im Blocksatz, deren
        // Fugen schmaler als eine Zeilenhöhe sind (wie in manchen Kochbüchern). Die zweite Titelzeile endet schon
        // neben der Fuge zwischen der zweiten und dritten Spalte.
        val title = 40f
        val lines = listOf(
            line("Sie brauchen:", 20f, 0f, 180f),
            line("Für 4 Portionen:", 20f, 30f, 200f),
            line("450 g Schweinefilet", 20f, 54f, 220f),
            line("4 EL Olivenöl", 20f, 78f, 200f),
            line("1 Zwiebel", 20f, 102f, 160f),
            TextLine(TextBox(300f + 360f, 30f, 720f, title, 0f), "Schweinefilet mit Käsekruste"),
            TextLine(TextBox(300f + 230f, 76f, 460f, title, 0f), "im Blätterteig-"),
            TextLine(TextBox(300f + 150f, 122f, 300f, title, 0f), "mantel"),
        ) + listOf(
            listOf("So wird's gemacht:", "1. Zuerst das Fleisch", "anbraten und warm", "stellen. Dann die"),
            listOf("Zwiebel würfeln.", "2. Den Teig auslegen", "und das Fleisch", "darauf legen."),
            listOf("3. Im Ofen backen.", "Zum Schluss in", "Scheiben schneiden", "und servieren."),
        ).flatMapIndexed { column, texts ->
            // Spalten 230 breit, Fuge 14 (0,7 Zeilenhöhen); kleiner Abstand zum Titel (0,4 Zeilenhöhen).
            texts.mapIndexed { row, text -> line(text, 300f + column * 244f, 150f + row * 24f, 230f) }
        }
        assertEquals(
            listOf(
                "Schweinefilet mit Käsekruste im Blätterteigmantel",
                "Sie brauchen:\n\nFür 4 Portionen:\n450 g Schweinefilet\n4 EL Olivenöl\n1 Zwiebel",
                "So wird's gemacht:\n1. Zuerst das Fleisch\nanbraten und warm\nstellen. Dann die",
                "Zwiebel würfeln.\n2. Den Teig auslegen\nund das Fleisch\ndarauf legen.",
                "3. Im Ofen backen.\nZum Schluss in\nScheiben schneiden\nund servieren.",
            ),
            ReadingOrder.blocks(lines),
        )
    }

    @Test
    fun schmaleFugeTrenntKeineKurzenZeilen() {
        // Menge und Zutat dicht nebeneinander, die Zutaten lang: keine Spalten, sondern Zeile für Zeile.
        val lines = listOf(
            "500 g" to "Weizenmehl Type 405",
            "1 Päckchen" to "Backpulver zum Backen",
            "200 ml" to "frische Vollmilch",
            "2" to "Eier Größe M",
        )
            .flatMapIndexed { row, (amount, name) ->
                listOf(line(amount, 20f, row * 24f, 100f), line(name, 130f, row * 24f))
            }
        assertEquals(
            "500 g Weizenmehl Type 405\n1 Päckchen Backpulver zum Backen\n200 ml frische Vollmilch\n2 Eier Größe M",
            ReadingOrder.text(lines),
        )
    }

    @Test
    fun ueberschriftUeberDemTitelBleibtDavor() {
        // Steht Text über dem großen Titel (z. B. der Schluss des Rezepts von der Seite davor), kommt er zuerst.
        val lines = listOf(
            line("Mit Salat servieren.", 20f, 0f, 300f),
            TextLine(TextBox(20f + 200f, 60f, 400f, 40f, 0f), "Nudelauflauf"),
            line("Zutaten", 20f, 100f),
            line("500 g Nudeln", 20f, 124f),
        )
        assertEquals("Mit Salat servieren.", ReadingOrder.blocks(lines).first())
    }

    @Test
    fun ohneZeilenKeinText() {
        assertEquals(emptyList<String>(), ReadingOrder.blocks(emptyList()))
    }
}
