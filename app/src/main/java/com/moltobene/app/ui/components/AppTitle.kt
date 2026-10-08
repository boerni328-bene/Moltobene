package com.moltobene.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import com.moltobene.app.R
import java.util.Locale

/**
 * Name der App als Titel oben („MOLTOBENE“), in der Sammlung und in der Rezeptansicht. Der Screenreader liest
 * „Moltobene“ statt einzelner Buchstaben. Passt der Name nicht ganz in den Platz – etwa bei großer Schrift neben dem
 * Umschalter „DE | EN“ –, wird er weggelassen statt abgeschnitten.
 */
@Composable
fun AppTitle(style: TextStyle, modifier: Modifier = Modifier, heading: Boolean = false) {
    val name = stringResource(R.string.app_name)
    val text = remember(name) { name.uppercase(Locale.ROOT) }
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val width = remember(text, style, measurer) { measurer.measure(text, style, maxLines = 1).size.width }
        if (width <= constraints.maxWidth) {
            Text(
                text = text,
                style = style,
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
