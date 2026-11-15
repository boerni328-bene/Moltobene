package com.moltobene.app.data.dough

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Hefe-Automatik (Entwurf 8, Wahl des Projektinhabers vom 06.11.2026, Version 0.75.0): Die Hefe richtet sich nach den
 * Gehzeiten und Temperaturen. Richtwerte (Quellen weichen bis Faktor 3 voneinander ab), so geeicht, dass die bisherigen
 * Standardwerte (0,2 % Frischhefe bei Pizza) herauskommen. Reines Kotlin, per Unit-Test prüfbar.
 *
 * - Gärfaktor: Bei 24 °C zählt eine Stunde als eine Stunde, alle 8 °C wärmer doppelt, kälter halb so viel.
 * - Ohne Vorteig: Frischhefe in % vom Mehl = 1,6 / Gehzeit (Teig, Teigkugeln bzw. Laib, Aufwärmen) bei 24 °C.
 * - Biga: 3,08 × Anteil des Mehls im Vorteig / Reifezeit bei 24 °C; Poolish ebenso mit 6,73.
 * - Sauerteig: keine Automatik, die Hefe bleibt von Hand.
 */
object DoughYeast {

    /** Temperatur, bei der eine Stunde Gehzeit als eine Stunde zählt. */
    const val REFERENCE_TEMP = 24.0

    const val FACTOR_NONE = 1.6
    const val FACTOR_BIGA = 3.08
    const val FACTOR_POOLISH = 6.73

    /** Gibt es die Automatik bei diesem Vorteig? Beim Sauerteig nicht. */
    fun applies(preferment: Preferment): Boolean = preferment != Preferment.SOURDOUGH

    /** Wie viel eine Stunde bei [temp] °C zählt, verglichen mit 24 °C (z. B. 0,84 bei 22 °C, 0,19 bei 5 °C). */
    fun factor(temp: Double): Double = 2.0.pow((temp - REFERENCE_TEMP) / DoughProcessRules.DOUBLING_DEGREES)

    /** Die Gehzeiten, die für die Hefe zählen, umgerechnet auf Stunden bei 24 °C; [effective] aus [DoughProcessRules.effective]. */
    fun hoursAt24(effective: DoughProcess, kitchen: DoughKitchen, type: DoughType, preferment: Preferment): Double {
        val times = if (preferment.carriesYeast) listOf(RestTime.RIPEN) else listOf(RestTime.BULK, RestTime.PROOF, RestTime.WARM)
        return times
            .filter { DoughProcessRules.applies(it, type, preferment, effective.proofPlace) }
            .sumOf { time ->
                val place = DoughProcessRules.place(time, preferment, effective.proofPlace)
                effective.hours(time) * factor(if (place == RestPlace.FRIDGE) kitchen.fridge else kitchen.room)
            }
    }

    /**
     * Die Hefe in % vom gesamten Mehl für die Hefeart [yeastKind], auf 0,01 gerundet und in den Grenzen des Felds;
     * `null`, wenn es keine Automatik gibt (Sauerteig) oder die Eingaben nicht reichen (z. B. Anteil des Vorteigs leer).
     */
    fun auto(
        type: DoughType,
        preferment: Preferment,
        prefermentFlour: Double,
        yeastKind: YeastKind,
        effective: DoughProcess,
        kitchen: DoughKitchen,
    ): Double? {
        if (!applies(preferment)) return null
        val hours = hoursAt24(effective, kitchen, type, preferment)
        if (!(hours > 0)) return null
        val fresh = when (preferment) {
            Preferment.NONE -> FACTOR_NONE / hours
            Preferment.BIGA, Preferment.POOLISH -> {
                if (!prefermentFlour.isFinite() || prefermentFlour <= 0) return null
                val factor = if (preferment == Preferment.BIGA) FACTOR_BIGA else FACTOR_POOLISH
                factor * (prefermentFlour / 100.0) / hours
            }
            Preferment.SOURDOUGH -> return null
        }
        val range = DoughLimits.range(type, DoughField.YEAST, preferment)
        val rounded = (YeastKind.FRESH.convert(fresh, yeastKind) * 100.0).roundToLong() / 100.0
        return rounded.coerceIn(range.min, range.max)
    }
}
