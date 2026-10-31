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
        DoughField.OIL, DoughField.BAKING_POWDER, DoughField.MALT, DoughField.HONEY -> 0.5
        DoughField.YEAST -> 0.05
        DoughField.PREFERMENT_FLOUR, DoughField.PREFERMENT_WATER, DoughField.SEMOLINA, DoughField.WHOLE_EGG, DoughField.YOLK,
        DoughField.YOGURT, DoughField.RYE, DoughField.WHOLE, DoughField.SPELT, DoughField.SEEDS -> 5.0
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

    /**
     * Der Bereich, den ein Regler zeigt (#83, Etappe 2): der **übliche** Bereich eines Felds, nie größer als die Grenzen
     * aus [DoughLimits]. Wer mehr braucht, tippt den Wert. Null, wenn das Feld keinen Regler bekommt.
     */
    fun sliderBounds(field: DoughField, limits: DoughLimits.Range?): DoughLimits.Range? {
        val practical = when (field) {
            DoughField.COUNT -> DoughLimits.Range(1.0, 12.0)
            DoughField.PIECE_GRAMS -> DoughLimits.Range(100.0, 1000.0)
            DoughField.WATER -> DoughLimits.Range(50.0, 90.0)
            DoughField.SALT -> DoughLimits.Range(0.0, 4.0)
            DoughField.OIL -> DoughLimits.Range(0.0, 8.0)
            DoughField.YEAST -> DoughLimits.Range(0.0, 2.0)
            DoughField.PREFERMENT_FLOUR -> DoughLimits.Range(5.0, 100.0)
            DoughField.PREFERMENT_WATER -> DoughLimits.Range(40.0, 120.0)
            DoughField.FLOUR_PER_PORTION -> DoughLimits.Range(50.0, 200.0)
            DoughField.FLOUR_GRAMS -> DoughLimits.Range(100.0, 1000.0)
            DoughField.SEMOLINA, DoughField.WHOLE_EGG, DoughField.YOLK, DoughField.FAT, DoughField.SUGAR, DoughField.EGG,
            DoughField.MILK, DoughField.QUARK, DoughField.BAKING_POWDER, DoughField.MALT, DoughField.YOGURT, DoughField.RYE, DoughField.WHOLE,
            DoughField.SPELT, DoughField.HONEY, DoughField.SEEDS -> null
            DoughField.PAN_SIZE -> return null
        }
        if (limits == null) return practical
        if (practical == null) return limits.takeIf { it.max > it.min }
        val low = maxOf(practical.min, limits.min)
        val high = minOf(practical.max, limits.max)
        return if (high > low) DoughLimits.Range(low, high) else limits.takeIf { it.max > it.min }
    }

    /** Schrittweite des Reglers; bei der Hefe feiner als bei Plus/Minus, damit kleine Mengen treffbar sind. */
    fun sliderStepFor(field: DoughField): Double = if (field == DoughField.YEAST) 0.01 else stepFor(field)

    /** Die Hefe hat eine gestauchte Skala: feine Schritte im unteren Bereich, wo die üblichen Mengen liegen. */
    fun isCurved(field: DoughField): Boolean = field == DoughField.YEAST

    /** Stellung des Reglers (0 bis 1) für einen Wert; Werte außerhalb des Bereichs stehen am Rand. */
    fun positionOf(value: Double, bounds: DoughLimits.Range, curved: Boolean): Float {
        if (!value.isFinite()) return 0f
        val linear = ((value - bounds.min) / (bounds.max - bounds.min)).coerceIn(0.0, 1.0)
        return (if (curved) Math.cbrt(linear) else linear).toFloat()
    }

    /** Der Wert an einer Reglerstellung, auf die Schrittweite gerundet und innerhalb von [limits] ([bounds] ohne Grenzen). */
    fun valueAt(position: Float, bounds: DoughLimits.Range, curved: Boolean, step: Double, limits: DoughLimits.Range?): Double {
        val p = position.toDouble().coerceIn(0.0, 1.0)
        val raw = bounds.min + (bounds.max - bounds.min) * (if (curved) p * p * p else p)
        val steps = Math.round(raw / step)
        val rounded = BigDecimal(steps).multiply(BigDecimal(step.toString())).setScale(4, RoundingMode.HALF_UP).toDouble()
        val range = limits ?: bounds
        return rounded.coerceIn(range.min, range.max)
    }

    fun isDefault(current: Double, default: Double): Boolean = current.isFinite() && abs(current - default) < EPSILON

    private const val EPSILON = 1e-6
}
