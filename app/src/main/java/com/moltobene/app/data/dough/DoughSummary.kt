package com.moltobene.app.data.dough

/**
 * Kurzfassungen für den Teigrechner (Entwurf 5 „Aufklappen“ vom 04.11.2026): wie schwer der Vorteig ist, was danach
 * zum Kneten in den Hauptteig kommt und wie viel Hefe der Teig insgesamt hat. Reines Kotlin, per Unit-Test prüfbar.
 */
object DoughSummary {

    /** Gewicht des ganzen Vorteigs: Mehl, Wasser und bei Biga und Poolish die Hefe. 0 ohne Vorteig. */
    fun prefermentGrams(result: DoughResult.Success): Double =
        result.sections.firstOrNull { it.kind == SectionKind.PREFERMENT }?.lines?.sumOf { it.grams } ?: 0.0

    /**
     * Was zum Kneten zum Vorteig in den Hauptteig kommt: zuerst die Hefe, falls sie nicht schon im Vorteig ist (beim
     * Sauerteig), dann Mehl, Wasser, Salz und alles Weitere in der Reihenfolge der Rechnung. Leer ohne Vorteig.
     */
    fun mainLines(result: DoughResult.Success): List<DoughLine> {
        val main = result.sections.firstOrNull { it.kind == SectionKind.MAIN } ?: return emptyList()
        val lines = main.lines.filter { it.grams > 0 }
        return lines.filter { it.role == LineRole.YEAST } + lines.filter { it.role != LineRole.YEAST }
    }

    /** Hefe im ganzen Teig in Gramm, gleich ob sie im Vorteig oder im Hauptteig steht. */
    fun yeastGrams(result: DoughResult.Success): Double =
        result.sections.flatMap { it.lines }.filter { it.role == LineRole.YEAST }.sumOf { it.grams }
}
