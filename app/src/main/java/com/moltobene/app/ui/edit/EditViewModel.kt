package com.moltobene.app.ui.edit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moltobene.app.R
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.ocr.GrayImage
import com.moltobene.app.data.ocr.RecipeTextParser
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.ocr.TextRecognizer
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.ui.navigation.EditRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Stand der Texterkennung; [page] zählt ab 1. */
sealed interface RecognitionState {
    data object Idle : RecognitionState
    data class Running(val page: Int, val pageCount: Int) : RecognitionState
}

/**
 * Formular zum Hinzufügen und Bearbeiten. Alle Eingaben liegen im SavedStateHandle und überstehen
 * so Drehen, Anrufe und das Beenden der App durch Android. Ein neues Rezept wird zusätzlich still
 * als Entwurf gespeichert, sobald die App in den Hintergrund geht.
 */
class EditViewModel(
    private val handle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val recognizer: TextRecognizer,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val route = handle.toRoute<EditRoute>()
    private val routeId: String? = route.id
    val isNew: Boolean = routeId == null

    private val titleField = SavedField(handle, "title", "")
    private val servingsField = SavedField(handle, "servings", "")
    private val servingsUnitField = SavedField(handle, "servingsUnit", "")
    private val ingredientsField = SavedField(handle, "ingredients", "")
    private val stepsField = SavedField(handle, "steps", "")
    private val sourceField = SavedField(handle, "source", "")
    private val notesField = SavedField(handle, "notes", "")
    /** Vollständiger erkannter Text; wird als Originaltext gespeichert, damit nichts verloren geht. */
    private val recognizedTextField = SavedField(handle, "recognizedText", "")
    private val languageField = SavedField<String?>(handle, "language", null)
    private val startPromptShownField = SavedField(handle, "startPromptShown", false)
    private val photoIdField = SavedField<String?>(handle, "photoId", null)
    private val originalPhotoIdField = SavedField<String?>(handle, "originalPhotoId", null)
    private val draftIdField = SavedField<String?>(handle, "draftId", null)
    private val dirtyField = SavedField(handle, "dirty", false)
    private val loadedField = SavedField(handle, "loaded", routeId == null)

    var title: String
        get() = titleField.value
        set(value) {
            titleField.value = value
            titleError = false
            dirtyField.value = true
        }
    var servings: String
        get() = servingsField.value
        set(value) = change(servingsField, value)
    var servingsUnit: String
        get() = servingsUnitField.value
        set(value) = change(servingsUnitField, value)
    var ingredients: String
        get() = ingredientsField.value
        set(value) = change(ingredientsField, value)
    var steps: String
        get() = stepsField.value
        set(value) = change(stepsField, value)
    var source: String
        get() = sourceField.value
        set(value) = change(sourceField, value)
    var notes: String
        get() = notesField.value
        set(value) = change(notesField, value)

    val photoFile: File? get() = photoIdField.value?.let { photoStore.photoFile(it) }
    val hasPhoto: Boolean get() = photoIdField.value != null
    val recognizedText: String get() = recognizedTextField.value
    val isDirty: Boolean get() = dirtyField.value

    var titleError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(!loadedField.value)
        private set
    var isProcessingPhoto by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var recognition by mutableStateOf<RecognitionState>(RecognitionState.Idle)
        private set
    val isRecognizing: Boolean get() = recognition is RecognitionState.Running

    /** Meldung für die Meldungsleiste (Text-Ressource), wird nach dem Anzeigen zurückgesetzt. */
    var message by mutableStateOf<Int?>(null)

    /** Gespeichert oder verworfen – danach wird nichts mehr automatisch gespeichert. */
    private var finished = false

    init {
        if (!loadedField.value && routeId != null) load(routeId)
    }

    private fun <T> change(field: SavedField<T>, value: T) {
        field.value = value
        dirtyField.value = true
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val recipe = runCatching { repository.getRecipe(id) }.getOrNull()
            if (recipe != null) {
                titleField.value = recipe.title
                servingsField.value = recipe.servings?.toString().orEmpty()
                servingsUnitField.value = recipe.servingsUnit.orEmpty()
                ingredientsField.value = RecipeText.formatIngredients(recipe.ingredients)
                stepsField.value = RecipeText.formatSteps(recipe.steps)
                sourceField.value = RecipeText.formatSource(recipe.source)
                notesField.value = recipe.notes
                photoIdField.value = recipe.photoIds.firstOrNull()
                originalPhotoIdField.value = recipe.photoIds.firstOrNull()
                recognizedTextField.value = recipe.originalText.orEmpty()
            }
            loadedField.value = true
            isLoading = false
        }
    }

    fun cameraUri(): Uri = photoStore.cameraUri()

    fun onPhotoPicked(uri: Uri) = importPhoto { photoStore.importFromUri(uri) }

    fun onCameraResult(success: Boolean) {
        if (success) importPhoto { photoStore.importCameraPhoto() }
    }

    /** „Aus Foto übernehmen“ in der Sammlung: Die Auswahl der Fotos erscheint beim ersten Öffnen von selbst. */
    fun takeStartPrompt(): Boolean {
        if (!route.fromPhoto || startPromptShownField.value) return false
        startPromptShownField.value = true
        return true
    }

    /** Liest das Rezeptfoto. */
    fun recognizeRecipePhoto() {
        val photoId = photoIdField.value ?: return
        runRecognition(listOf(suspend { photoStore.loadForRecognition(photoId) }))
    }

    /** Liest ausgewählte Fotos in der gewählten Reihenfolge; ohne Rezeptfoto wird das erste zum Rezeptfoto. */
    fun recognizePhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        runRecognition(
            pages = uris.map { uri -> suspend { photoStore.loadForRecognition(uri) } },
            prepare = { if (photoIdField.value == null) replacePhoto(photoStore.importFromUri(uris.first())) },
        )
    }

    /** Liest ein gerade aufgenommenes Foto; ohne Rezeptfoto wird es zum Rezeptfoto. */
    fun onRecognitionCameraResult(success: Boolean) {
        if (!success) return
        val capture = photoStore.cameraCaptureUri()
        runRecognition(
            pages = listOf(suspend { photoStore.loadForRecognition(capture) }),
            prepare = { if (photoIdField.value == null) replacePhoto(photoStore.importFromUri(capture)) },
            cleanUp = { photoStore.discardCameraPhoto() },
        )
    }

    fun cancelRecognition() = recognizer.cancel()

    private fun runRecognition(
        pages: List<suspend () -> GrayImage>,
        prepare: suspend () -> Unit = {},
        cleanUp: suspend () -> Unit = {},
    ) {
        if (isRecognizing) return
        recognition = RecognitionState.Running(1, pages.size)
        viewModelScope.launch {
            try {
                // Zuerst das Foto übernehmen: Es bleibt erhalten, auch wenn die Erkennung nicht klappt.
                try {
                    prepare()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = R.string.photo_error
                }
                val preferred = preferences.recognitionLanguage()
                    ?: TextLanguage.supportedOrDefault(Locale.getDefault().language)
                val result = recognizer.recognize(pages, preferred) { index ->
                    recognition = RecognitionState.Running(index + 1, pages.size)
                }
                preferences.setRecognitionLanguage(result.language)
                if (result.text.isBlank()) {
                    message = R.string.ocr_nothing
                } else {
                    applyRecognized(result)
                    message = R.string.ocr_done
                }
            } catch (e: TextRecognizer.CancelledException) {
                // Abgebrochen: nichts ändern.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = R.string.ocr_error
            } finally {
                runCatching { cleanUp() }
                recognition = RecognitionState.Idle
            }
        }
    }

    /**
     * Übernimmt den erkannten Text: Leere Felder werden ausgefüllt, Zutaten und Zubereitung ergänzt
     * (z. B. bei einer weiteren Seite). Der vollständige Text bleibt als Originaltext erhalten.
     */
    private fun applyRecognized(result: TextRecognizer.Result) {
        val text = result.text.trim()
        if (!recognizedTextField.value.contains(text)) {
            recognizedTextField.value = listOf(recognizedTextField.value.trim(), text).filter { it.isNotEmpty() }.joinToString("\n\n")
        }
        val parsed = RecipeTextParser.parse(text)
        if (title.isBlank()) parsed.title?.let { title = it }
        if (servings.isBlank()) parsed.servings?.takeIf { it <= 999 }?.let { servingsField.value = it.toString() }
        if (servingsUnit.isBlank()) parsed.servingsUnit?.let { servingsUnitField.value = it }
        ingredientsField.value = appendBlock(ingredients, parsed.ingredients)
        stepsField.value = appendBlock(steps, parsed.steps)
        languageField.value = result.language
        dirtyField.value = true
    }

    private fun appendBlock(current: String, lines: List<String>): String {
        if (lines.isEmpty()) return current
        val block = lines.joinToString("\n")
        return when {
            current.isBlank() -> block
            current.contains(block) -> current
            else -> current.trimEnd() + "\n" + block
        }
    }

    override fun onCleared() {
        recognizer.cancel()
    }

    fun removePhoto() {
        viewModelScope.launch { replacePhoto(null) }
    }

    private fun importPhoto(block: suspend () -> String) {
        viewModelScope.launch {
            isProcessingPhoto = true
            try {
                replacePhoto(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = R.string.photo_error
            } finally {
                isProcessingPhoto = false
            }
        }
    }

    private suspend fun replacePhoto(newPhotoId: String?) {
        val previous = photoIdField.value
        photoIdField.value = newPhotoId
        dirtyField.value = true
        // Ein neu gewähltes, noch nicht gespeichertes Foto wird sofort wieder entfernt, wenn es ersetzt wird.
        if (previous != null && previous != newPhotoId && previous != originalPhotoIdField.value) {
            photoStore.delete(previous)
        }
    }

    fun save(onSaved: (id: String, wasNew: Boolean) -> Unit) {
        if (title.isBlank()) {
            titleError = true
            return
        }
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                val id = routeId ?: draftIdField.value ?: RecipeIds.newId()
                val base = repository.getRecipe(id)
                repository.save(buildRecipe(id, base, isDraft = false))
                val oldMainPhoto = originalPhotoIdField.value
                if (oldMainPhoto != null && oldMainPhoto != photoIdField.value) photoStore.delete(oldMainPhoto)
                originalPhotoIdField.value = photoIdField.value
                dirtyField.value = false
                finished = true
                onSaved(id, isNew)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = R.string.save_error
            } finally {
                isSaving = false
            }
        }
    }

    /** Verwirft alle Änderungen: neue Fotos und ein automatisch gespeicherter Entwurf werden entfernt. */
    fun discard(onDone: () -> Unit) {
        finished = true
        viewModelScope.launch {
            val current = photoIdField.value
            if (current != null && current != originalPhotoIdField.value) photoStore.delete(current)
            draftIdField.value?.let { runCatching { repository.delete(it) } }
            onDone()
        }
    }

    /** App geht in den Hintergrund: ein neues Rezept still als Entwurf sichern. */
    fun onStop() {
        if (finished || !isNew || !isDirty || !hasContent()) return
        viewModelScope.launch {
            runCatching {
                val id = draftIdField.value ?: RecipeIds.newId().also { draftIdField.value = it }
                val base = repository.getRecipe(id)
                repository.save(buildRecipe(id, base, isDraft = true))
            }
        }
    }

    private fun hasContent(): Boolean =
        listOf(title, ingredients, steps, source, notes, recognizedText).any { it.isNotBlank() } || photoIdField.value != null

    private fun buildRecipe(id: String, base: Recipe?, isDraft: Boolean): Recipe {
        val now = System.currentTimeMillis()
        val start = base ?: Recipe(
            id = id,
            title = "",
            language = Locale.getDefault().language,
            createdAt = now,
            updatedAt = now,
        )
        // Unveränderte Quelle behält ihre Details (z. B. Seitenangabe aus einer Sicherung).
        val parsedSource = if (base != null && RecipeText.formatSource(base.source) == source.trim()) {
            base.source
        } else {
            RecipeText.parseSource(source)
        }
        val mainPhoto = photoIdField.value
        val furtherPhotos = base?.photoIds?.drop(1).orEmpty().filter { it != mainPhoto }
        return start.copy(
            title = title.trim(),
            servings = servings.toIntOrNull()?.takeIf { it > 0 },
            servingsUnit = servingsUnit.trim().ifBlank { null },
            source = parsedSource,
            notes = notes.trim(),
            isDraft = isDraft,
            ingredients = RecipeText.parseIngredients(ingredients),
            steps = RecipeText.parseSteps(steps),
            photoIds = listOfNotNull(mainPhoto) + furtherPhotos,
            originalText = recognizedText.trim().ifEmpty { null },
            language = languageField.value ?: start.language,
            updatedAt = now,
        )
    }
}

/** Ein Formularfeld, das Compose beobachten kann und das zugleich im SavedStateHandle liegt. */
internal class SavedField<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    default: T,
) {
    private val state = mutableStateOf(handle.get<T>(key) ?: default)

    var value: T
        get() = state.value
        set(newValue) {
            state.value = newValue
            handle[key] = newValue
        }
}
