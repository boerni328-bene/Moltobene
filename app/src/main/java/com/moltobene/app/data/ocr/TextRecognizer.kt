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

/** Graustufenbild für die Texterkennung: ein Byte je Bildpunkt, Zeile für Zeile. */
class GrayImage(val width: Int, val height: Int, val pixels: ByteArray)

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
        val tess = TessBaseAPI()
        synchronized(lock) { active = tess }
        try {
            if (!tess.init(dataDir.absolutePath, code, TessBaseAPI.OEM_LSTM_ONLY)) throw IOException("Sprachpaket $code")
            tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            // Sauvola: kommt mit Schatten und ungleichem Licht auf Handyfotos am besten zurecht.
            tess.setVariable("thresholding_method", "2")
            tess.setVariable("user_defined_dpi", "300")
            tess.setImage(image.pixels, image.width, image.height, 1, image.width)
            // getHOCRText erkennt mit Abbruchmöglichkeit; getUTF8Text liefert danach den fertigen Text.
            tess.getHOCRText(0)
            if (cancelRequested) throw CancelledException()
            return tess.getUTF8Text().orEmpty()
        } finally {
            synchronized(lock) { active = null }
            tess.recycle()
        }
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
