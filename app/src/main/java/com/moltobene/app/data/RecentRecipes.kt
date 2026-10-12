package com.moltobene.app.data

/**
 * „Zuletzt geöffnet“ auf der Startseite (#81): die Kennungen der zuletzt geöffneten Rezepte, das neueste zuerst.
 * Gespeichert nur auf diesem Handy in [AppPreferences] – nicht in der Datenbank, nicht in der Sicherung, nie geteilt.
 */
object RecentRecipes {
    const val MAX = 5

    fun decode(text: String?): List<String> =
        text.orEmpty().split(',').filter { RecipeIds.isValid(it) }.distinct().take(MAX)

    fun encode(ids: List<String>): String = ids.joinToString(",")

    /** Das geöffnete Rezept kommt nach vorn; ein früherer Eintrag desselben Rezepts entfällt. */
    fun add(ids: List<String>, id: String): List<String> =
        if (RecipeIds.isValid(id)) (listOf(id) + ids.filterNot { it == id }).take(MAX) else ids
}
