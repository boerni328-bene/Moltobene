package com.moltobene.app.ui.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.moltobene.app.R
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale

/**
 * Sprache des Textes für die Texterkennung (#42), im Menü oben rechts: Meist erkennt die App sie selbst;
 * bei kurzen Ausschnitten hilft die Wahl. [language] null bedeutet „automatisch erkennen“.
 */
@Composable
fun LanguageMenuButton(language: String?, onLanguageChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.ocr_language))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                text = stringResource(R.string.ocr_language),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.m, vertical = Spacing.s)
                    .semantics { heading() },
            )
            (listOf<String?>(null) + TextLanguage.SUPPORTED).forEach { code ->
                val selected = code == language
                DropdownMenuItem(
                    text = { Text(code?.let { languageName(it) } ?: stringResource(R.string.ocr_language_auto)) },
                    leadingIcon = {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        open = false
                        onLanguageChange(code)
                    },
                    modifier = Modifier.semantics { this.selected = selected },
                )
            }
        }
    }
}

/** Name einer Sprache in ihr selbst („Italiano“, „Français“), damit jeder seine Sprache findet. */
private fun languageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
