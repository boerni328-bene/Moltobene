package com.moltobene.app.ui.edit

import android.graphics.Bitmap
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
import com.moltobene.app.data.DraftMerge
import com.moltobene.app.data.DraftText
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.ocr.AreaFrame
import com.moltobene.app.data.ocr.AreaKind
import com.moltobene.app.data.ocr.CropArea
import com.moltobene.app.data.ocr.PendingRecognition
import com.moltobene.app.data.ocr.RecipeTextParser
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.ocr.TextRecognizer
import com.moltobene.app.data.ocr.boundsOf
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.ui.components.PageViewerModel
import com.moltobene.app.ui.navigation.EditRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

/** Höchstzahl der Seiten, die auf einmal gelesen werden – aus der Fotoauswahl wie aus „Teilen mit…“. */
internal const val MAX_PAGES = 6

/** Stand der Texterkennung; [page] zählt ab 1. */
sealed interface RecognitionState {
    data object Idle : RecognitionState
    /** @param percent Fortschritt der ganzen Erkennung (0–100) */
    data class Running(val page: Int, val pageCount: Int, val percent: Int = 0) : RecognitionState
}

/** „Bereich auswählen“ vor der Texterkennung; [page] zählt ab 1. */
data class AreaSelection(val page: Int, val pageCount: Int, val frames: List<AreaFrame>, val active: Int) {
    /** Nur ein Rahmen „Alles“ über die ganze Seite – so beginnt jede Seite. */
    val isWholePage: Boolean get() = frames.size == 1 && frames[0] == AreaFrame.WHOLE_PAGE
}

/** Eine gelesene Seite mit dem Bereich, der gelesen wurde. [ref] ist „photo:<Kennung>“ oder „page:<Name>“. */
private data class ReadPage(val ref: String, val area: CropArea) {
    fun encode(): String = ref + "|" + area.encode()

    companion object {
        fun decode(line: String) = ReadPage(line.substringBefore('|'), CropArea.decode(line.substringAfter('|', "")))
    }
}

/** Eine Seite in „Seiten ansehen“: eine schon gespeicherte Originalseite oder eine gerade gelesene Seite. */
private sealed interface ViewerPage {
    data class Stored(val photoId: String) : ViewerPage
    data class Read(val page: ReadPage) : ViewerPage
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
    private val pending: PendingRecognition,
    private val sharedPhotos: SharedPhotos,
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
    /** Seite in der Quelle, z. B. „47“ (#40). */
    private val sourcePageField = SavedField(handle, "sourcePage", "")
    /** Bei der Texterkennung gefundene Seitenzahl – als Vorschlag für „Seite“. */
    private val suggestedPageField = SavedField(handle, "suggestedPage", "")
    private val notesField = SavedField(handle, "notes", "")
    /** Vollständiger erkannter Text; wird als Originaltext gespeichert, damit nichts verloren geht. */
    private val recognizedTextField = SavedField(handle, "recognizedText", "")
    /** Sprache des Rezepts; durch die Texterkennung nur gesetzt, wenn sie eindeutig erkannt wurde (#42). */
    private val languageField = SavedField<String?>(handle, "language", null)
    /** In „Bereich auswählen“ gewählte Sprache des Textes; null = automatisch erkennen. */
    private val languageChoiceField = SavedField<String?>(handle, "languageChoice", null)
    private val startPromptShownField = SavedField(handle, "startPromptShown", false)
    /** Die Bilder aus „Teilen mit…“ wurden übernommen – nach dem Drehen nicht noch einmal. */
    private val sharedTakenField = SavedField(handle, "sharedTaken", false)
    /** Nach einer Erkennung bleibt der Hinweis zum Prüfen bis zum Speichern sichtbar. */
    private val checkHintField = SavedField(handle, "checkHint", false)
    /** Seiten für „Bereich auswählen“ und die laufende Erkennung, eine je Zeile: „photo:<Kennung>“ oder „page:<Name>“. */
    private val areaPagesField = SavedField(handle, "areaPages", "")
    /** Rahmen je Seite (#39), Seiten getrennt mit „;“ – siehe [AreaFrame.encodeAll]. */
    private val areaListField = SavedField(handle, "areaList", "")
    private val areaIndexField = SavedField(handle, "areaIndex", 0)
    /** Rahmen der aktuellen Seite, der gerade bearbeitet wird. */
    private val areaFrameField = SavedField(handle, "areaFrame", 0)
    /** Kennung, unter der die kopierten Seiten und ein Ergebnis der Erkennung als Dateien liegen (#37). */
    private val sessionField = SavedField<String?>(handle, "recognitionSession", null)
    /** Die Erkennung läuft; Seiten und Bereiche bleiben, bis ihr Ergebnis im Rezept steht. */
    private val recognitionStartedField = SavedField(handle, "recognitionStarted", false)
    /** Gelesene Seiten dieses Formulars (#38), eine je Zeile – zum Prüfen und als mögliche Originalseiten. */
    private val readPagesField = SavedField(handle, "readPages", "")
    /** Schon gespeicherte Originalseiten des Rezepts, eine Kennung je Zeile, und ihr Stand beim Öffnen. */
    private val pageIdsField = SavedField(handle, "pageIds", "")
    private val originalPageIdsField = SavedField(handle, "originalPageIds", "")
    /** Vor dem Speichern wird gefragt, ob die gelesenen Seiten als Originalseiten bleiben. */
    private val askKeepPagesField = SavedField(handle, "askKeepPages", false)
    private val photoIdField = SavedField<String?>(handle, "photoId", null)
    private val originalPhotoIdField = SavedField<String?>(handle, "originalPhotoId", null)
    /** Steht von Anfang an fest, damit der Entwurf auch nach dem Beenden durch Android dieselbe Kennung behält. */
    private val draftIdField = SavedField<String?>(handle, "draftId", null).apply {
        if (isNew && value == null) value = RecipeIds.newId()
    }
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
    var sourcePage: String
        get() = sourcePageField.value
        set(value) = change(sourcePageField, value)

