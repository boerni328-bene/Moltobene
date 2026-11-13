package com.moltobene.app.data.dough

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.TemperatureUnit
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.data.WeightUnit
import java.util.Locale
import kotlin.math.roundToLong

/** Zahlen für Zutatenzeilen (#69): kein Tausenderpunkt, Dezimalzeichen nach der Rezeptsprache, Runden je Zutat. */
object DoughNumbers {

    /** Menge in Gramm: Mehl und Wasser auf 1 g, Salz und Öl auf 0,5 g, Hefe auf 0,1 g; nie „0“, dann zwei Stellen. */
    fun grams(value: Double, role: LineRole, language: String?): String {
        val step = when (role) {
            LineRole.FLOUR, LineRole.WATER, LineRole.SEMOLINA, LineRole.EGG, LineRole.YOLK, LineRole.MILK, LineRole.LIQUID,
            LineRole.SUGAR, LineRole.BUTTER, LineRole.QUARK, LineRole.YOGURT, LineRole.WHEAT, LineRole.RYE, LineRole.WHOLE,
            LineRole.SPELT, LineRole.SEEDS -> 1.0
            LineRole.SALT, LineRole.OIL, LineRole.BAKING_POWDER, LineRole.MALT, LineRole.HONEY -> 0.5
            LineRole.YEAST -> 0.1
        }
        val rounded = (value / step).roundToLong() * step
        val text = when {
            value > 0 && rounded <= 0 -> plain(value, 2)
            step >= 1.0 -> rounded.roundToLong().toString()
            else -> plain(rounded, 1)
        }
        return separator(text, language)
    }

    /** Menge einer Zutat mit Einheit, z. B. „595 g“ oder „21 oz“. */
    fun weight(value: Double, role: LineRole, language: String?, unit: WeightUnit): String =
        if (unit == WeightUnit.GRAMS) "${grams(value, role, language)} g" else ounces(value, language)

    /** Gewicht ohne Zutat (Gesamtgewicht, Stück) mit höchstens [decimals] Stellen in Gramm, z. B. „1000 g“ oder „35,3 oz“. */
    fun weight(grams: Double, decimals: Int, language: String?, unit: WeightUnit): String =
        if (unit == WeightUnit.GRAMS) separator(plain(grams, decimals), language) + " g" else ounces(grams, language)

    /** Unzen: ab einer Unze eine Stelle, darunter zwei (z. B. 1,2 g Hefe = 0,04 oz). */
    private fun ounces(grams: Double, language: String?): String {
        val ounces = WeightUnit.OUNCES.fromGrams(grams)
        return separator(plain(ounces, if (ounces >= 1) 1 else 2), language) + " oz"
    }

    /** Anteil in Prozent vom gesamten Mehl mit höchstens einer Stelle, z. B. „30“ oder „0,5“. */
    fun percent(value: Double, language: String?): String = separator(plain(value, 1), language)

    /** Eine Zahl mit höchstens [decimals] Stellen ohne überflüssige Nullen. */
    fun plain(value: Double, decimals: Int): String =
        "%.${decimals}f".format(Locale.ROOT, value).let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }

    /** Dezimalzeichen nach der Rezeptsprache: Komma im Deutschen, Italienischen, Französischen, Spanischen. */
    fun separator(text: String, language: String?): String = if (language == "en") text else text.replace('.', ',')
}

/** Texte des Entwurfs aus strings.xml der Oberfläche; im Test feste Texte. */
interface DoughTexts {
    fun title(type: DoughType, preferment: Preferment): String

    /**
     * Zwischenüberschrift ohne Doppelpunkt mit dem Gewicht vorn (Wunsch vom 05.11.2026), z. B. „Für den Vorteig (277 g
     * Biga)“ oder „Für den Hauptteig (1000 g)“.
     */
    fun heading(kind: SectionKind, preferment: Preferment, amount: String): String

