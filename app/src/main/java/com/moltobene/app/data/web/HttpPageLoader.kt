package com.moltobene.app.data.web

import com.moltobene.app.data.web.WebException.Problem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException

/**
 * Lädt Seiten mit dem in Android eingebauten Werkzeug (HttpsURLConnection) – ohne eingebetteten Browser,
 * ohne Skripte und ohne Cookies (#55). Regeln aus der Besprechung vom 04.10.2026:
 * - nur https; Weiterleitungen höchstens [MAX_REDIRECTS]-mal und jedes Ziel wird wieder geprüft,
 * - keine Adressen im eigenen Netz, auch nicht über einen Namen, der dorthin zeigt,
 * - Zeitgrenzen und Größengrenzen; „Abbrechen“ trennt die Verbindung sofort.
 */
class HttpPageLoader(private val language: () -> String = { Locale.getDefault().toLanguageTag() }) : PageLoader {

    override suspend fun loadPage(url: String): WebPage = withTotalTimeout {
        fetch(url, PAGE_ACCEPT, PAGE_TYPES, PAGE_FETCH) { connection, finalUrl ->
            // Längere Seiten werden abgeschnitten: Die Rezeptangaben stehen fast immer weit vorn.
            val body = connection.inputStream.use { readUpTo(it, MAX_PAGE_BYTES) }
            WebPage(finalUrl, body, charsetOf(connection.contentType))
        }
    }

    override suspend fun loadImage(url: String, target: File) = withTotalTimeout {
        fetch(url, IMAGE_ACCEPT, IMAGE_TYPES, IMAGE_FETCH) { connection, _ ->
            if (connection.contentLengthLong > MAX_IMAGE_BYTES) throw WebException(Problem.TOO_LARGE)
            connection.inputStream.use { input -> target.outputStream().use { copyAtMost(input, it, MAX_IMAGE_BYTES) } }
        }
    }

    private suspend fun <T> withTotalTimeout(block: suspend () -> T): T =
        try {
            withTimeout(TOTAL_TIMEOUT_MILLIS) { block() }
        } catch (e: TimeoutCancellationException) {
            throw WebException(Problem.TIMEOUT, e)
        }

    private sealed interface Step<out T> {
        class Done<T>(val value: T) : Step<T>
        class Redirect(val url: String) : Step<Nothing>
    }

    private suspend fun <T> fetch(
        start: String,
        accept: String,
        types: List<String>,
        fetchHeaders: Map<String, String>,
        read: (HttpsURLConnection, String) -> T,
    ): T {
        var url = WebAddress.normalize(start) ?: throw WebException(Problem.NOT_ALLOWED)
        repeat(MAX_REDIRECTS + 1) {
            val connection = open(url, accept, fetchHeaders)
            when (val step = withConnection(connection) { respond(connection, url, types, read) }) {
                is Step.Done -> return step.value
                is Step.Redirect -> url = step.url
            }
        }
        throw WebException(Problem.NOT_ALLOWED)
    }

    /** Baut die Verbindung auf, nachdem geprüft ist, dass der Name ins öffentliche Internet zeigt. */
    private suspend fun open(url: String, accept: String, fetchHeaders: Map<String, String>): HttpsURLConnection = withContext(Dispatchers.IO) {
        val parsed = URL(url)
        val addresses = try {
            InetAddress.getAllByName(parsed.host)
        } catch (e: UnknownHostException) {
            throw WebException(Problem.NO_CONNECTION, e)
        }
        if (addresses.isEmpty() || addresses.any { !WebAddress.isPublic(it) }) throw WebException(Problem.NOT_ALLOWED)
        val connection = parsed.openConnection() as? HttpsURLConnection ?: throw WebException(Problem.NOT_ALLOWED)
        connection.apply {
            instanceFollowRedirects = false
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            useCaches = false
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", accept)
            setRequestProperty("Accept-Language", acceptLanguage())
            fetchHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
        }
    }

    /**
     * Führt [block] im Hintergrund aus. Wird abgebrochen, wird die Verbindung sofort getrennt – sonst würde
     * eine wartende Leseanfrage erst nach ihrer Zeitgrenze enden.
     */
    private suspend fun <T> withConnection(connection: HttpsURLConnection, block: () -> T): T = coroutineScope {
        val work = async(Dispatchers.IO) {
            try {
                block()
            } catch (e: WebException) {
                throw e
            } catch (e: IOException) {
                throw classify(e)
            } finally {
                connection.disconnect()
            }
        }
        try {
            work.await()
        } catch (e: CancellationException) {
            connection.disconnect()
            throw e
        }
    }

