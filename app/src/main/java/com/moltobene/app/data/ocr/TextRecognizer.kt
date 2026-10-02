package com.moltobene.app.data.ocr

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import com.moltobene.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Texterkennung mit Tesseract, ausschließlich auf dem Handy und ohne Internet.
 * Die Sprachpakete liegen in assets/tessdata und werden beim ersten Gebrauch in den App-Speicher kopiert.
 * Ohne gewählte Sprache wird mit der zuletzt erkannten begonnen. Bis die Sprache am Text eindeutig erkannt
 * ist, wird jede Seite geprüft; passt sie nicht, wird die Seite mit dem richtigen Sprachpaket noch einmal
 * gelesen (sonst gehen z. B. Akzente verloren).
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
     * Eine Erkennung (#43). Sie lässt sich jederzeit abbrechen – auch in den ersten Sekunden, bevor
     * das Lesen richtig begonnen hat. Darf von jedem Thread aus abgebrochen werden.
     */
    class Run {
        @Volatile
        private var cancelled = false
        private var active: TessBaseAPI? = null

        fun cancel() {
            cancelled = true
            synchronized(this) { active?.stop() }
        }

        internal fun check() {
            if (cancelled) throw CancelledException()
        }

        internal fun attach(tess: TessBaseAPI?) = synchronized(this) {
            active = tess
            if (cancelled) tess?.stop()
        }
    }

    private val mutex = Mutex()

    /** In noBackupFilesDir: Die Sprachpakete lassen sich jederzeit neu kopieren und gehören weder in Sicherungen noch zum Umzug. */
    private val dataDir: File get() = File(context.noBackupFilesDir, DATA_DIR)

    /**
     * @param pages lädt die Seiten nacheinander, damit nie mehrere Fotos zugleich im Speicher liegen
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
        withContext(Dispatchers.Default) {
            run.check()
            var language = TextLanguage.supportedOrDefault(chosenLanguage ?: preferredLanguage)
            var detected = chosenLanguage != null
            val parts = mutableListOf<String>()
            val progress = Progress(pages.size, onProgress)
            Reader(run, progress).use { reader ->
                pages.forEachIndexed { index, page ->
                    progress.startPage(index, page.areas.size)
                    val image = page.load()
                    run.check()
                    val crops = page.areas.map { image.whiten(it.blank).crop(it.area) }
                    suspend fun readAll() = crops.mapIndexed { area, crop ->
                        progress.startArea(area)
                        reader.read(crop, language)
                    }
                    var texts = readAll()
                    // Ist die erste Seite unklar (z. B. nur ein kurzer Ausschnitt), entscheidet der Text bis hierher.
                    if (!detected) {
                        TextLanguage.detect((parts + texts).joinToString("\n"))?.let { found ->
                            detected = true
                            if (found != language) {
                                language = found
                                texts = readAll()
                            }
                        }
                    }
                    parts += texts.map { it.trim() }
                }
            }
            Result(parts, language, detected)
        }
    }

    /**
     * Fortschritt der ganzen Erkennung aus Seite, Bereich und dem Fortschritt, den Tesseract meldet.
     * Er geht nie zurück und wird nur in ganzen Prozentschritten gemeldet, damit die Anzeige nicht
     * ständig neu gezeichnet wird, während der Prozessor ausgelastet ist.
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

        /** [percent] des gerade gelesenen Ausschnitts, wie Tesseract ihn meldet. */
        fun report(percent: Int) {
            val withinPage = (area + percent.coerceIn(0, 100) / 100f) / areaCount
            val total = ((page + withinPage) / pageCount * 100).toInt().coerceIn(0, 100)
            if (total <= reported) return
            reported = total
            onProgress(page, total)
        }
    }

    /**
     * Liest Ausschnitte mit einer einzigen Tesseract-Instanz je Erkennung; das Sprachpaket wird nur bei
     * einem Sprachwechsel neu geladen (#43).
     */
    private inner class Reader(private val run: Run, private val progress: Progress) : AutoCloseable {
        private var tess: TessBaseAPI? = null
        private var tessLanguage: String? = null

        suspend fun read(image: GrayImage, language: String): String {
            val tess = engine(language)
            val bands = PageLayout.analyze(image)
            tess.setImage(image.pixels, image.width, image.height, 1, image.width)
            if (bands == null) {
                tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                return recognize(tess)
            }
            // Tabelle mit Trennlinien: Abschnitte einzeln lesen, in Tabellenzeilen Zutat und Menge getrennt.
            val texts = bands.map { band ->
                when (band) {
                    is LayoutBand.Text -> readArea(tess, band.box, TessBaseAPI.PageSegMode.PSM_AUTO)
                    is LayoutBand.Row -> readArea(tess, band.left, TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
                }
            }
            // Mengen zuerst direkt auf der Seite lesen ...
            val amounts = bands.map { band ->
                val area = (band as? LayoutBand.Row)?.let { PageLayout.amountArea(image, it) } ?: return@map null
                readArea(tess, area, TessBaseAPI.PageSegMode.PSM_SINGLE_LINE)
            }.toMutableList()
            // ... und nur, wo das keine gültige Menge ergibt, noch einmal vergrößert.
            bands.forEachIndexed { index, band ->
                val right = (band as? LayoutBand.Row)?.right ?: return@forEachIndexed
                val direct = amounts[index].orEmpty()
                if (AmountText.isAmount(direct)) return@forEachIndexed
                val cell = PageLayout.cropScaled(image, right)
                tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE)
                tess.setImage(cell.pixels, cell.width, cell.height, 1, cell.width)
                val scaled = recognize(tess)
                if (AmountText.isAmount(scaled) || direct.isBlank()) amounts[index] = scaled
            }
            return bands.indices.joinToString("\n") { index ->
                when (bands[index]) {
                    is LayoutBand.Text -> texts[index]
                    is LayoutBand.Row -> RowText.compose(texts[index], amounts[index])
                }
            }
        }

        /** Die Instanz für [language]; bei einem Sprachwechsel wird sie mit dem anderen Sprachpaket neu gestartet. */
        private suspend fun engine(language: String): TessBaseAPI {
            tess?.takeIf { tessLanguage == language }?.let { return it }
            close()
            val code = TextLanguage.tesseractCode(language)
            ensureLanguageData(code)
            run.check()
            val api = TessBaseAPI(TessBaseAPI.ProgressNotifier { values -> progress.report(values.percent) })
            run.attach(api)
            tess = api
            tessLanguage = language
            if (!api.init(dataDir.absolutePath, code, TessBaseAPI.OEM_LSTM_ONLY)) throw IOException("Sprachpaket $code")
            // Sauvola: kommt mit Schatten und ungleichem Licht auf Handyfotos am besten zurecht.
            api.setVariable("thresholding_method", "2")
            api.setVariable("user_defined_dpi", "300")
            return api
        }

        /** Liest einen Ausschnitt der zuvor gesetzten Seite. */
        private fun readArea(tess: TessBaseAPI, box: Box, @TessBaseAPI.PageSegMode.Mode pageSegMode: Int): String {
            tess.setPageSegMode(pageSegMode)
            tess.setRectangle(box.left, box.top, box.width, box.height)
            return recognize(tess)
        }

        /**
         * Erkennt das gesetzte Bild bzw. den Ausschnitt. getHOCRText lässt sich abbrechen, meldet den Fortschritt
         * und liefert zu jedem Wort, wie sicher es gelesen wurde – damit fallen Reste von Symbolen, Knöpfen und Fotos weg.
         */
        private fun recognize(tess: TessBaseAPI): String {
            run.check()
            val hocr = tess.getHOCRText(0).orEmpty()
            run.check()
            return HocrText.compose(hocr).trim()
        }

        override fun close() {
            run.attach(null)
            tess?.recycle()
            tess = null
            tessLanguage = null
        }
    }

    /** Kopiert das Sprachpaket einmal je App-Version aus den mitgelieferten Dateien. */
    private suspend fun ensureLanguageData(code: String) = withContext(Dispatchers.IO) {
        // Bis 0.9.0 lagen die Sprachpakete in filesDir und gingen beim Umzug auf ein neues Handy mit (#45).
        File(context.filesDir, DATA_DIR).takeIf { it.exists() }?.deleteRecursively()
        val dir = File(dataDir, "tessdata").apply { mkdirs() }
        val target = File(dir, "$code.traineddata")
        val marker = File(dir, "$code.version")
        val version = BuildConfig.VERSION_CODE.toString()
        if (target.isFile && marker.isFile && marker.readText() == version) return@withContext
        val temp = File(dir, "$code.traineddata.tmp")
        context.assets.open("tessdata/$code.traineddata").use { input ->
            temp.outputStream().use { input.copyTo(it) }
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Sprachpaket $code nicht speicherbar")
        }
        marker.writeText(version)
    }

    private companion object {
        const val DATA_DIR = "tesseract"
    }
}
