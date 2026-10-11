package com.moltobene.app.data

import kotlin.math.PI

/**
 * Eine Backform (#67): rund mit Durchmesser oder eckig mit Länge × Breite, immer in cm.
 * @param a Durchmesser bzw. Länge
 * @param b Breite; bei einer runden Form ohne Bedeutung
 */
data class Pan(val round: Boolean, val a: Double, val b: Double = a) {
    /** Grundfläche in cm². */
    val area: Double get() = if (round) PI * (a / 2) * (a / 2) else a * b

    companion object {
        fun round(diameter: Double) = Pan(round = true, a = diameter)
        fun rectangle(length: Double, width: Double) = Pan(round = false, a = length, b = width)
    }
}

/**
 * „Backform umrechnen“ (#67): Die Menge des Teigs richtet sich nach der Grundfläche der Form. Von Ø 26 cm auf Ø 28 cm
 * ist das etwa das 1,16-Fache, nicht 28/26. Nur für die Anzeige; die Backzeit rechnet die App nie um.
 * Kastenform und Gugelhupf gibt es nicht, ihr Inhalt folgt nicht aus einem Maß. Reines Kotlin, per Unit-Test prüfbar.
 */
object PanScaling {

    const val CM_PER_INCH = 2.54

    /** Eine Seite oder ein Durchmesser: höchstens 100 cm, mindestens 5 cm. */
    const val MIN_CM = 5.0
    const val MAX_CM = 100.0

    /** Zwischen Faktor 1/10 und 10, damit kein Wert unsinnig groß oder klein wird. */
    const val MIN_FACTOR = 0.1
    const val MAX_FACTOR = 10.0

    fun isValid(pan: Pan): Boolean {
        fun side(value: Double) = value.isFinite() && value in MIN_CM..MAX_CM
        return side(pan.a) && (pan.round || side(pan.b))
    }

    /**
     * Faktor für die Mengen von [from] auf [to]; null, wenn eine Form ungültig ist oder der Faktor außerhalb von
     * [MIN_FACTOR] bis [MAX_FACTOR] liegt.
     */
    fun factor(from: Pan, to: Pan): Double? {
        if (!isValid(from) || !isValid(to)) return null
        return (to.area / from.area).takeIf { it in MIN_FACTOR..MAX_FACTOR }
    }

    /** Liest eine Eingabe in der Einheit [inch] (sonst cm) und gibt cm zurück. */
    fun toCm(value: Double, inch: Boolean): Double = if (inch) value * CM_PER_INCH else value

    /**
     * Die Backform aus einer Angabe wie „Springform (Ø 26 cm)“, „26er Springform“, „9-inch round pan“ oder
     * „Blech 30 x 40 cm“. Nur wenn die Form eindeutig ist; sonst null, und die Form wird gewählt – die App rät nie still.
     * Das zweite Ergebnis sagt, ob die Maße in Zoll angegeben waren.
     */
    fun parse(text: String?): Pair<Pan, Boolean>? {
        val value = text?.replace(' ', ' ')?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        RECTANGLE.find(value)?.let { match ->
            val inch = INCH.containsMatchIn(match.groupValues[3]) || INCH_AFTER.containsMatchIn(value.substring(match.range.last + 1))
            val a = number(match.groupValues[1]) ?: return null
            val b = number(match.groupValues[2]) ?: return null
            val pan = Pan.rectangle(toCm(a, inch), toCm(b, inch))
            return if (isValid(pan)) pan to inch else null
        }
        val diameter = DIAMETER.find(value)?.let { number(it.groupValues[1]) }
        val er = ER.find(value)?.let { number(it.groupValues[1]) }
        val sized = SIZED.find(value)
        val inchSized = sized?.groupValues?.get(2)?.let { INCH.containsMatchIn(it) } == true
        val (size, inch) = when {
            diameter != null -> diameter to INCH_AFTER.containsMatchIn(value.substringAfter(DIAMETER.find(value)!!.value))
            er != null -> er to false
            // „26 cm“ oder „9-inch“ zählen nur zusammen mit einem Wort für eine runde Form.
            sized != null && ROUND_WORD.containsMatchIn(value) -> (number(sized.groupValues[1]) ?: return null) to inchSized
            else -> return null
        }
        val pan = Pan.round(toCm(size, inch))
        return if (isValid(pan)) pan to inch else null
    }

    private fun number(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    private const val UNIT = "cm|zoll|inch|in\\b|\"|″|”"
    private val INCH = Regex("inch|zoll|in\\b|\"|″|”", RegexOption.IGNORE_CASE)
    private val INCH_AFTER = Regex("^\\s*-?\\s*(?:inch|zoll|in\\b|\"|″|”)", RegexOption.IGNORE_CASE)
    private val RECTANGLE = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:$UNIT)?\\s*[x×]\\s*(\\d+(?:[.,]\\d+)?)\\s*(-?\\s*(?:$UNIT)?)", RegexOption.IGNORE_CASE)
    private val DIAMETER = Regex("[Ø⌀]\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
    private val ER = Regex("(\\d{2})er\\b", RegexOption.IGNORE_CASE)
    private val SIZED = Regex("(\\d+(?:[.,]\\d+)?)\\s*-?\\s*($UNIT)", RegexOption.IGNORE_CASE)
    private val ROUND_WORD = Regex(
        "springform|tarte|quiche|kuchenform|tortenform|round|pie|cake pan|rund|tortiera|teglia tonda|moule|molde|redond",
        RegexOption.IGNORE_CASE,
    )
}
