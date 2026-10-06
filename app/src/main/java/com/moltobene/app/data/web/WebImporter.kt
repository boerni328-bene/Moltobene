package com.moltobene.app.data.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * „Aus Link übernehmen“ (#55): lädt die Seite und liest das Rezept. Das Ergebnis ist nie mehr als ein
 * Vorschlag für den Entwurf; was schon im Formular steht, entscheidet das Formular.
 */
class WebImporter(private val loader: PageLoader, private val cacheDir: File) {

    sealed interface Result {
        /** Adresse der Seite nach allen Weiterleitungen. */
        val url: String

        /** Die Seite enthält ein Rezept im Standardformat. */
        data class Found(override val url: String, val recipe: WebRecipe) : Result

        /** Kein Rezept im Standardformat, aber Text, der sich wie „Aus Text übernehmen“ einordnen lässt. */
        data class TextOnly(
            override val url: String,
            val title: String?,
            val text: String,
            val language: String?,
            val imageUrl: String?,
        ) : Result

        /** Auf der Seite steht kein erkennbares Rezept; nur der Titel der Seite ist bekannt. */
        data class NoRecipe(override val url: String, val title: String?) : Result
    }

    /** @throws WebException wenn die Seite nicht geladen werden konnte */
    suspend fun import(url: String): Result {
        val page = loader.loadPage(url)
        val content = withContext(Dispatchers.Default) { RecipePage.read(page) }
        val recipe = content.recipe
        return when {
            recipe != null && recipe.hasContent -> Result.Found(page.url, recipe)
            looksLikeRecipe(content.text) -> Result.TextOnly(page.url, content.title, content.text, content.language, content.imageUrl)
            else -> Result.NoRecipe(page.url, content.title)
        }
    }

    /** Lädt ein Foto in eine Datei im Zwischenspeicher; der Aufrufer übernimmt sie und löscht sie danach. */
    suspend fun downloadPhoto(url: String): File {
        val dir = File(cacheDir, WEB_DIR)
        withContext(Dispatchers.IO) { dir.mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.img")
        try {
            loader.loadImage(url, target)
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { target.delete() }
            throw e
        }
        return target
    }

    private companion object {
        const val WEB_DIR = "web"
    }
}

/**
 * Text einer Seite ohne Rezept im Standardformat wird nur eingeordnet, wenn er eine Überschrift für Zutaten
 * enthält – sonst würden Menüs oder Artikeltexte als Zutaten im Entwurf landen.
 */
internal fun looksLikeRecipe(text: String): Boolean = INGREDIENTS_HEADING.containsMatchIn(text)

private val INGREDIENTS_HEADING = Regex(
    "(?im)^\\s*(zutaten|ingredients|ingredienti|ingrédients|ingredientes)\\b.{0,40}$",
)
