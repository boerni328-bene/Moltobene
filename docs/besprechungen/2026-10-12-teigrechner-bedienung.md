# Besprechung vom 12.10.2026: Teigrechner – Regler, Voreinstellungen, Rezeptvorschau

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Performance, Texte, Übersetzungen, Marketing
**Moderation:** Claude · **App-Version:** 0.34.0 (auf `main`), 0.35.0 in Arbeit · **Issue:** #83

Die Beiträge der Spezialisten sind von Claude zusammengestellt (simulierte Besprechung wie bisher), gestützt auf den heutigen Code (`ui/dough/DoughScreen.kt`, `data/dough/DoughLimits.kt`, `DoughMemory`).

## Anlass (Projektinhaber, wörtlich, 12.10.2026)

> „Im Rechner wäre es angenehm auch wahlweise Schieberegler anstatt der Werte-Eingabe zu verwenden. Standard-Werte sollten markiert sein. Zusätzliche plus Minus Buttons für Feineinstellungen. Mehrere sinnvolle Presets (veränderbar) zum durchschalten wären cool. Dropdownfelder wären für manche Einstellungs-Werte evtl. auch hilfreich zur besseren Bedienungsfreundlichkeit. Das sich live verändernde Rezept über „Als Rezept speichern“ könnte man für bessere Lesbarkeit designen, bzw. ansprechender gestalten.“

## Stand heute (Fakten aus dem Code)
- Jedes Maß ist ein **Zahlenfeld** (Anzahl, Stückgewicht, Wasseranteil, Salz, Öl, Hefe, Vorteig-Mehl und -Wasser) mit Endung „g“ oder „%“. Grenzen stehen zentral in `DoughLimits` (z. B. Pizza: Wasser 45–100 %, Salz 0–5 %, Hefe 0,01–5 %); außerhalb wird nicht gerechnet.
- **Hefeart** (4) und **Vorteig** (4) sind Chips; die Teigart ist eine Chip-Reihe oben.
- Startwerte je Teigart in `DoughLimits.defaults` (Pizza: 4 Kugeln à 250 g, 65 % Wasser, 2,8 % Salz, 0,2 % Frischhefe; Brot: 1 Laib à 750 g, 70 %, 2 %, 0,5 %). „Zurücksetzen“ stellt sie wieder her; die zuletzt benutzten Werte werden je Teigart gemerkt.
- Unten steht immer die Leiste mit Summenzeile und „Als Rezept speichern“. Die ausgeschriebene **Rezeptvorschau** (Zutaten in Gramm) erscheint heute **nur mit Vorteig**; sonst sieht man nur die Summenzeile.
- Bei Schrift 200 % wandern die Summen ins Formular, damit die Leiste klein bleibt.
- **Auffälligkeit (Performance):** Die gültigen Werte werden bei **jeder** Änderung gespeichert (`LaunchedEffect(input)` → `viewModel.remember`). Bei einem Regler wären das Dutzende Schreibvorgänge pro Sekunde.

## Einigkeit