    /** Erste Zeile des Hauptteigs: der ganze Vorteig, z. B. „277 g Biga (der ganze Vorteig)“. */
    fun wholePreferment(preferment: Preferment, amount: String): String

    /** Name des Vorteigs, z. B. „Biga“. */
    fun prefermentName(preferment: Preferment): String

    fun ingredient(role: LineRole, yeastKind: YeastKind): String

    fun pastaTitle(kind: PastaKind): String

    fun cakeTitle(kind: CakeKind): String

    /** Einheit der Portionen ohne Anzahl, z. B. „Teigkugeln (je 250 g)“; [detail] ist das Gewicht, bei einer Form ihr Maß. */
    fun servingsUnit(kind: YieldKind, detail: String): String

    /** Text einer Phase im Zeitplan (#71), z. B. „Biga reifen lassen“ oder bei Pizza „Teigkugeln gehen lassen“. */
    fun phase(kind: PhaseKind, type: DoughType, preferment: Preferment): String

    /** Dauer als Text, z. B. „12 bis 72 Stunden“ oder „1 Stunde“ (bei gleicher [minMinutes] und [maxMinutes]). */
    fun duration(minMinutes: Int, maxMinutes: Int): String

    /** Ort mit Temperatur, z. B. „im Kühlschrank, 5 °C“; leer ohne Ort. */
    fun placeAt(place: PhasePlace, temperature: String): String

    /** Aufzählung, z. B. „184 g Mehl, 92 g Wasser und 1,2 g Frischhefe“. */
    fun join(parts: List<String>): String

    /** Anleitung zu einem Schritt der Zubereitung (Wunsch vom 05.11.2026), z. B. „Abgedeckt kalt stellen. Fertig, wenn …“. */
    fun stepDetail(detail: StepDetail): String
}

/**
 * Das Ergebnis des Teigrechners als normales Rezept (#69, „Als Rezept speichern“): Zutaten als Textzeilen mit der Menge
 * vorn und dem Anteil vom Mehl in Klammern, Zwischenüberschriften für Vorteig und Hauptteig mit dem Gewicht, im
 * Hauptteig zuerst der ganze Vorteig und beim Wasser die Temperatur (Wunsch vom 05.11.2026), Portionsangabe wie
 * „6 Teigkugeln (je 250 g)“, damit „Umrechnen …“ und das Teilen sie verstehen. Bei Pizza und Brot kommt der Zeitplan
 * (#71) mit den gewählten Gehzeiten dazu und als Zubereitung je Schritt eine Anleitung mit Mengen und Temperaturen;
 * bei Pasta und Kuchen bleibt die Zubereitung leer.
 */
