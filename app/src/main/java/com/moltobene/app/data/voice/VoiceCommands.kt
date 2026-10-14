package com.moltobene.app.data.voice

/** Was die Spracheingabe in der Kochansicht auslösen kann (#80). */
enum class VoiceCommand {
    NEXT,
    PREVIOUS,
    REPEAT,
    READ,
    STOP,

    /** Mikrofon ausschalten („Mikro aus“, „Mikrofon aus“, „ausschalten“, „Ende“, „beenden“, „fertig“). */
    MICROPHONE_OFF,
}

/**
 * Die Befehle der Spracheingabe (#80) auf Deutsch und ihre Zuordnung. Die Erkennung kennt die Befehle und zusätzlich häufige
 * Alltagswörter als „Köder“ ([DECOY_WORDS]): Ohne sie presst eine Erkennung, die nur ein paar Wörter kennt, normales Gerede
 * auf die nächstliegenden Befehle (Test mit dem echten Modell: 9 von 20 Alltagssätzen lösten einen Befehl aus, mit Ködern
 * keiner). Ein Befehl zählt nur, wenn der **ganze** erkannte Satz genau dieser Befehl ist. Reines Kotlin, per Unit-Test
 * prüfbar.
 *
 * Die Wörter stehen hier und nicht in `strings.xml`: Sie hängen an der Sprache des Erkennungspakets, nicht an der
 * Oberfläche (wie die gesprochenen Wörter beim Vorlesen). Die Befehle und Köder liest auch der Sprachtest in
 * `.github/erkennungspaket/sprachtest.py` aus dieser Datei.
 */
object VoiceCommands {
    /** Sprechweise → Befehl, genau so, wie sie in der Grammatik der Erkennung steht. */
    private val GERMAN: Map<String, VoiceCommand> = linkedMapOf(
        "weiter" to VoiceCommand.NEXT,
        "zurück" to VoiceCommand.PREVIOUS,
        "wiederholen" to VoiceCommand.REPEAT,
        "vorlesen" to VoiceCommand.READ,
        "stopp" to VoiceCommand.STOP,
        "stop" to VoiceCommand.STOP,
        "halt" to VoiceCommand.STOP,
        "mikro aus" to VoiceCommand.MICROPHONE_OFF,
        "mikrofon aus" to VoiceCommand.MICROPHONE_OFF,
        "ausschalten" to VoiceCommand.MICROPHONE_OFF,
        "ende" to VoiceCommand.MICROPHONE_OFF,
        "beenden" to VoiceCommand.MICROPHONE_OFF,
        "fertig" to VoiceCommand.MICROPHONE_OFF,
    )

    /**
     * Häufige Alltags- und Küchenwörter, die die Erkennung kennen soll, damit normales Gerede nicht zum Befehl wird. Keines
     * davon darf selbst ein Befehlswort sein ([GERMAN]).
     */
    internal const val DECOY_WORDS: String = """
        ich du er sie es wir ihr und oder aber das die der den dem ein eine einen einem ist sind war hat haben wird kann
        muss soll will nicht kein mit ohne für von zu bei nach vor auf an in im am um aus dann wenn weil auch noch schon nur
        sehr mehr weniger viel wenig alle man mal gleich jetzt später heute morgen hier da dort wo wie was wann warum wer
        bitte danke ja nein gut schön lecker schnell langsam heiß kalt warm groß klein neu alt
        salz pfeffer zwiebel zwiebeln knoblauch öl butter mehl eier ei milch wasser zucker nudeln reis kartoffeln tomaten
        fleisch gemüse suppe soße pfanne topf herd ofen minuten stunde gramm esslöffel teelöffel kochen braten backen rühren
        schneiden mischen geben nehmen brauchen holen gehen kommen machen sagen sehen essen trinken einkaufen telefon licht
        tür fenster kind kinder hunger durst zeit tag abend mittag frühstück musik radio fernseher film nachrichten wetter
    """

    /** Der Platzhalter der Erkennung für alles, was weder Befehl noch Köder ist. */
    private const val UNKNOWN = "[unk]"

    /** Grammatik für die Erkennung als JSON-Liste, z. B. `["weiter", "zurück", …, "ich", …, "[unk]"]`. */
    val GERMAN_GRAMMAR: String = (GERMAN.keys + DECOY_WORDS.trim().split(Regex("\\s+")).distinct() + UNKNOWN)
        .joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

    /**
     * Der Befehl, wenn der **ganze** erkannte Text genau ein Befehl ist; sonst null. Groß-/Kleinschreibung und
     * überzählige Leerzeichen sind egal. Alles andere – auch ein Befehl mitten in einem Satz („wir gehen weiter
     * einkaufen“) – gilt als Gerede und löst nichts aus.
     */
    fun parse(text: String?): VoiceCommand? {
        val normalized = text?.trim()?.lowercase()?.replace(Regex("\\s+"), " ").orEmpty()
        return GERMAN[normalized]
    }
}
