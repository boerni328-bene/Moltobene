package com.moltobene.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.moltobene.app.R
import com.moltobene.app.data.speech.HeadsetAction
import com.moltobene.app.data.speech.HeadsetButton
import com.moltobene.app.data.speech.SpeechPreset
import com.moltobene.app.data.speech.SpeechSettings
import com.moltobene.app.data.speech.VoiceInfo
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.recipe.openSpeechSettings
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale

/** Sprachen, für die es beim Vorlesen eine Stimme zu wählen gibt (Oberflächensprachen der App). */
private val SPEECH_LANGUAGES = listOf("de", "en")

/**
 * „Vorlesen“ in den Einstellungen (#85): Stimme je Sprache aus den auf dem Handy installierten Stimmen ohne Internet,
 * Hörprobe, Tempo und Tonhöhe mit Voreinstellungen. Gemerkt auf dem Handy; die Sprachausgabe meldet weder Geschlecht
 * noch Charakter einer Stimme, deshalb gibt es dazu keine Auswahl.
 */
@Composable
internal fun SpeechSection(
    settings: SpeechSettings,
    onChange: ((SpeechSettings) -> SpeechSettings) -> Unit,
    loadVoices: suspend (String) -> List<VoiceInfo>?,
    onSample: (String, SpeechSettings) -> Unit,
    onStopSpeech: () -> Unit,
) {
    var choosing by rememberSaveable { mutableStateOf<String?>(null) }
    val uiLanguage = LocalConfiguration.current.locales[0].language.takeIf { it in SPEECH_LANGUAGES } ?: "en"

    GroupLabel(stringResource(R.string.speech_voice_title))
    SPEECH_LANGUAGES.forEach { language ->
        ListItem(
            headlineContent = { Text(stringResource(R.string.speech_voice_for, languageDisplayName(language))) },
            supportingContent = {
                Text(settings.voiceFor(language)?.let { stringResource(R.string.speech_voice_chosen, it) } ?: stringResource(R.string.speech_voice_auto))
            },
            modifier = Modifier.clickable { choosing = language },
        )
    }
    Text(
        stringResource(R.string.speech_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.m),
    )

    GroupLabel(stringResource(R.string.speech_tempo_pitch_title))
    Row(
        modifier = Modifier.padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        SpeechPreset.entries.forEach { preset ->
            FilterChip(
                selected = settings.preset() == preset,
                onClick = { onChange { it.withPreset(preset) } },
                label = { Text(stringResource(presetLabel(preset)), maxLines = 1) },
            )
        }
    }
    StepRow(
        label = stringResource(R.string.speech_tempo),
        value = settings.rate,
        canLess = settings.rate > SpeechSettings.RATE_MIN,
        canMore = settings.rate < SpeechSettings.RATE_MAX,
        onLess = { onChange { it.steppedRate(-1) } },
        onMore = { onChange { it.steppedRate(+1) } },
    )
    StepRow(
        label = stringResource(R.string.speech_pitch),
        value = settings.pitch,
        canLess = settings.pitch > SpeechSettings.PITCH_MIN,
        canMore = settings.pitch < SpeechSettings.PITCH_MAX,
        onLess = { onChange { it.steppedPitch(-1) } },
        onMore = { onChange { it.steppedPitch(+1) } },
    )
    OutlinedButton(
        onClick = { onSample(uiLanguage, settings) },
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Text(stringResource(R.string.speech_sample))
    }

    GroupLabel(stringResource(R.string.speech_headset_title))
    ListItem(
        headlineContent = { Text(stringResource(R.string.speech_headset_switch)) },
        supportingContent = { Text(stringResource(R.string.speech_headset_note)) },
        trailingContent = { Switch(checked = settings.headsetButtons, onCheckedChange = null) },
        modifier = Modifier.toggleable(
            value = settings.headsetButtons,
            role = Role.Switch,
            onValueChange = { on -> onChange { it.withHeadsetButtons(on) } },
        ),
    )
    if (settings.headsetButtons) {
        val mapping = settings.headsetMapping()
        HeadsetButton.entries.forEach { button ->
            HeadsetMappingRow(
                label = stringResource(headsetButtonLabel(button)),
                selected = mapping.actionFor(button),
                onSelect = { action -> onChange { it.withHeadsetAction(button, action) } },
            )
        }
    }

    choosing?.let { language ->
        VoiceDialog(
            language = language,
            settings = settings,
            loadVoices = loadVoices,
            onChoose = { name -> onChange { it.withVoice(language, name) } },
            onSample = { voice -> onSample(language, settings.withVoice(language, voice)) },
            onDismiss = {
                onStopSpeech()
                choosing = null
            },
        )
    }
}

@Composable
private fun headsetButtonLabel(button: HeadsetButton): Int = when (button) {
    HeadsetButton.PLAY_PAUSE -> R.string.speech_headset_button_play_pause
    HeadsetButton.NEXT -> R.string.speech_headset_button_next
    HeadsetButton.PREVIOUS -> R.string.speech_headset_button_previous
}

