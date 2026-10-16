package com.moltobene.app.data

import java.text.Collator
import java.util.Locale

/** Reihenfolge der Sammlung (#57). Gespeichert wird der Name der Auswahl, daher nie umbenennen. */
enum class CollectionSort {
    /** Nach Titel, A–Z; Umlaute und Akzente stehen bei ihrem Buchstaben. */
    TITLE,

    /** Zuletzt hinzugefügte Rezepte zuerst. */
    RECENT,
    ;

    companion object {
        /** Standard wie bisher: nach Titel. */
        val DEFAULT = TITLE

        fun fromName(name: String?): CollectionSort = entries.firstOrNull { it.name == name } ?: DEFAULT

        /**
         * Bringt [items] in die gewählte Reihenfolge. Titel werden nach den Regeln der Sprache verglichen (`Collator`,
         * Groß- und Kleinschreibung zählt nicht): „Äpfel“ steht vor „Zwiebelkuchen“. Gleiche Titel und gleiche
         * Zeitpunkte behalten ihre Reihenfolge von vorher (stabil).
         */
        fun <T> order(items: List<T>, sort: CollectionSort, locale: Locale, title: (T) -> String, createdAt: (T) -> Long): List<T> {
            val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
            return when (sort) {
                TITLE -> items.sortedWith { a, b -> collator.compare(title(a), title(b)) }
                RECENT -> items.sortedWith(
                    compareByDescending<T> { createdAt(it) }.thenComparator { a, b -> collator.compare(title(a), title(b)) },
                )
            }
        }
    }
}
