package com.moltobene.app.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.ui.collection.AddChooser
import com.moltobene.app.ui.collection.RecipeListItem
import com.moltobene.app.ui.collection.RecipeRow
import com.moltobene.app.ui.components.AppTitle
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
    onOpenSettings: () -> Unit,
) {
    val count by viewModel.recipeCount.collectAsStateWithLifecycle()
    val suggestion by viewModel.suggestion.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            HomeTile(
                icon = R.drawable.ic_restaurant,
                title = stringResource(R.string.home_recipes_title),
                subtitle = count?.let { pluralStringResource(R.plurals.home_recipes_count, it, it) },
                onClick = onOpenCollection,
            )
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
                    SuggestionTile(
                        item = item,
                        onCook = { onOpenRecipe(item.id) },
                        onAnother = viewModel::suggestAnother,
                    )
                }
            }
            if (recent.isNotEmpty()) {
                RecentTile(items = recent, onOpenRecipe = onOpenRecipe)
            }
            HomeTile(
                icon = R.drawable.ic_restaurant,
                title = stringResource(R.string.basics_title),
                subtitle = stringResource(R.string.home_basics_text),
                onClick = onOpenBasics,
            )
            HomeTile(
                icon = R.drawable.ic_calculate,
                title = stringResource(R.string.dough_title),
                subtitle = stringResource(R.string.home_dough_text),
                onClick = onOpenDough,
            )
            // Platz unter der letzten Kachel, damit der Knopf „Hinzufügen“ nichts verdeckt.
            Spacer(Modifier.size(72.dp))
        }
    }
}

/** Eine Kachel: Symbol und Text nebeneinander, nie nur ein Symbol; mindestens 96 dp hoch. */
@Composable
private fun HomeTile(@DrawableRes icon: Int, title: String, subtitle: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
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

/** „Was koche ich heute?“: ein Rezept aus der eigenen Sammlung, zum Kochen oder zum Wechseln. */
@Composable
private fun SuggestionTile(item: RecipeListItem, onCook: () -> Unit, onAnother: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                stringResource(R.string.home_today_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.m).semantics { heading() },
            )
            RecipeRow(item = item, onClick = onCook)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
            ) {
                TextButton(onClick = onAnother) { Text(stringResource(R.string.home_today_another)) }
                Button(onClick = onCook) { Text(stringResource(R.string.home_today_cook)) }
            }
        }
    }
}

/** „Zuletzt geöffnet“: bis zu fünf Rezepte, das neueste zuerst. */
@Composable
private fun RecentTile(items: List<RecipeListItem>, onOpenRecipe: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.m)) {
            Text(
                stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.m).semantics { heading() },
            )
            for (item in items) RecipeRow(item = item, onClick = { onOpenRecipe(item.id) })
        }
    }
}
