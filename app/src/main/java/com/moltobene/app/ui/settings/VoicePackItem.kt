package com.moltobene.app.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.moltobene.app.R
import com.moltobene.app.data.translate.LanguagePackManager
import com.moltobene.app.data.voice.VoicePack
import com.moltobene.app.ui.theme.Spacing

/**
 * Erkennungspaket für die Spracheingabe (#80): Größe vorab, Fortschritt, Fehler mit dem nächsten Schritt – wie beim
 * Sprachpaket für „Rezept übersetzen“ (gleiche Zustände und Probleme).
 */
@Composable
internal fun VoicePackItem(
    state: LanguagePackManager.State,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    // Größen im Format des Handys, z. B. „46 MB“.
    fun size(bytes: Long): String = Formatter.formatShortFileSize(context, bytes)
    ListItem(
        headlineContent = { Text(stringResource(R.string.voice_pack_title)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                when (state) {
                    LanguagePackManager.State.Checking, LanguagePackManager.State.Missing -> {
                        Text(stringResource(R.string.voice_pack_text, size(VoicePack.DOWNLOAD_BYTES), size(VoicePack.INSTALLED_BYTES)))
                        if (state == LanguagePackManager.State.Missing) {
                            OutlinedButton(onClick = onDownload) { Text(stringResource(R.string.language_pack_download)) }
                        }
                    }
                    is LanguagePackManager.State.Downloading -> {
                        Text(
                            text = stringResource(R.string.voice_pack_downloading),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        val progress = state.progress
                        if (progress != null) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                    }
                    is LanguagePackManager.State.Installed -> {
                        Text(stringResource(R.string.voice_pack_installed, size(state.bytes)))
                        OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                    }
                    is LanguagePackManager.State.Failed -> {
                        Text(
                            text = when (state.problem) {
                                LanguagePackManager.Problem.NO_CONNECTION -> stringResource(R.string.voice_pack_error_connection)
                                LanguagePackManager.Problem.NO_SPACE ->
                                    stringResource(R.string.voice_pack_error_space, size(VoicePack.INSTALLED_BYTES))
                                LanguagePackManager.Problem.DAMAGED -> stringResource(R.string.voice_pack_error_damaged)
                                LanguagePackManager.Problem.UNAVAILABLE -> stringResource(R.string.voice_pack_error_unavailable)
                            },
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        OutlinedButton(onClick = onDownload) { Text(stringResource(R.string.language_pack_retry)) }
                    }
                }
            }
        },
    )
}
