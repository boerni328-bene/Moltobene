package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.GZIPInputStream

/**
 * test/resources/ocr/tabelle.pgm.gz stellt das Bildschirmfoto einer Rezept-App nach (selbst erzeugt):
 * Titel, „Für die Fülle:“, neun Zutaten mit Menge rechts und hellgrauen Trennlinien, darunter die Zubereitung.
 */
class PageLayoutTest {

    /** Liest ein Graustufenbild im einfachen Format PGM (P5), gzip-gepackt. */
    private fun loadGray(name: String): GrayImage {
        val bytes = requireNotNull(javaClass.classLoader?.getResource("ocr/$name")) { "Prüfbild $name fehlt" }
            .openStream().use { GZIPInputStream(it).readBytes() }
        // Kopf: „P5“, Breite, Höhe, Höchstwert – durch Leerraum getrennt, danach ein Byte je Bildpunkt.
        var position = 0
        val fields = mutableListOf<String>()
        while (fields.size < 4) {
            while (bytes[position].toInt().toChar().isWhitespace()) position++
            val start = position
            while (!bytes[position].toInt().toChar().isWhitespace()) position++
            fields += String(bytes, start, position - start)
        }
        position++
        val width = fields[1].toInt()
        val height = fields[2].toInt()
        return GrayImage(width, height, bytes.copyOfRange(position, position + width * height))
    }

    @Test
    fun tabelleMitTrennlinienWirdInZeilenZerlegt() {
        val image = loadGray("tabelle.pgm.gz")
        val bands = requireNotNull(PageLayout.analyze(image)) { "Tabelle nicht erkannt" }
        val rows = bands.filterIsInstance<LayoutBand.Row>()
        assertEquals(8, rows.size)
        assertTrue(bands.first() is LayoutBand.Text)
        assertTrue(bands.last() is LayoutBand.Text)
        // In jeder Tabellenzeile steht rechts die Menge, schmal und am rechten Rand.
        rows.forEach { row ->
            val right = requireNotNull(row.right)
            assertTrue(right.left > image.width / 2)
            assertTrue(row.left.right <= right.left)
        }
        // Zweizeilige Zutaten („Hackfleisch / gemischt“) bleiben eine Zeile der Tabelle.
        assertTrue(rows.maxOf { it.left.height } > rows.minOf { it.left.height } * 3 / 2)
    }

    @Test
    fun seiteOhneTrennlinienBleibtUnveraendert() {
        val width = 400
        val height = 300
        val pixels = ByteArray(width * height) { 255.toByte() }
        // ein paar „Wörter“, aber keine langen Linien
        for (y in 100 until 120) for (x in 40 until 120) pixels[y * width + x] = 20
        for (y in 100 until 120) for (x in 140 until 200) pixels[y * width + x] = 20
        assertNull(PageLayout.analyze(GrayImage(width, height, pixels)))
    }

    @Test
    fun ausschnittWirdVergroessertUndUmrandet() {
        val image = GrayImage(10, 10, ByteArray(100) { 255.toByte() }.also { it[55] = 0 })
        val cell = PageLayout.cropScaled(image, Box(4, 4, 8, 8), targetHeight = 16, border = 2)
        assertEquals(20, cell.height)
        assertEquals(20, cell.width)
        assertEquals(255, cell.pixels[0].toInt() and 0xFF)
        // Der dunkle Bildpunkt bleibt nach dem weichen Vergrößern dunkel.
        assertTrue(cell.pixels.any { (it.toInt() and 0xFF) < 128 })
    }

    @Test
    fun tabellenzeileWirdZurZutat() {
        assertEquals("500 g Hackfleisch gemischt", RowText.compose("Hackfleisch\ngemischt", "500 q"))
        assertEquals("2 Stk. Knoblauchzehen", RowText.compose("Knoblauchzehen", "2 Sik."))
        assertEquals("etwas Salz, Pfeffer", RowText.compose("Salz, Pfeffer", "etwas"))
        assertEquals("Für die Fülle:", RowText.compose("Für die Fülle:", null))
        assertEquals("Vanillezucker", RowText.compose("Vanille-\nzucker", ""))
    }
}
