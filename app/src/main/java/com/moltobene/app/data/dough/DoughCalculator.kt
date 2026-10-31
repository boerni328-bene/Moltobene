package com.moltobene.app.data.dough

/**
 * Teigrechner (#69): rechnet Pizza- und Brotteig mit oder ohne Vorteig. Rein, ohne Ausnahmen nach außen: Ungültiges ergibt
 * [DoughResult.Invalid], nie eine negative Menge, „NaN“ oder einen Absturz. Gerechnet wird ungerundet; gerundet wird
 * erst beim Schreiben des Rezepts ([DoughNumbers]).
 *
 * Alle Prozente beziehen sich auf das gesamte Mehl F. Mit dem Gewicht T des ganzen Teigs, Wasser h, Salz s, Öl o und
 * Hefe y (jeweils als Anteil, 65 % = 0,65) ist F = T ÷ (1 + h + s + o + y).
 *
 * Vorteig: Mehl Fp = p · F, Wasser Wp = Fp · hp. Biga und Poolish tragen die ganze Hefe; Sauerteig ist Anstellgut aus
 * Mehl und Wasser ohne Hefefaktor – seine Hefe (falls gewünscht) steht im Hauptteig. Der Hauptteig ist der Rest, das
 * Wasser im Vorteig zählt zum Gesamtwasser.
 */
object DoughCalculator {

    fun calculate(input: LeavenedInput): DoughResult {
        val issues = validate(input)
        if (issues.isNotEmpty()) return DoughResult.Invalid(issues)

        val total = input.count * input.pieceGrams
        val water = input.water / 100
        val salt = input.salt / 100
        val oil = input.oil / 100
        val yeast = input.yeast / 100
        val extras = listOf(input.malt, input.sugar, input.yogurt, input.milk, input.honey, input.egg, input.seeds).sumOf { it } / 100
        val flour = total / (1 + water + salt + oil + yeast + extras)
        // Mit Roggen, Vollkorn oder Dinkel wird das Mehl nach Sorte aufgeteilt (auch im Vorteig im gleichen Verhältnis); der Rest ist Weizenmehl.
        val mixed = input.rye + input.whole + input.spelt > 0
        fun flourLines(grams: Double): List<DoughLine> = if (!mixed) {
            listOf(DoughLine(LineRole.FLOUR, grams))
        } else {
            buildList {
                val wheat = 100 - input.rye - input.whole - input.spelt
                if (wheat > 1e-9) add(DoughLine(LineRole.WHEAT, grams * wheat / 100))
                if (input.rye > 0) add(DoughLine(LineRole.RYE, grams * input.rye / 100))
                if (input.whole > 0) add(DoughLine(LineRole.WHOLE, grams * input.whole / 100))
                if (input.spelt > 0) add(DoughLine(LineRole.SPELT, grams * input.spelt / 100))
            }
        }
        fun extraLines() = buildList {
            if (input.malt > 0) add(DoughLine(LineRole.MALT, input.malt / 100 * flour))
            if (input.sugar > 0) add(DoughLine(LineRole.SUGAR, input.sugar / 100 * flour))
            if (input.yogurt > 0) add(DoughLine(LineRole.YOGURT, input.yogurt / 100 * flour))
            if (input.milk > 0) add(DoughLine(LineRole.MILK, input.milk / 100 * flour))
            if (input.honey > 0) add(DoughLine(LineRole.HONEY, input.honey / 100 * flour))
            if (input.egg > 0) add(DoughLine(LineRole.EGG, input.egg / 100 * flour))
            if (input.seeds > 0) add(DoughLine(LineRole.SEEDS, input.seeds / 100 * flour))
        }

        val sections = if (input.preferment == Preferment.NONE) {
            listOf(
                DoughSection(
                    SectionKind.PLAIN, Preferment.NONE,
                    lines(flourLines(flour), water * flour, salt * flour, oil * flour, yeast * flour, extraLines()),
                ),
            )
        } else {
            val prefermentFlour = input.prefermentFlour / 100 * flour
            val prefermentWater = prefermentFlour * input.prefermentWater / 100
            val mainWater = water * flour - prefermentWater
            if (mainWater < -TOLERANCE) {
                return DoughResult.Invalid(
                    listOf(DoughIssue.PrefermentWaterTooHigh(neededPercent = prefermentWater / flour * 100, totalPercent = input.water)),
                )
            }
            val yeastInPreferment = input.preferment != Preferment.SOURDOUGH
            listOf(
                DoughSection(
                    SectionKind.PREFERMENT, input.preferment,
                    lines(flourLines(prefermentFlour), prefermentWater, 0.0, 0.0, if (yeastInPreferment) yeast * flour else 0.0),
                ),
                DoughSection(
                    SectionKind.MAIN, input.preferment,
                    lines(
                        flourLines(flour - prefermentFlour), mainWater.coerceAtLeast(0.0), salt * flour, oil * flour,
                        if (yeastInPreferment) 0.0 else yeast * flour, extraLines(),
                    ),
                ),
            )
        }
        val yieldKind = if (input.type == DoughType.BREAD) YieldKind.LOAF else YieldKind.BALL
        val result = DoughResult.Success(sections, DoughYield(input.count, yieldKind, input.pieceGrams), total, flour)
        return if (isConsistent(result)) result else DoughResult.Invalid(listOf(DoughIssue.Inconsistent))
    }

