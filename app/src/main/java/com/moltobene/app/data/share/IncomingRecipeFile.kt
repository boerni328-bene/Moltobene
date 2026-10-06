package com.moltobene.app.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * Rezeptdateien (#54), die per „Öffnen mit…“ oder „Teilen mit…“ an Moltobene kommen – z. B. eine Datei, die
 * jemand mit Moltobene geteilt hat (schema.org/Recipe). Wie bei Bildern (#46) zählen nur Adressen der Form
 * content:// einer anderen App, nie file:// und nie Adressen der App selbst. Fremde Dateien gelten als unsicher:
 * Gelesen werden höchstens [MAX_BYTES], und daraus wird nur Text.
 */
object IncomingRecipeFile {

    /** Dateitypen von Rezeptdateien; so teilt auch Moltobene selbst ([RecipeJsonLd.MIME_TYPE]). */
    val MIME_TYPES = listOf("application/json", "application/ld+json")

    /** Ein Rezept ist nie so groß – auch nicht mit eingebettetem Foto. */
    const val MAX_BYTES = 10 * 1024 * 1024

    /** Die Adresse der Rezeptdatei; null, wenn das [intent] keine bringt. */
    fun from(intent: Intent?, ownAuthority: String): Uri? {
        if (intent == null || intent.type?.lowercase()?.substringBefore(';')?.trim() !in MIME_TYPES) return null
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            else -> null
        }
        return uri?.takeIf { it.scheme == "content" && it.authority != ownAuthority }
    }

    /** Die Datei ist größer als [MAX_BYTES]. */
    class TooLargeException : IOException("Datei zu groß")

    internal fun readAtMost(input: InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (out.size() + read > max) throw TooLargeException()
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}

/** Liest Rezeptdateien aus „Öffnen mit…“, „Teilen mit…“ oder der Dateiauswahl (#54). */
class RecipeFileReader(private val context: Context) {

    /** Liest die Datei sofort ein – die Leseerlaubnis einer anderen App gilt nur vorübergehend. */
    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        if (uri.scheme != "content") throw IOException("Keine Datei einer App")
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Datei nicht lesbar")
        input.use { IncomingRecipeFile.readAtMost(it, IncomingRecipeFile.MAX_BYTES) }
    }
}
