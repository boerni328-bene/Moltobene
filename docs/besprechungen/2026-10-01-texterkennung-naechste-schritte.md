# Besprechung vom 01.10.2026: Texterkennung – was als Nächstes verbessern (Stand 0.6.0)

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.6.0

## Tagesordnung

1. Wo liefert die Texterkennung heute die meisten Fehler bzw. den meisten Nacharbeitsaufwand (Kochbuchfotos, Bildschirmfotos, schiefe oder gewölbte Seiten, mehrspaltige Seiten, Handschrift)? Was bringt Nutzern am meisten?
2. Bedienung: Ist der Ablauf (Foto wählen → Bereich wählen → erkennen → Entwurf prüfen → „Foto behalten?“) verständlich und barrierefrei? Wie lassen sich Erkennungsfehler leicht finden und korrigieren?
3. Kosten: App-Größe, Geschwindigkeit und Speicher auf einem günstigen Android-8-Handy – was ist nötig, was vertretbar?
4. Abgrenzung: Was gehört jetzt zur Texterkennung, was wartet auf spätere Etappen (Übernehmen von Internetseiten und geteiltem Text, PDF)?

## Lagebericht

- Seit 0.5.0 ist die Texterkennung fest eingebaut. Sie nutzt Tesseract4Android 4.9.0 und läuft nur auf dem Handy, ohne Internet und ohne Google-Dienste. Mitgeliefert sind Sprachpakete für Deutsch, Englisch, Italienisch, Französisch und Spanisch (zusammen rund 11,8 MB). Die Sprache erkennt die App selbst.
- Bisherige Schritte:
  - 0.5.1: Frage „Foto behalten?“ und Lizenzhinweise.
  - 0.5.2: Zutaten-Tabellen werden Zeile für Zeile gelesen.
  - 0.6.0: Bereich auswählen; Reste von Symbolen und Leisten fallen weg.
- Das Ergebnis landet im Bearbeiten-Formular. Der vollständige erkannte Text wird beim Rezept gespeichert.
- Die App-Größe ist von 1,5 MB (0.4.1) auf 21,9 MB (0.6.0) gestiegen.
- Offen ist nur Issue #16 (Hinweis auf neue Version); es betrifft das Thema nicht.
- Laut Nachtrag zur Besprechung vom 29.09.2026 folgt nach der Texterkennung das Übernehmen von Internetseiten, geteiltem Text und Zwischenablage. Erst damit kommt die Internet-Berechtigung in die App.

