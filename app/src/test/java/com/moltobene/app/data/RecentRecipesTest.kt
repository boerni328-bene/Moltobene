package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentRecipesTest {

    private fun id(n: Int) = "00000000-0000-0000-0000-%012d".format(n)

    @Test
    fun neuestesKommtNachVorn() {
        assertEquals(listOf(id(2), id(1)), RecentRecipes.add(listOf(id(1)), id(2)))
    }

    @Test
    fun dasselbeRezeptStehtNurEinmal() {
        assertEquals(listOf(id(1), id(2), id(3)), RecentRecipes.add(listOf(id(2), id(1), id(3)), id(1)))
    }

    @Test
    fun hoechstensFuenf() {
        val ids = (1..5).map { id(it) }
        val result = RecentRecipes.add(ids, id(6))
        assertEquals(RecentRecipes.MAX, result.size)
        assertEquals(id(6), result.first())
        assertEquals(id(4), result.last())
    }

    @Test
    fun ungueltigeKennungenWerdenIgnoriert() {
        assertEquals(listOf(id(1)), RecentRecipes.add(listOf(id(1)), "kein-rezept"))
        assertEquals(listOf(id(1), id(2)), RecentRecipes.decode("${id(1)},x,,${id(2)},${id(1)}"))
        assertEquals(emptyList<String>(), RecentRecipes.decode(null))
    }

    @Test
    fun speichernUndLesen() {
        val ids = listOf(id(3), id(1))
        assertEquals(ids, RecentRecipes.decode(RecentRecipes.encode(ids)))
    }
}
