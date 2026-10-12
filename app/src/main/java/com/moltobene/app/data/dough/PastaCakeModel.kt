package com.moltobene.app.data.dough

import com.moltobene.app.data.Pan

/**
 * Pasta und Kuchen im Teigrechner (#70): Eingaben, Grenzen und Merken. Reines Kotlin, per Unit-Test prüfbar.
 * Alle Prozente beziehen sich auf das gesamte Mehl (bei Pasta Weizenmehl und Grieß zusammen).
 */

/** Ein Vollei ohne Schale wiegt rund 55 g, ein Eigelb rund 20 g; Richtwerte für die Anzeige „≈ n Eier“. */
object DoughConstants {
    const val EGG_GRAMS = 55.0
    const val YOLK_GRAMS = 20.0

    /** Alle Kuchenmengen je Art beziehen sich auf diese Form. */
    val REFERENCE_PAN = Pan.round(26.0)

    /** Eier bzw. Eigelb als ganze Zahl für „≈ n“; nie unter 1, wenn es überhaupt Ei gibt. */
    fun eggsAbout(grams: Double, each: Double): Int =
        if (grams <= 0.0 || !grams.isFinite()) 0 else Math.round(grams / each).toInt().coerceAtLeast(1)
}

enum class PastaKind(val key: String) {
    /** Eierteig, auch nur mit Eigelb. */
    EGG("egg"),

    /** Wasserteig, z. B. Grieß und Wasser für Orecchiette, Cavatelli oder Pici. */
    WATER("water"),
    ;

    companion object {
        fun fromKey(key: String?): PastaKind? = entries.firstOrNull { it.key == key }
    }
}

/** [portions] Portionen zu je [flourPerPortion] g Mehl; [semolina] ist der Anteil Hartweizengrieß am Mehl. */
data class PastaInput(
    val kind: PastaKind,
    val portions: Int,
    val flourPerPortion: Double,
    val semolina: Double,
    val egg: Double,
    val yolk: Double,
    val water: Double,
    val salt: Double,
    val oil: Double,
) : DoughInput {
    override val type: DoughType get() = DoughType.PASTA
}

enum class CakeKind(val key: String) {
    BATTER("batter"),
    SPONGE("sponge"),
    SHORTCRUST("shortcrust"),
    YEAST_SWEET("yeast_sweet"),
    CHOUX("choux"),
    QUARK_OIL("quark_oil"),
    ;

    companion object {
        fun fromKey(key: String?): CakeKind? = entries.firstOrNull { it.key == key }
    }
}

/** Woher die Mehlmenge kommt: aus der Backform oder direkt in Gramm. */
sealed interface CakeSize {
    data class PanSize(val pan: Pan) : CakeSize
    data class FlourGrams(val grams: Double) : CakeSize
}

/**
 * Kuchenteig; je Art zählen nur die Felder aus [CakeLimits.fields]. [fat] ist Butter, beim Quark-Öl-Teig Öl;
 * [milk] ist beim Brandteig Wasser oder Milch.
 */
data class CakeInput(
    val kind: CakeKind,
    val size: CakeSize,
    val fat: Double,
    val sugar: Double,
    val egg: Double,
    val milk: Double,
    val quark: Double,
    val yeastKind: YeastKind,
    val yeast: Double,
    val bakingPowder: Double,
    val salt: Double,
) : DoughInput {
    override val type: DoughType get() = DoughType.CAKE
}

/** Grenzen und Startwerte für Pasta; die einzige Quelle, auch für gemerkte Werte. Eigene Festlegung (12.10.2026). */
object PastaLimits {
    fun range(kind: PastaKind, field: DoughField): DoughLimits.Range = when (field) {
        DoughField.COUNT -> DoughLimits.Range(1.0, 30.0)
        DoughField.FLOUR_PER_PORTION -> DoughLimits.Range(50.0, 250.0)
        DoughField.SEMOLINA -> DoughLimits.Range(0.0, 100.0)
        DoughField.WHOLE_EGG -> if (kind == PastaKind.EGG) DoughLimits.Range(0.0, 100.0) else DoughLimits.Range(0.0, 0.0)
        DoughField.YOLK -> if (kind == PastaKind.EGG) DoughLimits.Range(0.0, 80.0) else DoughLimits.Range(0.0, 0.0)
        DoughField.WATER -> if (kind == PastaKind.EGG) DoughLimits.Range(0.0, 30.0) else DoughLimits.Range(35.0, 70.0)
        DoughField.SALT -> DoughLimits.Range(0.0, 3.0)
        DoughField.OIL -> DoughLimits.Range(0.0, 5.0)
        else -> throw IllegalArgumentException("Das Feld $field gibt es bei Pasta nicht.")
    }

