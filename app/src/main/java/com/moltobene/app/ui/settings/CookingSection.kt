package com.moltobene.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.moltobene.app.R
import com.moltobene.app.data.ScreenOn

/**
 * „Kochen“ in den Einstellungen (#68): wie lange der Bildschirm in der Rezeptansicht anbleibt. Später kommt hier auch
 * „Große Schrift beim Kochen“ (#61) hin.
 */
@Composable
internal fun CookingSection(screenOn: ScreenOn, onScreenOn: (ScreenOn) -> Unit) {
    GroupLabel(stringResource(R.string.screen_on_title))
    Column(modifier = Modifier.selectableGroup()) {
        ScreenOn.entries.forEach { choice ->
            val selected = screenOn == choice
            ListItem(
                headlineContent = { Text(label(choice)) },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onScreenOn(choice) }),
            )
        }
    }
}

@Composable
private fun label(choice: ScreenOn): String {
    val minutes = choice.minutes
    return when {
        minutes != null -> pluralStringResource(R.plurals.screen_on_off_after, minutes, minutes)
        choice == ScreenOn.DEVICE -> stringResource(R.string.screen_on_device)
        else -> stringResource(R.string.screen_on_stays)
    }
}