## Beschlussvorschläge (gemeinsame Rangfolge)

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Nichts geht verloren, wenn die Erkennung unterbrochen wird.** Gewählte Seiten werden sofort als eigene Dateien in den Zwischenspeicher der App kopiert. Seiten und Bereiche werden erst nach dem Erkennen oder Verwerfen gelöscht. Ein neues Rezept wird nach dem Ergebnis auch dann als Entwurf gespeichert, wenn die App im Hintergrund ist. Nach einem Neustart erscheint „Texterkennung wurde unterbrochen“ mit „Erneut erkennen“. Zwischenkopien enthalten noch den Aufnahmeort: Sie werden danach gelöscht, Reste beim App-Start aufgeräumt. | F1, N2 | alle 8 (8× Top 3, davon 7× Platz 1) | hoch | mittel | **#37** |
| 2 | **Prüfen leicht gemacht – ein gemeinsamer Ablauf.** <br>• „Seiten ansehen“: alle gelesenen Seiten bis zum Speichern als Vollbild mit Zoom, dazu Schaltflächen +/− mit Beschreibung für den Screenreader. Geladen wird nur die sichtbare Seite. <br>• Der Prüfhinweis bleibt bis zum Speichern sichtbar: „Text ins Rezept übernommen. Mengen und Einheiten auf Lesefehler prüfen.“ <br>• Statt mehrerer Fenster nacheinander gibt es **eine einzige Frage, und zwar beim Speichern**, mit einem klareren Text als heute. Seit der Entscheidung zu Streitpunkt 1 geht es dabei auch um die Originalseite, z. B. „Als Originalseite behalten“ / „Als Rezeptfoto verwenden“ / „Entfernen“. <br>• „Erkannten Text entfernen“, mit Rückfrage „Erkannten Text wirklich entfernen?“ und der Schaltfläche „Entfernen“. <br>• Nebenbei (kleiner Aufwand, kam in keiner Top 3 vor): Fehlermeldungen nennen den nächsten Schritt, und der Starttext wird kürzer und enthält einen Fototipp (T3). | D1, T1, T2, S1, T3 | alle 8 (8× Top 3) | hoch | mittel | **#38** – mit Originalseite (Streitpunkt 1); baut auf #37 auf |
| 3 | **Bereiche zuordnen.** Auf einer Seite lassen sich mehrere Rahmen ziehen, jeder mit der Bezeichnung „Titel“, „Zutaten“, „Zubereitung“ oder „Alles“. Bezeichnete Rahmen gehen ohne Raten direkt ins passende Feld; das löst mehrspaltige Kochbuchseiten. Regeln: <br>• Die Bezeichnung steht als Text am Rahmen, nicht nur als Farbe. <br>• Es gelten die festen Begriffe; intern werden englische Kennungen gespeichert. <br>• „Ganze Seite“ bleibt die Voreinstellung, damit alles auch mit Screenreader ohne Ziehen geht. <br>• Die Rahmenlinie wird zweifarbig, damit sie auf hellen und dunklen Fotos mindestens 3:1 Kontrast hat (D3). | K1, D3 | Performance, Funktionalität, Kreativität, Übersetzungen, Design (5× Top 3) | hoch | mittel | **#39** – #42 vorher als Grundlage; Entzerren später (Streitpunkt 2) |
| 4 | **Quelle gleich miterfassen (Buch und Seite).** Kein zusätzliches Fenster: Am Feld „Quelle“ erscheinen zuletzt genutzte Bücher und die erkannte Seitenzahl als antippbare Vorschläge. Die Seite wird als Zahl in einem eigenen Feld gespeichert, nicht als Text „S. 47“. Das Datenmodell kennt Buch und Seite bereits, und beim Teilen werden sie schon mitgegeben. | K2 | Konnektivität, Texte (2× Top 3) | hoch | klein | **#40** |
| 5 | **Prüfsummen für Tesseract und Sprachpakete.** Der Build prüft Tesseract4Android, das von JitPack kommt und nicht signiert ist, gegen einen festen Fingerabdruck (SHA-256). Ein Test vergleicht die fünf Sprachpakete mit ihren bekannten Prüfsummen. Muss erledigt sein, bevor die Internet-Berechtigung kommt. | S2 | Sicherheit (1× Top 3), Konnektivität | mittel | klein | **#41** – vor der Etappe „Übernehmen“ |
| 6 | **Übernahme-Logik festigen.** Das Zusammenführen ins Formular (nur leere Felder füllen, ergänzen, Doppeltes weglassen) wird als eigener, getesteter Baustein aus dem Formular herausgelöst. Er wird auch für geteilten Text und Internetseiten gebraucht. Dabei werden zwei Fehler behoben: <br>• Die Prüfung auf Doppeltes arbeitet künftig Zeile für Zeile; heute wird z. B. „Salz“ nicht ergänzt, wenn „1 TL Salz“ schon dasteht. <br>• Die Rezeptsprache wird nur gespeichert, wenn sie wirklich erkannt wurde, und nicht mehr bei jedem Durchlauf überschrieben (im Code bestätigt). Unklare Sprache wird auf den folgenden Seiten erneut geprüft. | F3, Ü1 | Funktionalität, Konnektivität, Übersetzungen, Kreativität | mittel | klein–mittel | **#42** – mit Sprachauswahl (Streitpunkt 3); Grundlage für #39 und die nächste Etappe |
| 7 | **Erkennung schneller und mit Rückmeldung.** <br>• Tesseract wird nur einmal je Durchlauf gestartet; das Sprachpaket wird nur bei einem Sprachwechsel neu geladen. <br>• „Abbrechen“ wirkt auch in den ersten Sekunden. <br>• Ein Fortschrittsbalken zeigt den Stand; er wird nur in ganzen Prozentschritten aktualisiert. <br>• Der Screenreader sagt in ganzen Sätzen an, was passiert („Seite 2 von 3 wird gelesen“). | P2, D3, F3 (Abbruch) | Design, Funktionalität, Performance, Übersetzungen | mittel | klein–mittel | **#43** |
| 8 | **Den erkannten Text besser aufteilen, mit gemeinsamen Tests.** <br>• Seitenzahlen werden auf jeder Seite einzeln entfernt. <br>• Eine Zeile „47“ gilt nicht mehr als Zutat. <br>• Fehlende Einheiten kommen dazu: „gr“, „Esslöffel“, „tbsp.“, „c. à s.“, „cda.“ und weitere. <br>• Überschriften werden ohne Rücksicht auf Akzente verglichen, so wird auch „Elaboraciön“ erkannt. <br>• Tests auch mit kurzen Ausschnitten. | F2, Ü2 | Funktionalität, Übersetzungen, Kreativität | mittel | klein | **#44** |
| 9 | **Kleinere App, weniger Ballast beim Umzug.** Die Programmbibliotheken werden gepackt ausgeliefert (`useLegacyPackaging`): Die APK schrumpft voraussichtlich auf 14–15 MB, nach dem Build nachmessen. Die Sprachpakete kommen in einen Ordner, der beim Umzug von Handy zu Handy nicht mitgeht (`noBackupFilesDir`). Die alte Kopie wird dabei gelöscht. | P1, S (Randnotiz) | Performance, Sicherheit, Konnektivität, Funktionalität | mittel | klein | **#45** |
| 10 | **Bilder per „Teilen mit…“ an Moltobene schicken, mit einheitlichen Namen.** Ein Bildschirmfoto oder Bild aus WhatsApp führt direkt zu „Bereich auswählen“. Angenommen werden nur Inhalte über `content://`, mit Prüfung von Dateityp, Anzahl und Größe. Zusammen damit bekommen alle Erfassungswege einheitliche Namen: „Aus Foto / Link / Text übernehmen“; „Text erkennen“ bleibt nur die Schaltfläche in „Bereich auswählen“. | N1, T (Hinweis) | Kreativität, Konnektivität, Sicherheit (bedingt), Übersetzungen | mittel | mittel | **#46** – Einstieg in die Etappe „Übernehmen“ |
| 11 | **PDF-Seiten lesen** mit dem eingebauten Android-Werkzeug `PdfRenderer`, ohne neue Bibliothek und ohne größere App. Die Seiten werden einzeln umgewandelt; Seitenzahl und Bildgröße sind begrenzt. Es gilt als feste Regel: Tesseract bekommt nur Bildpunkte, nie Dateien (Schutz vor bekannten Lücken in der eingebauten PNG-Bibliothek). Kommt nach Rang 10, weil es denselben Weg über „Teilen mit…“ nutzt. | N3, S3, P3 | Konnektivität, Sicherheit, Performance | niedrig | mittel | **#47** |

