# Besprechung vom 12.10.2026: Design verbessern

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen, Marketing, Sprache
**Moderation:** Claude · **App-Version:** 0.38.0 (Bildschirmfotos der Version 0.37.1)

Alle zehn Spezialisten haben als eigene Agenten teilgenommen: Runde 1 mit je höchstens drei Beiträgen (30 insgesamt), Runde 2 als Aussprache mit Stellungnahme und Top 3. Sie haben nichts geändert.

## Tagesordnung

1. Wo wirkt die App noch wie ein Standard-Gerüst statt wie eine Koch-App („Fotos im Mittelpunkt, modern, schlicht, kompakt, einfach und intelligent bedienbar“)?
2. Was am Teigrechner (Regler, Plus/Minus, Voreinstellungen) ist überladen oder unklar?
3. Einheitliche Formensprache, Abstände, Typografie, Hell/Dunkel, 200 % Schrift.
4. Welche Verbesserungen bringen bei kleinem Aufwand am meisten?

## Lagebericht

Seit 0.31.0: Foto-Karten-Sammlung mit Filtern, Hauptknopf „Hinzufügen“, Startseite mit Kacheln, Kochleiste mit Vorlesen, Teigrechner mit vier Teigarten, Reglern, Plus/Minus und Voreinstellungen (0.33.0–0.38.0, größtenteils aus einer anderen Sitzung). Aus der Designprüfung vom 12.10. waren offen: Verlauf unter der Leiste in der Rezeptansicht, Foto-Fläche beim Bearbeiten, einheitliche Formensprache. Relevante offene Issues: #52, #57, #58, #59, #61, #73, #74, #76, #79, #80, #81, #83, #84, #85.

## Beschlussvorschläge (gemeinsame Rangfolge)

Punkte: Top-3-Nennungen der Spezialisten (Platz 1 = 3, Platz 2 = 2, Platz 3 = 1). Zusammengelegte Beiträge sind ein Punkt.

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Teigrechner in einem Zug umbauen:** pro Wert nur eine Bedienung (Regler mit antippbarer Zahl), Umschalter „Zahlen/Regler“ weg, „Standard 250 g“ als eine Zeile und Zurücksetzen nur bei Abweichung (mit Text), Ergebnisleiste bei großer Schrift einklappbar, kürzere Texte („1.000 g Teig“), maxLines in Schaltern, eine höfliche Live-Region und Beschriftung der Regler für den Screenreader | D2, F3, K3, T1, T3, Ü1, Ü2, V3 | 8 von 10 (16 Punkte) | hoch | mittel | #86 (neu), #83, #84 |
| 2 | **Startseite mit Foto:** „Was koche ich heute?“ als große Foto-Karte, „Rezepte“-Kachel mit kleiner Vorschaubilder-Reihe, eigene Symbole je Kachel, Titel „Sammlung“ statt Dopplung „Rezepte / 8 Rezepte“, kürzere Kachelzeilen | D1 (zurückgezogen zugunsten K1), K1, T2 | Design, Kreativität, Texte, Marketing (10) | hoch | klein bis mittel | #87 (neu), #81 |
| 3 | **Meldungen bei großer Schrift lesbar:** „Keine Stimme“-Meldung bricht mitten im Wort um; kürzen, Aktion unter den Text oder feste Karte | V1 | Sicherheit, Texte, Übersetzungen, Sprache (9) | hoch | klein | #88 (neu), #79, #85 |
| 4 | **Kochansicht in einem Gerüst:** Kochleiste als Zustandsanzeige („Schritt 2 von 5“, kräftiger Rand am aktuellen Schritt, nicht nur Farbe), bei 200 % drei Tasten, „Weiter“ betont, Foto niedriger statt einklappbar, „Umrechnen“ als Chip; zusammen mit Große Schrift und Notiz beim Kochen | V2, K2, F2 | Design, Funktionalität, Kreativität, Übersetzungen, Sprache (9) | hoch | mittel | #89 (neu), #61, #79, #80 |
| 5 | **Eine „Hinweiskarte“ (Symbol + Text) für Prüfhinweis nach dem Übernehmen, „Rohen Teig nicht essen“ und Fehler beim Übernehmen** (Fortschritt, Fehlergrund, „Link als Entwurf behalten“, „Erneut versuchen“); Übernehmen-Wege im Bearbeiten nicht abgeschnitten, Prüfhinweis nach oben | S2, N1, N2 | Sicherheit, Konnektivität, Sprache, Design (8) | mittel | mittel | #90 (neu), #52 |
| 6 | **Messbasis zuerst:** Prüflauf wie beim Nutzer (Kaltstart, Ruckeln, Speicher, Suche, APK-Größe) vor weiteren Design-Schritten | P3 | Funktionalität, Performance, Sicherheit, Konnektivität, Marketing (4) | hoch | mittel | #52, #59 |
| 7 | **Regeln gegen Abschneiden:** Gestaltungsregeln für lange Sprachen in CLAUDE.md und ein Rundgang-Test mit künstlich langen Texten | Ü3 | Übersetzungen, Funktionalität (1) | mittel | klein | – |
| 8 | **Datenschutz-Zusage sichtbar:** Hinweis „nur diese Seite wird geladen“ im Link-Dialog und beim Sprachpaket, Eintrag Datenschutz unter Info | S1 | Sicherheit, Konnektivität (2) | mittel | klein | #58 |
| 9 | **Sammlung nachschärfen (klein zuerst):** Platz unten, damit der Knopf keine Karte verdeckt, Titel zweizeilig, einheitliche Formen über MaterialTheme.shapes, Verlauf unter der Leiste in der Rezeptansicht; Filter-Umbau (Schlagwörter und Sortierung in einer Chip-Reihe) danach | F1 (abgeschwächt), D3 | Design, Marketing, Performance | mittel | klein | #57 |
| 10 | **Store-Auftritt zuletzt:** Schaufenster-Variante des Rundgangs mit selbst fotografierten Gerichten, Teigrechner und Rezeptansicht als Seite 2 und 3 mit kurzen Bildunterschriften, erst nach den Umbauten oben | M1, M2 | Marketing | hoch | mittel | #73, #74 |

