package com.moltobene.app.data.voice

/**
 * Gesprochene Zahlen in Ziffern, damit der Text aus „Aus Audio übernehmen“ und „Aus Sprache übernehmen“ (Beta) wie ein
 * getippter Rezepttext gelesen werden kann: „zweihundertfünfzig Gramm Mehl“ → „250 Gramm Mehl“, „ein halber Liter“ →
 * „½ Liter“, „zwei komma fünf“ → „2,5“. Die Erkennung liefert Zahlen mal zusammengeschrieben („zweihundertfünfzig“), mal
 * getrennt („zweihundert fünfzig“); beides wird verstanden. Reines Kotlin, per Unit-Test prüfbar.
 *
 * Vorsichtig bei „ein“, „eine“, „einen“: Sie bleiben Artikel („in einem Topf“, „ein bisschen Salz“) und werden nur vor einer
 * Mengeneinheit zur Ziffer („ein Esslöffel Öl“ → „1 Esslöffel Öl“).
 */
object SpokenNumbers {

    /** Wandelt alle gesprochenen Zahlen in [text] um; alles andere bleibt, wie es ist. */
    fun convert(text: String): String {
        var result = text
        for ((pattern, replacement) in FRACTIONS) result = pattern.replace(result, replacement)
        result = NULL_COMMA.replace(result, "0 komma")
        result = convertWords(result)
        return DECIMAL.replace(result) { "${it.groupValues[1]},${it.groupValues[2]}" }
    }

    private enum class Kind { UNIT, TEEN, TENS, HUNDRED, THOUSAND, AND }

    private class Atom(val kind: Kind, val value: Int)

    /** Die kleinsten Bausteine deutscher Zahlwörter; die längsten zuerst, damit „dreizehn“ nicht als „drei“ + … gelesen wird. */
    private val ATOMS: List<Pair<String, Atom>> = listOf(
        "ein" to Atom(Kind.UNIT, 1), "eins" to Atom(Kind.UNIT, 1), "zwei" to Atom(Kind.UNIT, 2), "zwo" to Atom(Kind.UNIT, 2),
        "drei" to Atom(Kind.UNIT, 3), "vier" to Atom(Kind.UNIT, 4), "fünf" to Atom(Kind.UNIT, 5), "sechs" to Atom(Kind.UNIT, 6),
        "sieben" to Atom(Kind.UNIT, 7), "acht" to Atom(Kind.UNIT, 8), "neun" to Atom(Kind.UNIT, 9),
        "zehn" to Atom(Kind.TEEN, 10), "elf" to Atom(Kind.TEEN, 11), "zwölf" to Atom(Kind.TEEN, 12),
        "dreizehn" to Atom(Kind.TEEN, 13), "vierzehn" to Atom(Kind.TEEN, 14), "fünfzehn" to Atom(Kind.TEEN, 15),
        "sechzehn" to Atom(Kind.TEEN, 16), "siebzehn" to Atom(Kind.TEEN, 17), "achtzehn" to Atom(Kind.TEEN, 18),
        "neunzehn" to Atom(Kind.TEEN, 19),
        "zwanzig" to Atom(Kind.TENS, 20), "dreißig" to Atom(Kind.TENS, 30), "dreissig" to Atom(Kind.TENS, 30),
        "vierzig" to Atom(Kind.TENS, 40), "fünfzig" to Atom(Kind.TENS, 50), "sechzig" to Atom(Kind.TENS, 60),
        "siebzig" to Atom(Kind.TENS, 70), "achtzig" to Atom(Kind.TENS, 80), "neunzig" to Atom(Kind.TENS, 90),
        "hundert" to Atom(Kind.HUNDRED, 100), "tausend" to Atom(Kind.THOUSAND, 1000),
        "und" to Atom(Kind.AND, 0),
    ).sortedByDescending { it.first.length }

    /** Mengeneinheiten, vor denen „ein“/„eine“ für die Ziffer 1 steht. */
    private val UNIT_WORDS = setOf(
        "gramm", "kilogramm", "kilo", "pfund", "liter", "milliliter", "esslöffel", "teelöffel", "löffel", "prise", "prisen",
        "messerspitze", "bund", "zehe", "zehen", "stück", "dose", "päckchen", "becher", "tasse", "scheibe", "packung", "würfel",
        "schuss", "tropfen", "handvoll", "glas", "stange", "stängel", "kugel", "blatt",
    )

