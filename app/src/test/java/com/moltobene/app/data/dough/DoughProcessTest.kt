package com.moltobene.app.data.dough

import com.moltobene.app.data.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gehzeiten und Temperaturen (Wunsch vom 05./06.11.2026, Entwurf 10C): Standardwerte, Raumtemperatur, eigene Werte. */
class DoughProcessTest {

    private val pizza = DoughType.PIZZA
    private val bread = DoughType.BREAD
    private fun kitchen(room: Double) = DoughKitchen(room = room)

    @Test
    fun standardwerteJeTeigartUndVorteig() {
        val biga = DoughProcessRules.defaults(pizza, Preferment.BIGA)
        assertEquals(24.0, biga.ripen, 0.0)
        assertEquals(1.0, biga.bulk, 0.0)
        assertEquals(4.0, biga.proof, 0.0)
        assertEquals(RestPlace.ROOM, biga.proofPlace)
        assertEquals(24.0, biga.doughTemp, 0.0)
        assertEquals(250.0, biga.ovenTemp, 0.0)
        // Pizza ohne Vorteig: Teigkugeln über Nacht im Kühlschrank, dann aufwärmen.
        val plain = DoughProcessRules.defaults(pizza, Preferment.NONE)
        assertEquals(RestPlace.FRIDGE, plain.proofPlace)
        assertEquals(24.0, plain.proof, 0.0)
        assertTrue(DoughProcessRules.applies(RestTime.WARM, pizza, Preferment.NONE, plain.proofPlace))
        assertFalse(DoughProcessRules.applies(RestTime.RIPEN, pizza, Preferment.NONE, plain.proofPlace))
        val loaf = DoughProcessRules.defaults(bread, Preferment.NONE)
        assertEquals(2.0, loaf.bulk, 0.0)
        assertEquals(1.0, loaf.proof, 0.0)
        assertEquals(230.0, loaf.ovenTemp, 0.0)
        assertFalse(DoughProcessRules.applies(RestTime.WARM, bread, Preferment.NONE, RestPlace.FRIDGE))
    }

    @Test
    fun jederStandardwertIstEineStufe() {
        for (type in listOf(pizza, bread)) {
            for (preferment in Preferment.entries) {
                val defaults = DoughProcessRules.defaults(type, preferment)
                for (place in RestPlace.entries) {
                    for (time in RestTime.entries) {
                        if (!DoughProcessRules.applies(time, type, preferment, place)) continue
                        val list = DoughProcessRules.steps(time, type, preferment, place)
                        val default = DoughProcessRules.defaultHours(time, type, preferment, place)
                        assertTrue("$type $preferment $place $time $default", list.any { it == default })
                        assertEquals(list.sorted(), list)
                    }
                }
                assertTrue(defaults.doughTemp in TemperatureField.DOUGH)
                assertTrue(defaults.ovenTemp in TemperatureField.OVEN)
            }
        }
    }

    /** Wärmer geht es schneller: etwa alle 8 °C doppelt so schnell, gerundet auf die nächste Stufe. */
    @Test
    fun gehzeitenBeiRaumtemperaturRichtenSichNachDemRaum() {
        val biga = DoughProcessRules.defaults(pizza, Preferment.BIGA)
        val warm = DoughProcessRules.effective(biga, kitchen(26.0), pizza, Preferment.BIGA)
        assertEquals(0.75, warm.bulk, 0.0)
        assertEquals(3.0, warm.proof, 0.0)
        val cool = DoughProcessRules.effective(biga, kitchen(18.0), pizza, Preferment.BIGA)
        assertEquals(1.5, cool.bulk, 0.0)
        assertEquals(6.0, cool.proof, 0.0)
        // Bei 22 °C wie eingestellt; die Biga steht im Kühlschrank und bleibt bei jeder Raumtemperatur gleich.
        assertEquals(biga, DoughProcessRules.effective(biga, kitchen(22.0), pizza, Preferment.BIGA))
        assertEquals(24.0, warm.ripen, 0.0)
        assertEquals(24.0, cool.ripen, 0.0)
        // Poolish reift bei Raumtemperatur und passt sich an.
        val poolish = DoughProcessRules.defaults(pizza, Preferment.POOLISH)
        assertEquals(8.0, DoughProcessRules.effective(poolish, kitchen(26.0), pizza, Preferment.POOLISH).ripen, 0.0)
    }

