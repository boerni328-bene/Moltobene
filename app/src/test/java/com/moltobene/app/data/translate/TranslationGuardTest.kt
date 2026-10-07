package com.moltobene.app.data.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Schutzregeln und Küchenwörterbuch für „Rezept übersetzen“ (#60); Beispiele aus der Machbarkeitsprobe. */
class TranslationGuardTest {

    @Test
    fun guteUebersetzungenGeltenLassen() {
        assertTrue(TranslationGuard.accept("200 g soft butter", "200 g Weichbutter"))
        assertTrue(TranslationGuard.accept("1,5 l Gemüsebrühe", "1.5 l vegetable broth"))
        assertTrue(TranslationGuard.accept("For the drizzle:", "Für den Guss:"))
        assertTrue(TranslationGuard.accept("Mit Essig abschmecken.", "Season with vinegar."))
    }

    @Test
    fun zahlenMuessenBleiben() {
        // Mit „fork-tender“ → „weich“ wurde aus „about 3 hours“ „Bis zu zwei Stunden“.
        assertFalse(TranslationGuard.accept("Simmer until weich, about 3 hours.", "Bis zu zwei Stunden."))
        assertFalse(TranslationGuard.accept("2 tbsp milk", "Milch"))
        assertTrue(TranslationGuard.accept("1.000 g Mehl", "1,000 g flour"))
    }

    @Test
    fun ueberschriftBleibtUeberschrift() {
        assertFalse(TranslationGuard.accept("Gremolata:", "- Ich weiß."))
        assertEquals("Gremolata:", TranslationGuard.choose("Gremolata:", "- Ich weiß."))
    }

    @Test
    fun unsinnErkennen() {
        assertFalse(TranslationGuard.accept("Simmer.", "Der der der der Topf."))
        assertFalse(TranslationGuard.accept("Bring to a boil.", "für den Bau der für den Bau der für den Bau der Kessel"))
        assertFalse(TranslationGuard.accept("Salt", "Salz " + "und Pfeffer ".repeat(10)))
        assertFalse(TranslationGuard.accept("Salz", ""))
        assertFalse(TranslationGuard.accept("200", "200"))
    }

    @Test
    fun kuechenwoerterbuch() {
        assertEquals("100 g Puderzucker", KitchenGlossary.apply("100 g icing sugar", "en-de"))
        assertEquals("Puderzucker, sifted", KitchenGlossary.apply("Icing sugar, sifted", "en-de"))
        assertEquals("Abrieb von 2 lemons", KitchenGlossary.apply("zest of 2 lemons", "en-de"))
        assertEquals("250 g brown lentils", KitchenGlossary.apply("250 g Tellerlinsen", "de-en"))
        assertEquals("1 bunch Petersilie", KitchenGlossary.apply("1 Bund Petersilie", "de-en"))
        // Nur ganze Wörter: „Bundesland“ bleibt.
        assertEquals("Bundesland", KitchenGlossary.apply("Bundesland", "de-en"))
        assertEquals("Grease a springform pan (26 cm).", KitchenGlossary.apply("Grease a springform pan (26 cm).", "fr-de"))
    }
}
