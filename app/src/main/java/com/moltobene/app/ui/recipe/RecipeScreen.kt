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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.ShareLabels
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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.recipe_deleted)
    val context = LocalContext.current
    val resources = LocalResources.current

    KeepScreenOn()

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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state is RecipeUiState.Content && !deleting) {
                        IconButton(
                            onClick = { viewModel.share(asFile = false, labels = shareLabels(resources)) },
                            enabled = !viewModel.preparingShare,
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_recipe))
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
                                    text = { Text(stringResource(R.string.share_as_file)) },
                                    enabled = !viewModel.preparingShare,
                                    onClick = {
                                        menuOpen = false
                                        viewModel.share(asFile = true, labels = shareLabels(resources))
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

@Composable
private fun RecipeContent(
    recipe: Recipe,
    photo: File?,
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
            Text(
                text = recipe.title.ifBlank { stringResource(R.string.untitled_recipe) },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            recipe.servings?.let { servings ->
                val unit = recipe.servingsUnit?.takeIf { it.isNotBlank() }
                Text(
                    text = if (unit == null) {
                        pluralStringResource(R.plurals.servings_count, servings, servings)
                    } else {
                        stringResource(R.string.servings_with_unit, servings, unit)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RecipeTimes(recipe.prepMinutes, recipe.totalMinutes)

            if (recipe.ingredients.isNotEmpty()) {
                SectionTitle(stringResource(R.string.ingredients))
                recipe.ingredients.forEach { ingredient ->
                    if (ingredient.isHeading) {
                        Text(
                            text = ingredient.text,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Text(ingredient.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (recipe.steps.isNotEmpty()) {
                SectionTitle(stringResource(R.string.instructions))
                recipe.steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(min = 24.dp),
                        )
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
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
