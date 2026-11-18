package com.moltobene.app.data.dough

import kotlin.math.roundToLong

/**
 * Menge oben im Teigrechner antippen und ändern (Wunsch vom 18.11.2026): Der ganze Teig wird im gleichen Verhältnis
 * umgerechnet. Die Prozente bleiben, nur das Gewicht je Teigkugel bzw. Laib ändert sich. Reines Kotlin.
 */
object DoughScale {

    /**
     * Neues Gewicht je Stück, wenn eine Menge von [currentGrams] auf [targetGrams] geändert wird (z. B. Gesamtteig
     * 1000 g auf 1500 g oder Mehl 607 g auf 500 g); auf 0,1 g gerundet und in den Grenzen [range]. null, wenn sich
     * nichts rechnen lässt.
     */
    fun pieceGrams(pieceGrams: Double, currentGrams: Double, targetGrams: Double, range: DoughLimits.Range): Double? {
        if (!(pieceGrams > 0) || !(currentGrams > 0) || !(targetGrams > 0) || !targetGrams.isFinite()) return null
        val piece = (pieceGrams * targetGrams / currentGrams * 10).roundToLong() / 10.0
        return piece.coerceIn(range.min, range.max)
    }

    /** Schritt für − und + im Fenster: 10 ab 100, 1 ab 10, sonst 0,1 (in der gewählten Einheit). */
    fun step(value: Double): Double = when {
        value >= 100 -> 10.0
        value >= 10 -> 1.0
        else -> 0.1
    }

    /** Ein Schritt weiter von [value] in Richtung [direction] (−1 oder +1), auf das Raster des Schritts, nie unter 0. */
    fun next(value: Double, direction: Int): Double {
        if (!value.isFinite()) return 0.0
        val step = step(if (direction < 0) (value - 1e-9).coerceAtLeast(0.0) else value)
        val snapped = if (direction > 0) Math.floor(value / step + 1e-9) * step + step else Math.ceil(value / step - 1e-9) * step - step
        return ((snapped * 10).roundToLong() / 10.0).coerceAtLeast(0.0)
    }
}
