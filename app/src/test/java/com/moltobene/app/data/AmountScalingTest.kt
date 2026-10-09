package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Portionen umrechnen (#56): Mengen so lesen und schreiben, wie das Rezept sie schreibt; runden nach Einheit (#65). */
class AmountScalingTest {

    private fun scale(line: String, factor: Double, language: String?) = AmountScaling.scaleText(line, factor, language)

    @Test
    fun zahlenJeNachSpracheLesen() {
        assertEquals(1000.0, AmountScaling.parseValue("1.000", "de")!!, 0.0)
        assertEquals(1000.0, AmountScaling.parseValue("1,000", "en")!!, 0.0)
        assertEquals(1.5, AmountScaling.parseValue("1,5", "de")!!, 0.0)
        assertEquals(1.5, AmountScaling.parseValue("1.5", "en")!!, 0.0)
        assertEquals(1.25, AmountScaling.parseValue("1,250", "de")!!, 0.0001)
        assertEquals(0.25, AmountScaling.parseValue("0,250", "it")!!, 0.0)
        assertEquals(1000.0, AmountScaling.parseValue("1.000", "es")!!, 0.0)
        assertEquals(1000.0, AmountScaling.parseValue("1.000", null)!!, 0.0)
        assertEquals(1.5, AmountScaling.parseValue("1 1/2", "en")!!, 0.0)
        assertEquals(1.5, AmountScaling.parseValue("1½", "de")!!, 0.0)
        assertEquals(1.5, AmountScaling.parseValue("1 ½", "fr")!!, 0.0)
        assertEquals(0.75, AmountScaling.parseValue("3/4", "es")!!, 0.0)
        assertEquals(null, AmountScaling.parseValue("1/0", "de"))
    }

    @Test
    fun deutsch() {
        assertEquals("1600 g reife Tomaten", scale("800 g reife Tomaten", 2.0, "de"))
        assertEquals("2.000 g Mehl", scale("1.000 g Mehl", 2.0, "de"))
        assertEquals("3 l Gemüsebrühe", scale("1,5 l Gemüsebrühe", 2.0, "de"))
        assertEquals("0,75 l Milch", scale("1,5 l Milch", 0.5, "de"))
        assertEquals("1½ TL Salz", scale("½ TL Salz", 3.0, "de"))
        assertEquals("4-6 Zwiebeln", scale("2-3 Zwiebeln", 2.0, "de"))
        assertEquals("2 bis 4 EL Öl", scale("1 bis 2 EL Öl", 2.0, "de"))
        assertEquals("1½ Zwiebel", scale("1 Zwiebel", 1.5, "de"))
        assertEquals("≈ 2,5 Zwiebeln", scale("1,5 Zwiebeln", 1.5, "de"))
        assertEquals("0,5 kg Kartoffeln", scale("0,250 kg Kartoffeln", 2.0, "de"))
    }

    @Test
    fun englisch() {
        assertEquals("3 cups flour", scale("1 1/2 cups flour", 2.0, "en"))
        assertEquals("500 g sugar", scale("1,000 g sugar", 0.5, "en"))
        assertEquals("2,000 g sugar", scale("1,000 g sugar", 2.0, "en"))
        assertEquals("0.75 cup milk", scale("1.5 cup milk", 0.5, "en"))
        assertEquals("¾ cup milk", scale("1/2 cup milk", 1.5, "en"))
        assertEquals("6 Tbsp (90g) tomato paste", scale("3 Tbsp (45g) tomato paste", 2.0, "en"))
        assertEquals(
            "3 large bone-in short ribs (about 1.5 lb / 0.7 kg total)",
            scale("6 large bone-in short ribs (about 3 lb / 1.4 kg total)", 0.5, "en"),
        )
        assertEquals("2 bottles (1500mL) red wine", scale("1 bottles (750mL) red wine", 2.0, "en"))
    }

    @Test
    fun italienischFranzoesischSpanisch() {
        assertEquals("3 kg di patate", scale("1,5 kg di patate", 2.0, "it"))
        assertEquals("4 c.à.s. d'huile", scale("2 c.à.s. d'huile", 2.0, "fr"))
        assertEquals("2 à 4 gousses d'ail", scale("1 à 2 gousses d'ail", 2.0, "fr"))
        assertEquals("1 taza de arroz", scale("1/2 taza de arroz", 2.0, "es"))
        assertEquals("2 o 4 dientes de ajo", scale("1 o 2 dientes de ajo", 2.0, "es"))
    }

    @Test
    fun zahlenMittenImTextBleiben() {
        assertEquals("Saft von 2 Zitronen", scale("Saft von 2 Zitronen", 2.0, "de"))
        assertEquals("zest of 2 lemons", scale("zest of 2 lemons", 3.0, "en"))
        assertEquals("2 Springformen (26 cm)", scale("1 Springformen (26 cm)", 2.0, "de"))
        assertEquals("1000 g Mehl Type 405", scale("500 g Mehl Type 405", 2.0, "de"))
        assertEquals("4 x 200 g Joghurt", scale("2 x 200 g Joghurt", 2.0, "de"))
        assertEquals("Salz, Pfeffer", scale("Salz, Pfeffer", 2.0, "de"))
        assertEquals("30 % Sahne", scale("30 % Sahne", 2.0, "de"))
        assertEquals("2 medium carrots, cut into ½-inch (1 cm) coins", scale("1 medium carrots, cut into ½-inch (1 cm) coins", 2.0, "en"))
    }

    @Test
    fun ohneUmrechnungBleibtDieZeile() {
        val parts = AmountScaling.scale("800 g Tomaten", 1.0, "de")
        assertEquals(listOf(AmountScaling.Part("800 g Tomaten", false)), parts)
    }

    @Test
    fun umgerechneteTeileSindMarkiert() {
        val parts = AmountScaling.scale("3 Tbsp (45g) tomato paste", 2.0, "en")
        assertEquals(
            listOf(
                AmountScaling.Part("6", true),
                AmountScaling.Part(" Tbsp (", false),
                AmountScaling.Part("90", true),
                AmountScaling.Part("g) tomato paste", false),
            ),
            parts,
        )
        assertTrue(parts.any { it.scaled })
        assertFalse(AmountScaling.scale("Salz", 2.0, "de").any { it.scaled })
    }

    @Test
    fun rundenNachGroesse() {
        assertEquals("≈ 335 g Mehl", scale("1000 g Mehl", 1.0 / 3, "de"))
        assertEquals("≈ 33 g Butter", scale("100 g Butter", 1.0 / 3, "de"))
        assertEquals("≈ 2,5 g Salz", scale("7 g Salz", 1.0 / 3, "de"))
        assertEquals("≈ 1.335 ml Wasser", scale("1.000 ml Wasser", 4.0 / 3, "de"))
        // Löffel, Liter und Kilo bleiben genau wie bisher.
        assertEquals("0,33 TL Salz", scale("1 TL Salz", 1.0 / 3, "de"))
        assertEquals("⅓ TL Salz", scale("1/2 TL Salz", 2.0 / 3, "de"))
        assertEquals("0,33 l Milch", scale("1 l Milch", 1.0 / 3, "de"))
        // Nie auf 0 runden.
        assertEquals("0,1 Ei", scale("1 Ei", 0.1, "de"))
    }

    /** Testgruppe „Backen“ (#65): Hefe, Päckchen, Eier, Mehl – und Zeilen, die bleiben müssen. */
    @Test
    fun backen() {
        assertEquals("42 g Frischhefe (1 Würfel)", scale("21 g Frischhefe (½ Würfel)", 2.0, "de"))
        assertEquals("21 g Frischhefe (½ Würfel)", scale("42 g Frischhefe (1 Würfel)", 0.5, "de"))
        assertEquals("≈ ¼ Würfel Hefe", scale("½ Würfel Hefe", 1.0 / 3, "de"))
        assertEquals("14 g Trockenhefe (2 Päckchen)", scale("7 g Trockenhefe (1 Päckchen)", 2.0, "de"))
        assertEquals("1½ Päckchen Backpulver", scale("1 Päckchen Backpulver", 1.5, "de"))
        assertEquals("≈ 1½ Päckchen Vanillezucker", scale("1 Päckchen Vanillezucker", 1.25, "de"))
        assertEquals("125 g Butter (½ Packung)", scale("250 g Butter (1 Packung)", 0.5, "de"))
        assertEquals("≈ 4 Eier", scale("3 Eier", 1.25, "de"))
        assertEquals("≈ 1½ Eier", scale("4 Eier", 1.0 / 3, "de"))
        assertEquals("≈ 2½-4 Eier", scale("2-3 Eier", 1.25, "de"))
        assertEquals("≈ 165 g Mehl", scale("500 g Mehl", 1.0 / 3, "de"))
        assertEquals("750 g Mehl Type 550", scale("500 g Mehl Type 550", 1.5, "de"))
        assertEquals("≈ 405 ml lauwarmes Wasser", scale("325 ml lauwarmes Wasser", 1.25, "de"))
        assertEquals("≈ 3,5 g Salz", scale("10 g Salz", 1.0 / 3, "de"))
        assertEquals("1½ packet (≈ 11 g) instant yeast", scale("1 packet (7 g) instant yeast", 1.5, "en"))
        assertEquals("4½ tsp instant yeast", scale("2 1/4 tsp instant yeast", 2.0, "en"))
        assertEquals("1½ bustina di lievito (24 g)", scale("1 bustina di lievito (16 g)", 1.5, "it"))
        assertEquals("(ca. ¼ Würfel)", scale("(ca. 1 Würfel)", 0.3, "de"))
        // Bleiben, wie sie sind:
        assertEquals("65 % Wasser", scale("65 % Wasser", 2.0, "de"))
        assertEquals("Mehl zum Bearbeiten", scale("Mehl zum Bearbeiten", 2.0, "de"))
        assertEquals("2 Springform (Ø 26 cm)", scale("1 Springform (Ø 26 cm)", 2.0, "de"))
    }

    @Test
    fun gerundeteTeileSindMarkiert() {
        assertEquals(
            listOf(AmountScaling.Part("≈ 165", scaled = true, approximate = true), AmountScaling.Part(" g Mehl", false)),
            AmountScaling.scale("500 g Mehl", 1.0 / 3, "de"),
        )
        val about = { number: String -> "etwa $number" }
        assertEquals("etwa 165 g Mehl", AmountScaling.spoken(AmountScaling.scale("500 g Mehl", 1.0 / 3, "de"), about))
        assertEquals("etwa 2½-4 Eier", AmountScaling.spoken(AmountScaling.scale("2-3 Eier", 1.25, "de"), about))
        assertEquals("1600 g Tomaten", AmountScaling.spoken(AmountScaling.scale("800 g Tomaten", 2.0, "de"), about))
    }
}
