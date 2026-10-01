package com.moltobene.app.data.ocr

import android.content.Context
import android.net.Uri
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Hält die gelesenen Seiten eines Formulars fest, bis das Rezept gespeichert oder verworfen ist
 * (Issues #37, #38). Beendet Android die App im Hintergrund, gehen so weder die Seiten noch ein
 * schon fertiges Ergebnis verloren, und bis zum Speichern lassen sich die Seiten zum Prüfen ansehen:
 * - Ausgewählte Fotos werden sofort kopiert, denn die Leseerlaubnis der Fotoauswahl gilt nur vorübergehend.
 *   Das Kamerafoto wird übernommen, damit die nächste Aufnahme es nicht überschreibt.
 * - Ein Ergebnis, das im Hintergrund fertig wird, kommt in eine Datei und wird beim Zurückkehren übernommen.
 *
 * Alles liegt in noBackupFilesDir und geht weder in Sicherungen noch beim Umzug auf ein neues Handy mit.
 * Die Seiten sind unveränderte Kopien (samt Aufnahmeort). Beim Speichern werden sie auf Wunsch als
 * Originalseiten neu gespeichert ([com.moltobene.app.data.photos.PhotoStore.importPage]); danach bzw. beim
 * Verwerfen wird alles gelöscht, übrig gebliebene Reste beim nächsten Start ([deleteLeftovers]).
 */
class PendingRecognition(private val context: Context, private val photoStore: PhotoStore) {

    private val rootDir: File get() = File(context.noBackupFilesDir, ROOT_DIR)

    private fun sessionDir(session: String): File = File(rootDir, session)

    private fun pageFile(session: String, name: String): File = File(sessionDir(session), name)

    private fun resultFile(session: String): File = File(sessionDir(session), RESULT_FILE)

    fun newSession(): String = UUID.randomUUID().toString()

    fun pageUri(session: String, name: String): Uri = Uri.fromFile(pageFile(session, name))

    /** Kopiert ein ausgewähltes Foto als Seite in die Erkennung [session]. Liefert den Namen der Seite. */
    suspend fun keepPage(session: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val name = UUID.randomUUID().toString()
        val target = pageFile(session, name).apply { parentFile?.mkdirs() }
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Foto nicht lesbar")
        try {
            input.use { source -> target.outputStream().use { copyLimited(source, it) } }
        } catch (e: IOException) {
            target.delete()
            throw e
        }
        name
    }

    /** Übernimmt das gerade aufgenommene Kamerafoto als Seite. Liefert den Namen der Seite. */
    suspend fun keepCameraPage(session: String): String {
        val name = UUID.randomUUID().toString()
        photoStore.moveCameraCapture(pageFile(session, name))
        return name
    }

    /** Sichert ein Ergebnis, das fertig wurde, während die App im Hintergrund war. */
    suspend fun saveResult(session: String, result: TextRecognizer.Result) = withContext(Dispatchers.IO) {
        val target = resultFile(session).apply { parentFile?.mkdirs() }
        val temp = File(target.parentFile, "$RESULT_FILE.tmp")
        temp.writeText(SavedResult.encode(result))
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Ergebnis nicht speicherbar")
        }
    }

    /** Das gesicherte Ergebnis der Erkennung [session]; null, wenn sie nicht fertig wurde. */
    suspend fun loadResult(session: String): TextRecognizer.Result? = withContext(Dispatchers.IO) {
        val file = resultFile(session)
        if (file.isFile) SavedResult.decode(file.readText()) else null
    }

    /** Löscht ein gesichertes Ergebnis, sobald es im Rezept steht. */
    suspend fun deleteResult(session: String) = withContext(Dispatchers.IO) {
        resultFile(session).delete()
    }

    /** Löscht einzelne Seiten, z. B. die einer abgebrochenen Erkennung. */
    suspend fun deletePages(session: String, names: List<String>) = withContext(Dispatchers.IO) {
        names.forEach { pageFile(session, it).delete() }
    }

    /** Löscht alle Seiten und das Ergebnis der Erkennung [session]. */
    suspend fun delete(session: String) = withContext(Dispatchers.IO) {
        sessionDir(session).deleteRecursively()
    }

    /**
     * Löscht Reste von Erkennungen, die nicht zu Ende geführt wurden, z. B. weil die App aus der Liste
     * der zuletzt genutzten Apps entfernt wurde. Jüngere bleiben, sie können noch zu einem offenen Rezept gehören.
     */
    suspend fun deleteLeftovers() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - LEFTOVER_GRACE_MILLIS
        rootDir.listFiles().orEmpty().filter { it.lastModified() < cutoff }.forEach { it.deleteRecursively() }
    }

    /** Kopiert höchstens [MAX_PAGE_BYTES], damit ein riesiges Bild den Speicher nicht füllt. */
    private fun copyLimited(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_PAGE_BYTES) throw IOException("Foto zu groß")
            output.write(buffer, 0, read)
        }
    }

    private companion object {
        const val ROOT_DIR = "recognition"
        const val RESULT_FILE = "result.txt"
        const val MAX_PAGE_BYTES = 40L * 1024 * 1024
        const val LEFTOVER_GRACE_MILLIS = 60L * 60 * 1000
    }
}

/** Aufbau der Ergebnisdatei: in der ersten Zeile die Sprache, danach der erkannte Text. */
internal object SavedResult {

    fun encode(result: TextRecognizer.Result): String = result.language + "\n" + result.text

    fun decode(content: String): TextRecognizer.Result? {
        val language = content.substringBefore('\n', missingDelimiterValue = "").trim()
        val text = content.substringAfter('\n', missingDelimiterValue = "")
        if (language.isEmpty() || text.isBlank()) return null
        return TextRecognizer.Result(text, language)
    }
}
