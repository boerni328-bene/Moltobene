package com.moltobene.app.ui.collection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Surface
import com.moltobene.app.ui.components.AppTitle
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.CollectionSort
import com.moltobene.app.data.TagCount
import com.moltobene.app.ui.components.DraftLabel
import com.moltobene.app.ui.components.OptionButton
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.components.TagLabels
import com.moltobene.app.ui.recipe.formatDuration
import com.moltobene.app.ui.theme.Spacing

/** Suchfeld als ruhige Pille ohne Rahmen, daneben der Umschalter Raster/Liste. */
@Composable
internal fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    grid: Boolean,
    onGridChange: (Boolean) -> Unit,
    sort: CollectionSort,
    onSortChange: (CollectionSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.m, end = Spacing.xs, top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
        )
        SortMenu(sort = sort, onSortChange = onSortChange)
        IconButton(onClick = { onGridChange(!grid) }) {
            Icon(
                painterResource(if (grid) R.drawable.ic_view_list else R.drawable.ic_grid_view),
                contentDescription = stringResource(if (grid) R.string.view_as_list else R.string.view_as_grid),
            )
        }
    }
}

/**
 * Filter „Alle / Favoriten / Entwürfe“ und Schlagwörter (#57) in einer verschiebbaren Zeile; erscheint nur, wenn es
 * etwas zu filtern gibt. Bei großer Schrift steht statt der Schlagwort-Chips eine Schaltfläche „Schlagwort: Alle“ mit Liste.
 * Der aktive Filter ist am Text und am Häkchen zu erkennen, nicht nur an der Farbe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterRow(
    filter: CollectionFilter,
    hasFavorites: Boolean,
    hasDrafts: Boolean,
    tagCounts: List<TagCount>,
    selectedTags: Set<String>,
    onFilterChange: (CollectionFilter) -> Unit,
    onToggleTag: (String) -> Unit,
) {
    if (!hasFavorites && !hasDrafts && tagCounts.isEmpty() && filter == CollectionFilter.ALL) return
    val resources = LocalResources.current
    val large = LocalDensity.current.fontScale > 1.3f
    var chooseTags by rememberSaveable { mutableStateOf(false) }
    // Alle / Favoriten / Entwürfe als Reiter (Entwurf „Dichte Liste“, 22.10.2026), darunter die Schlagwörter als Chips.
    val options = buildList {
        add(CollectionFilter.ALL to R.string.filter_all)
        if (hasFavorites || filter == CollectionFilter.FAVORITES) add(CollectionFilter.FAVORITES to R.string.filter_favorites)
        if (hasDrafts || filter == CollectionFilter.DRAFTS) add(CollectionFilter.DRAFTS to R.string.filter_drafts)
    }
    if (options.size > 1) {
        ScrollableTabRow(
            selectedTabIndex = options.indexOfFirst { it.first == filter }.coerceAtLeast(0),
            edgePadding = Spacing.m,
            containerColor = Color.Transparent,
        ) {
            for ((value, label) in options) {
                Tab(selected = filter == value, onClick = { onFilterChange(value) }, text = { Text(stringResource(label)) })
            }
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        if (large) {
            if (tagCounts.isNotEmpty()) {
                val label = when (selectedTags.size) {
                    0 -> stringResource(R.string.tag_filter_all)
                    1 -> stringResource(R.string.tag_filter_one, tagLabel(resources, tagCounts, selectedTags.first()))
                    else -> stringResource(R.string.tag_filter_many, selectedTags.size)
                }
                FilterChip(selected = selectedTags.isNotEmpty(), onClick = { chooseTags = true }, label = { Text(label) })
            }
        } else {
            for (entry in tagCounts) {
                val on = entry.tag.name in selectedTags
                FilterChip(
                    selected = on,
                    onClick = { onToggleTag(entry.tag.name) },
                    label = { Text(stringResource(R.string.tag_filter_chip, TagLabels.text(resources, entry.tag), entry.count)) },
                    leadingIcon = if (on) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
    if (chooseTags) {
        AlertDialog(
            onDismissRequest = { chooseTags = false },
            title = { Text(stringResource(R.string.tag_filter_title), modifier = Modifier.semantics { heading() }) },
            text = {
                LazyColumn {
                    items(tagCounts, key = { it.tag.name }) { entry ->
                        val on = entry.tag.name in selectedTags
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggleTag(entry.tag.name) }),
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Text(stringResource(R.string.tag_filter_chip, TagLabels.text(resources, entry.tag), entry.count))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chooseTags = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

private fun tagLabel(resources: Resources, counts: List<TagCount>, name: String): String =
    counts.firstOrNull { it.tag.name == name }?.let { TagLabels.text(resources, it.tag) } ?: name

@Composable
internal fun RecipeGrid(items: List<RecipeListItem>, onOpenRecipe: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.m, end = Spacing.m, top = Spacing.s, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        items(items, key = { it.id }) { item -> RecipeCard(item = item, onClick = { onOpenRecipe(item.id) }) }
    }
}

@Composable
private fun RecipeCard(item: RecipeListItem, onClick: () -> Unit) {
    val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Box {
            Thumbnail(item, title, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
            if (item.favorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(Spacing.s))
            if (item.isDraft) DraftLabel(Modifier.align(Alignment.TopStart).padding(Spacing.s))
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FavoriteBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(28.dp).clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = stringResource(R.string.favorite_label),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Foto des Gerichts oder, ohne Foto, eine ruhige Fläche mit dem Anfangsbuchstaben. */
@Composable
internal fun Thumbnail(item: RecipeListItem, title: String, modifier: Modifier) {
    if (item.thumbnail != null) {
        RecipePhoto(file = item.thumbnail, placeholderIconSize = 28.dp, modifier = modifier)
    } else {
        val scheme = MaterialTheme.colorScheme
        val (container, content) = when (item.id.hashCode().mod(3)) {
            0 -> scheme.primaryContainer to scheme.onPrimaryContainer
            1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
            else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        }
        Box(modifier.background(container), contentAlignment = Alignment.Center) {
            Text(
                text = title.take(1).uppercase(LocalConfiguration.current.locales[0]),
                style = MaterialTheme.typography.displaySmall,
                color = content,
            )
        }
    }
}

