package com.moltobene.app.data.ocr

/** Was in einem Bereich steht (#39). [key] wird gespeichert und nie übersetzt. */
enum class AreaKind(val key: String) {
    /** Alles im Rahmen; die App teilt es selbst in Titel, Zutaten und Zubereitung auf. */
    ALL("all"),
    TITLE("title"),
    INGREDIENTS("ingredients"),
    STEPS("steps");

    companion object {
        fun fromKey(key: String?): AreaKind = entries.firstOrNull { it.key == key } ?: ALL
    }
}

/**
 * Ein Rahmen in „Bereich auswählen“: wo gelesen wird und was dort steht. Bezeichnete Rahmen gehen
 * ohne Raten in ihr Feld – so lassen sich z. B. Kochbuchseiten mit zwei Spalten richtig lesen.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
data class AreaFrame(val area: CropArea, val kind: AreaKind = AreaKind.ALL) {

    /** Für den SavedStateHandle: „art@links,oben,rechts,unten“. */
    fun encode(): String = kind.key + "@" + area.encode()

    companion object {
        val WHOLE_PAGE = AreaFrame(CropArea.WHOLE_PAGE)

        /** Neue Rahmen liegen in der Mitte der Seite und lassen sich von dort verschieben. */
        private val NEW_AREA = CropArea(0.1f, 0.35f, 0.9f, 0.65f)

        fun encodeAll(frames: List<AreaFrame>): String = frames.joinToString("+") { it.encode() }

        /** Gegenstück zu [encodeAll]. Ein Bereich ohne Art (ältere Versionen) gilt als „Alles“; ohne Rahmen die ganze Seite. */
        fun decodeAll(text: String): List<AreaFrame> =
            text.split('+').filter { it.isNotBlank() }.map { part ->
                if ('@' in part) {
                    AreaFrame(CropArea.decode(part.substringAfter('@')), AreaKind.fromKey(part.substringBefore('@')))
                } else {
                    AreaFrame(CropArea.decode(part))
                }
            }.ifEmpty { listOf(WHOLE_PAGE) }

        /** Rahmen für „Bereich hinzufügen“: die erste Art, die noch fehlt – Zutaten, Zubereitung, Titel. */
        fun next(existing: List<AreaFrame>): AreaFrame {
            val used = existing.map { it.kind }.toSet()
            val kind = listOf(AreaKind.INGREDIENTS, AreaKind.STEPS, AreaKind.TITLE).firstOrNull { it !in used } ?: AreaKind.ALL
            return AreaFrame(NEW_AREA, kind)
        }
    }
}

/** Kleinster Bereich, der alle [areas] umfasst – z. B. für die Originalseite. */
fun boundsOf(areas: List<CropArea>): CropArea {
    if (areas.isEmpty()) return CropArea.WHOLE_PAGE
    return CropArea(areas.minOf { it.left }, areas.minOf { it.top }, areas.maxOf { it.right }, areas.maxOf { it.bottom })
}

/** Übermalt [areas] weiß: Was dort steht, liest die Texterkennung nicht (noch einmal). */
fun GrayImage.whiten(areas: List<CropArea>): GrayImage {
    if (areas.isEmpty()) return this
    val result = pixels.copyOf()
    areas.forEach { area ->
        val box = area.toBox(width, height)
        for (y in box.top until box.bottom) {
            result.fill(WHITE, y * width + box.left, y * width + box.right)
        }
    }
    return GrayImage(width, height, result)
}

private const val WHITE: Byte = -1
