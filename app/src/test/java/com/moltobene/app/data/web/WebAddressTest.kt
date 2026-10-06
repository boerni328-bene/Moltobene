package com.moltobene.app.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/** Prüfung der Links für „Aus Link übernehmen“ (#55). */
class WebAddressTest {

    @Test
    fun httpsBleibtUndHttpWirdHttps() {
        assertEquals("https://www.example.org/rezept/123", WebAddress.normalize("https://www.example.org/rezept/123"))
        assertEquals("https://www.example.org/rezept", WebAddress.normalize("http://www.example.org/rezept"))
        assertEquals("https://example.org", WebAddress.normalize("  HTTPS://Example.ORG  "))
    }

    @Test
    fun zaehlzusaetzeUndSprungmarkeFallenWeg() {
        assertEquals(
            "https://example.org/r?id=7",
            WebAddress.normalize("https://example.org/r?utm_source=whatsapp&id=7&fbclid=abc#zutaten"),
        )
        assertEquals("https://example.org/r", WebAddress.normalize("https://example.org/r?utm_medium=social"))
    }

    @Test
    fun umlauteImNamenUndImPfad() {
        assertEquals("https://xn--bcker-gra.de/k%C3%A4sekuchen", WebAddress.normalize("https://bäcker.de/käsekuchen"))
        // Schon kodierte Zeichen bleiben, wie sie sind.
        assertEquals("https://example.org/k%C3%A4se", WebAddress.normalize("https://example.org/k%C3%A4se"))
    }

    @Test
    fun keineInternetseiteWirdAbgelehnt() {
        listOf(
            "",
            "Kein Link",
            "ftp://example.org/datei",
            "file:///sdcard/rezept.html",
            "javascript:alert(1)",
            "content://com.example/rezept",
            "https://benutzer:geheim@example.org/",
            "https://localhost/",
            "https://router/",
            "https://drucker.local/",
            "https://nas.home.arpa/",
            "https://192.168.0.1/",
            "https://127.0.0.1/",
            "https://[::1]/",
            "https://example.org:8080/",
            "https://example.org/mit leerzeichen",
        ).forEach { assertNull(it, WebAddress.normalize(it)) }
    }

    @Test
    fun adressenImEigenenNetzSindNichtOeffentlich() {
        listOf("127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.178.1", "169.254.1.1", "100.64.0.1", "0.0.0.0", "::1", "fd00::1", "fe80::1")
            .forEach { assertFalse(it, WebAddress.isPublic(InetAddress.getByName(it))) }
        listOf("93.184.216.34", "104.46.162.229", "2001:4860:4860::8888")
            .forEach { assertTrue(it, WebAddress.isPublic(InetAddress.getByName(it))) }
    }
}
