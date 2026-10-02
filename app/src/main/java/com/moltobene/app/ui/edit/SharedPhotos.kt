package com.moltobene.app.ui.edit

import android.net.Uri

/**
 * Übergabe der Bilder aus „Teilen mit…“ an das Formular (#46): nur im Arbeitsspeicher und genau einmal.
 * Das Formular kopiert sie gleich nach dem Öffnen in die App.
 */
class SharedPhotos {
    private var pending: List<Uri> = emptyList()

    fun offer(uris: List<Uri>) {
        pending = uris
    }

    fun take(): List<Uri> = pending.also { pending = emptyList() }
}
