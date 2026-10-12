package com.moltobene.app.data.speech

import org.junit.Assert.assertEquals
import org.junit.Test

/** Vorlesen (#79): Mengen und Einheiten in Worten, je Sprache (selbst geschriebene Beispiele). */
class SpokenTextTest {

    private fun de(text: String) = SpokenText.prepare(text, "de")
    private fun en(text: String) = SpokenText.prepare(text, "en")

    @Test
    fun deutscheEinheitenWerdenAusgeschrieben() {
        assertEquals("2 Esslöffel Öl", de("2 EL Öl"))
        assertEquals("1 Teelöffel Salz", de("1 TL Salz"))
        assertEquals("250 Gramm Mehl", de("250 g Mehl"))
        assertEquals("1 Päckchen Vanillezucker", de("1 Pck. Vanillezucker"))
        assertEquals("20 Minuten backen", de("20 Min. backen"))
        assertEquals("2 Stunden gehen lassen", de("2 Std. gehen lassen"))
    }

    @Test
    fun englischeEinheitenMitEinzahlUndMehrzahl() {
        assertEquals("1 tablespoon oil", en("1 tbsp oil"))
        assertEquals("2 tablespoons oil", en("2 tbsp oil"))
        assertEquals("8 ounces flour", en("8 oz flour"))
        assertEquals("10 minutes", en("10 min"))
    }

    @Test
    fun bruecheWerdenZuWorten() {
        assertEquals("ein halb Esslöffel Salz", de("½ EL Salz"))
        assertEquals("1 und ein halb Liter Milch", de("1½ l Milch"))
        assertEquals("1 und ein halb Liter Milch", de("1 1/2 l Milch"))
        assertEquals("drei viertel Tasse", de("3/4 Tasse"))
        assertEquals("1 and a half cups", en("1½ cups"))
        assertEquals("a quarter teaspoon salt", en("¼ tsp salt"))
    }

    @Test
    fun ungefaehrWirdGesprochen() {
        assertEquals("etwa 335 Gramm Mehl", de("≈ 335 g Mehl"))
        assertEquals("about 335 grams flour", en("≈335 g flour"))
    }

    @Test
    fun tausenderzeichenUndBereiche() {
        assertEquals("1000 Gramm Mehl", de("1.000 g Mehl"))
        assertEquals("1000 grams flour", en("1,000 g flour"))
        assertEquals("1,5 Liter", de("1,5 l"))
        assertEquals("10 bis 15 Minuten", de("10–15 Minuten"))
        assertEquals("10 to 15 minutes", en("10-15 min"))
    }

    @Test
    fun temperaturen() {
        assertEquals("Backofen auf 180 Grad Celsius heizen", de("Backofen auf 180 °C heizen"))
        assertEquals("Preheat to 350 degrees Fahrenheit", en("Preheat to 350°F"))
    }

    @Test
    fun abkuerzungenBeendenKeinenSatz() {
        assertEquals(listOf("Circa 5 Minuten rühren.", "Dann abkühlen lassen."), SpokenText.sentences("ca. 5 Min. rühren. Dann abkühlen lassen.", "de").map { it.replaceFirstChar(Char::uppercase) })
        assertEquals(listOf("Zum Beispiel mit Reis."), SpokenText.sentences("z. B. mit Reis.", "de").map { it.replaceFirstChar(Char::uppercase) })
    }

    @Test
    fun satzEndeNachEinheitBleibt() {
        assertEquals(listOf("Mit 200 Gramm mischen.", "Dann ruhen lassen."), SpokenText.sentences("Mit 200 g mischen. Dann ruhen lassen.", "de"))
    }

    @Test
    fun langeSaetzeWerdenGeteilt() {
        val long = (1..30).joinToString(", ") { "Zutat Nummer $it dazugeben" }
        val parts = SpokenText.sentences("$long.", "de")
        assert(parts.size > 1)
        assert(parts.all { it.length <= 220 })
    }

    @Test
    fun andereSprachenWerdenNurGeteilt() {
        assertEquals(listOf("Mélanger 2 EL.", "Cuire."), SpokenText.sentences("Mélanger 2 EL. Cuire.", "fr"))
    }

    @Test
    fun schrittAnsage() {
        assertEquals("Schritt 3.", SpokenText.stepLabel("de", 3))
        assertEquals("Step 3.", SpokenText.stepLabel("en", 3))
        assertEquals("3.", SpokenText.stepLabel("fr", 3))
        assertEquals("etwa", SpokenText.about("de"))
    }
}
