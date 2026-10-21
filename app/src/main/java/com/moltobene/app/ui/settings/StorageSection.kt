package com.moltobene.app.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.moltobene.app.R
import com.moltobene.app.data.StorageUsage
import com.moltobene.app.data.StorageUsage.Item
import com.moltobene.app.ui.theme.Spacing

/**
 * Einstellungen → Speicher: was die App auf dem Handy belegt, je Posten und in Summe. Gemessen wird beim ersten
 * Aufklappen (im Hintergrund). Pakete und Zwischenspeicher lassen sich hier löschen; Fotos und Rezepte nie.
 *
 * @param busy es läuft gerade ein Sichern oder Wiederherstellen: dann bleibt der Zwischenspeicher unberührt, weil
 *   eine vorbereitete Wiederherstellung darin liegt
 */
@Composable
internal fun StorageSection(
    result: StorageUsage.Result?,
    busy: Boolean,
    onMeasure: () -> Unit,
    onClearCache: () -> Unit,
    onDeleteLanguagePack: () -> Unit,
    onDeleteVoicePack: () -> Unit,
) {
    val context = LocalContext.current
    // Größen im Format des Handys, z. B. „172 MB“.
    fun size(bytes: Long): String = Formatter.formatShortFileSize(context, bytes)
    LaunchedEffect(Unit) { onMeasure() }

    if (result == null) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.storage_measuring)) },
            leadingContent = { CircularProgressIndicator(modifier = Modifier.padding(Spacing.xs)) },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        return
    }

    // Immer gezeigt: Programm, Fotos, Rezepte. Die übrigen nur, wenn sie etwas belegen.
    val always = setOf(Item.PROGRAM, Item.PHOTOS, Item.DATABASE)
    Item.entries.filter { it in always || result[it] > 0 }.forEach { item ->
        val name = stringResource(nameOf(item))
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(hintOf(item)))
                    when (item) {
                        Item.LANGUAGE_PACK -> OutlinedButton(
                            onClick = onDeleteLanguagePack,
                            modifier = Modifier.semantics { contentDescription = context.getString(R.string.storage_delete_item, name) },
                        ) { Text(stringResource(R.string.delete)) }
                        Item.VOICE_PACK -> OutlinedButton(
                            onClick = onDeleteVoicePack,
                            modifier = Modifier.semantics { contentDescription = context.getString(R.string.storage_delete_item, name) },
                        ) { Text(stringResource(R.string.delete)) }
                        Item.CACHE -> OutlinedButton(onClick = onClearCache, enabled = !busy) {
                            Text(stringResource(R.string.storage_clear_cache))
                        }
                        else -> Unit
                    }
                }
            },
            trailingContent = { Text(size(result[item]), style = MaterialTheme.typography.bodyLarge) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.storage_total), style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(stringResource(R.string.storage_note)) },
        trailingContent = { Text(size(result.total), style = MaterialTheme.typography.titleMedium) },
    )
}

private fun nameOf(item: Item): Int = when (item) {
    Item.PROGRAM -> R.string.storage_program
    Item.PHOTOS -> R.string.storage_photos
    Item.THUMBNAILS -> R.string.storage_thumbnails
    Item.DATABASE -> R.string.storage_database
    Item.LANGUAGE_PACK -> R.string.storage_language_pack
    Item.VOICE_PACK -> R.string.storage_voice_pack
    Item.TRANSLATIONS -> R.string.storage_translations
    Item.CACHE -> R.string.storage_cache
    Item.OTHER -> R.string.storage_other
}

private fun hintOf(item: Item): Int = when (item) {
    Item.PROGRAM -> R.string.storage_program_hint
    Item.PHOTOS -> R.string.storage_photos_hint
    Item.THUMBNAILS -> R.string.storage_thumbnails_hint
    Item.DATABASE -> R.string.storage_database_hint
    Item.LANGUAGE_PACK -> R.string.storage_language_pack_hint
    Item.VOICE_PACK -> R.string.storage_voice_pack_hint
    Item.TRANSLATIONS -> R.string.storage_translations_hint
    Item.CACHE -> R.string.storage_cache_hint
    Item.OTHER -> R.string.storage_other_hint
}
