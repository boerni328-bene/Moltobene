package com.moltobene.app.data.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenRecipeTest {

    @Test
    fun `je Aeusserung eine Zeile mit Ziffern und grossem Anfang`() {
        val text = SpokenRecipe.text(
            listOf("zutaten", "zweihundertfünfzig gramm mehl", "  ", "ein halber liter milch", "zubereitung", "alles verrühren"),
        )
        // Nur der erste Buchstabe jeder Zeile wird groß, der Rest bleibt, wie die Erkennung ihn liefert.
        assertEquals("Zutaten\n250 gramm mehl\n½ liter milch\nZubereitung\nAlles verrühren", text)
    }

    @Test
    fun `ohne Aeusserungen entsteht kein Text`() {
        assertEquals("", SpokenRecipe.text(emptyList()))
        assertEquals("", SpokenRecipe.text(listOf("", "   ")))
    }
}