    /** Beim Eierteig müssen Vollei und Eigelb zusammen mindestens so viel ergeben, sonst gibt es keinen Teig. */
    const val MIN_EGG_TOTAL = 30.0

    fun defaults(kind: PastaKind): PastaInput = when (kind) {
        PastaKind.EGG -> PastaInput(kind, 4, 100.0, 0.0, 55.0, 0.0, 0.0, 1.0, 0.0)
        PastaKind.WATER -> PastaInput(kind, 3, 100.0, 100.0, 0.0, 0.0, 50.0, 1.0, 0.0)
    }
}

/** Felder des Kuchenteigs, die je Art vorkommen. */
enum class CakeField { FAT, SUGAR, EGG, MILK, QUARK, YEAST, BAKING_POWDER, SALT }

/** Grenzen, Startwerte und Bezugsmehl je Kuchenart; Richtwerte, kein Rezept (12.10.2026). */
object CakeLimits {

    fun fields(kind: CakeKind): List<CakeField> = when (kind) {
        CakeKind.BATTER -> listOf(CakeField.FAT, CakeField.SUGAR, CakeField.EGG, CakeField.MILK, CakeField.BAKING_POWDER, CakeField.SALT)
        CakeKind.SPONGE -> listOf(CakeField.SUGAR, CakeField.EGG, CakeField.BAKING_POWDER, CakeField.SALT)
        CakeKind.SHORTCRUST -> listOf(CakeField.FAT, CakeField.SUGAR, CakeField.EGG, CakeField.SALT)
        CakeKind.YEAST_SWEET -> listOf(CakeField.FAT, CakeField.SUGAR, CakeField.EGG, CakeField.MILK, CakeField.YEAST, CakeField.SALT)
        CakeKind.CHOUX -> listOf(CakeField.FAT, CakeField.EGG, CakeField.MILK, CakeField.SALT)
        CakeKind.QUARK_OIL ->
            listOf(CakeField.FAT, CakeField.SUGAR, CakeField.EGG, CakeField.MILK, CakeField.QUARK, CakeField.BAKING_POWDER, CakeField.SALT)
    }

    fun range(kind: CakeKind, field: CakeField): DoughLimits.Range {
        fun r(min: Double, max: Double) = DoughLimits.Range(min, max)
        return when (field) {
            CakeField.FAT -> when (kind) {
                CakeKind.BATTER -> r(20.0, 120.0)
                CakeKind.SHORTCRUST -> r(40.0, 90.0)
                CakeKind.YEAST_SWEET -> r(5.0, 40.0)
                CakeKind.CHOUX -> r(30.0, 80.0)
                CakeKind.QUARK_OIL -> r(10.0, 40.0)
                CakeKind.SPONGE -> r(0.0, 0.0)
            }
            CakeField.SUGAR -> when (kind) {
                CakeKind.BATTER -> r(20.0, 120.0)
                CakeKind.SPONGE -> r(60.0, 130.0)
                CakeKind.SHORTCRUST -> r(10.0, 60.0)
                CakeKind.YEAST_SWEET -> r(5.0, 30.0)
                CakeKind.QUARK_OIL -> r(10.0, 50.0)
                CakeKind.CHOUX -> r(0.0, 0.0)
            }
            CakeField.EGG -> when (kind) {
                CakeKind.BATTER -> r(30.0, 120.0)
                CakeKind.SPONGE -> r(120.0, 250.0)
                CakeKind.SHORTCRUST -> r(0.0, 60.0)
                CakeKind.YEAST_SWEET -> r(0.0, 40.0)
                CakeKind.CHOUX -> r(100.0, 200.0)
                CakeKind.QUARK_OIL -> r(0.0, 40.0)
            }
            CakeField.MILK -> when (kind) {
                CakeKind.BATTER -> r(0.0, 60.0)
                CakeKind.YEAST_SWEET -> r(40.0, 70.0)
                CakeKind.CHOUX -> r(120.0, 220.0)
                CakeKind.QUARK_OIL -> r(0.0, 40.0)
                else -> r(0.0, 0.0)
            }
            CakeField.QUARK -> if (kind == CakeKind.QUARK_OIL) r(30.0, 80.0) else r(0.0, 0.0)
            CakeField.YEAST -> if (kind == CakeKind.YEAST_SWEET) r(0.3, 8.0) else r(0.0, 0.0)
            CakeField.BAKING_POWDER -> when (kind) {
                CakeKind.BATTER -> r(0.0, 5.0)
                CakeKind.SPONGE -> r(0.0, 3.0)
                CakeKind.QUARK_OIL -> r(2.0, 8.0)
                else -> r(0.0, 0.0)
            }
            CakeField.SALT -> r(0.0, 2.0)
        }
    }