private fun headsetActionLabel(action: HeadsetAction): Int = when (action) {
    HeadsetAction.TOGGLE -> R.string.speech_headset_action_toggle
    HeadsetAction.NEXT -> R.string.speech_headset_action_next
    HeadsetAction.PREVIOUS -> R.string.speech_headset_action_previous
    HeadsetAction.NONE -> R.string.speech_headset_action_none
}

/** Eine Kopfhörertaste und die Wahl, was sie in der Kochansicht auslöst; die Wahl bricht bei großer Schrift um. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeadsetMappingRow(label: String, selected: HeadsetAction, onSelect: (HeadsetAction) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            HeadsetAction.entries.forEach { action ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = { Text(stringResource(headsetActionLabel(action))) },
                )
            }
        }
    }
}

private fun presetLabel(preset: SpeechPreset): Int = when (preset) {
    SpeechPreset.RELAXED -> R.string.speech_preset_relaxed
    SpeechPreset.NORMAL -> R.string.speech_preset_normal
    SpeechPreset.LIVELY -> R.string.speech_preset_lively
}

@Composable
private fun StepRow(
    label: String,
    value: Int,
    canLess: Boolean,
    canMore: Boolean,
    onLess: () -> Unit,
    onMore: () -> Unit,
) {
    val valueText = stringResource(R.string.speech_percent, value)
    val content: @Composable () -> Unit = {
        FilledTonalIconButton(onClick = onLess, enabled = canLess) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.speech_less, label))
        }
        Text(
            valueText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.s),
        )
        FilledTonalIconButton(onClick = onMore, enabled = canMore) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.speech_more, label))
        }
    }
    if (isLargeText()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) { content() }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs).semantics {
                stateDescription = valueText
            },
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            content()
        }
    }
}

/** Liste der Stimmen einer Sprache mit Hörprobe je Stimme; „Automatisch“ nimmt die beste passende. */
@Composable
private fun VoiceDialog(
    language: String,
    settings: SpeechSettings,
    loadVoices: suspend (String) -> List<VoiceInfo>?,
    onChoose: (String?) -> Unit,
    onSample: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // null = lädt noch; Fehler (keine Sprachausgabe) wird als eigener Zustand gezeigt.
    val loaded by produceState<VoicesLoaded>(VoicesLoaded.Loading, language) {
        value = loadVoices(language)?.let { VoicesLoaded.Ready(it) } ?: VoicesLoaded.Unavailable
    }
    val chosen = settings.voiceFor(language)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.speech_voice_for, languageDisplayName(language))) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                when (val state = loaded) {
                    VoicesLoaded.Loading -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.speech_voice_loading))
                    }
                    VoicesLoaded.Unavailable -> Text(stringResource(R.string.speech_voice_unavailable))
                    is VoicesLoaded.Ready -> {
                        if (state.voices.isEmpty()) {
                            Text(stringResource(R.string.speech_voice_none))
                            TextButton(onClick = { openSpeechSettings(context) }) { Text(stringResource(R.string.speech_voice_more)) }
                        } else {
                            Column(modifier = Modifier.selectableGroup()) {
                                VoiceRow(
                                    selected = chosen == null,
                                    title = stringResource(R.string.speech_voice_auto),
                                    detail = null,
                                    onSelect = { onChoose(null) },
                                    onSample = { onSample(null) },
                                )
                                state.voices.forEach { voice ->
                                    VoiceRow(
                                        selected = chosen == voice.name,
                                        title = regionLabel(voice),
                                        detail = voice.name,
                                        onSelect = { onChoose(voice.name) },
                                        onSample = { onSample(voice.name) },
                                    )
                                }
                            }
                            if (chosen != null && state.voices.none { it.name == chosen }) {
                                Text(
                                    stringResource(R.string.speech_voice_missing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { openSpeechSettings(context) }) { Text(stringResource(R.string.speech_voice_more)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private sealed interface VoicesLoaded {
    data object Loading : VoicesLoaded

    data object Unavailable : VoicesLoaded

    data class Ready(val voices: List<VoiceInfo>) : VoicesLoaded
}

@Composable
private fun VoiceRow(selected: Boolean, title: String, detail: String?, onSelect: () -> Unit, onSample: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.s, vertical = Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onSample) { Text(stringResource(R.string.speech_sample)) }
    }
}

/** „Deutsch (Österreich)“ in der Sprache der Oberfläche; die Region ersetzt „mit oder ohne Akzent“. */
@Composable
private fun regionLabel(voice: VoiceInfo): String {
    val ui = LocalConfiguration.current.locales[0]
    val locale = Locale.Builder().setLanguage(voice.language).setRegion(voice.country).build()
    return locale.getDisplayName(ui).replaceFirstChar { it.titlecase(ui) }
}

@Composable
private fun languageDisplayName(code: String): String {
    val ui = LocalConfiguration.current.locales[0]
    return Locale.forLanguageTag(code).getDisplayLanguage(ui).replaceFirstChar { it.titlecase(ui) }
}
