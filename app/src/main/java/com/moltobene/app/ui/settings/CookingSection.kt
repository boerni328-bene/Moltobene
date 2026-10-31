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
import com.moltobene.app.data.StartScreen
import com.moltobene.app.data.StepperSide
import com.moltobene.app.data.WeightUnit

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

/** „Teigrechner“ in den Einstellungen: auf welcher Seite des Werts Minus und Plus nebeneinander stehen. */
@Composable
internal fun DoughSection(
    stepperSide: StepperSide,
    onStepperSide: (StepperSide) -> Unit,
    weightUnit: WeightUnit,
    onWeightUnit: (WeightUnit) -> Unit,
) {
    GroupLabel(stringResource(R.string.stepper_side_title))
    Column(modifier = Modifier.selectableGroup()) {
        StepperSide.entries.forEach { choice ->
            val selected = stepperSide == choice
            ListItem(
                headlineContent = {
                    Text(stringResource(if (choice == StepperSide.RIGHT) R.string.stepper_side_right else R.string.stepper_side_left))
                },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onStepperSide(choice) }),
            )
        }
    }
    GroupLabel(stringResource(R.string.weight_unit_title))
    Column(modifier = Modifier.selectableGroup()) {
        WeightUnit.entries.forEach { choice ->
            val selected = weightUnit == choice
            ListItem(
                headlineContent = {
                    Text(stringResource(if (choice == WeightUnit.GRAMS) R.string.weight_unit_grams else R.string.weight_unit_ounces))
                },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onWeightUnit(choice) }),
            )
        }
    }
}

/** „Start“ in den Einstellungen (#81): Startseite oder gleich die Rezepte beim Öffnen der App. */
@Composable
internal fun StartSection(startScreen: StartScreen, onStartScreen: (StartScreen) -> Unit) {
    GroupLabel(stringResource(R.string.start_title))
    Column(modifier = Modifier.selectableGroup()) {
        StartScreen.entries.forEach { choice ->
            val selected = startScreen == choice
            ListItem(
                headlineContent = {
                    Text(stringResource(if (choice == StartScreen.HOME) R.string.start_home else R.string.start_collection))
                },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onStartScreen(choice) }),
            )
        }
    }
}
