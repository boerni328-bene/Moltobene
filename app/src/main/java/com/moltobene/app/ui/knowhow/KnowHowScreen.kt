package com.moltobene.app.ui.knowhow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.knowhow.KnowHow
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.ScreenTitle
import com.moltobene.app.ui.theme.Spacing

/**
 * Teigwissen (#84): Artikel zum Nachschlagen. Antippen klappt einen Artikel auf; Überschriften sind für den
 * Screenreader als solche gekennzeichnet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowHowScreen(viewModel: KnowHowViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppTitle(style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            KnowHowUiState.Loading -> CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            KnowHowUiState.Error -> CenteredMessage(text = stringResource(R.string.knowhow_error), modifier = Modifier.padding(padding))
            is KnowHowUiState.Content -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(bottom = Spacing.s)) {
                        ScreenTitle(stringResource(R.string.knowhow_title))
                        Text(
                            stringResource(R.string.knowhow_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(current.articles, key = { it.id }) { article ->
                    ArticleCard(
                        text = KnowHow.text(article, language),
                        expanded = expanded == article.id,
                        onToggle = { expanded = if (expanded == article.id) null else article.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(text: KnowHow.Text, expanded: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.basics_expanded else R.string.basics_collapsed)
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = stateText },
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(text.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(text.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                for (section in text.sections) {
                    Text(
                        section.heading,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                    )
                    for (paragraph in section.paragraphs) {
                        Text(paragraph, style = MaterialTheme.typography.bodyLarge)
                    }
                    for (row in section.rows) {
                        Column {
                            Text(row.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(row.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
