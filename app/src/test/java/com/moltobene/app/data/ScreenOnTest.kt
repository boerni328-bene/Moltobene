package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** „Bildschirm in der Rezeptansicht“ (#68) mit nachgestellter Uhr. */
class ScreenOnTest {

    private var now = 1_000_000L
    private fun timer(setting: ScreenOn) = ScreenOnTimer(setting) { now }
    private fun minutes(value: Int) = value * 60_000L

    @Test
    fun bleibtAn() {
        val timer = timer(ScreenOn.STAYS_ON)
        now += minutes(240)
        assertNull(timer.remainingMillis())
        assertTrue(timer.keepsScreenOn())
    }

    @Test
    fun wieDasHandy() {
        val timer = timer(ScreenOn.DEVICE)
        assertEquals(0L, timer.remainingMillis())
        assertFalse(timer.keepsScreenOn())
    }

    @Test
    fun gehtNachDerFristAus() {
        val timer = timer(ScreenOn.OFF_AFTER_15)
        assertEquals(minutes(15), timer.remainingMillis())
        now += minutes(14)
        assertTrue(timer.keepsScreenOn())
        now += minutes(1)
        assertEquals(0L, timer.remainingMillis())
        assertFalse(timer.keepsScreenOn())
        now += minutes(60)
        assertEquals(0L, timer.remainingMillis())
    }

    @Test
    fun tippenStartetDieFristNeu() {
        val timer = timer(ScreenOn.OFF_AFTER_30)
        now += minutes(29)
        timer.touch()
        now += minutes(29)
        assertEquals(minutes(1), timer.remainingMillis())
        now += minutes(2)
        assertFalse(timer.keepsScreenOn())
        // Nach dem Ausgehen schaltet die nächste Berührung wieder an.
        timer.touch()
        assertTrue(timer.keepsScreenOn())
    }

    @Test
    fun kennungen() {
        assertEquals(ScreenOn.OFF_AFTER_15, ScreenOn.fromKey("off_after_15_minutes"))
        assertEquals(ScreenOn.DEVICE, ScreenOn.fromKey("device"))
        assertEquals(ScreenOn.STAYS_ON, ScreenOn.fromKey(null))
        assertEquals(ScreenOn.STAYS_ON, ScreenOn.fromKey("unbekannt"))
        assertEquals(ScreenOn.entries.size, ScreenOn.entries.map { it.key }.toSet().size)
    }
}
