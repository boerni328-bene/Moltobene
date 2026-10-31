package com.moltobene.app.data.dough

import kotlin.math.round

/**
 * Mehlsorten und Wasser (Teigrechner, Wunsch des Projektinhabers vom 31.10.2026): Roggen und Vollkorn nehmen mehr Wasser
 * auf als Weizenmehl, Dinkel eher weniger. Die Zahlen sind grobe Richtwerte in eigener Festlegung (Prozentpunkte Wasser
 * je 1 % Anteil am Mehl, bezogen auf Weizenmehl); sie schlagen einen Wert vor, ändern ihn aber nie von selbst.
 */
object DoughFlours {

    /** Prozentpunkte Wasser, die 100 % dieser Sorte gegenüber Weizenmehl mehr (+) oder weniger (−) brauchen. */
    const val RYE_SHIFT = 12.0
    const val WHOLE_SHIFT = 8.0
    const val SPELT_SHIFT = -3.0

    /** Empfohlene Änderung des Wassers in Prozentpunkten für die Mehlmischung von [input], auf 0,5 gerundet; 0 ohne Mischung. */
    fun waterShift(input: LeavenedInput): Double {
        val raw = (input.rye * RYE_SHIFT + input.whole * WHOLE_SHIFT + input.spelt * SPELT_SHIFT) / 100
        return if (raw.isFinite()) round(raw * 2) / 2 else 0.0
    }
}
