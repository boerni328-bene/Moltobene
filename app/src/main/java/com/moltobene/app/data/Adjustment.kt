package com.moltobene.app.data

/**
 * „Umrechnen …“ (#66): wonach die Mengen eines Rezepts umgerechnet angezeigt werden – nach Portionen oder nach einer
 * Zutat („nur 300 g Mehl da“). Nur für die Anzeige; das gespeicherte Rezept ändert sich nie. Reines Kotlin, per
 * Unit-Test prüfbar.
 */
sealed interface Adjustment {

    /** Für [servings] statt der Portionen des Rezepts. */
    data class Servings(val servings: Int) : Adjustment

    /**
     * Die Zutat in Zeile [index] soll [amount] statt ihrer Menge im Rezept haben. [lineHash] erkennt die Zeile wieder,
     * damit nach einer Änderung am Rezept nicht still eine andere Zutat zählt.
     */
    data class ByIngredient(val index: Int, val lineHash: Int, val amount: Double) : Adjustment

    companion object {
        /** Wie im Formular: höchstens 999 Portionen. */
        const val MAX_SERVINGS = 999

        /** Nach einer Zutat höchstens das Hundertfache und mindestens ein Hundertstel des Rezepts. */
        const val MAX_FACTOR = 100.0
        const val MIN_FACTOR = 1.0 / MAX_FACTOR

        fun byIngredient(index: Int, line: String, amount: Double): Adjustment = ByIngredient(index, line.hashCode(), amount)

        /** Als Text für den Zwischenspeicher der Ansicht (übersteht das Drehen und das Beenden durch Android). */
        fun encode(adjustment: Adjustment): String = when (adjustment) {
            is Servings -> "$SERVINGS:${adjustment.servings}"
            is ByIngredient -> "$INGREDIENT:${adjustment.index}:${adjustment.lineHash}:${adjustment.amount}"
        }

        /** Gegenstück zu [encode]; null bei fehlender oder ungültiger Angabe. */
        fun decode(text: String?): Adjustment? {
            val parts = text?.split(':') ?: return null
            return when (parts.firstOrNull()) {
                SERVINGS -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..MAX_SERVINGS }?.let { Servings(it) }
                INGREDIENT -> {
                    val index = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
                    val hash = parts.getOrNull(2)?.toIntOrNull() ?: return null
                    val amount = parts.getOrNull(3)?.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() } ?: return null
                    ByIngredient(index, hash, amount)
                }
                else -> null
            }
        }

        /**
         * Der Faktor für die Anzeige: neue Portionen bzw. neue Menge geteilt durch die Angabe im Rezept. 1.0, wenn nichts
         * umgerechnet wird oder die Angabe nicht (mehr) zum Rezept passt.
         * @param ingredients die Zutatenzeilen des Rezepts im Original
         * @param language Sprache des Rezepts; bestimmt, ob „1.000“ tausend oder eins ist
         */
        fun factor(adjustment: Adjustment?, servings: Int?, ingredients: List<String>, language: String?): Double = when (adjustment) {
            null -> 1.0
            is Servings -> {
                val original = servings?.takeIf { it > 0 }
                if (original == null) 1.0 else adjustment.servings.coerceIn(1, MAX_SERVINGS).toDouble() / original
            }
            is ByIngredient -> {
                val original = ingredients.getOrNull(adjustment.index)
                    ?.takeIf { it.hashCode() == adjustment.lineHash }
                    ?.let { AmountScaling.leadingAmount(it, language) }
                    ?.value
                if (original != null && isAllowed(adjustment.amount, original)) adjustment.amount / original else 1.0
            }
        }

        /** Darf eine Zutat mit [original] im Rezept auf [amount] umgerechnet werden? Über 0, höchstens das Hundertfache. */
        fun isAllowed(amount: Double, original: Double): Boolean {
            if (!(amount > 0 && original > 0 && amount.isFinite())) return false
            return amount / original in MIN_FACTOR..MAX_FACTOR
        }

        private const val SERVINGS = "servings"
        private const val INGREDIENT = "ingredient"
    }
}
