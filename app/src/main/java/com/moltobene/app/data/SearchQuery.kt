package com.moltobene.app.data

import java.util.Locale

/** Macht aus einer Sucheingabe eine sichere Anfrage für die Volltextsuche (FTS). */
object SearchQuery {

    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Zerlegt die Eingabe in Wörter und sucht jedes als Wortanfang („Kart“ findet „Kartoffeln“).
     * Sonderzeichen werden entfernt und alles klein geschrieben, damit die Suchsprache der Datenbank
     * (z. B. die Befehle OR, AND, NOT, NEAR) nicht ausgelöst wird. Groß-/Kleinschreibung spielt bei der Suche keine Rolle.
     * Liefert null, wenn nichts Suchbares übrig bleibt.
     */
    fun toFtsMatch(input: String): String? {
        val words = input.split(SEPARATORS).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        return words.joinToString(" ") { "${it.lowercase(Locale.ROOT)}*" }
    }
}
