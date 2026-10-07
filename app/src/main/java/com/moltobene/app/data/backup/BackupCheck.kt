package com.moltobene.app.data.backup

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Prüft eine gerade geschriebene Sicherungsdatei (#51), ohne die Fotos noch einmal auszupacken:
 * - [entryNames] liest das Inhaltsverzeichnis am Ende der ZIP-Datei. Es wird als Letztes geschrieben – fehlt es,
 *   ist die Datei unvollständig (z. B. Speicher voll, App beendet).
 * - [recipeCount] liest nur den Anfang der Datei und zählt die Rezepte.
 * - [streamingCheck] liest die ganze Datei für Speicherorte, an denen sich nicht springen lässt.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object BackupCheck {

    /** Die Datei ist unvollständig oder enthält nicht, was geschrieben wurde. */
    class IncompleteException(message: String) : IOException(message)

    /** Namen aller Einträge laut Inhaltsverzeichnis am Ende der Datei. */
    fun entryNames(channel: SeekableByteChannel): List<String> {
        val size = channel.size()
        if (size < END_RECORD_SIZE) throw IncompleteException("Datei zu kurz")
        val tailSize = minOf(size, (END_RECORD_SIZE + MAX_COMMENT).toLong()).toInt()
        val tail = read(channel, size - tailSize, tailSize)
        val end = (tailSize - END_RECORD_SIZE downTo 0).firstOrNull { tail.getInt(it) == END_SIGNATURE }
            ?: throw IncompleteException("Inhaltsverzeichnis fehlt")
        val endPosition = size - tailSize + end

        var count = tail.getShort(end + 10).toInt() and 0xFFFF
        var directorySize = tail.getInt(end + 12).toLong() and 0xFFFFFFFFL
        var directoryOffset = tail.getInt(end + 16).toLong() and 0xFFFFFFFFL
        // Sehr große Sicherungen schreibt Java im Format ZIP64; dessen Angaben stehen kurz vor dem Ende.
        if (count == 0xFFFF || directorySize == 0xFFFFFFFFL || directoryOffset == 0xFFFFFFFFL) {
            if (endPosition < ZIP64_LOCATOR_SIZE) throw IncompleteException("ZIP64-Angaben fehlen")
            val locator = read(channel, endPosition - ZIP64_LOCATOR_SIZE, ZIP64_LOCATOR_SIZE)
            if (locator.getInt(0) != ZIP64_LOCATOR_SIGNATURE) throw IncompleteException("ZIP64-Angaben fehlen")
            val record = read(channel, locator.getLong(8), ZIP64_END_RECORD_SIZE)
            if (record.getInt(0) != ZIP64_END_SIGNATURE) throw IncompleteException("ZIP64-Angaben beschädigt")
            count = record.getLong(32).toInt()
            directorySize = record.getLong(40)
            directoryOffset = record.getLong(48)
        }
        if (directoryOffset < 0 || directorySize < 0 || directoryOffset + directorySize > endPosition ||
            directorySize > MAX_DIRECTORY_BYTES
        ) {
            throw IncompleteException("Inhaltsverzeichnis passt nicht zur Datei")
        }

        val directory = read(channel, directoryOffset, directorySize.toInt())
        val names = ArrayList<String>(count)
        var position = 0
        repeat(count) {
            if (position + CENTRAL_HEADER_SIZE > directory.limit() || directory.getInt(position) != CENTRAL_SIGNATURE) {
                throw IncompleteException("Inhaltsverzeichnis beschädigt")
            }
            val nameLength = directory.getShort(position + 28).toInt() and 0xFFFF
            val extraLength = directory.getShort(position + 30).toInt() and 0xFFFF
            val commentLength = directory.getShort(position + 32).toInt() and 0xFFFF
            val nameBytes = ByteArray(nameLength)
            directory.position(position + CENTRAL_HEADER_SIZE)
            directory.get(nameBytes)
            names += nameBytes.toString(Charsets.UTF_8)
            position += CENTRAL_HEADER_SIZE + nameLength + extraLength + commentLength
        }
        return names
    }

    /** Zahl der Rezepte in der Rezeptliste; gelesen wird nur bis zu ihr, Fotos kommen erst danach. */
    fun recipeCount(input: InputStream): Int = try {
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null && entry.name != BackupFormat.RECIPES_ENTRY) entry = zip.nextEntry
            if (entry == null) throw IncompleteException("Rezeptliste fehlt")
            countRecipes(readAll(zip))
        }
    } catch (e: ZipException) {
        throw IncompleteException("Keine lesbare Sicherungsdatei")
    }

    /** Liest die ganze Datei Eintrag für Eintrag: Zahl der Einträge und der Rezepte. */
    fun streamingCheck(input: InputStream): Pair<Int, Int> = try {
        ZipInputStream(input.buffered()).use { zip ->
            var entries = 0
            var recipes = -1
            val skip = ByteArray(64 * 1024)
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                if (entry.name == BackupFormat.RECIPES_ENTRY) {
                    recipes = countRecipes(readAll(zip))
                } else {
                    while (zip.read(skip) >= 0) Unit
                }
            }
            if (recipes < 0) throw IncompleteException("Rezeptliste fehlt")
            entries to recipes
        }
    } catch (e: ZipException) {
        throw IncompleteException("Keine lesbare Sicherungsdatei")
    }

    /** Stimmt die Datei mit dem überein, was geschrieben wurde? Sonst [IncompleteException]. */
    fun verify(entries: Int, recipes: Int, expectedEntries: Int, expectedRecipes: Int) {
        if (entries != expectedEntries) throw IncompleteException("$entries statt $expectedEntries Einträge")
        if (recipes != expectedRecipes) throw IncompleteException("$recipes statt $expectedRecipes Rezepte")
    }

    private fun countRecipes(json: ByteArray): Int = try {
        BackupFormat.json.parseToJsonElement(json.toString(Charsets.UTF_8)).jsonObject["recipes"]?.jsonArray?.size
            ?: throw IncompleteException("Rezeptliste leer")
    } catch (e: IllegalArgumentException) {
        throw IncompleteException("Rezeptliste unvollständig")
    }

    private fun readAll(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (out.size() + read > MAX_RECIPES_BYTES) throw IncompleteException("Rezeptliste zu groß")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun read(channel: SeekableByteChannel, position: Long, length: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(position)
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw IncompleteException("Datei endet zu früh")
        }
        buffer.flip()
        return buffer
    }

    private const val END_SIGNATURE = 0x06054b50
    private const val CENTRAL_SIGNATURE = 0x02014b50
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
    private const val ZIP64_END_SIGNATURE = 0x06064b50
    private const val END_RECORD_SIZE = 22
    private const val CENTRAL_HEADER_SIZE = 46
    private const val ZIP64_LOCATOR_SIZE = 20
    private const val ZIP64_END_RECORD_SIZE = 56
    private const val MAX_COMMENT = 0xFFFF
    private const val MAX_DIRECTORY_BYTES = 64L * 1024 * 1024

    /** Wie beim Lesen einer Sicherung (BackupReader.Limits.maxJsonBytes). */
    private const val MAX_RECIPES_BYTES = 50 * 1024 * 1024
}
