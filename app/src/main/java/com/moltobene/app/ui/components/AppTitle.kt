package com.moltobene.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.moltobene.app.R
import com.moltobene.app.ui.theme.MoltobeneTheme
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale

/**
 * Schriftzug „MOLTOBENE“ oben auf jedem Bildschirm (Wunsch des Projektinhabers vom 08.10.2026): fett, „M“ und „B“
 * in der Markenfarbe wie im Logo. Der Screenreader liest „Moltobene“ statt einzelner Buchstaben. Passt der Name nicht
 * ganz in den Platz – etwa bei großer Schrift neben dem Umschalter „DE | EN“ –, wird er weggelassen statt abgeschnitten.
 */
@Composable
fun AppTitle(style: TextStyle, modifier: Modifier = Modifier, heading: Boolean = false) {
    val name = stringResource(R.string.app_name)
    val text = remember(name) { name.uppercase(Locale.ROOT) }
    val bold = remember(style) { style.copy(fontWeight = FontWeight.Bold) }
    val brand = MoltobeneTheme.brand.blue
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val width = remember(text, bold, measurer) { measurer.measure(text, bold, maxLines = 1).size.width }
        if (width <= constraints.maxWidth) {
            Text(
                text = buildAnnotatedString {
                    text.forEachIndexed { index, char ->
                        // Das „M“ am Anfang und das „B“ wie im Logo „MB“.
                        if (index == 0 || char == 'B') {
                            withStyle(SpanStyle(color = brand)) { append(char) }
                        } else {
                            append(char)
                        }
                    }
                },
                style = bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.semantics {
                    contentDescription = name
                    if (heading) heading()
                },
            )
        }
    }
}

/**
 * Name des Bildschirms als Überschrift unter „MOLTOBENE“, z. B. „Einstellungen“ oder „Rezept bearbeiten“. So steht
 * oben immer der Name der App und darunter, wo man gerade ist.
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Für Bildschirme, auf denen jeder Platz für ein Foto gebraucht wird (Seitenübersicht, Bereich wählen, Vollbild):
 * „MOLTOBENE“ und darunter klein der Name des Bildschirms, beides in der oberen Leiste.
 */
@Composable
fun AppTitleWithScreen(screen: String, detail: String? = null) {
    Column {
        AppTitle(style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (detail == null) screen else stringResource(R.string.screen_title_detail, screen, detail),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Abstand der Überschrift [ScreenTitle] zum Rand, gleich auf allen Bildschirmen. */
val ScreenTitlePadding = PaddingValues(
    start = Spacing.m,
    end = Spacing.m,
    top = Spacing.s,
    bottom = Spacing.s,
)
