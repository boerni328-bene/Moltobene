package com.moltobene.app.ui.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.moltobene.app.R
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.theme.Spacing

/** Einstellungen → Info → „Open-Source-Lizenzen“. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(viewModel: LicensesViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when (val state = viewModel.state) {
            LicensesUiState.Loading ->
                CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            LicensesUiState.Error ->
                CenteredMessage(text = stringResource(R.string.licenses_error), modifier = Modifier.padding(padding))
            is LicensesUiState.Content -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.licenses_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Spacing.m),
                    )
                }
                items(LICENSE_GROUPS, key = { it.file }) { group ->
                    HorizontalDivider()
                    LicenseSection(group = group, text = state.texts[group.file].orEmpty())
                }
            }
        }
    }
}

@Composable
private fun LicenseSection(group: LicenseGroup, text: String) {
    var showText by rememberSaveable(group.file) { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            text = group.licenseName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        group.components.forEach { component ->
            Column {
                Text(component.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = component.holder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        group.note?.let { note ->
            Text(stringResource(note), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = { showText = !showText }) {
            Text(stringResource(if (showText) R.string.license_text_hide else R.string.license_text_show))
        }
        if (showText) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.m),
                )
            }
        }
    }
}
