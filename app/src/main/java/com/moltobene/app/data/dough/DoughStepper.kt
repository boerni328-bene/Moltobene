package com.moltobene.app.data.dough

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Plus und Minus im Teigrechner (#83, Besprechung vom 12.10.2026): Schrittweite je Feld, nächster Wert innerhalb der
 * Grenzen und Vergleich mit dem Standardwert. Rechnet mit Dezimalzahlen, damit 0,1 + 0,2 genau 0,3 ergibt. Die Grenzen
 * aus [DoughLimits] gelten unverändert: Die Knöpfe bleiben an der Grenze stehen, getippte Werte werden nie still
 * zurechtgeschnitten.
 */
object DoughStepper {

    /** Schrittweite in der Einheit der Anzeige (Gramm, Prozent, Stück). */
    fun stepFor(field: DoughField): Double = when (field) {
        DoughField.COUNT -> 1.0
        DoughField.PIECE_GRAMS, DoughField.FLOUR_PER_PORTION, DoughField.FLOUR_GRAMS -> 10.0
        DoughField.WATER, DoughField.FAT, DoughField.SUGAR, DoughField.EGG, DoughField.MILK, DoughField.QUARK -> 1.0
        DoughField.SALT -> 0.1
        DoughField.OIL, DoughField.BAKING_POWDER -> 0.5
        DoughField.YEAST -> 0.05
        DoughField.PREFERMENT_FLOUR, DoughField.PREFERMENT_WATER, DoughField.SEMOLINA, DoughField.WHOLE_EGG, DoughField.YOLK -> 5.0
        DoughField.PAN_SIZE -> 1.0
    }

    /**
     * Der Wert nach einem Schritt in [direction] (+1 oder −1). Ist [current] keine Zahl, beginnt es beim Standardwert
     * oder an der unteren Grenze. Ohne Grenzen geht es nie unter 0.
     */
    fun next(current: Double, step: Double, direction: Int, range: DoughLimits.Range?, default: Double? = null): Double {
        val start = if (current.isFinite()) current else default ?: range?.min ?: 0.0
        val stepped = BigDecimal(start.toString()).add(BigDecimal(step.toString()).multiply(BigDecimal(direction)))
        val value = stepped.setScale(4, RoundingMode.HALF_UP).toDouble()
        val low = range?.min ?: 0.0
        val high = range?.max ?: Double.MAX_VALUE
        return value.coerceIn(low, high)
    }

    /** Geht es in [direction] noch weiter? Mit ungültigem Wert immer, damit ein Schritt zurück zu einer Zahl führt. */
    fun canStep(current: Double, direction: Int, range: DoughLimits.Range?): Boolean {
        if (!current.isFinite()) return true
        return if (direction > 0) range == null || current < range.max - EPSILON else current > (range?.min ?: 0.0) + EPSILON
    }

    fun isDefault(current: Double, default: Double): Boolean = current.isFinite() && abs(current - default) < EPSILON

    private const val EPSILON = 1e-6
}
