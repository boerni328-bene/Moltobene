package com.moltobene.app.data.translate

import com.moltobene.app.data.Recipe
import com.moltobene.app.data.ocr.TextLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** Übersetzer für eine Richtung des Sprachpakets; im Rundgang auf dem Emulator nachgestellt. */
interface TranslationEngine {

    /** Ist das Sprachpaket vorhanden? Liest vom Speicher, also nicht auf dem Hauptthread aufrufen. */
    fun isAvailable(): Boolean

    /** Öffnet die Modelle für eine Richtung wie „en-de“; nach dem Übersetzen schließen. */
    fun open(direction: String): Session

    interface Session : LineTranslator, AutoCloseable
}

/** Der echte Übersetzer: OPUS-MT aus dem heruntergeladenen Sprachpaket. */
class MarianEngine(private val directory: () -> File) : TranslationEngine {

    override fun isAvailable(): Boolean = LanguagePack.isComplete(directory())

    override fun open(direction: String): TranslationEngine.Session {
        val translator = MarianTranslator(File(directory(), direction))
        return object : TranslationEngine.Session {
            override fun translate(text: String): String = translator.translate(text)
            override fun close() = translator.close()
        }
    }
}

/**
 * „Rezept übersetzen“ (#60): gespeicherte Übersetzung laden oder neu übersetzen und speichern. Es übersetzt immer
 * nur ein Rezept zugleich, weil die Modelle etwa 300 MB Speicher brauchen; danach werden sie wieder freigegeben.
 */
class RecipeTranslations(val store: TranslationStore, private val engine: () -> TranslationEngine) {

    /** Für die Übersetzung war zu wenig Speicher frei – ein normaler Fehler statt eines Absturzes. */
    class LowMemoryException(cause: Throwable) : IOException("Zu wenig Speicher für die Übersetzung", cause)

    private val mutex = Mutex()

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) { runCatching { engine().isAvailable() }.getOrDefault(false) }

    suspend fun cached(recipe: Recipe, from: String, to: String): TranslatedRecipe? = withContext(Dispatchers.IO) {
        store.load(recipe.id, to, RecipeTranslator.sourceHash(recipe, from, to))
    }

    suspend fun shownLanguage(recipeId: String): String? = withContext(Dispatchers.IO) { store.shownLanguage(recipeId) }

    suspend fun setShownLanguage(recipeId: String, language: String?) = withContext(Dispatchers.IO) {
        runCatching { store.setShownLanguage(recipeId, language) }
    }

    suspend fun delete(recipeId: String) = withContext(Dispatchers.IO) { runCatching { store.delete(recipeId) } }

    suspend fun deleteUnused(recipeIds: Set<String>) = withContext(Dispatchers.IO) { runCatching { store.deleteUnused(recipeIds) } }

    /** Übersetzt und speichert; Fortschritt als erledigte Zeilen von allen. Abbrechen wirkt nach der laufenden Zeile. */
    suspend fun translate(recipe: Recipe, from: String, to: String, onProgress: (done: Int, total: Int) -> Unit): TranslatedRecipe =
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val context = coroutineContext
                val direction = LanguagePack.direction(from, to) ?: throw IOException("Keine Übersetzung $from → $to")
                val translation = try {
                    engine().open(direction).use { session ->
                        RecipeTranslator.translate(recipe, from, to, session, check = { context.ensureActive() }, onProgress = onProgress)
                    }
                } catch (e: OutOfMemoryError) {
                    throw LowMemoryException(e)
                } catch (e: LinkageError) {
                    throw IOException("Übersetzung nicht verfügbar", e)
                }
                withContext(Dispatchers.IO) { runCatching { store.save(recipe.id, translation) } }
                translation
            }
        }

    companion object {
        /**
         * Sprache des Rezepts: wie gespeichert, sonst am Text erkannt. Übersetzt werden kann nur, wenn das Sprachpaket
         * die Sprache kennt (heute Deutsch und Englisch).
         */
        fun languageOf(recipe: Recipe): String? =
            recipe.language?.lowercase()?.substringBefore('-')
                ?: TextLanguage.detect(
                    (listOf(recipe.title) + recipe.ingredients.map { it.text } + recipe.steps).joinToString("\n"),
                )
    }
}