    private fun <T> respond(
        connection: HttpsURLConnection,
        url: String,
        types: List<String>,
        read: (HttpsURLConnection, String) -> T,
    ): Step<T> {
        val code = connection.responseCode
        return when {
            code in 300..399 && code != 304 -> {
                val location = connection.getHeaderField("Location") ?: throw WebException(Problem.SERVER_ERROR)
                val target = runCatching { URL(URL(url), location).toString() }.getOrNull()
                Step.Redirect(target?.let(WebAddress::normalize) ?: throw WebException(Problem.NOT_ALLOWED))
            }
            code == 404 || code == 410 -> throw WebException(Problem.NOT_FOUND)
            code == 401 || code == 403 || code == 429 || code == 451 -> throw WebException(Problem.BLOCKED)
            code !in 200..299 -> throw WebException(Problem.SERVER_ERROR)
            else -> {
                val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
                if (!type.isNullOrEmpty() && types.none { type.startsWith(it) }) throw WebException(Problem.NOT_A_PAGE)
                Step.Done(read(connection, url))
            }
        }
    }

    private fun acceptLanguage(): String {
        val tag = language().ifBlank { "en" }
        return listOf(tag, tag.substringBefore('-'), "en").distinct()
            .mapIndexed { index, value -> if (index == 0) value else "$value;q=0.${9 - index}" }
            .joinToString(",")
    }

    companion object {
        const val MAX_PAGE_BYTES = 5 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
        const val MAX_REDIRECTS = 5
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 20_000
        private const val TOTAL_TIMEOUT_MILLIS = 45_000L

        /**
         * Wie ein üblicher Handy-Browser, aber ohne Angaben zum eigenen Handy: „Android 10; K“ steht so in jedem
         * aktuellen Chrome. Manche Seiten liefern Programmen mit unbekannter Kennung sonst keine Rezepte aus.
         */
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

        /**
         * Angaben, die jeder Browser beim Öffnen einer Seite mitschickt: „eine Seite, direkt aufgerufen“. Ohne sie
         * weisen manche Koch-Portale die App ab (z. B. REWE oder Cookie and Kate, geprüft am 07.10.2026). Sie
         * enthalten nichts über das Handy oder die Person.
         */
        private val PAGE_FETCH = mapOf(
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
        )
        private val IMAGE_FETCH = mapOf(
            "Sec-Fetch-Dest" to "image",
            "Sec-Fetch-Mode" to "no-cors",
            "Sec-Fetch-Site" to "cross-site",
        )

        private const val PAGE_ACCEPT = "text/html,application/xhtml+xml,application/ld+json;q=0.9,*/*;q=0.5"
        private const val IMAGE_ACCEPT = "image/jpeg,image/png,image/webp,image/*;q=0.8"
        private val PAGE_TYPES = listOf("text/html", "application/xhtml+xml", "application/ld+json", "application/json", "text/plain")
        private val IMAGE_TYPES = listOf("image/")
        private val CHARSET = Regex("charset=\"?([^\";\\s]+)", RegexOption.IGNORE_CASE)

        internal fun charsetOf(contentType: String?): String? {
            val name = contentType?.let { CHARSET.find(it)?.groupValues?.get(1) } ?: return null
            return name.takeIf { runCatching { Charset.isSupported(it) }.getOrDefault(false) }
        }

        internal fun classify(e: IOException): WebException = WebException(
            when (e) {
                is SocketTimeoutException -> Problem.TIMEOUT
                is SSLException -> Problem.NOT_SECURE
                is UnknownHostException, is ConnectException, is NoRouteToHostException, is SocketException -> Problem.NO_CONNECTION
                else -> Problem.NO_CONNECTION
            },
            e,
        )

        /** Liest höchstens [max] Bytes; der Rest wird nicht mehr gelesen. */
        internal fun readUpTo(input: InputStream, max: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (out.size() < max) {
                val read = input.read(buffer, 0, minOf(buffer.size, max - out.size()))
                if (read < 0) break
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }

        /** Kopiert alles, aber höchstens [max] Bytes – sonst ist das Foto zu groß. */
        internal fun copyAtMost(input: InputStream, output: OutputStream, max: Long) {
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > max) throw WebException(Problem.TOO_LARGE)
                output.write(buffer, 0, read)
            }
        }
    }
}
