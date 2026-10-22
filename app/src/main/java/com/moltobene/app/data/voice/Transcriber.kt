package com.moltobene.app.data.voice

import android.net.Uri
import java.io.IOException

/** Warum sich eine Audiodatei nicht in Text umwandeln ließ („Aus Audio übernehmen“, Beta). */
class TranscribeException(val problem: Problem, cause: Throwable? = null) : IOException(problem.name, cause) {
    enum class Problem {
        /** Das Erkennungspaket ist nicht (vollständig) auf dem Handy. */
        NO_PACK,

        /** Kein lesbares Audio in der Datei. */
        UNSUPPORTED,

        /** Länger als erlaubt ([AudioDecoder.MAX_SECONDS]). */
        TOO_LONG,

        /** Zu wenig Arbeitsspeicher für das Erkennungsmodell. */
        LOW_MEMORY,

        FAILED,
    }
}

/**
 * Wandelt gesprochene Sprache in Text um, frei und ohne Befehlsliste (im Gegensatz zu [VoiceInput]). Alles läuft auf dem
 * Handy mit dem Erkennungspaket; es wird nichts gespeichert oder gesendet. Der Rundgang auf dem Emulator ersetzt sie über
 * `AppContainer.transcriber` durch eine nachgestellte Fassung.
 */
interface Transcriber {

    /** Ist das Erkennungspaket da? */
    fun isAvailable(): Boolean

    /**
     * Erkennt die Sprache in der Audiodatei [uri]; die Datei wird nur gelesen. Liefert die Äußerungen (eine je Sprechpause)
     * in der Reihenfolge der Aufnahme; [onProgress] meldet 0 bis 1. Läuft nie auf dem Hauptthread und lässt sich abbrechen.
     * @throws TranscribeException wenn sich die Datei nicht erkennen lässt
     */
    suspend fun transcribe(uri: Uri, onProgress: (Float) -> Unit = {}): List<String>
}
