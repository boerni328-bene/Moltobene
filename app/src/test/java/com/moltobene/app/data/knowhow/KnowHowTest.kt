package com.moltobene.app.data.knowhow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Teigwissen (#84): Aufbau der mitgelieferten Artikel und die Regeln für Texte (beide Sprachen, keine Anrede). */
class KnowHowTest {

    private val articles = KnowHow.parse(File("src/main/assets/${KnowHow.ASSET}").readText())

    private fun joined(text: KnowHow.Text): String =
        (listOf(text.title, text.summary) + text.sections.flatMap { s ->
            listOf(s.heading) + s.paragraphs + s.rows.flatMap { listOf(it.label, it.text) }
        }).joinToString("\n")

    @Test
    fun jederArtikelHatBeideSprachenMitGleichemAufbau() {
        assertEquals(articles.map { it.id }.toSet().size, articles.size)
        assertEquals(listOf("flour_types", "grains", "gluten_strength"), articles.map { it.id })
        for (article in articles) {
            val de = article.texts.getValue("de")
            val en = article.texts.getValue("en")
            assertEquals(setOf("de", "en"), article.texts.keys)
            assertEquals("Abschnitte ${article.id}", de.sections.size, en.sections.size)
            de.sections.zip(en.sections).forEach { (a, b) ->
                assertEquals("Absätze ${article.id}/${a.heading}", a.paragraphs.size, b.paragraphs.size)
                assertEquals("Zeilen ${article.id}/${a.heading}", a.rows.size, b.rows.size)
            }
            for (text in listOf(de, en)) {
                assertTrue(text.title.isNotBlank() && text.summary.isNotBlank())
                for (section in text.sections) {
                    assertTrue(section.heading.isNotBlank())
                    assertTrue("Abschnitt ohne Inhalt: ${section.heading}", section.paragraphs.isNotEmpty() || section.rows.isNotEmpty())
                    (section.paragraphs + section.rows.map { it.text } + section.rows.map { it.label }).forEach { assertTrue(it.isNotBlank()) }
                }
            }
        }
    }

    @Test
    fun keineDirekteAnredeKeineGleichsetzungKeineHeilversprechen() {
        val addressDe = Regex("""\b(du|dich|dein\w*|ihr|euch|sie\s+(sollten|können|müssen))\b""", RegexOption.IGNORE_CASE)
        val addressEn = Regex("""\b(you|your)\b""", RegexOption.IGNORE_CASE)
        val equalDe = Regex("""entspricht|ersetzt|ist dasselbe wie""", RegexOption.IGNORE_CASE)
        val equalEn = Regex("""corresponds|equivalent|is the same as""", RegexOption.IGNORE_CASE)
        val healthDe = Regex("""gesund(?!heitsberatung)|heilt|heilsam|verträglich""", RegexOption.IGNORE_CASE)
        val healthEn = Regex("""healthy|\bcures?\b|healing""", RegexOption.IGNORE_CASE)
        for (article in articles) {
            val de = joined(article.texts.getValue("de"))
            val en = joined(article.texts.getValue("en"))
            assertFalse("Anrede ${article.id}", addressDe.containsMatchIn(de))
            assertFalse("Anrede ${article.id} (en)", addressEn.containsMatchIn(en))
            assertFalse("Gleichsetzung ${article.id}: ${equalDe.find(de)?.value}", equalDe.containsMatchIn(de))
            assertFalse("Gleichsetzung ${article.id} (en): ${equalEn.find(en)?.value}", equalEn.containsMatchIn(en))
            assertFalse("Heilversprechen ${article.id}", healthDe.containsMatchIn(de))
            assertFalse("Heilversprechen ${article.id} (en)", healthEn.containsMatchIn(en))
        }
    }

    @Test
    fun mehltypenArtikelNenntAlleLaenderUndErklaertDasW() {
        val article = articles.first { it.id == "flour_types" }
        val de = article.texts.getValue("de")
        val en = article.texts.getValue("en")
        for (country in listOf("Deutschland", "Österreich", "Schweiz", "Italien")) {
            assertTrue(country, de.sections.any { it.heading == country })
        }
        for (country in listOf("Germany", "Austria", "Switzerland", "Italy")) {
            assertTrue(country, en.sections.any { it.heading == country })
        }
        val textDe = joined(de)
        val textEn = joined(en)
        assertTrue(textDe.contains("Backkraft") && textDe.contains("Weizen"))
        assertTrue(textEn.contains("baking strength") && textEn.contains("wheat"))
        // Italien: gesetzliche Werte stehen vollständig da.
        for (value in listOf("0,55", "0,65", "0,80", "0,95")) assertTrue(value, textDe.contains(value))
        for (value in listOf("0.55", "0.65", "0.80", "0.95")) assertTrue(value, textEn.contains(value))
        // Je Land mindestens vier Einträge.
        for (language in listOf(de, en)) {
            language.sections.filter { it.rows.isNotEmpty() }.forEach { assertTrue(it.heading, it.rows.size >= 4) }
        }
    }

    @Test
    fun nahrwerteSindRichtwerteUndGlutenIstGenannt() {
        val grains = articles.first { it.id == "grains" }
        val de = grains.texts.getValue("de")
        val en = grains.texts.getValue("en")
        assertTrue(de.sections.any { it.heading.contains("Richtwert") })
        assertTrue(en.sections.any { it.heading.contains("guide") })
        assertTrue(joined(de).contains("keine Ernährungsberatung"))
        assertTrue(joined(en).contains("not dietary advice"))
        for (article in articles) {
            assertTrue(joined(article.texts.getValue("de")).contains("Gluten"))
            assertTrue(joined(article.texts.getValue("en")).contains("luten"))
        }
        // Nährwerte nur als Spannen oder mit „etwa“.
        val nutrition = de.sections.first { it.heading.contains("Richtwert") }.rows
        assertTrue(nutrition.all { it.text.startsWith("etwa") })
    }

    @Test
    fun spracheDerOberflaecheBestimmtDenText() {
        val article = articles.first()
        assertEquals(article.texts.getValue("de"), KnowHow.text(article, "de"))
        assertEquals(article.texts.getValue("en"), KnowHow.text(article, "fr"))
        assertEquals("en", KnowHow.languageFor("en-US"))
    }
}
