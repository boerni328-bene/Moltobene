package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountTextTest {

    @Test
    fun einheitenWerdenGetrenntUndKorrigiert() {
        assertEquals("1 Pr Oregano", AmountText.normalize("1Pr Oregano"))
        assertEquals("3 EL Rotwein", AmountText.normalize("3EL Rotwein"))
        assertEquals("500 g Hackfleisch", AmountText.normalize("500 q Hackfleisch"))
        assertEquals("2 Stk. Knoblauchzehen", AmountText.normalize("2 Sik. Knoblauchzehen"))
        assertEquals("1,5 kg Äpfel", AmountText.normalize("1,5 kg Äpfel"))
        // Keine Einheit: unverändert
        assertEquals("4 Eier", AmountText.normalize("4 Eier"))
        assertEquals("1 Ei", AmountText.normalize("1 Ei"))
    }

    @Test
    fun mengeAmEndeKommtNachVorne() {
        assertEquals("4 EL Öl", AmountText.moveTrailingAmountToFront("Öl 4EL"))
        assertEquals("1 Pr Oregano getrocknet", AmountText.moveTrailingAmountToFront("Oregano getrocknet 1Pr"))
        assertEquals("500 g Hackfleisch", AmountText.moveTrailingAmountToFront("Hackfleisch ; 500 g"))
        assertEquals("etwas Salz, Pfeffer", AmountText.moveTrailingAmountToFront("Salz, Pfeffer etwas"))
        // Mit Komma davor ist „nach Belieben“ ein Satzteil wie in einem Rezept von einer Internetseite.
        assertEquals("Grated Parmigiano cheese, to taste", AmountText.moveTrailingAmountToFront("Grated Parmigiano cheese, to taste"))
        assertEquals("Salz, nach Belieben", AmountText.moveTrailingAmountToFront("Salz, nach Belieben"))
        assertEquals("2 Eier", AmountText.moveTrailingAmountToFront("Eier 2"))
        // Keine Mengen
        assertEquals("Mehl Type 405", AmountText.moveTrailingAmountToFront("Mehl Type 405"))
        assertEquals("200 g Mehl", AmountText.moveTrailingAmountToFront("200 g Mehl"))
        assertEquals("Den Ofen auf 180 Grad", AmountText.moveTrailingAmountToFront("Den Ofen auf 180 Grad"))
    }

    @Test
    fun mengeAmAnfang() {
        assertTrue(AmountText.startsWithAmount("etwas Salz"))
        assertTrue(AmountText.startsWithAmount("½ Bund Petersilie"))
        assertTrue(AmountText.startsWithAmount("200g Mehl"))
        assertTrue(AmountText.startsWithAmount("1,5 kg Äpfel"))
        assertFalse(AmountText.startsWithAmount("Salz"))
        // Eine Zahl allein (z. B. eine Seitenzahl) ist keine Menge.
        assertFalse(AmountText.startsWithAmount("47"))
        assertFalse(AmountText.startsWithAmount("2026"))
    }

    /** Schreibweisen aus Kochbüchern und Koch-Portalen der fünf Sprachen (#44). */
    @Test
    fun einheitenAllerSprachen() {
        assertEquals("300 gr Farina 00", AmountText.moveTrailingAmountToFront("Farina 00 300 gr"))
        assertEquals("2 Esslöffel Öl", AmountText.moveTrailingAmountToFront("Öl 2 Esslöffel"))
        assertEquals("1 Stange Lauch", AmountText.moveTrailingAmountToFront("Lauch 1 Stange"))
        assertEquals("250 Gramm Quark", AmountText.moveTrailingAmountToFront("Quark 250 Gramm"))
        assertEquals("2 tablespoons butter", AmountText.moveTrailingAmountToFront("butter 2 tablespoons"))
        assertEquals("1 tbsp. sugar", AmountText.moveTrailingAmountToFront("sugar 1 tbsp."))
        assertEquals("2 c.à.s. sucre", AmountText.moveTrailingAmountToFront("sucre 2 c. à s."))
        assertEquals("2 c.à.s. de sucre", AmountText.normalize("2 c. à s. de sucre"))
        assertEquals("1 cda. aceite", AmountText.moveTrailingAmountToFront("aceite 1 cda."))
        assertEquals("1 cdta. sal", AmountText.moveTrailingAmountToFront("sal 1 cdta."))
        assertTrue(AmountText.isAmount("300 gr"))
        assertTrue(AmountText.isAmount("1 c. à c."))
    }
}