    /** Mehl in Gramm, aus der Form oder direkt: 20 bis 3.000 g, beim Brandteig 50 bis 1.000 g. */
    fun flourRange(kind: CakeKind): DoughLimits.Range =
        if (kind == CakeKind.CHOUX) DoughLimits.Range(50.0, 1000.0) else DoughLimits.Range(20.0, 3000.0)

    /** Gramm Mehl für die Form Ø 26 cm. */
    fun referenceFlour(kind: CakeKind): Double = when (kind) {
        CakeKind.BATTER -> 250.0
        CakeKind.SPONGE -> 115.0
        CakeKind.SHORTCRUST -> 200.0
        CakeKind.YEAST_SWEET -> 220.0
        CakeKind.CHOUX -> 150.0
        CakeKind.QUARK_OIL -> 300.0
    }

    /** Hefeteig und Brandteig gibt es nicht nach Form, sondern nach Mehl. */
    fun defaultSize(kind: CakeKind): CakeSize = when (kind) {
        CakeKind.YEAST_SWEET -> CakeSize.FlourGrams(500.0)
        CakeKind.CHOUX -> CakeSize.FlourGrams(150.0)
        else -> CakeSize.PanSize(DoughConstants.REFERENCE_PAN)
    }

    fun defaults(kind: CakeKind): CakeInput {
        val base = CakeInput(kind, defaultSize(kind), 0.0, 0.0, 0.0, 0.0, 0.0, YeastKind.FRESH, 0.0, 0.0, 0.0)
        return when (kind) {
            CakeKind.BATTER -> base.copy(fat = 80.0, sugar = 80.0, egg = 80.0, milk = 20.0, bakingPowder = 3.0, salt = 0.4)
            CakeKind.SPONGE -> base.copy(sugar = 100.0, egg = 183.0)
            CakeKind.SHORTCRUST -> base.copy(fat = 65.0, sugar = 35.0, egg = 27.5, salt = 0.5)
            CakeKind.YEAST_SWEET -> base.copy(fat = 15.0, sugar = 12.0, egg = 11.0, milk = 50.0, yeast = 4.0, salt = 1.0)
            CakeKind.CHOUX -> base.copy(fat = 50.0, egg = 145.0, milk = 167.0, salt = 1.0)
            CakeKind.QUARK_OIL ->
                base.copy(fat = 20.0, sugar = 25.0, egg = 18.0, milk = 20.0, quark = 50.0, bakingPowder = 5.0, salt = 0.3)
        }
    }

    /** Der Wert eines Felds aus der Eingabe. */
    fun value(input: CakeInput, field: CakeField): Double = when (field) {
        CakeField.FAT -> input.fat
        CakeField.SUGAR -> input.sugar
        CakeField.EGG -> input.egg
        CakeField.MILK -> input.milk
        CakeField.QUARK -> input.quark
        CakeField.YEAST -> input.yeast
        CakeField.BAKING_POWDER -> input.bakingPowder
        CakeField.SALT -> input.salt
    }

    fun withValue(input: CakeInput, field: CakeField, value: Double): CakeInput = when (field) {
        CakeField.FAT -> input.copy(fat = value)
        CakeField.SUGAR -> input.copy(sugar = value)
        CakeField.EGG -> input.copy(egg = value)
        CakeField.MILK -> input.copy(milk = value)
        CakeField.QUARK -> input.copy(quark = value)
        CakeField.YEAST -> input.copy(yeast = value)
        CakeField.BAKING_POWDER -> input.copy(bakingPowder = value)
        CakeField.SALT -> input.copy(salt = value)
    }
}
