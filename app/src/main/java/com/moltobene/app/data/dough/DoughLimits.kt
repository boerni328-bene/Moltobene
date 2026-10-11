package com.moltobene.app.data.dough

/**
 * Grenzen aller Eingaben des Teigrechners (#69, Besprechung vom 08.10.2026): die einzige Quelle, auch für gemerkte
 * Werte beim Laden. Eigene Festlegung, kein Übernehmen aus fremden Quellen (Stand 11.10.2026). Außerhalb der Grenzen
 * wird nicht gerechnet und nichts still zurechtgeschnitten.
 */
object DoughLimits {

    data class Range(val min: Double, val max: Double) {
        operator fun contains(value: Double): Boolean = value.isFinite() && value in min..max
    }

    /** Alle Mengen eines Teigs zusammen: höchstens 100 kg. */
    const val MAX_TOTAL_GRAMS = 100_000.0

    fun range(type: DoughType, field: DoughField, preferment: Preferment = Preferment.NONE): Range = when (type) {
        DoughType.PIZZA -> when (field) {
            DoughField.COUNT -> Range(1.0, 50.0)
            DoughField.PIECE_GRAMS -> Range(50.0, 2000.0)
            DoughField.WATER -> Range(45.0, 100.0)
            DoughField.SALT -> Range(0.0, 5.0)
            DoughField.OIL -> Range(0.0, 10.0)
            DoughField.YEAST -> yeast(5.0, preferment)
            DoughField.PREFERMENT_FLOUR -> Range(1.0, 100.0)
            DoughField.PREFERMENT_WATER -> Range(40.0, 125.0)
        }
        DoughType.BREAD -> when (field) {
            DoughField.COUNT -> Range(1.0, 20.0)
            DoughField.PIECE_GRAMS -> Range(200.0, 3000.0)
            DoughField.WATER -> Range(50.0, 110.0)
            DoughField.SALT -> Range(0.0, 3.5)
            DoughField.OIL -> Range(0.0, 10.0)
            DoughField.YEAST -> yeast(3.0, preferment)
            DoughField.PREFERMENT_FLOUR -> Range(1.0, 100.0)
            DoughField.PREFERMENT_WATER -> Range(40.0, 125.0)
        }
        // Pasta und Kuchen haben eigene Eingaben (#70); für sie gelten vorerst dieselben weiten Grenzen wie bei Brot.
        DoughType.PASTA, DoughType.CAKE -> range(DoughType.BREAD, field, preferment)
    }

    /** Hefe: mindestens 0,01 %; mit Sauerteig darf sie ganz fehlen. */
    private fun yeast(max: Double, preferment: Preferment) =
        Range(if (preferment == Preferment.SOURDOUGH) 0.0 else 0.01, max)

    /** Sinnvolle Startwerte je Teigart, eigene Festlegung. */
    fun defaults(type: DoughType): LeavenedInput = when (type) {
        DoughType.BREAD -> LeavenedInput(
            type = DoughType.BREAD, count = 1, pieceGrams = 750.0, water = 70.0, salt = 2.0, oil = 0.0,
            yeastKind = YeastKind.FRESH, yeast = 0.5, preferment = Preferment.NONE, prefermentFlour = 20.0,
            prefermentWater = 100.0,
        )
        else -> LeavenedInput(
            type = DoughType.PIZZA, count = 4, pieceGrams = 250.0, water = 65.0, salt = 2.8, oil = 0.0,
            yeastKind = YeastKind.FRESH, yeast = 0.2, preferment = Preferment.NONE, prefermentFlour = 30.0,
            prefermentWater = 100.0,
        )
    }

    /** Wasser im Vorteig, das bei Biga und Poolish vorausgefüllt wird. */
    fun defaultPrefermentWater(preferment: Preferment): Double = when (preferment) {
        Preferment.BIGA -> 50.0
        else -> 100.0
    }
}
