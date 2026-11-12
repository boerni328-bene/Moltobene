package com.moltobene.app.data.dough

import com.moltobene.app.data.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zubereitung mit Mengen, Gehzeiten und Temperaturen (Wunsch vom 05.11.2026). */
class DoughStepsTest {

    private val biga = LeavenedInput(DoughType.PIZZA, 4, 250.0, 60.0, 2.8, 0.0, YeastKind.FRESH, 0.2, Preferment.BIGA, 30.0, 50.0)

    private fun steps(
        input: LeavenedInput,
        process: DoughProcess = DoughProcessRules.defaults(input.type, input.preferment),
        kitchen: DoughKitchen = DoughKitchen(),
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    ): List<String> {
        val result = DoughCalculator.calculate(input) as DoughResult.Success
        val recipe = DoughDraft.from(result, input, GermanTexts, "de", process = process, kitchen = kitchen, temperatureUnit = unit)
        assertEquals(recipe.timeline.size, recipe.steps.size)
        return recipe.steps
    }

    @Test
    fun jederSchrittNenntMengenUndTemperaturen() {
        val list = steps(biga)
        assertEquals("PREFERMENT_MIX:biga (5-15 min): mischen: 184 g Mehl, 92 g Wasser und 1,2 g Frischhefe", list[0])
        assertEquals("PREFERMENT_RIPEN:biga (1440-1440 min, fridge 5 °C): reifen biga", list[1])
        // Kneten: der Vorteig mit Gewicht, dann Mehl und Wasser mit Temperatur; das Salz nach 5 Minuten, Ziel 24 °C.
        assertEquals("KNEAD:biga (15-20 min): 277 g biga | mit 429 g Mehl und 276 g Wasser (30 °C) | Salz 17 g Salz | Ziel 24 °C", list[2])
        assertTrue(list[3].startsWith("BULK:biga (60-60 min, room 22 °C)"))
        assertEquals("SHAPE:biga (10-20 min): 4 × 250 g", list[4])
        assertEquals("PROOF:biga (240-240 min, room 22 °C): pizza room 250 °C", list[5])
        assertEquals("BAKE:biga (8-15 min, oven 250 °C): backen pizza", list[6])
    }

    @Test
    fun warmeKuecheUndKalteTeigkugeln() {
        val process = DoughProcessRules.withPlace(DoughProcessRules.defaults(DoughType.PIZZA, Preferment.BIGA), DoughType.PIZZA, Preferment.BIGA, RestPlace.FRIDGE)
        val list = steps(biga, process, DoughKitchen(room = 26.0))
        // Teig geht bei 26 °C kürzer; die Teigkugeln stehen 24 Std. im Kühlschrank und wärmen danach auf.
        assertTrue(list.toString(), list.any { it.startsWith("BULK:biga (45-45 min, room 26 °C)") })
        assertTrue(list.toString(), list.any { it.startsWith("PROOF:biga (1440-1440 min, fridge 5 °C)") })
        assertTrue(list.toString(), list.any { it.startsWith("WARM_UP:biga (90-90 min, room 26 °C): aufwärmen 250 °C") })
    }

    @Test
    fun ohneVorteigUndInFahrenheit() {
        val list = steps(biga.copy(preferment = Preferment.NONE), unit = TemperatureUnit.FAHRENHEIT)
        // Wasser mit Raumtemperatur-Mehl: etwa 20 °C = 68 °F; Ziel 24 °C = 75 °F, Backofen 250 °C = 482 °F.
        assertTrue(list[0], list[0].startsWith("KNEAD:none (15-20 min): mit 613 g Mehl, 368 g Wasser (69 °F) und 1,2 g Frischhefe | Salz 17 g Salz | Ziel 75 °F"))
        assertTrue(list.last(), list.last().contains("oven 482 °F"))
    }
}
