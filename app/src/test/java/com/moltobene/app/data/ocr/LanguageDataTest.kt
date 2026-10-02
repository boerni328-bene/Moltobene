package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Die mitgelieferten Sprachpakete müssen genau den Originalen entsprechen (#41):
 * tesseract-ocr/tessdata_fast, Stand 87416418657359cb625c412a48b6e1d6d41c29bd (Lizenz Apache 2.0).
 * Wird ein Sprachpaket bewusst ersetzt, wird hier die neue Prüfsumme eingetragen – vorher mit dem
 * Original auf GitHub vergleichen.
 */
class LanguageDataTest {

    private val expected = mapOf(
        "deu" to "19d219bbb6672c869d20a9636c6816a81eb9a71796cb93ebe0cb1530e2cdb22d",
        "eng" to "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2",
        "fra" to "ced037562e8c80c13122dece28dd477d399af80911a28791a66a63ac1e3445ca",
        "ita" to "b8f89e1e785118dac4d51ae042c029a64edb5c3ee42ef73027a6d412748d8827",
        "spa" to "6f2e04d02774a18f01bed44b1111f2cd7f3ba7ac9dc4373cd3f898a40ea6b464",
    )

    @Test
    fun sprachpaketeEntsprechenDenOriginalen() {
        val dir = File("src/main/assets/tessdata")
        val present = dir.listFiles { file -> file.name.endsWith(".traineddata") }.orEmpty().associate { file ->
            file.name.removeSuffix(".traineddata") to sha256(file)
        }
        // Jede Sprache der Texterkennung hat genau ihr geprüftes Sprachpaket – nicht mehr und nicht weniger.
        assertEquals(TextLanguage.SUPPORTED.map { TextLanguage.tesseractCode(it) }.toSet(), expected.keys)
        assertEquals(expected, present)
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