    /** Bei einem Link gibt es keine Seite. */
    val sourceIsLink: Boolean get() = RecipeText.isWebLink(source.trim())

    /** Zuletzt genutzte Bücher aus der Sammlung; nach einer Texterkennung als Vorschlag für die Quelle (#40). */
    private var recentSources by mutableStateOf<List<String>>(emptyList())

    /** Vorschläge für „Quelle“ – nur nach einer Texterkennung und solange noch keine Quelle eingetragen ist. */
    val sourceSuggestions: List<String>
        get() = if (showCheckHint && source.isBlank()) recentSources else emptyList()

    /** Vorschlag für „Seite“: die erkannte Seitenzahl, solange noch keine Seite eingetragen ist. */
    val pageSuggestion: String?
        get() = suggestedPageField.value.takeIf { it.isNotEmpty() && sourcePage.isBlank() && !sourceIsLink }

    fun takeSourceSuggestion(name: String) {
        source = name
    }

    fun takePageSuggestion() {
        pageSuggestion?.let { sourcePage = it }
    }

    private fun loadSourceSuggestions() {
        viewModelScope.launch {
            recentSources = runCatching { repository.recentSourceNames() }.getOrDefault(emptyList())
        }
    }
    var notes: String
        get() = notesField.value
        set(value) = change(notesField, value)

    val photoFile: File? get() = photoIdField.value?.let { photoStore.photoFile(it) }
    val hasPhoto: Boolean get() = photoIdField.value != null
    val recognizedText: String get() = recognizedTextField.value
    val showCheckHint: Boolean get() = checkHintField.value
    val askKeepPages: Boolean get() = askKeepPagesField.value
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

    /** Android hat die App während der Erkennung beendet; sie lässt sich mit denselben Seiten wiederholen. */
    var recognitionInterrupted by mutableStateOf(false)
        private set

    /** Foto der Seite, deren Bereich gerade gewählt wird; null, solange es lädt. */
    var areaPreview by mutableStateOf<Bitmap?>(null)
        private set
    var areaPreviewFailed by mutableStateOf(false)
        private set
    private var previewJob: Job? = null

