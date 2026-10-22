package com.moltobene.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.moltobene.app.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.ui.collection.AddChooser
import com.moltobene.app.ui.collection.RecipeListItem
import com.moltobene.app.ui.collection.Thumbnail
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.schedule.formatMoment
import com.moltobene.app.ui.theme.Spacing

/**
 * Startseite (Wunsch des Projektinhabers vom 12.10.2026, #81): große Kacheln statt gleich der Rezeptliste. Zuerst
 * „Rezepte“ und „Teigrechner“, dazu „Was koche ich heute?“ und „Zuletzt geöffnet“; Länder, Saison und mitgelieferte Grundrezepte kommen, sobald es sie gibt – keine
 * leeren Kacheln. Hinzufügen und Einstellungen wie in der Sammlung.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenCollection: () -> Unit,
    onOpenDough: () -> Unit,
    onOpenBasics: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onAddFromPhoto: () -> Unit,
    onAddFromText: () -> Unit,
    onAddFromLink: () -> Unit,
    onAddFromFile: () -> Unit,
    onAddFromAudio: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val count by viewModel.recipeCount.collectAsStateWithLifecycle()
    val suggestion by viewModel.suggestion.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    var chooseAdd by rememberSaveable { mutableStateOf(false) }
    if (chooseAdd) {
        AddChooser(
            onWrite = {
                chooseAdd = false
                onAddRecipe()
            },
            onFromLink = {
                chooseAdd = false
                onAddFromLink()
            },
            onFromPhoto = {
                chooseAdd = false
                onAddFromPhoto()
            },
            onFromText = {
                chooseAdd = false
                onAddFromText()
            },
            onFromFile = {
                chooseAdd = false
                onAddFromFile()
            },
            onFromAudio = {
                chooseAdd = false
                onAddFromAudio()
            },
            onDough = {
                chooseAdd = false
                onOpenDough()
            },
            onDismiss = { chooseAdd = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppTitle(style = MaterialTheme.typography.titleLarge, heading = true) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_short)
            ExtendedFloatingActionButton(
                onClick = { chooseAdd = true },
                modifier = Modifier.semantics { contentDescription = addLabel },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_short)) },
            )
        },
    ) { padding ->
        // Bei Schrift 200 % werden die Kacheln länger als der Bildschirm; die Seite scrollt.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (running.isNotEmpty()) RunningTile(running, onOpenRecipe)
            if (count == 0) {
                // Noch keine Rezepte: statt eines Vorschlags der Weg zum ersten Rezept.
                HomeTile(
                    icon = R.drawable.ic_edit_note,
                    title = stringResource(R.string.home_today_title),
                    subtitle = stringResource(R.string.add_recipe),
                    onClick = { chooseAdd = true },
                )
            } else {
                suggestion?.let { item ->
                    SuggestionCard(item = item, onCook = { onOpenRecipe(item.id) }, onAnother = viewModel::suggestAnother)
                }
            }
            CollectionTile(
                count = count,
                recent = recent,
                onOpenCollection = onOpenCollection,
                onOpenRecipe = onOpenRecipe,
            )
            ToolTiles(dough = onOpenDough, basics = onOpenBasics)
            // Platz unter der letzten Kachel, damit der Knopf „Hinzufügen“ nichts verdeckt.
            Spacer(Modifier.size(72.dp))
        }
    }
}

/** Eine Kachel: Symbol und Text nebeneinander, nie nur ein Symbol; mindestens 72 dp hoch. */
@Composable
private fun HomeTile(@DrawableRes icon: Int, title: String, subtitle: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.fillMaxWidth().padding(Spacing.m),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * „Was koche ich heute?“ (#87): die größte Kachel, mit dem Vorschaubild des vorgeschlagenen Rezepts; ohne Foto die ruhige
 * Fläche mit dem Anfangsbuchstaben. Der Text steht unter dem Foto, nicht darauf, damit der Kontrast immer stimmt. Tippen
 * öffnet das Rezept, der Knopf zeigt ein anderes.
 */
@Composable
private fun SuggestionCard(item: RecipeListItem, onCook: () -> Unit, onAnother: () -> Unit) {
    val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
    Card(
        onClick = onCook,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Ohne Foto genügt ein flacher Streifen; mit Foto das volle Bild.
        Thumbnail(item, title, Modifier.fillMaxWidth().aspectRatio(if (item.thumbnail != null) 16f / 9f else 3f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.m, top = Spacing.xs, bottom = Spacing.xs, end = Spacing.xs),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_today_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onAnother) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.home_today_another))
            }
        }
    }
}

