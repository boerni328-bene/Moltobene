package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** „Beim Öffnen der App“ (#81): feste Kennungen, Standard „Startseite“. */
class StartScreenTest {

    @Test
    fun kennungen() {
        assertEquals(StartScreen.HOME, StartScreen.fromKey(null))
        assertEquals(StartScreen.HOME, StartScreen.fromKey("unbekannt"))
        assertEquals(StartScreen.HOME, StartScreen.fromKey("home"))
        assertEquals(StartScreen.COLLECTION, StartScreen.fromKey("collection"))
        assertEquals(StartScreen.entries.size, StartScreen.entries.map { it.key }.toSet().size)
    }
}
