package com.moltobene.app.data.web

import java.net.IDN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URL

/**
 * Prüft Links für „Aus Link übernehmen“ (#55), bevor etwas geladen wird: nur öffentliche Internetseiten, nur
 * verschlüsselt (https), keine Zugangsdaten im Link, keine Adressen im eigenen Netz. Zählzusätze wie „utm_…“
 * fallen weg. Reines Kotlin, per Unit-Test prüfbar.
 */
object WebAddress {

    /** Längere Links sind kein Rezept-Link. */
    const val MAX_LENGTH = 2_000

    /**
     * Der Link, wie er geladen wird: immer https, ohne Zählzusätze und ohne Sprungmarke (#…).
     * null, wenn es kein Link zu einer öffentlichen Internetseite ist.
     * @param maxLength längste erlaubte Länge; Downloads werden auf lange, signierte Links weitergeleitet
     */
    fun normalize(input: String, maxLength: Int = MAX_LENGTH): String? {
        val text = input.trim()
        if (text.isEmpty() || text.length > maxLength || text.any { it.isWhitespace() || it.isISOControl() }) return null
        val url = runCatching { URL(text) }.getOrNull() ?: return null
        val scheme = url.protocol.lowercase()
        if (scheme != "https" && scheme != "http") return null
        if (url.userInfo != null) return null
        // Nur die üblichen Ports: Rezeptseiten brauchen keine anderen, Geräte im Heimnetz oft schon.
        if (url.port != -1 && url.port != 80 && url.port != 443) return null
        val host = asciiHost(url.host) ?: return null
        if (!isPublicName(host)) return null
        val path = encodeIllegal(url.path.orEmpty())
        val query = url.query
            ?.split('&')
            ?.filter { it.isNotEmpty() && !isTracking(it.substringBefore('=')) }
            ?.joinToString("&")
            ?.takeIf { it.isNotEmpty() }
            ?.let(::encodeIllegal)
        return buildString {
            append("https://").append(host).append(path)
            if (query != null) append('?').append(query)
        }
    }

    /** Rechnername in ASCII-Schreibweise (Umlaut-Domains als „xn--…“), klein geschrieben; null, wenn ungültig. */
    private fun asciiHost(host: String?): String? {
        val trimmed = host?.trim()?.trimEnd('.')?.takeIf { it.isNotEmpty() } ?: return null
        val ascii = runCatching { IDN.toASCII(trimmed, IDN.ALLOW_UNASSIGNED) }.getOrNull() ?: return null
        return ascii.lowercase().takeIf { HOST.matches(it) }
    }

    /**
     * Ein Name im Internet: mit Punkt, keine reine IP-Adresse (Rezeptseiten haben Namen) und keine Endung
     * fürs eigene Netz wie „.local“ oder „localhost“.
     */
    internal fun isPublicName(host: String): Boolean {
        if (!host.contains('.')) return false
        if (IPV4.matches(host) || host.contains(':')) return false
        val lastLabel = host.substringAfterLast('.')
        if (lastLabel.all { it.isDigit() }) return false
        return LOCAL_ENDINGS.none { host == it || host.endsWith(".$it") }
    }

    /**
     * Gehört die Adresse zum öffentlichen Internet? Geprüft wird nach der Namensauflösung, damit auch ein Name,
     * der auf das eigene Netz zeigt, nicht geladen wird.
     */
    fun isPublic(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) {
            return false
        }
        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val first = bytes[0].toInt() and 0xFF
                val second = bytes[1].toInt() and 0xFF
                // 0.0.0.0/8, 100.64.0.0/10 (Netz des Anbieters), 198.18.0.0/15 (Prüfnetze), 240.0.0.0/4 (reserviert)
                !(first == 0 || (first == 100 && second in 64..127) || (first == 198 && second in 18..19) || first >= 240)
            }
            // fc00::/7: private Adressen im IPv6-Netz
            is Inet6Address -> (bytes[0].toInt() and 0xFE) != 0xFC
            else -> false
        }
    }

    private fun isTracking(name: String): Boolean {
        val key = name.lowercase()
        return key.startsWith("utm_") || key in TRACKING
    }

    /** Kodiert Zeichen, die in einem Link nicht stehen dürfen (z. B. Umlaute oder Leerzeichen); „%“ bleibt, wie es ist. */
    internal fun encodeIllegal(value: String): String = buildString {
        for (char in value) {
            if (char.code < 128 && (char.isLetterOrDigit() || char in ALLOWED)) {
                append(char)
            } else {
                char.toString().toByteArray(Charsets.UTF_8).forEach { append('%').append("%02X".format(it.toInt() and 0xFF)) }
            }
        }
    }

    private const val ALLOWED = "-._~:/?#[]@!$&'()*+,;=%"
    private val HOST = Regex("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*")
    private val IPV4 = Regex("\\d{1,3}(\\.\\d{1,3}){3}")
    private val LOCAL_ENDINGS = listOf("localhost", "local", "internal", "lan", "home", "intranet", "corp", "home.arpa")
    private val TRACKING = setOf("fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid", "igshid", "_ga", "_gl", "yclid")
}