    /** Alle Eingaben gegen [DoughLimits]; leer heißt gültig. */
    fun validate(input: LeavenedInput): List<DoughIssue> {
        val issues = mutableListOf<DoughIssue>()
        val type = input.type
        val preferment = input.preferment
        fun check(field: DoughField, value: Double) {
            val range = DoughLimits.range(type, field, preferment)
            when {
                !value.isFinite() -> issues += DoughIssue.NotANumber(field)
                value !in range -> issues += DoughIssue.OutOfRange(field, range.min, range.max)
            }
        }
        check(DoughField.COUNT, input.count.toDouble())
        check(DoughField.PIECE_GRAMS, input.pieceGrams)
        check(DoughField.WATER, input.water)
        check(DoughField.SALT, input.salt)
        check(DoughField.OIL, input.oil)
        check(DoughField.YEAST, input.yeast)
        check(DoughField.MALT, input.malt)
        check(DoughField.YOGURT, input.yogurt)
        check(DoughField.MILK, input.milk)
        check(DoughField.SUGAR, input.sugar)
        check(DoughField.RYE, input.rye)
        check(DoughField.WHOLE, input.whole)
        check(DoughField.SPELT, input.spelt)
        check(DoughField.HONEY, input.honey)
        check(DoughField.EGG, input.egg)
        check(DoughField.SEEDS, input.seeds)
        if (issues.isEmpty() && input.rye + input.whole + input.spelt > 100.0 + 1e-9) issues += DoughIssue.FlourMixTooHigh
        if (preferment != Preferment.NONE) {
            check(DoughField.PREFERMENT_FLOUR, input.prefermentFlour)
            check(DoughField.PREFERMENT_WATER, input.prefermentWater)
        }
        if (issues.isEmpty() && input.count * input.pieceGrams > DoughLimits.MAX_TOTAL_GRAMS) {
            issues += DoughIssue.OutOfRange(DoughField.PIECE_GRAMS, DoughLimits.range(type, DoughField.PIECE_GRAMS).min, DoughLimits.MAX_TOTAL_GRAMS / input.count)
        }
        return issues
    }

    /** Selbstprüfung: alles endlich und nicht negativ, die Summe der Zutaten ergibt das Gewicht des Teigs. */
    private fun isConsistent(result: DoughResult.Success): Boolean {
        val lines = result.sections.flatMap { it.lines }
        if (lines.any { !it.grams.isFinite() || it.grams < 0 }) return false
        if (!result.totalGrams.isFinite() || !result.flourGrams.isFinite()) return false
        return kotlin.math.abs(lines.sumOf { it.grams } - result.totalGrams) <= result.totalGrams * 1e-9 + 1e-9
    }

    /** Zeilen ohne Menge 0 weglassen; Mehl und Wasser bleiben immer. */
    private fun lines(
        flour: List<DoughLine>, water: Double, salt: Double, oil: Double, yeast: Double, extras: List<DoughLine> = emptyList(),
    ): List<DoughLine> = buildList {
        addAll(flour)
        add(DoughLine(LineRole.WATER, water))
        if (salt > 0) add(DoughLine(LineRole.SALT, salt))
        if (oil > 0) add(DoughLine(LineRole.OIL, oil))
        if (yeast > 0) add(DoughLine(LineRole.YEAST, yeast))
        addAll(extras)
    }

    /** Rechenungenauigkeit beim Wasser des Vorteigs, in Gramm. */
    private const val TOLERANCE = 1e-9
}
