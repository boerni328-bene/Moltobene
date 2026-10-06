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
        return withContext(Dispatchers.Default) { classify(page.url, RecipePage.read(page)) }
    }

    /**
     * Eine Rezeptdatei (#54): schema.org/Recipe als JSON oder eine gespeicherte Rezeptseite (HTML). Ohne Internet;
     * relative Links in der Datei zählen nicht, weil es keine Seite gibt, zu der sie gehören.
     */
    suspend fun readFile(body: ByteArray): Result = withContext(Dispatchers.Default) {
        val content = RecipePage.read(WebPage(url = "", body = body, charset = null))
        classify(content.recipe?.url.orEmpty(), content)
    }

    private fun classify(url: String, content: RecipePage.Content): Result {
        val recipe = content.recipe
        return when {
            recipe != null && recipe.hasContent -> Result.Found(url, recipe)
            looksLikeRecipe(content.text) -> Result.TextOnly(url, content.title, content.text, content.language, content.imageUrl)
            else -> Result.NoRecipe(url, content.title)
        }
    }

    /**
     * Ein in die Rezeptdatei eingebettetes Foto („data:image/…;base64,…“), wie Moltobene es beim Teilen schreibt –
     * ohne Internet in eine Datei im Zwischenspeicher. Der Aufrufer übernimmt sie und löscht sie danach.
     * @throws java.io.IOException wenn es kein eingebettetes JPEG, PNG oder WebP ist oder es zu groß ist
     */
    suspend fun embeddedPhoto(dataUrl: String): File = withContext(Dispatchers.IO) {
        val bytes = EmbeddedImage.decode(dataUrl) ?: throw java.io.IOException("Kein eingebettetes Foto")
        val dir = File(cacheDir, WEB_DIR).apply { mkdirs() }
        File(dir, "${UUID.randomUUID()}.img").also { it.writeBytes(bytes) }
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

/** Eingebettete Fotos („data:“-Links) aus Rezeptdateien: nur JPEG, PNG und WebP, nur Base64, höchstens 10 MB. */
internal object EmbeddedImage {

    fun decode(dataUrl: String): ByteArray? {
        val match = DATA_URL.find(dataUrl.take(HEADER_LENGTH)) ?: return null
        val data = dataUrl.substring(match.range.last + 1)
        if (data.length > MAX_BYTES / 3 * 4 + 4) return null
        return runCatching { java.util.Base64.getMimeDecoder().decode(data) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private const val MAX_BYTES = 10 * 1024 * 1024
    private const val HEADER_LENGTH = 64
    private val DATA_URL = Regex("^data:image/(jpeg|jpg|png|webp);base64,", RegexOption.IGNORE_CASE)
}

/**
 * Text einer Seite ohne Rezept im Standardformat wird nur eingeordnet, wenn er eine Überschrift für Zutaten
 * enthält – sonst würden Menüs oder Artikeltexte als Zutaten im Entwurf landen.
 */
internal fun looksLikeRecipe(text: String): Boolean = INGREDIENTS_HEADING.containsMatchIn(text)

private val INGREDIENTS_HEADING = Regex(
    "(?im)^\\s*(zutaten|ingredients|ingredienti|ingrédients|ingredientes)\\b.{0,40}$",
)
