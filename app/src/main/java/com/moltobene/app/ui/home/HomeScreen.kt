package com.moltobene.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            TileGrid(
                count = count,
                running = running,
                onOpenCollection = onOpenCollection,
                onOpenDough = onOpenDough,
                onOpenBasics = onOpenBasics,
                onOpenRecipe = onOpenRecipe,
            )
            RecentRow(recent = recent, onOpenRecipe = onOpenRecipe)
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
 * „Was koche ich heute?“ (#87), Entwurf „Foto-Bühne“ vom 22.10.2026: das Foto läuft über die ganze Breite, Titel und Frage
 * stehen auf einem dunklen Verlauf am unteren Rand (weiße Schrift, Kontrast bleibt). Ohne Foto die ruhige Fläche mit dem
 * Anfangsbuchstaben und normaler Schrift darunter. Tippen öffnet das Rezept, der Knopf zeigt ein anderes.
 */
@Composable
private fun SuggestionCard(item: RecipeListItem, onCook: () -> Unit, onAnother: () -> Unit) {
    val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
    val hasPhoto = item.thumbnail != null
    Card(
        onClick = onCook,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasPhoto) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Thumbnail(item, title, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                        .padding(start = Spacing.m, end = 56.dp, top = Spacing.l, bottom = Spacing.s),
                ) {
                    Text(stringResource(R.string.home_today_title), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onAnother, modifier = Modifier.align(Alignment.BottomEnd)) {
                    Icon(
                        painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.home_today_another),
                        tint = Color.White,
                    )
                }
            }
        } else {
            Thumbnail(item, title, Modifier.fillMaxWidth().aspectRatio(3f))
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
}

/**
 * Vier kompakte Kacheln in zwei Reihen: Kochbuch (mit der Zahl der Rezepte) und Teigrechner, darunter Grundrezepte und,
 * solange ein Teig läuft, „Laufende Teige“. Bei großer Schrift untereinander.
 */
@Composable
private fun TileGrid(
    count: Int?,
    running: List<RunningDough>,
    onOpenCollection: () -> Unit,
    onOpenDough: () -> Unit,
    onOpenBasics: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val recipesText = count?.let { pluralStringResource(R.plurals.home_recipes_count, it, it) }
    val context = LocalContext.current
    val runningText = running.firstOrNull()?.let { first ->
        val name = first.title.ifBlank { stringResource(R.string.untitled_recipe) }
        val phase = if (first.phase == null) stringResource(R.string.schedule_finished) else stringResource(R.string.schedule_now, first.phase)
        val next = if (first.phase == null) "" else "\n" + if (first.earliest == first.latest) {
            stringResource(R.string.schedule_next_at, formatMoment(context, first.earliest))
        } else {
            stringResource(R.string.schedule_next_range, formatMoment(context, first.earliest), formatMoment(context, first.latest))
        }
        "$name\n$phase$next"
    }
    val tiles = buildList<@Composable (Modifier) -> Unit> {
        add { m -> CompactTile(R.drawable.ic_grid_view, stringResource(R.string.home_recipes_title), recipesText, onOpenCollection, m) }
        add { m -> CompactTile(R.drawable.ic_calculate, stringResource(R.string.dough_title), stringResource(R.string.home_dough_text), onOpenDough, m) }
        add { m -> CompactTile(R.drawable.ic_menu_book, stringResource(R.string.basics_title), stringResource(R.string.home_basics_text), onOpenBasics, m) }
        if (runningText != null) {
            add { m ->
                CompactTile(
                    R.drawable.ic_restaurant,
                    stringResource(R.string.home_running_title),
                    runningText,
                    { onOpenRecipe(running.first().recipeId) },
                    m,
                    tertiary = true,
                )
            }
        }
    }
    if (isLargeText()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            tiles.forEach { it(Modifier.fillMaxWidth()) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.height(IntrinsicSize.Max)) {
                    pair.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                    // Eine einzelne Kachel in der letzten Reihe bleibt halb breit, damit alle gleich aussehen.
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Kompakte Kachel: Symbol und Titel in einer Zeile, darunter der kurze Text; nie nur ein Symbol. */
@Composable
private fun CompactTile(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    tertiary: Boolean = false,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (tertiary) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (tertiary) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** „Zuletzt geöffnet“: Überschrift und eine Reihe Vorschaubilder (höchstens fünf, feste Größe); Tippen öffnet das Rezept. */
@Composable
private fun RecentRow(recent: List<RecipeListItem>, onOpenRecipe: (String) -> Unit) {
    if (recent.isEmpty()) return
    Text(
        stringResource(R.string.home_recent_title),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = Spacing.xs).semantics { heading() },
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
