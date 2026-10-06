package com.moltobene.app.ui.edit

import android.net.Uri
import com.moltobene.app.data.share.IncomingText

/** Was per „Teilen mit…“ oder „Öffnen mit…“ an Moltobene kommt: Bilder (#46), Text oder eine Rezeptdatei (#54). */
sealed interface SharedContent {
    data class Photos(val uris: List<Uri>) : SharedContent
    data class Text(val shared: IncomingText.Shared) : SharedContent
    data class RecipeFile(val uri: Uri) : SharedContent
}

/**
 * Übergabe aus „Teilen mit…“ an das Formular: nur im Arbeitsspeicher und genau einmal.
 * Das Formular übernimmt den Inhalt gleich nach dem Öffnen.
 */
class SharedInput {
    private var pending: SharedContent? = null

    fun offer(content: SharedContent) {
        pending = content
    }

    fun take(): SharedContent? = pending.also { pending = null }
}
