package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Teigrechner (#69): feste Beispiele zum Nachrechnen und Eigenschaftstests mit festem Startwert. */
class DoughCalculatorTest {

    private fun pizza(
        count: Int = 4,
        grams: Double = 250.0,
        water: Double = 65.0,
        salt: Double = 3.0,
        oil: Double = 0.0,
        yeast: Double = 0.2,
        kind: YeastKind = YeastKind.FRESH,
        preferment: Preferment = Preferment.NONE,
        prefermentFlour: Double = 30.0,
        prefermentWater: Double = 100.0,
        type: DoughType = DoughType.PIZZA,
    ) = LeavenedInput(type, count, grams, water, salt, oil, kind, yeast, preferment, prefermentFlour, prefermentWater)

    private fun success(input: LeavenedInput) = DoughCalculator.calculate(input) as DoughResult.Success

    private fun issues(input: LeavenedInput) = (DoughCalculator.calculate(input) as DoughResult.Invalid).issues

    private fun DoughResult.Success.grams(section: SectionKind, role: LineRole): Double =
        sections.first { it.kind == section }.lines.firstOrNull { it.role == role }?.grams ?: 0.0

    @Test
    fun p1PizzaOhneVorteig() {
        val result = success(pizza())
        assertEquals(594.53, result.flourGrams, 0.01)
        assertEquals(386.45, result.grams(SectionKind.PLAIN, LineRole.WATER), 0.01)
        assertEquals(17.84, result.grams(SectionKind.PLAIN, LineRole.SALT), 0.01)
        assertEquals(1.19, result.grams(SectionKind.PLAIN, LineRole.YEAST), 0.01)
        assertEquals(1000.0, result.totalGrams, 0.0)
        assertEquals(DoughYield(4, YieldKind.BALL, 250.0), result.yield)
    }

    @Test
    fun p2PizzaMitBiga() {
        val result = success(
            pizza(count = 6, water = 60.0, salt = 2.5, yeast = 0.1, preferment = Preferment.BIGA, prefermentFlour = 40.0, prefermentWater = 45.0),
        )
        assertEquals(922.51, result.flourGrams, 0.01)
        assertEquals(369.0, result.grams(SectionKind.PREFERMENT, LineRole.FLOUR), 0.01)
        assertEquals(166.05, result.grams(SectionKind.PREFERMENT, LineRole.WATER), 0.01)
        assertEquals(0.92, result.grams(SectionKind.PREFERMENT, LineRole.YEAST), 0.01)
        assertEquals(553.51, result.grams(SectionKind.MAIN, LineRole.FLOUR), 0.01)
        assertEquals(387.45, result.grams(SectionKind.MAIN, LineRole.WATER), 0.01)
        assertEquals(23.06, result.grams(SectionKind.MAIN, LineRole.SALT), 0.01)
        // Die Hefe steckt ganz im Vorteig.
        assertEquals(0.0, result.grams(SectionKind.MAIN, LineRole.YEAST), 0.0)
    }

    @Test
    fun p3PizzaMitPoolishUndOel() {
        val result = success(
            pizza(
                count = 2, grams = 300.0, water = 70.0, salt = 2.0, oil = 3.0, yeast = 0.5, kind = YeastKind.INSTANT_DRY,
                preferment = Preferment.POOLISH, prefermentFlour = 30.0,
            ),
        )
        assertEquals(341.88, result.flourGrams, 0.01)
        assertEquals(102.56, result.grams(SectionKind.PREFERMENT, LineRole.FLOUR), 0.01)
        assertEquals(102.56, result.grams(SectionKind.PREFERMENT, LineRole.WATER), 0.01)
        assertEquals(1.71, result.grams(SectionKind.PREFERMENT, LineRole.YEAST), 0.01)
        assertEquals(239.32, result.grams(SectionKind.MAIN, LineRole.FLOUR), 0.01)
        assertEquals(136.75, result.grams(SectionKind.MAIN, LineRole.WATER), 0.01)
        assertEquals(6.84, result.grams(SectionKind.MAIN, LineRole.SALT), 0.01)
        assertEquals(10.26, result.grams(SectionKind.MAIN, LineRole.OIL), 0.01)
    }

    @Test
    fun b1BrotMitSauerteig() {
        val result = success(
            pizza(
                type = DoughType.BREAD, count = 1, grams = 900.0, water = 70.0, salt = 2.0, yeast = 0.0,
                preferment = Preferment.SOURDOUGH, prefermentFlour = 20.0,
            ),
        )
        assertEquals(523.26, result.flourGrams, 0.01)
        assertEquals(104.65, result.grams(SectionKind.PREFERMENT, LineRole.FLOUR), 0.01)
        assertEquals(104.65, result.grams(SectionKind.PREFERMENT, LineRole.WATER), 0.01)
        assertEquals(418.60, result.grams(SectionKind.MAIN, LineRole.FLOUR), 0.01)
        assertEquals(261.63, result.grams(SectionKind.MAIN, LineRole.WATER), 0.01)
        assertEquals(10.47, result.grams(SectionKind.MAIN, LineRole.SALT), 0.01)
        assertEquals(DoughYield(1, YieldKind.LOAF, 900.0), result.yield)
        // Das Wasser im Anstellgut zählt: Gesamtwasser = 70 % vom Mehl.
        val water = result.grams(SectionKind.PREFERMENT, LineRole.WATER) + result.grams(SectionKind.MAIN, LineRole.WATER)
        assertEquals(0.70, water / result.flourGrams, 1e-9)
    }

    @Test
    fun unmoeglicheKombinationGibtEineMeldung() {
        // Poolish aus 80 % des Mehls braucht 80 % Wasser vom Gesamtmehl, erlaubt sind nur 65 %.
        val issue = issues(pizza(preferment = Preferment.POOLISH, prefermentFlour = 80.0)).single() as DoughIssue.PrefermentWaterTooHigh
        assertEquals(80.0, issue.neededPercent, 1e-9)
        assertEquals(65.0, issue.totalPercent, 1e-9)
    }

    @Test
    fun grenzenUndUngueltigeWerte() {
        assertEquals(DoughIssue.OutOfRange(DoughField.COUNT, 1.0, 50.0), issues(pizza(count = 0)).single())
        assertEquals(DoughIssue.OutOfRange(DoughField.COUNT, 1.0, 50.0), issues(pizza(count = 51)).single())
        assertTrue(issues(pizza(grams = 49.0)).single() is DoughIssue.OutOfRange)
        assertTrue(issues(pizza(water = 44.9)).single() is DoughIssue.OutOfRange)
        assertTrue(issues(pizza(salt = -1.0)).single() is DoughIssue.OutOfRange)
        assertTrue(issues(pizza(yeast = 0.0)).single() is DoughIssue.OutOfRange)
        assertEquals(DoughIssue.NotANumber(DoughField.WATER), issues(pizza(water = Double.NaN)).single())
        assertEquals(DoughIssue.NotANumber(DoughField.SALT), issues(pizza(salt = Double.POSITIVE_INFINITY)).single())
        assertEquals(DoughIssue.NotANumber(DoughField.PIECE_GRAMS), issues(pizza(grams = Double.NaN)).single())
        // Gemerkte Werte wie 1e99 sind einfach außerhalb.
        assertTrue(issues(pizza(grams = 1e99)).single() is DoughIssue.OutOfRange)
        // Mit Sauerteig darf die Hefe fehlen, sonst nicht.
        success(pizza(yeast = 0.0, preferment = Preferment.SOURDOUGH, prefermentFlour = 20.0))
        // Das Äußerste bei Brot: 20 Stück zu 3 kg sind 60 kg, erlaubt.
        success(pizza(type = DoughType.BREAD, count = 20, grams = 3000.0, water = 70.0, salt = 2.0, yeast = 0.5))
    }

    @Test
    fun hefeartenUmrechnen() {
        assertEquals(7.0, YeastKind.FRESH.convert(21.0, YeastKind.INSTANT_DRY), 1e-9)
        assertEquals(8.4, YeastKind.FRESH.convert(21.0, YeastKind.ACTIVE_DRY), 1e-9)
        assertEquals(21.0, YeastKind.INSTANT_DRY.convert(7.0, YeastKind.FRESH), 1e-9)
    }

    /** 2.000 gültige Eingaben: Summe, Mehl, Wasser und Vorzeichen stimmen immer. */
    @Test
    fun eigenschaftenBeiVielenEingaben() {
        val random = Random(20261011)
        repeat(2000) {
            val type = if (random.nextBoolean()) DoughType.PIZZA else DoughType.BREAD
            val preferment = Preferment.entries[random.nextInt(Preferment.entries.size)]
            fun between(range: DoughLimits.Range) = range.min + random.nextDouble() * (range.max - range.min)
            val input = LeavenedInput(
                type = type,
                count = random.nextInt(1, 6),
                pieceGrams = between(DoughLimits.range(type, DoughField.PIECE_GRAMS)),
                water = between(DoughLimits.range(type, DoughField.WATER)),
                salt = between(DoughLimits.range(type, DoughField.SALT)),
                oil = between(DoughLimits.range(type, DoughField.OIL)),
                yeastKind = YeastKind.entries[random.nextInt(YeastKind.entries.size)],
                yeast = between(DoughLimits.range(type, DoughField.YEAST, preferment)),
                preferment = preferment,
                prefermentFlour = between(DoughLimits.range(type, DoughField.PREFERMENT_FLOUR)),
                prefermentWater = between(DoughLimits.range(type, DoughField.PREFERMENT_WATER)),
            )
            when (val result = DoughCalculator.calculate(input)) {
                is DoughResult.Invalid -> {
                    // Ungültig nur wegen des Vorteigs, nie wegen etwas anderem.
                    assertTrue(input.toString(), result.issues.all { it is DoughIssue.PrefermentWaterTooHigh })
                    assertTrue(input.preferment != Preferment.NONE)
                }
                is DoughResult.Success -> {
                    val lines = result.sections.flatMap { it.lines }
                    assertTrue(lines.all { it.grams.isFinite() && it.grams >= 0 })
                    assertEquals(input.toString(), result.totalGrams, lines.sumOf { it.grams }, 1e-6)
                    val flour = lines.filter { it.role == LineRole.FLOUR }.sumOf { it.grams }
                    assertEquals(result.flourGrams, flour, 1e-6)
                    val water = lines.filter { it.role == LineRole.WATER }.sumOf { it.grams }
                    assertEquals(input.water / 100, water / flour, 1e-9)
                    val salt = lines.filter { it.role == LineRole.SALT }.sumOf { it.grams }
                    assertEquals(input.salt / 100, salt / flour, 1e-9)
                }
            }
        }
    }

    @Test
    fun weitereZutatenRechnenMitUndLiegenImHauptteig() {
        // Backmalz 1 %, Joghurt 25 %, Zucker 4 % und Milch 20 % kommen zum Mehl-Nenner dazu: F = T ÷ (1 + Summe der Anteile).
        val input = pizza(count = 1, grams = 1000.0, water = 65.0, salt = 2.0, yeast = 1.0, type = DoughType.BREAD)
            .copy(malt = 1.0, yogurt = 25.0, sugar = 4.0, milk = 20.0)
        val result = success(input)
        val flour = 1000.0 / (1 + 0.65 + 0.02 + 0.01 + 0.01 + 0.25 + 0.04 + 0.20)
        assertEquals(flour, result.flourGrams, 1e-9)
        assertEquals(flour * 0.01, result.grams(SectionKind.PLAIN, LineRole.MALT), 1e-9)
        assertEquals(flour * 0.25, result.grams(SectionKind.PLAIN, LineRole.YOGURT), 1e-9)
        assertEquals(flour * 0.04, result.grams(SectionKind.PLAIN, LineRole.SUGAR), 1e-9)
        assertEquals(flour * 0.20, result.grams(SectionKind.PLAIN, LineRole.MILK), 1e-9)
        assertEquals(1000.0, result.sections.flatMap { it.lines }.sumOf { it.grams }, 1e-6)
        assertEquals(DoughMemory.decode(DoughType.BREAD, DoughMemory.encode(input)), input)
    }

    @Test
    fun weitereZutatenAusserhalbDerGrenzenRechnenNicht() {
        assertTrue(issues(pizza(type = DoughType.BREAD).copy(malt = 6.0)).single() is DoughIssue.OutOfRange)
        assertTrue(issues(pizza().copy(yogurt = 61.0)).single() is DoughIssue.OutOfRange)
    }

    @Test
    fun mehlsortenTeilenDasMehlAufUndBrauchenMehrWasser() {
        val input = pizza(count = 1, grams = 1000.0, water = 70.0, salt = 2.0, yeast = 1.0, type = DoughType.BREAD)
            .copy(rye = 30.0, whole = 20.0, honey = 3.0, egg = 10.0, seeds = 15.0)
        val result = success(input)
        val flour = result.flourGrams
        assertEquals(flour * 0.5, result.grams(SectionKind.PLAIN, LineRole.WHEAT), 1e-9)
        assertEquals(flour * 0.3, result.grams(SectionKind.PLAIN, LineRole.RYE), 1e-9)
        assertEquals(flour * 0.2, result.grams(SectionKind.PLAIN, LineRole.WHOLE), 1e-9)
        assertEquals(0.0, result.grams(SectionKind.PLAIN, LineRole.FLOUR), 0.0)
        assertEquals(flour * 0.03, result.grams(SectionKind.PLAIN, LineRole.HONEY), 1e-9)
        assertEquals(flour * 0.10, result.grams(SectionKind.PLAIN, LineRole.EGG), 1e-9)
        assertEquals(flour * 0.15, result.grams(SectionKind.PLAIN, LineRole.SEEDS), 1e-9)
        assertEquals(1000.0, result.sections.flatMap { it.lines }.sumOf { it.grams }, 1e-6)
        assertEquals(input, DoughMemory.decode(DoughType.BREAD, DoughMemory.encode(input)))
    }

    @Test
    fun mehlsortenMitVorteigTeilenAuchDenVorteigAuf() {
        val input = pizza(count = 1, grams = 1000.0, water = 80.0, type = DoughType.BREAD, preferment = Preferment.SOURDOUGH, prefermentFlour = 30.0)
            .copy(rye = 80.0)
        val result = success(input)
        assertEquals(result.flourGrams * 0.3 * 0.8, result.grams(SectionKind.PREFERMENT, LineRole.RYE), 1e-9)
        assertEquals(result.flourGrams * 0.7 * 0.2, result.grams(SectionKind.MAIN, LineRole.WHEAT), 1e-9)
    }

    @Test
    fun zuvielMehlMischungRechnetNicht() {
        val input = pizza(type = DoughType.BREAD).copy(rye = 60.0, whole = 30.0, spelt = 20.0)
        assertTrue(issues(input).single() is DoughIssue.FlourMixTooHigh)
    }

    @Test
    fun wasserVorschlagFuerDieMehlmischung() {
        val base = pizza(type = DoughType.BREAD)
        assertEquals(0.0, DoughFlours.waterShift(base), 0.0)
        assertEquals(3.5, DoughFlours.waterShift(base.copy(rye = 30.0)), 0.0)
        assertEquals(8.0, DoughFlours.waterShift(base.copy(whole = 100.0)), 0.0)
        assertEquals(-3.0, DoughFlours.waterShift(base.copy(spelt = 100.0)), 0.0)
        assertEquals(0.0, DoughFlours.waterShift(base.copy(rye = Double.NaN)), 0.0)
    }
}
