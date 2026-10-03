package com.moltobene.app.data.ocr

import kotlin.math.abs
import kotlin.math.roundToInt

/** Rechteck im Bild in Bildpunkten (rechts und unten ausschließlich). */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Teil des Rahmens, der gerade gezogen wird. */
enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, MOVE }

/**
 * Bereich eines Fotos, der gelesen wird („Bereich auswählen“) – als Anteil von Breite und Höhe
 * (0 bis 1) des richtig gedrehten Fotos, damit er unabhängig von der Bildgröße ist.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
data class CropArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    val isWholePage: Boolean
        get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    /** Rechteck in Bildpunkten eines Bildes der Größe [width] × [height], mindestens 1 × 1. */
    fun toBox(width: Int, height: Int): Box {
        val boxLeft = (left * width).roundToInt().coerceIn(0, width - 1)
        val boxTop = (top * height).roundToInt().coerceIn(0, height - 1)
        val boxRight = (right * width).roundToInt().coerceIn(boxLeft + 1, width)
        val boxBottom = (bottom * height).roundToInt().coerceIn(boxTop + 1, height)
        return Box(boxLeft, boxTop, boxRight, boxBottom)
    }

    /**
     * Welcher Teil des Rahmens liegt an der Stelle [x], [y] (Anteile des Fotos)? Ecken vor Rändern,
     * Ränder vor dem Inneren; [toleranceX] und [toleranceY] machen die Griffe ausreichend groß.
     */
    fun handleAt(x: Float, y: Float, toleranceX: Float, toleranceY: Float): CropHandle? {
        val nearLeft = abs(x - left) <= toleranceX
        val nearRight = abs(x - right) <= toleranceX
        val nearTop = abs(y - top) <= toleranceY
        val nearBottom = abs(y - bottom) <= toleranceY
        val withinX = x >= left - toleranceX && x <= right + toleranceX
        val withinY = y >= top - toleranceY && y <= bottom + toleranceY
        // Ist der Rahmen sehr klein, gewinnt die nähere Seite.
        val onLeft = nearLeft && (!nearRight || abs(x - left) <= abs(x - right))
        val onRight = nearRight && !onLeft
        val onTop = nearTop && (!nearBottom || abs(y - top) <= abs(y - bottom))
        val onBottom = nearBottom && !onTop
        return when {
            onTop && onLeft -> CropHandle.TOP_LEFT
            onTop && onRight -> CropHandle.TOP_RIGHT
            onBottom && onLeft -> CropHandle.BOTTOM_LEFT
            onBottom && onRight -> CropHandle.BOTTOM_RIGHT
            onLeft && withinY -> CropHandle.LEFT
            onRight && withinY -> CropHandle.RIGHT
            onTop && withinX -> CropHandle.TOP
            onBottom && withinX -> CropHandle.BOTTOM
            x in left..right && y in top..bottom -> CropHandle.MOVE
            else -> null
        }
    }

    /**
     * Rahmen nach dem Ziehen von [handle] um [dx], [dy] (Anteile des Fotos). Er bleibt im Foto und
     * mindestens [minWidth] × [minHeight] groß.
     */
    fun dragged(handle: CropHandle, dx: Float, dy: Float, minWidth: Float, minHeight: Float): CropArea {
        if (handle == CropHandle.MOVE) {
            val moveX = dx.coerceIn(-left, 1f - right)
            val moveY = dy.coerceIn(-top, 1f - bottom)
            return CropArea(left + moveX, top + moveY, right + moveX, bottom + moveY)
        }
        var newLeft = left
        var newTop = top
        var newRight = right
        var newBottom = bottom
        if (handle in LEFT_SIDE) newLeft = (left + dx).coerceIn(0f, right - minWidth)
        if (handle in RIGHT_SIDE) newRight = (right + dx).coerceIn(left + minWidth, 1f)
        if (handle in TOP_SIDE) newTop = (top + dy).coerceIn(0f, bottom - minHeight)
        if (handle in BOTTOM_SIDE) newBottom = (bottom + dy).coerceIn(top + minHeight, 1f)
        return CropArea(newLeft, newTop, newRight, newBottom)
    }

    /** Für den SavedStateHandle: „links,oben,rechts,unten“. */
    fun encode(): String = "$left,$top,$right,$bottom"

    companion object {
        val WHOLE_PAGE = CropArea(0f, 0f, 1f, 1f)

        private val LEFT_SIDE = setOf(CropHandle.TOP_LEFT, CropHandle.BOTTOM_LEFT, CropHandle.LEFT)
        private val RIGHT_SIDE = setOf(CropHandle.TOP_RIGHT, CropHandle.BOTTOM_RIGHT, CropHandle.RIGHT)
        private val TOP_SIDE = setOf(CropHandle.TOP_LEFT, CropHandle.TOP_RIGHT, CropHandle.TOP)
        private val BOTTOM_SIDE = setOf(CropHandle.BOTTOM_LEFT, CropHandle.BOTTOM_RIGHT, CropHandle.BOTTOM)

        /** Gegenstück zu [encode]; bei ungültigem Text die ganze Seite. */
        fun decode(text: String): CropArea {
            val values = text.split(',').mapNotNull { it.trim().toFloatOrNull() }
            if (values.size != 4) return WHOLE_PAGE
            val (left, top, right, bottom) = values.map { it.coerceIn(0f, 1f) }
            return if (left < right && top < bottom) CropArea(left, top, right, bottom) else WHOLE_PAGE
        }
    }
}

/** Ausschnitt [box] als eigenes Bild. */
fun GrayImage.crop(box: Box): GrayImage {
    if (box.left == 0 && box.top == 0 && box.width == width && box.height == height) return this
    val result = ByteArray(box.width * box.height)
    for (y in 0 until box.height) {
        System.arraycopy(pixels, (box.top + y) * width + box.left, result, y * box.width, box.width)
    }
    return GrayImage(box.width, box.height, result)
}

/** Nur der gewählte Bereich; bei der ganzen Seite das Bild selbst. */
fun GrayImage.crop(area: CropArea): GrayImage = if (area.isWholePage) this else crop(area.toBox(width, height))
