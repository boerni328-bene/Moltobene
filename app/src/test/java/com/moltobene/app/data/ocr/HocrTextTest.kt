package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prüft das Zusammensetzen aus hOCR, wie es Tesseract liefert. Die Werte der Sicherheit stammen aus
 * echten Ergebnissen: Bildschirmfoto einer Rezept-Seite mit Knöpfen über dem Text und einer Kochbuchseite
 * mit Foto des Gerichts.
 */
class HocrTextTest {

    private fun word(text: String, confidence: Int) =
        "<span class='ocrx_word' id='w' title='bbox 0 0 10 10; x_wconf $confidence'>$text</span>"

    private fun line(vararg words: String) =
        "<span class='ocr_line' id='l' title=\"bbox 0 0 10 10; baseline 0 0\">\n" + words.joinToString(" ") + "\n</span>"

    private fun paragraph(vararg lines: String) =
        "<p class='ocr_par' id='p' lang='deu' title=\"bbox 0 0 10 10\">\n" + lines.joinToString("\n") + "\n</p>"

    private fun page(vararg paragraphs: String) =
        "<div class='ocr_page' id='page_1'>\n<div class='ocr_carea' id='block_1_1'>\n" +
            paragraphs.joinToString("\n") + "\n</div>\n</div>"

    @Test
    fun zeilenUndAbsaetze() {
        val hocr = page(
            paragraph(line(word("Für", 96), word("die", 96), word("Fülle:", 96))),
            paragraph(
                line(word("4", 93), word("EL", 92), word("Öl", 95)),
                line(word("1", 96), word("Päckchen", 96), word("Vanillezucker", 96)),
            ),
        )
        assertEquals("Für die Fülle:\n\n4 EL Öl\n1 Päckchen Vanillezucker", HocrText.compose(hocr))
    }

    @Test
    fun unsichereZeilenUndSymbolresteFallenWeg() {
        val hocr = page(
            paragraph(
                line(word("3EL", 91), word("Rotwein", 96)),
                // Knöpfe (Drucker, Geschenk) über einer Überschrift
                line(word("©", 94), word("Überrasche", 79), word("\\", 79)),
                line(word("zu", 90), word("erzi,‚en:", 0), word("—_", 0), word("_", 29)),
                line(word("Butter", 96), word("oder", 96), word("A", 20)),
                // Fetzen aus dem Foto eines Gerichts
                line(word("A", 64), word("Wa", 39), word("u", 71), word("aa", 29)),
                line(word("ss", 30), word("LL", 32), word("7", 0), word(".7-mi-Ve", 0)),
                line(word("Öl", 86)),
            ),
        )
        assertEquals("3EL Rotwein\nÜberrasche\nButter oder\nÖl", HocrText.compose(hocr))
    }

    @Test
    fun sonderzeichenWerdenZurueckgewandelt() {
        val hocr = page(paragraph(line(word("6", 95), word("jaunes", 96), word("d&#39;œufs", 96), word("&amp;", 90), word("sel", 96))))
        assertEquals("6 jaunes d'œufs & sel", HocrText.compose(hocr))
    }

    @Test
    fun leeresErgebnis() {
        assertEquals("", HocrText.compose(""))
        assertEquals("", HocrText.compose(page(paragraph(line(word("~", 10))))))
    }
}