Außerdem ohne eigenen Rang: **Hinzufügen-Auswahl mit Zweitzeile und Gruppe „Übernehmen“** (N3, niedrig, klein), **Leitplanken-Prüfliste** (S3, abgeschwächt: Spende nie auf der Startseite, keine neue Berechtigung, Links nur http/https, deutliche Dialoge, Fotos ohne Aufnahmeort), **sparsame Foto-Karten** (P1: kein Blur, keine Schatten je Karte, höchstens ein Zwei-Farben-Verlauf, nur Vorschaubilder, höchstens 3–5 Bilder auf der Startseite, Crossfade ≤ 100 ms).

## Einigkeit

- Der Teigrechner wird einmal als Ganzes umgebaut (Aufbau, Texte, Screenreader, 200 %), nicht stückweise.
- Die Startseite bekommt Fotos; ein Gerüst für Startseite, Texte und Symbole.
- Die Kochansicht wird mit „Große Schrift und Notiz beim Kochen“ in einem Zug gebaut, damit nicht zweimal umgebaut wird.
- Ein gemeinsamer Hinweis- und Meldungsbaustein statt vieler Einzellösungen; Bedeutung nie nur über Farbe, bei 200 % steht die Aktion unter dem Text.
- Hinweise („Rohen Teig nicht essen“, Prüfhinweis, Fehler) dürfen durch Einklappen oder Schrumpfen nie verschwinden.
- Jeder der vier Umbauten verschiebt Fotonummern im Rundgang; je Umbau ein Durchgang, Rechenlogik bleibt in `data/` mit Unit-Tests.
- Mehrsprachigkeit: Sätze wie „Standard 250 g“ und „Schritt 2 von 5“ nur als ganze Texte mit nummerierten Platzhaltern, Umbruch statt fester Breite.
- Store-Fotos erst nach den Umbauten, mit eigenen Fotos des Projektinhabers.
- Kein Blur, keine Schatten je Karte, keine Dauer-Animation (Performance, Android 8).

## Streitpunkte – Entscheidung nötig

1. **Teigrechner: eine oder mehrere Eingaben pro Wert?** Am 12.10. (Besprechung zu #83) hat der Projektinhaber Regler, Plus/Minus, Zahlenfeld und Voreinstellungen gewünscht. Jetzt empfehlen Design, Funktionalität, Kreativität, Texte, Übersetzungen und Sprache, pro Wert nur noch eine Bedienung zu zeigen (Regler als Hauptbedienung, Zahl antippbar) und den Umschalter „Zahlen/Regler“ zu streichen; Plus/Minus bliebe nur bei kleinen ganzen Zahlen oder an den Reglerenden. Dagegen spricht nur, dass es Teil des ausdrücklichen Wunsches war. Texte warnen zusätzlich: Erklärsätze (Biga, Poolish …) dürfen nicht nur als Info-Symbol versteckt werden.
2. **Foto in der Kochansicht:** Funktionalität (F2) schlägt vor, das Foto beim Kochen klein zu halten oder einzuklappen. Kreativität (K2) will es niedriger machen und beim Scrollen schrumpfen lassen, Design widerspricht beiden („Fotos im Mittelpunkt“), Funktionalität nennt Vollbild und Scrollverhalten als heikel. Mehrheit: niedriger statt einklappbar.
3. **Sicherungs-Erinnerung (S2):** Sicherheit schlug eine einmalige Erinnerung vor, Konnektivität und Funktionalität erinnern an die Vision „die App bittet nie aktiv“. Hinweis des Moderators: Ein ruhiger Sicherungshinweis in der Sammlung existiert bereits (#51); der Vorschlag deckt sich weitgehend damit und entfällt, falls der Projektinhaber nichts anderes will.
4. **Baseline-Profil (Performance):** beschleunigt den Start auf Android 8, braucht aber ein zusätzliches Gradle-Modul (Architekturänderung). Vorher Entscheidung nötig.

## Offene Fragen an den Projektinhaber

1. Soll der Teigrechner pro Wert nur noch eine Bedienung zeigen (Streitpunkt 1)?
2. Foto in der Kochansicht: niedriger und schrumpfend, einklappbar oder wie heute (Streitpunkt 2)?
3. Bei Schrift 200 %: darf „Zutaten“ aus der Kochleiste ins Menü wandern, damit die Leiste einzeilig bleibt, und der Knopf „Hinzufügen“ als „+“ erscheinen?
4. Soll die App bei eingeschaltetem TalkBack selbst vorlesen (zwei Stimmen) oder nur den Fokus setzen?
5. Baseline-Profil (Streitpunkt 4): ja prüfen oder zurückstellen?
6. Store-Fotos: Sind selbst fotografierte Gerichte (ohne Aufnahmeort, ohne erkennbare Personen) für den Rundgang für „Schaufenster“ möglich, und wann?
7. Name „Molto Bene“ als Kochsendung in Gebrauch (Marktbericht): Markenregister vor dem Store-Start prüfen lassen?

## Zurückgezogen / bereits als Issue vorhanden

- **Zurückgezogen:** D1 (identisch mit K1), Leerzustand-Teil von K3 (N3 deckt die Wege ab), P2 abgeschwächt auf niedrig, S3 zur Prüfliste abgeschwächt, M3 abgeschwächt auf „Kobaltblau-Marke und zwei Zeilen Titel“, F1 abgeschwächt (erst Platz und Titelzeilen).
- **Schon als Issue:** #61 (Große Schrift beim Kochen), #57 (Finden und ordnen), #58 (Texte-Durchgang), #73/#74 (Store, IzzyOnDroid), #76 (Umzug aus anderen Apps), #79/#80/#85 (Sprache), #81 (Startseite), #83/#84 (Teigrechner), #52/#59 (Prüflauf, App-Größe).

## Anhang: Beiträge und Stellungnahmen (Kurzfassung)

**Design:** D1 Startseite mit Foto, D2 Teigrechner entschlacken, D3 Formensprache; Top 3: Startseite, Teigrechner, Kochleiste.
**Funktionalität:** F1 Sammlung kompakter, F2 Kochansicht entschlacken, F3 Teigrechner mit einer Eingabeart; Risiko Rundgang/Fotonummern; Top 3: F3, V2, P3.
**Sicherheit:** S1 Datenschutz-Zusage, S2 Hinweiskarte, S3 Leitplanken; Top 3: S2/N1/N2, V1, S1.
**Kreativität:** K1 Startseite mit Foto, K2 Rezeptkopf mit Kennzahlen, K3 Teigrechner und Leerzustand; Top 3: K1, K2+V2, D2/F3/K3.
**Konnektivität:** N1 Übernehmen-Chips und Prüfhinweis, N2 Fortschritt und Fehler, N3 Auswahl ordnen; Top 3: N2, N1+S2, S1.
**Performance:** P1 sparsame Foto-Karten, P2 Platzhalter/Regler, P3 Messbasis; Top 3: P3, P1, D2.
**Texte:** T1 Regleransicht, T2 Startseite, T3 Teigrechner-Texte; Top 3: D2/F3/K3+T1/T3, V1, T2+D1.
**Übersetzungen:** Ü1 Ergebnisleiste, Ü2 maxLines, Ü3 Regeln und Pseudosprachen-Test; Top 3: Ü1+D2/F3, V1/V2, Ü3.
**Marketing:** M1 Schaufenster mit echten Fotos, M2 Store-Seiten, M3 eigener Stil; Top 3: D1/K1+T2, D2/F3/K3, M1+M2.
**Sprache:** V1 Meldungen lesbar, V2 Kochleiste als Zustandsanzeige, V3 TalkBack und Live-Regionen; Top 3: V1, V2+F2/K2, D2/F3+V3.

## Entscheidungen des Projektinhabers (12.10.2026)

- Issues für Rang 1–5 angelegt: #86 (Teigrechner), #87 (Startseite), #88 (Meldungen), #89 (Kochansicht), #90 (Hinweiskarte). Rang 6–10 bleiben Vorschläge im Protokoll.
- Teigrechner: Regler + Zahl, Plus/Minus nur bei kleinen Zahlen; Umschalter „Zahlen | Regler“ entfällt.
- Kochansicht: Foto niedriger, schrumpft beim Scrollen (nicht einklappbar).
- Noch offen: Fragen 3 bis 7 oben (Zutaten ins Menü bei großer Schrift, TalkBack, Baseline-Profil, Store-Fotos, Markenprüfung).