data class DoughDraft(
    val title: String,
    val language: String,
    val servings: Int,
    val servingsUnit: String,
    val ingredients: List<Ingredient>,
    /** Zeitplan (#71) und seine Schritte als Text; leer für Pasta und Kuchen. */
    val timeline: List<TimelinePhase> = emptyList(),
    val steps: List<String> = emptyList(),
) {
    /** Ein Entwurf mit der Quelle „eigenes Rezept“. */
    fun toRecipe(id: String, now: Long): Recipe = Recipe(
        id = id,
        title = title,
        language = language,
        servings = servings,
        servingsUnit = servingsUnit,
        source = RecipeSource(SourceType.OWN),
        ingredients = ingredients,
        steps = steps,
        timeline = timeline,
        isDraft = true,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        /**
         * @param language Sprache der Oberfläche; in ihr werden Zahlen und Zeilen geschrieben und als Rezeptsprache gesetzt
         * @param process Gehzeiten und Temperaturen wie eingestellt; wirksam werden sie über [DoughProcessRules.effective]
         */
        @Suppress("LongParameterList")
        fun from(
            result: DoughResult.Success,
            input: LeavenedInput,
            texts: DoughTexts,
            language: String,
            unit: WeightUnit = WeightUnit.GRAMS,
            process: DoughProcess = DoughProcessRules.defaults(input.type, input.preferment),
            kitchen: DoughKitchen = DoughKitchen(),
            temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        ): DoughDraft {
            val water = DoughWater.temperature(result, input.preferment, kitchen, process.doughTemp)
            val waterText = water?.let { temperatureUnit.format(it.coerceAtLeast(0.0)) }
            val draft = build(
                result, texts, language, input.yeastKind, texts.title(input.type, input.preferment),
                DoughNumbers.weight(result.yield.pieceGrams, 1, language, unit), unit, waterText,
            )
            val effective = DoughProcessRules.effective(process, kitchen, input.type, input.preferment)
            val plan = DoughTimeline.plan(input.type, input.preferment, effective, kitchen.kneading)
            val timeline = plan.map { TimelinePhase(texts.phase(it.kind, input.type, input.preferment), it.min, it.max, it.place) }
            val steps = DoughSteps.build(plan, timeline, result, input, effective, kitchen, waterText, texts, language, unit, temperatureUnit)
            return draft.copy(timeline = timeline, steps = steps)
        }

        fun fromPasta(
            result: DoughResult.Success,
            input: PastaInput,
            texts: DoughTexts,
            language: String,
            unit: WeightUnit = WeightUnit.GRAMS,
        ): DoughDraft = build(
            result, texts, language, YeastKind.FRESH, texts.pastaTitle(input.kind),
            DoughNumbers.weight(result.yield.pieceGrams, 1, language, unit), unit,
        )

        /** Bei einer Form steht ihr Maß in der Einheit, damit „Backform umrechnen“ sie wieder erkennt. */
        fun fromCake(
            result: DoughResult.Success,
            input: CakeInput,
            texts: DoughTexts,
            language: String,
            unit: WeightUnit = WeightUnit.GRAMS,
        ): DoughDraft {
            val detail = (input.size as? CakeSize.PanSize)?.let { panText(it.pan, language) }.orEmpty()
            return build(result, texts, language, input.yeastKind, texts.cakeTitle(input.kind), detail, unit)
        }

        /** [waterText]: Temperatur des Wassers, das zum Kneten dazukommt (nicht das des Vorteigs); null ohne. */
        @Suppress("LongParameterList")
        private fun build(
            result: DoughResult.Success,
            texts: DoughTexts,
            language: String,
            yeastKind: YeastKind,
            title: String,
            detail: String,
            unit: WeightUnit,
            waterText: String? = null,
        ): DoughDraft {
            val lines = mutableListOf<Ingredient>()
            val sections = result.sections
            val prefermentGrams = DoughSummary.prefermentGrams(result)
            for (section in sections) {
                if (sections.size > 1) {
                    val grams = if (section.kind == SectionKind.PREFERMENT) prefermentGrams else result.totalGrams
                    val amount = DoughNumbers.weight(grams, 0, language, unit)
                    lines += Ingredient(text = texts.heading(section.kind, section.preferment, amount), isHeading = true)
                    if (section.kind == SectionKind.MAIN && section.preferment != Preferment.NONE && prefermentGrams > 0) {
                        lines += Ingredient(texts.wholePreferment(section.preferment, DoughNumbers.weight(prefermentGrams, 0, language, unit)))
                    }
                }
                for (line in section.lines) {
                    val amount = DoughNumbers.weight(line.grams, line.role, language, unit)
                    val percent = DoughNumbers.percent(line.grams / result.flourGrams * 100, language)
                    val warm = if (line.role == LineRole.WATER && section.kind != SectionKind.PREFERMENT && waterText != null) ", $waterText" else ""
                    lines += Ingredient("$amount ${texts.ingredient(line.role, yeastKind)} ($percent %)$warm")
                }
            }
            return DoughDraft(
                title = title,
                language = language,
                servings = result.yield.count,
                servingsUnit = texts.servingsUnit(result.yield.kind, detail),
                ingredients = lines,
            )
        }
    }
}
