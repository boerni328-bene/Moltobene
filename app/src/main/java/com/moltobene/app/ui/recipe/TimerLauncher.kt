package com.moltobene.app.ui.recipe

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Timer beim Kochen: Moltobene stellt ihn in der Uhr-App des Handys. Dort klingelt er zuverlässig, auch wenn das
 * Handy gesperrt oder die App geschlossen ist. Der Rundgang setzt in `AppContainer` einen nachgestellten Timer ein.
 */
fun interface TimerLauncher {
    /**
     * @param label Bezeichnung in der Uhr-App, z. B. „Tomatensoße, Schritt 2“
     * @return false, wenn es keine Uhr-App gibt, die Timer stellen kann
     */
    fun start(context: Context, seconds: Int, label: String): Boolean
}

/** Stellt den Timer in der Uhr-App, ohne sie zu öffnen; dafür braucht die App die Berechtigung „Wecker stellen“. */
object ClockAppTimer : TimerLauncher {
    override fun start(context: Context, seconds: Int, label: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
