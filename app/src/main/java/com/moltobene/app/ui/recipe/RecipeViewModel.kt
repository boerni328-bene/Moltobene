package com.moltobene.app.ui.recipe

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moltobene.app.data.Adjustment
import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.dough.DoughRunPlanner
import com.moltobene.app.data.ocr.CropArea
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.RecipeSharer
import com.moltobene.app.data.share.ShareLabels
import com.moltobene.app.data.speech.StepNavigation
import com.moltobene.app.data.translate.LanguagePack
import com.moltobene.app.data.translate.RecipeTranslations
import com.moltobene.app.data.translate.RecipeTranslator
import com.moltobene.app.data.translate.TranslatedRecipe
import com.moltobene.app.ui.components.PageViewerModel
import com.moltobene.app.ui.navigation.RecipeRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

sealed interface RecipeUiState {
    data object Loading : RecipeUiState
    data object NotFound : RecipeUiState
    data object Error : RecipeUiState
    data class Content(val recipe: Recipe, val photo: File?) : RecipeUiState
}

/** „Rezept übersetzen“ (#60): was gerade statt des Originals gezeigt wird oder warum nicht. */
sealed interface TranslationState {
    data object Original : TranslationState

    /** Es wird übersetzt; [done] von [total] Zeilen und Sätzen sind fertig. */
    data class Working(val language: String, val done: Int, val total: Int) : TranslationState

    data class Shown(val translation: TranslatedRecipe) : TranslationState

    /** Das Sprachpaket fehlt noch. */
    data class NeedsPack(val language: String) : TranslationState

    data class Failed(val language: String, val lowMemory: Boolean) : TranslationState
}

/** Ergebnis von „Weiter“ und „Zurück“ (#79). */
sealed interface StepMove {
    data class To(val index: Int) : StepMove

    /** Schon der letzte Schritt. */
    data object AtEnd : StepMove

    /** Das Rezept hat keine Schritte. */
    data object None : StepMove
}

sealed interface ShareEvent {
    data class Ready(val share: PreparedShare) : ShareEvent
    data object Failed : ShareEvent
}

class RecipeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val sharer: RecipeSharer,
    private val translations: RecipeTranslations,
) : ViewModel() {

    val recipeId: String = savedStateHandle.toRoute<RecipeRoute>().id

    val state: StateFlow<RecipeUiState> = repository.observeRecipe(recipeId)
        .map { recipe ->
            if (recipe == null) {
                RecipeUiState.NotFound
            } else {
                RecipeUiState.Content(recipe, recipe.photoIds.firstOrNull()?.let { photoStore.photoFile(it) })
            }
        }
        .catch { emit(RecipeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeUiState.Loading)

    /**
     * Beim Kochen (#56): abgehakte Zutaten. Gemerkt werden Stelle und Text, damit nach einer Änderung am Rezept
     * keine andere Zutat abgehakt erscheint. Liegt im [SavedStateHandle] und übersteht so das Drehen des Handys
     * und das Beenden der App durch Android.
     */
    val checkedIngredients: StateFlow<List<String>> = savedStateHandle.getStateFlow(KEY_CHECKED, arrayListOf<String>())

    /** Der aktuelle Schritt, gemerkt wie die Zutaten; null, wenn keiner markiert ist. */
    val currentStep: StateFlow<String?> = savedStateHandle.getStateFlow<String?>(KEY_STEP, null)

    /**
     * „Umrechnen …“ (#66): nach Portionen oder nach einer Zutat; null heißt: wie im Rezept. Das gespeicherte Rezept
     * ändert sich nie. Liegt im [SavedStateHandle] wie die Häkchen.
     */
    val adjustment: StateFlow<Adjustment?> = savedStateHandle.getStateFlow<String?>(KEY_ADJUSTMENT, null)
        .map { Adjustment.decode(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Adjustment.decode(savedStateHandle.get<String>(KEY_ADJUSTMENT)))

    /** Laufende Teige dieses Rezepts (#71), die zuletzt gestarteten zuerst. */
    val runs: StateFlow<List<DoughRun>> = repository.observeRuns()
        .map { all -> all.filter { it.recipeId == recipeId } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** „Teig starten“: Der Zeitplan des Rezepts beginnt jetzt. */
    fun startRun() {
        viewModelScope.launch {
            repository.saveRun(DoughRun(id = UUID.randomUUID().toString(), recipeId = recipeId, startedAt = System.currentTimeMillis()))
        }
    }

    /** „Phase abschließen“: Die laufende Phase endet jetzt, die Folgezeiten rechnen ab hier neu. */
    fun completePhase(run: DoughRun) {
        val count = (state.value as? RecipeUiState.Content)?.recipe?.timeline?.size ?: return
        viewModelScope.launch { repository.saveRun(DoughRunPlanner.complete(run, count, System.currentTimeMillis())) }
    }

    fun undoPhase(run: DoughRun) {
        viewModelScope.launch { repository.saveRun(DoughRunPlanner.undo(run)) }
    }

    fun removeRun(run: DoughRun) {
        viewModelScope.launch { repository.deleteRun(run.id) }
    }

    fun toggleIngredient(index: Int, text: String) {
        val key = progressKey(index, text)
        val checked = ArrayList(checkedIngredients.value)
        if (!checked.remove(key)) checked += key
        savedStateHandle[KEY_CHECKED] = checked
    }

    /** Markiert einen Schritt als aktuellen Schritt; nochmals antippen hebt die Markierung auf. */
    fun toggleCurrentStep(index: Int, text: String) {
        val key = progressKey(index, text)
        savedStateHandle[KEY_STEP] = if (currentStep.value == key) null else key
    }

    /** „Weiter“ in der Kochleiste (#79); ohne Markierung der erste Schritt, nach dem letzten [StepMove.AtEnd]. */
    fun nextStep(): StepMove = moveStep(forward = true)

    /** „Zurück“ in der Kochleiste (#79); beim ersten Schritt bleibt es dabei. */
    fun previousStep(): StepMove = moveStep(forward = false)

    private fun moveStep(forward: Boolean): StepMove {
        val steps = (state.value as? RecipeUiState.Content)?.recipe?.steps.orEmpty()
        if (steps.isEmpty()) return StepMove.None
        val key = currentStep.value
        val current = steps.indices.firstOrNull { progressKey(it, steps[it]) == key }
        val target = (if (forward) StepNavigation.next(current, steps.size) else StepNavigation.previous(current, steps.size))
            ?: return StepMove.AtEnd
        savedStateHandle[KEY_STEP] = progressKey(target, steps[target])
        return StepMove.To(target)
    }

    /** Der markierte Schritt (Stelle), falls er noch zum Rezept passt. */
    fun currentStepIndex(): Int? {
        val steps = (state.value as? RecipeUiState.Content)?.recipe?.steps.orEmpty()
        val key = currentStep.value ?: return null
        return steps.indices.firstOrNull { progressKey(it, steps[it]) == key }
    }

    fun setAdjustment(adjustment: Adjustment?) {
        savedStateHandle[KEY_ADJUSTMENT] = adjustment?.let { Adjustment.encode(it) }
    }

    /** „Abhaken zurücksetzen“: keine Zutat abgehakt, kein aktueller Schritt. */
    fun resetProgress() {
        savedStateHandle[KEY_CHECKED] = arrayListOf<String>()
        savedStateHandle[KEY_STEP] = null
    }

    /**
     * Gewählte Sprache (#60); null heißt: das Original. Liegt im [SavedStateHandle] und wird je Rezept gemerkt,
     * damit ein übersetztes Rezept beim nächsten Öffnen wieder übersetzt erscheint.
     */
    private val chosenLanguage: StateFlow<String?> = savedStateHandle.getStateFlow<String?>(KEY_LANGUAGE, null)
    private val retry = MutableStateFlow(0)
    private val mutableTranslation = MutableStateFlow<TranslationState>(TranslationState.Original)
    val translation: StateFlow<TranslationState> = mutableTranslation.asStateFlow()

    init {
        viewModelScope.launch {
            if (!savedStateHandle.contains(KEY_LANGUAGE)) savedStateHandle[KEY_LANGUAGE] = translations.shownLanguage(recipeId)
            combine(state, chosenLanguage, retry) { current, language, _ -> (current as? RecipeUiState.Content)?.recipe to language }
                .collectLatest { (recipe, language) -> if (recipe != null) updateTranslation(recipe, language) }
        }
    }

    /** Zeigt das Rezept in [language]; die Originalsprache oder null zeigt das Original. */
    fun showLanguage(language: String?, original: String?) {
        val chosen = language?.takeIf { it != original }
        savedStateHandle[KEY_LANGUAGE] = chosen
        viewModelScope.launch { translations.setShownLanguage(recipeId, chosen) }
    }

    /** Nach einem Fehler oder wenn das Sprachpaket inzwischen geladen ist. */
    fun retryTranslation() {
        if (mutableTranslation.value is TranslationState.Failed || mutableTranslation.value is TranslationState.NeedsPack) retry.value++
    }

    private suspend fun updateTranslation(recipe: Recipe, language: String?) {
        val from = withContext(Dispatchers.Default) { RecipeTranslations.languageOf(recipe) }
        if (language == null || from == null || language == from || LanguagePack.direction(from, language) == null) {
            mutableTranslation.value = TranslationState.Original
            return
        }
        translations.cached(recipe, from, language)?.let {
            mutableTranslation.value = TranslationState.Shown(it)
            return
        }
        if (!translations.isAvailable()) {
            mutableTranslation.value = TranslationState.NeedsPack(language)
            return
        }
        val total = RecipeTranslator.workCount(recipe)
        mutableTranslation.value = TranslationState.Working(language, 0, total)
        // Der Fortschritt kommt aus dem Hintergrund; an die Anzeige geht er nur vom Hauptthread aus.
        val progress = MutableStateFlow(0)
        mutableTranslation.value = coroutineScope {
            val shown = launch { progress.collect { done -> mutableTranslation.value = TranslationState.Working(language, done, total) } }
            try {
                TranslationState.Shown(translations.translate(recipe, from, language) { done, _ -> progress.value = done })
            } catch (e: CancellationException) {
                throw e
            } catch (e: RecipeTranslations.LowMemoryException) {
                TranslationState.Failed(language, lowMemory = true)
            } catch (e: Exception) {
                TranslationState.Failed(language, lowMemory = false)
            } finally {
                shown.cancel()
            }
        }
    }

    /** „Originalseiten ansehen“ (#38). */
    val viewer = PageViewerModel(viewModelScope) { index ->
        val pageId = (state.value as? RecipeUiState.Content)?.recipe?.pageIds?.getOrNull(index)
            ?: throw IOException("Seite fehlt")
        photoStore.loadPage(Uri.fromFile(photoStore.photoFile(pageId)), CropArea.WHOLE_PAGE)
    }

    /** Das Foto des Rezepts als Vollbild, ganz und zum Vergrößern. */
    val photoViewer = PageViewerModel(viewModelScope) {
        val photo = (state.value as? RecipeUiState.Content)?.photo ?: throw IOException("Foto fehlt")
        photoStore.loadPage(Uri.fromFile(photo), CropArea.WHOLE_PAGE)
    }

    /** true, solange das Teilen vorbereitet wird. */
    var preparingShare by mutableStateOf(false)
        private set

    /** Ergebnis fürs Teilen; die Oberfläche öffnet damit das Teilen-Menü und setzt es zurück. */
    var shareEvent by mutableStateOf<ShareEvent?>(null)

    /** Teilt das Rezept als Text mit Foto oder ([asFile]) als Rezeptdatei. */
    fun share(asFile: Boolean, labels: ShareLabels) {
        val recipe = (state.value as? RecipeUiState.Content)?.recipe ?: return
        if (preparingShare) return
        preparingShare = true
        viewModelScope.launch {
            shareEvent = try {
                val prepared = if (asFile) {
                    sharer.prepareFile(recipe, labels)
                } else {
                    sharer.prepareText(recipe, labels)
                }
                ShareEvent.Ready(prepared)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ShareEvent.Failed
            }
            preparingShare = false
        }
    }

    /** Favorit setzen oder entfernen; Änderungszeit und Inhalt des Rezepts bleiben unberührt. */
    fun toggleFavorite() {
        val recipe = (state.value as? RecipeUiState.Content)?.recipe ?: return
        viewModelScope.launch { repository.save(recipe.copy(favorite = !recipe.favorite)) }
    }

    /** Eigene Notiz zum Rezept speichern (#61); zählt als Änderung, damit der Hinweis zum Sichern sie mitzählt. */
    fun saveNotes(notes: String) {
        val recipe = (state.value as? RecipeUiState.Content)?.recipe ?: return
        if (notes == recipe.notes) return
        viewModelScope.launch { repository.save(recipe.copy(notes = notes, updatedAt = System.currentTimeMillis())) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(recipeId)
            translations.delete(recipeId)
            onDeleted()
        }
    }

    companion object {
        private const val KEY_CHECKED = "cooking_checked"
        private const val KEY_STEP = "cooking_step"
        private const val KEY_ADJUSTMENT = "cooking_adjustment"
        private const val KEY_LANGUAGE = "translation_language"

        /** Kennung einer Zeile beim Kochen: Stelle und Text. */
        fun progressKey(index: Int, text: String): String = "$index:${text.hashCode()}"
    }
}
