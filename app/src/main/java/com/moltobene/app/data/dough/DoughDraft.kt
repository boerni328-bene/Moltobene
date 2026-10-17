package com.moltobene.app.data.dough

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.TimelinePhase
import java.util.Locale
import kotlin.math.roundToLong

/** Zahlen für Zutatenzeilen (#69): kein Tausenderpunkt, Dezimalzeichen nach der Rezeptsprache, Runden je Zutat. */
object DoughNumbers {

    /** Menge in Gramm: Mehl und Wasser auf 1 g, Salz und Öl auf 0,5 g, Hefe auf 0,1 g; nie „0“, dann zwei Stellen. */
    fun grams(value: Double, role: LineRole, language: String?): String {
        val step = when (role) {
            LineRole.FLOUR, LineRole.WATER, LineRole.SEMOLINA, LineRole.EGG, LineRole.YOLK, LineRole.MILK, LineRole.LIQUID,
            LineRole.SUGAR, LineRole.BUTTER, LineRole.QUARK -> 1.0
            LineRole.SALT, LineRole.OIL, LineRole.BAKING_POWDER -> 0.5
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

    /** Zwischenüberschrift ohne Doppelpunkt, z. B. „Für den Vorteig (Biga)“. */
    fun heading(kind: SectionKind, preferment: Preferment): String

    fun ingredient(role: LineRole, yeastKind: YeastKind): String

    fun pastaTitle(kind: PastaKind): String

    fun cakeTitle(kind: CakeKind): String

    /** Einheit der Portionen ohne Anzahl, z. B. „Teigkugeln (je 250 g)“; [detail] ist das Gewicht, bei einer Form ihr Maß. */
    fun servingsUnit(kind: YieldKind, detail: String): String

    /** Text einer Phase im Zeitplan (#71), z. B. „Vorteig (Biga) reifen lassen“. */
    fun phase(kind: PhaseKind, preferment: Preferment): String

    /** Dauer als Text, z. B. „12 bis 72 Stunden“ oder „1 Stunde“ (bei gleicher [minMinutes] und [maxMinutes]). */
    fun duration(minMinutes: Int, maxMinutes: Int): String

    /** Ort der Phase als Zusatz zur Dauer, z. B. „im Kühlschrank“; leer ohne Ort. */
    fun place(place: PhasePlace): String
}

/**
 * Das Ergebnis des Teigrechners als normales Rezept (#69, „Als Rezept speichern“): Zutaten als Textzeilen mit der Menge
 * vorn und dem Anteil vom Mehl in Klammern, Zwischenüberschriften für Vorteig und Hauptteig, Portionsangabe wie
 * „6 Teigkugeln (je 250 g)“, damit „Umrechnen …“ und das Teilen sie verstehen. Bei Pizza und Brot kommt der Zeitplan (#71) dazu, auch als Zubereitung; bei Pasta und Kuchen bleibt die Zubereitung leer.
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
        /** @param language Sprache der Oberfläche; in ihr werden Zahlen und Zeilen geschrieben und als Rezeptsprache gesetzt */
        fun from(
            result: DoughResult.Success,
            input: LeavenedInput,
            texts: DoughTexts,
            language: String,
        ): DoughDraft {
            val draft = build(
                result, texts, language, input.yeastKind, texts.title(input.type, input.preferment),
                DoughNumbers.separator(DoughNumbers.plain(result.yield.pieceGrams, 1), language),
            )
            val timeline = DoughTimeline.build(input, texts)
            return draft.copy(timeline = timeline, steps = timeline.map { stepText(it, texts) })
        }

        /** Eine Phase als Schritt der Zubereitung, z. B. „Biga reifen lassen (1 bis 3 Tage, im Kühlschrank)“. */
        fun stepText(phase: TimelinePhase, texts: DoughTexts): String {
            val place = texts.place(phase.place)
            val detail = texts.duration(phase.minMinutes, phase.maxMinutes) + if (place.isEmpty()) "" else ", $place"
            return "${phase.text} ($detail)"
        }

        fun fromPasta(result: DoughResult.Success, input: PastaInput, texts: DoughTexts, language: String): DoughDraft = build(
            result, texts, language, YeastKind.FRESH, texts.pastaTitle(input.kind),
            DoughNumbers.separator(DoughNumbers.plain(result.yield.pieceGrams, 1), language),
        )

        /** Bei einer Form steht ihr Maß in der Einheit, damit „Backform umrechnen“ sie wieder erkennt. */
        fun fromCake(result: DoughResult.Success, input: CakeInput, texts: DoughTexts, language: String): DoughDraft {
            val detail = (input.size as? CakeSize.PanSize)?.let { panText(it.pan, language) }.orEmpty()
            return build(result, texts, language, input.yeastKind, texts.cakeTitle(input.kind), detail)
        }

        private fun build(
            result: DoughResult.Success,
            texts: DoughTexts,
            language: String,
            yeastKind: YeastKind,
            title: String,
            detail: String,
        ): DoughDraft {
            val lines = mutableListOf<Ingredient>()
            val sections = result.sections
            for (section in sections) {
                if (sections.size > 1) lines += Ingredient(text = texts.heading(section.kind, section.preferment), isHeading = true)
                for (line in section.lines) {
                    val amount = DoughNumbers.grams(line.grams, line.role, language)
                    val percent = DoughNumbers.percent(line.grams / result.flourGrams * 100, language)
                    lines += Ingredient("$amount g ${texts.ingredient(line.role, yeastKind)} ($percent %)")
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
