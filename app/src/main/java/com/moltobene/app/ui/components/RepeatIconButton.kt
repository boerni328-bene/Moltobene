package com.moltobene.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** So lange wird gehalten, bis das Zählen beginnt. */
internal const val REPEAT_START_MILLIS = 400L

/** Abstand der ersten Schritte beim Halten; er wird mit jedem Schritt kürzer. */
internal const val REPEAT_SLOW_MILLIS = 320L

/** So schnell wird höchstens gezählt. */
internal const val REPEAT_FAST_MILLIS = 35L

/** Der nächste Abstand beim Halten: etwa 15 % kürzer, nie unter [REPEAT_FAST_MILLIS]. */
internal fun nextRepeatDelay(current: Long): Long = maxOf(REPEAT_FAST_MILLIS, current * 85 / 100)

/**
 * Knopf, der beim Halten selbst weiterzählt: ein Tippen macht einen Schritt, langes Drücken zählt zuerst langsam und
 * dann immer schneller weiter, bis losgelassen wird oder [enabled] endet (Grenze erreicht). Für Screenreader bleibt
 * es ein Knopf mit [contentDescription].
 */
@Composable
internal fun RepeatIconButton(
    onStep: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    val canStep by rememberUpdatedState(enabled)
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        repeated = false
        delay(REPEAT_START_MILLIS)
        var wait = REPEAT_SLOW_MILLIS
        while (canStep) {
            repeated = true
            step()
            delay(wait)
            wait = nextRepeatDelay(wait)
        }
    }
    val color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                role = Role.Button,
            ) {
                // Nach dem Halten löst das Loslassen keinen zusätzlichen Schritt aus.
                if (repeated) repeated = false else step()
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides color) { content() }
    }
}
