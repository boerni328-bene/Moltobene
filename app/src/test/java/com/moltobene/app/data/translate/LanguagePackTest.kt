package com.moltobene.app.data.translate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Der Übersetzer der App mit dem echten Sprachpaket (#60): liefert er Wort für Wort dasselbe wie das Original?
 * Läuft nur in `.github/workflows/sprachpaket.yml`, das Paket und Vergleichswerte (`reference.py`) baut und über
 * `LANGUAGE_PACK_DIR` und `LANGUAGE_PACK_FIXTURES` übergibt – das Paket ist zu groß für das Repository.
 */
class LanguagePackTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val pack = System.getenv("LANGUAGE_PACK_DIR")?.let(::File)
    private val fixtures = System.getenv("LANGUAGE_PACK_FIXTURES")?.let(::File)
    private val archive = System.getenv("LANGUAGE_PACK_ARCHIVE")?.let(::File)

    private class Fixture(val line: String, val inputIds: List<Int>, val outputIds: List<Int>, val text: String)

    private fun fixtures(): Map<String, List<Fixture>> {
        assumeTrue("Kein Sprachpaket angegeben", pack != null && fixtures != null)
        val json = Json.parseToJsonElement(requireNotNull(fixtures).readText()).jsonObject
        return json.mapValues { (_, rows) ->
            rows.jsonArray.map { row ->
                val entry = row.jsonObject
                fun ids(name: String) = entry.getValue(name).jsonArray.map { it.jsonPrimitive.int }
                Fixture(
                    line = entry.getValue("line").jsonPrimitive.content,
                    inputIds = ids("input_ids"),
                    outputIds = ids("output_ids"),
                    text = entry.getValue("text").jsonPrimitive.content,
                )
            }
        }
    }

    @Test
    fun paketIstVollstaendig() {
        fixtures()
        assertTrue(LanguagePack.isComplete(requireNotNull(pack)))
        assertEquals(setOf("de"), LanguagePack.targets("en"))
        assertEquals(setOf("en"), LanguagePack.targets("de"))
    }

    /** Die App nimmt nur dieses Paket an: Prüfsumme in [LanguagePack.MANIFEST_SHA256] und Entpacken wie auf dem Handy. */
    @Test
    fun appNimmtDasPaketAn() {
        assumeTrue("Kein Sprachpaket angegeben", pack != null && archive != null)
        val manifest = File(requireNotNull(pack), LanguagePack.MANIFEST).readBytes()
        assertEquals(LanguagePack.MANIFEST_SHA256, LanguagePackInstaller.sha256(manifest))
        val target = File(folder.root, "sprachpaket")
        requireNotNull(archive).inputStream().buffered().use { LanguagePackInstaller.unpack(it, target, LanguagePack.MANIFEST_SHA256) }
        assertTrue(LanguagePack.isComplete(target))
        assertTrue(archive.length() <= LanguagePack.MAX_DOWNLOAD_BYTES)
        assertTrue(LanguagePackManager.sizeOf(target) <= LanguagePackInstaller.MAX_UNPACKED_BYTES)
    }

    @Test
    fun gleichesErgebnisWieDasOriginal() {
        val differences = mutableListOf<String>()
        fixtures().forEach { (direction, rows) ->
            MarianTranslator(File(requireNotNull(pack), direction), threads = 1).use { translator ->
                rows.forEach { row ->
                    val input = translator.encode(row.line)
                    if (input != row.inputIds) differences += "$direction Wortteile: ${row.line} → $input statt ${row.inputIds}"
                    val output = translator.generate(row.inputIds)
                    if (output != row.outputIds) differences += "$direction Übersetzung: ${row.line} → $output statt ${row.outputIds}"
                    val text = translator.decode(row.outputIds)
                    if (text != row.text) differences += "$direction Text: ${row.line} → „$text“ statt „${row.text}“"
                    // So, wie die App es zeigt: mit Küchenwörterbuch und Schutzregeln.
                    val shown = TranslationGuard.choose(row.line, translator.translate(KitchenGlossary.apply(row.line, direction)))
                    println("$direction: ${row.line}  →  $shown")
                }
            }
        }
        assertTrue(differences.joinToString("\n"), differences.isEmpty())
    }
}
