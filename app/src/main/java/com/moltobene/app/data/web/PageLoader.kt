package com.moltobene.app.data.web

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Lädt Internetseiten und ihre Fotos für „Aus Link übernehmen“ (#55). Die App geht nur hierüber ins Internet,
 * und nur, wenn ein Link übernommen wird. Der Rundgang auf dem Emulator ersetzt den Lader durch nachgestellte
 * Seiten, damit er nicht vom echten Internet abhängt.
 */
interface PageLoader {

    /** Lädt die Seite [url] (mit [WebAddress.normalize] geprüft). Folgt Weiterleitungen, aber nur auf https. */
    suspend fun loadPage(url: String): WebPage

    /** Lädt ein Foto nach [target]; zu große Fotos werden nicht geladen. */
    suspend fun loadImage(url: String, target: File)
}

/**
 * Lädt große Dateien wie das Sprachpaket für „Rezept übersetzen“ (#60) – nur, wenn es in den Einstellungen
 * heruntergeladen wird. Gleiche Regeln wie für Seiten (nur https, nichts im eigenen Netz), aber ohne Zeitgrenze
 * für den ganzen Vorgang; „Abbrechen“ trennt die Verbindung sofort.
 */
interface FileDownloader {

    /**
     * Lädt [url] und gibt den Inhalt Stück für Stück an [read], mit der Größe laut Server (-1, wenn unbekannt).
     * Mehr als [maxBytes] wird nicht gelesen.
     */
    suspend fun <T> download(url: String, maxBytes: Long, read: (input: InputStream, length: Long) -> T): T
}

/**
 * Eine geladene Seite.
 * @param url Adresse nach allen Weiterleitungen – Grundlage für relative Links auf der Seite
 * @param charset Zeichensatz laut Server; null, wenn er in der Seite selbst steht
 */
class WebPage(val url: String, val body: ByteArray, val charset: String?)

/** Die Seite ließ sich nicht laden; [problem] sagt warum, damit die Meldung den nächsten Schritt nennen kann. */
class WebException(val problem: Problem, cause: Throwable? = null) : IOException(problem.name, cause) {
    enum class Problem {
        /** Kein Netz oder der Name der Seite ist unbekannt. */
        NO_CONNECTION,

        /** Die Seite antwortet nicht rechtzeitig. */
        TIMEOUT,

        /** Unter dem Link gibt es keine Seite (mehr). */
        NOT_FOUND,

        /** Die Seite lässt Programme nicht zu oder verlangt eine Anmeldung. */
        BLOCKED,

        /** Die Seite meldet einen eigenen Fehler. */
        SERVER_ERROR,

        /** Unter dem Link liegt keine Internetseite, z. B. ein PDF. */
        NOT_A_PAGE,

        /** Keine sichere (verschlüsselte) Verbindung möglich. */
        NOT_SECURE,

        /** Kein Link zu einer öffentlichen Internetseite, z. B. eine Adresse im eigenen Netz. */
        NOT_ALLOWED,

        /** Das Foto ist zu groß. */
        TOO_LARGE,
    }
}
