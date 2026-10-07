package com.moltobene.app.data

/**
 * Erkennt „Speicher voll“ an einem Fehler beim Schreiben (#51), damit die Meldung sagen kann, was zu tun ist –
 * statt z. B. „Die Sicherungsdatei ist beschädigt“. Android meldet das als „ENOSPC (No space left on device)“.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object StorageFull {

    fun isCause(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.take(MAX_DEPTH).any { cause ->
            val message = cause.message.orEmpty()
            message.contains("ENOSPC") || message.contains("No space left", ignoreCase = true)
        }

    private const val MAX_DEPTH = 10
}