@Composable
internal fun RecipeList(items: List<RecipeListItem>, onOpenRecipe: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.xs, bottom = 96.dp),
    ) {
        items(items, key = { it.id }) { item ->
            RecipeRow(item = item, onClick = { onOpenRecipe(item.id) })
            HorizontalDivider(modifier = Modifier.padding(start = Spacing.m + 56.dp + Spacing.m))
        }
    }
}

@Composable
internal fun RecipeRow(item: RecipeListItem, onClick: () -> Unit) {
    val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Thumbnail(item, title, Modifier.size(56.dp).clip(MaterialTheme.shapes.medium))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Zeit und Portionen in einer Zeile (Entwurf „Dichte Liste“, 22.10.2026); fehlt beides, bleibt die Zeile weg.
            val locale = LocalConfiguration.current.locales[0]
            val meta = listOfNotNull(
                item.totalMinutes?.takeIf { it > 0 }?.let { formatDuration(it, locale) },
                item.servings?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.servings_count, it, it) },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(text = meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.isDraft) DraftLabel()
        }
        if (item.favorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.favorite_label),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * „Hinzufügen“ (Entwurf „Eigene Seite“ vom 22.10.2026): eine Seite in Gruppen – Selbst schreiben, Aus einer Quelle
 * übernehmen, Sprache (Beta) und der Teigrechner. Jeder Weg hat unter dem Namen eine kurze Zeile, was dahintersteckt.
 * Die Seite deckt den Bildschirm; Schließen oder Zurück führt zurück.
 */
@Composable
internal fun AddChooser(
    onWrite: () -> Unit,
    onFromLink: () -> Unit,
    onFromPhoto: () -> Unit,
    onFromText: () -> Unit,
    onFromFile: () -> Unit,
    onFromAudio: () -> Unit,
    onFromSpeech: () -> Unit,
    onDough: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                    AppTitle(style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    stringResource(R.string.add_short),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).semantics { heading() },
                )
                AddGroup(R.string.add_group_own)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                ) {
                    AddOption(R.drawable.ic_edit_note, R.string.add_recipe, R.string.add_hint_write, onWrite)
                }
                AddGroup(R.string.add_group_import)
                // Reihenfolge wie in der Vision: Internetseiten zuerst.
                AddOption(R.drawable.ic_link, R.string.import_from_link, R.string.add_hint_link, onFromLink)
                AddOption(R.drawable.ic_document_scanner, R.string.import_from_photo, R.string.add_hint_photo, onFromPhoto)
                AddOption(R.drawable.ic_content_paste, R.string.import_from_text, R.string.add_hint_text, onFromText)
                AddOption(R.drawable.ic_description, R.string.import_from_file, R.string.add_hint_file, onFromFile)
                AddGroup(R.string.add_group_voice)
                AddOption(R.drawable.ic_graphic_eq, R.string.import_from_audio, R.string.add_hint_audio, onFromAudio)
                AddOption(R.drawable.ic_mic, R.string.add_record, R.string.add_hint_record, onFromSpeech)
                AddGroup(R.string.add_group_tools)
                AddOption(R.drawable.ic_calculate, R.string.dough_title, R.string.add_hint_dough, onDough)
                Spacer(Modifier.size(Spacing.l))
            }
        }
    }
}

@Composable
private fun AddGroup(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.m, bottom = Spacing.xs).semantics { heading() },
    )
}

/** Eine Zeile: Symbol, Name und darunter ein kurzer Hinweis; mindestens 56 dp hoch. */
@Composable
private fun AddOption(@DrawableRes icon: Int, @StringRes title: Int, @StringRes hint: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Knopf mit Menü „Sortieren“: Titel A–Z oder zuletzt hinzugefügt; die gewählte Reihenfolge trägt ein Häkchen (#57). */
@Composable
private fun SortMenu(sort: CollectionSort, onSortChange: (CollectionSort) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.sort_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in CollectionSort.entries) {
                val selected = option == sort
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(if (option == CollectionSort.TITLE) R.string.sort_title else R.string.sort_recent),
                            fontWeight = if (selected) FontWeight.Bold else null,
                        )
                    },
                    leadingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = null) },
                    modifier = Modifier.semantics { this.selected = selected },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                )
            }
        }
    }
}
