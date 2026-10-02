package com.moltobene.app.data.share

import android.content.Intent

/**
 * Text, der per „Teilen mit…“ an Moltobene kommt – z. B. ein Rezept aus WhatsApp oder einer E-Mail,
 * oder ein Link aus dem Browser. Fremder Text gilt als unsicher: Er wird auf [MAX_CHARS] gekürzt
 * und nur als Text verwendet.
 */
object IncomingText {

    /** [subject] ist der Betreff einer E-Mail bzw. der Titel einer geteilten Seite, falls vorhanden. */
    data class Shared(val text: String, val subject: String?)

    /** Mehr hat kein Rezept; so bleibt auch der gespeicherte Zustand des Formulars klein. */
    const val MAX_CHARS = 50_000

    fun from(intent: Intent?): Shared? {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return null
        val text = intent.textExtra(Intent.EXTRA_TEXT)?.take(MAX_CHARS)
        if (text.isNullOrEmpty()) return null
        val subject = intent.textExtra(Intent.EXTRA_SUBJECT)?.take(MAX_SUBJECT_CHARS)
        return Shared(text, subject?.ifEmpty { null })
    }

    // Fehlerhafte Zusatzdaten einer fremden App dürfen Moltobene nicht abstürzen lassen.
    private fun Intent.textExtra(name: String): String? =
        runCatching { getCharSequenceExtra(name)?.toString()?.trim() }.getOrNull()

    private const val MAX_SUBJECT_CHARS = 200
}
