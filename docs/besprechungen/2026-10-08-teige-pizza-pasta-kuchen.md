# Besprechung vom 08.10.2026: Funktion für Teige, Pizza, Pasta und Kuchen

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.26.0 (versionCode 47)

## Anlass

Wunsch des Projektinhabers, wörtlich:

> „Ich hätte gerne noch eine Funktion für Teige, Pizza, Pasta, Kuchen... Machen wir eine Besprechung, wie wir das am besten implementieren.“

Was genau gemeint ist, war offen. Die Besprechung sollte das klären.

## Tagesordnung

1. **Bedarf:** Was brauchen Menschen beim Teigmachen und Backen, das die App heute nicht kann? Denkbar waren:
   - (a) Teigrechner mit Bäckerprozenten
   - (b) Pizza nach Anzahl und Gewicht der Teigkugeln, Hefe nach Gehzeit
   - (c) Pasta (Mehl/Ei, Gramm pro Portion)
   - (d) Backform umrechnen
   - (e) Grundrezepte als Vorlagen
   - (f) Zeitplan oder Timer
   - (g) nur ordnen, z. B. mit einem Schlagwort
2. **Vision:** Passt das zur Vision, vor allem zu Kernfunktion 4 („Portionen umrechnen“) und „Lieber eine Funktion fertig als mehrere halb“? Gehört es ins Rezept oder wird es ein eigenes Werkzeug?
3. **Bestehendes:** Wie bauen wir auf „Portionen umrechnen“, die Einheit der Portionen und die Zutatenzeilen auf? Was folgt daraus für Datenbank, Sicherung und Teilen?
4. **Reihenfolge:** Was ist der kleinste sinnvolle erste Schritt, und wann kommt er im Verhältnis zu 1.0?

## Lagebericht

- **Stand der App (0.26.0):**
  - **Übernehmen:** Rezepte kommen aus Link, Foto, Text, Datei und YouTube.
  - **Kochen, Teil 1 (0.20.0):** abhaken, aktueller Schritt, **Portionen umrechnen**. Umgerechnet werden nur Mengen am Zeilenanfang und Gewicht oder Volumen in Klammern, nur in der Anzeige.
  - **Weitere Funktionen:** Übersetzen Deutsch ↔ Englisch (0.21.0), Darstellung wählbar (0.26.0).
