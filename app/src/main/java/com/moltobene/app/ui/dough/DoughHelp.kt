package com.moltobene.app.ui.dough

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.moltobene.app.R
import com.moltobene.app.ui.theme.Spacing

/**
 * Hinweise „Was bewirkt das?“ im Teigrechner (Teigwissen, #84, Etappe 1): je Thema drei kurze Teile – Wirkung, Üblich,
 * Gut zu wissen. Die Texte stehen einzeln in beiden Sprachen in strings.xml; alle Zahlen sind Richtwerte
 * (Quellenliste: `docs/recherche/2026-10-13-teigwissen-hinweise.md`).
 */
internal enum class HelpTopic(
    @StringRes val title: Int,
    @StringRes val effect: Int,
    @StringRes val range: Int,
    @StringRes val care: Int,
) {
    WATER(R.string.dough_help_water, R.string.dough_help_water_effect, R.string.dough_help_water_range, R.string.dough_help_water_care),
    SALT(R.string.dough_help_salt, R.string.dough_help_salt_effect, R.string.dough_help_salt_range, R.string.dough_help_salt_care),
    OIL(R.string.dough_help_oil, R.string.dough_help_oil_effect, R.string.dough_help_oil_range, R.string.dough_help_oil_care),
    YEAST(R.string.dough_help_yeast, R.string.dough_help_yeast_effect, R.string.dough_help_yeast_range, R.string.dough_help_yeast_care),
    BIGA(R.string.dough_pre_biga, R.string.dough_help_biga_effect, R.string.dough_help_biga_range, R.string.dough_help_biga_care),
    POOLISH(R.string.dough_pre_poolish, R.string.dough_help_poolish_effect, R.string.dough_help_poolish_range, R.string.dough_help_poolish_care),
    SOURDOUGH(R.string.dough_pre_sourdough, R.string.dough_help_sourdough_effect, R.string.dough_help_sourdough_range, R.string.dough_help_sourdough_care),
    FRESH_YEAST(R.string.dough_yeast_fresh, R.string.dough_help_fresh_effect, R.string.dough_help_fresh_range, R.string.dough_help_fresh_care),
    INSTANT_YEAST(R.string.dough_yeast_instant, R.string.dough_help_instant_effect, R.string.dough_help_instant_range, R.string.dough_help_instant_care),
    ACTIVE_YEAST(R.string.dough_yeast_active, R.string.dough_help_active_effect, R.string.dough_help_active_range, R.string.dough_help_active_care),
    PREFERMENT(
        R.string.dough_help_preferment, R.string.dough_help_preferment_effect, R.string.dough_help_preferment_range,
        R.string.dough_help_preferment_care,
    ),
}

/** Die Themen, die eine Karte des Formulars erklärt; [key] wird beim Drehen des Handys gemerkt. */
internal fun helpTopics(key: String?): List<HelpTopic> = when (key) {
    "all" -> listOf(HelpTopic.WATER, HelpTopic.SALT, HelpTopic.OIL, HelpTopic.YEAST, HelpTopic.PREFERMENT)
    "dough" -> listOf(HelpTopic.WATER, HelpTopic.SALT, HelpTopic.OIL)
    "yeast" -> listOf(HelpTopic.YEAST)
    "preferment" -> listOf(HelpTopic.PREFERMENT)
    "biga" -> listOf(HelpTopic.BIGA)
    "poolish" -> listOf(HelpTopic.POOLISH)
    "sourdough" -> listOf(HelpTopic.SOURDOUGH)
    "fresh" -> listOf(HelpTopic.FRESH_YEAST)
    "instant_dry" -> listOf(HelpTopic.INSTANT_YEAST)
    "active_dry" -> listOf(HelpTopic.ACTIVE_YEAST)
    else -> emptyList()
}

/** Ein Fenster mit den Hinweisen zu [topics]; scrollt bei großer Schrift. */
@Composable
internal fun DoughHelpDialog(topics: List<HelpTopic>, onDismiss: () -> Unit) {
    if (topics.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        // Leicht durchsichtig (Wunsch des Projektinhabers vom 22.10.2026); der Text bleibt gut lesbar.
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        title = { Text(stringResource(R.string.dough_help_title), modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                for (topic in topics) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            stringResource(topic.title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        HelpPart(R.string.dough_help_effect, topic.effect)
                        HelpPart(R.string.dough_help_range, topic.range)
                        HelpPart(R.string.dough_help_care, topic.care)
                    }
                }
                Text(
                    stringResource(R.string.dough_help_values_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dough_help_close)) } },
    )
}

@Composable
private fun HelpPart(@StringRes label: Int, @StringRes body: Int) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}
