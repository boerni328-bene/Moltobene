package com.moltobene.app.ui.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.moltobene.app.R
import com.moltobene.app.ui.theme.MoltobeneTheme
import com.moltobene.app.ui.theme.Spacing

/** Übersicht „Neu in Version …“ – nach einem Update und jederzeit aus den Einstellungen. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.whats_new_title, WhatsNew.VERSION_NAME),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                stringArrayResource(R.array.whats_new_items).forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Preview(name = "Hell")
@Preview(name = "Schrift 200 %", fontScale = 2f)
@Preview(name = "Dunkel", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WhatsNewDialogPreview() {
    MoltobeneTheme { WhatsNewDialog(onDismiss = {}) }
}
