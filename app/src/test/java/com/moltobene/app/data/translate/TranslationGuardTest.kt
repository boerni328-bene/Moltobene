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

    /** Back- und Teigbegriffe (#64), ins Englische amerikanisch – nur, wo das Modell sie sonst falsch übersetzt. */
    @Test
    fun backenUndTeig() {
        assertEquals("Den Teig in 4 dough balls teilen.", KitchenGlossary.apply("Den Teig in 4 Teigkugeln teilen.", "de-en"))
        assertEquals("Den Teig auf das baking sheet geben.", KitchenGlossary.apply("Den Teig auf das Blech geben.", "de-en"))
        assertEquals("Bei 220 °C conventional oven backen.", KitchenGlossary.apply("Bei 220 °C Ober-/Unterhitze backen.", "de-en"))
        assertEquals("Bei 180 °C convection oven backen.", KitchenGlossary.apply("Bei 180 °C Umluft backen.", "de-en"))
        assertEquals("Powdered sugar zum Bestäuben", KitchenGlossary.apply("Puderzucker zum Bestäuben", "de-en"))
        assertEquals("3 green onions", KitchenGlossary.apply("3 Frühlingszwiebeln", "de-en"))
        assertEquals("200 ml heavy cream", KitchenGlossary.apply("200 ml Schlagsahne", "de-en"))
        // Hefen und Päckchen übersetzt das Modell ohne Hilfe richtig.
        assertEquals("1 Päckchen Trockenhefe", KitchenGlossary.apply("1 Päckchen Trockenhefe", "de-en"))
        assertEquals("1 Würfel Frischhefe", KitchenGlossary.apply("1 Würfel Frischhefe", "de-en"))
        // Ganze Wörter: „Blechkuchen“ und „Sauerteigbrot“ bleiben; Mehltypen werden nie gleichgesetzt.
        assertEquals("Blechkuchen", KitchenGlossary.apply("Blechkuchen", "de-en"))
        assertEquals("1 Sauerteigbrot", KitchenGlossary.apply("1 Sauerteigbrot", "de-en"))
        assertEquals("500 g Weizenmehl Type 550", KitchenGlossary.apply("500 g Weizenmehl Type 550", "de-en"))

        assertEquals("Let the Vorteig rest overnight.", KitchenGlossary.apply("Let the preferment rest overnight.", "en-de"))
        assertEquals("2 1/4 tsp Trockenhefe", KitchenGlossary.apply("2 1/4 tsp instant yeast", "en-de"))
        assertEquals("Place the dough balls on a Backblech.", KitchenGlossary.apply("Place the dough balls on a sheet pan.", "en-de"))
        assertEquals("Bake in a Umluftofen at 200 °C.", KitchenGlossary.apply("Bake in a convection oven at 200 °C.", "en-de"))
        assertEquals("Knead with the Knethaken.", KitchenGlossary.apply("Knead with the dough hook.", "en-de"))
        assertEquals("1 cup Schlagsahne", KitchenGlossary.apply("1 cup heavy whipping cream", "en-de"))
        assertEquals("1 packet active dry yeast", KitchenGlossary.apply("1 packet active dry yeast", "en-de"))
        assertEquals("2 cups bread flour", KitchenGlossary.apply("2 cups bread flour", "en-de"))
    }
}
