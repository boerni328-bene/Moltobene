package com.moltobene.app.ui.recipe

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.AmountScaling
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.RecipeShareText
import com.moltobene.app.data.share.ShareLabels
import com.moltobene.app.data.translate.LanguagePack
import com.moltobene.app.data.translate.RecipeTranslations
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.DraftLabel
import com.moltobene.app.ui.components.PageViewer
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.components.RecipePhotoLarge
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()
    val checked by viewModel.checkedIngredients.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val shownServings by viewModel.shownServings.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.recipe_deleted)
    val context = LocalContext.current
    val resources = LocalResources.current

    KeepScreenOn()

    // Zurück aus den Einstellungen: Ist das Sprachpaket inzwischen da, wird gleich übersetzt.
    LifecycleResumeEffect(Unit) {
        viewModel.retryTranslation()
        onPauseOrDispose {}
    }

    viewModel.shareEvent?.let { event ->
        val chooserTitle = stringResource(R.string.share_recipe)
        val errorText = stringResource(R.string.share_error)
        LaunchedEffect(event) {
            val opened = event is ShareEvent.Ready && openShareMenu(context, event.share, chooserTitle)
            if (!opened) snackbarHostState.showSnackbar(errorText)
            viewModel.shareEvent = null
        }
    }

    // „Originalseiten ansehen“ als Vollbild.
    val pageCount = (state as? RecipeUiState.Content)?.recipe?.pageIds?.size ?: 0
    viewModel.viewer.page?.let { page ->
        if (pageCount > 0) {
            PageViewer(
                page = page.coerceAtMost(pageCount - 1),
                pageCount = pageCount,
                bitmap = viewModel.viewer.bitmap,
                failed = viewModel.viewer.failed,
                onPageChange = { if (it in 0 until pageCount) viewModel.viewer.open(it) },
                onClose = viewModel.viewer::close,
            )
            return
        }
    }
    viewModel.photoViewer.page?.let {
        PageViewer(
            page = 0,
            pageCount = 1,
            bitmap = viewModel.photoViewer.bitmap,
            failed = viewModel.photoViewer.failed,
            onPageChange = {},
            onClose = viewModel.photoViewer::close,
            title = stringResource(R.string.photo),
            errorText = stringResource(R.string.photo_load_error),
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Name der App und Umschalter „DE | EN“ bleiben beim Blättern oben stehen (Wunsch vom 08.10.2026).
                title = { AppTitle(style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val content = state as? RecipeUiState.Content
                    if (content != null && !deleting) {
                        val original = remember(content.recipe) { RecipeTranslations.languageOf(content.recipe) }
                        if (original != null && LanguagePack.targets(original).isNotEmpty()) {
                            LanguageSwitch(
                                original = original,
                                selected = translation.selectedLanguage(original),
                                onSelect = { viewModel.showLanguage(it, original) },
                            )
                        }
                        IconButton(onClick = { onEdit(viewModel.recipeId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_recipe))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_recipe)) },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    enabled = !viewModel.preparingShare,
                                    onClick = {
                                        menuOpen = false
                                        viewModel.share(asFile = false, labels = shareLabels(resources))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_as_file)) },
                                    enabled = !viewModel.preparingShare,
                                    onClick = {
                                        menuOpen = false
                                        viewModel.share(asFile = true, labels = shareLabels(resources))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cooking_reset)) },
                                    enabled = checked.isNotEmpty() || currentStep != null,
                                    onClick = {
                                        menuOpen = false
                                        viewModel.resetProgress()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_recipe)) },
                                    onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = state
        when {
            deleting -> CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            current is RecipeUiState.Content -> RecipeContent(
                recipe = current.recipe,
                photo = current.photo,
                cooking = CookingState(
                    checked = checked.toSet(),
                    currentStep = currentStep,
                    shownServings = shownServings,
                    onToggleIngredient = viewModel::toggleIngredient,
                    onToggleStep = viewModel::toggleCurrentStep,
                    onServingsChange = viewModel::setServings,
                ),
                translation = TranslationActions(
                    state = translation,
                    onShowLanguage = viewModel::showLanguage,
                    onRetry = viewModel::retryTranslation,
                    onOpenSettings = onOpenSettings,
                ),
                snackbarHostState = snackbarHostState,
                onShowPages = { viewModel.viewer.open(0) },
                onShowPhoto = { viewModel.photoViewer.open(0) },
                modifier = Modifier.padding(padding),
            )
            current is RecipeUiState.Loading ->
                CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            current is RecipeUiState.NotFound ->
                CenteredMessage(text = stringResource(R.string.recipe_not_found), modifier = Modifier.padding(padding))
            else -> CenteredMessage(text = stringResource(R.string.recipe_load_error), modifier = Modifier.padding(padding))
        }
    }

    val content = state as? RecipeUiState.Content
    if (confirmDelete && content != null) {
        val title = content.recipe.title.ifBlank { stringResource(R.string.untitled_recipe) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleting = true
                    viewModel.delete { onDeleted(deletedMessage) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Texte für den geteilten Rezepttext, in der Sprache der Oberfläche. */
private fun shareLabels(resources: Resources) = ShareLabels(
    untitled = resources.getString(R.string.untitled_recipe),
    ingredients = resources.getString(R.string.ingredients),
    instructions = resources.getString(R.string.instructions),
    servings = { count, unit ->
        if (unit == null) {
            resources.getQuantityString(R.plurals.servings_count, count, count)
        } else {
            resources.getString(R.string.servings_with_unit, count, unit)
        }
    },
    source = { resources.getString(R.string.share_source, it) },
    page = { resources.getString(R.string.source_page, it) },
    video = { resources.getString(R.string.share_video, it) },
)

/** Öffnet das Android-Teilen-Menü. Andere Apps dürfen nur die übergebene Datei lesen. */
private fun openShareMenu(context: Context, share: PreparedShare, chooserTitle: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = share.mimeType
        putExtra(Intent.EXTRA_SUBJECT, share.subject)
        share.text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        share.fileUri?.let { uri ->
            putExtra(Intent.EXTRA_STREAM, uri)
            // Text und Datei gemeinsam, wie Android es selbst macht: Manche Apps (z. B. WhatsApp)
            // lesen nur diesen Teil und übernehmen den Text sonst nicht.
            clipData = ClipData(share.subject, arrayOf(share.mimeType), ClipData.Item(share.text, null, null, uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return try {
        context.startActivity(Intent.createChooser(send, chooserTitle))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Beim Kochen soll der Bildschirm nicht ausgehen. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Was beim Kochen dazukommt (#56): Häkchen, aktueller Schritt und angezeigte Portionen. */
private class CookingState(
    val checked: Set<String>,
    val currentStep: String?,
    val shownServings: Int?,
    val onToggleIngredient: (Int, String) -> Unit,
    val onToggleStep: (Int, String) -> Unit,
    val onServingsChange: (Int?) -> Unit,
)

/** „Rezept übersetzen“ (#60): Zustand und Aktionen für den Umschalter „DE | EN“. */
private class TranslationActions(
    val state: TranslationState,
    val onShowLanguage: (language: String?, original: String?) -> Unit,
    val onRetry: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
private fun RecipeContent(
    recipe: Recipe,
    photo: File?,
    cooking: CookingState,
    translation: TranslationActions,
    snackbarHostState: SnackbarHostState,
    onShowPages: () -> Unit,
    onShowPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (photo != null) {
            RecipePhotoLarge(file = photo, onOpen = onShowPhoto, modifier = Modifier.fillMaxWidth())
        } else {
            RecipePhoto(file = null, placeholderIconSize = 40.dp, modifier = Modifier.fillMaxWidth().height(120.dp))
        }

        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (recipe.isDraft) DraftLabel()
            // Übersetzung (#60): Sie ersetzt nur die Anzeige; Abhaken und Portionen beziehen sich weiter aufs Original.
            val originalLanguage = remember(recipe) { RecipeTranslations.languageOf(recipe) }
            val translated = (translation.state as? TranslationState.Shown)?.translation
            val shownLanguage = translated?.language ?: originalLanguage
            val textLocale = remember(shownLanguage) { shownLanguage?.let { LocaleList(it) } }
            val title = translated?.title ?: recipe.title
            Text(
                text = localized(title.ifBlank { stringResource(R.string.untitled_recipe) }, textLocale),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (translated != null && recipe.title.isNotBlank() && recipe.title != title) {
                Text(
                    text = stringResource(R.string.translation_original_title, recipe.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (originalLanguage != null && LanguagePack.targets(originalLanguage).isNotEmpty()) {
                TranslationStatus(original = originalLanguage, actions = translation)
            }
            val originalServings = recipe.servings?.takeIf { it > 0 }
            val servings = originalServings?.let { cooking.shownServings ?: it }
            if (originalServings != null && servings != null) {
                ServingsControl(
                    original = originalServings,
                    shown = servings,
                    unit = (translated?.servingsUnit ?: recipe.servingsUnit)?.takeIf { it.isNotBlank() },
                    onChange = { cooking.onServingsChange(it.takeIf { value -> value != originalServings }) },
                )
            }
            RecipeTimes(recipe.prepMinutes, recipe.totalMinutes)

            if (recipe.ingredients.isNotEmpty()) {
                SectionTitle(stringResource(R.string.ingredients))
                val factor = if (originalServings != null && servings != null) servings.toDouble() / originalServings else 1.0
                val deviceLanguage = LocalConfiguration.current.locales[0].language
                // Mengen werden so geschrieben wie im Rezept bzw. in der Übersetzung; ohne Angabe gilt die erkannte Sprache.
                val language = remember(recipe.language, recipe.ingredients, deviceLanguage, translated) {
                    translated?.language
                        ?: recipe.language
                        ?: TextLanguage.detect(recipe.ingredients.joinToString("\n") { it.text })
                        ?: deviceLanguage
                }
                val texts = translated?.ingredients?.takeIf { it.size == recipe.ingredients.size } ?: recipe.ingredients.map { it.text }
                val lines = remember(texts, factor, language) {
                    texts.map { AmountScaling.scale(it, factor, language) }
                }
                recipe.ingredients.forEachIndexed { index, ingredient ->
                    if (ingredient.isHeading) {
                        Text(
                            text = localized(texts[index], textLocale),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                        )
                    } else {
                        IngredientRow(
                            parts = lines[index],
                            locale = textLocale,
                            checked = RecipeViewModel.progressKey(index, ingredient.text) in cooking.checked,
                            onToggle = { cooking.onToggleIngredient(index, ingredient.text) },
                        )
                    }
                }
            }

            if (recipe.steps.isNotEmpty()) {
                SectionTitle(stringResource(R.string.instructions))
                val steps = translated?.steps?.takeIf { it.size == recipe.steps.size } ?: recipe.steps
                recipe.steps.forEachIndexed { index, step ->
                    StepRow(
                        number = index + 1,
                        text = localized(steps[index], textLocale),
                        current = cooking.currentStep == RecipeViewModel.progressKey(index, step),
                        onClick = { cooking.onToggleStep(index, step) },
                    )
                }
            }

            if (recipe.notes.isNotBlank()) {
                SectionTitle(stringResource(R.string.notes))
                Text(recipe.notes, style = MaterialTheme.typography.bodyLarge)
            }

            recipe.source?.let { source ->
                SectionTitle(stringResource(R.string.source))
                val url = source.url
                if (url != null && RecipeText.isWebLink(url)) {
                    SourceLink(url = url, snackbarHostState = snackbarHostState)
                } else {
                    val resources = LocalResources.current
                    val text = listOfNotNull(
                        source.name,
                        source.page?.let { page -> RecipeText.formatPage(page) { resources.getString(R.string.source_page, it) } },
                    ).joinToString(", ")
                    if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Der Link zum Video, wenn er nicht schon als Quelle dasteht (z. B. bei einem Rezept nur aus dem Video).
            RecipeShareText.videoLink(recipe)?.let { video ->
                SectionTitle(stringResource(R.string.video))
                if (RecipeText.isWebLink(video)) {
                    SourceLink(url = video, snackbarHostState = snackbarHostState)
                } else {
                    Text(video, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Originalseiten (#38), z. B. die Kochbuchseite oder ein handgeschriebenes Rezept zum Nachlesen.
            if (recipe.pageIds.isNotEmpty()) {
                OutlinedButton(onClick = onShowPages, modifier = Modifier.padding(top = Spacing.m)) {
                    Icon(painterResource(R.drawable.ic_document_scanner), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.s))
                    Text(pluralStringResource(R.plurals.original_pages_show, recipe.pageIds.size, recipe.pageIds.size))
                }
            }
        }
    }
}

/** Portionen mit − und +; umgerechnet wird nur in der Anzeige (#56). */
@Composable
private fun ServingsControl(original: Int, shown: Int, unit: String?, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(shown - 1) }, enabled = shown > 1) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.servings_fewer))
        }
        Text(
            text = servingsText(shown, unit),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Der Screenreader sagt die neue Zahl an.
            modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = { onChange(shown + 1) }, enabled = shown < MAX_SERVINGS) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.servings_more))
        }
    }
    if (shown != original) {
        Text(
            text = stringResource(R.string.servings_scaled_hint, servingsText(original, unit)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onChange(original) }) { Text(stringResource(R.string.servings_reset)) }
    }
}

@Composable
private fun servingsText(servings: Int, unit: String?): String =
    if (unit == null) {
        pluralStringResource(R.plurals.servings_count, servings, servings)
    } else {
        stringResource(R.string.servings_with_unit, servings, unit)
    }

/**
 * Eine Zutat zum Abhaken: Die ganze Zeile ist antippbar, abgehakt heißt Häkchen und durchgestrichen.
 * Umgerechnete Mengen sind fett, damit sie nicht nur an der Farbe zu erkennen sind.
 */
@Composable
private fun IngredientRow(parts: List<AmountScaling.Part>, locale: LocaleList?, checked: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (checked) R.string.ingredient_checked else R.string.ingredient_unchecked)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { stateDescription = state },
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(localeList = locale)) {
                    parts.forEach { part ->
                        if (part.scaled) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                        } else {
                            append(part.text)
                        }
                    }
                }
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Ein Schritt; antippen markiert ihn als aktuellen Schritt – mit Symbol und Hintergrund, nicht nur über die Farbe. */
@Composable
private fun StepRow(number: Int, text: AnnotatedString, current: Boolean, onClick: () -> Unit) {
    val currentLabel = stringResource(R.string.step_current)
    val markLabel = stringResource(R.string.step_mark_current)
    val highlight = if (current) {
        Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(highlight)
            .clickable(onClickLabel = markLabel, onClick = onClick)
            .padding(Spacing.s)
            .semantics(mergeDescendants = true) { if (current) stateDescription = currentLabel },
    ) {
        val color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        if (current) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

private const val MAX_SERVINGS = 99

/** Text in der Sprache des Rezepts: Der Screenreader liest ihn mit der passenden Aussprache vor (#60). */
private fun localized(text: String, locale: LocaleList?): AnnotatedString =
    if (locale == null) AnnotatedString(text) else buildAnnotatedString { withStyle(SpanStyle(localeList = locale)) { append(text) } }

/** Name einer Sprache in der Sprache der Oberfläche, z. B. „Englisch“. */
@Composable
private fun languageName(code: String): String {
    val ui = LocalConfiguration.current.locales[0]
    return Locale.forLanguageTag(code).getDisplayLanguage(ui).replaceFirstChar { it.titlecase(ui) }
}

/** Die gezeigte bzw. gewünschte Sprache: das Original, eine Übersetzung oder eine, die gerade entsteht. */
private fun TranslationState.selectedLanguage(original: String): String = when (this) {
    TranslationState.Original -> original
    is TranslationState.Working -> language
    is TranslationState.Shown -> translation.language
    is TranslationState.NeedsPack -> language
    is TranslationState.Failed -> language
}

/**
 * Umschalter „DE | EN“ oben in der Leiste (#60): Abkürzungen statt Flaggen, die Originalsprache mit Sternchen markiert,
 * z. B. „DE*“ (Wunsch vom 08.10.2026). Die gewählte Sprache ist ausgefüllt und fett, nicht nur farbig; der Screenreader liest
 * den Namen der Sprache. Schmal gehalten, damit daneben „MOLTOBENE“, Bearbeiten und ⋮ Platz haben.
 */
@Composable
private fun LanguageSwitch(original: String, selected: String, onSelect: (String) -> Unit) {
    val languages = (listOf(original) + LanguagePack.targets(original)).sorted()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.selectableGroup(),
    ) {
        languages.forEach { code ->
            val isSelected = code == selected
            val name = languageName(code)
            val description = if (code == original) stringResource(R.string.translation_language_original, name) else name
            val shape = MaterialTheme.shapes.small
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(code) })
                    .semantics { contentDescription = description }
                    .widthIn(min = 40.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                        },
                    )
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            ) {
                Text(
                    text = if (code == original) {
                        stringResource(R.string.translation_code_original, code.uppercase(Locale.ROOT))
                    } else {
                        code.uppercase(Locale.ROOT)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Unter dem Titel: Fortschritt, Hinweis auf die maschinelle Übersetzung oder was zu tun ist (#60). */
@Composable
private fun TranslationStatus(original: String, actions: TranslationActions) {
    val state = actions.state
    when (state) {
        TranslationState.Original -> Unit
        is TranslationState.Working -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(
                    text = stringResource(R.string.translation_running, state.done, state.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { actions.onShowLanguage(null, original) }) { Text(stringResource(R.string.cancel)) }
            }
            LinearProgressIndicator(
                progress = { if (state.total > 0) state.done.toFloat() / state.total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is TranslationState.Shown -> Text(
            text = stringResource(R.string.translation_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        is TranslationState.NeedsPack -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.translation_needs_pack, Formatter.formatShortFileSize(context, LanguagePack.DOWNLOAD_BYTES)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(onClick = actions.onOpenSettings) { Text(stringResource(R.string.translation_open_settings)) }
        }
        is TranslationState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(if (state.lowMemory) R.string.translation_low_memory else R.string.translation_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(onClick = actions.onRetry) { Text(stringResource(R.string.translation_retry)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(top = Spacing.m)
            .semantics { heading() },
    )
}

/** Öffnet nur Links mit http/https im Browser. */
@Composable
private fun SourceLink(url: String, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorText = stringResource(R.string.source_link_error)
    TextButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(errorText) }
            }
        },
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(url) }
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Zeiten aus einer Übernahme (#55), im Format des Handys, z. B. „Gesamtzeit: 1 Std., 30 Min.“. */
@Composable
private fun RecipeTimes(prepMinutes: Int?, totalMinutes: Int?) {
    val locale = LocalConfiguration.current.locales[0]
    listOfNotNull(
        prepMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_prep to it },
        totalMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_total to it },
    ).forEach { (label, minutes) ->
        Text(
            text = stringResource(label, formatDuration(minutes, locale)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Dauer in Stunden und Minuten mit den Abkürzungen der Sprache, z. B. „1 Std., 30 Min.“ oder “1 hr, 30 min”. */
private fun formatDuration(minutes: Int, locale: Locale): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val parts = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (rest > 0 || hours == 0) add(Measure(rest, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(*parts.toTypedArray())
}
