package com.moltobene.app.data.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

/**
 * Texterkennung mit PP-OCRv6 (PaddleOCR) über ONNX Runtime, nur auf dem Handy: ein kleines Modell sucht die
 * Textzeilen, ein größeres liest jede Zeile. Zu jeder Zeile gibt es ihre Lage auf der Seite, daraus entsteht in
 * [ReadingOrder] die Lesereihenfolge. Ohne Android-Abhängigkeiten, damit es auch in Unit-Tests läuft.
 *
 * @param detectionModel Modell für die Zeilensuche (ONNX)
 * @param recognitionModel Modell zum Lesen einer Zeile (ONNX)
 * @param dictionary Zeichen des Lesemodells, ein Zeichen je Eintrag
 */
class PaddleOcr(
    detectionModel: ByteArray,
    recognitionModel: ByteArray,
    dictionary: List<String>,
    threads: Int = DEFAULT_THREADS,
) : AutoCloseable {

    private val environment = OrtEnvironment.getEnvironment()
    private val keys = CtcDecoder.keys(dictionary)
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(threads)
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
    }
    private val detection = environment.createSession(detectionModel, options)
    private val recognition = try {
        environment.createSession(recognitionModel, options)
    } catch (e: Exception) {
        detection.close()
        throw e
    }

    /**
     * Findet die Textzeilen einer Seite.
     * @param maxSide längste Seite des Bildes für die Suche; größere Bilder werden dafür verkleinert
     * @return Zeilen in Koordinaten von [image]
     */
    fun detect(image: GrayImage, run: OrtSession.RunOptions, maxSide: Int = DETECTION_MAX_SIDE): List<TextBox> {
        val input = OcrInput.forDetection(image, maxSide)
        val probability = infer(detection, input, run)
        val scaleX = image.width.toFloat() / input.width
        val scaleY = image.height.toFloat() / input.height
        return TextDetection.boxes(probability, input.width, input.height)
            .map { it.scaled(scaleX, scaleY) }
            .filter { it.height >= MIN_LINE_HEIGHT }
    }

    /** Liest eine gefundene Zeile. */
    fun read(image: GrayImage, box: TextBox, run: OrtSession.RunOptions): CtcDecoder.Decoded {
        val input = OcrInput.forLine(image, box)
        OnnxTensor.createTensor(environment, input.values, longArrayOf(1, 3, input.height.toLong(), input.width.toLong())).use { tensor ->
            recognition.run(mapOf(recognition.inputNames.first() to tensor), run).use { result ->
                val output = result.get(0) as OnnxTensor
                val shape = output.info.shape
                return CtcDecoder.decode(output.floatBuffer, shape[1].toInt(), shape[2].toInt(), keys)
            }
        }
    }

    /** Erkennt eine Seite: Zeilen suchen, lesen und in Lesereihenfolge bringen. [onLine] meldet gelesene Zeilen. */
    fun recognize(image: GrayImage, run: OrtSession.RunOptions, onLine: (done: Int, total: Int) -> Unit = { _, _ -> }): List<TextLine> {
        val boxes = detect(image, run)
        return boxes.mapIndexedNotNull { index, box ->
            val decoded = read(image, box, run)
            onLine(index + 1, boxes.size)
            decoded.takeIf { it.confidence >= MIN_CONFIDENCE && it.text.isNotBlank() }?.let { TextLine(box, it.text) }
        }
    }

    private fun infer(session: OrtSession, input: OcrInput.Scaled, run: OrtSession.RunOptions): FloatArray {
        val shape = longArrayOf(1, 3, input.height.toLong(), input.width.toLong())
        OnnxTensor.createTensor(environment, input.values, shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor), run).use { result ->
                val buffer = (result.get(0) as OnnxTensor).floatBuffer
                return FloatArray(buffer.remaining()).also { buffer.get(it) }
            }
        }
    }

    override fun close() {
        recognition.close()
        detection.close()
        options.close()
    }

    companion object {
        /** Mitgelieferte Modelle in assets: PP-OCRv6, Zeilensuche „tiny“ und Lesen „small“ (Prüfsummen in ModelDataTest). */
        const val MODEL_DIR = "ocr"
        const val DETECTION_MODEL = "det.onnx"
        const val RECOGNITION_MODEL = "rec.onnx"
        const val DICTIONARY = "keys.txt"

        /** Längste Seite für die Zeilensuche; in Tests mit nachgestellten Handyfotos der beste Kompromiss. */
        const val DETECTION_MAX_SIDE = 1600

        /** Unsicherer gelesene Zeilen sind meist Reste von Fotos, Symbolen oder Knöpfen. */
        const val MIN_CONFIDENCE = 0.5f

        /** Niedrigere Zeilen (in Bildpunkten) sind Striche oder Krümel, kein Text. */
        private const val MIN_LINE_HEIGHT = 6f

        private val DEFAULT_THREADS = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
    }
}
