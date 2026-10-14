package com.moltobene.app.data.voice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

/**
 * Das Erkennungspaket für die Spracheingabe (#80): das kleine Modell „vosk-model-small-de-0.15“ von Alpha Cephei
 * (Apache 2.0), unverändert, mit Prüfsummen. Gebaut wird es nachvollziehbar in `.github/workflows/erkennungspaket.yml`;
 * die App lädt es nur auf Wunsch einmal herunter und löscht es auf Wunsch wieder.
 *
 * Aufbau: [DIRECTORY_GERMAN] mit den Modelldateien (`am/`, `conf/`, `graph/`, `ivector/`, `COPYING`, `README`), dazu
 * `NOTICE.txt` und [MANIFEST] mit Größe und Prüfsumme jeder Datei.
 */
object VoicePack {
    const val MANIFEST = "erkennungspaket.json"

    /** Modellverzeichnis für Deutsch im Paket. */
    const val DIRECTORY_GERMAN = "de"

    /** Version des Pakets: Ein geändertes Paket bekommt eine neue Version, ein neues Release und eine neue Prüfsumme. */
    const val VERSION = 1

    /** Eigenes Release im Repository, gebaut von `.github/workflows/erkennungspaket.yml`. */
    const val URL = "https://github.com/boerni328-bene/Moltobene/releases/download/erkennungspaket-de-$VERSION/erkennungspaket-de-$VERSION.zip"

    /** Feste Prüfsumme (SHA-256) von [MANIFEST]; das Verzeichnis enthält die Prüfsummen aller anderen Dateien. */
    const val MANIFEST_SHA256 = "6fe4a8186ef41a6cecaecf94d03ee4126fab58ca3901fe64e53d2a9aba4eb9bb"

    /** Ungefähre Größen für die Anzeige vor dem Download (gemessen am 14.10.2026). */
    const val DOWNLOAD_BYTES = 46_500_000L
    const val INSTALLED_BYTES = 96_000_000L

    /** Mehr wird nie geladen. */
    const val MAX_DOWNLOAD_BYTES = 80L * 1024 * 1024

    /** Mehr wird nie entpackt. */
    const val MAX_UNPACKED_BYTES = 160L * 1024 * 1024

    /** Erlaubte Dateinamen im Paket: einfache Namen in Verzeichnissen unter `de/`, plus `NOTICE.txt`. */
    val SAFE_NAME = Regex("(NOTICE\\.txt|de(/[A-Za-z0-9_-]+)*/[A-Za-z0-9_-]+(\\.[A-Za-z0-9]+)?)")

    /** Dateien laut [MANIFEST] mit ihrer Größe in Bytes. */
    fun files(directory: File): Map<String, Long> {
        val json = Json.parseToJsonElement(File(directory, MANIFEST).readText()).jsonObject
        return json.getValue("files").jsonObject.mapValues { (_, entry) -> entry.jsonObject.getValue("size").jsonPrimitive.long }
    }

    /** Ist das Paket vollständig vorhanden? Prüft Vorhandensein und Größe; die Prüfsummen prüft das Entpacken. */
    fun isComplete(directory: File): Boolean = runCatching {
        val files = files(directory)
        files.isNotEmpty() && files.all { (name, size) -> File(directory, name).let { it.isFile && it.length() == size } }
    }.getOrDefault(false)

    /** Modellverzeichnis für die Erkennung; null, wenn das Paket nicht vollständig da ist. */
    fun germanModel(directory: File): File? = File(directory, DIRECTORY_GERMAN).takeIf { isComplete(directory) && it.isDirectory }
}
