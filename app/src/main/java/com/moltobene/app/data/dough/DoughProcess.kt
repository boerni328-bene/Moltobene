package com.moltobene.app.data.dough

import com.moltobene.app.data.TemperatureUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Wie geknetet wird; die Maschine erwärmt den Teig stärker als die Hände (Wassertemperatur, [DoughWater]). */
enum class Kneading(val key: String) {
    HAND("hand"),
    MACHINE("machine"),
    ;

    companion object {
        fun fromKey(key: String?): Kneading? = entries.firstOrNull { it.key == key }
    }
}

/** Wo der Teig während einer Gehzeit steht. */
enum class RestPlace(val key: String) {
    ROOM("room"),
    FRIDGE("fridge"),
    ;

    companion object {
        fun fromKey(key: String?): RestPlace? = entries.firstOrNull { it.key == key }
    }
}

/** Die einstellbaren Gehzeiten (Entwurf 10C, Wahl des Projektinhabers vom 12.11.2026). */
enum class RestTime(val key: String) {
    /** Vorteig reifen lassen (Biga im Kühlschrank, Poolish und Sauerteig bei Raumtemperatur). */
    RIPEN("ripen"),

    /** Teig gehen lassen (bei Raumtemperatur). */
    BULK("bulk"),

    /** Teigkugeln bzw. Laib gehen lassen, bei Raumtemperatur oder im Kühlschrank. */
    PROOF("proof"),

    /** Teigkugeln aufwärmen, wenn sie im Kühlschrank waren (nur Pizza). */
    WARM("warm"),
    ;

