package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CropAreaTest {

    private val area = CropArea(0.2f, 0.3f, 0.8f, 0.7f)

    @Test
    fun bereichInBildpunkten() {
        assertEquals(Box(200, 600, 800, 1400), area.toBox(1000, 2000))
        assertEquals(Box(0, 0, 1000, 2000), CropArea.WHOLE_PAGE.toBox(1000, 2000))
        // Auch ein winziger Bereich ergibt mindestens einen Bildpunkt.
        assertEquals(Box(5, 5, 6, 6), CropArea(0.5f, 0.5f, 0.5f, 0.5f).toBox(10, 10))
    }

    @Test
    fun ausschnittEnthaeltDieRichtigenBildpunkte() {
        val image = GrayImage(4, 3, ByteArray(12) { it.toByte() })
        val cropped = image.crop(Box(1, 1, 3, 3))
        assertEquals(2, cropped.width)
        assertEquals(2, cropped.height)
        assertEquals(listOf(5, 6, 9, 10), cropped.pixels.map { it.toInt() })
        assertSame(image, image.crop(CropArea.WHOLE_PAGE))
    }

    @Test
    fun griffeWerdenGefunden() {
        val tolerance = 0.05f
        assertEquals(CropHandle.TOP_LEFT, area.handleAt(0.21f, 0.29f, tolerance, tolerance))
        assertEquals(CropHandle.BOTTOM_RIGHT, area.handleAt(0.83f, 0.72f, tolerance, tolerance))
        assertEquals(CropHandle.LEFT, area.handleAt(0.18f, 0.5f, tolerance, tolerance))
        assertEquals(CropHandle.BOTTOM, area.handleAt(0.5f, 0.71f, tolerance, tolerance))
        assertEquals(CropHandle.MOVE, area.handleAt(0.5f, 0.5f, tolerance, tolerance))
        assertNull(area.handleAt(0.05f, 0.05f, tolerance, tolerance))
    }

    @Test
    fun ziehenBleibtImFotoUndBehaeltMindestgroesse() {
        // Ecke nach außen über den Rand hinaus: endet am Rand des Fotos.
        assertEquals(CropArea(0f, 0f, 0.8f, 0.7f), area.dragged(CropHandle.TOP_LEFT, -0.5f, -0.5f, 0.1f, 0.1f))
        // Rand über den gegenüberliegenden hinaus: Mindestgröße bleibt.
        val narrowed = area.dragged(CropHandle.LEFT, 0.9f, 0f, 0.1f, 0.1f)
        assertEquals(0.7f, narrowed.left, 0.0001f)
        assertEquals(0.8f, narrowed.right, 0.0001f)
        // Verschieben behält die Größe und stößt am Rand an.
        val moved = area.dragged(CropHandle.MOVE, 0.5f, -0.1f, 0.1f, 0.1f)
        assertEquals(0.4f, moved.left, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(0.2f, moved.top, 0.0001f)
        assertEquals(0.6f, moved.bottom, 0.0001f)
    }

    @Test
    fun speichernUndLesen() {
        assertEquals(area, CropArea.decode(area.encode()))
        assertEquals(CropArea.WHOLE_PAGE, CropArea.decode(""))
        assertEquals(CropArea.WHOLE_PAGE, CropArea.decode("0.5,0.5,0.2,0.9"))
        assertTrue(CropArea.WHOLE_PAGE.isWholePage)
        assertFalse(area.isWholePage)
    }
}
