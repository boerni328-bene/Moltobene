# Besprechung vom 04.10.2026: Allgemeiner Stand des Projekts – was ist jetzt am wichtigsten?

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.14.0 (versionCode 30)

## Tagesordnung

Thema: Allgemeiner Stand des Projekts – was ist jetzt am wichtigsten?

1. Wo hat die App, gemessen an den fünf Kernfunktionen der Vision, die größten Lücken? Was fehlt im Alltag am meisten?
2. Welche Etappe kommt als Nächstes: „Übernehmen von Internetseiten“, „Kochen“ oder „Finden und ordnen“?
3. Gibt es nach dem Umbau der Texterkennung in 0.14.0 Risiken oder Nacharbeiten, die vorher erledigt sein sollten? 0.14.0 brachte eine neue Bibliothek (ONNX Runtime), und die App wuchs von 14 auf etwa 45 MB.
4. Was muss stehen, bevor die App als 1.0 gelten bzw. öffentlich bekannt gemacht werden kann?

## Lagebericht

- **Stand:** Version 0.14.0 vom 03.10.2026. Alle Builds sind grün, auch der Rundgang auf dem Emulator. Bildschirmfotos der Version lagen allen Teilnehmern vor.
- **Kernfunktion 1 „Rezept hinzufügen“:** fertig.
- **Kernfunktion 2 „Übernehmen“:**
  - Vorhanden: „Aus Text übernehmen“ und „Aus Foto übernehmen“ (mehrere Seiten, neue Texterkennung PP-OCRv6) sowie „Teilen mit…“ für Text und Bilder.
  - Fehlt: Internetseiten werden nicht gelesen, ein geteilter Link wird nur als Quelle gespeichert. Ebenso fehlen PDF (#47), YouTube und Dateien. Auch eine von Moltobene selbst geteilte Rezeptdatei kann die App nicht einlesen.
- **Kernfunktion 3 „Finden und ordnen“:** nur die Suche. Es gibt keine Schlagwörter, keine Favoriten und kein Sortieren.
- **Kernfunktion 4 „Kochen“:** nur „Bildschirm bleibt an“. Abhaken und Portionen umrechnen fehlen.
- **Kernfunktion 5 „Teilen, sichern, umziehen“:** fertig.
- **Etappenplan:** Der Plan vom 29.09. wurde am 01.10. bewusst umgestellt, die Texterkennung kam zuerst. Ihre Verbesserungen (#37–#46) sind erledigt. Die Vorbedingungen für die erste Internet-Funktion (#41, #42, #46) sind erfüllt.
- **Offen:**
  - Issues #16 (Nach neuer Version suchen) und #47 (PDF) sowie der Dependabot-Vorschlag #48.
  - Die Testsammlung mit 1.000 Rezepten (Beschluss vom 29.09., Rang 9) gibt es noch nicht.

## Wichtigster Befund: Microsoft-Telemetrie in der veröffentlichten Version 0.14.0

Sicherheit hat ihn eingebracht, der Moderator hat ihn an der Release-APK nachgeprüft. Die Prüfsumme der APK stimmt mit dem Release überein.

- **Was in der APK steckt:**
  - Das Manifest der APK enthält die Berechtigungen `android.permission.INTERNET` und `ACCESS_NETWORK_STATE`. Dazu kommt ein eigener Startbaustein `ai.onnxruntime.TelemetryInitializer` mit der Kennung `com.moltobene.app.onnxruntime_telemetry_initializer`.
  - Die mitgelieferte Programmbibliothek `libonnxruntime.so` enthält das Sendeziel `events.data.microsoft.com` und das Microsoft-Sendewerkzeug „1DS“.
- **Herkunft:** All das kam unbemerkt mit der Bibliothek `onnxruntime-android` in die App. Im Repository steht nichts davon, der Code der App selbst nutzt das Internet nicht.
- **Was offen ist:** Ob tatsächlich Daten gesendet wurden, ließe sich nur mit einem Mitschnitt des Netzverkehrs belegen. Laut Microsoft ist die Telemetrie in den offiziellen Fassungen eingeschaltet. Laut Berichten anderer Projekte startet der Baustein bei jedem App-Start und meldet Gerätedaten (z. B. die Android-ID sowie Hersteller und Modell des Handys).
- **Widerspruch zu Vision und eigenen Aussagen:**
  - Die Vision verspricht „kein Tracking, keine Weitergabe an Dritte, Internet nur auf Wunsch“.
  - README (Zeile 5 und 56) und `SECURITY.md` sagen: keine Berechtigungen. Das stimmt für 0.14.0 nicht.
  - Rechtlich ist das ein Risiko nach DSGVO und § 25 TDDDG, weil eine Geräte-Kennung ohne Einwilligung gelesen und weitergegeben werden kann.
- **Gezielt entfernbar:** Der Startbaustein hängt *nicht* am gemeinsamen Startbaustein `androidx.startup.InitializationProvider`. Er lässt sich deshalb gezielt entfernen, ohne andere Startbausteine (Lifecycle, Emoji2, Profileinstaller) zu verlieren. Diese Sorge hatte Funktionalität geäußert.

## Beschlussvorschläge (gemeinsame Rangfolge)

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Sofortpaket 0.14.1: Telemetrie und Berechtigungen entfernen, Berechtigungen im Build überwachen** (Einzelheiten unten) | S1, S3, T2 (Teil), P3 (Teil), F2 (Teil) | alle 8 (8× Top 3, alle auf Platz 1) | hoch | klein | **#49** |
| 2 | **Texterkennung stabil auf schwachen Handys:** kein Absturz bei Speichermangel, Entwurf vorher sichern, Modell außerhalb des Java-Speichers laden, Dauer und Speicher messen | F1, P2 | alle 8 (8× Top 3, alle auf Platz 2) | hoch | mittel | **#50** – baut auf #37 auf |
| 3 | **Sichern verlässlich machen:** Datei nach dem Schreiben prüfen, „Zuletzt gesichert“ anzeigen, „Speicher voll“ als eigene Meldung | N3 | Übersetzungen, Konnektivität, Funktionalität (3× Top 3); Sicherheit, Design, Kreativität, Performance, Texte | hoch | klein | **#51** – #29 |
| 4 | **Prüflauf wie beim Nutzer:** Rundgang bzw. Rauchtest mit einer Fassung wie der veröffentlichten (R8), Lauf auf Android 8 mit wenig Speicher, Testsammlung mit 1.000 Rezepten und Messwerten | F2, P1 | Sicherheit, Performance (2× Top 3); Funktionalität, Konnektivität, Kreativität | hoch | mittel | **#52** – Rang 9 vom 29.09. |
| 5 | **Foto-Übernahme bei großer Schrift und veraltete Hinweise:** „Bereich auswählen“, Seitenübersicht und „Aus Text übernehmen“ bei 200 % bedienbar; Hinweise zu zwei Spalten berichtigen; einheitlich „auswählen“ | D1, T1 | Design, Texte (2× Top 3); Kreativität | hoch | klein | **#53** – #39, #46 |
| 6 | **Etappe „Übernehmen“, Schritt A – Rezeptdateien einlesen (ohne Internet):** ein gemeinsamer, abgesicherter Leser für schema.org/Recipe | N1, F3, K1 (A) | Kreativität (Top 3); als nächste Etappe von 7 der 8 genannt | hoch | mittel | **#54** – Grundlage für Rang 7 |
| 7 | **Etappe „Übernehmen“, Schritt B – „Aus Link übernehmen“:** feste Regeln für Netz und Sicherheit, Datenschutzerklärung, Textpaket, gemeinsam mit „Nach neuer Version suchen“ | N2, S2, T2, Ü3, K1 (B) | Konnektivität, Sicherheit, Texte, Übersetzungen, Funktionalität, Performance | hoch | mittel–groß | **#55** – umfasst #16 |
| 8 | **Etappe „Kochen“ (schlank, in der Rezeptansicht):** abhaken, aktueller Schritt, Portionen umrechnen; vorher Zahlen richtig lesen und „Einheit“ statt „Angabe“ | K2, D2, Ü2, T3 (Teil) | Design, Funktionalität, Übersetzungen, Kreativität | hoch | mittel | **#56** – Beschluss 29.09. (Originalzeile + Menge) |
| 9 | **Etappe „Finden und ordnen“ (schlank) und Abnahmeliste für 1.0** | K3, D3, Ü1 | Funktionalität, Übersetzungen, Performance (Bedingung: erst nach Rang 4) | mittel | mittel | **#57** |
| 10 | **Kleiner Texte-Durchgang:** Leerzustand, Kernsatz in der App, „wiederherstellen“ statt „übernehmen“ bei der Sicherung, englischer Absatz in der Release-Beschreibung | T3 (Rest), Ü3 (Rest) | Übersetzungen, Design | mittel | klein | **#58** – Beschluss 29.09. (Kernsatz in `strings.xml`) |
| 11 | **App wieder kleiner:** Lesemodell auf 16 bzw. 8 Bit prüfen, nur wenn die Erkennung nicht schlechter wird | P3 (Rest) | – | niedrig | mittel | **#59** – nach 1.0 |

### Rang 1 im Detail: Sofortpaket 0.14.1

- **Im Manifest entfernen** mit `tools:node="remove"`:
  - die Berechtigungen `INTERNET` und `ACCESS_NETWORK_STATE`
  - gezielt den Provider `ai.onnxruntime.TelemetryInitializer`, aber nicht den gemeinsamen `InitializationProvider`
- **Zweite Sicherung:** `ORT_DISABLE_TELEMETRY=1` per `Os.setenv` setzen, bevor ONNX Runtime geladen wird. So bleibt die Telemetrie auch dann aus, wenn die Internet-Berechtigung mit Rang 7 gewollt zurückkommt.
- **Prüfung im Build:** Nach dem Bauen werden die Berechtigungen der fertigen APK gegen eine feste Erlaubt-Liste geprüft, die heute leer ist. Zusätzlich sind `TelemetryInitializer` und `ACCESS_NETWORK_STATE` dauerhaft verboten. Bei Abweichung bricht der Build ab. Das schützt auch vor künftigen Dependabot-Updates wie #48.
- **Prüfen, ob die Erkennung noch läuft:** an einer Fassung, die wie die veröffentlichte mit R8 verkleinert ist (Teil von F2).
- **Aufräumen beim Update:**
  - Dateien löschen, die die Telemetrie angelegt hat. Sonst gingen sie beim Umzug von Handy zu Handy mit.
  - Die alten Tesseract-Sprachpakete (rund 12 MB) beim App-Start löschen statt erst beim nächsten „Text erkennen“ (Teil von P3).
- **Ehrlicher Eintrag im CHANGELOG ohne Fachbegriffe.** Vorschlag von Texte: „Version 0.14.0 enthielt unbemerkt einen Baustein von Microsoft, der Nutzungsdaten der Texterkennung ins Internet senden konnte, und dafür die Internet-Berechtigung. Beides ist entfernt; die App hat wieder keine Berechtigung.“ „Gesendet hat“ darf nur dort stehen, wenn das belegt ist.
- **Repository:**
  - README und `SECURITY.md` bleiben dann wieder richtig. Sie sollten aber den Hinweis bekommen, dass 0.14.0 betroffen war.
  - S3: „Dependabot alerts“ und „Dependency graph“ in den GitHub-Einstellungen einschalten.

### Rang 2–8: wichtige Bedingungen aus der Aussprache

- **Rang 2:**
  - `OutOfMemoryError` und `LinkageError` abfangen, damit sie als normaler Fehler gemeldet werden. Dann bleiben die Seiten erhalten und „Erneut erkennen“ ist möglich.
  - Vor der Erkennung `saveDraft()` aufrufen.
  - `rec.onnx` nicht mehr per `readBytes()` laden, sondern als direkten `ByteBuffer` oder über den Dateipfad. Heute belegt das kurzzeitig etwa 42 MB im Java-Speicher.
  - Stellschrauben nur, wenn die Messung sie verlangt: `isLowRamDevice`, `setMemoryPatternOptimization(false)`, `allow_spinning=0`.
- **Rang 3:**
  - Zur Prüfung nur das Inhaltsverzeichnis der Datei lesen und die Einträge zählen, nicht alle Fotos noch einmal entpacken (Performance). Sonst dauert die Sicherung doppelt so lange.
  - Der Hinweis „Zuletzt gesichert: …“ steht nur in den Einstellungen (Design, siehe Streitpunkt 3).
- **Rang 4:**
  - Messungen mit der Debug-Fassung sagen wenig aus. Gemessen wird deshalb an einer Fassung, die wie die veröffentlichte gebaut ist.
  - Grenzwerte gelten zunächst nur als Warnung.
  - Rang 4 ist die Voraussetzung für Rang 9, weil „Finden und ordnen“ die Listenabfrage verändert.
- **Rang 5:** Die kürzeren Hinweistexte (T1) helfen auch bei 200 % Schrift. Beides wird in einem Durchgang gemacht.
- **Rang 6:**
  - Vorher den Ablauf „Übernahme läuft“ aus `EditViewModel` (1.052 Zeilen, ohne eigenen Test) herauslösen und testen. Er dient später auch dem Link.
  - Sicherheitsregeln für die Datei:
    - nur `content://`
    - höchstens etwa 10 MB, begrenzte Verschachtelungstiefe
    - Adressen in `image` werden nie abgerufen, `file://`- und `content://`-Angaben im Dateiinhalt werden nie geöffnet
    - `url` nur mit http/https
    - Fotos nur über `PhotoStore`
  - Was der Leser übernimmt:
    - `inLanguage` nur, wenn eindeutig
    - Portionsangaben in allen fünf Sprachen. Dabei wird der Fehler „Für 1 Zopf“ behoben: „Zopf“ geht heute verloren.
  - Die eigene Zeile „Quelle: …“ wird beim Übernehmen aus Text erkannt.
  - Getestet wird mit selbst geschriebenen Beispieldateien, als Hin-und-zurück-Test mit dem Teilen.
- **Rang 7:**
  - **Eine einzige Stelle für Internetzugriffe** (`data/web/`), gebaut mit dem in Android eingebauten `HttpURLConnection`: keine neue Bibliothek, kein WebView, kein JavaScript, keine Cookies.
  - **Nur verschlüsselt:** `usesCleartextTraffic="false"` und `network_security_config` ausdrücklich setzen, denn Android 8 erlaubt sonst http. Weiterleitungen nur auf https.
  - **Feste Grenzen:**
    - keine Adressen im Heimnetz
    - Zeitlimits von etwa 10/20 s, Seite höchstens 5 MB, Foto höchstens 10 MB
    - „Abbrechen“ wirkt sofort
    - ein eigener, knapper User-Agent, damit die Seite Android-Version und Handymodell nicht erfährt
  - **Ablauf:**
    - Erst den Entwurf mit dem Link speichern, dann laden.
    - Auswerten in dieser Reihenfolge: schema.org → Seitentext → Link-Entwurf.
    - Kein automatisches Nachholen. Stattdessen hat der Entwurf eine Schaltfläche „Aus Link übernehmen“.
  - **Meldungen** je Fall; der Text bei fehlendem Netz verweist genau auf diese Schaltfläche.
  - **Gestaltung:** ein gestalteter Ladezustand mit Ansage für den Screenreader. Die dritte Schaltfläche muss auch bei 200 % ins Auswahlfenster passen.
  - **Datenschutzerklärung:** zweisprachig über `res/raw` und `res/raw-de`, in der App und im Repository. README und `SECURITY.md` werden angepasst.
  - **#16:** über dieselbe Internet-Stelle, nur auf Knopfdruck in den Einstellungen. Die Abfrage bei GitHub kommt in die Datenschutzerklärung.
  - **YouTube:** nur als bestmöglicher Versuch über die Seite, ohne Programmzugang mit eigenem Schlüssel.
  - Der Rundgang nutzt eine nachgestellte Seite statt echtem Internet.
  - **Die Internet-Berechtigung selbst braucht eine eigene Freigabe des Projektinhabers.**
- **Rang 8:**
  - **Kein eigener Bildschirm:** Das Abhaken kommt direkt in die Rezeptansicht. Design hat D2 zugunsten von K2 abgeschwächt.
  - **Bedienregeln aus D2:**
    - ganze Zeile antippbar, mindestens 48 dp hoch
    - Häkchen und Durchstreichen, nicht nur eine andere Farbe
    - der Screenreader sagt „abgehakt“ bzw. „nicht abgehakt“ an
    - optional „Große Schrift beim Kochen“ im Menü oben
  - **Häkchen bleiben erhalten:** Sie überstehen das Drehen des Handys und das Beenden der App durch Android.
  - **Portionen umrechnen:**
    - Umgerechnet wird nur in der Anzeige, die Originalzeile bleibt.
    - Vorher muss Ü2 erledigt sein: Zahlen werden nach der Rezeptsprache gelesen, „1.000 g“ ist tausend Gramm, und Brüche wie „1 1/2“ und „1½“ werden verstanden.
    - Vorher wird außerdem das Feld „Angabe“ in „Einheit“ umbenannt (T3).
  - **Feste Begriffe:** „abhaken“ und gegebenenfalls der Name des Schalters kommen vorher in die Liste der festen Begriffe. Werden einmal Schritte geblättert, heißen die Schaltflächen „Nächster Schritt“ und „Vorheriger Schritt“, nicht „Weiter“ und „Zurück“ (Texte).

### Rang 9 im Detail: schlankes „Finden und ordnen“ und Abnahmeliste für 1.0

- **Erst nach Rang 4**, also mit Messwerten.
- **Inhalt:**
  - Favorit mit Herz
  - Schlagwörter: mitgelieferte und eigene
  - Filter „Favoriten / Entwürfe / Schlagwörter“ als eine verschiebbare Chip-Zeile unter der Suche
  - Sortieren nach Titel A–Z und „zuletzt hinzugefügt“, Sortierung im Menü oben
- **Vorher:**
  - In `recipe_tags` muss bei mitgelieferten Schlagwörtern die feste Kennung (z. B. `main_course`) der Schlüssel sein, nicht der angezeigte Name. Dafür ist ein Datenbank-Umbau mit Migration nötig, sonst entstehen nach einem Sprachwechsel Doppelte.
  - Die Titel-Sortierung muss Umlaute und Akzente richtig einordnen. Heute steht „Äpfel“ hinter „Zwiebelkuchen“.
- **Platz in der Liste (D3):** Die zwei großen Knöpfe klappen beim Blättern zu Symbolknöpfen zusammen und behalten ihre Beschreibung für den Screenreader. Technisch läuft das über `derivedStateOf`.
- **Abnahmeliste für 1.0 (Vorschlag):**
  - jede der fünf Kernfunktionen mindestens in Grundform (Rang 6–9)
  - Rang 1–4 erledigt, d. h. die Testsammlung mit 1.000 Rezepten ist bestanden und der Lauf auf Android 8 ist ohne Absturz
  - Datenschutzerklärung
  - Berechtigungsprüfung im Build
  - Dependabot-Warnungen eingeschaltet
  - die Prüfrunde vor 1.0, wie in der CLAUDE.md vorgesehen
- **Nicht nötig für 1.0:** PDF (#47), Formate anderer Rezept-Apps, Umrechnen von Einheiten, ein kleineres Modell (Rang 11).

## Einigkeit

- **Rang 1 kommt vor allem anderen.** Alle acht haben S1 auf Platz 1 gesetzt.
  - Performance ergänzt: Der Startbaustein läuft bei jedem App-Start, auch ohne Texterkennung, und meldet sich für Akku- und Netzänderungen an. Das kostet zusätzlich Startzeit und Akku.
- **Rang 2 kommt direkt danach.** Alle acht haben „Texterkennung stabil“ auf Platz 2 gesetzt. Ein Absturz im Vordergrund verliert Seiten und Eingaben und bricht das Versprechen „Nichts geht verloren“.
- **Der Schutz vor Telemetrie muss dauerhaft sein**, nicht nur „keine Internet-Berechtigung“. Mit Rang 7 kommt diese Berechtigung gewollt zurück. Deshalb bleiben auch danach drei Sicherungen bestehen:
  - der Startbaustein bleibt entfernt
  - die Telemetrie bleibt per Umgebungsvariable abgeschaltet
  - die Build-Prüfung bleibt aktiv
- **Die Etappe „Übernehmen“ geht in zwei Schritten:**
  - zuerst ein Leser für Rezeptdateien, ohne Internet und ohne Berechtigung (Rang 6)
  - danach „Aus Link übernehmen“ mit demselben Leser (Rang 7)

  Die drei gleichartigen Vorschläge N1, F3 und K1-A sind zu einem Punkt zusammengelegt.
- **„Aus Link übernehmen“ ist ein Paket:** Netz-Regeln (N2, führend), Sicherheits-Prüfliste (S2), Texte und Datenschutzerklärung (T2, Ü3) und #16.
- **„Kochen“ bekommt keinen eigenen Bildschirm.** Abhaken und Portionen kommen in die Rezeptansicht (K2 mit den Bedienregeln aus D2). Portionen werden erst umgerechnet, wenn Zahlen richtig gelesen werden (Ü2).
- **„Finden und ordnen“ kommt erst nach der Messung mit 1.000 Rezepten** (Rang 4), weil es genau die Listenabfrage verändert.
- **Der Satz „Alles bleibt nur auf diesem Handy“** (Leerzustand, T3) darf erst nach Rang 1 in die App, sonst wäre er falsch.
- **Das Lesemodell wird nur verkleinert,** wenn die Erkennung nicht messbar schlechter wird. Auch Umlaute, Akzente und ß müssen in allen fünf Sprachen erhalten bleiben. Das verkleinerte Modell braucht eine nachvollziehbare Herkunft (Werkzeug, Ausgangsdatei, Prüfsumme) in der CLAUDE.md. Frühestens nach 1.0.
- **Nicht empfohlen:**
  - die 32-Bit-Unterstützung zu streichen, denn günstige Android-8-Handys sind oft reine 32-Bit-Geräte
  - getrennte APKs je Prozessorart
  - ein selbst gebautes ONNX Runtime

## Streitpunkte – Entscheidung nötig

1. **Welche Etappe kommt nach den Nacharbeiten (Rang 1–5)?**
   - *„Übernehmen“ zuerst (Rezeptdatei, dann Link). Dafür sind Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte und Übersetzungen (7 von 8). Ihre Gründe:*
     - Die Vision nennt Internetseiten als ersten Erfassungsweg.
     - Heute endet ein geteilter Link mit einer Notlösung.
     - Der Leser für Rezeptdateien bringt sofort Nutzen, auch ohne Internet.
     - Die Etappe ändert die Listenabfrage nicht, sodass die Messung aus Rang 4 nebenher entstehen kann.
   - *„Kochen“ zuerst. Dafür ist Design. Die Gründe:*
     - Die Rezeptansicht ist der meistgenutzte Bildschirm.
     - „Kochen“ verbessert sofort jedes vorhandene Rezept.
     - Es braucht weder Internet noch eine Datenschutzerklärung.

   Einig sind sich alle, dass „Kochen“ direkt danach kommt.
2. **Wie groß soll Version 0.14.1 sein?**
   - *Nur Rang 1. Dafür sind Sicherheit und Performance, die Mehrheit der Hinweise:* Die Behebung erreicht die Nutzer so schnell wie möglich. Rang 2–5 folgen als eigene Version.
   - *Rang 1 zusammen mit Rang 2 und Rang 5. Dafür ist Kreativität:* ein Paket „Nacharbeiten zu 0.14.0“.
3. **Wo erscheinen ruhige Hinweise, etwa „Zuletzt gesichert“ oder dass seit der letzten Sicherung viele Rezepte dazugekommen sind?**
   - *Nur in den Einstellungen. Dafür ist Design:* Die Sammlung soll ohne Fenster und Leisten auskommen.
   - *Optional auch als ruhiger Hinweis in der Sammlung. Dafür ist Konnektivität:* Wer nie in die Einstellungen schaut, sichert sonst nie.

**Technische Detailfrage (keine Entscheidung des Projektinhabers nötig):** Ob nach Titel in Kotlin (`Collator`, besser testbar, so Übersetzungen und Funktionalität) oder in der Datenbank (Sortierregel bzw. gespeicherter Sortierschlüssel, schneller bei vielen Änderungen, so Performance) sortiert wird, entscheidet die Messung aus Rang 4.

## Offene Fragen an den Projektinhaber

1. Welche Beschlussvorschläge sollen als GitHub-Issues angelegt werden?
2. Soll Rang 1 (Telemetrie entfernen) **sofort** als 0.14.1 umgesetzt werden? Alle acht empfehlen das.
3. Was geschieht mit dem veröffentlichten Release **v0.14.0**? Möglich sind:
   - stehen lassen und in der Beschreibung auf 0.14.1 verweisen
   - als „veraltet“ kennzeichnen
   - die APK entfernen

   Soll außerdem mit einem Mitschnitt auf dem Emulator geprüft werden, ob tatsächlich gesendet wurde? Davon hängt ab, ob der CHANGELOG „senden konnte“ oder „gesendet hat“ schreibt.
4. **Dependabot-Warnungen (S3):** Sollen sie selbst in den GitHub-Einstellungen eingeschaltet werden, oder soll Claude das nach Freigabe über die GitHub-Schnittstelle erledigen?
5. Entscheidungen zu den drei Streitpunkten.
6. **Vorab-Hinweis:** Rang 7 braucht die Internet-Berechtigung. Laut CLAUDE.md ist dafür eine eigene Freigabe nötig. Sie wird erst bei der Umsetzung erfragt.

### Entscheidungen des Projektinhabers

- **Issues:** Alle 11 Beschlussvorschläge werden angelegt (#49–#59).
- **Frage 2:** Rang 1 wird **sofort** umgesetzt, als Version 0.14.1 und nur mit Rang 1 (#49). Damit ist auch Streitpunkt 2 entschieden. Rang 2–5 folgen danach als eigene Versionen.
- **Frage 3:** Das Release v0.14.0 bleibt stehen. Seine Beschreibung bekommt einen deutlichen Hinweis auf die Panne und auf 0.14.1.
- **Streitpunkt 1:** Nach den Nacharbeiten kommt die Etappe **„Übernehmen“**: zuerst Rezeptdateien (#54), dann „Aus Link übernehmen“ (#55). Danach folgt „Kochen“ (#56).
- **Noch offen:**
  - Frage 3, zweiter Teil: ein Mitschnitt, ob tatsächlich gesendet wurde
  - Frage 4: Dependabot-Warnungen einschalten
  - Streitpunkt 3: wo „Zuletzt gesichert“ erscheint (#51)

### Nachtrag vom 05.10.2026

- **Rang 1 (#49) ist umgesetzt** in Version 0.14.1 (Commit `d729ad3`, aus einer parallelen Sitzung). An der veröffentlichten APK 0.15.0 nachgeprüft: keine Internet-Berechtigung, kein `ACCESS_NETWORK_STATE`, kein Telemetrie-Baustein.
- **Das Release v0.14.0** trägt oben in der Beschreibung einen Hinweis auf die Panne und auf die neueste Version.
- **Was im Quelltext von ONNX Runtime nachgelesen wurde:**
  - Ohne den entfernten Startbaustein ist die Telemetrie auf Android nicht lauffähig.
  - `ORT_DISABLE_TELEMETRY` schaltet sie zusätzlich ab.
  - Gesammelt wurden u. a. eine (gehashte) Geräte-Kennung und Angaben zum Handy.
  - Zwischengespeichert wurde im Cache-Ordner der App. Diesen nimmt ein Umzug auf ein neues Handy nie mit.
- **Dependabot-Warnungen einschalten (S3):** weiter offen, die Entscheidung des Projektinhabers steht aus.

### Nachtrag vom 06.10.2026

- **Entscheidungen des Projektinhabers zu den drei offenen Punkten:** jeweils „ja“.
  - Dependabot-Warnungen sind eingeschaltet. Automatische Pull-Requests bleiben aus. GitHub erkennt bisher nur die Bausteine des Bauablaufs, nicht die Gradle-Bausteine der App. Dafür ist eine Meldung der Bausteine aus dem Build nötig; der Job braucht Schreibrecht, darüber wird noch entschieden.
  - Der Mitschnitt wurde gemacht (Ergebnis unten).
  - „Zuletzt gesichert“ steht in den Einstellungen. Zusätzlich erscheint ein ruhiger Hinweis in der Sammlung, wenn seit der letzten Sicherung viele Rezepte dazugekommen sind (#51).
- **Ergebnis des Mitschnitts:** Version 0.14.0 hat nach der Texterkennung tatsächlich Daten an Microsoft gesendet.
  - **Aufbau der Prüfung:** Rundgang von 0.14.0 mit Texterkennung auf einem virtuellen Handy, danach zweimal Start der veröffentlichten APK. Mitgeschnitten wurden Namensanfragen und verschlüsselte Verbindungen.
  - **Beobachtet:** Etwa 10 Sekunden nach der Texterkennung fragte die App nach `mobile.events.data.microsoft.com`. Danach baute sie eine verschlüsselte Verbindung dorthin auf (Microsoft-Sammelserver, Servername im Verbindungsaufbau sichtbar) und übertrug rund 1,6 KB verschlüsselte Daten; der Server antwortete.
  - **Wiederholbar:** In zwei Läufen zeigte sich dasselbe. Der bloße App-Start ohne Texterkennung löste keine Verbindung aus.
  - **Inhalt:** Er ist verschlüsselt und nicht einsehbar. Laut Quelltext von ONNX Runtime gehören dazu Angaben zum Handy (u. a. eine gehashte Geräte-Kennung, Modell) und zur Nutzung der Erkennung. Für Fotos ist die Datenmenge viel zu klein.
  - **Folge:** README, `SECURITY.md` und der Hinweis am Release v0.14.0 sagen jetzt „gesendet“ statt „konnte senden“.

## Zurückgezogen / bereits als Issue vorhanden

- **Zurückgezogen oder abgeschwächt:**
  - S2: als eigener Punkt zurückgezogen, geht als Prüfliste in Rang 7 (N2) auf.
  - S3: abgeschwächt auf „beim Sofortpaket mit erledigen“ (Rang 1).
  - F3: zugunsten von N1/K1 zurückgezogen. Bestehen bleibt der Teil „Übernahme-Ablauf vorher aus `EditViewModel` herauslösen“ (Rang 6).
  - N1 und K1-A: gehen im gemeinsamen Leser (Rang 6) auf.
  - D2: kein eigener Bildschirm mehr. Es bleiben nur die Bedienregeln und der optionale Schalter für große Schrift (Rang 8).
  - D3: kommt erst mit „Finden und ordnen“ (Rang 9).
  - K3: abgeschwächt, „Finden und ordnen“ kommt erst nach der Messung (Rang 4). Die Abnahmeliste für 1.0 bleibt, ergänzt um N3, Datenschutzerklärung, Berechtigungsprüfung und S3.
  - P3: auf „niedrig, nach 1.0“ gesenkt. Nur das Löschen der Tesseract-Reste wandert in Rang 1.
  - Ü3: geht in T2 auf (Rang 7). Es bleiben `res/raw` für die Datenschutzerklärung, die Anzeige der Zeiten im Format des Handys und der englische Absatz in der Release-Beschreibung (Rang 10).
  - Ü1: kann mit „Finden und ordnen“ kommen (Rang 9), bleibt aber nötig.
  - T2: Die Korrektur von README und `SECURITY.md` erledigt sich bis zur Internet-Etappe durch Rang 1.
  - T3: Der Kernsatz in der App kann bis kurz vor 1.0 warten.
- **Bestehende Issues:**
  - #16 (Nach neuer Version suchen): geht in Rang 7 auf, nur auf Knopfdruck und über dieselbe Internet-Stelle.
  - #47 (PDF): bleibt niedrig, für 1.0 nicht nötig.
  - Dependabot-Vorschlag #48: Er sollte erst nach der Berechtigungsprüfung im Build (Rang 1) übernommen werden.
- **Aus früheren Besprechungen wieder aufgenommen:**
  - Testsammlung mit 1.000 Rezepten (29.09., Rang 9): jetzt Rang 4.
  - Kernsatz in `strings.xml` (29.09.): jetzt Rang 10.

## Anhang: Beiträge und Stellungnahmen

**Design**
- **Beiträge:**
  - D1: Foto-Übernahme bei 200 % Schrift.
    - „Bereich auswählen“ ist dann unbedienbar, Knopftexte brechen mitten im Wort um.
    - Abhilfe: Foto mit Mindesthöhe, Bedienteil scrollbar, Knöpfe untereinander.
  - D2: eigene Kochansicht mit Bedienregeln.
  - D3: Knöpfe in der Sammlung beim Blättern einklappen, Filter als eine Chip-Zeile.
- **Stellungnahme:**
  - Top 3: S1, F1+P2, D1+T1.
  - D2 abgeschwächt zugunsten von K2.
  - Hinweise (#16, Sicherung) nur in den Einstellungen.
  - Für Rang 7: Ladezustand gestalten, dritte Schaltfläche muss bei 200 % passen.
  - Nächste Etappe: „Kochen“.

**Funktionalität**
- **Beiträge:**
  - F1: Absturz bei Speichermangel in der Texterkennung, das Modell belegt kurzzeitig 42 MB im Java-Speicher.
  - F2: Der Rundgang prüft nur die Debug-Fassung auf Android 14, nie die veröffentlichte Fassung und nie Android 8.
  - F3: erst der Leser für Rezeptdateien. `EditViewModel` vorher aufteilen. Nebenbefund „Für 1 Zopf“.
- **Stellungnahme:**
  - Top 3: S1 (mit F2), F1+P2, N3.
  - Warnung, den gemeinsamen Startbaustein nicht zu entfernen; vom Moderator geprüft, siehe oben.
  - Bevorzugt K2 vor D2. Die Häkchen müssen das Beenden der App überstehen.
  - Sortieren mit `Collator` in Kotlin.
  - F3 zurückgezogen.
  - Nächste Etappe: „Übernehmen“.

**Sicherheit**
- **Beiträge:**
  - S1: Microsoft-Telemetrie und Berechtigungen in 0.14.0, dazu eine Prüfung im Build.
  - S2: Sicherheitsregeln für „Aus Link übernehmen“ und Datenschutzerklärung.
  - S3: Dependabot-Warnungen einschalten.
- **Stellungnahme:**
  - Top 3: S1, F1, F2.
  - README und SECURITY schon in 0.14.1 berichtigen. „Alles bleibt nur auf diesem Handy“ erst nach S1.
  - Regeln für den Leser von Rezeptdateien: keine Abrufe, keine `file://`-/`content://`-Angaben aus dem Inhalt öffnen, Größe und Tiefe begrenzen.
  - Herkunftsnachweis für ein verkleinertes Modell.
  - S2 und S3 abgeschwächt.
  - Nächste Etappe: „Übernehmen“.

**Kreativität**
- **Beiträge:**
  - K1: „Übernehmen“ in zwei Schritten (Rezeptdatei, dann Link), mit #16 und einem Hinweis im Leerzustand.
  - K2: Kochen direkt in der Rezeptansicht.
  - K3: Abnahmeliste für 1.0 mit schlankem „Finden und ordnen“.
- **Stellungnahme:**
  - Top 3: S1, F1+P2, N1/F3.
  - Schlägt 0.14.1 = S1+F1+P2+T1+D1 vor.
  - D2-Schalter erst als zweiten Schritt.
  - K1-A geht in N1/F3 auf. K3 abgeschwächt (erst nach P1), Abnahmeliste um N3 ergänzt.
  - Nächste Etappe: „Übernehmen“.

**Konnektivität**
- **Beiträge:**
  - N1: schema.org-Rezeptdateien einlesen, dazu die Zeile „Quelle:“ beim Übernehmen aus Text erkennen.
  - N2: „Aus Link übernehmen“ mit festen Regeln für Netz, Grenzen und Meldungen.
  - N3: Sicherung prüfen, „Zuletzt gesichert“ anzeigen, „Speicher voll“ als eigene Meldung.
- **Stellungnahme:**
  - Top 3: S1 mit Build-Prüfung, F1+P2, N3.
  - Der Schutz vor Telemetrie muss dauerhaft sein, auch mit Internet-Berechtigung.
  - #16 und YouTube nur über die eine Internet-Stelle, ohne Programmzugang mit eigenem Schlüssel.
  - N3 gehört in die Abnahmeliste für 1.0.
  - Nächste Etappe: „Übernehmen“.

**Performance**
- **Beiträge:**
  - P1: Leistungsziel mit 1.000 Rezepten messen, auch auf Android 8.
  - P2: Dauer und Speicherbedarf der Erkennung auf schwachen Handys.
  - P3: App verkleinern (Lesemodell 16/8 Bit), Tesseract-Reste früher löschen.
- **Stellungnahme:**
  - Top 3: S1, F1+P2, F2+P1.
  - Der Telemetrie-Baustein kostet zusätzlich Startzeit und Akku.
  - Die Prüfung der Sicherung soll nur das Inhaltsverzeichnis lesen.
  - Sortieren lieber in der Datenbank. D3 mit `derivedStateOf`.
  - P3 auf „niedrig, nach 1.0“ gesenkt.
  - Nächste Etappe: „Übernehmen“.

**Texte**
- **Beiträge:**
  - T1: veraltete Hinweise zu zwei Spalten, einheitlich „auswählen“.
  - T2: Textpaket für die Internet-Etappe (README/SECURITY, Datenschutzerklärung, drei Meldungen).
  - T3: Leerzustand, Kernsatz in der App, „wiederherstellen“ statt „übernehmen“, „Einheit“ statt „Angabe“.
- **Stellungnahme:**
  - Top 3: S1 (mit ehrlichem Text für 0.14.1), F1, D1+T1.
  - Formulierungsvorschlag für den CHANGELOG (siehe Rang 1).
  - „Nächster/Vorheriger Schritt“ statt „Weiter/Zurück“, „abhaken“ in die festen Begriffe.
  - Die Meldung „kein Netz“ verweist auf „Aus Link übernehmen“.
  - Nächste Etappe: „Übernehmen“.

**Übersetzungen**
- **Ausgangslage:** Die Sprachdateien sind vollständig und stimmig, es gibt keine fest eingebauten Texte.
- **Beiträge:**
  - Ü1: Sortieren wie im Wörterbuch; bei Schlagwörtern die feste Kennung als Schlüssel.
  - Ü2: Zahlen nach der Rezeptsprache lesen (aus „1.000 g“ wird sonst „2 g“), Brüche verstehen.
  - Ü3: Datenschutzerklärung zweisprachig über `res/raw`, Zeiten im Format des Handys, englischer Absatz im Release-Text.
- **Stellungnahme:**
  - Top 3: S1, F1, N3.
  - K2 erst nach Ü2. Für P3 auch Umlaute und Akzente prüfen. D3 braucht weiter Beschreibungen für den Screenreader.
  - Ü3 geht in T2 auf.
  - Nächste Etappe: „Übernehmen“.
