package com.moltobene.app.data.dough

/**
 * Teigrechner (#69): die Begriffe, Eingaben und das Ergebnis der Rechnung. Reines Kotlin ohne Android, per Unit-Test
 * prüfbar. Die Rechnung selbst steht in [DoughCalculator], die Grenzen in [DoughLimits].
 */

/** Teigart; die Kennung ist fest und wird gemerkt, der angezeigte Name kommt aus strings.xml. */
enum class DoughType(val key: String) {
    PIZZA("pizza"),
    BREAD("bread"),
    PASTA("pasta"),
    CAKE("cake"),
    ;

    companion object {
        fun fromKey(key: String?): DoughType? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Hefeart. [perFresh] sind Gramm dieser Hefe, die 1 g Frischhefe ersetzen; Richtwerte, in der Anzeige mit „≈“.
 * Eigene Festlegung, kein Übernehmen aus fremden Quellen (Stand 11.10.2026).
 */
enum class YeastKind(val key: String, val perFresh: Double) {
    FRESH("fresh", 1.0),
    INSTANT_DRY("instant_dry", 1.0 / 3.0),
    ACTIVE_DRY("active_dry", 0.4),
    ;

    /** Rechnet [grams] dieser Hefe in Gramm der Hefe [to] um. */
    fun convert(grams: Double, to: YeastKind): Double = grams / perFresh * to.perFresh

    companion object {
        fun fromKey(key: String?): YeastKind? = entries.firstOrNull { it.key == key }
    }
}

/** Vorteig: keiner, Biga (fest), Poolish (flüssig) oder Sauerteig als Anstellgut mit eigenem Mehl und Wasser. */
enum class Preferment(val key: String) {
    NONE("none"),
    BIGA("biga"),
    POOLISH("poolish"),
    SOURDOUGH("sourdough"),
    ;

    companion object {
        fun fromKey(key: String?): Preferment? = entries.firstOrNull { it.key == key }
    }
}

/** Eingaben für Pizza und Brot (Teige mit Hefe oder Sauerteig); alle Prozente als Zahl, 65 heißt 65 % vom Mehl. */
data class LeavenedInput(
    val type: DoughType,
    /** Anzahl der Teigkugeln bzw. Laibe. */
    val count: Int,
    /** Gewicht je Teigkugel bzw. Laib in Gramm. */
    val pieceGrams: Double,
    val water: Double,
    val salt: Double,
    val oil: Double,
    val yeastKind: YeastKind,
    /** Menge der gewählten Hefe in % vom gesamten Mehl. */
    val yeast: Double,
    val preferment: Preferment,
    /** Anteil des Mehls im Vorteig in % vom gesamten Mehl. */
    val prefermentFlour: Double,
    /** Wasser im Vorteig in % vom Mehl im Vorteig. */
    val prefermentWater: Double,
)

enum class LineRole { FLOUR, WATER, SALT, OIL, YEAST }

/** Eine Zutat in Gramm, ungerundet; gerundet wird erst beim Schreiben ([DoughNumbers]). */
data class DoughLine(val role: LineRole, val grams: Double)

enum class SectionKind { PLAIN, PREFERMENT, MAIN }

/** Ein Abschnitt des Ergebnisses: bei einem Vorteig „Vorteig“ und „Hauptteig“, sonst ein einziger. */
data class DoughSection(val kind: SectionKind, val preferment: Preferment, val lines: List<DoughLine>)

enum class YieldKind { BALL, LOAF }

/** Was herauskommt: [count] Teigkugeln bzw. Laibe zu je [pieceGrams] Gramm. */
data class DoughYield(val count: Int, val kind: YieldKind, val pieceGrams: Double)

enum class DoughField { COUNT, PIECE_GRAMS, WATER, SALT, OIL, YEAST, PREFERMENT_FLOUR, PREFERMENT_WATER }

/** Warum eine Eingabe nicht rechnet. */
sealed interface DoughIssue {
    /** Keine Zahl (leer, Text, unendlich). */
    data class NotANumber(val field: DoughField) : DoughIssue

    /** Außerhalb der Grenzen ([DoughLimits]). */
    data class OutOfRange(val field: DoughField, val min: Double, val max: Double) : DoughIssue

    /** Der Vorteig bräuchte mehr Wasser, als der ganze Teig haben soll. */
    data class PrefermentWaterTooHigh(val neededPercent: Double, val totalPercent: Double) : DoughIssue

    /** Die Rechnung ergab etwas Unmögliches; sollte nie vorkommen und wird nie angezeigt. */
    data object Inconsistent : DoughIssue
}

sealed interface DoughResult {
    /** [totalGrams] Teig, darin [flourGrams] Mehl insgesamt. */
    data class Success(
        val sections: List<DoughSection>,
        val yield: DoughYield,
        val totalGrams: Double,
        val flourGrams: Double,
    ) : DoughResult

    data class Invalid(val issues: List<DoughIssue>) : DoughResult
}
