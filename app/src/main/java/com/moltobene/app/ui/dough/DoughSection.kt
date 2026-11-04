package com.moltobene.app.ui.dough

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.ui.theme.Spacing

/** Die aufklappbaren Bereiche des Teigrechners; gemerkt wird, welcher offen ist. */
internal const val SECTION_YEAST = "yeast"
internal const val SECTION_PREFERMENT = "preferment"
internal const val SECTION_DOUGH = "dough"
internal const val SECTION_SIZE = "size"
internal const val SECTION_INGREDIENTS = "ingredients"

/** Trennt die Werte in der Zusammenfassung eines Bereichs, z. B. „Frischhefe · 0,2 % vom Mehl · 1,2 g“. */
internal const val SUMMARY_SEPARATOR = " · "

/**
 * Ein aufklappbarer Bereich des Teigrechners (Entwurf 5 „Aufklappen“ vom 04.11.2026): oben der Name, darunter die
 * Zusammenfassung der Werte und rechts der Pfeil; aufgeklappt folgen die Felder. Es ist immer nur ein Bereich offen,
 * so passen bei normaler Schrift alle Werte auf einen Bildschirm. Der offene Bereich hat einen Rahmen in der Hauptfarbe.
 */
@Composable
internal fun DoughSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val stateText = stringResource(if (expanded) R.string.basics_expanded else R.string.basics_collapsed)
    Surface(
        color = if (expanded) colors.surfaceContainer else colors.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = if (expanded) BorderStroke(1.5.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column {
            // Die ganze Kopfzeile klappt auf und zu; der Screenreader liest Name, Zusammenfassung und Zustand zusammen.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = stateText }
                    .padding(start = Spacing.m, end = Spacing.s, top = Spacing.xs, bottom = Spacing.xs),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.primary,
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = Spacing.s, end = Spacing.s, bottom = Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    content = content,
                )
            }
        }
    }
}
