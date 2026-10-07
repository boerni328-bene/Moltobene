package com.moltobene.app.data.backup

import com.moltobene.app.data.StorageFull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files

/** Prüfung einer gerade geschriebenen Sicherungsdatei (#51), ohne die Fotos noch einmal auszupacken. */
class BackupCheckTest {

    private val dir: File = Files.createTempDirectory("sicherung").toFile()

    /** Schreibt eine Sicherung mit [recipes] Rezepten und je einem Foto samt Vorschau; liefert Datei und Einträge. */
    private fun writeBackup(recipes: Int): Pair<File, Int> {
        val photos = (1..recipes).associate { index ->
            val id = "00000000-0000-4000-8000-%012d".format(index)
            val full = File(dir, "$id.jpg").apply { writeBytes(ByteArray(5_000) { (it % 7).toByte() }) }
            val thumb = File(dir, "${id}_thumb.jpg").apply { writeBytes(ByteArray(500)) }
            id to (full to thumb)
        }
        val backupRecipes = photos.keys.mapIndexed { index, photoId ->
            BackupRecipe(id = "10000000-0000-4000-8000-%012d".format(index), title = "Rezept $index", photos = listOf(photoId), createdAt = 0, updatedAt = 0)
        }
        val file = File(dir, "sicherung.zip")
        val entries = file.outputStream().use {
            BackupWriter.write(it, BackupManifest(appVersion = "test", createdAt = 0, recipeCount = recipes), backupRecipes, photos)
        }
        return file to entries
    }

    @Test
    fun vollstaendigeSicherung() {
        val (file, entries) = writeBackup(recipes = 3)
        assertEquals(2 + 3 * 2, entries)
        val names = RandomAccessFile(file, "r").channel.use { BackupCheck.entryNames(it) }
        assertEquals(entries, names.size)
        assertTrue(BackupFormat.RECIPES_ENTRY in names)
        val recipes = file.inputStream().use { BackupCheck.recipeCount(it) }
        assertEquals(3, recipes)
        BackupCheck.verify(names.size, recipes, expectedEntries = entries, expectedRecipes = 3)

        // Derselbe Befund, wenn die Datei ganz gelesen werden muss.
        assertEquals(entries to 3, file.inputStream().use { BackupCheck.streamingCheck(it) })
    }

    @Test(expected = BackupCheck.IncompleteException::class)
    fun abgeschnitteneSicherungFaelltAuf() {
        val (file, _) = writeBackup(recipes = 3)
        // Wie bei vollem Speicher: Das Ende mit dem Inhaltsverzeichnis fehlt.
        RandomAccessFile(file, "rw").use { it.setLength(it.length() - 40) }
        RandomAccessFile(file, "r").channel.use { BackupCheck.entryNames(it) }
    }

    @Test(expected = BackupCheck.IncompleteException::class)
    fun fehlendeRezepteFallenAuf() {
        BackupCheck.verify(entries = 8, recipes = 2, expectedEntries = 8, expectedRecipes = 3)
    }

    @Test(expected = IOException::class)
    fun abgeschnitteneSicherungBeimGanzenLesen() {
        val (file, _) = writeBackup(recipes = 3)
        RandomAccessFile(file, "rw").use { it.setLength(it.length() / 2) }
        file.inputStream().use { BackupCheck.streamingCheck(it) }
    }

    @Test
    fun speicherVoll() {
        assertTrue(StorageFull.isCause(IOException("write failed: ENOSPC (No space left on device)")))
        assertTrue(StorageFull.isCause(IllegalStateException("Fehler", IOException("No space left on device"))))
        assertFalse(StorageFull.isCause(IOException("Datei nicht lesbar")))
    }
}
