package com.moltobene.app.data.voice

import com.moltobene.app.data.translate.LanguagePackInstaller
import com.moltobene.app.data.translate.LanguagePackInstaller.InstallException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Erkennungspaket (#80): sicheres Entpacken mit einem kleinen, selbst gebauten Paket. Mit der Umgebungsvariable
 * `VOICE_PACK_ARCHIVE` (und `VOICE_PACK_MANIFEST_SHA256`) prüft derselbe Test auch das echte Paket aus
 * `.github/workflows/erkennungspaket.yml`.
 */
class VoicePackTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val files = mapOf(
        "de/am/final.mdl" to ByteArray(300) { it.toByte() },
        "de/conf/mfcc.conf" to "--use-energy=false\n".toByteArray(),
        "de/graph/phones/word_boundary.int" to "1 nonword\n".toByteArray(),
        "de/COPYING" to "Apache License\n".toByteArray(),
        "NOTICE.txt" to "Herkunft\n".toByteArray(),
    )

    private fun manifest(entries: Map<String, ByteArray> = files): ByteArray {
        val list = entries.entries.joinToString(",") { (name, bytes) ->
            "\"$name\": {\"sha256\": \"${LanguagePackInstaller.sha256(bytes)}\", \"size\": ${bytes.size}}"
        }
        return """{"version": 1, "files": {$list}}""".toByteArray()
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun unpack(archive: ByteArray, manifestSha: String, target: File) = LanguagePackInstaller.unpack(
        ByteArrayInputStream(archive),
        target,
        manifestSha,
        manifestName = VoicePack.MANIFEST,
        safeName = VoicePack.SAFE_NAME,
        maxUnpackedBytes = VoicePack.MAX_UNPACKED_BYTES,
    )

    @Test
    fun richtigesPaketWirdEntpacktUndIstVollstaendig() {
        val manifest = manifest()
        val target = File(folder.root, "paket")
        unpack(zip(VoicePack.MANIFEST to manifest, *files.toList().toTypedArray()), LanguagePackInstaller.sha256(manifest), target)
        assertTrue(VoicePack.isComplete(target))
        assertEquals(files.keys, VoicePack.files(target).keys)
        assertNotNull(VoicePack.germanModel(target))
        assertTrue(File(target, "de/graph/phones/word_boundary.int").isFile)
    }

    @Test
    fun fehlendeDateiMachtDasPaketUnvollstaendig() {
        val manifest = manifest()
        val target = File(folder.root, "paket")
        unpack(zip(VoicePack.MANIFEST to manifest, *files.toList().toTypedArray()), LanguagePackInstaller.sha256(manifest), target)
        File(target, "de/am/final.mdl").delete()
        assertFalse(VoicePack.isComplete(target))
        assertNull(VoicePack.germanModel(target))
    }

    @Test
    fun veraenderteOderFremdeDateienWerdenAbgelehnt() {
        val manifest = manifest()
        val sha = LanguagePackInstaller.sha256(manifest)
        val changed = files + ("de/COPYING" to "Etwas anderes\n".toByteArray())
        assertDamaged { unpack(zip(VoicePack.MANIFEST to manifest, *changed.toList().toTypedArray()), sha, File(folder.root, "a")) }
        assertDamaged { unpack(zip(VoicePack.MANIFEST to manifest, *files.toList().toTypedArray(), "de/fremd.bin" to byteArrayOf(1)), sha, File(folder.root, "b")) }
        assertDamaged { unpack(zip(VoicePack.MANIFEST to manifest), sha, File(folder.root, "c")) }
        assertDamaged { unpack(zip(VoicePack.MANIFEST to manifest, *files.toList().toTypedArray()), "0".repeat(64), File(folder.root, "d")) }
    }

    @Test
    fun gefaehrlicheNamenWerdenAbgelehnt() {
        for (name in listOf("../de/x.bin", "/de/x.bin", "de/../x.bin", "andere/x.bin", "de/x y.bin", "de//x.bin")) {
            assertFalse(name, VoicePack.SAFE_NAME.matches(name))
            val entries = mapOf(name to byteArrayOf(1, 2, 3))
            val manifest = manifest(entries)
            assertDamaged {
                unpack(zip(VoicePack.MANIFEST to manifest, name to byteArrayOf(1, 2, 3)), LanguagePackInstaller.sha256(manifest), File(folder.root, "x"))
            }
        }
        for (name in files.keys + "de/ivector/online_cmvn.conf" + "de/README") {
            assertTrue(name, VoicePack.SAFE_NAME.matches(name))
        }
    }

    @Test
    fun echtesPaketAusDemBau() {
        val archive = System.getenv("VOICE_PACK_ARCHIVE")
        val manifestSha = System.getenv("VOICE_PACK_MANIFEST_SHA256")
        assumeTrue("Kein echtes Erkennungspaket angegeben", !archive.isNullOrEmpty() && !manifestSha.isNullOrEmpty())
        val target = File(folder.root, "echt")
        File(archive).inputStream().use { input ->
            LanguagePackInstaller.unpack(
                input,
                target,
                manifestSha,
                manifestName = VoicePack.MANIFEST,
                safeName = VoicePack.SAFE_NAME,
                maxUnpackedBytes = VoicePack.MAX_UNPACKED_BYTES,
            )
        }
        assertTrue(VoicePack.isComplete(target))
        val model = VoicePack.germanModel(target)
        assertNotNull(model)
        for (name in listOf("am/final.mdl", "conf/model.conf", "graph/HCLr.fst", "graph/Gr.fst", "ivector/final.ie", "COPYING")) {
            assertTrue(name, File(model, name).isFile)
        }
        val total = VoicePack.files(target).values.sum()
        println("Erkennungspaket entpackt: ${total / 1_000_000} MB, ${VoicePack.files(target).size} Dateien")
        assertTrue("Größer als in VoicePack angegeben", total <= VoicePack.INSTALLED_BYTES + 5_000_000)
    }

    private fun assertDamaged(block: () -> Unit) {
        try {
            block()
            fail("Fehler erwartet")
        } catch (e: InstallException) {
            assertEquals(LanguagePackInstaller.Problem.DAMAGED, e.problem)
        }
    }
}
