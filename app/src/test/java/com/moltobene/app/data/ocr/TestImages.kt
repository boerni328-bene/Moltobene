package com.moltobene.app.data.ocr

import java.io.File
import java.util.zip.GZIPInputStream

/** Prüfbilder und Modelle für die Tests der Texterkennung. */
internal object TestImages {

    /** Liest ein Graustufenbild im einfachen Format PGM (P5), gzip-gepackt, aus test/resources/ocr. */
    fun loadGray(name: String): GrayImage {
        val bytes = requireNotNull(javaClass.classLoader?.getResource("ocr/$name")) { "Prüfbild $name fehlt" }
            .openStream().use { GZIPInputStream(it).readBytes() }
        // Kopf: „P5“, Breite, Höhe, Höchstwert – durch Leerraum getrennt, danach ein Byte je Bildpunkt.
        var position = 0
        val fields = mutableListOf<String>()
        while (fields.size < 4) {
            while (bytes[position].toInt().toChar().isWhitespace()) position++
            val start = position
            while (!bytes[position].toInt().toChar().isWhitespace()) position++
            fields += String(bytes, start, position - start)
        }
        position++
        val width = fields[1].toInt()
        val height = fields[2].toInt()
        return GrayImage(width, height, bytes.copyOfRange(position, position + width * height))
    }

    /** Die mitgelieferten Modelle aus src/main/assets/ocr (Unit-Tests laufen im Ordner des Moduls). */
    fun model(name: String): File = File("src/main/assets/${PaddleOcr.MODEL_DIR}", name)

    fun openOcr(): PaddleOcr = PaddleOcr(
        detectionModel = model(PaddleOcr.DETECTION_MODEL).readBytes(),
        recognitionModel = model(PaddleOcr.RECOGNITION_MODEL).readBytes(),
        dictionary = model(PaddleOcr.DICTIONARY).readLines(Charsets.UTF_8),
    )
}
