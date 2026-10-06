package com.moltobene.app.data.ocr

import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Texterkennung mit PP-OCRv6 ([PaddleOcr]), ausschließlich auf dem Handy und ohne Internet.
 * Die Modelle liegen in assets/ocr und werden je Erkennung einmal geladen. Das Lesemodell kennt alle
 * Sprachen der App zugleich; die Sprache des Rezepts wird erst am gelesenen Text erkannt (oder ist gewählt).
 */
class TextRecognizer(private val context: Context) {

    /**
     * Eine Seite: [load] lädt das Foto, gelesen werden nur die [areas] – jeweils ohne das, was in
     * ihren [Area.blank]-Bereichen liegt (das steht schon in einem anderen Bereich).
     */
    class Page(val load: suspend () -> GrayImage, val areas: List<Area>)

    class Area(val area: CropArea, val blank: List<CropArea> = emptyList())

    /**
     * @param parts der Text Bereich für Bereich, in der Reihenfolge der Seiten und Bereiche
     * @param detected die Sprache wurde am Text eindeutig erkannt oder gewählt – sonst ist sie nur vermutet
     */
    class Result(val parts: List<String>, val language: String, val detected: Boolean = true) {
        /** Der ganze erkannte Text. */
        val text: String get() = parts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
    }

    /** Die Erkennung wurde über [Run.cancel] abgebrochen. */
    class CancelledException : Exception()

    /**
     * Für die Erkennung war zu wenig Speicher frei (#50). Statt eines Absturzes ein normaler Fehler:
     * Seiten und Eingaben bleiben erhalten, die Erkennung lässt sich wiederholen.
     */
    class LowMemoryException(cause: Throwable) : IOException("Zu wenig Speicher für die Texterkennung", cause)

    /**
     * Eine Erkennung (#43). Sie lässt sich jederzeit abbrechen – auch in den ersten Sekunden, bevor
     * das Lesen richtig begonnen hat, und mitten in einer Rechnung der Modelle. Darf von jedem Thread aus
     * abgebrochen werden.
     */
    class Run {
        @Volatile
        private var cancelled = false
        private var active: OrtSession.RunOptions? = null

        fun cancel() {
            cancelled = true
            synchronized(this) { active?.setTerminate(true) }
        }

        internal fun check() {
            if (cancelled) throw CancelledException()
        }

        internal fun attach(options: OrtSession.RunOptions?) = synchronized(this) {
            active = options
            if (cancelled) options?.setTerminate(true)
        }
    }

    private val mutex = Mutex()

    /**
     * @param pages lädt die Seiten nacheinander, damit nie mehrere Fotos zugleich im Speicher liegen
     * @param preferredLanguage Sprache, falls sie sich am Text nicht erkennen lässt
     * @param chosenLanguage vom Nutzer gewählte Sprache – dann wird sie nicht am Text erkannt
     * @param onProgress Seite (ab 0) und Fortschritt der ganzen Erkennung in Prozent; nur bei Änderung gemeldet
     */
    suspend fun recognize(
        run: Run,
        pages: List<Page>,
        preferredLanguage: String,
        chosenLanguage: String?,
        onProgress: (page: Int, percent: Int) -> Unit,
    ): Result = mutex.withLock {
        try {
            recognizePages(run, pages, preferredLanguage, chosenLanguage, onProgress)
        } catch (e: OutOfMemoryError) {
            throw LowMemoryException(e)
        } catch (e: LinkageError) {
            // Die Programmbibliothek der Erkennung ließ sich nicht laden, z. B. bei knappem Speicher.
            throw IOException("Texterkennung nicht verfügbar", e)
        }
    }

    private suspend fun recognizePages(
        run: Run,
        pages: List<Page>,
        preferredLanguage: String,
        chosenLanguage: String?,
        onProgress: (page: Int, percent: Int) -> Unit,
    ): Result =
        withContext(Dispatchers.Default) {
            run.check()
            val progress = Progress(pages.size, onProgress)
            val parts = mutableListOf<String>()
            val ocr = openModels()
            try {
                OrtSession.RunOptions().use { options ->
                    run.attach(options)
                    try {
                        pages.forEachIndexed { index, page ->
                            progress.startPage(index, page.areas.size)
                            val image = page.load()
                            run.check()
                            page.areas.forEachIndexed { area, frame ->
                                progress.startArea(area)
                                val crop = image.whiten(frame.blank).crop(frame.area)
                                val lines = ocr.recognize(crop, options) { done, total ->
                                    run.check()
                                    progress.report(DETECTION_SHARE + (100 - DETECTION_SHARE) * done / total)
                                }
                                parts += ReadingOrder.text(lines).trim()
                            }
                        }
                    } catch (e: OrtException) {
                        // Ein Abbruch beendet die laufende Rechnung des Modells mit einem Fehler.
                        run.check()
                        throw IOException("Texterkennung fehlgeschlagen", e)
                    } finally {
                        run.attach(null)
                    }
                }
            } finally {
                ocr.close()
            }
            val text = parts.joinToString("\n")
            val found = chosenLanguage?.let { TextLanguage.supportedOrDefault(it) } ?: TextLanguage.detect(text)
            Result(parts, found ?: TextLanguage.supportedOrDefault(preferredLanguage), detected = found != null)
        }

    /** Lädt die beiden Modelle und die Zeichenliste aus den mitgelieferten Dateien. */
    private suspend fun openModels(): PaddleOcr = withContext(Dispatchers.IO) {
        val assets = context.assets
        val dir = PaddleOcr.MODEL_DIR
        // Außerhalb des Java-Speichers (#50); available() liefert bei Dateien aus assets ihre ganze Größe.
        val detection = assets.open("$dir/${PaddleOcr.DETECTION_MODEL}").use { PaddleOcr.modelBuffer(it, it.available()) }
        val recognition = assets.open("$dir/${PaddleOcr.RECOGNITION_MODEL}").use { PaddleOcr.modelBuffer(it, it.available()) }
        val dictionary = assets.open("$dir/${PaddleOcr.DICTIONARY}").bufferedReader(Charsets.UTF_8).use { it.readLines() }
        PaddleOcr(detection, recognition, dictionary)
    }

    /**
     * Fortschritt der ganzen Erkennung aus Seite, Bereich und gelesenen Zeilen. Er geht nie zurück und wird nur
     * in ganzen Prozentschritten gemeldet, damit die Anzeige nicht ständig neu gezeichnet wird, während der
     * Prozessor ausgelastet ist.
     */
    private class Progress(private val pageCount: Int, private val onProgress: (page: Int, percent: Int) -> Unit) {
        private var page = 0
        private var areaCount = 1
        private var area = 0
        private var reported = -1

        fun startPage(index: Int, areas: Int) {
            page = index
            areaCount = areas.coerceAtLeast(1)
            area = 0
            report(0)
        }

        fun startArea(index: Int) {
            area = index
            report(0)
        }

        /** [percent] des gerade gelesenen Bereichs. */
        fun report(percent: Int) {
            val withinPage = (area + percent.coerceIn(0, 100) / 100f) / areaCount
            val total = ((page + withinPage) / pageCount * 100).toInt().coerceIn(0, 100)
            if (total <= reported) return
            reported = total
            onProgress(page, total)
        }
    }

    private companion object {
        /** Anteil der Zeilensuche am Fortschritt eines Bereichs; der Rest ist das Lesen der Zeilen. */
        private const val DETECTION_SHARE = 15
    }
}
