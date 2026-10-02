package com.moltobene.app.data.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * Bilder, die per „Teilen mit…“ an Moltobene kommen (#46), z. B. ein Bildschirmfoto oder ein Foto aus
 * WhatsApp. Angenommen werden nur Adressen der Form content://, die eine andere App freigegeben hat –
 * nie file:// (damit ließen sich private Dateien unterschieben) und nie Adressen der App selbst.
 * Ob es wirklich Bilder sind und wie groß sie sind, wird beim Kopieren geprüft.
 * Die Leseerlaubnis wird nicht dauerhaft festgehalten; die Bilder werden sofort kopiert.
 */
object IncomingImages {

    /** Die Bild-Adressen aus einem „Teilen mit…“; leer, wenn das [intent] keines ist. */
    fun from(intent: Intent?, ownAuthority: String): List<Uri> {
        if (intent == null || intent.type?.startsWith("image/") != true) return emptyList()
        val streams = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> return emptyList()
        }
        // Manche Apps übergeben die Bilder nur über ClipData.
        val uris = streams.ifEmpty {
            val clip = intent.clipData ?: return@ifEmpty emptyList()
            (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
        }
        return uris.filter { it.scheme == "content" && it.authority != ownAuthority }.distinct()
    }
}
