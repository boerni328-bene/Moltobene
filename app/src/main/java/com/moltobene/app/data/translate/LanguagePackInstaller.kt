package com.moltobene.app.data.translate

import com.moltobene.app.data.StorageFull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Entpackt das Sprachpaket (#60) schon während des Herunterladens, damit das Archiv nicht zusätzlich Platz braucht.
 * Fremde Dateien gelten als unsicher, deshalb:
 * - Die erste Datei muss [LanguagePack.MANIFEST] sein, mit der fest in der App hinterlegten Prüfsumme.
 * - Jede weitere Datei muss dort mit Name, Größe und Prüfsumme stehen; nur einfache Namen wie `en-de/vocab.tsv`.
 * - Es wird nie mehr geschrieben, als das Verzeichnis angibt; fehlt etwas, gilt das Paket als beschädigt.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object LanguagePackInstaller {

    class InstallException(val problem: Problem) : Exception(problem.name)

    enum class Problem {
        /** Das Paket stimmt nicht mit der Prüfsumme überein oder ist unvollständig. */
        DAMAGED,

        /** Kein Platz mehr auf dem Handy. */
        NO_SPACE,
    }

    private class Entry(val sha256: String, val size: Long)

    /**
     * Liest das Archiv aus [input] und legt die Dateien in [target] ab (wird vorher geleert).
     * Fehler beim Lesen aus [input], z. B. eine abgebrochene Verbindung, kommen unverändert als [IOException].
     */
    fun unpack(input: InputStream, target: File, manifestSha256: String) {
        target.deleteRecursively()
        if (!target.mkdirs()) throw InstallException(Problem.NO_SPACE)
        val zip = ZipInputStream(input)
        try {
            val first = zip.nextEntry ?: throw InstallException(Problem.DAMAGED)
            if (first.name != LanguagePack.MANIFEST) throw InstallException(Problem.DAMAGED)
            val manifestBytes = readSmall(zip)
            if (sha256(manifestBytes) != manifestSha256) throw InstallException(Problem.DAMAGED)
            val entries = parseManifest(manifestBytes)
            val written = HashSet<String>()
            while (true) {
                val entry = zip.nextEntry ?: break
                val expected = entries[entry.name]
                if (entry.isDirectory) continue
                if (expected == null || !written.add(entry.name)) throw InstallException(Problem.DAMAGED)
                copyChecked(zip, File(target, entry.name), expected)
            }
            if (written != entries.keys) throw InstallException(Problem.DAMAGED)
            // Zuletzt: Erst mit dem Verzeichnis gilt das Paket als vollständig (LanguagePack.isComplete).
            write(File(target, LanguagePack.MANIFEST)) { it.write(manifestBytes) }
        } catch (e: ZipException) {
            throw InstallException(Problem.DAMAGED)
        }
    }

    /** Name → (Prüfsumme, Größe); nur einfache Namen in einem Verzeichnis je Richtung, nicht zu groß. */
    private fun parseManifest(bytes: ByteArray): Map<String, Entry> {
        val files = runCatching {
            Json.parseToJsonElement(bytes.decodeToString()).jsonObject.getValue("files").jsonObject.mapValues { (_, value) ->
                val entry = value.jsonObject
                Entry(entry.getValue("sha256").jsonPrimitive.content, entry.getValue("size").jsonPrimitive.long)
            }
        }.getOrNull() ?: throw InstallException(Problem.DAMAGED)
        val total = files.values.sumOf { it.size }
        if (files.isEmpty() || files.keys.any { !SAFE_NAME.matches(it) } || files.values.any { it.size < 0 } || total > MAX_UNPACKED_BYTES) {
            throw InstallException(Problem.DAMAGED)
        }
        return files
    }

    private fun readSmall(input: InputStream): ByteArray {
        val bytes = input.readNBytesCompat(MAX_MANIFEST_BYTES + 1)
        if (bytes.size > MAX_MANIFEST_BYTES) throw InstallException(Problem.DAMAGED)
        return bytes
    }

    /** Kopiert genau [expected].size Bytes und prüft die Prüfsumme; mehr oder weniger heißt beschädigt. */
    private fun copyChecked(input: InputStream, file: File, expected: Entry) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        write(file) { output ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > expected.size) throw InstallException(Problem.DAMAGED)
                digest.update(buffer, 0, read)
                output.write(buffer, 0, read)
            }
        }
        if (total != expected.size || digest.digest().toHex() != expected.sha256) throw InstallException(Problem.DAMAGED)
    }

    /** Schreibt in eine Datei; Fehler beim Schreiben (meist voller Speicher) werden zu [InstallException]. */
    private fun write(file: File, block: (OutputStream) -> Unit) {
        try {
            file.parentFile?.mkdirs()
            file.outputStream().use(block)
        } catch (e: InstallException) {
            throw e
        } catch (e: IOException) {
            if (StorageFull.isCause(e)) throw InstallException(Problem.NO_SPACE)
            throw e
        }
    }

    internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    /** InputStream.readNBytes gibt es erst ab Android 13. */
    private fun InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (out.size() < max) {
            val read = read(buffer, 0, minOf(buffer.size, max - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private const val BUFFER_SIZE = 64 * 1024
    private const val MAX_MANIFEST_BYTES = 64 * 1024
    const val MAX_UNPACKED_BYTES = 400L * 1024 * 1024
    private val SAFE_NAME = Regex("[a-z]{2}-[a-z]{2}/[A-Za-z0-9_-]+(\\.[A-Za-z0-9]+)?")
}