    companion object {
        fun fromKey(key: String?): RestTime? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Temperaturen mit Grenzen in °C und Schrittweite je Einheit (eigene Festlegung, Richtwerte): in °C 1 Grad, beim
 * Backofen 10 Grad; in °F 1 Grad, beim Backofen 25 Grad wie auf amerikanischen Backöfen (450, 475, 500 °F).
 */
enum class TemperatureField(val min: Double, val max: Double, val step: Double, val stepFahrenheit: Double) {
    ROOM(10.0, 35.0, 1.0, 1.0),
    FRIDGE(0.0, 12.0, 1.0, 1.0),
    DOUGH(18.0, 30.0, 1.0, 1.0),
    OVEN(150.0, 500.0, 10.0, 25.0),
    ;

    operator fun contains(value: Double): Boolean = value.isFinite() && value in min..max

    /** Schrittweite in der angezeigten Einheit, z. B. für „um 25 °F erhöhen“. */
    fun step(unit: TemperatureUnit): Double = if (unit == TemperatureUnit.FAHRENHEIT) stepFahrenheit else step

    /**
     * Ein Schritt in [direction] (+1 oder −1) in der angezeigten Einheit, nie über die Grenzen. Liegt der Wert zwischen
     * zwei Stufen (z. B. 250 °C = 482 °F), geht es zur nächsten Stufe in dieser Richtung (500 °F bzw. 475 °F).
     */
    fun next(value: Double, direction: Int, unit: TemperatureUnit = TemperatureUnit.CELSIUS): Double {
        val fahrenheit = unit == TemperatureUnit.FAHRENHEIT
        val stepSize = step(unit)
        val shown = if (fahrenheit) value * 9.0 / 5.0 + 32.0 else value
        val grid = shown / stepSize
        val near = grid.roundToLong().toDouble()
        val target = when {
            abs(grid - near) < GRID_TOLERANCE -> near + direction
            direction > 0 -> ceil(grid)
            else -> floor(grid)
        } * stepSize
        val celsius = if (fahrenheit) (target - 32.0) * 5.0 / 9.0 else target
        return celsius.coerceIn(min, max)
    }

    private companion object {
        const val GRID_TOLERANCE = 1e-6
    }
}

/** Die Küche (Wunsch vom 05.11.2026): Raum, Kühlschrank und wie geknetet wird; gilt für alle Teigarten. Temperaturen in °C. */
data class DoughKitchen(
    val room: Double = DEFAULT_ROOM,
    val fridge: Double = DEFAULT_FRIDGE,
    val kneading: Kneading = Kneading.HAND,
) {
    companion object {
        const val DEFAULT_ROOM = 22.0
        const val DEFAULT_FRIDGE = 5.0
    }
}

/**
 * Gehzeiten und Temperaturen eines Teigs (Wunsch vom 05./06.11.2026, Entwurf 10C). Gehzeiten in Stunden, Temperaturen
 * in °C. Eine Gehzeit bei Raumtemperatur gilt für 22 °C und passt sich der Raumtemperatur an, solange sie nicht selbst
 * geändert wurde ([manual], siehe [DoughProcessRules.effective]); im Kühlschrank bleibt sie, wie sie eingestellt ist.
 */
data class DoughProcess(
    /** Teig nach dem Kneten. */
    val doughTemp: Double,
    val ovenTemp: Double,
    val ripen: Double,
    val bulk: Double,
    val proof: Double,
    val warm: Double,
    val proofPlace: RestPlace,
    val manual: Set<RestTime> = emptySet(),
) {
    fun hours(time: RestTime): Double = when (time) {
        RestTime.RIPEN -> ripen
        RestTime.BULK -> bulk
        RestTime.PROOF -> proof
        RestTime.WARM -> warm
    }

    fun withHours(time: RestTime, hours: Double): DoughProcess = when (time) {
        RestTime.RIPEN -> copy(ripen = hours)
        RestTime.BULK -> copy(bulk = hours)
        RestTime.PROOF -> copy(proof = hours)
        RestTime.WARM -> copy(warm = hours)
    }
}

/**
 * Regeln für Gehzeiten und Temperaturen (Richtwerte, Recherche vom 06.11.2026: Der Teig geht etwa alle 8 °C doppelt so
 * schnell). Reines Kotlin, per Unit-Test prüfbar.
 */
object DoughProcessRules {

    /** Raumtemperatur, für die die eingestellten Gehzeiten gelten. */
    const val REFERENCE_ROOM = 22.0

    /** Etwa so viele Grad wärmer, und der Teig geht doppelt so schnell. */
    const val DOUBLING_DEGREES = 8.0

    /** Standardwerte zu Teigart und Vorteig; die Temperaturen hängen nur an der Teigart. */
    fun defaults(type: DoughType, preferment: Preferment): DoughProcess {
        val bread = type == DoughType.BREAD
        val ripen = when (preferment) {
            Preferment.BIGA -> 24.0
            Preferment.POOLISH -> 12.0
            Preferment.SOURDOUGH -> 10.0
            Preferment.NONE -> 0.0
        }
        val doughTemp = if (bread) 25.0 else 24.0
        val ovenTemp = if (bread) 230.0 else 250.0
        return when {
            bread -> DoughProcess(doughTemp, ovenTemp, ripen, bulk = 2.0, proof = 1.0, warm = 1.0, proofPlace = RestPlace.ROOM)
            preferment == Preferment.NONE -> DoughProcess(doughTemp, ovenTemp, ripen, bulk = 2.0, proof = 24.0, warm = 2.0, proofPlace = RestPlace.FRIDGE)
            else -> DoughProcess(doughTemp, ovenTemp, ripen, bulk = 1.0, proof = 4.0, warm = 2.0, proofPlace = RestPlace.ROOM)
        }
    }

    /** Standard-Gehzeit für [time]; beim Wechsel des Orts der Teigkugeln die passende zum neuen Ort. */
    fun defaultHours(time: RestTime, type: DoughType, preferment: Preferment, proofPlace: RestPlace): Double {
        if (time == RestTime.PROOF && proofPlace != defaults(type, preferment).proofPlace) {
            return when {
                proofPlace == RestPlace.FRIDGE -> if (type == DoughType.BREAD) 12.0 else 24.0
                type == DoughType.BREAD -> 1.0
                else -> 4.0
            }
        }
        return defaults(type, preferment).hours(time)
    }

    /** Die wählbaren Stufen einer Gehzeit in Stunden, aufsteigend. */
    fun steps(time: RestTime, type: DoughType, preferment: Preferment, proofPlace: RestPlace): List<Double> = when (time) {
        RestTime.RIPEN -> when (preferment) {
            Preferment.BIGA -> listOf(8.0, 12.0, 16.0, 18.0, 20.0, 24.0, 30.0, 36.0, 42.0, 48.0, 60.0, 72.0)
            Preferment.POOLISH -> listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0)
            Preferment.SOURDOUGH -> listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0)
            Preferment.NONE -> emptyList()
        }
        RestTime.BULK -> listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0)
        RestTime.PROOF -> when {
            proofPlace == RestPlace.FRIDGE -> listOf(6.0, 8.0, 10.0, 12.0, 16.0, 20.0, 24.0, 36.0, 48.0, 72.0)
            type == DoughType.BREAD -> listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0)
            else -> listOf(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 5.0, 6.0, 8.0)
        }
        RestTime.WARM -> listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)
    }

    /** Gibt es diese Gehzeit beim Teig? Vorteig nur mit Vorteig, Aufwärmen nur bei Pizza aus dem Kühlschrank. */
    fun applies(time: RestTime, type: DoughType, preferment: Preferment, proofPlace: RestPlace): Boolean = when (time) {
        RestTime.RIPEN -> preferment != Preferment.NONE
        RestTime.BULK, RestTime.PROOF -> true
        RestTime.WARM -> type == DoughType.PIZZA && proofPlace == RestPlace.FRIDGE
    }

    /** Wo die Gehzeit stattfindet: Biga im Kühlschrank, Poolish und Sauerteig im Raum, Teigkugeln nach Wahl. */
    fun place(time: RestTime, preferment: Preferment, proofPlace: RestPlace): RestPlace = when (time) {
        RestTime.RIPEN -> if (preferment == Preferment.BIGA) RestPlace.FRIDGE else RestPlace.ROOM
        RestTime.PROOF -> proofPlace
        RestTime.BULK, RestTime.WARM -> RestPlace.ROOM
    }

    /** Wie viel länger es bei [room] dauert als bei 22 °C (z. B. 0,7 bei 26 °C, 1,4 bei 18 °C). */
    fun roomFactor(room: Double): Double = 2.0.pow((REFERENCE_ROOM - room) / DOUBLING_DEGREES)

    /**
     * Die Gehzeiten, die gelten: selbst geänderte und die im Kühlschrank wie eingestellt, die übrigen bei
     * Raumtemperatur auf die Raumtemperatur umgerechnet und auf die nächste Stufe gerundet.
     */
    fun effective(process: DoughProcess, kitchen: DoughKitchen, type: DoughType, preferment: Preferment): DoughProcess {
        var out = process
        for (time in RestTime.entries) {
            if (!applies(time, type, preferment, process.proofPlace)) continue
            if (time in process.manual || place(time, preferment, process.proofPlace) == RestPlace.FRIDGE) continue
            val list = steps(time, type, preferment, process.proofPlace)
            out = out.withHours(time, nearest(process.hours(time) * roomFactor(kitchen.room), list))
        }
        return out
    }

    /** Die Stufe, die [hours] am nächsten liegt (im Verhältnis, damit 45 Min. und 8 Std. gleich fein behandelt werden). */
    fun nearest(hours: Double, list: List<Double>): Double {
        if (list.isEmpty() || !(hours > 0)) return list.firstOrNull() ?: 0.0
        return list.minBy { abs(ln(it / hours)) }
    }

    /**
     * Eine Gehzeit eine Stufe kürzer oder länger, ausgehend von der wirksamen ([effective]); sie gilt danach als selbst
     * geändert und passt sich nicht mehr der Raumtemperatur an.
     */
    fun step(process: DoughProcess, kitchen: DoughKitchen, type: DoughType, preferment: Preferment, time: RestTime, direction: Int): DoughProcess {
        val list = steps(time, type, preferment, process.proofPlace)
        if (list.isEmpty()) return process
        val current = effective(process, kitchen, type, preferment).hours(time)
        val index = list.indexOfFirst { abs(it - current) < 1e-6 }.takeIf { it >= 0 } ?: list.indexOf(nearest(current, list))
        val next = list[(index + direction).coerceIn(0, list.lastIndex)]
        return process.withHours(time, next).copy(manual = process.manual + time)
    }

    /** Geht es in [direction] noch weiter? */
    fun canStep(process: DoughProcess, kitchen: DoughKitchen, type: DoughType, preferment: Preferment, time: RestTime, direction: Int): Boolean {
        val list = steps(time, type, preferment, process.proofPlace)
        if (list.isEmpty()) return false
        val current = effective(process, kitchen, type, preferment).hours(time)
        return if (direction > 0) current < list.last() - 1e-6 else current > list.first() + 1e-6
    }

    /** Teigkugeln bzw. Laib an einen anderen Ort: Gehzeit (und Aufwärmen) beginnen beim Standardwert für diesen Ort. */
    fun withPlace(process: DoughProcess, type: DoughType, preferment: Preferment, place: RestPlace): DoughProcess {
        if (place == process.proofPlace) return process
        return process.copy(
            proofPlace = place,
            proof = defaultHours(RestTime.PROOF, type, preferment, place),
            warm = defaults(type, preferment).warm,
            manual = process.manual - RestTime.PROOF - RestTime.WARM,
        )
    }

    /** Anderer Vorteig: Gehzeiten und Ort wie beim neuen Vorteig üblich; die Temperaturen bleiben. */
    fun forPreferment(process: DoughProcess, type: DoughType, preferment: Preferment): DoughProcess =
        defaults(type, preferment).copy(doughTemp = process.doughTemp, ovenTemp = process.ovenTemp)

    /** Knetzeit in Minuten (frühestens, spätestens): von Hand länger als mit der Maschine. */
    fun kneadMinutes(kneading: Kneading): Pair<Int, Int> = if (kneading == Kneading.MACHINE) 8 to 12 else 15 to 20

    /**
     * Vom Ansetzen bis zum Backen in Minuten: alle Gehzeiten aus [effective] und die Arbeitsschritte mit ihrer längsten
     * Dauer, auf 5 Minuten aufgerundet.
     */
    fun totalMinutes(effective: DoughProcess, type: DoughType, preferment: Preferment, kneading: Kneading): Int {
        val plan = DoughTimeline.plan(type, preferment, effective, kneading)
        val minutes = plan.filter { it.kind != PhaseKind.BAKE }.sumOf { it.max }
        return (ceil(minutes / 5.0) * 5).roundToInt()
    }
}