1. **Plus/Minus an jedem Maß** (Design, Funktionalität): Eingabefeld bleibt, daneben „−“ und „+“ (Tippfläche mindestens 48 dp), Schrittweite je Feld sinnvoll (Wasser 1 %, Salz 0,1 %, Hefe 0,05 %, Stückgewicht 10 g, Anzahl 1); gedrückt halten wiederholt. Beschreibung für den Screenreader mit Feld, Richtung und Schritt („Wasseranteil um 1 Prozentpunkt erhöhen“).
2. **Standardwert sichtbar** (Design, Texte): am Feld eine kleine Markierung „Standard“ (Text, nicht nur Farbe), beim Regler eine Marke auf der Leiste; weicht der Wert ab, erscheint ein Knopf „Standard“ für dieses Feld (zusätzlich zum bestehenden „Zurücksetzen“ für alles).
3. **Regler nur zusätzlich und wahlweise** (Design, Funktionalität): Die Zahl bleibt immer sichtbar und bearbeitbar; der Regler steht darunter, wenn er eingeschaltet ist (ein Umschalter „Zahlen | Regler“ oben im Rechner, gemerkt). Der Regler zeigt den **praktischen** Bereich (z. B. Wasser 55–85 %), nicht die ganze erlaubte Spanne; wer mehr will, tippt den Wert. Hefe auf einer **gestauchten Skala** (feine Schritte im unteren Bereich), da 0,05 % und 1 % sonst nicht gleichzeitig treffbar sind. Die Grenzen aus `DoughLimits` gelten unverändert; nichts wird still zurechtgeschnitten.
4. **Speichern gebremst** (Performance): Gemerkt wird erst nach einer kurzen Pause bzw. wenn der Regler losgelassen wird, nicht bei jeder Bewegung. Das Rechnen selbst ist reine Arithmetik und unkritisch; die Vorschau wird nur neu gezeichnet, wenn sich Werte ändern.
5. **Rezeptvorschau** (Design, Texte): immer da, nicht nur mit Vorteig – eigener Abschnitt „So wird das Rezept“ über der Leiste mit „Als Rezept speichern“, ruhige Karte mit
   - Titelzeile (z. B. „4 Teigkugeln · 1.000 g Teig“),
   - Zutaten als **zweispaltige Liste** (Name links, Menge rechtsbündig in Gramm; Vorteig und Hauptteig mit Zwischenüberschriften; „% vom Mehl“ klein dahinter),
   - zuletzt geänderte Zeilen kurz hervorgehoben, **mit Text/Symbol**, nicht nur Farbe,
   - größere Schrift für die Mengen (aus Armlänge lesbar), Schriftgröße nur über `MaterialTheme.typography`.
   Bei Schrift 200 % einspaltig untereinander; die Karte darf einklappen.
6. **Screenreader** (Design, Sprache der Oberfläche): Die Vorschau meldet nur die Summenzeile als höfliche Live-Region, **nicht** jede Zeile bei jeder Änderung; einzelne Zeilen lesbar beim Durchgehen.
7. **Voreinstellungen** (Funktionalität, Kreativität, Marketing): Auswahl **oben je Teigart** als „Voreinstellung 2/5 · Name“ mit Pfeilen zum Durchschalten (und Liste zum direkten Wählen). Antippen lädt die Werte in den Rechner; danach frei änderbar. **Mitgelieferte** bleiben unverändert; geändert wird eine Kopie. „Als Voreinstellung speichern“ mit eigenem Namen, Löschen nur bei eigenen. Die zuletzt benutzten Werte bleiben als eigener Eintrag „Zuletzt benutzt“.
8. **Dropdown** (Design, Funktionalität): Chips bleiben, solange alle Möglichkeiten auf die Zeile passen; ein **Auswahlfeld** nur dort, wo viele Möglichkeiten sind oder die Schrift groß ist (Hefeart, Vorteig bei Schrift ab 130 %; später Backform, Mehlart, Teigart bei Pasta/Kuchen). Die Erklärsätze (z. B. „Biga: Fester Vorteig …“) bleiben darunter sichtbar.
9. **Datenschutz/Sicherheit:** keine neue Berechtigung, keine Verbindung. Gemerkte und eigene Voreinstellungen werden beim Laden gegen `DoughLimits` geprüft (wie `DoughMemory`), ungültige verworfen; Namen eigener Voreinstellungen begrenzt (Länge, Anzahl, etwa 20).
10. **Feste Begriffe** vor dem ersten Text in CLAUDE.md: **Voreinstellung / preset**, **Regler / slider**, **Standardwert / default value**, **Rezeptvorschau / recipe preview**. Nicht „Vorlage“ (belegt für die Originalseite), nicht „Profil“.
11. **Übersetzungen:** Mitgelieferte Voreinstellungen haben feste englische Kennungen (z. B. `neapolitan`, `pan_pizza`), Anzeige aus `strings.xml`; eigene Namen werden nie übersetzt. Zahlen im Format des Handys (Dezimalzeichen), englisch amerikanisch; keine Umrechnung in Cups oder Unzen.