/**
 * „Sammlung“ mit der Zahl der Rezepte und, wenn es welche gibt, einer Reihe der zuletzt geöffneten Rezepte als
 * Vorschaubilder (höchstens fünf, feste Größe). Tippen auf die Kachel öffnet die Sammlung, auf ein Bild das Rezept.
 */
@Composable
private fun CollectionTile(
    count: Int?,
    recent: List<RecipeListItem>,
    onOpenCollection: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    Card(
        onClick = onOpenCollection,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Icon(painterResource(R.drawable.ic_grid_view), contentDescription = null, modifier = Modifier.size(32.dp))
                Column {
                    Text(stringResource(R.string.home_recipes_title), style = MaterialTheme.typography.titleLarge)
                    count?.let {
                        Text(pluralStringResource(R.plurals.home_recipes_count, it, it), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (recent.isNotEmpty()) {
                Text(
                    stringResource(R.string.home_recent_title),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    for (item in recent.take(5)) {
                        val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(onClickLabel = title) { onOpenRecipe(item.id) }
                                .semantics { contentDescription = title },
                        ) {
                            Thumbnail(item, title, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

/** Teigrechner und Grundrezepte als zwei schmale Kacheln nebeneinander, bei großer Schrift untereinander. */
@Composable
private fun ToolTiles(dough: () -> Unit, basics: () -> Unit) {
    val doughTitle = stringResource(R.string.dough_title)
    val doughText = stringResource(R.string.home_dough_text)
    val basicsTitle = stringResource(R.string.basics_title)
    val basicsText = stringResource(R.string.home_basics_text)
    if (isLargeText()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            ToolTile(R.drawable.ic_calculate, doughTitle, doughText, dough, Modifier.fillMaxWidth())
            ToolTile(R.drawable.ic_menu_book, basicsTitle, basicsText, basics, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.height(IntrinsicSize.Max)) {
            ToolTile(R.drawable.ic_calculate, doughTitle, doughText, dough, Modifier.weight(1f).fillMaxHeight())
            ToolTile(R.drawable.ic_menu_book, basicsTitle, basicsText, basics, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun ToolTile(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * „Laufende Teige“ (#71): je Teig der Titel, die aktuelle Phase und wann es weitergeht. Steht ganz oben, weil die Zeit
 * drängt, und fehlt, solange kein Teig läuft. Tippen öffnet das Rezept mit dem ganzen Zeitplan.
 */
@Composable
private fun RunningTile(items: List<RunningDough>, onOpenRecipe: (String) -> Unit) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.s)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
            ) {
                Icon(painterResource(R.drawable.ic_restaurant), contentDescription = null, modifier = Modifier.size(32.dp))
                Text(
                    stringResource(R.string.home_running_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRecipe(item.recipeId) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = Spacing.m, vertical = Spacing.s),
                ) {
                    Text(item.title.ifBlank { stringResource(R.string.untitled_recipe) }, style = MaterialTheme.typography.titleMedium)
                    if (item.phase == null) {
                        Text(stringResource(R.string.schedule_finished), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(stringResource(R.string.schedule_now, item.phase), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (item.earliest == item.latest) {
                                stringResource(R.string.schedule_next_at, formatMoment(context, item.earliest))
                            } else {
                                stringResource(
                                    R.string.schedule_next_range,
                                    formatMoment(context, item.earliest),
                                    formatMoment(context, item.latest),
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
