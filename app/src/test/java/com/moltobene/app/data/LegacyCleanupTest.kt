package com.moltobene.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacyCleanupTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun entferntTesseractUndTelemetrieDateienAberNichtsAnderes() {
        val files = folder.newFolder("files")
        val noBackup = folder.newFolder("no_backup")
        val cache = folder.newFolder("cache")
        val tesseract = File(noBackup, "tesseract/tessdata").apply { mkdirs() }
        File(tesseract, "deu.traineddata").writeText("alt")
        val photo = File(files, "photos/abc.jpg").apply { parentFile!!.mkdirs(); writeText("Foto") }
        val telemetry = listOf("6d084bbf6a9644ef83f40a77c9e34580.db", "6d084bbf.db-journal", "6d084bbf.db.ses")
            .map { File(cache, it).apply { writeText("x") } }
        // Ordner der App im Zwischenspeicher bleiben, auch wenn ihr Inhalt zufällig so heißt.
        val cameraDir = File(cache, "camera").apply { mkdirs() }
        val cameraPhoto = File(cameraDir, "seite.db").apply { writeText("Foto") }

        LegacyCleanup.run(files, noBackup, cache)

        assertFalse(File(noBackup, "tesseract").exists())
        telemetry.forEach { assertFalse(it.name, it.exists()) }
        assertTrue(photo.exists())
        assertTrue(cameraPhoto.exists())
    }

    @Test
    fun ohneResteGeschiehtNichts() {
        val root = folder.newFolder("leer")
        LegacyCleanup.run(File(root, "files"), File(root, "no_backup"), File(root, "cache"))
        assertTrue(root.listFiles()!!.isEmpty())
    }
}