    /** „Seiten ansehen“ (#38): gespeicherte Originalseiten und die gerade gelesenen Seiten. */
    val viewer = PageViewerModel(viewModelScope) { index -> loadViewerPage(index) }

    /** Anzahl der Seiten in „Seiten ansehen“. */
    val pageCount: Int get() = viewerPages().size

    /** Läuft gerade „Bereich auswählen“? */
    val areaSelection: AreaSelection?
        get() {
            val pages = areaPages()
            if (pages.isEmpty() || recognitionStartedField.value) return null
            val index = areaIndexField.value.coerceIn(0, pages.lastIndex)
            val frames = pageFrames(pages.size)[index]
            return AreaSelection(index + 1, pages.size, frames, areaFrameField.value.coerceIn(0, frames.lastIndex))
        }

    /** Meldung für die Meldungsleiste (Text-Ressource), wird nach dem Anzeigen zurückgesetzt. */
    var message by mutableStateOf<Int?>(null)

    /** Gespeichert oder verworfen – danach wird nichts mehr automatisch gespeichert. */
    private var finished = false

    /** Die App ist nicht sichtbar; Android kann sie jederzeit beenden. */
    private var inBackground = false

    /**
     * Aufräumen nach einer Erkennung, die im Hintergrund zu Ende ging. Der zuletzt von Android
     * gesicherte Stand kennt sie noch – beendet Android die App, wird sie damit fortgesetzt.
     * Erst wenn die App wieder sichtbar ist, werden Ergebnisdatei und ungelesene Seiten gelöscht.
     */
    private var cleanUpWhenVisible: (suspend () -> Unit)? = null