## Einigkeit

- **Rang 1 hat Vorrang vor allem anderen.** Alle acht halten den möglichen Verlust eines Erkennungsergebnisses für den wichtigsten Punkt. Er widerspricht dem Grundsatz „Nichts geht verloren“. Rang 1 ist außerdem die technische Grundlage für Rang 2, denn die Seiten 2–6 sind beim Speichern nur noch lesbar, wenn sie vorher in die App kopiert wurden.
- **Nach der Erkennung höchstens eine Frage, und zwar beim Speichern.** Meldung, Foto-Frage und Quellen-Frage nacheinander wären drei Unterbrechungen und würden nur weggeklickt. Die Quelle (Rang 4) kommt deshalb als Vorschlag am Feld, nicht als eigenes Fenster.
- **Kostenrahmen:** Für die Texterkennung kommen keine neuen Bild- oder KI-Bibliotheken hinzu (z. B. OpenCV oder eigene Handschrift-Modelle). Geradeziehen, Entzerren und PDF gehen mit den vorhandenen Mitteln: Leptonica steckt schon in Tesseract, `PdfRenderer` ist Teil von Android. Weitere Sprachpakete sind derzeit nicht nötig.
- **Handschrift** wird nicht eigens erkannt. Tesseract liest sie kaum, eine eigene Erkennung wäre groß und unsicher.
- **Vor der Etappe „Übernehmen von Internetseiten“** sollen Rang 5 (Prüfsummen) und Rang 6 (Übernahme-Logik) erledigt sein. Rang 10 („Teilen mit…“ und einheitliche Namen) ist der natürliche Einstieg in diese Etappe.
- **Hinweis von Performance:** „Rezeptfoto lesen“ liest das gespeicherte Rezeptfoto, und das ist schon auf 1600 px verkleinert. Kleine Schrift wird dadurch schlechter erkannt als beim Lesen des Originalfotos. Mit Rang 1 und 2 (Seiten als eigene Dateien) lässt sich bei „Erneut erkennen“ die schärfere Vorlage verwenden.

