package com.moltobene.app.data.dough

/**
 * Wie warm das Wasser zum Kneten sein muss, damit der Teig danach die gewünschte Temperatur hat (Wunsch vom 05.11.2026).
 * Wärmebilanz mit Richtwerten: Mehl speichert etwa 1,7, Wasser 4,18 kJ je kg und Grad; Kneten von Hand erwärmt den Teig
 * um etwa 3 °C, mit der Maschine um etwa 8 °C. Das Mehl hat Raumtemperatur, die Biga kommt aus dem Kühlschrank, Poolish
 * und Sauerteig haben Raumtemperatur. Salz, Öl, Hefe und Kleinmengen bleiben außen vor. Reines Kotlin.
 */
object DoughWater {

    private const val FLOUR_HEAT = 1.7
    private const val WATER_HEAT = 4.18

    /** Darunter müsste das Wasser eiskalt sein. */
    const val TOO_COLD = 2.0

    /** Darüber schadet es der Hefe. */
    const val TOO_HOT = 40.0

    private val FLOURS = setOf(LineRole.FLOUR, LineRole.WHEAT, LineRole.RYE, LineRole.WHOLE, LineRole.SPELT)

    fun friction(kneading: Kneading): Double = if (kneading == Kneading.MACHINE) 8.0 else 3.0

    /**
     * Wassertemperatur in °C für das Wasser, das zum Kneten dazukommt (mit Vorteig: das des Hauptteigs); null, wenn
     * dabei kein Wasser dazukommt. Kann unter 0 oder über 40 °C liegen; die Anzeige weist dann darauf hin.
     */
    fun temperature(result: DoughResult.Success, preferment: Preferment, kitchen: DoughKitchen, doughTemp: Double): Double? {
        val pre = result.sections.firstOrNull { it.kind == SectionKind.PREFERMENT }
        val main = result.sections.firstOrNull { it.kind == SectionKind.MAIN }
            ?: result.sections.firstOrNull { it.kind == SectionKind.PLAIN }
            ?: return null
        val mainFlour = main.lines.filter { it.role in FLOURS }.sumOf { it.grams }
        val mainWater = main.lines.filter { it.role == LineRole.WATER }.sumOf { it.grams }
        if (mainWater <= 0.0) return null
        val preFlour = pre?.lines?.filter { it.role in FLOURS }?.sumOf { it.grams } ?: 0.0
        val preWater = pre?.lines?.filter { it.role == LineRole.WATER }?.sumOf { it.grams } ?: 0.0
        val preTemp = if (preferment == Preferment.BIGA) kitchen.fridge else kitchen.room
        val flourHeat = mainFlour * FLOUR_HEAT
        val waterHeat = mainWater * WATER_HEAT
        val preHeat = preFlour * FLOUR_HEAT + preWater * WATER_HEAT
        val target = doughTemp - friction(kitchen.kneading)
        return (target * (flourHeat + waterHeat + preHeat) - flourHeat * kitchen.room - preHeat * preTemp) / waterHeat
    }
}
