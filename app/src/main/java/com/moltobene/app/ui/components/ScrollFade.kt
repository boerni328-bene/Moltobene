package com.moltobene.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import com.moltobene.app.ui.theme.Spacing

/**
 * Zeigt, dass eine Reihe seitlich weitergeht: Der Inhalt läuft an dem Rand aus, hinter dem noch etwas liegt.
 * Vor `horizontalScroll(state)` setzen. Die Farben sind hier nur eine Maske (deckend bis durchsichtig), keine
 * Farben der Oberfläche; deshalb passt es zu jedem Hintergrund.
 */
fun Modifier.scrollFade(state: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val width = Spacing.l.toPx().coerceAtMost(size.width / 2)
        val rtl = layoutDirection == LayoutDirection.Rtl
        val moreRight = if (rtl) state.canScrollBackward else state.canScrollForward
        val moreLeft = if (rtl) state.canScrollForward else state.canScrollBackward
        if (moreRight) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - width,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - width, 0f),
                size = Size(width, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
        if (moreLeft) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = width,
                ),
                topLeft = Offset.Zero,
                size = Size(width, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }
