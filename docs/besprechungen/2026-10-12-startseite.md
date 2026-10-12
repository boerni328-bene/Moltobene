# Besprechung vom 12.10.2026: Startseite mit Kacheln

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen, Marketing
**Moderation:** Claude · **App-Version:** 0.32.0

## Anlass (Projektinhaber, wörtlich, nach Test von 0.31.0)

> „Der Teig-Rechner ist nun bei Hinzufügen als Unterpunkt enthalten. Beim Öffnen der App sollte nicht gleich die Rezeptliste erscheinen. Zuerst hätte ich gerne die Auswahl Rezepte, Teigrechner, usw. (Saison-Produkte, Tips & Tricks, Ideen oder Landes-Rezepte...).“

Vorab entschieden: erst Besprechung; Inhalte „beides, Schlagwörter zuerst“ (Länder und Saison als Schlagwörter über die eigenen Rezepte, später mitgelieferte selbst geschriebene Inhalte).

## Einigkeit

1. **Startseite ja, klein und fertig:** neue `HomeRoute` als Start; erste Fassung mit den Kacheln **Sammlung/Rezepte** (mit Anzahl) und **Teigrechner**, dazu Hinzufügen, Übernehmen und Einstellungen als Symbole oben, „MOLTOBENE“ oben. Nur Kacheln zeigen, die etwas leisten; keine leeren oder „bald“-Kacheln (Fokus).
2. **Einstellung „Beim Öffnen der App: Startseite / Sammlung“,** Standard Startseite; Merkwert in `AppPreferences` (nicht in der Sicherung), beim Start im Hintergrund gelesen.
3. **Zurück:** Sammlung → Zurück → Startseite → Zurück beendet die App; nach Speichern zurück zur Sammlung. Teilen/Links (`EditRoute(fromShare)`) bleiben unverändert und landen danach richtig; Rundgang-Test bei Kaltstart.
4. **Startseite schnell:** ohne Datenbank und ohne Fotos; Zahlen erst nach dem ersten Bild als eine Abfrage; keine Fotos auf Kacheln in der ersten Fassung; die Liste wird nicht im Hintergrund vorgewärmt.
5. **Länder und Saison** hängen an **#57** (Schlagwörter zuweisen, Filter, Kennungen): feste englische Kennungen, Anzeige aus `strings.xml`; Kachel öffnet die Sammlung gefiltert; leerer Zustand mit Hinweis, wie ein Schlagwort vergeben wird. Wenige Länder (etwa 8–12), Saison als vier Jahreszeiten. Es heißt immer „Schlagwort“, nie „Kategorie“.
6. **Saison:** kein Standort; die Halbkugel wird nie erraten (entweder fest „Mitteleuropa“ ehrlich benannt oder von Hand gewählt); Datum höchstens als Vorauswahl, nie als Hintergrundrechnung.
7. **Später mitgelieferte Inhalte** (Saisonkalender, Tipps & Tricks, Ideen): erst nach 1.0, ein Inhaltstyp pro Version, selbst geschrieben und gegengelesen in DE/EN, Herkunft im Repository festgehalten, als Datei in der App (JSON in `assets/`, einige KB), kein Download; nicht in Sicherung oder Datenbank; Tipps zu Lebensmittelsicherheit besonders vorsichtig.
8. **Keine neue Berechtigung, Verbindung oder Intent-Filter.**
9. **Schlagwörter beim Teilen/Übernehmen:** Land als schema.org `recipeCuisine`, Saison als `keywords`; beim Übernehmen nur genau passende Werte einer mitgelieferten Liste zuordnen (Länge/Anzahl begrenzen).

## Streitpunkte

- **Kachelname:** „Sammlung“ (fester Begriff; Texte) oder „Rezepte“ (Wunsch; Übersetzungen, Marketing; Marketing: „Meine Rezepte aus Italien“ grenzt gegen Inhalte-Apps ab).
- **Länder oder Küchen:** „Italien“ (neutraler, Übersetzungen) oder „Italienische Küche“.
- **Zusatzkacheln aus Kreativität:** „Zuletzt gekocht“ (braucht neues Datenfeld und Migration; Funktionalität/Sicherheit dagegen, höchstens „zuletzt geöffnet“), „Was koche ich heute?“ (Zufall im eigenen Bestand, Favoriten bevorzugt) – nach der ersten Fassung.
- **Teigrechner:** nur noch auf der Startseite oder zusätzlich im Hinzufügen-Menü (und Weg aus der Sammlung)?
- **Startseite oben:** nur „MOLTOBENE“ oder zusätzlich „Start“ als Bildschirmname?

## Richtwerte (Performance, Vorschlag, erst nach Messung festlegen)
Startseite sichtbar unter 1 s; Zahlen unter 0,5 s mit 1.000 Rezepten; Startseite höchstens ~5 MB zusätzlicher Speicher; Zurück zur Startseite ohne Neuaufbau der Liste. Rundgang und Prüflauf (#52) müssen angepasst werden.

## Reihenfolge (Empfehlung)
1. Startseite mit Sammlung + Teigrechner + Einstellung „Beim Öffnen der App“ (klein, bald).
2. #57 Schlagwörter und Filter (Voraussetzung für Länder/Saison).
3. Kacheln Länder und Saison.
4. Später ein mitgelieferter Inhaltstyp je Version.

## Offene Entscheidungen des Projektinhabers
Kachelname; Länder oder Küchen; „Was koche ich heute?“ / „Zuletzt geöffnet“ gewünscht; Teigrechner nur Startseite; Saison fest Mitteleuropa oder wählbar; mitgelieferte Texte nur selbst geschrieben.

## Ergänzung des Projektinhabers (12.10.2026)

> „Auch Basis-Rezept wie z.B eine Béchamelsauce usw. wären auf der Startseite gut.“

**Entscheidung:** **Mitgelieferte Grundrezepte zum Übernehmen.** Kachel „Grundrezepte“ mit einigen selbst geschriebenen Rezepten (z. B. Béchamelsauce, Tomatensauce, Mürbeteig, Brühe), in Deutsch und Englisch, jedes per Tipp als normales Rezept in die eigene Sammlung zu übernehmen (Quelle „Moltobene“, danach frei bearbeitbar, übersetzbar, umrechenbar). Es gelten die Leitplanken für mitgelieferte Inhalte (selbst geschrieben, Herkunft im Repository, kein Download, keine fremden Fotos, Garzeiten und Lebensmittelsicherheit sorgfältig). Der Schlagwort-Schlüssel `basics` aus der Besprechung vom 09.10.2026 bleibt für eigene Grundrezepte. Das ist der **erste mitgelieferte Inhaltstyp** (vor Saisonkalender und Tipps) und kommt nach der ersten Startseite; Umfang zunächst klein (etwa 8–10 Rezepte). Zu klären: Wie oft darf ein Grundrezept übernommen werden (Duplikate), Begriff „Grundrezept“ / “basic recipe” in die festen Begriffe.
