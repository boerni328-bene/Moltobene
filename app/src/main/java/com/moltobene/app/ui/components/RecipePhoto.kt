package com.moltobene.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.moltobene.app.R
import com.moltobene.app.ui.theme.Spacing
import java.io.File

/** Foto eines Rezepts – oder ein gestalteter Platzhalter, wenn es keines gibt. */
@Composable
fun RecipePhoto(
    file: File?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 32.dp,
) {
    if (file != null) {
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.photo),
            contentScale = ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        PhotoPlaceholder(modifier = modifier, iconSize = placeholderIconSize)
    }
}

@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier, iconSize: Dp = 32.dp) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_restaurant),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Kleines Etikett „Entwurf“. */
@Composable
fun DraftLabel(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.draft_label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
        )
    }
}

/** Zentrierter Hinweis für Leer-, Lade- und Fehlerzustände. */
@Composable
fun CenteredMessage(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.l), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            content()
        }
    }
}
