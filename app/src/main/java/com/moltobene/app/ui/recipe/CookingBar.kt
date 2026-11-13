package com.moltobene.app.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.theme.Spacing

/** Alles, was die Kochleiste zeigt und tut; unten als [CookingBar], im niedrigen Querformat oben als [CookingBarTop]. */
internal class CookingBarData(
    val hasSteps: Boolean,
    val speaking: Boolean,
    val stepNumber: Int?,
    val stepCount: Int,
    val message: String?,
    val voiceHint: String?,
    val microphoneOn: Boolean,
    val onMicrophone: () -> Unit,
    val onPrevious: () -> Unit,
    val onRead: () -> Unit,
    val onNext: () -> Unit,
)

/**
 * Übergabe der Kochleiste an die obere Leiste: Nur diese liest [data]. Eine Änderung baut deshalb nur die Leiste neu
 * auf, nicht das Rezept.
 */
internal class CookingBarHolder {
    var data by mutableStateOf<CookingBarData?>(null)
}

/** Status der Kochleiste als Text: eine Kurzmeldung, sonst „Schritt 2 von 5“. */
@Composable
internal fun CookingBarData.statusText(): String? =
    message ?: stepNumber?.let { stringResource(R.string.cook_step_of, it, stepCount) }

/** Kochleiste in der oberen Leiste (Handy quer): „Zurück“, „Weiter“, Mikrofon und Vorlesen als Symbole. */
@Composable
internal fun CookingBarTop(bar: CookingBarData) {
    val microphoneIcon = if (bar.microphoneOn) R.drawable.ic_mic_off else R.drawable.ic_mic
    val microphoneDescription = stringResource(
        if (bar.microphoneOn) R.string.cook_bar_mic_off_description else R.string.cook_bar_mic_on_description,
    )
    val readIcon = if (bar.speaking) R.drawable.ic_stop else R.drawable.ic_volume_up
    val readDescription = stringResource(if (bar.speaking) R.string.cook_bar_stop_description else R.string.cook_bar_read_description)
    FilledTonalIconButton(onClick = bar.onPrevious, enabled = bar.hasSteps) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cook_bar_previous_description))
    }
    FilledIconButton(onClick = bar.onNext, enabled = bar.hasSteps) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cook_bar_next_description))
    }
    val micModifier = Modifier.semantics { contentDescription = microphoneDescription }
    if (bar.microphoneOn) {
        FilledIconButton(onClick = bar.onMicrophone, modifier = micModifier) {
            Icon(painterResource(microphoneIcon), contentDescription = null)
        }
    } else {
        FilledTonalIconButton(onClick = bar.onMicrophone, modifier = micModifier) {
            Icon(painterResource(microphoneIcon), contentDescription = null)
        }
    }
    FilledTonalIconButton(onClick = bar.onRead, enabled = bar.hasSteps) {
        Icon(painterResource(readIcon), contentDescription = readDescription)
    }
}

/**
 * Kochleiste (#79, #89): „Zurück“ und „Weiter“ nebeneinander – „Weiter“ ist der häufigste Knopf, deshalb betont –, davon
 * getrennt ganz rechts das Mikrofon (Spracheingabe, #80) und „Vorlesen“ als Lautsprecher-Symbol (beim Vorlesen als „Anhalten“-Symbol, Wunsch vom 14.10.2026). Darüber steht, welcher Schritt gerade dran ist
 * („Schritt 2 von 5“, als Text, nicht nur Farbe). Mindestens 64 dp hoch. Bei großer Schrift stehen „Zurück“ und „Weiter“
 * oben, „Vorlesen“ darunter rechts, damit kein Wort mitten im Wort umbricht. Das Vorlesen startet nur auf Antippen.
 */
@Composable
internal fun CookingBar(bar: CookingBarData) {
    val hasSteps = bar.hasSteps
    val speaking = bar.speaking
    val stepNumber = bar.stepNumber
    val stepCount = bar.stepCount
    val message = bar.message
    val voiceHint = bar.voiceHint
    val microphoneOn = bar.microphoneOn
    val onMicrophone = bar.onMicrophone
    val onPrevious = bar.onPrevious
    val onRead = bar.onRead
    val onNext = bar.onNext
    val previous = stringResource(R.string.cook_bar_previous)
    val previousDescription = stringResource(R.string.cook_bar_previous_description)
    val readIcon = if (speaking) R.drawable.ic_stop else R.drawable.ic_volume_up
    val readDescription = stringResource(if (speaking) R.string.cook_bar_stop_description else R.string.cook_bar_read_description)
    val next = stringResource(R.string.cook_bar_next)
    val nextDescription = stringResource(R.string.cook_bar_next_description)
    val microphoneIcon = if (microphoneOn) R.drawable.ic_mic_off else R.drawable.ic_mic
    val microphoneDescription = stringResource(if (microphoneOn) R.string.cook_bar_mic_off_description else R.string.cook_bar_mic_on_description)
    val compact = isLargeText()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            val status = message ?: stepNumber?.let { stringResource(R.string.cook_step_of, it, stepCount) }
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                )
            }
            if (voiceHint != null) {
                Text(
                    text = voiceHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                )
            }
            if (compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    BarButton(previous, previousDescription, hasSteps, onPrevious)
                    BarButton(next, nextDescription, hasSteps, onNext, primary = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Spacer(Modifier.weight(1f))
                    BarIconButton(microphoneIcon, microphoneDescription, true, onMicrophone, active = microphoneOn)
                    BarIconButton(readIcon, readDescription, hasSteps, onRead)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    BarButton(previous, previousDescription, hasSteps, onPrevious)
                    BarButton(next, nextDescription, hasSteps, onNext, primary = true)
                    // Lücke, damit „Vorlesen“ getrennt von den beiden Schritt-Knöpfen ganz rechts steht.
                    Spacer(Modifier.width(Spacing.m))
                    BarIconButton(microphoneIcon, microphoneDescription, true, onMicrophone, active = microphoneOn)
                    BarIconButton(readIcon, readDescription, hasSteps, onRead)
                }
            }
        }
    }
}

@Composable
internal fun RowScope.BarButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit, primary: Boolean = false) {
    val modifier = Modifier
        .weight(1f)
        .heightIn(min = 64.dp)
        .semantics { contentDescription = description }
    val padding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.xs)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = padding) {
            Text(text, textAlign = TextAlign.Center)
        }
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = padding) {
            Text(text, textAlign = TextAlign.Center)
        }
    }
}

/** Knopf nur mit Symbol (z. B. „Vorlesen“ als Lautsprecher); die Beschreibung für den Screenreader steht in strings.xml. */
@Composable
internal fun BarIconButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit, active: Boolean = false) {
    val modifier = Modifier.width(64.dp).heightIn(min = 64.dp).semantics { contentDescription = description }
    val content: @Composable () -> Unit = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp)) }
    if (active) {
        // Eingeschaltet (z. B. das Mikrofon hört zu): betont und mit anderem Symbol, nicht nur durch die Farbe.
        Button(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = PaddingValues(Spacing.xs)) { content() }
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = PaddingValues(Spacing.xs)) { content() }
    }
}

/** So lange steht eine Kurzmeldung („Letzter Schritt“) in der Kochleiste. */
internal const val BAR_MESSAGE_MILLIS = 3_000L

/** Deckkraft des Verlaufs oben auf dem Foto, damit Titel und Symbole der durchsichtigen Leiste lesbar bleiben. */
internal const val BAR_GRADIENT_ALPHA = 0.9f
internal val BAR_HEIGHT = 64.dp
