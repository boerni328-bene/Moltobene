package com.moltobene.app.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Meldungen der App (#88): Bei großer Schrift steht die Aktion unter dem Text statt daneben. Sonst bliebe dem Text eine
 * schmale Spalte, in der Wörter mitten im Wort umbrechen und die Meldung fast den Bildschirm füllt.
 */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val large = isLargeText()
    SnackbarHost(hostState, modifier) { data -> Snackbar(data, actionOnNewLine = large) }
}
