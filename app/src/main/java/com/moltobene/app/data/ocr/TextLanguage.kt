package com.moltobene.app.data.ocr

/**
 * Sprachen der Rezepte. Die Sprache wird am Text erkannt (typische kurze Wörter wie „und“, „the“, „di“,
 * „les“, „los“) und als Sprache des Rezepts gespeichert; die Texterkennung selbst liest alle diese Sprachen
 * zugleich. Reines Kotlin, per Unit-Test prüfbar.
 */
object TextLanguage {

    /** Sprachkürzel nach ISO 639-1, wie in [com.moltobene.app.data.Recipe.language]. */
    val SUPPORTED = listOf("de", "en", "it", "fr", "es")

    private const val FALLBACK = "en"

    private val TYPICAL_WORDS = mapOf(
        "de" to setOf(
            "und", "der", "die", "das", "mit", "den", "dem", "ein", "eine", "einem", "einer", "auf", "ist", "nicht",
            "für", "von", "im", "aus", "bis", "zum", "zur", "oder", "etwas", "dann", "mehl", "zucker", "eier", "salz",
        ),
        "en" to setOf(
            "the", "and", "with", "of", "to", "until", "into", "then", "for", "add", "from", "or", "each", "about",
            "flour", "sugar", "eggs", "salt", "minutes",
        ),
        "it" to setOf(
            "il", "di", "e", "con", "per", "del", "della", "dei", "delle", "una", "che", "al", "alla", "gli", "sale",
            "farina", "uova", "burro", "olio", "zucchero", "minuti", "fate", "aggiungete",
        ),
        "fr" to setOf(
            "le", "les", "et", "avec", "des", "du", "une", "pour", "dans", "au", "aux", "puis", "sur", "sel", "farine",
            "sucre", "beurre", "œufs", "oeufs", "faites", "ajoutez",
        ),
        "es" to setOf(
            "el", "los", "las", "y", "con", "del", "al", "para", "hasta", "que", "sal", "harina", "azúcar", "huevos",
            "aceite", "añade", "unos", "minutos",
        ),
    )

    fun supportedOrDefault(language: String?): String = language?.takeIf { it in SUPPORTED } ?: FALLBACK

    /** Erkannte Sprache oder null, wenn der Text zu kurz oder nicht eindeutig ist. */
    fun detect(text: String): String? {
        val words = text.lowercase()
            .split(Regex("[^\\p{L}]+"))
            .filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val scores = TYPICAL_WORDS.mapValues { (_, typical) -> words.count { it in typical } }
        val ranked = scores.entries.sortedByDescending { it.value }
        val best = ranked[0]
        val second = ranked[1].value
        return if (best.value >= MIN_HITS && best.value >= second * 3 / 2 + 1) best.key else null
    }

    private const val MIN_HITS = 3
}
