package com.moltobene.app.data.knowhow

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Teigwissen (#84, Etappe 2/3): mitgelieferte Artikel in einer JSON-Datei unter `assets/knowhow/`, je Artikel ein eigens
 * geschriebener deutscher und englischer Text (nicht maschinell übersetzt). Nicht in der Datenbank, nicht in der
 * Sicherung, kein Download. Zahlen sind gerundete Richtwerte (Quellen: `docs/recherche/2026-10-13-mehl-ueberblick.md`).
 */
object KnowHow {
    const val ASSET = "knowhow/knowhow.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class File(val version: Int, val articles: List<Article>)

    /** Ein Artikel in allen Sprachen; [texts] ist nach Sprache („de“, „en“) geordnet. */
    @Serializable
    data class Article(val id: String, val texts: Map<String, Text>)

    @Serializable
    data class Text(val title: String, val summary: String, val sections: List<Section>)

    /** Ein Abschnitt: Überschrift, Absätze und/oder eine Liste „Name – Beschreibung“. */
    @Serializable
    data class Section(val heading: String, val paragraphs: List<String> = emptyList(), val rows: List<Row> = emptyList())

    @Serializable
    data class Row(val label: String, val text: String)

    fun parse(text: String): List<Article> = json.decodeFromString<File>(text).articles

    /** Deutsch für deutsche Oberfläche, sonst Englisch (Rückfallsprache). */
    fun languageFor(deviceLanguage: String): String = if (deviceLanguage.lowercase().startsWith("de")) "de" else "en"

    fun text(article: Article, language: String): Text = article.texts[languageFor(language)] ?: article.texts.getValue("en")
}
