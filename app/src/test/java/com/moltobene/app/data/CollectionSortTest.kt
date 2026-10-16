package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CollectionSortTest {

    private data class Item(val title: String, val createdAt: Long = 0L)

    private fun order(items: List<Item>, sort: CollectionSort, locale: Locale = Locale.GERMAN) =
        CollectionSort.order(items, sort, locale, { it.title }, { it.createdAt }).map { it.title }

    @Test
    fun titleOrdersUmlautsAtTheirLetter() {
        val items = listOf(Item("Zwiebelkuchen"), Item("Äpfel im Schlafrock"), Item("Brot"), Item("Aubergine"))
        assertEquals(listOf("Äpfel im Schlafrock", "Aubergine", "Brot", "Zwiebelkuchen"), order(items, CollectionSort.TITLE))
    }

    @Test
    fun titleIgnoresCase() {
        val items = listOf(Item("banane"), Item("Apfel"), Item("Cassis"))
        assertEquals(listOf("Apfel", "banane", "Cassis"), order(items, CollectionSort.TITLE))
    }

    @Test
    fun titleKeepsOrderOfEqualTitles() {
        val items = listOf(Item("Suppe", 2), Item("Suppe", 1))
        assertEquals(listOf(2L, 1L), CollectionSort.order(items, CollectionSort.TITLE, Locale.GERMAN, { it.title }, { it.createdAt }).map { it.createdAt })
    }

    @Test
    fun recentPutsNewestFirstAndTitleBreaksTies() {
        val items = listOf(Item("Brot", 10), Item("Torte", 30), Item("Apfel", 30), Item("Suppe", 20))
        assertEquals(listOf("Apfel", "Torte", "Suppe", "Brot"), order(items, CollectionSort.RECENT))
    }

    @Test
    fun emptyListStaysEmpty() {
        assertEquals(emptyList<String>(), order(emptyList(), CollectionSort.RECENT))
    }

    @Test
    fun fromNameFallsBackToTitle() {
        assertEquals(CollectionSort.RECENT, CollectionSort.fromName("RECENT"))
        assertEquals(CollectionSort.TITLE, CollectionSort.fromName("irgendwas"))
        assertEquals(CollectionSort.TITLE, CollectionSort.fromName(null))
    }
}
