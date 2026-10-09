package com.moltobene.app.data.translate

import com.moltobene.app.data.Recipe
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/** Übersetzt eine einzelne Zeile, z. B. mit [MarianTranslator]; im Rundgang nachgestellt. */
fun interface LineTranslator {
    fun translate(text: String): String
}

/**
 * Übersetzung eines Rezepts (#60), zusätzlich zum Original gespeichert. Nur Titel, Einheit der Portionen, Zutaten
 * und Schritte – Notizen sind eigene Eingaben und werden nie übersetzt, Quelle und Schlagwörter auch nicht.
 * Zutaten und Schritte stehen an derselben Stelle wie im Original, damit Abhaken und Portionen umrechnen gleich
 * funktionieren.
 *
 * @param sourceHash Fingerabdruck des Originals; ändert sich das Original, wird neu übersetzt
 */
@Serializable
data class TranslatedRecipe(
    val language: String,
    val sourceHash: String,
    val title: String,
    val servingsUnit: String? = null,
    val ingredients: List<String>,
    val steps: List<String>,
)

/**
 * Übersetzt ein Rezept Zeile für Zeile (#60): erst das Küchenwörterbuch, dann das Modell, dann die Schutzregeln –
 * eine Zeile, die sie nicht erfüllt, bleibt im Original stehen. Lange Schritte werden Satz für Satz übersetzt
 * (schneller und genauer). Reines Kotlin, per Unit-Test prüfbar.
 */
object RecipeTranslator {

    /** Erhöhen, wenn sich Wörterbuch, Schutzregeln oder Paket ändern: Dann wird neu übersetzt. */
    const val VERSION = 3

    /** Fingerabdruck von allem, was übersetzt wird, samt Ziel- und Ausgangssprache. */
    fun sourceHash(recipe: Recipe, from: String, to: String): String {
        val text = buildString {
            append(VERSION).append('|').append(LanguagePack.VERSION).append('|').append(from).append('>').append(to).append('\n')
            append(recipe.title).append('\n')
            append(recipe.servingsUnit.orEmpty()).append('\n')
            recipe.ingredients.forEach { append(if (it.isHeading) "#" else "-").append(it.text).append('\n') }
            append('\n')
            recipe.steps.forEach { append(it).append('\n') }
        }
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    /** Zahl der Zeilen und Sätze für die Fortschrittsanzeige. */
    fun workCount(recipe: Recipe): Int =
        1 + (if (recipe.servingsUnit.isNullOrBlank()) 0 else 1) + recipe.ingredients.size + recipe.steps.sumOf { sentences(it).size }

    /**
     * @param check wird vor jeder Zeile aufgerufen; wirft, wenn abgebrochen wurde
     * @param onProgress erledigte Zeilen und Sätze von [workCount]
     */
    fun translate(
        recipe: Recipe,
        from: String,
        to: String,
        translator: LineTranslator,
        check: () -> Unit = {},
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): TranslatedRecipe {
        val direction = requireNotNull(LanguagePack.direction(from, to)) { "Keine Übersetzung $from → $to" }
        val total = workCount(recipe)
        var done = 0
        fun line(text: String): String {
            check()
            val result = translateLine(text, direction, translator)
            onProgress(++done, total)
            return result
        }
        return TranslatedRecipe(
            language = to,
            sourceHash = sourceHash(recipe, from, to),
            title = line(recipe.title),
            servingsUnit = recipe.servingsUnit?.takeIf { it.isNotBlank() }?.let(::line),
            ingredients = recipe.ingredients.map { line(it.text) },
            steps = recipe.steps.map { step -> sentences(step).joinToString(" ") { line(it) } },
        )
    }

    /** Eine Zeile mit Wörterbuch und Schutzregeln; ohne Buchstaben (z. B. nur „200 g“) bleibt sie, wie sie ist. */
    internal fun translateLine(text: String, direction: String, translator: LineTranslator): String {
        if (text.isBlank() || text.none { it.isLetter() }) return text
        val prepared = KitchenGlossary.apply(text, direction)
        return TranslationGuard.choose(text, translator.translate(prepared))
    }

    /**
     * Teilt einen Schritt in Sätze: nach „.“, „!“ oder „?“, wenn danach ein Großbuchstabe oder eine Zahl kommt –
     * aber nicht nach Abkürzungen wie „ca.“, „z. B.“ oder „tbsp.“.
     */
    internal fun sentences(text: String): List<String> {
        val trimmed = text.trim()
        val result = mutableListOf<String>()
        var start = 0
        SENTENCE_END.findAll(trimmed).forEach { match ->
            val before = trimmed.substring(start, match.range.first)
            val lastWord = before.trimEnd('.', '!', '?').substringAfterLast(' ').substringAfterLast('(')
            if (lastWord.length > 1 && lastWord.lowercase() !in ABBREVIATIONS) {
                result += before.trim()
                start = match.range.last + 1
            }
        }
        result += trimmed.substring(start).trim()
        return result.filter { it.isNotEmpty() }.ifEmpty { listOf(text) }
    }

    private val SENTENCE_END = Regex("(?<=[.!?])\\s+(?=[\\p{Lu}\\d])")
    private val ABBREVIATIONS = setOf(
        "ca", "bzw", "evtl", "ggf", "etc", "usw", "inkl", "std", "min", "sek", "msp", "pck", "pkg", "tl", "el", "gr",
        "approx", "hr", "hrs", "mins", "sec", "tbsp", "tsp", "oz", "lb", "lbs", "pt", "qt", "no", "vs", "st", "dr",
    )
}
