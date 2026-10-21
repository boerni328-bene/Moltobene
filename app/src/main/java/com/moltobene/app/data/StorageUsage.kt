package com.moltobene.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wie viel Speicher die App belegt, nach Posten getrennt und in Summe (Einstellungen → Speicher). Zählt nur Dateien der
 * App selbst; die Sicherungsdatei liegt außerhalb und wird nicht gezählt. Alles läuft auf dem Hintergrundthread.
 *
 * @param program die Programmdateien (APK und Teile davon), vom System verwaltet
 * @param database die Datenbankdatei samt Journal-Dateien
 * @param files `filesDir`: Fotos (`photos/`) und Einstellungen
 * @param noBackupFiles `noBackupFilesDir`: Pakete, Übersetzungen, laufende Texterkennung
 * @param cache `cacheDir`
 */
class StorageUsage(
    private val program: List<File>,
    private val database: List<File>,
    private val files: File,
    private val noBackupFiles: File,
    private val packs: Packs,
    private val translations: File,
    private val cache: File,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Die Ordner der beiden Pakete, die sich löschen und neu laden lassen. */
    class Packs(val language: File, val voice: File)

    /** Die Posten der Anzeige, in der Reihenfolge, in der sie gezeigt werden. */
    enum class Item { PROGRAM, PHOTOS, THUMBNAILS, DATABASE, LANGUAGE_PACK, VOICE_PACK, TRANSLATIONS, CACHE, OTHER }

    class Result(val bytes: Map<Item, Long>) {
        val total: Long get() = bytes.values.sum()

        operator fun get(item: Item): Long = bytes[item] ?: 0L
    }

    suspend fun measure(): Result = withContext(Dispatchers.IO) {
        val photoFiles = File(files, PHOTO_DIR).listFiles().orEmpty().filter { it.isFile }
        val thumbnails = photoFiles.filter { it.name.endsWith(THUMB_SUFFIX) }.sumOf { it.length() }
        val photos = photoFiles.sumOf { it.length() } - thumbnails
        val languagePack = sizeOf(packs.language)
        val voicePack = sizeOf(packs.voice)
        val translated = sizeOf(translations)
        // Alles Übrige in den beiden Ordnern (Einstellungen, laufende Texterkennung …), damit die Summe stimmt.
        val other = (sizeOf(files) + sizeOf(noBackupFiles) - photos - thumbnails - languagePack - voicePack - translated)
            .coerceAtLeast(0L)
        Result(
            linkedMapOf(
                Item.PROGRAM to program.sumOf { sizeOf(it) },
                Item.PHOTOS to photos,
                Item.THUMBNAILS to thumbnails,
                Item.DATABASE to database.sumOf { sizeOf(it) },
                Item.LANGUAGE_PACK to languagePack,
                Item.VOICE_PACK to voicePack,
                Item.TRANSLATIONS to translated,
                Item.CACHE to sizeOf(cache),
                Item.OTHER to other,
            ),
        )
    }

    /**
     * Leert den Zwischenspeicher: Dateien, die dort liegen, lassen sich jederzeit neu erzeugen. Dateien der letzten
     * Minuten bleiben, damit nichts verschwindet, was gerade gebraucht wird (z. B. ein Foto vom Kamera-Knopf).
     * @return die freigegebene Größe in Byte
     */
    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val cutoff = now() - CACHE_GRACE_MILLIS
        var freed = 0L
        fun clean(dir: File) {
            for (child in dir.listFiles().orEmpty()) {
                if (child.isDirectory) {
                    clean(child)
                    child.delete() // gelingt nur, wenn der Ordner leer ist
                } else if (child.lastModified() < cutoff) {
                    val size = child.length()
                    if (child.delete()) freed += size
                }
            }
        }
        clean(cache)
        freed
    }

    companion object {
        const val PHOTO_DIR = "photos"
        const val THUMB_SUFFIX = "_thumb.jpg"

        /** Dateien jünger als zehn Minuten bleiben beim Leeren des Zwischenspeichers. */
        const val CACHE_GRACE_MILLIS = 10L * 60 * 1000

        /** Größe einer Datei oder eines ganzen Ordners samt Unterordnern; 0, wenn es sie nicht gibt. */
        fun sizeOf(file: File): Long = when {
            file.isFile -> file.length()
            file.isDirectory -> file.listFiles().orEmpty().sumOf { sizeOf(it) }
            else -> 0L
        }
    }
}
