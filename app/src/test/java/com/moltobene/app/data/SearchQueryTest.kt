package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {

    @Test
    fun jedesWortWirdAlsWortanfangGesucht() {
        assertEquals("kart* salat*", SearchQuery.toFtsMatch("Kart Salat"))
    }

    @Test
    fun sonderzeichenUndBefehleDerSuchspracheWerdenEntschaerft() {
        // „OR“ wäre sonst ein Befehl der Datenbanksuche; klein geschrieben ist es ein normales Wort.
        assertEquals("käse* or* nudeln* not*", SearchQuery.toFtsMatch("\"Käse\" OR* (Nudeln)- NOT"))
    }

    @Test
    fun leereEingabeErgibtKeineSuche() {
        assertNull(SearchQuery.toFtsMatch("  *\"()  "))
    }
}
