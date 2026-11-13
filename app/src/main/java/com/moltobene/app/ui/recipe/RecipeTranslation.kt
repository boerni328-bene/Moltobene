package com.moltobene.app.ui.recipe

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.translate.LanguagePack
import com.moltobene.app.data.translate.RecipeTranslations
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale

/** „Rezept übersetzen“ (#60): Zustand und Aktionen für den Umschalter „DE | EN“. */
internal class TranslationActions(
    val state: TranslationState,
    val onShowLanguage: (language: String?, original: String?) -> Unit,
    val onRetry: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/** Text in der Sprache des Rezepts: Der Screenreader liest ihn mit der passenden Aussprache vor (#60). */
internal fun localized(text: String, locale: LocaleList?): AnnotatedString =
    if (locale == null) AnnotatedString(text) else buildAnnotatedString { withStyle(SpanStyle(localeList = locale)) { append(text) } }

/** Name einer Sprache in der Sprache der Oberfläche, z. B. „Englisch“. */
@Composable
internal fun languageName(code: String): String {
    val ui = LocalConfiguration.current.locales[0]
    return Locale.forLanguageTag(code).getDisplayLanguage(ui).replaceFirstChar { it.titlecase(ui) }
}

/** Die gezeigte bzw. gewünschte Sprache: das Original, eine Übersetzung oder eine, die gerade entsteht. */
internal fun TranslationState.selectedLanguage(original: String): String = when (this) {
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
internal fun LanguageSwitch(original: String, selected: String, onSelect: (String) -> Unit) {
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

/** Fest unter der Leiste: Fortschritt, Hinweis auf die maschinelle Übersetzung oder was zu tun ist (#60). */
@Composable
internal fun TranslationStatus(original: String, actions: TranslationActions) {
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

/** Ob unter der Leiste der Übersetzungsbalken steht (Rezept mit möglicher Übersetzung, die gerade nicht das Original zeigt). */
internal fun translationBarShown(recipe: Recipe, state: TranslationState): Boolean {
    val original = RecipeTranslations.languageOf(recipe)
    return original != null && LanguagePack.targets(original).isNotEmpty() && state != TranslationState.Original
}
