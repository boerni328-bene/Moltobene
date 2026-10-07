package com.moltobene.app.data.translate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

/**
 * Das Sprachpaket Deutsch ↔ Englisch für „Rezept übersetzen“ (#60): zwei Modelle OPUS-MT der Universität Helsinki
 * (CC-BY 4.0), auf 8 Bit verkleinert, mit ihren Wortteilen als Text. Gebaut wird es nachvollziehbar in
 * `.github/workflows/sprachpaket.yml`; die App lädt es nur auf Wunsch einmal herunter.
 *
 * Aufbau (je Richtung ein Verzeichnis, z. B. `en-de/`): [ENCODER], [DECODER], [SOURCE_PIECES], [VOCABULARY],
 * [CONFIG] und die Herkunft `NOTICE.txt`; dazu [MANIFEST] mit Größe und Prüfsumme jeder Datei.
 */
object LanguagePack {
    const val MANIFEST = "sprachpaket.json"
    const val ENCODER = "encoder.onnx"
    const val DECODER = "decoder.onnx"
    const val SOURCE_PIECES = "source.tsv"
    const val VOCABULARY = "vocab.tsv"
    const val CONFIG = "config.json"

    /** Version des Pakets: Ein geändertes Paket bekommt eine neue Version, ein neues Release und eine neue Prüfsumme. */
    const val VERSION = 1

    /** Eigenes Release im Repository, gebaut von `.github/workflows/sprachpaket.yml`. */
    const val URL = "https://github.com/boerni328-bene/Moltobene/releases/download/sprachpaket-de-en-$VERSION/sprachpaket-de-en-$VERSION.zip"

    /** Feste Prüfsumme (SHA-256) von [MANIFEST]; das Verzeichnis enthält die Prüfsummen aller anderen Dateien. */
    const val MANIFEST_SHA256 = "e55995bdcaf4d547dcec96a0dfe2d6c7db8545d55ed73a2d934741226c551a9e"

    /** Ungefähre Größen für die Anzeige vor dem Download. */
    const val DOWNLOAD_BYTES = 172_000_000L
    const val INSTALLED_BYTES = 274_000_000L

    /** Mehr wird nie geladen. */
    const val MAX_DOWNLOAD_BYTES = 300L * 1024 * 1024

    /** Sprachen, zwischen denen übersetzt werden kann: Ausgangssprache → Zielsprache → Verzeichnis. */
    private val DIRECTIONS = mapOf("en" to mapOf("de" to "en-de"), "de" to mapOf("en" to "de-en"))

    /** Verzeichnis für die Richtung, z. B. „en-de“; null, wenn das Paket diese Richtung nicht kennt. */
    fun direction(from: String?, to: String): String? = DIRECTIONS[from]?.get(to)

    /** Sprachen, in die ein Rezept in [from] übersetzt werden kann. */
    fun targets(from: String?): Set<String> = DIRECTIONS[from]?.keys.orEmpty()

    fun readConfig(file: File): MarianTranslator.Config {
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        fun value(name: String) = json.getValue(name).jsonPrimitive.int
        return MarianTranslator.Config(
            eos = value("eos"),
            pad = value("pad"),
            start = value("start"),
            unk = value("unk"),
            maxLength = value("maxLength"),
        )
    }

    /** Dateien laut [MANIFEST] mit ihrer Größe in Bytes. */
    fun files(directory: File): Map<String, Long> {
        val json = Json.parseToJsonElement(File(directory, MANIFEST).readText()).jsonObject
        return json.getValue("files").jsonObject.mapValues { (_, entry) -> entry.jsonObject.getValue("size").jsonPrimitive.long }
    }

    /** Ist das Paket vollständig vorhanden? Prüft Vorhandensein und Größe; die Prüfsummen prüft das Entpacken. */
    fun isComplete(directory: File): Boolean = runCatching {
        val files = files(directory)
        files.isNotEmpty() && files.all { (name, size) -> File(directory, name).let { it.isFile && it.length() == size } }
    }.getOrDefault(false)
}