## Streitpunkte – Entscheidung nötig

1. **„Originalseite“ dauerhaft getrennt vom Rezeptfoto speichern (K3) – jetzt oder später?**
   - *Design:* jetzt, zusammen mit Rang 2. Eine Buchseite als Vorschaubild in der Sammlung widerspricht dem Grundsatz „Fotos der Gerichte im Mittelpunkt“. Eine getrennte Originalseite löst das sauber, auch bei handgeschriebenen Familienrezepten.
   - *Kreativität (Einbringer), Funktionalität, Konnektivität:* später. Rang 2 deckt das Prüfen bis zum Speichern ab, und K3 braucht eine Datenbank-Migration und eine neue Formatversion der Sicherungsdatei.
   - *Performance:* Mit bis zu 1 MB je Seite kämen bei 1.000 Rezepten bis zu 1 GB zusammen. Nötig wären dann Größengrenze, Graustufen-JPEG und nie in Listen laden.
   - *Sicherheit:* Auf Bildschirmfotos können fremde Namen und Nummern stehen. Originalseiten müssen sichtbar und einzeln löschbar sein, und es braucht den Hinweis, dass sie mitgesichert werden.
   - **Entscheidung des Projektinhabers:** jetzt, zusammen mit Rang 2 (#38). Die Bedingungen von Performance, Sicherheit und Konnektivität gelten.
2. **Schräg fotografierte Seiten entzerren (P3: Rahmen mit vier frei verschiebbaren Ecken) – zusammen mit Rang 3 oder später?**
   - *Performance:* zusammen mit Rang 3. Zuerst die Seite entzerren, dann die Bereiche zuordnen; der Aufwand liegt bei unter 1 Sekunde Rechenzeit, und die App wird nicht größer.
   - *Design:* möglich, aber nur als zwei getrennte Schritte: erst gerade ziehen, dann Bereiche wählen. Beides auf einem Bildschirm wäre zu kompliziert.
   - *Kreativität:* erst Rang 3, das Entzerren später, damit „Bereich auswählen“ einfach bleibt.
   - **Entscheidung des Projektinhabers:** später, nach Rang 3 (#39). Dafür wird noch kein eigenes Issue angelegt.
3. **Eigene Sprachauswahl für die Erkennung („automatisch / Deutsch / English / …“)?**
   - *Übersetzungen (Einbringer):* zurückgezogen. Es reicht, die Sprache nur zu speichern, wenn sie wirklich erkannt wurde (Rang 6); so bleibt die Bedienung einfach.
   - *Performance:* dafür, denn bei gewählter Sprache entfällt das doppelte Lesen von Seite 1.
   - *Design:* Falls es sie gibt, gehört sie ins Menü oben rechts, nicht als weitere Schaltfläche.
   - **Entscheidung des Projektinhabers:** ja, im Menü oben rechts in „Bereich auswählen“; Voreinstellung „automatisch“. Aufgenommen in #42.

## Offene Fragen an den Projektinhaber

1. Welche Beschlussvorschläge sollen als GitHub-Issues angelegt werden?
2. Entscheidungen zu den drei Streitpunkten.
3. Reihenfolge: Sollen die Verbesserungen der Texterkennung, also Rang 1–4 und die kleineren Punkte, als nächste Version kommen, bevor die Etappe „Übernehmen von Internetseiten“ beginnt?

### Entscheidungen des Projektinhabers

- **Issues:** Alle 11 Beschlussvorschläge werden angelegt (#37–#47).
- **Streitpunkt 1:** Die Originalseite kommt jetzt, zusammen mit Rang 2 (#38).
- **Streitpunkt 2:** Das Entzerren kommt später, nach Rang 3 (#39).
- **Streitpunkt 3:** Es gibt eine Sprachauswahl im Menü oben rechts (#42).
- **Frage 3 (Reihenfolge)** ist noch offen und wird vor Beginn der Umsetzung geklärt.

## Zurückgezogen / bereits als Issue vorhanden

- **Zurückgezogen oder abgeschwächt:**
  - D2 (unsichere Stellen markieren): von Design auf niedrig gesetzt und zurückgestellt. Mit Rang 2 und 3 und dem genaueren Prüfhinweis bringt es weniger, als es kostet. Texte schlug vor, es mit T1 zu verbinden („3 unsichere Stellen“, dann als Mehrzahlform).
  - K2: kein eigenes Fenster, sondern Vorschläge am Feld „Quelle“.
  - K3: von Kreativität selbst auf niedrig gesetzt (siehe Streitpunkt 1).
  - T2: nur noch die Formulierung. Der endgültige Text hängt davon ab, wann gefragt wird.
  - S1: nur im Bündel mit Rang 2, dort niedrig.
  - S3: kein eigenes Issue mehr, geht in Rang 11 auf.
  - P2: Den Teil „Sprache an einem Ausschnitt erkennen“ hat Performance zurückgezogen.
  - Ü1: Die zusätzliche Sprachauswahl hat Übersetzungen zurückgezogen (siehe Streitpunkt 3).
  - N3: soll erst nach „Teilen mit…“ kommen.
  - F1: Die Meldung „Texterkennung wurde unterbrochen“ übernimmt N2.
- **Ausblick (nicht zur Abstimmung):** ein „Stapel-Modus“ für ganze Kochbücher: mehrere Fotos, je Foto ein Entwurf, das Buch voreingestellt. Er baut auf Rang 4 auf.
- **Bestehende Issues:** keine Überschneidung. #16 betrifft das Thema nicht.

## Anhang: Beiträge und Stellungnahmen

**Design:**
- Beiträge:
  - D1: Seiten beim Prüfen groß ansehen, „Foto behalten?“ erst beim Speichern
  - D2: unsichere Stellen markieren
  - D3: Fortschritt mit Ansage für den Screenreader, Rahmen-Kontrast
- Aussprache: Unterstützt K1, K3, T1 und T2. Keine Quellen-Frage als Fenster; Entzerren und Bereiche als zwei Schritte; eine Sprachauswahl höchstens im Menü. D2 zurückgestellt. Top 3: D1+K3+T1/T2, F1+N2, K1.

**Funktionalität:**
- Beiträge:
  - F1: Ergebnis geht bei Unterbrechung verloren
  - F2: Seitenzahlen bei mehreren Seiten, Zeile „47“ als Zutat
  - F3: Übernahme-Logik herauslösen; Fehler bei der Prüfung auf Doppeltes und beim Abbrechen
- Aussprache: Ü1 im Code bestätigt. D1 setzt N2 voraus; K3 später; bei P2 muss das Abbrechen neu gebaut werden; beim Verschieben der Sprachpakete die alten löschen. Top 3: F1+N2, K1 (mit F3 als Grundlage), D1+T1+T2.

**Sicherheit:**
- Beiträge:
  - S1: erkannten Text löschbar machen
  - S2: Prüfsummen für Tesseract und Sprachpakete
  - S3: Tesseract bekommt nur Bildpunkte
  - Randnotiz: Sprachpakete in `noBackupFilesDir`
- Aussprache: N1 nur mit `content://`, Typprüfung und Grenzen. Kopien aus N2 aufräumen, weil sie noch den Aufnahmeort enthalten. Originalseiten sichtbar und löschbar. S3 geht in PDF auf, S1 im Bündel mit Rang 2. Top 3: F1+N2, S2, T1+T2.

**Kreativität:**
- Beiträge:
  - K1: Bereiche zuordnen
  - K2: Quelle mit Buch und Seite
  - K3: Originalseite getrennt speichern
  - Ausblick: Stapel-Modus
- Aussprache: K2 als Vorschläge am Feld statt Fenster; K1 vor dem Entzerren; K3 herabgestuft. Zusammenlegen: F2 mit K2 (Seitenzahl als Vorschlag), P2 mit Ü1. Top 3: F1+N2, D1+T1+T2, K1.

**Konnektivität:**
- Beiträge:
  - N1: Bilder per „Teilen mit…“ empfangen
  - N2: gewählte Seiten sofort in die App kopieren
  - N3: PDF über `PdfRenderer`
- Aussprache: K2 braucht keine Änderung an der Sicherungsdatei. K3 braucht Sicherungsformat 2 und darf beim Teilen nicht mitgehen. D1 nur auf Grundlage von N2. N3 nach N1. Top 3: F1+N2, K2, D1.

**Performance:**
- Beiträge:
  - P1: Programmbibliotheken gepackt ausliefern (App etwa ein Drittel kleiner)
  - P2: Tesseract einmal je Durchlauf starten
  - P3: keine neuen Bibliotheken; Schieflage, Entzerren und PDF mit vorhandenen Mitteln
  - Hinweis: „Rezeptfoto lesen“ nutzt das verkleinerte Foto
- Aussprache: D1 nur die sichtbare Seite laden; K3 braucht wegen des Speicherbedarfs Grenzen; Fortschritt nur in Prozentschritten aktualisieren. Spracherkennung am Ausschnitt zurückgezogen. Top 3: F1+N2, K1, T1+T2.

**Texte:**
- Beiträge:
  - T1: Prüfhinweis geht unter
  - T2: Dialog „Foto behalten?“ missverständlich
  - T3: Fehlermeldungen mit nächstem Schritt, kürzerer Starttext
  - Hinweis: einheitliche Namen „Aus Foto / Link / Text übernehmen“
- Aussprache: Meldungen und Rückfragen nach den Textregeln; nach der Erkennung nur ein Schritt; Screenreader-Ansagen als ganze Sätze; bei K1 die festen Begriffe. T2 auf die Formulierung abgeschwächt. Top 3: F1+N2, T1+D2, K2 im Bündel mit T2/D1/K3.

**Übersetzungen:**
- Beiträge:
  - Ü1: Rezeptsprache wird geraten und still gespeichert
  - Ü2: Einheiten der fünf Sprachen ergänzen, Überschriften ohne Akzente vergleichen
- Aussprache: Zähler als Mehrzahlformen; „bis zu 6 Seiten“ als Platzhalter; Seite als Zahl statt „S. 47“; Rahmen-Bezeichnungen mit englischer Kennung. Sprachauswahl zurückgezogen. Top 3: F1+N2, T1+T2+D1, K1.