    @Test
    fun selbstGeaenderteGehzeitBleibtUndIstMarkiert() {
        val biga = DoughProcessRules.defaults(pizza, Preferment.BIGA)
        val stepped = DoughProcessRules.step(biga, kitchen(26.0), pizza, Preferment.BIGA, RestTime.PROOF, +1)
        // Von der wirksamen Stufe (3 Std. bei 26 °C) eine weiter.
        assertEquals(3.5, stepped.proof, 0.0)
        assertTrue(RestTime.PROOF in stepped.manual)
        assertEquals(3.5, DoughProcessRules.effective(stepped, kitchen(18.0), pizza, Preferment.BIGA).proof, 0.0)
        // Andere Gehzeiten passen sich weiter an.
        assertEquals(1.5, DoughProcessRules.effective(stepped, kitchen(18.0), pizza, Preferment.BIGA).bulk, 0.0)
    }

    @Test
    fun anDenEndenGehtEsNichtWeiter() {
        var process = DoughProcessRules.defaults(pizza, Preferment.BIGA)
        repeat(20) { process = DoughProcessRules.step(process, kitchen(22.0), pizza, Preferment.BIGA, RestTime.RIPEN, +1) }
        assertEquals(72.0, process.ripen, 0.0)
        assertFalse(DoughProcessRules.canStep(process, kitchen(22.0), pizza, Preferment.BIGA, RestTime.RIPEN, +1))
        assertTrue(DoughProcessRules.canStep(process, kitchen(22.0), pizza, Preferment.BIGA, RestTime.RIPEN, -1))
        repeat(20) { process = DoughProcessRules.step(process, kitchen(22.0), pizza, Preferment.BIGA, RestTime.RIPEN, -1) }
        assertEquals(8.0, process.ripen, 0.0)
    }

    @Test
    fun anderOrtUndAndererVorteigBeginnenBeimStandard() {
        val biga = DoughProcessRules.step(DoughProcessRules.defaults(pizza, Preferment.BIGA), kitchen(22.0), pizza, Preferment.BIGA, RestTime.PROOF, +1)
        val cold = DoughProcessRules.withPlace(biga, pizza, Preferment.BIGA, RestPlace.FRIDGE)
        assertEquals(RestPlace.FRIDGE, cold.proofPlace)
        assertEquals(24.0, cold.proof, 0.0)
        assertFalse(RestTime.PROOF in cold.manual)
        val other = DoughProcessRules.forPreferment(cold.copy(doughTemp = 26.0), pizza, Preferment.POOLISH)
        assertEquals(12.0, other.ripen, 0.0)
        assertEquals(RestPlace.ROOM, other.proofPlace)
        assertEquals(26.0, other.doughTemp, 0.0)
    }

    @Test
    fun gesamtzeitVomAnsetzenBisZumBacken() {
        val biga = DoughProcessRules.defaults(pizza, Preferment.BIGA)
        // 15 Min. Ansetzen, 24 Std. Biga, 20 Min. Kneten von Hand, 1 Std. Teig, 20 Min. Formen, 4 Std. Teigkugeln.
        assertEquals(29 * 60 + 55, DoughProcessRules.totalMinutes(biga, pizza, Preferment.BIGA, Kneading.HAND))
        assertEquals(29 * 60 + 50, DoughProcessRules.totalMinutes(biga, pizza, Preferment.BIGA, Kneading.MACHINE))
    }

    @Test
    fun temperaturenBleibenInIhrenGrenzen() {
        assertEquals(35.0, TemperatureField.ROOM.next(35.0, +1), 0.0)
        assertEquals(0.0, TemperatureField.FRIDGE.next(0.0, -1), 0.0)
        assertEquals(260.0, TemperatureField.OVEN.next(250.0, +1), 0.0)
        assertEquals("75 °F", TemperatureUnit.FAHRENHEIT.format(24.0))
        assertEquals("482 °F", TemperatureUnit.FAHRENHEIT.format(250.0))
        assertEquals("24 °C", TemperatureUnit.CELSIUS.format(24.0))
    }

