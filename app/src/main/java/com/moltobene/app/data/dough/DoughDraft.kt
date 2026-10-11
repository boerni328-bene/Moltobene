package com.moltobene.app.data.dough

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import java.util.Locale
import kotlin.math.roundToLong

/** Zahlen für Zutatenzeilen (#69): kein Tausenderpunkt, Dezimalzeichen nach der Rezeptsprache, Runden je Zutat. */
object DoughNumbers {

    /** Menge in Gramm: Mehl und Wasser auf 1 g, Salz und Öl auf 0,5 g, Hefe auf 0,1 g; nie „0“, dann zwei Stellen. */
    fun grams(value: Double, role: LineRole, language: String?): String {
        val step = when (role) {
            LineRole.FLOUR, LineRole.WATER -> 1.0
            LineRole.SALT, LineRole.OIL -> 0.5
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

    /** Einheit der Portionen ohne Anzahl, z. B. „Teigkugeln (je 250 g)“. */
    fun servingsUnit(kind: YieldKind, pieceGrams: String): String
}

/**
 * Das Ergebnis des Teigrechners als normales Rezept (#69, „Als Rezept speichern“): Zutaten als Textzeilen mit der Menge
 * vorn und dem Anteil vom Mehl in Klammern, Zwischenüberschriften für Vorteig und Hauptteig, Portionsangabe wie
 * „6 Teigkugeln (je 250 g)“, damit „Umrechnen …“ und das Teilen sie verstehen. Die Zubereitung bleibt leer.
 */
data class DoughDraft(
    val title: String,
    val language: String,
    val servings: Int,
    val servingsUnit: String,
    val ingredients: List<Ingredient>,
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
            val lines = mutableListOf<Ingredient>()
            val sections = result.sections
            for (section in sections) {
                if (sections.size > 1) lines += Ingredient(text = texts.heading(section.kind, section.preferment), isHeading = true)
                for (line in section.lines) {
                    val amount = DoughNumbers.grams(line.grams, line.role, language)
                    val percent = DoughNumbers.percent(line.grams / result.flourGrams * 100, language)
                    lines += Ingredient("$amount g ${texts.ingredient(line.role, input.yeastKind)} ($percent %)")
                }
            }
            val unit = texts.servingsUnit(result.yield.kind, DoughNumbers.plain(result.yield.pieceGrams, 1).let { DoughNumbers.separator(it, language) })
            return DoughDraft(
                title = texts.title(input.type, input.preferment),
                language = language,
                servings = result.yield.count,
                servingsUnit = unit,
                ingredients = lines,
            )
        }
    }
}
