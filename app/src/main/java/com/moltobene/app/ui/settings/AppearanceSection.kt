package com.moltobene.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.Appearance
import com.moltobene.app.data.Palette
import com.moltobene.app.data.ThemeMode
import com.moltobene.app.ui.theme.Spacing
import com.moltobene.app.ui.theme.colorSchemeOf

/**
 * „Darstellung“ in den Einstellungen (Wunsch des Projektinhabers vom 08.10.2026): Hell oder dunkel und die
 * Farbwelt. Jede Farbwelt steht mit Namen da, nicht nur als Farbe; die gewählte hat zusätzlich einen Haken.
 */
@Composable
internal fun AppearanceSection(
    appearance: Appearance,
    onMode: (ThemeMode) -> Unit,
    onPalette: (Palette) -> Unit,
) {
    GroupLabel(stringResource(R.string.appearance_mode))
    Column(modifier = Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            val selected = appearance.mode == mode
            ListItem(
                headlineContent = { Text(stringResource(mode.label)) },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onMode(mode) }),
            )
        }
    }
    GroupLabel(stringResource(R.string.appearance_palette))
    PaletteChoices(selected = appearance.palette, onPalette = onPalette)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteChoices(selected: Palette, onPalette: (Palette) -> Unit) {
    // Die Muster zeigen jede Farbwelt so, wie sie gerade aussähe: hell oder dunkel wie die App.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    FlowRow(
        modifier = Modifier
            .selectableGroup()
            .padding(start = Spacing.s, end = Spacing.s, bottom = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Palette.entries.forEach { palette ->
            PaletteChoice(palette = palette, dark = dark, selected = palette == selected, onClick = { onPalette(palette) })
        }
    }
}

@Composable
private fun PaletteChoice(palette: Palette, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = colorSchemeOf(palette, dark)
    val ring = if (selected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .widthIn(min = 76.dp)
            .padding(Spacing.s),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer)
                .then(ring),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(
            text = stringResource(palette.label),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = Spacing.m, end = Spacing.m, top = Spacing.s, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

@get:StringRes
private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@get:StringRes
internal val Palette.label: Int
    get() = when (this) {
        Palette.SLATE -> R.string.palette_slate
        Palette.COBALT -> R.string.palette_cobalt
        Palette.SAGE -> R.string.palette_sage
        Palette.TERRACOTTA -> R.string.palette_terracotta
        Palette.SAFFRON -> R.string.palette_saffron
    }