    private val FRACTIONS: List<Pair<Regex, String>> = listOf(
        Regex("\\b(eineinhalb|anderthalb)\\b", RegexOption.IGNORE_CASE) to "1½",
        Regex("\\b(ein|eine|einen|einem)\\s+halb(e|er|es|en|em)?\\b", RegexOption.IGNORE_CASE) to "½",
        Regex("\\bdrei\\s?viertel\\b", RegexOption.IGNORE_CASE) to "¾",
        Regex("\\b(ein|eine)\\s+viertel\\b", RegexOption.IGNORE_CASE) to "¼",
        Regex("\\bzwei\\s+drittel\\b", RegexOption.IGNORE_CASE) to "⅔",
        Regex("\\b(ein|eine)\\s+drittel\\b", RegexOption.IGNORE_CASE) to "⅓",
        Regex("\\b(ein|eine)\\s+achtel\\b", RegexOption.IGNORE_CASE) to "⅛",
    )

    private val NULL_COMMA = Regex("\\bnull\\s+komma\\b", RegexOption.IGNORE_CASE)
    private val DECIMAL = Regex("(\\d+)\\s+komma\\s+(\\d+)", RegexOption.IGNORE_CASE)
    private val WHITESPACE = Regex("\\s+")

    private fun convertWords(text: String): String {
        val words = text.split(WHITESPACE).filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            // „eine Prise“, „einen Esslöffel“: der Artikel vor einer Mengeneinheit ist die Ziffer 1.
            val article = words[i].lowercase().trim(*PUNCTUATION)
            if ((article == "eine" || article == "einen") && words.getOrNull(i + 1)?.lowercase()?.trim(*PUNCTUATION) in UNIT_WORDS) {
                out += "1"
                i++
                continue
            }
            val first = atomsOf(words[i])
            if (first == null || first.all { it.kind == Kind.AND }) {
                out += words[i]
                i++
                continue
            }
            // So viele aufeinanderfolgende Zahlwörter wie möglich: „zweihundert fünfzig“ ist eine Zahl.
            val atoms = mutableListOf<Atom>()
            var j = i
            while (j < words.size) {
                val next = atomsOf(words[j]) ?: break
                if (next.all { it.kind == Kind.AND }) break
                atoms += next
                j++
            }
            // Ein einzelnes „ein“ ist meist ein Artikel: nur vor einer Mengeneinheit die Ziffer 1.
            val lone = atoms.size == 1 && atoms[0].value == 1
            if (lone && words.getOrNull(j)?.lowercase()?.trim(*PUNCTUATION) !in UNIT_WORDS) {
                out += words[i]
                i++
                continue
            }
            out += numbersOf(atoms).joinToString(" ")
            i = j
        }
        return out.joinToString(" ")
    }

    private val PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?')

    /** Zerlegt ein Wort in Zahlbausteine; null, wenn es kein reines Zahlwort ist („weiter“, „viertel“). */
    private fun atomsOf(word: String): List<Atom>? {
        var rest = word.lowercase().trim(*PUNCTUATION)
        if (rest.isEmpty()) return null
        val result = mutableListOf<Atom>()
        while (rest.isNotEmpty()) {
            val hit = ATOMS.firstOrNull { rest.startsWith(it.first) } ?: return null
            result += hit.second
            rest = rest.substring(hit.first.length)
        }
        return result
    }

    /** Teilt eine Folge von Bausteinen in Zahlen auf („zwei drei“ sind zwei Zahlen) und rechnet jede aus. */
    private fun numbersOf(atoms: List<Atom>): List<Long> {
        val numbers = mutableListOf<Long>()
        var group = mutableListOf<Atom>()
        fun close() {
            val clean = group.dropLastWhile { it.kind == Kind.AND }
            if (clean.isNotEmpty()) numbers += valueOf(clean)
            group = mutableListOf()
        }
        for (atom in atoms) {
            val previous = group.lastOrNull()
            if (previous != null && !allowed(previous.kind, atom.kind)) close()
            if (group.isEmpty() && atom.kind == Kind.AND) continue
            group += atom
        }
        close()
        return numbers
    }

    private fun allowed(previous: Kind, next: Kind): Boolean = when (previous) {
        Kind.UNIT -> next == Kind.AND || next == Kind.HUNDRED || next == Kind.THOUSAND
        Kind.TEEN, Kind.TENS -> next == Kind.THOUSAND
        Kind.HUNDRED -> next == Kind.UNIT || next == Kind.TEEN || next == Kind.TENS || next == Kind.THOUSAND
        Kind.THOUSAND -> next == Kind.UNIT || next == Kind.TEEN || next == Kind.TENS || next == Kind.HUNDRED
        Kind.AND -> next == Kind.TENS
    }

    private fun valueOf(atoms: List<Atom>): Long {
        var total = 0L
        var chunk = 0L
        for (atom in atoms) {
            when (atom.kind) {
                Kind.UNIT, Kind.TEEN, Kind.TENS -> chunk += atom.value
                Kind.HUNDRED -> chunk = if (chunk == 0L) 100 else chunk * 100
                Kind.THOUSAND -> {
                    total += (if (chunk == 0L) 1 else chunk) * 1000
                    chunk = 0
                }
                Kind.AND -> Unit
            }
        }
        return total + chunk
    }
}
