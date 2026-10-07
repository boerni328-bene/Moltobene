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

        /**
         * Kein Rezept im Standardformat, aber Text, der sich wie „Aus Text übernehmen“ einordnen lässt. [details] sind
         * eingebettete Rezeptdaten ohne Zutaten und Zubereitung, z. B. mit Portionen und Zeiten.
         */
        data class TextOnly(
            override val url: String,
            val title: String?,
            val text: String,
            val language: String?,
            val imageUrl: String?,
            val details: WebRecipe? = null,
        ) : Result

        /** Auf der Seite steht kein erkennbares Rezept; nur der Titel der Seite ist bekannt. */
        data class NoRecipe(override val url: String, val title: String?) : Result

        /**
         * Ein YouTube-Video: [recipe] ist das Rezept aus der Beschreibung, falls darin eines steht. [recipeLink] ist
         * ein Link zum Rezept auf einer Internetseite, den die Beschreibung nennt – er wird nur auf Wunsch geladen.
         */
        data class Video(
            override val url: String,
            val video: YouTube.Video,
            val recipe: WebRecipe?,
            val recipeLink: String?,
        ) : Result {
            /** Titel und vollständige Beschreibung für „Übernommener Text“ – so geht nichts verloren. */
            val text: String get() = listOfNotNull(video.title, video.description.trim().ifEmpty { null }).joinToString("\n\n")
        }
    }

    /** @throws WebException wenn die Seite nicht geladen werden konnte */
    suspend fun import(url: String): Result {
        YouTube.videoId(url)?.let { return importVideo(it) }
        val page = loader.loadPage(url)
        return withContext(Dispatchers.Default) { classify(page.url, RecipePage.read(page)) }
    }

    /**
     * YouTube (Vision: „nur als bestmöglicher Versuch“): Das Rezept steht höchstens in der Beschreibung des Videos.
     * Als Quelle zählt der Link zum Video ohne Zusätze, mit denen YouTube Weitergaben verfolgt.
     * @throws WebException wenn die Seite nicht geladen werden konnte oder kein Video enthält
     */
    private suspend fun importVideo(id: String): Result {
        val page = loader.loadPage(YouTube.pageUrl(id))
        return withContext(Dispatchers.Default) {
            val video = YouTube.read(page) ?: throw WebException(WebException.Problem.BLOCKED)
            Result.Video(
                url = YouTube.watchUrl(video.id),
                video = video,
                recipe = VideoDescription.recipe(video),
                recipeLink = VideoDescription.recipeLink(video.description),
            )
        }
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
            looksLikeRecipe(content.text) -> Result.TextOnly(
                url = url,
                title = content.title,
                text = content.text,
                language = recipe?.language ?: content.language,
                imageUrl = recipe?.imageUrls?.firstOrNull() ?: content.imageUrl,
                details = recipe,
            )
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
