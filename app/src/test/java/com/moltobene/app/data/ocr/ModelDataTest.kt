package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Die mitgelieferten Modelle der Texterkennung müssen genau den geprüften Dateien entsprechen (#41):
 * PP-OCRv6 von PaddleOCR (Lizenz Apache 2.0) in der Umwandlung nach ONNX aus dem Paket „onnxocr“ 4.0.0
 * (PyPI, Lizenz Apache 2.0): Zeilensuche „tiny“ (models/ppocrv6/tiny/det/det.onnx), Lesen „small“
 * (models/ppocrv6/small/rec/rec.onnx) und dessen Zeichenliste (models/ppocrv6/ppocrv6_dict.txt).
 * Wird ein Modell bewusst ersetzt, wird hier die neue Prüfsumme eingetragen – vorher mit dem Original vergleichen.
 */
class ModelDataTest {

    private val expected = mapOf(
        PaddleOcr.DETECTION_MODEL to "193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8",
        PaddleOcr.RECOGNITION_MODEL to "5435fd747c9e0efe15a96d0b378d5bd157e9492ed8fd80edf08f30d02fa24634",
        PaddleOcr.DICTIONARY to "769e7fa79bb297b5f18d8dbd149e364a45bc61f2b3f574e5ea836f0b261c23a6",
    )

    @Test
    fun modelleEntsprechenDenGeprueftenDateien() {
        val present = File("src/main/assets/${PaddleOcr.MODEL_DIR}").listFiles().orEmpty()
            .associate { it.name to sha256(it) }
        // Genau diese drei Dateien – nicht mehr und nicht weniger.
        assertEquals(expected, present)
    }

    @Test
    fun zeichenlistePasstZumLesemodell() {
        // Das Lesemodell kennt 18 710 Zeichen: „kein Zeichen“, die 18 708 der Liste und das Leerzeichen.
        val keys = CtcDecoder.keys(File("src/main/assets/${PaddleOcr.MODEL_DIR}/${PaddleOcr.DICTIONARY}").readLines())
        assertEquals(18_710, keys.size)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
