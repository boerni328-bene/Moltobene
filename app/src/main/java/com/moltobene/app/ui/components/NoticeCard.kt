package com.moltobene.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.ui.theme.Spacing

/** Art eines Hinweises: ruhige Information oder Problem. Die Bedeutung steht immer auch im Symbol und im Text. */
enum class NoticeKind(@DrawableRes val icon: Int) {
    INFO(R.drawable.ic_info),
    PROBLEM(R.drawable.ic_warning),
}

/**
 * Gemeinsamer Baustein für Hinweise (#90): Prüfhinweis nach dem Übernehmen, Fehler beim Übernehmen, Sicherheitshinweise.
 * Symbol und Text, nie nur Farbe. Der Screenreader liest den Text einmal vor, sobald er erscheint. Die Aktionen
 * stehen unter dem Text und brechen bei großer Schrift um.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticeCard(
    text: String,
    kind: NoticeKind,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val problem = kind == NoticeKind.PROBLEM
    Surface(
        color = if (problem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (problem) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Top) {
                Icon(painterResource(kind.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) { actions() }
        }
    }
}
