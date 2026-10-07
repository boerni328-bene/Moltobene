package com.moltobene.app.tour

import com.moltobene.app.data.translate.TranslationEngine

/**
 * Nachgestellte Übersetzung für den Rundgang (#60): Das echte Sprachpaket ist zu groß für den Emulator und käme aus
 * dem Internet. Die deutschen Sätze zum Beispielrezept „Lemon drizzle cake“ sind selbst geschrieben. Die App setzt
 * wie beim echten Übersetzer vorher das Küchenwörterbuch ein und prüft danach mit den Schutzregeln.
 */
object SampleTranslation {
    const val TITLE = "Lemon drizzle cake"
    const val FIRST_INGREDIENT = "200 g soft butter"
    const val FIRST_INGREDIENT_DE = "200 g weiche Butter"

    private val EN_DE = mapOf(
        TITLE to "Zitronenkuchen mit Guss",
        FIRST_INGREDIENT to FIRST_INGREDIENT_DE,
        "200 g sugar" to "200 g Zucker",
        "3 eggs" to "3 Eier",
        "2 tbsp milk" to "2 EL Milch",
        "Abrieb von 2 lemons" to "Abrieb von 2 Zitronen",
        "For the drizzle:" to "Für den Guss:",
        "juice of 2 lemons" to "Saft von 2 Zitronen",
        "Heat the oven to 180 °C and line a Kastenform with baking paper." to
            "Den Ofen auf 180 °C vorheizen und eine Kastenform mit Backpapier auslegen.",
        "Beat butter and sugar until pale, then add the eggs one by one." to
            "Butter und Zucker hell schlagen, dann die Eier einzeln unterrühren.",
        "Fold in flour, milk and lemon zest, fill the tin and bake for about 50 minutes." to
            "Mehl, Milch und Zitronenabrieb unterheben, in die Form füllen und etwa 50 Minuten backen.",
        "Stir lemon juice and Puderzucker together and pour over the warm cake." to
            "Zitronensaft und Puderzucker verrühren und über den warmen Kuchen gießen.",
    )

    /** @param available false stellt nach, dass das Sprachpaket noch nicht geladen ist. */
    fun engine(available: Boolean = true): TranslationEngine = object : TranslationEngine {
        override fun isAvailable(): Boolean = available

        override fun open(direction: String): TranslationEngine.Session = object : TranslationEngine.Session {
            override fun translate(text: String): String {
                // Etwas Zeit pro Zeile wie auf einem Handy, damit der Fortschritt zu sehen ist.
                Thread.sleep(LINE_MILLIS)
                return if (direction == "en-de") EN_DE[text] ?: text else text
            }

            override fun close() = Unit
        }
    }

    private const val LINE_MILLIS = 150L
}