    /** In °F geht es in runden °F-Stufen weiter: Backofen 482 °F (250 °C) → 500 °F bzw. 475 °F, Raum 72 °F → 73 °F. */
    @Test
    fun inFahrenheitAufRundeStufen() {
        val f = TemperatureUnit.FAHRENHEIT
        assertEquals(260.0, TemperatureField.OVEN.next(250.0, +1, f), 1e-9)
        val lower = TemperatureField.OVEN.next(250.0, -1, f)
        assertEquals("475 °F", f.format(lower))
        assertEquals("450 °F", f.format(TemperatureField.OVEN.next(lower, -1, f)))
        val room = TemperatureField.ROOM.next(22.0, +1, f)
        assertEquals("72 °F", f.format(room))
        assertEquals("73 °F", f.format(TemperatureField.ROOM.next(room, +1, f)))
        // Zurück in °C: vom Zwischenwert (22,2 °C) zur nächsten ganzen Stufe.
        assertEquals(23.0, TemperatureField.ROOM.next(room, +1), 1e-9)
        assertEquals(22.0, TemperatureField.ROOM.next(room, -1), 1e-9)
        assertEquals(25.0, TemperatureField.OVEN.step(f), 0.0)
        assertEquals(10.0, TemperatureField.OVEN.step(TemperatureUnit.CELSIUS), 0.0)
    }

    /** Wassertemperatur über die Wärmebilanz (Richtwerte): Biga kalt aus dem Kühlschrank, Maschine wärmt stärker. */
    @Test
    fun wassertemperaturNachWaermebilanz() {
        val input = LeavenedInput(pizza, 4, 250.0, 60.0, 2.8, 0.0, YeastKind.FRESH, 0.2, Preferment.BIGA, 30.0, 50.0)
        val result = DoughCalculator.calculate(input) as DoughResult.Success
        val hand = DoughWater.temperature(result, Preferment.BIGA, DoughKitchen(), 24.0)!!
        assertEquals(30.0, hand, 0.2)
        val machine = DoughWater.temperature(result, Preferment.BIGA, DoughKitchen(kneading = Kneading.MACHINE), 24.0)!!
        assertEquals(18.9, machine, 0.2)
        // Ohne Vorteig: alles Mehl hat Raumtemperatur.
        val plainInput = input.copy(preferment = Preferment.NONE)
        val plain = DoughWater.temperature(DoughCalculator.calculate(plainInput) as DoughResult.Success, Preferment.NONE, DoughKitchen(), 24.0)!!
        assertEquals(21.0 - 1.7 / (4.18 * 0.6), plain, 0.01)
        // Kommt beim Kneten kein Wasser mehr dazu, gibt es keine Wassertemperatur.
        val allInPoolish = input.copy(preferment = Preferment.POOLISH, prefermentFlour = 60.0, prefermentWater = 100.0)
        assertNull(DoughWater.temperature(DoughCalculator.calculate(allInPoolish) as DoughResult.Success, Preferment.POOLISH, DoughKitchen(), 24.0))
    }

    @Test
    fun merkenUndLesenAuchAlteOderKaputteWerte() {
        val process = DoughProcessRules.step(DoughProcessRules.defaults(pizza, Preferment.BIGA), kitchen(22.0), pizza, Preferment.BIGA, RestTime.RIPEN, +1)
            .copy(doughTemp = 23.0, ovenTemp = 280.0)
        assertEquals(process, DoughProcessMemory.decode(pizza, Preferment.BIGA, DoughProcessMemory.encode(process)))
        // Ohne gemerkte Werte, mit Fremdem oder mit Werten außerhalb der Stufen: Standardwerte.
        assertEquals(DoughProcessRules.defaults(pizza, Preferment.BIGA), DoughProcessMemory.decode(pizza, Preferment.BIGA, null))
        val broken = DoughProcessMemory.decode(pizza, Preferment.BIGA, "ripen=25;bulk=x;doughTemp=99;proofPlace=keller;manual=foo,bulk;;=;x")
        assertEquals(24.0, broken.ripen, 0.0)
        assertEquals(1.0, broken.bulk, 0.0)
        assertEquals(24.0, broken.doughTemp, 0.0)
        assertEquals(RestPlace.ROOM, broken.proofPlace)
        assertEquals(setOf(RestTime.BULK), broken.manual)
        val kitchen = DoughKitchen(room = 26.0, fridge = 7.0, kneading = Kneading.MACHINE)
        assertEquals(kitchen, DoughProcessMemory.decodeKitchen(DoughProcessMemory.encodeKitchen(kitchen)))
        assertEquals(DoughKitchen(), DoughProcessMemory.decodeKitchen("room=80;fridge=-5;kneading=fuss"))
    }
}
