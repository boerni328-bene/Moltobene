package com.moltobene.app.data

import com.moltobene.app.data.RecipeYield.Servings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rezeptmenge lesen (#63): Portionen, Stück und Backformen in den Sprachen der Texterkennung und in Zoll. */
class RecipeYieldTest {

    private fun yield(text: String) = RecipeYield.parse(text)

    @Test
    fun portionenUndPersonen() {
        assertEquals(Servings(4, null), yield("4 Portionen"))
        assertEquals(Servings(6, null), yield("Für 6 Personen"))
        assertEquals(Servings(4, null), yield("Serves 4"))
        assertEquals(Servings(4, null), yield("4-6 servings"))
        assertEquals(Servings(4, null), yield("4 to 6 servings"))
        assertEquals(Servings(8, null), yield("Makes: 8 servings"))
        assertEquals(Servings(4, null), yield("Per 4 persone"))
        assertEquals(Servings(6, null), yield("Pour 6 personnes"))
        assertEquals(Servings(4, null), yield("Para 4 personas"))
        assertEquals(Servings(4, null), yield("Portionen: 4"))
        assertEquals(Servings(12, null), yield("12"))
    }

    @Test
    fun eigeneEinheit() {
        assertEquals(Servings(12, "Stück"), yield("12 Stück"))
        assertEquals(Servings(1, "Springform (26 cm)"), yield("1 Springform (26 cm)"))
        assertEquals(Servings(1, "Springform (Ø 26 cm)"), yield("Zutaten für eine Springform (Ø 26 cm)"))
        assertEquals(Servings(1, "26er Springform"), yield("Für eine 26er Springform"))
        assertEquals(Servings(1, "9-inch cake"), yield("One 9-inch cake"))
        assertEquals(Servings(1, "9x13-inch pan"), yield("Makes 1 9x13-inch pan"))
        assertEquals(Servings(2, "loaves"), yield("Yield: 2 loaves"))
        assertEquals(Servings(12, "Muffins"), yield("Ergibt 12 Muffins"))
        assertEquals(Servings(1, "teglia (30 x 40 cm)"), yield("Per una teglia (30 x 40 cm)"))
        assertEquals(Servings(1, "moule de 24 cm"), yield("Pour un moule de 24 cm"))
        assertEquals(Servings(1, "molde de 22 cm"), yield("Para un molde de 22 cm"))
        // Pizza und Teig: Das Gewicht je Teigkugel ist eine Größe, keine Anzahl (auch für den Teigrechner, #69).
        assertEquals(Servings(4, "Pizzen à 250 g"), yield("Teig für 4 Pizzen à 250 g"))
        assertEquals(Servings(6, "Teigkugeln (je 250 g)"), yield("6 Teigkugeln (je 250 g)"))
        assertEquals(Servings(8, "dough balls (250 g each)"), yield("8 dough balls (250 g each)"))
        assertEquals(Servings(4, "panetti da 250 g"), yield("4 panetti da 250 g"))
    }

    @Test
    fun groessenSindKeineAnzahl() {
        assertEquals(Servings(null, "Springform Ø 26 cm"), yield("Springform Ø 26 cm"))
        assertEquals(Servings(null, "Blech 30 x 40 cm"), yield("Blech 30 x 40 cm"))
        assertEquals(Servings(null, "26 cm"), yield("26 cm"))
        assertEquals(Servings(null, "26er Springform"), yield("26er Springform"))
        assertEquals(Servings(null, "9-inch cake"), yield("9-inch cake"))
        assertEquals(Servings(null, "9x13 pan"), yield("9x13 pan"))
        assertEquals(Servings(null, "9\" round pan"), yield("9\" round pan"))
        assertEquals(Servings(null, "1,5 kg"), yield("1,5 kg"))
        assertEquals(Servings(null, "Springform Ø 26 cm"), yield("für die Springform Ø 26 cm"))
    }

    @Test
    fun unklaresBleibtLeer() {
        assertNull(yield("einige"))
        assertNull(yield(""))
        assertNull(yield("Portionen"))
        assertNull(yield("nach Belieben"))
        assertNull(yield("0 Portionen"))
    }

    @Test
    fun groesseErkennen() {
        assertTrue(RecipeYield.hasSize("Springform Ø 26 cm"))
        assertTrue(RecipeYield.hasSize("Blech 30 x 40 cm"))
        assertTrue(RecipeYield.hasSize("26er Springform"))
        assertTrue(RecipeYield.hasSize("9-inch cake"))
        assertTrue(RecipeYield.hasSize("Pizzen à 250 g"))
        assertFalse(RecipeYield.hasSize("Zopf"))
        assertFalse(RecipeYield.hasSize("die Füllung"))
    }
}
