package com.moltobene.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moltobene.app.data.AmountScaling
import com.moltobene.app.ui.theme.Spacing

/**
 * Eine Zutatenzeile in der Rezeptansicht und bei den Grundrezepten: Mit [amountWidth] steht die Menge rechtsbündig in
 * einer eigenen Spalte, der Rest links daneben (Zeilen ohne Menge beginnen in der Spalte des Rests). Ohne [amountWidth]
 * (große Schrift, keine Mengen im Rezept) steht die Zeile wie geschrieben in einem Stück.
 */
@Composable
fun IngredientLine(
    parts: List<AmountScaling.Part>,
    amountWidth: Dp?,
    style: TextStyle,
    color: Color,
    locale: LocaleList? = null,
    modifier: Modifier = Modifier,
) {
    if (amountWidth == null) {
        Text(ingredientText(parts, locale), style = style, color = color, modifier = modifier)
        return
    }
    val (amount, name) = remember(parts) { AmountScaling.splitAmount(parts) }
    // Der Screenreader und die Tests sehen die Zeile als ein Text, nicht als zwei Spalten.
    val full = remember(parts, locale) { ingredientText(parts, locale) }
    Row(modifier = modifier.clearAndSetSemantics { text = full }, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (amount.isEmpty()) {
            Spacer(Modifier.width(amountWidth))
        } else {
            Text(
                ingredientText(amount, locale),
                style = style,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier.width(amountWidth),
            )
        }
        Text(ingredientText(name, locale), style = style, color = color, modifier = Modifier.weight(1f))
    }
}

/** Umgerechnete Mengen sind fett, damit sie nicht nur an der Farbe zu erkennen sind. */
fun ingredientText(parts: List<AmountScaling.Part>, locale: LocaleList?): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(localeList = locale)) {
        parts.forEach { part ->
            if (part.scaled) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
            } else {
                append(part.text)
            }
        }
    }
}

/**
 * Breite der Spalte mit den Mengen: die breiteste Menge aller [lines] (null für Überschriften). null, wenn keine Zeile
 * eine Menge hat oder bei großer Schrift, wo die Zeile ganz bleibt, damit nichts abgeschnitten wird.
 */
@Composable
fun rememberAmountWidth(lines: List<List<AmountScaling.Part>?>, style: TextStyle): Dp? {
    if (isLargeText()) return null
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(lines, style, density) {
        val bold = style.copy(fontWeight = FontWeight.Bold)
        val widest = lines.maxOfOrNull { parts ->
            val amount = parts?.let { AmountScaling.splitAmount(it).first }.orEmpty()
            if (amount.isEmpty()) 0 else measurer.measure(AnnotatedString(amount.joinToString("") { it.text }), bold).size.width
        } ?: 0
        if (widest == 0) null else with(density) { widest.toDp() + 2.dp }
    }
}

/** Gestrichelte Trennlinie zwischen zwei Zutaten. */
@Composable
fun DashedDivider() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            },
    )
}
