package com.moltobene.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/**
 * Großes Foto eines Rezepts in seinem eigenen Seitenverhältnis – Hoch- wie Querformat werden ganz gezeigt,
 * nur sehr lange oder breite Fotos zugeschnitten. Antippen öffnet es als Vollbild zum Vergrößern ([onOpen]).
 */
@Composable
fun RecipePhotoLarge(file: File, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    var ratio by rememberSaveable(file) { mutableFloatStateOf(DEFAULT_RATIO) }
    val openLabel = stringResource(R.string.photo_open)
    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clickable(onClickLabel = openLabel, onClick = onOpen),
    ) {
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.photo),
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.width > 0f && size.height > 0f) ratio = (size.width / size.height).coerceIn(MIN_RATIO, MAX_RATIO)
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        // Zeigt, dass sich das Foto vergrößern lässt; die Beschreibung trägt das Antippen selbst.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = ICON_BACKGROUND_ALPHA),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.s),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_zoom_in),
                contentDescription = null,
                modifier = Modifier.padding(Spacing.s).size(20.dp),
            )
        }
    }
}

/** Seitenverhältnis bis das Foto geladen ist (übliche Handyfotos), und die Grenzen für sehr hohe oder breite Fotos. */
private const val DEFAULT_RATIO = 4f / 3f
private const val MIN_RATIO = 3f / 4f
private const val MAX_RATIO = 2f

/** Das Symbol bleibt auf hellen wie dunklen Fotos erkennbar. */
private const val ICON_BACKGROUND_ALPHA = 0.85f

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
