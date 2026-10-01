package com.moltobene.app.data.ocr

/** Ein erkanntes Wort und wie sicher sich die Texterkennung dabei ist (0–100). */
data class OcrWord(val text: String, val confidence: Int)

/**
 * Setzt den Text aus dem Ergebnis von Tesseract im Format hOCR zusammen und lässt dabei weg, was
 * die Texterkennung nur aus Symbolen, Knöpfen oder Fotos „gelesen“ hat: Wörter und Zeilen mit sehr
 * geringer Sicherheit sowie einzelne Zeichen wie „@“ oder „|“, die von Symbolen stammen.
 * Zeilen werden mit einem Zeilenumbruch getrennt, Absätze mit einer Leerzeile (wie bei getUTF8Text).
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object HocrText {

    /** Darunter ist ein Wort am Zeilenrand oder ein kurzes Wort fast immer ein Bildrest. */
    private const val MIN_WORD_CONFIDENCE = 30

    /** Darunter ist eine ganze Zeile im Mittel unbrauchbar. */
    private const val MIN_LINE_CONFIDENCE = 50

    /** Wortfetzen („A Wa u“) werden verworfen, wenn sie im Mittel darunter liegen. */
    private const val SALAD_CONFIDENCE = 70

    private val TOKEN = Regex(
        "<p class='ocr_par'|<span class='ocr_(?:line|caption|header|textfloat)'|" +
            "<span class='ocrx_word'[^>]*?x_wconf (\\d+)[^>]*>(.*?)</span>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val TAG = Regex("<[^>]+>")

    /** Einzelne Zeichen, die von Symbolen stammen (Drucker, Herz, Teilen …) und in Rezepten nicht allein stehen. */
    private val SYMBOL_ONLY = Regex("^[@©®™\\\\|=<>{}\\[\\]~^_»«()]+$")

    /** Absätze → Zeilen → Wörter, in der Reihenfolge des Textes. */
    fun parse(hocr: String): List<List<List<OcrWord>>> {
        val paragraphs = mutableListOf<MutableList<MutableList<OcrWord>>>()
        for (match in TOKEN.findAll(hocr)) {
            val token = match.value
            when {
                token.startsWith("<p ") -> paragraphs += mutableListOf<MutableList<OcrWord>>()
                token.startsWith("<span class='ocr_") -> {
                    if (paragraphs.isEmpty()) paragraphs += mutableListOf<MutableList<OcrWord>>()
                    paragraphs.last() += mutableListOf<OcrWord>()
                }
                else -> {
                    val text = unescape(match.groupValues[2].replace(TAG, "")).trim()
                    if (text.isEmpty()) continue
                    if (paragraphs.isEmpty()) paragraphs += mutableListOf<MutableList<OcrWord>>()
                    if (paragraphs.last().isEmpty()) paragraphs.last() += mutableListOf<OcrWord>()
                    paragraphs.last().last() += OcrWord(text, match.groupValues[1].toInt())
                }
            }
        }
        return paragraphs
    }

    /** Text ohne Bildreste. */
    fun compose(hocr: String): String =
        parse(hocr)
            .map { paragraph -> paragraph.mapNotNull { line -> cleanLine(line) } }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { it.joinToString("\n") }

    private fun cleanLine(line: List<OcrWord>): String? {
        if (line.isEmpty() || line.sumOf { it.confidence } < MIN_LINE_CONFIDENCE * line.size) return null
        var words = line.filterNot { SYMBOL_ONLY.matches(it.text) }
        // Unsichere Wörter am Rand (z. B. ein Symbol neben dem Text) und unsichere kurze Wörter weglassen.
        words = words.dropWhile { it.confidence < MIN_WORD_CONFIDENCE }.dropLastWhile { it.confidence < MIN_WORD_CONFIDENCE }
        words = words.filterNot { it.confidence < MIN_WORD_CONFIDENCE && it.text.count { c -> c.isLetterOrDigit() } <= 2 }
        if (isWordSalad(words)) return null
        return words.joinToString(" ") { it.text }.takeIf { it.isNotBlank() }
    }

    /**
     * Mehrere Wortfetzen ohne Zahl und ohne ein Wort aus drei oder mehr Buchstaben, dazu unsicher gelesen
     * („A Wa u“) – stammt von einem Foto. Einzelne kurze Wörter wie „Öl“ oder „Ei“ bleiben.
     */
    private fun isWordSalad(words: List<OcrWord>): Boolean =
        words.size >= 2 &&
            words.none { word -> word.text.any { it.isDigit() } || word.text.count { it.isLetter() } >= 3 } &&
            words.sumOf { it.confidence } < SALAD_CONFIDENCE * words.size

    private fun unescape(text: String): String =
        text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
}
