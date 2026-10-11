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
        val flour = total / (1 + water + salt + oil + yeast)

        val sections = if (input.preferment == Preferment.NONE) {
            listOf(
                DoughSection(
                    SectionKind.PLAIN, Preferment.NONE,
                    lines(flour, water * flour, salt * flour, oil * flour, yeast * flour),
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
                    lines(prefermentFlour, prefermentWater, 0.0, 0.0, if (yeastInPreferment) yeast * flour else 0.0),
                ),
                DoughSection(
                    SectionKind.MAIN, input.preferment,
                    lines(
                        flour - prefermentFlour, mainWater.coerceAtLeast(0.0), salt * flour, oil * flour,
                        if (yeastInPreferment) 0.0 else yeast * flour,
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
    private fun lines(flour: Double, water: Double, salt: Double, oil: Double, yeast: Double): List<DoughLine> = buildList {
        add(DoughLine(LineRole.FLOUR, flour))
        add(DoughLine(LineRole.WATER, water))
        if (salt > 0) add(DoughLine(LineRole.SALT, salt))
        if (oil > 0) add(DoughLine(LineRole.OIL, oil))
        if (yeast > 0) add(DoughLine(LineRole.YEAST, yeast))
    }

    /** Rechenungenauigkeit beim Wasser des Vorteigs, in Gramm. */
    private const val TOLERANCE = 1e-9
}
