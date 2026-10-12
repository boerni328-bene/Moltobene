package com.moltobene.app.data.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepNavigationTest {

    @Test
    fun weiterOhneMarkierungBeginntBeimErstenSchritt() {
        assertEquals(0, StepNavigation.next(null, 4))
    }

    @Test
    fun weiterGehtEinenSchrittUndHoertAmEndeAuf() {
        assertEquals(2, StepNavigation.next(1, 4))
        assertNull(StepNavigation.next(3, 4))
    }

    @Test
    fun zurueckBleibtBeimErstenSchritt() {
        assertEquals(1, StepNavigation.previous(2, 4))
        assertEquals(0, StepNavigation.previous(0, 4))
        assertEquals(0, StepNavigation.previous(null, 4))
    }

    @Test
    fun ohneSchritteGibtEsNichts() {
        assertNull(StepNavigation.next(null, 0))
        assertNull(StepNavigation.previous(null, 0))
    }
}
