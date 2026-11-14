package com.moltobene.app.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.PredefinedTags
import com.moltobene.app.data.TagCodec
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.ui.components.TagLabels
import com.moltobene.app.ui.components.RecipePhotoLarge
import com.moltobene.app.ui.theme.Spacing

/**
 * Sprache des Rezepts (#62): meist erkennt die App sie selbst; bei kurzen Rezepten hilft die Wahl. Die Sprachen
 * stehen mit ihrem eigenen Namen in der Liste („English“, „Italiano“), damit jeder seine Sprache findet.
 */
@Composable
internal fun RecipeLanguageField(language: String?, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = language?.let { nativeLanguageName(it) } ?: stringResource(R.string.ocr_language_auto)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.field_language, current), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Text(
                    text = stringResource(R.string.field_language_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.m, vertical = Spacing.s)
                        .semantics { heading() },
                )
                val codes = listOf<String?>(null) + TextLanguage.SUPPORTED + listOfNotNull(language?.takeIf { it !in TextLanguage.SUPPORTED })
                codes.forEach { code ->
                    val selected = code == language
                    DropdownMenuItem(
                        text = { Text(code?.let { nativeLanguageName(it) } ?: stringResource(R.string.ocr_language_auto)) },
                        leadingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = null) },
                        onClick = {
                            open = false
                            onChange(code)
                        },
                        modifier = Modifier.semantics { this.selected = selected },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.field_language_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
    }
}

/**
 * Quelle mit Seite (#40). Nach einer Texterkennung stehen darunter antippbare Vorschläge: zuletzt
 * genutzte Bücher und die erkannte Seitenzahl – ohne zusätzliches Fenster. Darunter der Link zum Video.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SourceSection(viewModel: EditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = viewModel.source,
                onValueChange = { viewModel.source = it },
                label = { Text(stringResource(R.string.source)) },
                supportingText = { Text(stringResource(R.string.field_source_hint)) },
                modifier = Modifier.weight(2f),
            )
            if (!viewModel.sourceIsLink) {
                OutlinedTextField(
                    value = viewModel.sourcePage,
                    onValueChange = { value -> if (value.length <= 9) viewModel.sourcePage = value },
                    label = { Text(stringResource(R.string.field_source_page)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val books = viewModel.sourceSuggestions
        val page = viewModel.pageSuggestion
        if (books.isNotEmpty() || page != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(
                    text = stringResource(R.string.source_suggestions),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                books.forEach { book ->
                    SuggestionChip(
                        onClick = { viewModel.takeSourceSuggestion(book) },
                        label = { Text(book, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                page?.let {
                    SuggestionChip(
                        onClick = viewModel::takePageSuggestion,
                        label = { Text(stringResource(R.string.source_page, it)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = viewModel.video,
            onValueChange = { viewModel.video = it },
            label = { Text(stringResource(R.string.video)) },
            supportingText = { Text(stringResource(R.string.field_video_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Links aus der Videobeschreibung, der wahrscheinlichste zuerst. Jeder Link zeigt, wohin er führt
 * (z. B. „example.org/rezepte/linsensuppe“), und übernimmt beim Antippen das Rezept von dieser Seite.
 */
@Composable
internal fun VideoLinks(links: List<String>, enabled: Boolean, onChoose: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = Spacing.s, top = Spacing.m, end = Spacing.s, bottom = Spacing.xs)) {
            Text(
                text = pluralStringResource(R.plurals.video_recipe_links, links.size, links.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
            links.forEach { link ->
                TextButton(onClick = { onChoose(link) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.s))
                    Text(
                        text = linkLabel(link),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Ein Link ohne „https://“ und „www.“, z. B. „example.org/rezepte/linsensuppe“. */
internal fun linkLabel(link: String): String =
    link.substringAfter("://").removePrefix("www.").trimEnd('/')

@Composable
internal fun PhotoSection(
    viewModel: EditViewModel,
    onChoose: () -> Unit,
    onTake: () -> Unit,
) {
    val photo = viewModel.photoFile
    val idle = !viewModel.isProcessingPhoto && !viewModel.isRecognizing
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        when {
            viewModel.isProcessingPhoto -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(Spacing.s))
                    Text(
                        text = stringResource(R.string.photo_processing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            photo != null -> RecipePhotoLarge(
                file = photo,
                onOpen = { viewModel.photoViewer.open(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedButton(onClick = onChoose, enabled = idle, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_image), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_choose))
            }
            OutlinedButton(onClick = onTake, enabled = idle, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_take))
            }
        }
        if (photo != null && idle) {
            TextButton(onClick = viewModel::removePhoto) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_remove))
            }
        }
    }
}

/**
 * Schlagwörter (#57): mitgelieferte Vorschläge zum Antippen und eigene Schlagwörter. Ein eigenes Schlagwort, das einem
 * Vorschlag entspricht, wird zu diesem. Eigene Eingaben werden nie übersetzt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(viewModel: EditViewModel) {
    val resources = LocalResources.current
    val labels = remember(resources) { TagLabels.all(resources) }
    var input by rememberSaveable { mutableStateOf("") }
    val selected = viewModel.tags
    val addInput = {
        viewModel.addCustomTag(input, labels)
        input = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.tags),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            for (key in PredefinedTags.KEYS) {
                val tag = PredefinedTags.tag(key)
                val on = selected.any { it.name == key }
                FilterChip(
                    selected = on,
                    onClick = { viewModel.toggleTag(tag) },
                    label = { Text(TagLabels.text(resources, tag)) },
                    leadingIcon = if (on) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        // Vorschläge der übernommenen Seite (Kategorie, Stichwörter): nie automatisch gesetzt, nur auf Antippen.
        val suggestions = viewModel.suggestedTags
        if (suggestions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tags_suggestions),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                for (text in suggestions) {
                    SuggestionChip(
                        onClick = { viewModel.addSuggestedTag(text) },
                        label = { Text(text) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
        val own = selected.filter { it.predefinedKey == null }
        if (own.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                for (tag in own) {
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeTag(tag) },
                        label = { Text(tag.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.tags_remove, tag.name),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= TagCodec.MAX_NAME) input = it },
                label = { Text(stringResource(R.string.tags_custom)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addInput() }),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = addInput, enabled = input.isNotBlank()) { Text(stringResource(R.string.tags_add)) }
        }
    }
}
