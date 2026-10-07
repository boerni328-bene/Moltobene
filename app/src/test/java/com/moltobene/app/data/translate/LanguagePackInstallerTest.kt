package com.moltobene.app.data.translate

import com.moltobene.app.data.translate.LanguagePackInstaller.InstallException
import com.moltobene.app.data.translate.LanguagePackInstaller.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Sicheres Entpacken des Sprachpakets (#60) mit einem kleinen, selbst gebauten Paket. */
class LanguagePackInstallerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val files = mapOf(
        "en-de/config.json" to """{"eos":0}""".toByteArray(),
        "en-de/vocab.tsv" to "0\t</s>\n1\t<unk>\n".toByteArray(),
        "de-en/vocab.tsv" to "0\t</s>\n".toByteArray(),
    )

    private fun manifest(entries: Map<String, ByteArray> = files, sizes: Map<String, Long> = emptyMap()): ByteArray {
        val list = entries.entries.joinToString(",") { (name, bytes) ->
            "\"$name\": {\"sha256\": \"${LanguagePackInstaller.sha256(bytes)}\", \"size\": ${sizes[name] ?: bytes.size}}"
        }
        return """{"format": 1, "version": 1, "files": {$list}}""".toByteArray()
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

    private fun pack(manifest: ByteArray = manifest(), entries: Map<String, ByteArray> = files): ByteArray =
        zip(LanguagePack.MANIFEST to manifest, *entries.toList().toTypedArray())

    private fun unpack(archive: ByteArray, manifestSha: String, target: File = File(folder.root, "paket")) =
        LanguagePackInstaller.unpack(ByteArrayInputStream(archive), target, manifestSha)

    private fun assertProblem(problem: Problem, block: () -> Unit) {
        try {
            block()
            fail("Fehler erwartet: $problem")
        } catch (e: InstallException) {
            assertEquals(problem, e.problem)
        }
    }

    @Test
    fun richtigesPaketWirdEntpackt() {
        val manifest = manifest()
        val target = File(folder.root, "paket")
        unpack(pack(manifest), LanguagePackInstaller.sha256(manifest), target)
        files.forEach { (name, bytes) -> assertTrue(File(target, name).readBytes().contentEquals(bytes)) }
        assertTrue(LanguagePack.isComplete(target))
        assertEquals(files.mapValues { it.value.size.toLong() }, LanguagePack.files(target))
    }

    @Test
    fun falschePruefsummeDesVerzeichnisses() {
        val target = File(folder.root, "paket")
        assertProblem(Problem.DAMAGED) { unpack(pack(), "0".repeat(64), target) }
        assertFalse(LanguagePack.isComplete(target))
    }

    @Test
    fun veraenderteDatei() {
        val manifest = manifest()
        val changed = files + ("en-de/vocab.tsv" to "0\t</s>\n1\t<UNK>\n".toByteArray())
        assertProblem(Problem.DAMAGED) { unpack(pack(manifest, changed), LanguagePackInstaller.sha256(manifest)) }
    }

    @Test
    fun zuGrosseOderFehlendeDatei() {
        val longer = manifest(sizes = mapOf("de-en/vocab.tsv" to 3))
        assertProblem(Problem.DAMAGED) { unpack(pack(longer), LanguagePackInstaller.sha256(longer)) }
        val manifest = manifest()
        assertProblem(Problem.DAMAGED) { unpack(pack(manifest, files - "de-en/vocab.tsv"), LanguagePackInstaller.sha256(manifest)) }
    }

    @Test
    fun unbekannteDateiOderGefaehrlicherName() {
        val manifest = manifest()
        val extra = files + ("../ausserhalb.txt" to "x".toByteArray())
        assertProblem(Problem.DAMAGED) { unpack(pack(manifest, extra), LanguagePackInstaller.sha256(manifest)) }
        assertFalse(File(folder.root, "ausserhalb.txt").exists())
        val outside = manifest(files + ("../ausserhalb.txt" to "x".toByteArray()))
        assertProblem(Problem.DAMAGED) {
            unpack(pack(outside, files + ("../ausserhalb.txt" to "x".toByteArray())), LanguagePackInstaller.sha256(outside))
        }
        assertFalse(File(folder.root, "ausserhalb.txt").exists())
    }

    @Test
    fun verzeichnisMussZuerstKommen() {
        val manifest = manifest()
        val archive = zip(*files.toList().toTypedArray(), LanguagePack.MANIFEST to manifest)
        assertProblem(Problem.DAMAGED) { unpack(archive, LanguagePackInstaller.sha256(manifest)) }
    }

    @Test
    fun keinPaket() {
        assertProblem(Problem.DAMAGED) { unpack("<html>Fehler</html>".toByteArray(), "0".repeat(64)) }
    }
}
