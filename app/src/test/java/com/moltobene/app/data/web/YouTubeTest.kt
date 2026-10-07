package com.moltobene.app.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** YouTube-Links und Videoseiten. Die Seiten sind selbst geschrieben, nach dem Aufbau echter Videoseiten. */
class YouTubeTest {

    @Test
    fun videoAusAllenLinkformen() {
        val id = "AbCdEfGhI-_"
        listOf(
            "https://www.youtube.com/watch?v=$id",
            "https://m.youtube.com/watch?v=$id&t=42s",
            "https://youtube.com/watch?feature=share&v=$id",
            "http://www.youtube.com/watch?v=$id",
            "https://youtu.be/$id?si=Zx8trackingId",
            "https://www.youtube.com/shorts/$id?feature=share",
            "https://www.youtube.com/live/$id",
            "https://www.youtube.com/embed/$id",
            "https://www.youtube-nocookie.com/embed/$id",
        ).forEach { assertEquals(it, id, YouTube.videoId(it)) }

        listOf(
            "https://music.youtube.com/watch?v=$id",
            "https://www.youtube.com/@beispielkueche",
            "https://www.youtube.com/playlist?list=PL123",
            "https://www.example.org/watch?v=$id",
            "https://youtu.be/kurz",
            "kein Link",
        ).forEach { assertNull(it, YouTube.videoId(it)) }
    }

    @Test
    fun quelleOhneZusaetze() {
        // „si=…“ verfolgt, wer den Link geteilt hat – als Quelle bleibt nur das Video.
        assertEquals("https://www.youtube.com/watch?v=AbCdEfGhIjK", YouTube.watchUrl(YouTube.videoId("https://youtu.be/AbCdEfGhIjK?si=abc")!!))
        assertEquals("https://m.youtube.com/watch?v=AbCdEfGhIjK", YouTube.pageUrl("AbCdEfGhIjK"))
    }

    @Test
    fun videoAusDerSeite() {
        val video = YouTube.read(page(ogImage = true))
        assertNotNull(video)
        assertEquals("AbCdEfGhIjK", video!!.id)
        // Ohne Bildzeichen und Schlagwörter, sonst wie im Original.
        assertEquals("Schnelle Linsensuppe", video.title)
        assertEquals("Beispielküche", video.channel)
        assertTrue(video.description.startsWith("Zutaten:\n250 g rote Linsen\n1 Zwiebel"))
        assertTrue(video.description.endsWith("Alles kochen. {nicht} \"zitiert\" & fertig."))
        assertEquals("https://i.ytimg.com/vi/AbCdEfGhIjK/maxresdefault.jpg", video.imageUrl)
    }

    @Test
    fun ohneVorschaufotoDasGroessteBild() {
        assertEquals("https://i.ytimg.com/vi/AbCdEfGhIjK/hqdefault.jpg", YouTube.read(page(ogImage = false))?.imageUrl)
    }

    @Test
    fun seiteOhneVideo() {
        // Etwa eine Seite zur Zustimmung statt des Videos.
        val html = """
            <html><head><title>Bevor es weitergeht</title></head>
            <body><form action="https://consent.example/save"><button>Alle ablehnen</button></form></body></html>
        """.trimIndent()
        assertNull(YouTube.read(WebPage("https://consent.example/", html.toByteArray(), "UTF-8")))
    }

    @Test
    fun titelOhneBeiwerk() {
        assertEquals("PASTA AL TONNO", YouTube.cleanTitle("🔥 PASTA AL TONNO 😍 #Shorts"))
        assertEquals("Mac & Cheese", YouTube.cleanTitle("Mac & Cheese #comfortfood #pasta"))
        assertEquals("Linsensuppe", YouTube.cleanTitle("Linsensuppe |"))
    }

    @Test
    fun endeEinesJsonObjekts() {
        val text = "var x = {\"a\":\"}\",\"b\":[1,{\"c\":\"\\\"}\"}]};var y = {};"
        val start = text.indexOf('{')
        assertEquals(text.indexOf("};var y") , JsonObjectEnd.find(text, start))
        assertNull(JsonObjectEnd.find("{\"offen\":[1,2", 0))
    }

    private fun page(ogImage: Boolean): WebPage {
        val image = if (ogImage) """<meta property="og:image" content="https://i.ytimg.com/vi/AbCdEfGhIjK/maxresdefault.jpg">""" else ""
        val html = """
            <!doctype html><html lang="de"><head>
            <title>Schnelle Linsensuppe - YouTube</title>
            <meta property="og:title" content="Schnelle Linsensuppe">
            $image
            </head><body>
            <script nonce="n">var ytInitialPlayerResponse = null;</script>
            <script nonce="n">var ytInitialPlayerResponse = {"responseContext":{"serviceTrackingParams":[{"service":"GFEEDBACK","params":[{"key":"e","value":"1"}]}]},"playabilityStatus":{"status":"OK"},"videoDetails":{"videoId":"AbCdEfGhIjK","title":"🍲 Schnelle Linsensuppe #shorts","lengthSeconds":"58","author":"Beispielküche","shortDescription":"Zutaten:\n250 g rote Linsen\n1 Zwiebel\n\nZubereitung:\nAlles kochen. {nicht} \"zitiert\" \u0026 fertig.","thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/AbCdEfGhIjK/default.jpg","width":120,"height":90},{"url":"https://i.ytimg.com/vi/AbCdEfGhIjK/hqdefault.jpg","width":480,"height":360}]}}};var ytplayer = {"x":"}"};</script>
            </body></html>
        """.trimIndent()
        return WebPage("https://m.youtube.com/watch?v=AbCdEfGhIjK", html.toByteArray(), "UTF-8")
    }
}
