package com.moltobene.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageUsageTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val now = 10_000_000_000L

    private fun file(path: String, size: Int, modified: Long = now - 24L * 60 * 60 * 1000): File {
        val target = File(temp.root, path)
        target.parentFile.mkdirs()
        target.writeBytes(ByteArray(size))
        target.setLastModified(modified)
        return target
    }

    private fun usage() = StorageUsage(
        program = listOf(file("apk/base.apk", 1000)),
        database = listOf(file("db/moltobene.db", 100), File(temp.root, "db/moltobene.db-wal")),
        files = File(temp.root, "files"),
        noBackupFiles = File(temp.root, "nobackup"),
        packs = StorageUsage.Packs(File(temp.root, "nobackup/sprachpaket"), File(temp.root, "nobackup/erkennungspaket")),
        translations = File(temp.root, "nobackup/uebersetzungen"),
        cache = File(temp.root, "cache"),
        now = { now },
    )

    @Test
    fun `jeder Posten wird einzeln gezaehlt und die Summe stimmt`() {
        file("files/photos/a.jpg", 500)
        file("files/photos/a_thumb.jpg", 50)
        file("files/datastore/moltobene.preferences_pb", 7)
        file("nobackup/sprachpaket/model.bin", 3000)
        file("nobackup/erkennungspaket/de/model.bin", 2000)
        file("nobackup/uebersetzungen/x.json", 20)
        file("nobackup/recognition/s/page.jpg", 5)
        file("cache/share/r.jpg", 30)

        val result = runBlocking { usage().measure() }

        assertEquals(1000L, result[StorageUsage.Item.PROGRAM])
        assertEquals(500L, result[StorageUsage.Item.PHOTOS])
        assertEquals(50L, result[StorageUsage.Item.THUMBNAILS])
        assertEquals(100L, result[StorageUsage.Item.DATABASE])
        assertEquals(3000L, result[StorageUsage.Item.LANGUAGE_PACK])
        assertEquals(2000L, result[StorageUsage.Item.VOICE_PACK])
        assertEquals(20L, result[StorageUsage.Item.TRANSLATIONS])
        assertEquals(30L, result[StorageUsage.Item.CACHE])
        assertEquals(12L, result[StorageUsage.Item.OTHER])
        assertEquals(1000L + 500 + 50 + 100 + 3000 + 2000 + 20 + 30 + 12, result.total)
    }

    @Test
    fun `fehlende Ordner zaehlen als null`() {
        val result = runBlocking { usage().measure() }
        assertEquals(1100L, result.total)
        assertEquals(0L, result[StorageUsage.Item.PHOTOS])
    }

    @Test
    fun `Zwischenspeicher leeren behaelt neue Dateien und meldet die freigegebene Groesse`() {
        val old = file("cache/web/old.img", 400)
        val fresh = file("cache/camera/capture.jpg", 60, modified = now - 60_000)

        val freed = runBlocking { usage().clearCache() }

        assertEquals(400L, freed)
        assertFalse(old.exists())
        assertTrue(fresh.exists())
        assertFalse(File(temp.root, "cache/web").exists())
    }

    @Test
    fun `Leeren des Zwischenspeichers beruehrt Fotos und Pakete nicht`() {
        val photo = file("files/photos/a.jpg", 500)
        val pack = file("nobackup/sprachpaket/model.bin", 3000)
        file("cache/web/old.img", 10)

        runBlocking { usage().clearCache() }

        assertTrue(photo.exists())
        assertTrue(pack.exists())
    }
}