    init {
        if (!loadedField.value && routeId != null) load(routeId)
        if (checkHintField.value) loadSourceSuggestions()
        if (route.fromShare && !sharedTakenField.value) takeSharedPhotos()
        when {
            // Android hat die App während der Erkennung beendet.
            recognitionStartedField.value -> resumeRecognition()
            // Nach dem Beenden der App durch Android geht „Bereich auswählen“ an derselben Stelle weiter.
            areaPages().isNotEmpty() -> loadAreaPreview()
        }
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
                sourcePageField.value = recipe.source?.page.orEmpty()
                notesField.value = recipe.notes
                photoIdField.value = recipe.photoIds.firstOrNull()
                originalPhotoIdField.value = recipe.photoIds.firstOrNull()
                pageIdsField.value = recipe.pageIds.joinToString("\n")
                originalPageIdsField.value = pageIdsField.value
                recognizedTextField.value = recipe.originalText.orEmpty()
                languageField.value = recipe.language
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

    /** Liest das Rezeptfoto – zuerst wird der Bereich gewählt. */
    fun recognizeRecipePhoto() {
        val photoId = photoIdField.value ?: return
        if (isRecognizing) return
        ensureSession()
        startAreaSelection(listOf(PAGE_PHOTO + photoId))
    }

    /** Bilder aus „Teilen mit…“ (#46): gleich kopieren und mit „Bereich auswählen“ beginnen. */
    private fun takeSharedPhotos() {
        sharedTakenField.value = true
        val uris = sharedPhotos.take()
        if (uris.size > MAX_PAGES) message = R.string.share_too_many
        recognizePhotos(uris.take(MAX_PAGES))
    }

    /** Liest ausgewählte Fotos in der gewählten Reihenfolge. */
    fun recognizePhotos(uris: List<Uri>) {
        if (uris.isNotEmpty()) keepPages { session -> uris.map { pending.keepPage(session, it) } }
    }

    /** Liest ein gerade aufgenommenes Foto. */
    fun onRecognitionCameraResult(success: Boolean) {
        if (success) keepPages { session -> listOf(pending.keepCameraPage(session)) }
    }

    /**
     * Legt die Seiten sofort als eigene Dateien ab – die Leseerlaubnis der Fotoauswahl gilt nur vorübergehend –
     * und startet dann „Bereich auswählen“.
     */
    private fun keepPages(block: suspend (session: String) -> List<String>) {
        if (isRecognizing || isProcessingPhoto) return
        val session = ensureSession()
        viewModelScope.launch {
            isProcessingPhoto = true
            try {
                val names = block(session)
                startAreaSelection(names.map { PAGE_KEPT + it })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = R.string.area_load_error
            } finally {
                isProcessingPhoto = false
            }
        }
    }

    /** Alle Seiten dieses Formulars liegen unter einer Kennung; gelöscht wird beim Speichern oder Verwerfen. */
    private fun ensureSession(): String = sessionField.value ?: pending.newSession().also { sessionField.value = it }

    /** Ändert die Rahmen der aktuellen Seite. */
    private fun changeFrames(transform: (List<AreaFrame>) -> List<AreaFrame>) {
        val pages = areaPages()
        if (pages.isEmpty()) return
        val all = pageFrames(pages.size).toMutableList()
        val page = areaIndexField.value.coerceIn(0, pages.lastIndex)
        all[page] = transform(all[page]).ifEmpty { listOf(AreaFrame.WHOLE_PAGE) }
        areaListField.value = all.joinToString(";") { AreaFrame.encodeAll(it) }
    }

    fun changeFrameArea(index: Int, area: CropArea) = changeFrames { frames ->
        frames.mapIndexed { i, frame -> if (i == index) frame.copy(area = area) else frame }
    }

    fun selectFrame(index: Int) {
        areaFrameField.value = index
    }

    /** Was im gerade bearbeiteten Rahmen steht. */
    fun changeFrameKind(kind: AreaKind) {
        val active = areaSelection?.active ?: return
        changeFrames { frames -> frames.mapIndexed { i, frame -> if (i == active) frame.copy(kind = kind) else frame } }
    }

    /** „Bereich hinzufügen“: ein weiterer Rahmen, z. B. für die zweite Spalte. */
    fun addFrame() {
        val frames = areaSelection?.frames ?: return
        changeFrames { it + AreaFrame.next(it) }
        areaFrameField.value = frames.size
    }

    fun removeFrame() {
        val selection = areaSelection ?: return
        if (selection.frames.size <= 1) return
        changeFrames { frames -> frames.filterIndexed { i, _ -> i != selection.active } }
        areaFrameField.value = (selection.active - 1).coerceAtLeast(0)
    }

    /** „Ganze Seite“: wieder ein Rahmen „Alles“ über die ganze Seite. */
    fun resetArea() {
        changeFrames { listOf(AreaFrame.WHOLE_PAGE) }
        areaFrameField.value = 0
    }

    /** Gewählte Sprache des Textes; null = automatisch erkennen. */
    val languageChoice: String? get() = languageChoiceField.value

    fun chooseLanguage(language: String?) {
        languageChoiceField.value = language?.takeIf { it in TextLanguage.SUPPORTED }
    }

    /** Weiter zur nächsten Seite; nach der letzten startet die Texterkennung. */
    fun confirmArea() {
        val pages = areaPages()
        if (pages.isEmpty() || isRecognizing) return
        val index = areaIndexField.value
        if (index + 1 < pages.size) {
            areaIndexField.value = index + 1
            areaFrameField.value = 0
            loadAreaPreview()
            return
        }
        // Seiten und Bereiche bleiben gespeichert, bis das Ergebnis im Rezept steht.
        recognitionStartedField.value = true
        previewJob?.cancel()
        areaPreview = null
        startRecognition()
    }

    /** „Bereich auswählen“ abbrechen: Es wird nichts gelesen, die kopierten Seiten werden gelöscht. */
    fun cancelAreaSelection() = finishRun(read = false)

    /** „Erneut erkennen“ nach einer Unterbrechung: dieselben Seiten mit denselben Bereichen lesen. */
    fun retryRecognition() {
        if (recognitionInterrupted) startRecognition()
    }

    /** Eine unterbrochene Erkennung verwerfen; ihre Seiten werden gelöscht. */
    fun discardRecognition() = finishRun(read = false)

    private fun startAreaSelection(pages: List<String>) {
        areaPagesField.value = pages.joinToString("\n")
        areaListField.value = pages.joinToString(";") { AreaFrame.WHOLE_PAGE.encode() }
        areaIndexField.value = 0
        areaFrameField.value = 0
        recognitionStartedField.value = false
        loadAreaPreview()
    }

    /**
     * Beendet „Bereich auswählen“ bzw. eine Erkennung. Wurden die Seiten gelesen ([read]), kommen sie zu den
     * gelesenen Seiten, sonst werden die kopierten gelöscht. Im Hintergrund wird erst aufgeräumt, wenn die App
     * wieder sichtbar ist (siehe [cleanUpWhenVisible]).
     */
    private fun finishRun(read: Boolean) {
        previewJob?.cancel()
        val pages = areaPages()
        val frames = pageFrames(pages.size)
        if (read) {
            // Als Seite (und Originalseite) gilt, was alle Rahmen zusammen umfassen.
            val newPages = pages.mapIndexed { index, ref -> ReadPage(ref, boundsOf(frames[index].map { it.area })) }
            val kept = readPages().filter { old -> newPages.none { it.ref == old.ref } }
            readPagesField.value = (kept + newPages).joinToString("\n") { it.encode() }
        }
        areaPagesField.value = ""
        areaListField.value = ""
        areaIndexField.value = 0
        areaFrameField.value = 0
        recognitionStartedField.value = false
        recognitionInterrupted = false
        areaPreview = null
        areaPreviewFailed = false
        val session = sessionField.value ?: return
        val unread = if (read) emptyList() else pages.filter { it.startsWith(PAGE_KEPT) }.map { it.removePrefix(PAGE_KEPT) }
        val cleanUp: suspend () -> Unit = {
            runCatching { pending.deletePages(session, unread) }
            runCatching { pending.deleteResult(session) }
        }
        if (inBackground) cleanUpWhenVisible = cleanUp else viewModelScope.launch { cleanUp() }
    }

    private fun areaPages(): List<String> = areaPagesField.value.lines().filter { it.isNotEmpty() }

    /** Rahmen je Seite; fehlt etwas, die ganze Seite. */
    private fun pageFrames(count: Int): List<List<AreaFrame>> {
        val saved = areaListField.value.split(';').map { AreaFrame.decodeAll(it) }
        return List(count) { saved.getOrElse(it) { listOf(AreaFrame.WHOLE_PAGE) } }
    }

    private fun readPages(): List<ReadPage> = readPagesField.value.lines().filter { it.isNotEmpty() }.map { ReadPage.decode(it) }

    private fun storedPageIds(): List<String> = pageIdsField.value.lines().filter { it.isNotEmpty() }

    /** Adresse einer Seite: das Rezeptfoto oder eine kopierte Seite. */
    private fun pageUri(ref: String): Uri {
        val session = sessionField.value
        return when {
            ref.startsWith(PAGE_PHOTO) -> Uri.fromFile(photoStore.photoFile(ref.removePrefix(PAGE_PHOTO)))
            ref.startsWith(PAGE_KEPT) && session != null -> pending.pageUri(session, ref.removePrefix(PAGE_KEPT))
            else -> throw IOException("Seite nicht lesbar")
        }
    }

    private fun loadAreaPreview() {
        val page = areaPages().getOrNull(areaIndexField.value) ?: return
        previewJob?.cancel()
        areaPreview = null
        areaPreviewFailed = false
        previewJob = viewModelScope.launch {
            try {
                areaPreview = photoStore.loadPreview(pageUri(page))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                areaPreviewFailed = true
            }
        }
    }

    /**
     * Nach dem Beenden durch Android: Wurde die Erkennung im Hintergrund noch fertig, wird ihr Ergebnis
     * übernommen; sonst lässt sie sich mit „Erneut erkennen“ wiederholen.
     */
    private fun resumeRecognition() {
        val session = sessionField.value
        viewModelScope.launch {
            val saved = session?.let { runCatching { pending.loadResult(it) }.getOrNull() }
            if (saved != null) {
                applyResult(saved)
                finishRun(read = true)
            } else {
                recognitionInterrupted = true
            }
        }
    }

    /**
     * Liest die Seiten der Erkennung, jeweils nur die Rahmen. Ein Rahmen „Alles“ liest nicht noch einmal,
     * was schon in einem bezeichneten Rahmen derselben Seite liegt – so entsteht nichts doppelt.
     */
    private fun startRecognition() {
        val refs = areaPages()
        if (refs.isEmpty() || isRecognizing) return
        val frames = pageFrames(refs.size)
        val pages = refs.mapIndexed { index, ref ->
            val labeled = frames[index].filter { it.kind != AreaKind.ALL }.map { it.area }
            TextRecognizer.Page(
                load = { photoStore.loadForRecognition(pageUri(ref)) },
                areas = frames[index].map { frame ->
                    TextRecognizer.Area(frame.area, blank = if (frame.kind == AreaKind.ALL) labeled else emptyList())
                },
            )
        }
        runRecognition(pages)
    }

    /** Die laufende Erkennung; „Abbrechen“ wirkt auf sie, auch bevor das Lesen richtig begonnen hat (#43). */
    private var run: TextRecognizer.Run? = null

    fun cancelRecognition() {
        run?.cancel()
    }

    private fun runRecognition(pages: List<TextRecognizer.Page>) {
        if (isRecognizing) return
        recognitionInterrupted = false
        recognition = RecognitionState.Running(1, pages.size)
        val session = sessionField.value
        val current = TextRecognizer.Run().also { run = it }
        viewModelScope.launch {
            var read = false
            try {
                // Ein Ergebnis einer früheren Erkennung darf nicht für diese gehalten werden.
                session?.let { runCatching { pending.deleteResult(it) } }
                val preferred = preferences.recognitionLanguage()
                    ?: TextLanguage.supportedOrDefault(Locale.getDefault().language)
                val result = recognizer.recognize(current, pages, preferred, languageChoiceField.value) { page, percent ->
                    recognition = RecognitionState.Running(page + 1, pages.size, percent)
                }
                // Nur eine sicher erkannte oder gewählte Sprache gilt beim nächsten Mal als zuletzt genutzt.
                if (result.detected) preferences.setRecognitionLanguage(result.language)
                if (result.text.isBlank()) {
                    message = R.string.ocr_nothing
                } else {
                    // Im Hintergrund kann Android die App jederzeit beenden: Ergebnis zuerst als Datei sichern.
                    if (inBackground && session != null) runCatching { pending.saveResult(session, result) }
                    applyResult(result)
                    read = true
                }
            } catch (e: TextRecognizer.CancelledException) {
                // Abgebrochen: nichts ändern.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = R.string.ocr_error
            } finally {
                if (run === current) run = null
                recognition = RecognitionState.Idle
                finishRun(read)
            }
        }
    }

    private fun applyResult(result: TextRecognizer.Result) {
        applyRecognized(result)
        checkHintField.value = true
        loadSourceSuggestions()
        // Im Hintergrund wird ein neues Rezept gleich als Entwurf gesichert, wie beim Wechsel in den Hintergrund.
        if (inBackground) saveDraft()
    }

    /**
     * Übernimmt den erkannten Text in das Formular, nach den Regeln von [DraftMerge]. Jeder Teil des Ergebnisses
     * gehört zu einem Rahmen; stimmt die Anzahl nicht (Ergebnis einer älteren Version), gilt alles als „Alles“.
     */
    private fun applyRecognized(result: TextRecognizer.Result) {
        val text = result.text.trim()
        val kinds = pageFrames(areaPages().size).flatten().map { it.kind }
        val parts = if (kinds.size == result.parts.size) kinds.zip(result.parts) else listOf(AreaKind.ALL to text)
        val parsed = RecipeTextParser.parseParts(parts, result.language)
        // Die Seitenzahl der ersten gelesenen Seite wird zum Vorschlag für „Seite“ (#40).
        if (suggestedPageField.value.isEmpty()) parsed.pageNumber?.let { suggestedPageField.value = it }
        val merged = DraftMerge.merge(
            draft = DraftText(
                title = title,
                servings = servings,
                servingsUnit = servingsUnit,
                ingredients = ingredients,
                steps = steps,
                originalText = recognizedText,
                language = languageField.value,
            ),
            text = text,
            parsed = parsed,
            language = result.language.takeIf { result.detected },
        )
        if (merged.title != title) title = merged.title
        servingsField.value = merged.servings
        servingsUnitField.value = merged.servingsUnit
        ingredientsField.value = merged.ingredients
        stepsField.value = merged.steps
        recognizedTextField.value = merged.originalText
        languageField.value = merged.language
        dirtyField.value = true
    }

    /** Entfernt den gespeicherten erkannten Text, z. B. weil ein Bildschirmfoto fremde Namen enthielt. */
    fun clearRecognizedText() = change(recognizedTextField, "")

    /** Gespeicherte Originalseiten zuerst, dann die gelesenen; das Rezeptfoto nur, solange es noch dasselbe ist. */
    private fun viewerPages(): List<ViewerPage> =
        storedPageIds().map { ViewerPage.Stored(it) } +
            readPages().filter { !it.ref.startsWith(PAGE_PHOTO) || it.ref == PAGE_PHOTO + photoIdField.value }
                .map { ViewerPage.Read(it) }

    private suspend fun loadViewerPage(index: Int): Bitmap =
        when (val page = viewerPages().getOrNull(index) ?: throw IOException("Seite fehlt")) {
            is ViewerPage.Stored -> photoStore.loadPage(Uri.fromFile(photoStore.photoFile(page.photoId)), CropArea.WHOLE_PAGE)
            is ViewerPage.Read -> photoStore.loadPage(pageUri(page.page.ref), page.page.area)
        }

    fun showPages() = viewer.open(0)

    fun showPage(index: Int) {
        if (index in 0 until pageCount) viewer.open(index)
    }

    private fun viewerPage(): ViewerPage? = viewer.page?.let { viewerPages().getOrNull(it) }

    /** Die offene Seite ist eine Originalseite oder eine kopierte Seite – nicht das Rezeptfoto selbst. */
    val canChangeViewerPage: Boolean
        get() = when (val page = viewerPage()) {
            is ViewerPage.Stored -> true
            is ViewerPage.Read -> page.page.ref.startsWith(PAGE_KEPT)
            null -> false
        }

    /** Macht die offene Seite zum Rezeptfoto – z. B. wenn die Buchseite das Gericht zeigt. */
    fun useViewerPageAsPhoto() {
        val uri = when (val page = viewerPage()) {
            is ViewerPage.Stored -> Uri.fromFile(photoStore.photoFile(page.photoId))
            is ViewerPage.Read -> if (page.page.ref.startsWith(PAGE_KEPT)) pageUri(page.page.ref) else return
            null -> return
        }
        viewer.close()
        importPhoto { photoStore.importFromUri(uri) }
    }

    /** Nimmt die offene Seite heraus; gelöscht wird erst beim Speichern. */
    fun removeViewerPage() {
        val index = viewer.page ?: return
        when (val page = viewerPage()) {
            is ViewerPage.Stored -> change(pageIdsField, storedPageIds().filter { it != page.photoId }.joinToString("\n"))
            is ViewerPage.Read -> if (page.page.ref.startsWith(PAGE_KEPT)) {
                change(readPagesField, readPages().filter { it.ref != page.page.ref }.joinToString("\n") { it.encode() })
            } else {
                return
            }
            null -> return
        }
        if (pageCount == 0) viewer.close() else viewer.open(index.coerceAtMost(pageCount - 1))
    }

    override fun onCleared() {
        run?.cancel()
    }

    private companion object {
        const val PAGE_PHOTO = "photo:"
        const val PAGE_KEPT = "page:"
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

    /** „Speichern“: Gibt es gelesene Seiten, wird zuerst gefragt, ob sie als Originalseiten bleiben (#38). */
    fun requestSave(onSaved: (id: String, wasNew: Boolean) -> Unit) {
        if (title.isBlank()) {
            titleError = true
            return
        }
        if (readPages().any { it.ref.startsWith(PAGE_KEPT) }) {
            askKeepPagesField.value = true
        } else {
            save(keepPages = false, onSaved = onSaved)
        }
    }

    fun cancelKeepPages() {
        askKeepPagesField.value = false
    }

    /** @param keepPages die gelesenen Seiten werden als Originalseiten beim Rezept gespeichert */
    fun save(keepPages: Boolean, onSaved: (id: String, wasNew: Boolean) -> Unit) {
        askKeepPagesField.value = false
        if (title.isBlank()) {
            titleError = true
            return
        }
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            val newPages = mutableListOf<String>()
            try {
                if (keepPages) {
                    readPages().filter { it.ref.startsWith(PAGE_KEPT) }.forEach { page ->
                        newPages += photoStore.importPage(pageUri(page.ref), page.area)
                    }
                }
                val id = routeId ?: draftIdField.value ?: RecipeIds.newId()
                val base = repository.getRecipe(id)
                repository.save(buildRecipe(id, base, isDraft = false, newPages = newPages))
                val oldMainPhoto = originalPhotoIdField.value
                if (oldMainPhoto != null && oldMainPhoto != photoIdField.value) photoStore.delete(oldMainPhoto)
                originalPhotoIdField.value = photoIdField.value
                val removedPages = originalPageIdsField.value.lines().filter { it.isNotEmpty() && it !in storedPageIds() }
                removedPages.forEach { photoStore.delete(it) }
                deleteSession()
                dirtyField.value = false
                finished = true
                onSaved(id, isNew)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                newPages.forEach { runCatching { photoStore.delete(it) } }
                message = R.string.save_error
            } finally {
                isSaving = false
            }
        }
    }

    /** Verwirft alle Änderungen: neue Fotos, gelesene Seiten und ein automatisch gespeicherter Entwurf werden entfernt. */
    fun discard(onDone: () -> Unit) {
        finished = true
        viewModelScope.launch {
            val current = photoIdField.value
            if (current != null && current != originalPhotoIdField.value) photoStore.delete(current)
            draftIdField.value?.let { runCatching { repository.delete(it) } }
            deleteSession()
            onDone()
        }
    }

    /** Kopierte Seiten löschen, bevor das Formular geschlossen wird. */
    private suspend fun deleteSession() {
        sessionField.value?.let { runCatching { pending.delete(it) } }
    }

    /** App wieder sichtbar: Aufräumen nach einer Erkennung, die im Hintergrund zu Ende ging. */
    fun onStart() {
        inBackground = false
        val cleanUp = cleanUpWhenVisible ?: return
        cleanUpWhenVisible = null
        viewModelScope.launch { cleanUp() }
    }

    /** App geht in den Hintergrund: ein neues Rezept still als Entwurf sichern. */
    fun onStop() {
        inBackground = true
        saveDraft()
    }

    private fun saveDraft() {
        if (finished || !isNew || !isDirty || !hasContent()) return
        val id = draftIdField.value ?: return
        viewModelScope.launch {
            runCatching {
                val base = repository.getRecipe(id)
                repository.save(buildRecipe(id, base, isDraft = true))
            }
        }
    }

    private fun hasContent(): Boolean =
        listOf(title, ingredients, steps, source, sourcePage, notes, recognizedText).any { it.isNotBlank() } ||
            photoIdField.value != null

    private fun buildRecipe(id: String, base: Recipe?, isDraft: Boolean, newPages: List<String> = emptyList()): Recipe {
        val now = System.currentTimeMillis()
        val start = base ?: Recipe(
            id = id,
            title = "",
            language = Locale.getDefault().language,
            createdAt = now,
            updatedAt = now,
        )
        // Unveränderte Quelle behält ihre Details (z. B. die Art „Person“ aus einer Sicherung).
        val page = sourcePage.takeIf { !sourceIsLink }
        val parsedSource = if (
            base?.source != null &&
            RecipeText.formatSource(base.source) == source.trim() &&
            base.source.page.orEmpty() == page.orEmpty().trim()
        ) {
            base.source
        } else {
            RecipeText.parseSource(source, page)
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
            pageIds = storedPageIds() + newPages,
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