- **Datenmodell:**
  - Portionen als Zahl, dazu eine freie Einheit, z. B. „Stück“ oder „Springform 26 cm“.
  - Zutaten als Textzeilen.
  - Schlagwörter stehen im Modell, haben aber noch keine Oberfläche (#57).
- **Offene Issues:**
  - #52 Prüflauf
  - #53 Foto-Übernahme bei großer Schrift
  - #57 Finden und ordnen, Abnahmeliste 1.0
  - #58 Texte-Durchgang
  - #61 Kochen, Teil 2
  - #62 Übersetzen
  - #59, #47, #16
- **Vorgemerkt, noch nicht beauftragt:** Besprechung zu Rezeptsuche über mehrere Plattformen und zu Variationen eines Rezepts.

## Ergebnis in einem Satz

Kein eigener „Teigrechner“. Stattdessen soll **„Portionen umrechnen“ zu „Umrechnen“ wachsen**:
- nach Portionen,
- nach einer Zutat („nur 350 g Mehl da“, „3 Eier“),
- später nach Backform.

Die Grundlage zuerst richten: Backform und Teigkugeln in der Portionsangabe richtig lesen, krumme Mengen sinnvoll runden.

## Beschlussvorschläge (gemeinsame Rangfolge)

Unterstützung = Nennungen unter den Top 3 der Aussprache (8 Teilnehmer).

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | Portionsangabe und Backform richtig lesen und anzeigen | F1, N1, T2, Ü2 (Erkennung) | alle 8 (7 × Platz 1) | hoch | mittel | heutiger Fehler; vor 1.0 · **#63** |
| 2 | „Umrechnen …“: ein Fenster mit „Nach Portionen“ und „Nach Zutat“, auch ohne Portionen | K1, D1, F3 | 7 | hoch | mittel | Kernfunktion 4; mit/nach #61 · **#66** |
| 3 | Backzutaten richtig umrechnen und runden („≈“) | D2, F2, Ü3 (Packungen) | 5 | mittel | klein | heutiger Fehler („3,75 Eier“) · **#65** |
| 4 | Begriffe festlegen, bevor der erste Text entsteht | T1, Ü1 | 2 (Texte, Übersetzungen), sonst unstrittig | mittel | klein | CLAUDE.md, nur Dokumentation · **#64** |
| 5 | Backform umrechnen (rund, eckig, Blech; cm und Zoll) | K2, Ü2, D3 | 1, als nächster Schritt nach 1 und 2 von allen getragen | mittel | klein bis mittel | baut auf Rang 1 und 2 auf · **#67** |
| 6 | Ehrliche Hinweise beim Umrechnen | S3, T3 | 1 (Sicherheit), von allen getragen | mittel | klein | Abnahmebedingung in #66 und #67 |
| 7 | Gehzeiten: zuerst nur „fertig um 14:35“, Uhr-App erst nach Freigabe | S1, P1 | – | niedrig | klein | später; passt zu #61 · **#71** |

### Rang 1: Portionsangabe und Backform richtig lesen und anzeigen

**Heute:**
- „Springform Ø 26 cm“ wird zu 26 Portionen mit der Einheit „cm“. Ein Tipp auf + rechnet dann mit 27/26.
- Aus „Für eine 26er Springform“ wird „26 er Springform“, aus „Blech 30 x 40 cm“ werden 30 Portionen.
- „Zutaten für eine Springform (Ø 26 cm)“ aus Text oder Foto landet in keinem Feld.
- Eine Einheit ohne Portionszahl wird nirgends gezeigt und nicht geteilt.
- Mit + entsteht „2 Springform (26 cm)“.

**Vorschlag:**
- Eine gemeinsame Lese-Stelle für die Rezeptmenge in `data/`, statt zwei getrennter Regeln in `RecipeYield` und `RecipeTextParser`.
- Größen (Ø, cm, „26er“, „30 x 40“, inch) sind nie eine Anzahl. „ein/eine/one/un/una“ zählt als 1.
- Ist etwas unklar, bleiben die Portionen leer, statt geraten zu werden.
- Die Erkennung kennt alle üblichen Schreibweisen, auch Teigling, Teigkugel, pan, tin und sheet, unabhängig davon, welche Begriffe die Oberfläche zeigt.
- Eine Einheit ohne Zahl wird angezeigt und beim Teilen als `recipeYield` mitgegeben.
- In der Zeile zwischen − und + steht bei eigener Einheit „Springform (Ø 26 cm): 2“ statt „2 Springform (26 cm)“.
- Testtabelle mit typischen Kuchen- und Pizzaangaben in 5 Sprachen und in Zoll.

### Rang 2: „Umrechnen …“ mit „Nach Portionen“ und „Nach Zutat“

**Warum:** Beim Teig fragt niemand „für wie viele Personen?“, sondern zum Beispiel:
- „Ich habe 750 g Mehl.“
- „Nur 3 Eier.“

Das deckt Pizza, Pasta und Teige ab, ohne dass jemand „Bäckerprozente“ kennen muss.

**Vorschlag:**
- **Faktor statt Portionen:** Die Rezeptansicht merkt sich einen Faktor statt einer ganzen Portionszahl. Damit wird möglich:
  - Umrechnen auch ohne Portionsangabe,
  - Halbieren eines Kuchens „für 1 Springform“,
  - eine einheitliche Obergrenze (heute 99 in der Ansicht, 999 im Formular).
- **Ein Fenster:** „Umrechnen …“ öffnet ein Fenster. Die Wege stehen untereinander als Liste, jede Zeile mindestens 48 dp hoch, damit es auch bei 200 % Schrift passt.
- **„Nach Zutat“:**
  - Zur Wahl stehen nur Zeilen, deren Menge die App lesen kann.
  - Die Menge ist vorausgefüllt.
  - Das Zahlenfeld nimmt Komma und Punkt an. 0 und unsinnig große Werte sind gesperrt: kein Absturz, kein „∞ g“.
  - Das Antippen einer Zutatenzeile hakt sie weiterhin ab.
- **Ein Muster für alle Hinweise:** „Mengen umgerechnet für 350 g Mehl. Im Rezept: 500 g.“, dazu wie heute „Zurücksetzen“. Die Zutatenzeile wird über einen Platzhalter eingesetzt.
- **Nur Anzeige** (siehe Grundsatz): Der Faktor liegt im Zwischenspeicher der Ansicht und übersteht das Drehen des Handys.
- **Code:** Die Bedienung kommt in eine eigene Datei neben `RecipeScreen.kt`.
- **Rundgang:** neues Foto, ein Beispielrezept mit „Springform Ø 26 cm“.

### Rang 3: Backzutaten richtig umrechnen und runden

**Heute:**
- „21 g Frischhefe (½ Würfel)“ wird beim Verdoppeln zu „42 g Frischhefe (½ Würfel)“, weil Würfel, Päckchen, packet, grams usw. in Klammern nicht mitgerechnet werden.
- Krumme Faktoren ergeben „3,75 Eier“ oder „1,77 Eier“.

**Vorschlag:**
- **Einheiten:** eine gemeinsame Liste für Umrechnen und Texterkennung.
- **Packungen** (Würfel, Päckchen, packet) nur als Anzahl umrechnen, nie in Gramm.
- **Runden nur in der Anzeige:**
  - Stück auf ½ bzw. ¼.
  - Gramm und Milliliter ab 100 auf 5.
  - Unter 10 g auf 0,5 g.
- **Kennzeichnen:** Gerundete Werte bekommen „≈“. Der Screenreader liest „etwa …, umgerechnet“.
- **Testgruppe „Backen“** mit etwa 15 Zeilen, auch solchen, die unverändert bleiben müssen, z. B. „65 % Wasser“.

### Rang 4: Begriffe vorher festlegen

Das ist eine Ergänzung der Tabelle „Feste Begriffe“ in CLAUDE.md.

**Deutsch, in der Oberfläche nur Alltagswörter:**
- Teigkugel (nicht Teigling)
- Wasseranteil bzw. „% vom Mehl“ (nicht Hydration, Bäckerprozente)
- Gehzeit (nicht Gare)
- Backform mit „Ø 26 cm“
- pro Portion (nicht pro Person)

**Englisch, als Regel amerikanisch** (die Oberfläche schreibt schon „Favorite“ und „Colors“):
- pan (nicht tin)
- baking sheet
- dough ball
- rising time
- „% of flour“

**Benennung:**
- Kein Sammelname wie „Teige“ oder „Teigrechner“. „Teig“ heißt im Englischen je nach Art dough, batter oder pastry.
- Funktionen werden nach der Tätigkeit benannt: „Nach Zutat umrechnen“ / “Adjust by ingredient”, „Backform umrechnen“ / “Adjust for pan size”.

**Kleine Korrekturen:**
- Der Feldhinweis „e.g. pieces, 26 cm tin“ wird zu „… Ø 26 cm pan“, der deutsche Hinweis zu „z. B. Stück, Springform Ø 26 cm“.
- „brackets“ wird zu „parentheses“.

### Rang 5: Backform umrechnen

- **Dritter Weg** im Fenster: „Nach Backform“.
  - Rund mit Durchmesser.
  - Eckig oder Blech mit Länge × Breite.
  - cm und inch, Anzeige z. B. „23 cm (9 inch)“.
- **Rechnung:** über das Verhältnis der Flächen. 26 → 28 cm ergibt etwa 1,16, nicht 28/26.
- **Form im Rezept:** wird aus der Einheit vorausgefüllt (Rang 1), sonst gewählt. Die App rät nie still.
- **Nicht angeboten:** Kastenform und Gugelhupf, weil ihr Inhalt nicht aus einem Maß folgt. Bleche haben keine Standardgröße.
- **Mehrzahl:** richtige Mehrzahlformen je Form in beiden Sprachen.
- **Später möglich:** eigene Formen als Merkwert.

### Rang 6: Ehrliche Hinweise

Das ist kein eigenes Issue, sondern eine Abnahmebedingung für Rang 2 und 5.
- **Backzeit:** Backzeit und Temperatur werden nie still umgerechnet. Der Hinweis muss beide Richtungen abdecken: „Die Backzeit kann sich ändern – früher prüfen, ob der Kuchen durch ist.“ Der Fachbegriff „Garprobe“ wird nicht verwendet.
- **Keine Empfehlungen:** Die App gibt keine eigenen Empfehlungen zu Hefe, Gehzeit oder Temperatur.
- **Vorlagen:** Sollte es je Vorlagen geben, werden sie nur selbst geschrieben (Urheberrecht).

### Rang 7: Gehzeiten und Timer, später

- **Stufe 1, ohne Berechtigung:**
  - Nur eine Anzeige wie „fertig um 14:35“ in der Uhrzeit-Schreibweise des Handys.
  - Gespeichert wird die Endzeit, nicht die Restzeit.
  - Kein Sekunden-Countdown, damit der Bildschirm nicht stundenlang an bleibt.
- **Stufe 2, nur nach Freigabe:**
  - Übergabe an die Uhr-App des Handys. Dafür ist die Berechtigung `SET_ALARM` nötig, ohne Abfrage beim Nutzer, aber mit neuem Eintrag in `ERLAUBT`.
  - Übergeben wird ein neutraler Name („Gehzeit“), nicht der Rezepttitel.
- **Nie:** ein eigener Wecker im Hintergrund.

## Einigkeit

- **Grundsatz „nur Anzeige, keine neue Datenart“** (N2, P3, S2, von allen getragen):
  - Die Backform bleibt Text in der Einheit der Portionen und geht so als `recipeYield` in schema.org mit.
  - Umgerechnet wird nur in der Anzeige.
  - Keine Änderung an der Datenbank, keine neue Formatversion der Sicherung, nichts Neues beim Teilen. Alte Sicherungen und Rezeptdateien anderer Apps profitieren sofort.
  - Die vorhandenen leeren Felder `quantity`/`unit` der Zutaten werden nicht befüllt.
- **Ins Rezept, kein eigenes Werkzeug:** „Umrechnen“ ist eine Funktion mit mehreren Wegen. Ein eigener Teigrechner ohne Rezept hätte keinen Bezug zur Sammlung und braucht einen neuen Zugang.
- **Bewusst nicht:**
  - **Hefemenge nach Gehzeit und Temperatur:** ein Schätzmodell, das Genauigkeit vortäuscht.
  - **Mitgelieferte Grundrezepte:** Urheberrecht, Pflege und Übersetzung in jede Sprache.
  - **Eigener Hintergrund-Wecker:** Akku, mehrere Berechtigungen, unzuverlässig auf günstigen Handys.
- **Ordnen** (Deutung g) gehört zu #57. Vorgeschlagen sind Schlagwörter mit fester Kennung, z. B. `baking`, `pizza`, `pasta`, `bread`, `cake`, oder „Grundrezepte“ (`basics`). Ein Schlagwort „Teige“ ist nicht vorgesehen.
- **Reihenfolge:**
  - Rang 1 und 3 beheben heutige Fehler und passen vor 1.0.
  - Rang 4 kommt sofort (nur Dokumentation).
  - Rang 2 folgt mit oder nach #61, Rang 5 danach. Beide brauchen keinen Datenumbau.
  - Rang 7 kommt später.

## Streitpunkte – Entscheidung nötig

1. **Bildschirm bei langen Wartezeiten** (P2): Heute bleibt der Bildschirm an, solange die Rezeptansicht offen ist. Bei Teigen mit langer Gehzeit kostet das viel Akku.
   - **Performance:** Freigeben, wenn eine Wartezeit beginnt, oder nach langer Untätigkeit, mindestens 30 Minuten.
   - **Gegenposition:** Die Vision verspricht „Bildschirm bleibt an“.
   - **Bedingungen der anderen:**
     - Design: nur mit sichtbarer Rückmeldung, ein Tippen schaltet ihn wieder ein.
     - Texte: keine eigene Meldung.
     - Kreativität: gehört zu #61.
   - **Frage:** Soll der Bildschirm nach langer Untätigkeit wieder ausgehen dürfen?
2. **Uhr-App für Gehzeiten** (S1/P1): Alle sind für „zuerst nur Anzeige“.
   - **Frage:** Kommt die Übergabe an die Uhr-App später überhaupt in Frage? Das wäre eine neue Berechtigung `SET_ALARM`, ohne Abfrage beim Nutzer.
3. **Englisches Wort für den Wasseranteil:**
   - Übersetzungen und Texte: „hydration“, unter Hobbybäckern üblich.
   - Kreativität: Alltagsform “Water: 65% of flour”.
   - Wichtig wird das erst mit einer Teig-Übersicht (siehe „Zurückgezogen“), also nach 1.0.

## Offene Fragen an den Projektinhaber

1. **War das gemeint?** Ist „Nach Zutat umrechnen“ plus „Backform umrechnen“ im Rezept das, was gemeint war? Oder war ein eigener Rechner gemeint, etwa „Pizzateig für 6 Pizzen à 250 g“ ohne Rezept? Das Team rät davon ab.
2. **Pizza:** Reicht es, „Pizzen“ als Einheit der Portionen einzutragen (geht heute schon mit − und +)? Oder soll man auch das Gewicht je Teigkugel ändern können?
3. **Zeitpunkt:** Sollen Rang 2 und 5 noch vor 1.0 kommen? Laut Abnahmeliste in #57 ist „Umrechnen von Einheiten“ für 1.0 nicht nötig.

## Entscheidungen des Projektinhabers (08.10.2026)

Wörtlich auf die offenen Fragen und Streitpunkte:

- **Was gemeint war:** „Ein eigener Teigrechner ist gemeint, für Pizza mit Biga oder Poolish, verschiedenen Hefen usw.“ und „Ja auch das Umrechnen in den Rezepten und ein eigener Teigrechner“. Es kommt also **beides**:
  - Rang 1–6 (Umrechnen im Rezept) bleiben.
  - Dazu kommt ein **eigener Teigrechner**. Die Empfehlung des Teams gegen ein eigenes Werkzeug ist damit überstimmt.
- **Bildschirm** (Streitpunkt 1): „Soll in den Einstellungen individuell einstellbar sein“.
- **Uhr-App für Gehzeiten** (Streitpunkt 2): „Später prüfen“. Es bleibt bei Rang 7, Stufe 2 nur nach eigener Freigabe.

Darauf folgte eine Zusatzrunde (Runde 3) aller acht Spezialisten zum Teigrechner und zur Bildschirm-Einstellung.

**Zweite Entscheidungsrunde nach der Zusatzrunde:**
- **Issues:** alle angelegt.
  - Rang 1 → #63
  - Rang 2 → #66
  - Rang 3 → #65
  - Rang 4 und Z3 → #64
  - Rang 5 → #67
  - Rang 7 → #71
  - Z1 → #68
  - Z2 → #69
  - Rang 6 ist Abnahmebedingung in #66 und #67.
  - N3 ist als Punkt bei #57 vermerkt.
- **Vision:** „Ja und auch Teige für Brote, Pasta und Kuchen. Jeweils eine eigene Kategorie.“
  - Die Vision in CLAUDE.md ist unter Kernfunktion 4 um den Teigrechner für Pizza, Brot, Pasta und Kuchen ergänzt.
  - Die Teigarten Brot, Pasta und Kuchen folgen nach Pizza → #70.
  - In der Oberfläche heißt das „Teigart“, weil „Kategorie“ nach den festen Begriffen nur bei Schlagwörtern vermieden wird.
- **Zugang zum Teigrechner:** eigenes Symbol oben in der Sammlung (Vorschlag Design).
- **Standard für „Bildschirm in der Rezeptansicht“:** „Bleibt an“, wie heute.

## Zusatzrunde: Teigrechner und Bildschirm-Einstellung

### Beschlussvorschläge aus der Zusatzrunde

| Nr. | Punkt | Von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|
| Z1 | Einstellung „Bildschirm in der Rezeptansicht“ | Design, Performance, Texte, Sicherheit, Funktionalität, Kreativität, Übersetzungen | mittel | klein | zusammen mit #61 möglich · **#68** |
| Z2 | Teigrechner für Pizza: Rechenkern, Bildschirm, „Als Rezept speichern“ | alle 8 | hoch | groß | eigene Etappe auf einem Arbeitszweig · **#69**; Brot, Pasta, Kuchen: **#70** |
| Z3 | Begriffe für den Teigrechner (Ergänzung von Rang 4) | Texte, Übersetzungen | hoch | klein | vor der ersten Zeile Code · **#64** |

### Z1: Einstellung „Bildschirm in der Rezeptansicht“

- **Ort:** In den Einstellungen gibt es einen neuen Abschnitt „Kochen“ zwischen „Übersetzung“ und „Darstellung“. Später kommt dort auch „Große Schrift beim Kochen“ aus #61 hin.
- **Auswahl:** eine Liste zum Antippen wie bei „Hell oder dunkel“:
  - „Bleibt an“ (wie heute)
  - „Geht 15 Minuten nach dem letzten Tippen aus“
  - „Geht 30 Minuten nach dem letzten Tippen aus“
  - „Wie das Handy“
  - Englisch: “Stays on”, “Turns off 15 minutes after last tap” (Mehrzahlformen), “Same as device”.
- **Gilt für:** die Rezeptansicht und den Teigrechner.
- **Umsetzung:**
  - Nur über die vorhandene Funktion „Bildschirm anlassen“ der Ansicht (`keepScreenOn`), nie über `WAKE_LOCK`. Das wäre eine neue Berechtigung.
  - Kein Wecker, kein Hintergrunddienst.
  - Jede Berührung startet die Frist neu.
  - Gespeichert als Merkwert in `AppPreferences`, nicht in der Sicherungsdatei.
  - Die Zeitlogik wird mit einer nachgestellten Uhr getestet.
- **Offen:** der Standard. Design, Texte und Funktionalität sind für „Bleibt an“, wie die Vision es verspricht. Performance und Kreativität sind für 30 Minuten, um Akku zu sparen. Siehe Fragen unten.

### Z2: Teigrechner für Pizza

**Name:** „Teigrechner“ / “dough calculator”. Das ist das Wort des Projektinhabers; Texte zieht „Pizzateig berechnen“ zurück.

**Zugang:** ein eigenes Ziel in der App, erst beim Öffnen erzeugt. Kein Internet, keine Berechtigung, keine neue Bibliothek. Offen ist, wo man ihn öffnet:
- Design ist für ein viertes Symbol oben in der Sammlung (Taschenrechner), weil dort bei normaler Schrift Platz ist.
- Kreativität und Texte sind für einen Eintrag im Menü ⋮ der Sammlung, das #57 für „Sortieren“ ohnehin einführt.
- Einig sind sich alle: Das „+“ öffnet weiter direkt das Formular.

**Aufbau:** einspaltig, ohne Tabelle, mit Abschnitten als Überschriften für den Screenreader:
- **Teigkugeln:**
  - „Anzahl Teigkugeln“ mit − und +
  - „Gewicht je Teigkugel“ in g
- **Teig:** „Wasseranteil“, „Salz“ und „Öl“, jeweils „% vom Mehl“, mit üblichem Bereich darunter, z. B. „üblich 60–70 %“.
- **Hefe:**
  - Art als Liste zum Antippen: Frischhefe, Trockenhefe (Instant), aktive Trockenhefe.
  - Menge in „% vom Mehl“.
  - Umrechnung zwischen den Arten mit festen, dokumentierten Faktoren, angezeigt mit „≈“, z. B. „21 g Frischhefe ≈ 7 g Trockenhefe“.
- **Vorteig:**
  - Auswahl „Ohne Vorteig“, „Biga“, „Poolish“, jeweils mit einer Zeile Erklärung.
  - „Mehl im Vorteig“ in %.
  - Wasseranteil des Vorteigs vorausgefüllt (Poolish 100 %, Biga etwa 45–50 %) und änderbar.
  - **Sauerteig und Lievito madre** werden nicht als Hefe mit Faktor gerechnet, sondern als Vorteig bzw. Starter mit eigenem Mehl und Wasser, die im Wasseranteil mitzählen. Ihre Triebkraft schwankt, ein fester Faktor wäre falsch.
- **Ergebnis:** immer sichtbar darunter, in Zeilen wie im Rezept („620 g Mehl“), getrennt nach „Vorteig (Biga)“, „Hauptteig“ und „Gesamt“. Der Screenreader liest nicht jede Zahl beim Tippen vor, sondern die Gesamtmenge nach einer kurzen Pause.
- **Bedienung:**
  - Zahlenfelder mit Zahlentastatur, Komma und Punkt erlaubt, keine Schieberegler.
  - Fehler mit Text am Feld, nicht nur rot.
  - Kein leerer Zustand: Der Rechner startet mit den zuletzt benutzten oder üblichen Werten.
  - „Zurücksetzen“.

**Rechenkern:** `data/dough/DoughCalculator.kt` als reines Kotlin mit Unit-Tests.
- **Grundrechnung:** Mehl = Teiggewicht ÷ (1 + Wasser + Salz + Öl + Hefe). Der Vorteig ist ein Anteil des Gesamtmehls, der Hauptteig ist der Rest.
- **Tests:**
  - Die Summe ergibt das Teiggewicht.
  - Mehl im Vorteig plus Mehl im Hauptteig ergibt das Gesamtmehl.
  - Der Wasseranteil stimmt auch mit Starter.
  - Nichts ist negativ.
  - Feste Beispiele für Biga, Poolish und Lievito madre stimmen.
  - Komma und Punkt werden richtig gelesen.
- **Runden erst in der Anzeige:**
  - Mehl und Wasser auf 1 g, Salz und Öl auf 0,5 g, Hefe auf 0,1 g.
  - Was größer als 0 ist, wird nie zu „0 g“, sondern zu „< 0,1 g“. Eine 24-Stunden-Biga braucht z. B. nur etwa 0,3 g Hefe.
- **Grenzen** (Sicherheit), zum Beispiel:
  - Teigkugeln 1–50, je 50–2.000 g
  - Wasseranteil 45–100 %, Salz 0–5 %, Öl 0–10 %, Hefe 0,01–5 %
  - Mehl im Vorteig 0–100 %, Wasser im Vorteig 40–125 %
- **Unmögliche Kombinationen:** Passen Vorteig und Teig nicht zusammen (z. B. Poolish aus 80 % des Mehls bei 65 % Wasseranteil), kommt eine klare Meldung statt einer negativen Wassermenge.

**Hefe nach Gehzeit und Temperatur:** In der ersten Fassung wird die Hefe in „% vom Mehl“ eingegeben. Ein Richtwert nach Gehzeit und Temperatur kommt später:
- Grundlage ist eine selbst erstellte, im Code dokumentierte Tabelle, keine Formel oder Tabelle aus fremden Apps (Urheberrecht, Schutz von Datensammlungen).
- Gilt nur für magere Teige (Mehl, Wasser, Salz, Hefe, Öl, Zucker, Malz).
- Die Werte sind begrenzt (Gehzeit 1–96 h, Temperatur 2–30 °C), °C oder °F je nach Region des Handys.
- Fester Hinweis: „Richtwert. Wie schnell der Teig geht, hängt von Wärme, Mehl und Hefe ab – auf den Teig achten, nicht nur auf die Uhr.“
- Der Richtwert darf ein Feld vorausfüllen, bleibt aber änderbar.

**„Als Rezept speichern“:** legt einen normalen Entwurf an (Grundsatz N2). Datenbank, Sicherung und Teilen bleiben unverändert.
- **Titel:** z. B. „Pizzateig mit Biga“.
- **Sprache:** Die Rezeptsprache ist die Sprache der Oberfläche. Zahlen werden ohne Tausenderpunkt geschrieben („1000 g“), das Dezimalzeichen richtet sich nach der Rezeptsprache.
- **Portionen:** die Anzahl, Einheit „Teigkugeln (je 250 g)“. Die Lese-Stelle aus Rang 1 muss „je 250 g“ und „à 250 g“ als Größe lesen.
- **Zutaten:**
  - Zwischenüberschriften „Für den Vorteig (Biga):“ und „Für den Hauptteig:“.
  - Die Menge steht immer vorn, damit „Umrechnen …“ (Rang 2) greift.
  - Ein Test schickt jeden erzeugten Entwurf durch Umrechnen und Portionserkennung.
- **Eckdaten** (Wasseranteil, Hefeart, Anteil Vorteig): Konnektivität will sie als Prozent in Klammern in die Zutatenzeilen schreiben („300 g Mehl (30 %)“), denn Notizen werden nie geteilt. Texte will sie in die Notizen schreiben. Empfehlung der Moderation: in den Zutatenzeilen, damit sie beim Teilen erhalten bleiben.
- **Quelle:** „eigenes Rezept“.
- **Zubereitung:** bleibt leer.
- **Meldung:** „Als Entwurf gespeichert. Zubereitung ergänzen.“

**Merken:**
- Gemerkt wird nur ein Satz zuletzt benutzter Werte in `AppPreferences`, ohne Verlauf („Zurücksetzen“ löscht ihn).
- Eigene Vorlagen sind gespeicherte Teigrezepte. Sie werden damit gesichert, geteilt, durchsucht und über Schlagwörter (#57) gefunden.
- Es gibt keine eigene Tabelle für Vorlagen.

**„Im Teigrechner wieder öffnen“:** ist später zu entscheiden.
- Konnektivität: geht ohne neues Feld, wenn die Prozente in den Zutatenzeilen stehen. Abgesichert durch einen Rundweg-Test: Rechner → Rezept → Rezeptdatei → Übernehmen → Rechner.
- Kreativität und Funktionalität: lieber nicht oder erst mit der Besprechung „Variationen“.

**Reihenfolge:**
- Erst Z3 und Rang 4 (Begriffe), dann Rang 1–3.
- Der Rechenkern kann unabhängig davon mit Tests entstehen.
- Der Bildschirm und „Als Rezept speichern“ kommen danach als eigene Etappe auf einem Arbeitszweig, weil sie die Lese-Stelle (Rang 1) und das Runden (Rang 3) nutzen.
- Design, Kreativität und Funktionalität sehen den Teigrechner nach 1.0 und nicht als Bedingung für 1.0.

**Vision:** Ein eigenes Werkzeug ist heute keine Kernfunktion. Kreativität und Funktionalität bitten darum, die Vision mit Zustimmung des Projektinhabers vor dem Bau zu ergänzen, z. B. unter Kernfunktion 4: „Teigrechner für Pizza (Vorteig wie Biga oder Poolish, verschiedene Hefen); das Ergebnis wird ein Rezept.“

### Z3: Begriffe für den Teigrechner

Zur Ergänzung der Tabelle „Feste Begriffe“ in CLAUDE.md, zusammen mit Rang 4:

| Deutsch | Englisch | Hinweis |
|---|---|---|
| Teigrechner | dough calculator | nicht: Pizzarechner, Assistent |
| Vorteig | preferment | Erklärung: „Ein Teil von Mehl, Wasser und Hefe wird vorher angesetzt. Das gibt mehr Geschmack und einen lockereren Teig.“ |
| Hauptteig | final dough | „Alles, was danach dazukommt.“ |
| Biga, Poolish | biga, poolish | Namen, nicht übersetzt (`translatable="false"`); „Fester Vorteig mit wenig Wasser.“ / „Flüssiger Vorteig mit gleich viel Wasser wie Mehl.“ |
| Frischhefe | fresh yeast | feste Kennung `fresh` |
| Trockenhefe (Instant) | instant yeast | `instant_dry`; in Deutschland ist „Trockenhefe“ meist Instant |
| Aktive Trockenhefe | active dry yeast | `active_dry` |
| Sauerteig (Lievito madre im Erklärsatz) | sourdough starter | `sourdough`, gerechnet als Starter mit Mehl und Wasser |
| Teigkugel | dough ball | |
| Wasseranteil, „% vom Mehl“ | hydration, “% of flour” | |

Mehl bekommt keine Auswahlliste, weil Mehltypen keine genauen Gegenstücke haben. Höchstens gibt es ein freies Feld, das wörtlich ins Rezept übernommen wird.

## Zurückgezogen / bereits als Issue vorhanden

- **D3** (eigenes Datenbankfeld für die Backform) von Design zurückgezogen. Es geht in Rang 5 auf. Neu bewerten frühestens nach 1.0 und nur, wenn das Lesen aus dem Text nachweislich nicht reicht. Konnektivität stellt richtig: Eine neue Formatversion der Sicherung wäre dafür nicht nötig, ein Feld mit Standardwert reicht.
- **N1** geht in Rang 1 (F1) auf. **F3** geht in Rang 2 (K1) auf.
- **S2** ist kein eigenes Issue mehr, sondern eine Abnahmebedingung in Rang 2 (Eingaben begrenzen).
- **Ü3:**
  - Der Teil °C/°F ist zurückgezogen, weil Hefe nach Gehzeit ausgeschlossen ist.
  - Der Teil Zahlenformat gilt in Rang 2 und 7.
  - Der Teil Packungen und Mehltypen gilt in Rang 3.
- **K3** (Teig-Übersicht mit Wasseranteil und Gewicht der Teigkugeln) ist herabgestuft. Den Kern deckt Rang 2 ab, der Rest geht in die vorgemerkte Besprechung „Variationen“.
- **T2:** Der zweite Teil (neuer Feldhinweis ohne Springform) ist abgeschwächt. Der Hinweis bleibt, mit Ø: „z. B. Stück, Springform Ø 26 cm“.
- **P2** gehört nach Kreativität zu #61, siehe Streitpunkt 1.
- **N3** (Schlagwörter aus Rezeptdateien wieder einlesen, `recipeCategory` nur als Vorschlag) gehört als Punkt zu #57.
- **Bezug zu bestehenden Issues:**
  - #61 Kochen, Teil 2: Ort für Rang 7 und P2.
  - #57 Schlagwörter, Abnahmeliste.
  - #52 Prüflauf: die Umrechnung mitmessen.
  - #58 Texte-Durchgang: Feldhinweis, „parentheses“.
  - Protokoll vom 07.10.2026, Rang 5 („Einheiten umrechnen“): nutzt dieselbe Einheitenliste wie Rang 3.

## Anhang: Beiträge und Stellungnahmen

### Design
- **Beiträge:**
  - D1: „Nach Zutat umrechnen“ unter den Portionen.
  - D2: runden und „≈“, auch für den Screenreader.
  - D3: eigene Backform-Angabe nach 1.0.
- **Gründe:**
  - Die Kopfzeile ist voll, ein eigener Bildschirm hätte keinen Zugang.
  - Bäckerprozente sind ein Fachbegriff und passen bei 360 dp und 200 % nicht als Spalte.
- **Aussprache:**
  - D3 zieht Design als Datenbankfeld zurück.
  - Statt einer eigenen Schaltfläche ein Fenster „Umrechnen …“ mit Liste.
  - Begriffe: deutsch Alltagswörter, englisch Ü1.
  - P2 nur mit sichtbarer Rückmeldung.
- **Top 3:** F1/N1/T2 · D2/F2 · D1/K1/F3.

### Funktionalität
- **Beiträge:**
  - F1: gemeinsame Lese-Stelle für die Rezeptmenge (belegte Fehlfälle).
  - F2: Klammern mit Würfel/Päckchen/packet/grams, krumme Faktoren, gemeinsame Einheitenliste.
  - F3: Faktor statt Portionen mit Faktorquellen, Felder `quantity`/`unit` nicht befüllen, keine Hefe nach Gehzeit.
- **Aussprache:**
  - Gegen ein eigenes Backform-Feld, weil es doppelt zum Text in `recipeYield` stünde.
  - Ein Countdown muss die Endzeit speichern.
  - F3 geht in K1 auf.
- **Top 3:** F1/N1 · F2/D2 · K1 mit D1/F3 unter N2.

### Sicherheit
- **Beiträge:**
  - S1: Timer zuerst ohne Berechtigung, Uhr-App nur nach Freigabe, kein Hintergrund-Wecker.
  - S2: nur rechnen, nichts speichern. Beim Speichern Wertebereiche an jedem Eingang prüfen, denn das Wiederherstellen prüft Portionen heute nicht.
  - S3: ehrliche Hinweise, keine Gehzeit-Empfehlungen, Vorlagen nur selbst geschrieben.
- **Aussprache:**
  - `SET_ALARM` ist akzeptabel, aber erst nach Freigabe.
  - Der Hinweis zur Backzeit muss beide Richtungen abdecken.
  - S2 wird Abnahmebedingung.
- **Top 3:** N2 (mit S2/P3) · F1+N1 · S3+T3.

### Kreativität
- **Beiträge:**
  - K1: Umrechnen nach einer Zutat, auch ohne Portionen.
  - K2: Backform umrechnen.
  - K3: Teig-Übersicht nach 1.0.
- **Wertvollste Deutung:** Umrechnen nach dem, was beim Backen zählt (Zutat, Form, Teigmenge), im Rezept statt als eigenes Werkzeug.
- **Aussprache:**
  - Gegen ein eigenes Backform-Feld.
  - P2 geht nur mit großzügiger Frist, gehört zu #61.
  - K3 herabgestuft, Rest in die Besprechung „Variationen“.
- **Top 3:** F1+N1+T2 · K1+D1+F3 mit D2/F2 · K2+Ü2.

### Konnektivität
- **Beiträge:**
  - N1: Backform beim Übernehmen richtig erkennen (nachgerechnete Fehlfälle).
  - N2: keine neue Datenart, Backform bleibt Einheit, Rechner-Ergebnisse höchstens als normaler Entwurf.
  - N3: Schlagwörter aus Rezeptdateien wieder einlesen (#57).
- **Aussprache:**
  - Ein eigenes Feld bringt beim Austausch nichts, weil schema.org kein Backform-Feld hat.
  - Die Erkennung muss alle Schreibweisen kennen.
  - Der Uhr-App einen neutralen Namen übergeben.
  - N1 geht in F1 auf, N2 wird Grundsatz.
- **Top 3:** F1 (+N1, T2) · K1/D1/F3 · F2+D2.

### Performance
- **Beiträge:**
  - P1: Gehzeiten an die Uhr-App, kein eigener Hintergrund-Timer.
  - P2: Bildschirm in langen Wartezeiten freigeben.
  - P3: Rechnen nur für die Anzeige. Eingaben im Zwischenspeicher, nicht in der Datenbank, die bei jedem Speichern alles neu schreibt. Kein Schieberegler. Vorlagen ohne Fotos.
- **Aussprache:**
  - Kein Sekunden-Countdown.
  - P1 auf „später“ herabgestuft.
  - P2 abgeschwächt: 30 Minuten oder beim Beginn einer Wartezeit, nach 1.0.
- **Top 3:** F1+N1 · N2+P3 · F3/K1/D1 mit F2/D2.

### Texte
- **Beiträge:**
  - T1: Alltagswörter als feste Begriffe vorab.
  - T2: „2 Springform (26 cm)“ ist heute falsch, richtig wäre „Springform (26 cm): 2“.
  - T3: ein Verb „umrechnen“ mit ehrlichen Hinweisen, kein Fantasiename.
- **Aussprache:**
  - Englisch übernimmt Ü1: pan, „parentheses“, hydration.
  - „Teigling“ auch in der Oberfläche vermeiden.
  - Feldhinweis mit Ø.
  - Ein einheitliches Hinweismuster.
  - „Früher prüfen, ob der Kuchen durch ist“ statt „Garprobe“.
- **Top 3:** F1+N1+T2 · T1+Ü1 in CLAUDE.md vor dem ersten Code · K1/D1/F3 mit D2 und einheitlichem Hinweis.

### Übersetzungen
- **Beiträge:**
  - Ü1: Begriffe vorab festlegen. „Teig“ hat drei englische Wörter, amerikanisches Englisch als Regel.
  - Ü2: Backform in cm und Zoll erkennen, Mehrzahl je Form.
  - Ü3: Zahlen wie im Handy eingestellt, Hefepackungen nur als Anzahl, Mehltypen nie gleichsetzen.
- **Aussprache:**
  - Übernimmt die deutschen Alltagswörter aus T1.
  - Englisch: dough ball, hydration, „% of flour“, pan.
  - Hinweise über Platzhalter.
  - °C/°F zurückgezogen.
- **Top 3:** F1+N1+T2 · T1+Ü1+T3 · K1+D1+F3.
