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
 * Die erste Seite wird mit der zuletzt erkannten Sprache gelesen; passt die Sprache des Textes nicht dazu,
 * wird sie mit dem richtigen Sprachpaket noch einmal gelesen (sonst gehen z. B. Akzente verloren).
 */
class TextRecognizer(private val context: Context) {

    class Result(val text: String, val language: String)

    /** Die Erkennung wurde über [cancel] abgebrochen. */
    class CancelledException : Exception()

    private val mutex = Mutex()
    private val lock = Any()
    private var active: TessBaseAPI? = null

    @Volatile
    private var cancelRequested = false

    private val dataDir: File get() = File(context.filesDir, DATA_DIR)

    /**
     * @param pages lädt die Seiten nacheinander, damit nie mehrere Fotos zugleich im Speicher liegen
     * @param onPage wird vor jeder Seite mit ihrer Nummer (ab 0) aufgerufen
     */
    suspend fun recognize(
        pages: List<suspend () -> GrayImage>,
        preferredLanguage: String,
        onPage: (Int) -> Unit,
    ): Result = mutex.withLock {
        cancelRequested = false
        withContext(Dispatchers.Default) {
            var language = TextLanguage.supportedOrDefault(preferredLanguage)
            val texts = mutableListOf<String>()
            pages.forEachIndexed { index, load ->
                onPage(index)
                val image = load()
                var text = read(image, language)
                if (index == 0) {
                    val detected = TextLanguage.detect(text)
                    if (detected != null && detected != language) {
                        language = detected
                        text = read(image, language)
                    }
                }
                text.trim().takeIf { it.isNotEmpty() }?.let { texts += it }
            }
            Result(texts.joinToString("\n\n"), language)
        }
    }

    /** Bricht eine laufende Erkennung ab. Darf von jedem Thread aus aufgerufen werden. */
    fun cancel() {
        cancelRequested = true
        synchronized(lock) { active?.stop() }
    }

    private suspend fun read(image: GrayImage, language: String): String {
        val code = TextLanguage.tesseractCode(language)
        ensureLanguageData(code)
        if (cancelRequested) throw CancelledException()
        val bands = PageLayout.analyze(image)
        val tess = TessBaseAPI()
        synchronized(lock) { active = tess }
        try {
            if (!tess.init(dataDir.absolutePath, code, TessBaseAPI.OEM_LSTM_ONLY)) throw IOException("Sprachpaket $code")
            // Sauvola: kommt mit Schatten und ungleichem Licht auf Handyfotos am besten zurecht.
            tess.setVariable("thresholding_method", "2")
            tess.setVariable("user_defined_dpi", "300")
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
        } finally {
            synchronized(lock) { active = null }
            tess.recycle()
        }
    }

    /** Liest einen Ausschnitt der zuvor gesetzten Seite. */
    private fun readArea(tess: TessBaseAPI, box: Box, @TessBaseAPI.PageSegMode.Mode pageSegMode: Int): String {
        tess.setPageSegMode(pageSegMode)
        tess.setRectangle(box.left, box.top, box.width, box.height)
        return recognize(tess)
    }

    /**
     * Erkennt das gesetzte Bild bzw. den Ausschnitt. getHOCRText lässt sich abbrechen und liefert zu jedem
     * Wort, wie sicher es gelesen wurde – damit fallen Reste von Symbolen, Knöpfen und Fotos weg.
     */
    private fun recognize(tess: TessBaseAPI): String {
        val hocr = tess.getHOCRText(0).orEmpty()
        if (cancelRequested) throw CancelledException()
        return HocrText.compose(hocr).trim()
    }

    /** Kopiert das Sprachpaket einmal je App-Version aus den mitgelieferten Dateien. */
    private suspend fun ensureLanguageData(code: String) = withContext(Dispatchers.IO) {
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
