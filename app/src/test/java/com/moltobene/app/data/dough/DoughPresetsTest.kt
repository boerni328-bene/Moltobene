package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Voreinstellungen des Teigrechners (#83): Werte, Durchschalten, eigene speichern, kaputte Daten. */
class DoughPresetsTest {

    private val pizza = DoughType.PIZZA
    private val bread = DoughType.BREAD

    @Test
    fun mitgelieferteRechnenUndLiegenInDenGrenzen() {
        for (type in listOf(pizza, bread)) {
            val list = DoughPresets.shipped(type)
            assertTrue("${type.key} hat Voreinstellungen", list.size >= 3)
            assertEquals("Kennungen sind einmalig", list.size, list.map { it.key }.toSet().size)
            for (preset in list) {
                val input = preset.input
                assertEquals(type, input.type)
                assertTrue("${preset.key} rechnet", DoughCalculator.calculate(input) is DoughResult.Success)
                // Jeder Wert liegt innerhalb der Grenzen; sonst würde das Laden ihn verwerfen.
                assertEquals(input, DoughMemory.decode(type, DoughMemory.encode(input)))
            }
        }
    }

    @Test
    fun keineVoreinstellungenFuerPastaUndKuchen() {
        assertTrue(DoughPresets.shipped(DoughType.PASTA).isEmpty())
        assertTrue(DoughPresets.shipped(DoughType.CAKE).isEmpty())
    }

    @Test
    fun anwendenLaesstAnzahlUndGewichtStehen() {
        val current = DoughLimits.defaults(pizza).copy(count = 7, pieceGrams = 333.0)
        val neapolitan = DoughPresets.shipped(pizza).first { it.key == "neapolitan" }.input
        val applied = DoughPresets.apply(current, neapolitan)
        assertEquals(7, applied.count)
        assertEquals(333.0, applied.pieceGrams, 0.0)
        assertEquals(60.0, applied.water, 0.0)
        assertTrue(DoughPresets.matches(applied, neapolitan))
    }

    @Test
    fun erkenntDieVoreinstellungAnDenWerten() {
        val list = DoughPresets.shipped(pizza).map { it.input }
        val neapolitan = list[0]
        assertEquals(0, DoughPresets.indexOf(neapolitan.copy(count = 9), list))
        assertEquals(-1, DoughPresets.indexOf(neapolitan.copy(water = 61.0), list))
        // Anderes Hefeart-Format oder ein Vorteig ohne Wirkung zählt nicht als gleich.
        assertFalse(DoughPresets.matches(neapolitan.copy(yeastKind = YeastKind.INSTANT_DRY), neapolitan))
        // Ohne Vorteig sind seine Mengen egal.
        assertTrue(DoughPresets.matches(neapolitan.copy(prefermentFlour = 55.0), neapolitan))
        // Mit Vorteig zählen sie.
        val poolish = DoughLimits.defaults(bread).copy(preferment = Preferment.POOLISH, prefermentFlour = 30.0, prefermentWater = 100.0)
        assertFalse(DoughPresets.matches(poolish.copy(prefermentFlour = 55.0), poolish))
        assertFalse(DoughPresets.matches(poolish.copy(water = Double.NaN), poolish))
    }

    @Test
    fun durchschaltenGehtReihumUndAusDerMitteHeraus() {
        assertEquals(1, DoughPresets.step(0, 3, +1))
        assertEquals(0, DoughPresets.step(2, 3, +1))
        assertEquals(2, DoughPresets.step(0, 3, -1))
        assertEquals(0, DoughPresets.step(-1, 3, +1))
        assertEquals(2, DoughPresets.step(-1, 3, -1))
        assertEquals(-1, DoughPresets.step(0, 0, +1))
    }

    @Test
    fun eigeneSpeichernErsetzenUndLoeschen() {
        val input = DoughLimits.defaults(pizza).copy(water = 68.0)
        var (list, result) = DoughPresets.save(emptyList(), "  Omas   Pizza ", input)
        assertEquals(DoughPresets.SaveResult.SAVED, result)
        assertEquals("Omas Pizza", list.single().name)
        assertEquals(68.0, DoughPresets.values(pizza, list.single()).water, 0.0)

        // Derselbe Name (Groß- und Kleinschreibung egal) ersetzt die Werte.
        val changed = input.copy(water = 72.0)
        val again = DoughPresets.save(list, "omas pizza", changed)
        assertEquals(DoughPresets.SaveResult.SAVED, again.second)
        assertEquals(1, again.first.size)
        assertEquals(72.0, DoughPresets.values(pizza, again.first.single()).water, 0.0)

        assertTrue(DoughPresets.delete(again.first, "Omas Pizza").isEmpty())
        assertEquals(again.first, DoughPresets.delete(again.first, "Gibt es nicht"))
    }

    @Test
    fun leereNamenUndZuvieleWerdenAbgelehnt() {
        val input = DoughLimits.defaults(pizza)
        assertEquals(DoughPresets.SaveResult.EMPTY_NAME, DoughPresets.save(emptyList(), "   \n ", input).second)
        var list = emptyList<DoughPresets.Own>()
        repeat(DoughPresets.MAX_OWN) { i -> list = DoughPresets.save(list, "Eigene $i", input).first }
        assertEquals(DoughPresets.MAX_OWN, list.size)
        val full = DoughPresets.save(list, "Noch eine", input)
        assertEquals(DoughPresets.SaveResult.TOO_MANY, full.second)
        assertEquals(list, full.first)
        // Ein vorhandener Name lässt sich auch bei voller Liste noch ändern.
        assertEquals(DoughPresets.SaveResult.SAVED, DoughPresets.save(list, "Eigene 3", input).second)
    }

    @Test
    fun langeNamenWerdenGekuerzt() {
        val name = DoughPresets.cleanName("a".repeat(100))
        assertEquals(DoughPresets.MAX_NAME_LENGTH, name.length)
        assertEquals("ab", DoughPresets.cleanName("a\u0000\nb".replace("\n", "")))
    }

    @Test
    fun hinUndZurueckAlsText() {
        val input = DoughLimits.defaults(bread).copy(water = 77.0)
        val list = DoughPresets.save(
            DoughPresets.save(emptyList(), "Zwei; Wörter=\"mit\" Zeichen", input).first, "Ünïcode 🍞", input,
        ).first
        assertEquals(list, DoughPresets.decode(DoughPresets.encode(list)))
    }

    @Test
    fun kaputtesWirdNieZurAusnahme() {
        assertTrue(DoughPresets.decode(null).isEmpty())
        assertTrue(DoughPresets.decode("").isEmpty())
        assertTrue(DoughPresets.decode("Unsinn").isEmpty())
        assertTrue(DoughPresets.decode("{\"a\":1}").isEmpty())
        assertTrue(DoughPresets.decode("[{\"name\":1}]").isEmpty())
        // Leere oder zu lange Namen und Doppelte fallen weg, kaputte Werte werden beim Laden ersetzt.
        val text = """[{"name":"","values":"water=70"},{"name":"${"x".repeat(80)}","values":""},
            |{"name":"Gut","values":"water=999;salt=abc"},{"name":"gut","values":""}]""".trimMargin()
        val list = DoughPresets.decode(text)
        assertEquals(listOf("Gut"), list.map { it.name })
        val values = DoughPresets.values(pizza, list.single())
        assertEquals(DoughLimits.defaults(pizza).water, values.water, 0.0)
        assertEquals(DoughLimits.defaults(pizza).salt, values.salt, 0.0)
    }

    @Test
    fun hoechstensZwanzigBeimLaden() {
        val entries = (1..30).joinToString(",") { """{"name":"N$it","values":""}""" }
        assertEquals(DoughPresets.MAX_OWN, DoughPresets.decode("[$entries]").size)
    }

    @Test
    fun schluesselJeTeigart() {
        assertNotEquals(DoughPresets.key(pizza), DoughPresets.key(bread))
    }
}
