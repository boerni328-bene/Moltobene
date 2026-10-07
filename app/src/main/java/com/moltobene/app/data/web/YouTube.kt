package com.moltobene.app.data.web

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.net.URL

/**
 * „Aus Link übernehmen“ für YouTube-Videos: Das Rezept steht – wenn überhaupt – in der Videobeschreibung.
 * Laut Vision nur ein bestmöglicher Versuch ohne Zusage, denn YouTube kann den Aufbau seiner Seiten jederzeit
 * ändern. Gelesen wird die Videoseite wie jede andere Seite: ohne Konto, ohne Cookies, ohne Skripte.
 * Ohne Android-Abhängigkeiten, per Unit-Test prüfbar.
 */
object YouTube {

    /** Ein Video mit dem, was für einen Entwurf zählt. */
    data class Video(
        val id: String,
        val title: String?,
        val description: String,
        val channel: String?,
        val imageUrl: String?,
    )

    /**
     * Kennung des Videos aus allen üblichen Linkformen (watch?v=…, youtu.be/…, /shorts/…, /live/…, /embed/…);
     * null, wenn der Link kein YouTube-Video ist.
     */
    fun videoId(link: String): String? {
        val url = runCatching { URL(link.trim()) }.getOrNull() ?: return null
        val host = url.host.lowercase().removePrefix("www.").removePrefix("m.")
        val path = url.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val id = when (host) {
            "youtube.com" -> when (path.firstOrNull()) {
                "watch" -> url.query?.split('&')?.firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                "shorts", "live", "embed", "v" -> path.getOrNull(1)
                else -> null
            }
            "youtu.be" -> path.firstOrNull()
            "youtube-nocookie.com" -> path.takeIf { it.firstOrNull() == "embed" }?.getOrNull(1)
            else -> null
        }
        return id?.takeIf { VIDEO_ID.matches(it) }
    }

    /** Der Link, der als Quelle gespeichert wird – ohne Zusätze wie „si=…“, mit denen YouTube Weitergaben verfolgt. */
    fun watchUrl(id: String): String = "https://www.youtube.com/watch?v=$id"

    /** Die Seite, die geladen wird: Die Handy-Fassung kommt ohne vorgeschaltete Seite zur Zustimmung aus. */
    fun pageUrl(id: String): String = "https://m.youtube.com/watch?v=$id"

    /**
     * Liest Titel, Beschreibung, Kanal und Vorschaubild aus der Videoseite. YouTube legt sie als Daten
     * („ytInitialPlayerResponse“) in die Seite; das Skript selbst wird nie ausgeführt.
     * null, wenn die Seite kein Video enthält, z. B. eine Seite zur Zustimmung statt des Videos.
     */
    fun read(page: WebPage): Video? {
        val document = Jsoup.parse(ByteArrayInputStream(page.body), page.charset, page.url)
        return read(document)
    }

    fun read(document: Document): Video? {
        val ogImage = meta(document, "og:image")?.takeIf { it.startsWith("https://") }
        val player = playerResponse(document)
        val details = player?.get("videoDetails") as? JsonObject
        val microformat = (player?.get("microformat") as? JsonObject)?.get("playerMicroformatRenderer") as? JsonObject
        val id = string(details, "videoId")?.takeIf { VIDEO_ID.matches(it) } ?: return null
        val description = string(details, "shortDescription")
            ?: ((microformat?.get("description") as? JsonObject)?.let { string(it, "simpleText") })
            ?: ""
        return Video(
            id = id,
            title = string(details, "title")?.let(::cleanTitle)?.ifEmpty { null },
            description = description.replace("\r", "").take(MAX_DESCRIPTION),
            channel = string(details, "author")?.let { HtmlText.inline(it) }?.ifEmpty { null },
            imageUrl = ogImage ?: largestThumbnail(details),
        )
    }

    /**
     * Titel ohne Schlagwörter (#…) und Bildzeichen: „🔥 PASTA AL TONNO 😍 #Shorts“ → „PASTA AL TONNO“.
     * Die Schreibweise bleibt, wie sie ist.
     */
    internal fun cleanTitle(title: String): String =
        HtmlText.inline(VideoDescription.withoutPictographs(title).replace(HASHTAG, " "))
            .trim(' ', '-', '–', '|', ':', ',')
            .trim()

    /** Der Datenblock „ytInitialPlayerResponse = {…}“ aus einem der Skripte der Seite. */
    private fun playerResponse(document: Document): JsonObject? {
        for (script in document.select("script")) {
            val data = script.data()
            val start = data.indexOf(PLAYER_MARKER).takeIf { it >= 0 } ?: continue
            val open = data.indexOf('{', start + PLAYER_MARKER.length).takeIf { it >= 0 } ?: continue
            val end = JsonObjectEnd.find(data, open) ?: continue
            val element = runCatching { JSON.parseToJsonElement(data.substring(open, end + 1)) }.getOrNull()
            if (element is JsonObject) return element
        }
        return null
    }

    private fun largestThumbnail(details: JsonObject?): String? {
        val thumbnails = ((details?.get("thumbnail") as? JsonObject)?.get("thumbnails") as? JsonArray).orEmpty()
        return thumbnails.filterIsInstance<JsonObject>()
            .mapNotNull { item -> string(item, "url")?.let { it to ((item["width"] as? JsonPrimitive)?.intOrNull ?: 0) } }
            .filter { it.first.startsWith("https://") }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun string(item: JsonObject?, key: String): String? =
        (item?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun meta(document: Document, property: String): String? =
        document.select("meta[content]")
            .firstOrNull { it.attr("property").equals(property, ignoreCase = true) || it.attr("name").equals(property, ignoreCase = true) }
            ?.attr("content")?.trim()?.ifEmpty { null }

    private const val PLAYER_MARKER = "ytInitialPlayerResponse"

    /** YouTube erlaubt höchstens 5.000 Zeichen; mehr wird nicht gelesen. */
    private const val MAX_DESCRIPTION = 10_000
    private val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
    private val HASHTAG = Regex("(^|\\s)#[\\p{L}\\p{N}_]+")
    private val JSON = Json { isLenient = true }
}

/** Findet das Ende eines JSON-Objekts in einem Skript, ohne den Rest des Skripts verstehen zu müssen. */
internal object JsonObjectEnd {

    /** Stelle der schließenden Klammer zum Objekt, das bei [start] („{“) beginnt; null, wenn es nicht endet. */
    fun find(text: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                inString -> Unit
                char == '{' || char == '[' -> depth++
                char == '}' || char == ']' -> {
                    depth--
                    if (depth == 0) return index.takeIf { char == '}' }
                }
            }
        }
        return null
    }
}
