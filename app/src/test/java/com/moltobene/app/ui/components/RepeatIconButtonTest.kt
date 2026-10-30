package com.moltobene.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatIconButtonTest {
    @Test
    fun abstandWirdKuerzerBisZurGrenze() {
        var wait = REPEAT_SLOW_MILLIS
        var previous = wait
        repeat(60) {
            wait = nextRepeatDelay(wait)
            assertTrue(wait <= previous)
            assertTrue(wait >= REPEAT_FAST_MILLIS)
            previous = wait
        }
        assertEquals(REPEAT_FAST_MILLIS, wait)
    }

    @Test
    fun ersteSchritteSindLangsamUndNachEinerSekundeSchnell() {
        var wait = REPEAT_SLOW_MILLIS
        var elapsed = 0L
        var steps = 0
        while (elapsed < 2_000) {
            elapsed += wait
            wait = nextRepeatDelay(wait)
            steps++
        }
        // In den ersten zwei Sekunden Halten sind es weder zu wenige noch zu viele Schritte.
        assertTrue(steps in 8..30)
    }
}
