package com.moltobene.app.data.backup

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Schreibt eine Sicherungsdatei. Fotos werden Stück für Stück kopiert, nie alle auf einmal in den Speicher geladen. */
object BackupWriter {

    /**
     * @param photos Fotokennung → (Detailbild, Vorschaubild). Fehlende Dateien werden übersprungen.
     */
    fun write(
        out: OutputStream,
        manifest: BackupManifest,
        recipes: List<BackupRecipe>,
        photos: Map<String, Pair<File, File>>,
    ) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST_ENTRY))
            zip.write(BackupFormat.json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(BackupFormat.RECIPES_ENTRY))
            zip.write(BackupFormat.json.encodeToString(BackupRecipes.serializer(), BackupRecipes(recipes)).toByteArray())
            zip.closeEntry()

            photos.forEach { (photoId, files) ->
                val (full, thumb) = files
                if (full.isFile) copyEntry(zip, BackupFormat.photoEntry(photoId), full)
                if (thumb.isFile) copyEntry(zip, BackupFormat.thumbEntry(photoId), thumb)
            }
        }
    }

    private fun copyEntry(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
