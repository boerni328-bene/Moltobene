package com.moltobene.app.data.translate

import java.text.Normalizer

/**
 * Zerlegt Text in Wortteile wie SentencePiece („Unigram“), nachgebaut in reinem Kotlin, damit „Rezept übersetzen“
 * (#60) keinen zusätzlichen Baustein braucht: Unter allen Zerlegungen gewinnt die mit der höchsten Summe der Werte
 * (Viterbi). In der Machbarkeitsprobe vom 07.10.2026 stimmte das in 49 von 49 Zeilen mit dem Original überein;
 * `LanguagePackTest` prüft es mit jedem Sprachpaket erneut.
 *
 * @param pieces Wortteil → (Wert, Kennung im Wortschatz des Modells)
 * @param unknownId Kennung für Zeichen, die kein Wortteil kennt
 */
class SentencePieceEncoder(private val pieces: Map<String, Piece>, private val unknownId: Int) {

    data class Piece(val score: Float, val id: Int)

    private val maxPieceLength = pieces.keys.maxOfOrNull { it.length } ?: 1
    private val unknownScore = (pieces.values.minOfOrNull { it.score } ?: 0f) - UNKNOWN_PENALTY

    /** Kennungen der Wortteile, ohne das Endzeichen des Modells. */
    fun encode(text: String): List<Int> {
        val normalized = normalize(text)
        if (normalized.length <= 1) return emptyList()
        // Grenzen zwischen Zeichen (nicht zwischen den beiden Hälften eines Zeichens außerhalb der Grundebene).
        val bounds = buildList {
            var index = 0
            while (index < normalized.length) {
                add(index)
                index += Character.charCount(normalized.codePointAt(index))
            }
            add(normalized.length)
        }
        val best = FloatArray(bounds.size) { Float.NEGATIVE_INFINITY }
        val from = IntArray(bounds.size) { -1 }
        val ids = IntArray(bounds.size)
        best[0] = 0f
        for (end in 1 until bounds.size) {
            // Von lang nach kurz, damit bei gleichem Wert wie im Original der längere Wortteil gewinnt.
            var first = end - 1
            while (first > 0 && bounds[end] - bounds[first - 1] <= maxPieceLength) first--
            for (start in first until end) {
                if (best[start] != Float.NEGATIVE_INFINITY) {
                    val piece = pieces[normalized.substring(bounds[start], bounds[end])]
                    val single = end - start == 1
                    if (piece != null || single) {
                        val score = best[start] + (piece?.score ?: unknownScore)
                        if (score > best[end]) {
                            best[end] = score
                            from[end] = start
                            ids[end] = piece?.id ?: unknownId
                        }
                    }
                }
            }
        }
        val result = ArrayList<Int>()
        var end = bounds.size - 1
        while (end > 0) {
            result += ids[end]
            end = from[end]
        }
        return result.asReversed()
    }

    companion object {
        /** Leerzeichen-Ersatz von SentencePiece. */
        const val SPACE = '▁'

        private const val UNKNOWN_PENALTY = 10f
        private val WHITESPACE = Regex("\\s+")

        /** Wie SentencePiece mit „nmt_nfkc“: Unicode vereinheitlichen, Leerraum zusammenfassen, führendes ▁. */
        fun normalize(text: String): String {
            val cleaned = Normalizer.normalize(text, Normalizer.Form.NFKC).trim().replace(WHITESPACE, " ")
            return SPACE + cleaned.replace(' ', SPACE)
        }

        /** Liest `source.tsv` des Sprachpakets: Wortteil, Wert, Kennung – je Zeile, durch Tab getrennt. */
        fun read(lines: Sequence<String>, unknownId: Int): SentencePieceEncoder {
            val pieces = HashMap<String, Piece>()
            lines.filter { it.isNotEmpty() }.forEach { line ->
                val parts = line.split('\t')
                require(parts.size == 3) { "Ungültige Zeile in den Wortteilen" }
                pieces[parts[0]] = Piece(parts[1].toFloat(), parts[2].toInt())
            }
            return SentencePieceEncoder(pieces, unknownId)
        }
    }
}

/** Wortschatz des Modells: Kennung → Wortteil; macht aus den Kennungen einer Übersetzung wieder Text. */
class Vocabulary(private val tokens: Array<String>, private val skip: Set<Int>) {

    val size: Int get() = tokens.size

    fun decode(ids: List<Int>): String = buildString {
        ids.forEach { id -> if (id !in skip && id in tokens.indices) append(tokens[id]) }
    }.replace(SentencePieceEncoder.SPACE, ' ').trim()

    companion object {
        /** Liest `vocab.tsv` des Sprachpakets: Kennung und Wortteil je Zeile. */
        fun read(lines: Sequence<String>, skip: Set<Int>): Vocabulary {
            val entries = lines.filter { it.isNotEmpty() }.map { line ->
                val tab = line.indexOf('\t')
                require(tab > 0) { "Ungültige Zeile im Wortschatz" }
                line.substring(0, tab).toInt() to line.substring(tab + 1)
            }.toList()
            val tokens = Array(entries.maxOf { it.first } + 1) { "" }
            entries.forEach { (id, token) -> tokens[id] = token }
            return Vocabulary(tokens, skip)
        }
    }
}
