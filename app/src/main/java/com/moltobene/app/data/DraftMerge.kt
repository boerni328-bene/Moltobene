package com.moltobene.app.data

import com.moltobene.app.data.ocr.ParsedRecipe
import java.util.Locale

/** Die Felder eines Rezept-Entwurfs, so wie sie in den Eingabefeldern stehen. */
data class DraftText(
    val title: String = "",
    val servings: String = "",
    val servingsUnit: String = "",
    val ingredients: String = "",
    val steps: String = "",
    /** Vollständiger übernommener Text (Originaltext). */
    val originalText: String = "",
    val language: String? = null,
)

/**
 * Übernimmt einen erkannten oder geteilten Text in einen Entwurf (#42) – für die Texterkennung und
 * später für weitere Erfassungswege. Was schon im Entwurf steht, wird nie überschrieben:
 * - Leere Felder (Titel, Portionen) werden ausgefüllt.
 * - Zutaten und Schritte werden ergänzt. Steht derselbe Abschnitt schon Zeile für Zeile genauso da
 *   (z. B. dieselbe Seite noch einmal gelesen), kommt er nicht doppelt dazu. Verglichen werden ganze Zeilen:
 *   „Salz“ wird also auch ergänzt, wenn schon „1 TL Salz“ dasteht. Einzelne Zeilen werden nie weggelassen,
 *   denn eine Zutat wie „1 Prise Salz“ kann in Teig und Füllung vorkommen.
 * - Der vollständige Text kommt zum Originaltext, außer er steht dort schon genauso.
 * - Die Sprache wird nur gesetzt, wenn sie eindeutig erkannt wurde und noch keine feststeht.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object DraftMerge {

    /**
     * @param text der vollständige übernommene Text
     * @param parsed seine Aufteilung in Titel, Portionen, Zutaten und Zubereitung
     * @param language eindeutig erkannte Sprache des Textes; null, wenn sie unklar ist
     */
    fun merge(draft: DraftText, text: String, parsed: ParsedRecipe, language: String?): DraftText = draft.copy(
        title = draft.title.ifBlank { parsed.title.orEmpty() },
        servings = draft.servings.ifBlank { parsed.servings?.takeIf { it in 1..MAX_SERVINGS }?.toString().orEmpty() },
        servingsUnit = draft.servingsUnit.ifBlank { parsed.servingsUnit.orEmpty() },
        ingredients = appendLines(draft.ingredients, parsed.ingredients),
        steps = appendLines(draft.steps, parsed.steps),
        originalText = appendText(draft.originalText, text.trim()),
        language = draft.language ?: language,
    )

    /** Hängt [lines] an [current] an, außer sie stehen dort schon Zeile für Zeile in dieser Reihenfolge. */
    fun appendLines(current: String, lines: List<String>): String {
        val added = lines.map { key(it) }.filter { it.isNotEmpty() }
        if (added.isEmpty()) return current
        val existing = current.lines().map { key(it) }.filter { it.isNotEmpty() }
        if (existing.windowed(added.size).any { it == added }) return current
        return listOf(current.trimEnd(), lines.joinToString("\n")).filter { it.isNotEmpty() }.joinToString("\n")
    }

    /** Hängt [text] als eigenen Absatz an, außer er steht schon als ganzer Absatz da (z. B. dieselbe Seite noch einmal gelesen). */
    fun appendText(current: String, text: String): String {
        if (text.isEmpty()) return current
        val existing = current.trim()
        if (existing.isEmpty()) return text
        val alreadyThere = existing == text ||
            existing.startsWith(text + PARAGRAPH) ||
            existing.endsWith(PARAGRAPH + text) ||
            existing.contains(PARAGRAPH + text + PARAGRAPH)
        return if (alreadyThere) current else existing + PARAGRAPH + text
    }

    /** Vergleich ohne Rücksicht auf Leerzeichen am Rand und Groß-/Kleinschreibung. */
    private fun key(line: String): String = line.trim().lowercase(Locale.ROOT)

    private const val PARAGRAPH = "\n\n"
    private const val MAX_SERVINGS = 999
}
