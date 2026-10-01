package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * test/resources/ocr/tabelle.png stellt das Bildschirmfoto einer Rezept-App nach (selbst erzeugt):
 * Titel, „Für die Fülle:“, neun Zutaten mit Menge rechts und hellgrauen Trennlinien, darunter die Zubereitung.
 */
class PageLayoutTest {

    private fun loadGray(name: String): GrayImage {
        val image = requireNotNull(javaClass.classLoader?.getResource("ocr/$name")) { "Prüfbild $name fehlt" }
            .openStream().use { ImageIO.read(it) }
        // Grauwerte direkt aus den Bilddaten (getRGB würde Graustufen-PNGs umrechnen).
        val raster = image.raster
        val pixels = ByteArray(image.width * image.height)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                pixels[y * image.width + x] = raster.getSample(x, y, 0).toByte()
            }
        }
        return GrayImage(image.width, image.height, pixels)
    }

    @Test
    fun tabelleMitTrennlinienWirdInZeilenZerlegt() {
        val image = loadGray("tabelle.png")
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
