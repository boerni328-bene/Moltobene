package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Timer beim Kochen: Zeitangaben in Schritten finden, in Sekunden. */
class StepTimesTest {

    private fun find(step: String, language: String? = "de") = StepTimes.find(step, language)

    @Test
    fun minutenStundenSekunden() {
        assertEquals(listOf(40 * 60), find("Im vorgeheizten Ofen etwa 40 Minuten backen."))
        assertEquals(listOf(20 * 60), find("Tomaten würfeln, dazugeben und 20 Minuten offen köcheln lassen."))
        assertEquals(listOf(5 * 60), find("5 Min. ruhen lassen."))
        assertEquals(listOf(3600), find("Den Teig 1 Stunde gehen lassen."))
        assertEquals(listOf(2 * 3600), find("Zugedeckt 2 Std. schmoren."))
        assertEquals(listOf(30), find("Mit dem Stabmixer 30 Sekunden pürieren."))
        assertEquals(listOf(25 * 60), find("Bake for 25 minutes until golden.", "en"))
        assertEquals(listOf(3 * 3600), find("Simmer for 3 hrs.", "en"))
        assertEquals(listOf(10 * 60), find("Cuocere per 10 minuti.", "it"))
        assertEquals(listOf(3600), find("Laisser reposer 1 heure.", "fr"))
        assertEquals(listOf(15 * 60), find("Hornear durante 15 minutos.", "es"))
    }

    @Test
    fun brücheUndKommazahlen() {
        assertEquals(listOf(90 * 60), find("1,5 Stunden schmoren."))
        assertEquals(listOf(90 * 60), find("Braise for 1.5 hours.", "en"))
        assertEquals(listOf(90 * 60), find("1 ½ Std. im Ofen garen."))
        assertEquals(listOf(90 * 60), find("1½ Stunden gehen lassen."))
        assertEquals(listOf(90 * 60), find("Roast for 1 1/2 hours.", "en"))
        assertEquals(listOf(30 * 60), find("½ Stunde ziehen lassen."))
    }

    @Test
    fun spanneNimmtDieKürzereZeit() {
        assertEquals(listOf(20 * 60), find("20–25 Minuten backen."))
        assertEquals(listOf(20 * 60), find("20-25 Min. backen."))
        assertEquals(listOf(20 * 60), find("20 bis 25 Minuten backen."))
        assertEquals(listOf(3600), find("1-2 Stunden marinieren."))
        assertEquals(listOf(10 * 60), find("Bake for 10 to 12 minutes.", "en"))
        assertEquals(listOf(10 * 60), find("Cocinar de 10 a 15 minutos.", "es"))
    }

    @Test
    fun stundenUndMinutenZusammen() {
        assertEquals(listOf(90 * 60), find("1 Stunde 30 Minuten backen."))
        assertEquals(listOf(75 * 60), find("1 Std. 15 Min. backen."))
        assertEquals(listOf(75 * 60), find("1 Std., 15 Min. backen."))
        assertEquals(listOf(80 * 60), find("Simmer for 1 hour and 20 minutes.", "en"))
        assertEquals(listOf(90 * 60), find("Cuire 1h30 à feu doux.", "fr"))
        assertEquals(listOf(135 * 60), find("2 h 15 garen."))
        assertEquals(listOf(90 * 60), find("1h30min backen."))
        assertEquals(listOf(90), find("1 Minute 30 Sekunden rühren."))
    }

    @Test
    fun zahlwörterUndAusdrücke() {
        assertEquals(listOf(3600), find("Eine Stunde ruhen lassen."))
        assertEquals(listOf(10 * 60), find("Noch zehn Minuten köcheln."))
        assertEquals(listOf(90 * 60), find("Anderthalb Stunden schmoren."))
        assertEquals(listOf(30 * 60), find("Eine halbe Stunde quellen lassen."))
        assertEquals(listOf(30 * 60), find("Nach einer halben Stunde umrühren."))
        assertEquals(listOf(15 * 60), find("Eine Viertelstunde ziehen lassen."))
        assertEquals(listOf(45 * 60), find("Eine Dreiviertelstunde backen."))
        assertEquals(listOf(3600), find("Bake for an hour.", "en"))
        assertEquals(listOf(60), find("Stir for a minute.", "en"))
        assertEquals(listOf(30 * 60), find("Rest for half an hour.", "en"))
        assertEquals(listOf(90 * 60), find("Braise for an hour and a half.", "en"))
        assertEquals(listOf(15 * 60), find("Leave for a quarter of an hour.", "en"))
        assertEquals(listOf(5 * 60), find("Simmer for five minutes.", "en"))
    }

    @Test
    fun mehrereZeitenImSchritt() {
        assertEquals(
            listOf(25 * 60, 10 * 60),
            find("Auf einem Blech 25 Minuten rösten, Feta darüberbröseln und weitere 10 Minuten backen."),
        )
        // Dieselbe Dauer nur einmal.
        assertEquals(listOf(15 * 60), find("15 Minuten kochen, umrühren und noch einmal 15 Minuten kochen."))
    }

    @Test
    fun keineZeit() {
        assertEquals(emptyList<Int>(), find(""))
        assertEquals(emptyList<Int>(), find("Zwiebel und Knoblauch fein hacken und im Olivenöl glasig dünsten."))
        assertEquals(emptyList<Int>(), find("Den Ofen auf 180 °C vorheizen."))
        assertEquals(emptyList<Int>(), find("Ein paar Minuten ziehen lassen."))
        assertEquals(emptyList<Int>(), find("Wait a few minutes.", "en"))
        assertEquals(emptyList<Int>(), find("Jede Minute umrühren."))
        assertEquals(emptyList<Int>(), find("3 H-Eier verquirlen."))
        assertEquals(emptyList<Int>(), find("Im 10-Minuten-Takt umrühren."))
        assertEquals(emptyList<Int>(), find("5 Minzblätter und 2 Hände voll Spinat dazugeben."))
        assertEquals(emptyList<Int>(), find("Über Nacht im Kühlschrank ruhen lassen."))
    }

    @Test
    fun längerAlsEinTagIstKeinTimer() {
        assertEquals(emptyList<Int>(), find("48 Stunden marinieren."))
        assertEquals(listOf(24 * 3600), find("24 Stunden marinieren."))
    }
}