## Streitpunkte

- **Regler standardmäßig an oder aus?** Design: aus (ruhiger, Zahlen bleiben die Wahrheit), Kreativität/Marketing: an (der Regler ist das Besondere und zeigt die Wirkung). Kompromiss-Vorschlag: aus, aber ein deutlich sichtbarer Umschalter oben.
- **Dropdown grundsätzlich oder nur bei großer Schrift:** Funktionalität: überall dort, wo mehr als drei Möglichkeiten sind; Design: Chips sind schneller, ein Tipp statt zwei. Mehrheit: nur bei großer Schrift und bei vielen Möglichkeiten.
- **Mitgelieferte Voreinstellungen in der ersten Fassung?** Funktionalität/Sicherheit: erst nach Prüfung der Werte (Richtwerte sind „eigene Festlegung“ wie die Grenzen, nicht aus Quellen kopiert; Abgleich wie bei den Grundrezepten, Quellenliste in `docs/recherche/`). Kreativität: gleich wenige (3 je Teigart).
- **Eigene Voreinstellungen in der Sicherungsdatei?** Sicherheit/Funktionalität: sie sind Inhalte des Nutzers und gehören beim Umzug mit; das braucht eine neue **Formatversion** der Sicherung (alte bleiben lesbar). Performance/Fokus: erste Fassung nur lokal wie die „zuletzt benutzten Werte“, Sicherung als eigener Schritt.
- **Vorschau einklappbar?** Design: ja (mehr Platz für Eingaben bei kleinem Bildschirm); Texte: egal.

## Richtwerte für Voreinstellungen (Vorschlag, erst nach Prüfung festlegen)
- **Pizza:** Neapolitanisch (Wasser um 60–62 %, Salz um 2,8 %, wenig Frischhefe bei langer Gehzeit), Pizza vom Blech (Wasser um 70–75 %), Pfannenpizza/Hausofen (Wasser um 65 %).
- **Brot:** Weißbrot (um 65 %), Mischbrot (um 75 %), mit Sauerteig oder Poolish.
- Pasta und Kuchen haben eigene Eingaben; dort zunächst Plus/Minus und Standard-Markierung, Voreinstellungen später.
- Alle Zahlen sind Platzhalter für die Prüfung; **nichts wird übernommen, bevor es mit mehreren Quellen abgeglichen und in eigenen Worten festgehalten ist.**

## Empfohlene Reihenfolge
1. **Etappe 1 (klein, sofort nützlich):** Plus/Minus, Standard-Markierung und „Standard“-Knopf je Feld, neue **Rezeptvorschau**, Speichern gebremst; Begriffe in CLAUDE.md. Gilt für Pizza/Brot sowie Pasta und Kuchen soweit die Felder gleich sind.
2. **Etappe 2:** Regler (wahlweise, praktische Bereiche, Hefe gestaucht) und Auswahlfelder bei großer Schrift/vielen Möglichkeiten.
3. **Etappe 3:** Voreinstellungen (mitgelieferte nach Prüfung, durchschalten, eigene speichern, später in der Sicherung).
Jede Etappe mit Rundgang in vier Darstellungen und Tests für Schrittweiten, Grenzen und Voreinstellungen (reines Kotlin, ohne Oberfläche).

## Offene Entscheidungen des Projektinhabers
Reihenfolge der Etappen; Regler standardmäßig aus oder an; Dropdown nur bei großer Schrift; Voreinstellungen zuerst nur lokal oder gleich mit Sicherung; ob die Rezeptvorschau einklappbar sein soll.
