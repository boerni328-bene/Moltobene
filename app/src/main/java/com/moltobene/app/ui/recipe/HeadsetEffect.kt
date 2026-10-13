package com.moltobene.app.ui.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moltobene.app.data.speech.HeadsetActions
import com.moltobene.app.data.speech.HeadsetButtons

/**
 * Schaltet die Kopfhörertasten (#79) ein, solange [enabled] gilt und die Ansicht im Vordergrund steht. [speaking] sagt
 * Android, ob gerade vorgelesen wird. Die Aktionen laufen immer mit dem aktuellen Stand der Ansicht.
 */
@Composable
internal fun HeadsetEffect(
    enabled: Boolean,
    speaking: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val play by rememberUpdatedState(onPlay)
    val pause by rememberUpdatedState(onPause)
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val buttons = remember(context) {
        HeadsetButtons(
            context,
            object : HeadsetActions {
                override fun onPlay() = play()
                override fun onPause() = pause()
                override fun onNext() = next()
                override fun onPrevious() = previous()
            },
        )
    }
    DisposableEffect(enabled, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (enabled) buttons.activate()
                Lifecycle.Event.ON_STOP -> buttons.deactivate()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (enabled && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) buttons.activate()
        onDispose {
            lifecycle.removeObserver(observer)
            buttons.deactivate()
        }
    }
    LaunchedEffect(speaking, enabled) { buttons.update(speaking) }
}
