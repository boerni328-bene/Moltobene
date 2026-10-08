package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Darstellung aus den Einstellungen: feste Kennungen, unbekannte Werte werden zum Standard. */
class AppearanceTest {

    @Test
    fun kennungenHinUndZurueck() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromKey(it.key)) }
        Palette.entries.forEach { assertEquals(it, Palette.fromKey(it.key)) }
        // Gespeichert wird englisch und fest – diese Werte dürfen sich nie ändern.
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.key })
        assertEquals(listOf("slate", "cobalt", "sage", "terracotta", "saffron"), Palette.entries.map { it.key })
    }

    @Test
    fun unbekanntWirdStandard() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("sepia"))
        assertEquals(Palette.SLATE, Palette.fromKey(null))
        assertEquals(Palette.SLATE, Palette.fromKey("aubergine"))
        assertEquals(Appearance(ThemeMode.SYSTEM, Palette.SLATE), Appearance())
    }

    @Test
    fun hellOderDunkel() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }
}
