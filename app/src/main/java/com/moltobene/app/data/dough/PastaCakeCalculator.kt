package com.moltobene.app.data.dough

import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import kotlin.math.abs

/**
 * Pasta (#70): F = Portionen × Mehl je Portion; Grieß, Ei, Eigelb, Wasser, Salz und Öl sind Anteile von F. Rein,
 * ohne Ausnahmen nach außen: Ungültiges ergibt [DoughResult.Invalid], nie eine negative Menge oder „NaN“.
 */
object PastaCalculator {

    fun calculate(input: PastaInput): DoughResult {
        val issues = validate(input)
        if (issues.isNotEmpty()) return DoughResult.Invalid(issues)
        val flour = input.portions * input.flourPerPortion
        val semolina = flour * input.semolina / 100
        val lines = buildList {
            if (semolina > 0) add(DoughLine(LineRole.SEMOLINA, semolina))
            if (flour - semolina > 0) add(DoughLine(LineRole.FLOUR, flour - semolina))
            if (input.kind == PastaKind.EGG) {
                if (input.egg > 0) add(DoughLine(LineRole.EGG, flour * input.egg / 100))
                if (input.yolk > 0) add(DoughLine(LineRole.YOLK, flour * input.yolk / 100))
            }
            if (input.water > 0) add(DoughLine(LineRole.WATER, flour * input.water / 100))
            if (input.salt > 0) add(DoughLine(LineRole.SALT, flour * input.salt / 100))
            if (input.oil > 0) add(DoughLine(LineRole.OIL, flour * input.oil / 100))
        }
        return finish(lines, DoughYield(input.portions, YieldKind.PORTION, input.flourPerPortion), flour)
    }

    fun validate(input: PastaInput): List<DoughIssue> {
        val issues = mutableListOf<DoughIssue>()
        fun check(field: DoughField, value: Double) = checkRange(issues, field, value, PastaLimits.range(input.kind, field))
        check(DoughField.COUNT, input.portions.toDouble())
        check(DoughField.FLOUR_PER_PORTION, input.flourPerPortion)
        check(DoughField.SEMOLINA, input.semolina)
        check(DoughField.WHOLE_EGG, input.egg)
        check(DoughField.YOLK, input.yolk)
        check(DoughField.WATER, input.water)
        check(DoughField.SALT, input.salt)
        check(DoughField.OIL, input.oil)
        if (issues.isEmpty() && input.kind == PastaKind.EGG && input.egg + input.yolk < PastaLimits.MIN_EGG_TOTAL) {
            issues += DoughIssue.TooLittleLiquid(PastaLimits.MIN_EGG_TOTAL)
        }
        return issues
    }
}

/**
 * Kuchenteig (#70): F kommt aus der Backform (Bezugsmehl der Art für Ø 26 cm mal Faktor der Fläche, wie bei „Backform
 * umrechnen“) oder wird direkt eingegeben; alle anderen Mengen sind Anteile von F. Nur Richtwerte.
 */
object CakeCalculator {

    fun calculate(input: CakeInput): DoughResult {
        val issues = validate(input)
        if (issues.isNotEmpty()) return DoughResult.Invalid(issues)
        val flour = flourGrams(input) ?: return DoughResult.Invalid(listOf(DoughIssue.Inconsistent))
        val kind = input.kind
        fun part(field: CakeField) = if (field in CakeLimits.fields(kind)) flour * CakeLimits.value(input, field) / 100 else 0.0
        val lines = buildList {
            add(DoughLine(LineRole.FLOUR, flour))
            part(CakeField.FAT).takeIf { it > 0 }?.let {
                add(DoughLine(if (kind == CakeKind.QUARK_OIL) LineRole.OIL else LineRole.BUTTER, it))
            }
            part(CakeField.SUGAR).takeIf { it > 0 }?.let { add(DoughLine(LineRole.SUGAR, it)) }
            part(CakeField.EGG).takeIf { it > 0 }?.let { add(DoughLine(LineRole.EGG, it)) }
            part(CakeField.QUARK).takeIf { it > 0 }?.let { add(DoughLine(LineRole.QUARK, it)) }
            part(CakeField.MILK).takeIf { it > 0 }?.let {
                add(DoughLine(if (kind == CakeKind.CHOUX) LineRole.LIQUID else LineRole.MILK, it))
            }
            part(CakeField.YEAST).takeIf { it > 0 }?.let { add(DoughLine(LineRole.YEAST, it)) }
            part(CakeField.BAKING_POWDER).takeIf { it > 0 }?.let { add(DoughLine(LineRole.BAKING_POWDER, it)) }
            part(CakeField.SALT).takeIf { it > 0 }?.let { add(DoughLine(LineRole.SALT, it)) }
        }
        val yield = when (val size = input.size) {
            is CakeSize.PanSize -> DoughYield(1, YieldKind.PAN, size.pan.area)
            is CakeSize.FlourGrams -> DoughYield(1, YieldKind.DOUGH, 0.0)
        }
        return finish(lines, yield, flour)
    }

