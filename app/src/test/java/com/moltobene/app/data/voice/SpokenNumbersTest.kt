package com.moltobene.app.data.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenNumbersTest {

    private fun convert(text: String) = SpokenNumbers.convert(text)

    @Test
    fun `zusammengeschriebene und getrennte Zahlwoerter`() {
        assertEquals("250 gramm mehl", convert("zweihundertfünfzig gramm mehl"))
        assertEquals("250 gramm mehl", convert("zweihundert fünfzig gramm mehl"))
        assertEquals("23 minuten", convert("dreiundzwanzig minuten"))
        assertEquals("1200 gramm", convert("eintausendzweihundert gramm"))
        assertEquals("1200 gramm", convert("tausend zweihundert gramm"))
        assertEquals("100 gramm", convert("hundert gramm"))
        assertEquals("100 gramm", convert("einhundert gramm"))
        assertEquals("12 eier", convert("zwölf eier"))
        assertEquals("30 minuten", convert("dreißig minuten"))
        assertEquals("13 eier", convert("dreizehn eier"))
        assertEquals("99 gramm", convert("neunundneunzig gramm"))
    }

    @Test
    fun `aufeinanderfolgende Zahlen bleiben getrennt`() {
        assertEquals("2 3 zwiebeln", convert("zwei drei zwiebeln"))
        assertEquals("10 10", convert("zehn zehn"))
    }

    @Test
    fun `ein bleibt Artikel ausser vor einer Mengeneinheit`() {
        assertEquals("1 esslöffel öl", convert("ein esslöffel öl"))
        assertEquals("1 prise salz", convert("eine prise salz"))
        assertEquals("in einem topf", convert("in einem topf"))
        assertEquals("ein bisschen salz", convert("ein bisschen salz"))
        assertEquals("eine zwiebel", convert("eine zwiebel"))
    }

    @Test
    fun `Brueche`() {
        assertEquals("½ liter milch", convert("ein halber liter milch"))
        assertEquals("½ zwiebel", convert("eine halbe zwiebel"))
        assertEquals("1½ esslöffel zucker", convert("eineinhalb esslöffel zucker"))
        assertEquals("¾ liter", convert("drei viertel liter"))
        assertEquals("¼ liter", convert("ein viertel liter"))
        assertEquals("⅔ tasse", convert("zwei drittel tasse"))
    }

    @Test
    fun `Dezimalzahlen mit Komma`() {
        assertEquals("2,5 liter", convert("zwei komma fünf liter"))
        assertEquals("0,5 liter", convert("null komma fünf liter"))
    }

    @Test
    fun `normaler Text bleibt unveraendert`() {
        assertEquals("wir gehen weiter einkaufen", convert("wir gehen weiter einkaufen"))
        assertEquals("zwiebel in würfel schneiden", convert("zwiebel in würfel schneiden"))
        assertEquals("", convert(""))
    }
}
