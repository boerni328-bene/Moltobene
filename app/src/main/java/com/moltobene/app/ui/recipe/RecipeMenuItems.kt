package com.moltobene.app.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.CookingTextSize
import com.moltobene.app.ui.theme.Spacing

/**
 * Die Einträge des Menüs zu einem Rezept: Favorit, Teilen, Löschen und (nur in der Rezeptansicht, [textSize] nicht null) Schriftgröße. Dasselbe Menü steht oben in der
 * Rezeptansicht (⋮) und beim langen Berühren eines Rezepts im Kochbuch. „Fortschritt zurücksetzen“ gibt es nur in der
 * Rezeptansicht ([onReset] nicht null).
 */
@Composable
internal fun RecipeMenuItems(
    favorite: Boolean,
    shareEnabled: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onShareFile: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    textSize: CookingTextSize? = null,
    onTextSize: (CookingTextSize) -> Unit = {},
    onReset: (() -> Unit)? = null,
    resetEnabled: Boolean = true,
) {
    DropdownMenuItem(
        text = { Text(stringResource(if (favorite) R.string.favorite_remove else R.string.favorite_add)) },
        leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
        onClick = {
            onDismiss()
            onFavorite()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.share_recipe)) },
        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
        enabled = shareEnabled,
        onClick = {
            onDismiss()
            onShare()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.share_as_file)) },
        enabled = shareEnabled,
        onClick = {
            onDismiss()
            onShareFile()
        },
    )
    if (textSize != null) {
        TextSizeRow(selected = textSize, onSelect = {
            onDismiss()
            onTextSize(it)
        })
    }
    if (onReset != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.cooking_reset)) },
            enabled = resetEnabled,
            onClick = {
                onDismiss()
                onReset()
            },
        )
    }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.delete_recipe)) },
        onClick = {
            onDismiss()
            onDelete()
        },
    )
}

/**
 * Schriftgröße in einer Zeile: drei Knöpfe mit einem „A“ in kleiner, mittlerer und großer Schrift. Die Wahl gilt für
 * alle Rezepte. Jeder Knopf hat eine Beschreibung für den Screenreader.
 */
@Composable
private fun TextSizeRow(selected: CookingTextSize, onSelect: (CookingTextSize) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(stringResource(R.string.cooking_text_size), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        SizeButton(CookingTextSize.SMALL, R.string.cooking_size_small, MaterialTheme.typography.bodyMedium, selected, onSelect)
        SizeButton(CookingTextSize.MEDIUM, R.string.cooking_size_medium, MaterialTheme.typography.titleMedium, selected, onSelect)
        SizeButton(CookingTextSize.LARGE, R.string.cooking_size_large, MaterialTheme.typography.headlineSmall, selected, onSelect)
    }
}

@Composable
private fun RowScope.SizeButton(
    size: CookingTextSize,
    description: Int,
    style: TextStyle,
    selected: CookingTextSize,
    onSelect: (CookingTextSize) -> Unit,
) {
    val label = stringResource(description)
    val modifier = Modifier.size(48.dp).semantics {
        contentDescription = label
        this.selected = size == selected
    }
    val content: @Composable () -> Unit = { Text("A", style = style) }
    // Die gewählte Größe ist gefüllt, die anderen nur umrandet: nicht nur durch die Farbe zu erkennen.
    if (size == selected) {
        FilledTonalButton(onClick = { onSelect(size) }, modifier = modifier, contentPadding = PaddingValues(0.dp)) { content() }
    } else {
        OutlinedButton(onClick = { onSelect(size) }, modifier = modifier, contentPadding = PaddingValues(0.dp)) { content() }
    }
}
