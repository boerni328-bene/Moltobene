package com.moltobene.app.data.dough

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hinweise „Was bewirkt das?“ im Teigrechner (Teigwissen, #84): jeder Text gibt es in beiden Sprachen, ohne direkte
 * Anrede, mit gleichen Platzhaltern und in überschaubarer Länge.
 */
class DoughHelpTextsTest {

    private fun strings(path: String): Map<String, String> {
        val text = File(path).readText()
        val entry = Regex("""<string name="(dough_help_[a-z_]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return entry.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val english = strings("src/main/res/values/strings.xml")
    private val german = strings("src/main/res/values-de/strings.xml")

    @Test
    fun beideSprachenHabenDieselbenTexte() {
        assertTrue("Es gibt Hinweise", english.size >= 20)
        assertEquals(english.keys, german.keys)
    }

    @Test
    fun jedesThemaHatWirkungUeblichUndGutZuWissen() {
        for (topic in listOf("water", "salt", "oil", "yeast", "preferment")) {
            for (part in listOf("", "_effect", "_range", "_care")) {
                val key = "dough_help_$topic$part"
                assertTrue("$key englisch", english[key].orEmpty().isNotBlank())
                assertTrue("$key deutsch", german[key].orEmpty().isNotBlank())
            }
        }
    }

    @Test
    fun keineDirekteAnrede() {
        val germanAddress = Regex("""\b(du|dir|dich|dein\w*|Sie|Ihnen|Ihr\w*)\b""")
        val englishAddress = Regex("""\b(you|your|yours)\b""", RegexOption.IGNORE_CASE)
        for ((key, text) in german) assertFalse("$key: $text", germanAddress.containsMatchIn(text))
        for ((key, text) in english) assertFalse("$key: $text", englishAddress.containsMatchIn(text))
    }

    @Test
    fun platzhalterStimmenUeberein() {
        val placeholder = Regex("""%\d\$[sd]""")
        for (key in english.keys) {
            assertEquals(key, placeholder.findAll(english.getValue(key)).map { it.value }.toList(),
                placeholder.findAll(german.getValue(key)).map { it.value }.toList())
        }
    }

    @Test
    fun hinweiseSindKurzGenug() {
        for ((key, text) in german + english) {
            if (Regex("dough_help_[a-z]+_(effect|range|care)").matches(key)) {
                assertTrue("$key hat ${text.length} Zeichen", text.length in 20..420)
            }
        }
    }

    @Test
    fun keineVerbotenenBegriffe() {
        // Feste Begriffe: nicht „Gare“/„Stockgare“, nicht „Hydration“, nicht „Bäckerprozente“.
        val forbidden = Regex("""\b(Gare|Stockgare|Hydration|Bäckerprozente|Stückgare)\b""")
        for ((key, text) in german) assertFalse("$key: $text", forbidden.containsMatchIn(text))
    }
}
