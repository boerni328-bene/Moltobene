package com.moltobene.app.data

import java.io.File

/**
 * Räumt beim App-Start Reste älterer Versionen auf (#49):
 * - die Sprachpakete von Tesseract (bis Version 0.13, rund 12 MB), die die neue Texterkennung nicht mehr braucht;
 * - Dateien, die die Microsoft-Telemetrie aus ONNX Runtime in Version 0.14.0 im Zwischenspeicher der App angelegt
 *   haben kann (eine kleine Datenbank für noch nicht gesendete Ereignisse und eine Sitzungsdatei).
 *
 * Die App selbst legt im Zwischenspeicher nur Ordner an (Kamera, Teilen, Wiederherstellen), nie solche Dateien.
 * Reines Kotlin, per Unit-Test prüfbar. Läuft nicht auf dem Hauptthread.
 */
object LegacyCleanup {

    private const val OLD_TESSERACT_DIR = "tesseract"

    /** Datenbank der Telemetrie samt Begleitdateien und Sitzungsdatei, direkt im Zwischenspeicher. */
    private val TELEMETRY_FILE = Regex(".+\\.(db|db-journal|db-wal|db-shm|ses)", RegexOption.IGNORE_CASE)

    fun run(filesDir: File, noBackupFilesDir: File, cacheDir: File) {
        listOf(File(noBackupFilesDir, OLD_TESSERACT_DIR), File(filesDir, OLD_TESSERACT_DIR))
            .filter { it.exists() }
            .forEach { it.deleteRecursively() }
        cacheDir.listFiles()
            ?.filter { it.isFile && TELEMETRY_FILE.matches(it.name) }
            ?.forEach { it.delete() }
    }
}