    /** Mehl in Gramm; null, wenn die Form ungültig ist ([validate] meldet das vorher). */
    fun flourGrams(input: CakeInput): Double? = when (val size = input.size) {
        is CakeSize.FlourGrams -> size.grams
        is CakeSize.PanSize ->
            PanScaling.factor(DoughConstants.REFERENCE_PAN, size.pan)?.let { CakeLimits.referenceFlour(input.kind) * it }
    }

    fun validate(input: CakeInput): List<DoughIssue> {
        val issues = mutableListOf<DoughIssue>()
        for (field in CakeLimits.fields(input.kind)) {
            checkRange(issues, field.toDoughField(), CakeLimits.value(input, field), CakeLimits.range(input.kind, field))
        }
        val flourRange = CakeLimits.flourRange(input.kind)
        when (val size = input.size) {
            is CakeSize.FlourGrams -> checkRange(issues, DoughField.FLOUR_GRAMS, size.grams, flourRange)
            is CakeSize.PanSize -> if (!PanScaling.isValid(size.pan)) {
                issues += DoughIssue.PanInvalid
            } else {
                val flour = flourGrams(input)
                if (flour == null) {
                    issues += DoughIssue.OutOfRange(DoughField.FLOUR_GRAMS, flourRange.min, flourRange.max)
                } else {
                    checkRange(issues, DoughField.FLOUR_GRAMS, flour, flourRange)
                }
            }
        }
        return issues
    }

    private fun CakeField.toDoughField() = when (this) {
        CakeField.FAT -> DoughField.FAT
        CakeField.SUGAR -> DoughField.SUGAR
        CakeField.EGG -> DoughField.EGG
        CakeField.MILK -> DoughField.MILK
        CakeField.QUARK -> DoughField.QUARK
        CakeField.YEAST -> DoughField.YEAST
        CakeField.BAKING_POWDER -> DoughField.BAKING_POWDER
        CakeField.SALT -> DoughField.SALT
    }
}

private fun checkRange(issues: MutableList<DoughIssue>, field: DoughField, value: Double, range: DoughLimits.Range) {
    when {
        !value.isFinite() -> issues += DoughIssue.NotANumber(field)
        value !in range -> issues += DoughIssue.OutOfRange(field, range.min, range.max)
    }
}

/** Selbstprüfung: alles endlich und nicht negativ, Summe der Zutaten gleich dem Gewicht; höchstens 100 kg. */
private fun finish(lines: List<DoughLine>, yield: DoughYield, flour: Double): DoughResult {
    val total = lines.sumOf { it.grams }
    val sound = lines.none { !it.grams.isFinite() || it.grams < 0 } && total.isFinite() && flour.isFinite() &&
        total <= DoughLimits.MAX_TOTAL_GRAMS && abs(lines.sumOf { it.grams } - total) <= total * 1e-9 + 1e-9
    if (!sound) return DoughResult.Invalid(listOf(DoughIssue.Inconsistent))
    return DoughResult.Success(listOf(DoughSection(SectionKind.PLAIN, Preferment.NONE, lines)), yield, total, flour)
}

/** Text der Form für die Portionsangabe, z. B. „Ø 26 cm“ oder „30 × 40 cm“, nach dem Dezimalzeichen der Sprache. */
fun panText(pan: Pan, language: String?): String {
    fun cm(value: Double) = DoughNumbers.separator(DoughNumbers.plain(value, 1), language)
    return if (pan.round) "Ø ${cm(pan.a)} cm" else "${cm(pan.a)} × ${cm(pan.b)} cm"
}
