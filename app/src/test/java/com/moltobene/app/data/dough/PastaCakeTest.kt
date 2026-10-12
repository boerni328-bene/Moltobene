package com.moltobene.app.data.dough

import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import com.moltobene.app.data.RecipeYield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Pasta und Kuchen im Teigrechner (#70): Beispiele zum Nachrechnen, ungültige Eingaben, Merken, Entwurf. */
class PastaCakeTest {

    private fun success(result: DoughResult) = result as DoughResult.Success

    private fun issues(result: DoughResult) = (result as DoughResult.Invalid).issues

    private fun DoughResult.Success.grams(role: LineRole): Double =
        sections.single().lines.firstOrNull { it.role == role }?.grams ?: 0.0

    private fun pasta(
        kind: PastaKind = PastaKind.EGG,
        portions: Int = 4,
        flour: Double = 100.0,
        semolina: Double = 0.0,
        egg: Double = 55.0,
        yolk: Double = 0.0,
        water: Double = 0.0,
        salt: Double = 1.0,
        oil: Double = 0.0,
    ) = PastaInput(kind, portions, flour, semolina, egg, yolk, water, salt, oil)

    private fun cake(kind: CakeKind, size: CakeSize? = null, change: (CakeInput) -> CakeInput = { it }) =
        CakeLimits.defaults(kind).let { change(if (size != null) it.copy(size = size) else it) }

    // ---------------------------------------------------------------- Pasta

    @Test
    fun eierteig() {
        val result = success(PastaCalculator.calculate(pasta()))
        assertEquals(400.0, result.flourGrams, 1e-9)
        assertEquals(220.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(4.0, result.grams(LineRole.SALT), 1e-9)
        assertEquals(624.0, result.totalGrams, 1e-9)
        assertEquals(DoughYield(4, YieldKind.PORTION, 100.0), result.yield)
        assertEquals(4, DoughConstants.eggsAbout(220.0, DoughConstants.EGG_GRAMS))
    }

    @Test
    fun nurEigelb() {
        val result = success(PastaCalculator.calculate(pasta(portions = 2, flour = 120.0, egg = 0.0, yolk = 60.0)))
        assertEquals(144.0, result.grams(LineRole.YOLK), 1e-9)
        assertEquals(386.4, result.totalGrams, 1e-9)
        assertEquals(7, DoughConstants.eggsAbout(144.0, DoughConstants.YOLK_GRAMS))
        assertEquals("2,5", DoughNumbers.grams(result.grams(LineRole.SALT), LineRole.SALT, "de"))
    }

    @Test
    fun wasserteigMitGriess() {
        val result = success(
            PastaCalculator.calculate(pasta(PastaKind.WATER, 3, 100.0, semolina = 100.0, egg = 0.0, water = 50.0, salt = 0.0)),
        )
        assertEquals(300.0, result.grams(LineRole.SEMOLINA), 1e-9)
        assertEquals(0.0, result.grams(LineRole.FLOUR), 0.0)
        assertEquals(150.0, result.grams(LineRole.WATER), 1e-9)
        assertEquals(450.0, result.totalGrams, 1e-9)
        // Mehl mit 0 g fehlt ganz.
        assertTrue(result.sections.single().lines.none { it.role == LineRole.FLOUR })
    }

    @Test
    fun wasserteigGemischt() {
        val result = success(
            PastaCalculator.calculate(pasta(PastaKind.WATER, 4, 120.0, semolina = 70.0, egg = 0.0, water = 45.0, salt = 1.0)),
        )
        assertEquals(336.0, result.grams(LineRole.SEMOLINA), 1e-9)
        assertEquals(144.0, result.grams(LineRole.FLOUR), 1e-9)
        assertEquals(216.0, result.grams(LineRole.WATER), 1e-9)
        assertEquals(700.8, result.totalGrams, 1e-9)
        assertEquals("5", DoughNumbers.grams(result.grams(LineRole.SALT), LineRole.SALT, "de"))
    }

    @Test
    fun eierteigMitGriess() {
        val result = success(PastaCalculator.calculate(pasta(portions = 2, semolina = 50.0)))
        assertEquals(100.0, result.grams(LineRole.SEMOLINA), 1e-9)
        assertEquals(100.0, result.grams(LineRole.FLOUR), 1e-9)
        assertEquals(110.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(312.0, result.totalGrams, 1e-9)
    }

    @Test
    fun pastaUngueltig() {
        // Zu wenig Flüssigkeit: Vollei 10 % + Eigelb 5 %.
        assertEquals(DoughIssue.TooLittleLiquid(30.0), issues(PastaCalculator.calculate(pasta(egg = 10.0, yolk = 5.0))).single())
        // Wasserteig mit 20 % Wasser ist zu trocken.
        assertTrue(issues(PastaCalculator.calculate(pasta(PastaKind.WATER, egg = 0.0, water = 20.0, semolina = 100.0))).single() is DoughIssue.OutOfRange)
        assertEquals(DoughIssue.OutOfRange(DoughField.COUNT, 1.0, 30.0), issues(PastaCalculator.calculate(pasta(portions = 0))).single())
        assertEquals(DoughIssue.NotANumber(DoughField.FLOUR_PER_PORTION), issues(PastaCalculator.calculate(pasta(flour = Double.NaN))).single())
        assertEquals(DoughIssue.NotANumber(DoughField.SALT), issues(PastaCalculator.calculate(pasta(salt = Double.POSITIVE_INFINITY))).single())
        // Beim Wasserteig gibt es kein Ei.
        assertTrue(PastaCalculator.calculate(pasta(PastaKind.WATER, egg = 55.0, water = 50.0, semolina = 100.0)) is DoughResult.Invalid)
    }

    @Test
    fun pastaMerken() {
        // Wasser 5 % im Wasserteig ist außerhalb: Startwert 50.
        val broken = PastaMemory.decode(PastaKind.WATER, "water=5;portions=6")
        assertEquals(50.0, broken.water, 0.0)
        assertEquals(6, broken.portions)
        val input = pasta(portions = 6, flour = 90.0, semolina = 30.0, egg = 40.0, yolk = 10.0, water = 5.0)
        assertEquals(input, PastaMemory.decode(PastaKind.EGG, PastaMemory.encode(input)))
        assertEquals(PastaLimits.defaults(PastaKind.EGG), PastaMemory.decode(PastaKind.EGG, null))
        assertEquals(PastaLimits.defaults(PastaKind.EGG), PastaMemory.decode(PastaKind.EGG, "Unsinn;=;a=b=c;egg=NaN;portions=1e99"))
        // Zusammen zu wenig Flüssigkeit: Startwerte.
        assertEquals(PastaLimits.defaults(PastaKind.EGG), PastaMemory.decode(PastaKind.EGG, "egg=10;yolk=5"))
    }

    // ---------------------------------------------------------------- Kuchen

    @Test
    fun ruehrteigRund26() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.BATTER)))
        assertEquals(250.0, result.flourGrams, 1e-9)
        assertEquals(200.0, result.grams(LineRole.BUTTER), 1e-9)
        assertEquals(200.0, result.grams(LineRole.SUGAR), 1e-9)
        assertEquals(200.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(50.0, result.grams(LineRole.MILK), 1e-9)
        assertEquals(7.5, result.grams(LineRole.BAKING_POWDER), 1e-9)
        assertEquals(1.0, result.grams(LineRole.SALT), 1e-9)
        assertEquals(908.5, result.totalGrams, 1e-9)
        assertEquals(DoughYield(1, YieldKind.PAN, Pan.round(26.0).area), result.yield)
    }

    @Test
    fun ruehrteigRund28FolgtDerFlaeche() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.BATTER, CakeSize.PanSize(Pan.round(28.0)))))
        // Der Faktor ist die Fläche, nicht 28 / 26.
        assertEquals(615.75 / 530.93, result.flourGrams / 250.0, 1e-4)
        assertEquals(290.0, result.flourGrams, 0.1)
    }

    @Test
    fun ruehrteigBlech() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.BATTER, CakeSize.PanSize(Pan.rectangle(30.0, 40.0)))))
        assertEquals(565.0, result.flourGrams, 0.5)
    }

    @Test
    fun biskuit() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.SPONGE)))
        assertEquals(115.0, result.flourGrams, 1e-9)
        assertEquals(115.0, result.grams(LineRole.SUGAR), 1e-9)
        assertEquals(210.45, result.grams(LineRole.EGG), 1e-9)
        assertEquals(440.45, result.totalGrams, 1e-9)
        assertEquals(4, DoughConstants.eggsAbout(result.grams(LineRole.EGG), DoughConstants.EGG_GRAMS))
        assertEquals("210", DoughNumbers.grams(result.grams(LineRole.EGG), LineRole.EGG, "de"))
    }

    @Test
    fun muerbeteig() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.SHORTCRUST)))
        assertEquals(130.0, result.grams(LineRole.BUTTER), 1e-9)
        assertEquals(70.0, result.grams(LineRole.SUGAR), 1e-9)
        assertEquals(55.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(456.0, result.totalGrams, 1e-9)
    }

    @Test
    fun hefezopfMitMehlDirekt() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.YEAST_SWEET, CakeSize.FlourGrams(500.0))))
        assertEquals(250.0, result.grams(LineRole.MILK), 1e-9)
        assertEquals(75.0, result.grams(LineRole.BUTTER), 1e-9)
        assertEquals(60.0, result.grams(LineRole.SUGAR), 1e-9)
        assertEquals(55.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(20.0, result.grams(LineRole.YEAST), 1e-9)
        assertEquals(5.0, result.grams(LineRole.SALT), 1e-9)
        assertEquals(965.0, result.totalGrams, 1e-9)
        assertEquals(YieldKind.DOUGH, result.yield.kind)
        // Trockenhefe (Instant): ein Drittel.
        assertEquals(20.0 / 3, YeastKind.FRESH.convert(20.0, YeastKind.INSTANT_DRY), 1e-9)
    }

    @Test
    fun brandteig() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.CHOUX)))
        assertEquals(150.0, result.flourGrams, 1e-9)
        assertEquals(250.5, result.grams(LineRole.LIQUID), 1e-6)
        assertEquals(75.0, result.grams(LineRole.BUTTER), 1e-9)
        assertEquals(217.5, result.grams(LineRole.EGG), 1e-9)
        assertEquals(1.5, result.grams(LineRole.SALT), 1e-9)
        assertEquals(694.5, result.totalGrams, 1e-6)
    }

    @Test
    fun quarkOelTeig() {
        val result = success(CakeCalculator.calculate(cake(CakeKind.QUARK_OIL)))
        assertEquals(300.0, result.flourGrams, 1e-9)
        assertEquals(150.0, result.grams(LineRole.QUARK), 1e-9)
        assertEquals(60.0, result.grams(LineRole.OIL), 1e-9)
        assertEquals(0.0, result.grams(LineRole.BUTTER), 0.0)
        assertEquals(75.0, result.grams(LineRole.SUGAR), 1e-9)
        assertEquals(60.0, result.grams(LineRole.MILK), 1e-9)
        assertEquals(54.0, result.grams(LineRole.EGG), 1e-9)
        assertEquals(15.0, result.grams(LineRole.BAKING_POWDER), 1e-9)
        assertEquals(0.9, result.grams(LineRole.SALT), 1e-9)
        assertEquals(714.9, result.totalGrams, 1e-9)
    }

    @Test
    fun kuchenUngueltig() {
        for (size in listOf(Pan.round(4.0), Pan.round(120.0), Pan.rectangle(100.0, 100.0), Pan.round(Double.NaN))) {
            val result = CakeCalculator.calculate(cake(CakeKind.BATTER, CakeSize.PanSize(size)))
            val found = issues(result)
            // Zu groß für den Faktor oder die Form selbst ist ungültig: nie ein Ergebnis.
            assertTrue(size.toString(), found.any { it === DoughIssue.PanInvalid || it is DoughIssue.OutOfRange })
        }
        assertTrue(issues(CakeCalculator.calculate(cake(CakeKind.YEAST_SWEET, CakeSize.FlourGrams(15.0)))).single() is DoughIssue.OutOfRange)
        assertTrue(issues(CakeCalculator.calculate(cake(CakeKind.SPONGE) { it.copy(egg = 50.0) })).single() is DoughIssue.OutOfRange)
        assertEquals(DoughIssue.NotANumber(DoughField.SUGAR), issues(CakeCalculator.calculate(cake(CakeKind.BATTER) { it.copy(sugar = Double.NaN) })).single())
        // Brandteig: höchstens 1.000 g Mehl.
        assertTrue(issues(CakeCalculator.calculate(cake(CakeKind.CHOUX, CakeSize.FlourGrams(1200.0)))).single() is DoughIssue.OutOfRange)
    }

    @Test
    fun kuchenMerken() {
        for (kind in CakeKind.entries) {
            val input = CakeLimits.defaults(kind)
            assertEquals(kind.key, input, CakeMemory.decode(kind, CakeMemory.encode(input)))
        }
        val own = cake(CakeKind.BATTER, CakeSize.PanSize(Pan.rectangle(30.0, 40.0))) { it.copy(sugar = 60.0, egg = 90.0) }
        assertEquals(own, CakeMemory.decode(CakeKind.BATTER, CakeMemory.encode(own)))
        val defaults = CakeLimits.defaults(CakeKind.BATTER)
        assertEquals(defaults, CakeMemory.decode(CakeKind.BATTER, null))
        assertEquals(defaults, CakeMemory.decode(CakeKind.BATTER, "Unsinn;=;sugar=NaN;egg=1e99;size=pan;round=1;a=4;b=4"))
        // Eine Form mit zu viel Mehl für die Art fällt auf die Startgröße zurück.
        assertEquals(defaults.size, CakeMemory.decode(CakeKind.BATTER, "size=pan;round=0;a=100;b=100").size)
    }

    // ---------------------------------------------------------------- Entwurf

    @Test
    fun kuchenEntwurfWirdVonDerBackformUmrechnungGelesen() {
        for (language in listOf("de", "en")) {
            for (size in listOf(Pan.round(26.0), Pan.round(28.5), Pan.rectangle(30.0, 40.0))) {
                val input = cake(CakeKind.BATTER, CakeSize.PanSize(size))
                val draft = DoughDraft.fromCake(success(CakeCalculator.calculate(input)), input, GermanTexts, language)
                val parsed = PanScaling.parse(draft.servingsUnit)
                assertNotNull("${draft.servingsUnit} ($language)", parsed)
                assertEquals(size.area, parsed!!.first.area, size.area * 0.001)
                assertEquals(1, draft.servings)
            }
        }
    }

    @Test
    fun pastaEntwurf() {
        val input = pasta(PastaKind.WATER, 3, 100.0, semolina = 70.0, egg = 0.0, water = 50.0)
        val draft = DoughDraft.fromPasta(success(PastaCalculator.calculate(input)), input, GermanTexts, "de")
        assertEquals(
            listOf("210 g Hartweizengrieß (70 %)", "90 g Mehl (30 %)", "150 g Wasser (50 %)", "3 g Salz (1 %)"),
            draft.ingredients.map { it.text },
        )
        assertEquals(3, draft.servings)
        assertEquals("Portionen (je 100 g Mehl)", draft.servingsUnit)
        val parsed = RecipeYield.parse("${draft.servings} ${draft.servingsUnit}")
        assertEquals(3, parsed?.count)
    }

    @Test
    fun hefeteigEntwurfMitDoughEinheit() {
        val input = cake(CakeKind.YEAST_SWEET, CakeSize.FlourGrams(500.0))
        val draft = DoughDraft.fromCake(success(CakeCalculator.calculate(input)), input, GermanTexts, "de")
        assertEquals("Teig", draft.servingsUnit)
        assertTrue(draft.ingredients.first().text.startsWith("500 g Mehl"))
    }

    // ---------------------------------------------------------------- Eigenschaften

    /** Je 2.000 gültige Eingaben: Summe, Vorzeichen, Mehlanteile stimmen immer. */
    @Test
    fun eigenschaftenBeiVielenEingaben() {
        val random = Random(20261012)
        fun between(range: DoughLimits.Range) = range.min + random.nextDouble() * (range.max - range.min)
        repeat(2000) {
            val kind = PastaKind.entries[random.nextInt(PastaKind.entries.size)]
            val input = PastaInput(
                kind, random.nextInt(1, 31), between(PastaLimits.range(kind, DoughField.FLOUR_PER_PORTION)),
                between(PastaLimits.range(kind, DoughField.SEMOLINA)), between(PastaLimits.range(kind, DoughField.WHOLE_EGG)),
                between(PastaLimits.range(kind, DoughField.YOLK)), between(PastaLimits.range(kind, DoughField.WATER)),
                between(PastaLimits.range(kind, DoughField.SALT)), between(PastaLimits.range(kind, DoughField.OIL)),
            )
            when (val result = PastaCalculator.calculate(input)) {
                is DoughResult.Invalid -> assertTrue(input.toString(), result.issues.all { it is DoughIssue.TooLittleLiquid })
                is DoughResult.Success -> {
                    val lines = result.sections.flatMap { it.lines }
                    assertTrue(lines.all { it.grams.isFinite() && it.grams >= 0 })
                    assertEquals(result.totalGrams, lines.sumOf { it.grams }, 1e-6)
                    val flour = lines.filter { it.role == LineRole.FLOUR || it.role == LineRole.SEMOLINA }.sumOf { it.grams }
                    assertEquals(result.flourGrams, flour, 1e-6)
                }
            }
        }
        repeat(2000) {
            val kind = CakeKind.entries[random.nextInt(CakeKind.entries.size)]
            var input = CakeLimits.defaults(kind)
            for (field in CakeLimits.fields(kind)) input = CakeLimits.withValue(input, field, between(CakeLimits.range(kind, field)))
            input = input.copy(
                size = if (random.nextBoolean()) {
                    CakeSize.FlourGrams(between(CakeLimits.flourRange(kind)))
                } else {
                    CakeSize.PanSize(Pan.round(5 + random.nextDouble() * 60))
                },
            )
            when (val result = CakeCalculator.calculate(input)) {
                is DoughResult.Invalid ->
                    // Ungültig nur wegen der Mehlmenge aus der Form.
                    assertTrue(input.toString(), result.issues.all { it is DoughIssue.OutOfRange && it.field == DoughField.FLOUR_GRAMS })
                is DoughResult.Success -> {
                    val lines = result.sections.flatMap { it.lines }
                    assertTrue(lines.all { it.grams.isFinite() && it.grams >= 0 })
                    assertEquals(result.totalGrams, lines.sumOf { it.grams }, 1e-6)
                    assertEquals(result.flourGrams, lines.first { it.role == LineRole.FLOUR }.grams, 1e-9)
                }
            }
        }
    }
}
