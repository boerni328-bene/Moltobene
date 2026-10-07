# Besprechung vom 07.10.2026: Rezepte in einer anderen Sprache anzeigen (Übersetzung)

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.19.1 (versionCode 39)

> **Hinweis zur Durchführung:** Die Besprechung wurde in einer Cloud-Sitzung nachgestellt, auf Wunsch des Projektinhabers. Die acht Spezialisten sind nur auf dem PC eingerichtet. Hier haben allgemeine Helfer ihre Rollen übernommen, nach dem Muster der bisherigen Protokolle: zwei Runden, Beiträge und Aussprache. Bildschirmfotos lagen nicht vor. Angaben aus dem Internet sind in den Beiträgen mit Quelle genannt; unsichere Angaben sind als solche gekennzeichnet.

## Anlass

Der Projektinhaber hat mit Version 0.19.1 ein englisches Rezept übernommen („The Best Short Ribs Ever“ von joshuaweissman.com). Sein Wunsch, wörtlich:

> „Es gibt noch keinen Übersetzungsbutton. Eigentlich wäre es am besten, wenn man im Rezept eine Länderflagge antippt oder auswählt und das Rezept dann in dieser Sprache erscheint, ungeachtet in welcher Sprache es hinzugefügt wurde.“

## Tagesordnung

1. Passt eine Übersetzung zur Vision, und unter welchen Bedingungen? Welche Sätze der Vision müssten geändert werden?
2. Welcher Weg?
   - Weg 1: ein Übersetzungsdienst im Internet
   - Weg 2: Übersetzung im Handy mit eigenen Sprachmodellen
   - Weg 3: „Übersetzen mit …“ einer schon installierten App
   - oder ein anderer Weg
3. Wie sieht es in der App aus? Gefragt war nach:
   - Auswahl der Sprache
   - Original und Übersetzung
   - Kennzeichnung
   - Bearbeiten, Teilen, Sichern, Suche
   - Portionen umrechnen
4. Wann, im Verhältnis zu den offenen Etappen und zu 1.0?

## Lagebericht

- **Vision heute:** „Rezepte bleiben in ihrer Originalsprache (keine automatische Übersetzung).“ Dazu gelten:
  - „keine Weitergabe von Daten an Dritte“
  - „Internet nur auf Wunsch“, also nur zum Übernehmen und für „Nach neuer Version suchen“
  - Texterkennung ohne Google-Dienste
  - Leistung auf einem günstigen Android-8-Handy
- **App:**
  - Größe etwa 45 MB, davon 23 MB für die Modelle der Texterkennung. ONNX Runtime ist schon eingebaut.
  - Die Texterkennung kennt Deutsch, Englisch, Italienisch, Französisch und Spanisch. Die Oberfläche gibt es auf Deutsch und Englisch.
- **Rezepte:**
  - Jedes Rezept hat ein Feld `language`. Es wird beim Übernehmen gefüllt, aber in der Rezeptansicht nirgends genutzt (vom Moderator nachgeprüft).
  - „Teilen als Text“ gibt es schon. Bei Rezepten mit Foto wird das Foto mitgeschickt.
- **Offene Etappen:** #56 „Kochen“, #52 Prüflauf, #53 Foto-Übernahme bei großer Schrift, #57 „Finden und ordnen“ mit Abnahmeliste für 1.0, #58 Texte-Durchgang, #16 „Nach neuer Version suchen“.

## Ergebnis in Kürze

- **Weg 3, in zwei Stufen:**
  - Stufe 1: Ein Eintrag „Übersetzen mit …“ gibt das Rezept bewusst an eine installierte Übersetzer-App weiter. Die Übersetzung erscheint dort.
  - Stufe 2 (später): Kommt die Übersetzung zurück, wird sie in Moltobene ein eigener, gekennzeichneter Entwurf. Das Original bleibt unverändert.
- **Weg 1** lehnen alle acht ab, **Weg 2** frühestens nach 1.0.
- **Sprachnamen statt Flaggen** („English“, „Italiano“): Das empfehlen alle acht.
- **Sofort sinnvoll, auch ohne Übersetzung:** die Sprache des Rezepts anzeigen und für den Screenreader auszeichnen. Heute liest der Screenreader das englische Rezept mit deutscher Aussprache.

## Beschlussvorschläge (gemeinsame Rangfolge)

| Rang | Punkt | Von | In den Top 3 bei | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Stufe 1: „Übersetzen mit …“** – das Rezept bewusst an eine installierte Übersetzer-App weitergeben, mit ehrlichem Hinweis und einem Absatz in der Datenschutzerklärung | S1, K1 (Schritt 1), N1, F2, P2, D1, T3 | alle 8 | hoch | klein | Wunsch des Projektinhabers vom 07.10.2026 |
| 2 | **Sprache des Rezepts anzeigen und für den Screenreader auszeichnen** (gilt auch ohne Übersetzung) | D3, Ü1, T2 | 7 von 8 (Sicherheit stimmt zu) | hoch | klein | passt zu #58 oder #57 |
| 3 | **Stufe 2: Rückweg als eigener, gekennzeichneter Entwurf**; Mengen aus dem Original, Prüfhinweis immer | F1, K1 (Schritt 2), Ü3, Ü2, F3, S2, N2 (zurückgezogen, aufgegangen in F1) | Sicherheit, Konnektivität (Entwurf); Design, Funktionalität (Mengen aus dem Original) | mittel | mittel | baut auf #56 auf |
| 4 | **Leitplanke:** kein Übersetzungsmodell in der App vor 1.0; kein Übersetzungsdienst im Internet | P1, K3, S3, N3, P3 | Sicherheit, Kreativität, Performance | hoch (als Leitplanke) | klein (nur Entscheidung) | #59, #49 |
| 5 | **Einheiten umrechnen** (cups, oz, lb, °F), als Ergänzung, nicht als Ersatz | K2 | Texte, Übersetzungen | mittel | mittel | Vision sieht das schon vor; nach #56 |
| 6 | **Kleiner Fund:** Die Rezeptansicht und die Sammlung zeigen beim Laden nur einen Kreisel ohne Text; der Screenreader bleibt stumm | D (Aussprache) | – | niedrig | klein | #52 oder #58 |

### Rang 1 im Detail: Stufe 1 „Übersetzen mit …“

- **Bedienung:**
  - ein beschrifteter Eintrag „Übersetzen mit …“ im Menü ⋮ der Rezeptansicht, Tippfläche mindestens 48 dp (D1)
  - Er erscheint nur, wenn eine Übersetzer-App installiert ist. Ohne Google-Dienste funktioniert das z. B. mit „Offline Translator“ von F-Droid; ob diese App Text zurückgibt, ist ungeprüft.
  - Die Zielsprache wählt man in der Übersetzer-App. Eine eigene Sprachliste in Moltobene ist dafür nicht nötig (Ü hat seine Liste der Zielsprachen zurückgezogen).
- **Technik:**
  - Android-Schnittstelle zum Weitergeben von Text an andere Apps (`ACTION_PROCESS_TEXT`, auf Android 8 vorhanden)
  - Rückfall ist das vorhandene Teilen (`ACTION_SEND`).
  - Moltobene baut selbst keine Verbindung auf und braucht keine neue Berechtigung und keinen neuen Baustein.
  - Damit die App installierte Übersetzer findet, braucht das Manifest ab Android 11 einen `<queries>`-Eintrag, nur für diese Schnittstelle. Das ist keine Berechtigung, die Prüfung im Build bleibt unberührt, aber es ist eine Änderung am Manifest: mit Kommentar und Rückfrage (N1, F2, S1).
  - Nicht `QUERY_ALL_PACKAGES`, keine feste Bindung an Google (S1).
- **Was weitergegeben wird:** nur Titel, Zutaten mit Zwischenüberschriften und Schritte. Nie Notizen, nie Fotos (S1).
- **Ehrlicher Hinweis beim ersten Mal** (T3, S1): „Der Rezepttext geht an die gewählte App. Ob er dort ins Internet gesendet wird, entscheidet diese App.“
  - Hintergrund: Google Übersetzer sendet im Online-Betrieb, mit heruntergeladenen Sprachpaketen übersetzt er auf dem Handy; ob dann gar nichts übertragen wird, ist unsicher. DeepL Free nutzt Texte laut seiner Datenschutzerklärung zum Training.
- **Datenschutzerklärung in der App und README** (T3, S1): Der Satz „Sonst verlassen Inhalte das Handy nur, wenn sie selbst geteilt oder gesichert werden“ wird um „… oder zum Übersetzen an eine andere App gegeben“ ergänzt. Das kommt im selben Release wie die Funktion (Bedingung von Texte und Sicherheit).
- **Ehrlich zum Wunsch des Projektinhabers** (D): In Stufe 1 erscheint die Übersetzung in der anderen App, ohne Abhaken und ohne Portionen umrechnen. Das muss in der Beschreibung des Releases so stehen.
- **Feste Begriffe** (T1), erst zusammen mit der Funktion eintragen:
  - übersetzen / translate (nicht: übertragen)
  - Übersetzung / Translation
  - Originalsprache / Original language (nicht: Ursprache)
  - Nicht verwenden: „Originaltext“ und „Kopie“. Sonst gibt es Verwechslungen mit „Originalseite“ und „Übernommener Text“.

### Rang 2 im Detail: Sprache des Rezepts

- **Anzeige:** die Sprache klein beim Titel oder bei der Quelle, z. B. „English“. Ohne Angabe gibt es einen eigenen Zustand „Sprache unbekannt“, denn bei selbst eingetippten Rezepten fehlt sie oft (F).
- **Ändern:** Geändert wird die Sprache beim Bearbeiten, nicht in der Rezeptansicht, damit die Ansicht beim Kochen ruhig bleibt (D gegen Ü1). Die Auswahl nutzt die vorhandene Liste in `ui/edit/LanguageMenu.kt` mit dem eigenen Namen jeder Sprache.
- **Screenreader:** Die Rezepttexte bekommen die Sprache des Rezepts als Auszeichnung (`localeList`), damit der Screenreader die passende Aussprache wählt. Ob TalkBack die Stimme wirklich wechselt, hängt von den installierten Stimmen ab; auf dem Handy prüfen (D3).
- **Warum Voraussetzung:** Jede spätere Übersetzung muss von der richtigen Sprache ausgehen. Auch `inLanguage` beim Teilen hängt daran (N).

### Rang 3 im Detail: Stufe 2 – Rückweg als eigener Entwurf

- **Vorher prüfen:** Auf einem echten Handy wird getestet, ob Google Übersetzer, DeepL und Offline Translator die Übersetzung zurückgeben. Belegt ist das nur für Google Übersetzer im Jahr 2015 („ersetzen“ in bearbeitbaren Feldern), für heute ist es unsicher (N1, F2, Ü3).
- **Zweiter Rückweg ohne neuen Code:** Die Übersetzer-App teilt ihr Ergebnis, und Moltobene nimmt geteilten Text schon heute an („Aus Text übernehmen“). Dann fehlt allerdings der Verweis auf das Original (N).
- **Die Übersetzung wird ein neuer Entwurf**, keine zweite Fassung im selben Rezept (F1). Sie bekommt:
  - die Zielsprache als Sprache
  - dieselbe Quelle wie das Original
  - ein neues Feld `translatedFrom` mit der Kennung des Originals
- **Warum ein eigener Entwurf:**
  - Bearbeiten, Suche, Portionen umrechnen (#56) und Sichern funktionieren ohne Sonderfall.
  - Fehler wie „Nelken“ statt „Zehen“ lassen sich korrigieren.
  - Die Übersetzung geht nicht verloren, wenn Android die App im Hintergrund beendet.
- **Mengen und Einheiten** (Ü2 und F3 zusammengelegt):
  - Stimmt die Zeilenzahl und erkennt `AmountText` die Menge sicher, wird die Menge aus dem Original eingesetzt. Sonst bleibt die Zeile, wie sie ist. Nie still überschreiben.
  - Ergebnis ist ehrlich „2 cups Mehl“ statt falsch „2 Tassen Mehl“.
  - Vorschlag für den Hinweis (T): „Maschinell übersetzt. Mengen und Einheiten stehen wie im Original; mit dem Original vergleichen.“
  - Der Hinweis steht immer da, denn auch Zutatennamen, Temperaturen und Garzeiten können falsch übersetzt sein (S).
- **Zurückkommender Text ist fremder Inhalt** (S2): Er wird wie geteilter Text behandelt, also nur Klartext, Fehler abgefangen, höchstens 50.000 Zeichen. Die Zeilen ordnet reines Kotlin mit Unit-Tests zu. Im Rundgang ersetzt eine nachgestellte Übersetzer-App die echte (F2).
- **Kennzeichnung** (D, T):
  - „Übersetzt aus: Englisch“ (mit Doppelpunkt, damit der Satz nicht gebeugt werden muss) und ein Verweis zum Original
  - Text und Symbol, nicht nur Farbe; in der Sammlung erkennbar am Sprachnamen
- **Daten** (F1, N, nachgeprüft):
  - Die Datenbank geht von Version 2 auf 3, mit Migration und Test.
  - In der Sicherungsdatei kommt ein neues Feld mit Standardwert dazu; die Formatversion bleibt 1. Eine höhere Formatversion würde dazu führen, dass ältere App-Versionen die Sicherung ablehnen (`BackupReader`, `NEWER_VERSION`).
  - Beim Teilen wird mit `translationOfWork` auf das Original verwiesen.
- **Fotos** (Fund von Funktionalität, nachgeprüft):
  - Ein Foto gehört über seine Kennung genau zu einem Rezept, und beim Löschen eines Rezepts werden seine Fotodateien mitgelöscht.
  - Die Übersetzung darf Fotos deshalb nicht mit dem Original teilen. Sie bekommt Kopien mit neuen Kennungen, Originalseiten nie.
- **Zeitpunkt:** nach 1.0, damit Datenbank und Sicherungsdatei für 1.0 unverändert bleiben (F, K, P, T, Ü, D). Konnektivität: früher nur, wenn der Test auf dem Handy die Rückgabe belegt.

### Rang 4 im Detail: Leitplanke

- **Weg 1 (Dienst im Internet):** einstimmig abgelehnt.
  - Weitergabe an Dritte, Auftragsverarbeitung, Kosten
  - Ein Schlüssel ließe sich in einem öffentlichen Repository nicht schützen (S).
  - Mehrere zentrale Aussagen in README und App würden falsch (T).
- **Weg 2 (Modelle im Handy):** frühestens nach 1.0, als eigene Etappe mit Rückfrage zu den Bausteinen.
  - **Größe** (Angaben schwanken, teils unsicher): Für 5 Sprachen über Englisch wären es etwa 160–280 MB zusätzlich zu 45 MB. Mozilla nennt etwa 15–40 MB je Sprachpaar.
  - **Neue native Bibliothek:** Die Firefox-Modelle (Bergamot) laufen nicht mit ONNX Runtime. Ob sie auf 32-Bit-Handys laufen, ist offen.
  - **Geschwindigkeit:** geschätzt 10 Sekunden bis Minuten je Rezept auf schwachen Handys, nicht gemessen.
  - **Ausgeschlossen:**
    - Google ML Kit: lädt Modelle von Google und sendet laut seinen Bedingungen Nutzungsdaten, also eine Wiederholung von #49.
    - NLLB-200: Die Lizenz verbietet kommerzielle Nutzung.
    - Die Übersetzung von Android selbst (`TranslationManager`): erst ab Android 12 und meist über einen Dienst von Google.
  - **Prüfbar wären:** OPUS-MT (CC-BY 4.0) und Firefox-Modelle (MPL 2.0; ob das auch für die Modelldateien gilt, ist unsicher).
  - **Bedingungen:**
    - Probeaufbau, gemessen auf Android 8 mit 2 GB Arbeitsspeicher und 32 Bit, Grenzen vorher festlegen (P3)
    - Laden nur auf Wunsch über `data/web/`, mit fester Prüfsumme wie bei der Texterkennung (#41)
    - Build-Prüfung auf Telemetrie wie bei #49
    - Das Laden von Sprachpaketen wäre ein neuer Zweck fürs Internet und bräuchte eine eigene Änderung der Vision.

### Rang 5 im Detail: Einheiten umrechnen

- Bei Rezepten aus den USA stören beim Kochen vor allem die Einheiten (cups, oz, lb, °F). Die Vision sieht die Umrechnung schon vor („eine Umrechnung kann später folgen“), sie muss dafür nicht geändert werden.
- **Nur in der Anzeige**, nach dem Muster von #56, z. B. „240 ml (1 cup)“. Dass umgerechnet wurde, steht im Text, nicht nur in einer Farbe (D).
- **Cups werden in ml umgerechnet, nicht in Gramm**, denn Gramm hängen von der Zutat ab (K).
- **Fund von Übersetzungen** (nachgeprüft): `RecipeJsonLdReader.language()` schneidet die Region ab, aus „en-US“ wird „en“. US-Cup, britische Pint und metrischer Cup lassen sich dann nicht unterscheiden. Deshalb die Region behalten oder nur Eindeutiges umrechnen (oz, lb, °F), Cups mit „≈“.
- **Ergänzung, kein Ersatz**: Bei italienischen Rezepten fehlen die Wörter, nicht die Einheiten, und der Wunsch gilt der Sprache (alle acht).

## Einigkeit

- **Weg 3 ist der richtige Weg.** Alle acht haben ihn gewählt:
  - Moltobene sendet selbst nichts.
  - keine Berechtigung, kein neuer Baustein
  - Die App wird nicht größer.
  - Es geht auch ohne Google-Dienste.
- **Sprachnamen statt Flaggen.** Flaggen stehen für Länder, nicht für Sprachen: Englisch gehört zu GB, US, AU, Spanisch zu ES, MX. Der Screenreader läse „Flagge Vereinigtes Königreich“ vor. Eine Reihe von Flaggen passt bei 200 % Schrift nicht, und Information steckt dann nur im Bild.
- **Das Original bleibt immer unverändert und maßgeblich.** Übersetzt wird nur auf Wunsch und immer gekennzeichnet.
- **Keine zweite Fassung im selben Rezept** (N2 zurückgezogen). Sie veraltet bei jeder Bearbeitung und macht Suche, Teilen und Sicherung mehrdeutig.
- **Die Sprache des Rezepts anzeigen und auszeichnen** kommt vorher und unabhängig von der Übersetzung.
- **Mengen aus dem Original und ein Prüfhinweis**, nicht entweder–oder.
- **Einheiten umrechnen** ist eine Ergänzung, kein Ersatz.
- **Reihenfolge:**
  - Stufe 1 kommt nach #56, #52 und #53 und darf diese nicht verdrängen.
  - Stufe 2 kommt nach 1.0.
  - Der Texte-Durchgang #58 wird nicht mit dem neuen Thema belastet; nur die Sprachanzeige (Rang 2) darf mit ihm kommen.
  - Die Übersetzung ist keine Bedingung für 1.0.

## Streitpunkte – Entscheidung nötig

1. **Wie lautet der neue Satz in der Vision?**
   - *Ohne den Weg* – dafür sind Design, Kreativität, Sicherheit, Texte und Übersetzungen (5 von 8): „Rezepte werden in ihrer Originalsprache gespeichert; übersetzt wird nur auf Wunsch und gekennzeichnet, das Original bleibt erhalten.“ Ihre Gründe:
     - Der Satz legt den Weg nicht fest.
     - Den Datenschutz sichern schon „Internet nur auf Wunsch“ und „keine Weitergabe an Dritte“.
   - *Mit dem Weg* – dafür sind Funktionalität, Konnektivität und Performance (3 von 8): „… übersetzt wird nur auf Wunsch mit einer installierten Übersetzer-App, gekennzeichnet, das Original bleibt erhalten.“ Ihr Grund: Ein eingebautes Modell bräuchte dann ausdrücklich eine eigene Entscheidung des Projektinhabers.
   - **In beiden Fällen ändern sich auch:**
     - der Satz „Rezeptinhalte bleiben in ihrer Originalsprache“ unter „Übersetzbarkeit“
     - die Textregel „Eigene Eingaben der Nutzer werden nie *automatisch* übersetzt“ (das Wort „automatisch“ kommt dazu)
     - die Datenschutzerklärung, mit dem Satz aus Rang 1
2. **Stufe 1 noch vor 1.0?** Alle sind dafür, unter Bedingungen:
   - #56, #52 und #53 sind fertig.
   - Hinweis, Datenschutzerklärung und README kommen im selben Release.
   - Design will Stufe 1 nur dann vor 1.0, wenn diese Etappen dann wirklich fertig sind.
   - Die Entscheidung liegt beim Projektinhaber, weil sie den Weg zu 1.0 verlängert.

**Technische Detailfrage (keine Entscheidung des Projektinhabers nötig):** Welche Fotos die übersetzte Fassung bekommt, wird bei der Umsetzung von Stufe 2 entschieden:
- Funktionalität: alle Fotos als Kopie mit neuer Kennung
- Performance: nur das Titelfoto, damit Speicher und Sicherungsdatei nicht mit jeder Übersetzung wachsen

## Offene Fragen an den Projektinhaber

1. **Vision:** Soll der Satz zur Sprache geändert werden? Wenn ja, in welcher Fassung (Streitpunkt 1)?
2. **Flaggen:** Einverstanden mit Sprachnamen statt Flaggen? Bei Weg 3 wählt man die Sprache ohnehin in der Übersetzer-App.
3. **Erwartung:** Reicht es zunächst, dass die Übersetzung in der Übersetzer-App erscheint (Stufe 1)? Die Übersetzung direkt im Rezept, mit Abhaken und Portionen, käme erst mit Stufe 2 nach 1.0.
4. **Auf dem eigenen Handy ausprobieren:**
   - Welche Übersetzer-App ist installiert (z. B. Google Übersetzer, DeepL)?
   - Erscheint sie beim Teilen eines Rezepts als Text in der Liste? Bei Rezepten mit Foto wird das Foto mitgeschickt; das kann die Auswahl verändern.
   - Das zeigt schon vor der Umsetzung, ob Stufe 1 und 2 auf dem Handy funktionieren.
5. **Issues:** Welche Beschlussvorschläge sollen als GitHub-Issues angelegt werden?
   - Rang 1, 2, 3 und 5 wären eigene Issues.
   - Rang 4 käme als Leitplanke in die CLAUDE.md.
   - Rang 6 käme in #52 oder #58.
6. **Manifest:** Für Stufe 1 braucht es den `<queries>`-Eintrag. Das ist keine Berechtigung, aber eine Änderung am Manifest. Die Rückfrage dazu kommt bei der Umsetzung.

### Entscheidungen des Projektinhabers (07.10.2026)

- **Originalsprache:** Rezepte werden in ihrer Originalsprache gespeichert.
- **Weg:** Die Übersetzung soll mindestens für Deutsch ↔ Englisch **offline und ohne Zusatz-App** verfügbar sein, also Weg 2 statt Weg 3. Damit weicht der Projektinhaber bewusst von der Empfehlung ab.
  - Stufe 1, das Weitergeben an eine Übersetzer-App, entfällt.
  - Die Übersetzung erscheint direkt im Rezept.
  - Die Leitplanke aus Rang 4 gilt für Deutsch ↔ Englisch nicht mehr in dieser Form. Ihre Bedingungen bleiben als Prüfliste:
    - Messung auf einem schwachen Handy
    - Lizenz und Herkunft der Modelle, feste Prüfsummen
    - Prüfung auf Telemetrie
    - kein Google ML Kit
- **Auswahl der Sprache:** keine Flaggen, sondern die Abkürzungen **„DE“ und „EN“** (später z. B. IT, FR, ES). Der Screenreader liest den vollen Namen der Sprache vor.
- **Modelle:** Sie werden **einmal auf Wunsch heruntergeladen**, als „Sprachpaket Deutsch–Englisch“ in den Einstellungen, mit fester Prüfsumme. Danach läuft alles offline, und die App selbst bleibt bei etwa 45 MB. Die Vision wird um diesen Download ergänzt; der Wortlaut folgt mit der Umsetzung.
- **Zeitpunkt:** Jetzt kommt zuerst eine **Machbarkeitsprobe** auf einem Arbeitszweig, ohne neue Version. Gemessen werden Größe, Geschwindigkeit und Qualität. Die Umsetzung folgt **direkt nach #56 „Kochen“**.

### Nachtrag vom 07.10.2026: Ergebnis der Machbarkeitsprobe

- **Aufbau der Probe:**
  - drei Läufe in GitHub Actions auf dem Arbeitszweig `claude/remote-control-sejwxu`, Skript `.github/probe/uebersetzung_probe.py` im Commit `d17e7f5`, danach entfernt
  - Modelle OPUS-MT `opus-mt-en-de` und `opus-mt-de-en` der Universität Helsinki (laut Modellkarte CC-BY 4.0), umgewandelt nach ONNX und auf 8 Bit verkleinert
  - Gerechnet wurde mit ONNX Runtime, demselben Baustein wie bei der Texterkennung.
  - Getestet wurden 29 englische und 20 deutsche, selbst geschriebene Rezeptzeilen sowie je ein langer Schritt.
- **Größe je Richtung:** 138 MB auf dem Handy, zum Herunterladen gepackt 83–90 MB. Für Deutsch ↔ Englisch sind das zusammen etwa 276 MB auf dem Handy und etwa 175 MB Download.
  - Ungeprüfte Schätzung: Etwa 60 MB je Richtung entfallen auf Gewichte, die doppelt gespeichert sind. Mit mehr Aufwand ließe sich das auf etwa 80 MB je Richtung senken.
  - Die nicht verkleinerte Fassung (539 MB je Richtung) kommt nicht infrage.
- **Speicher:** höchstens etwa 300 MB beim Übersetzen; die nicht verkleinerte Fassung braucht etwa 860 MB. Geladen wird immer nur eine Richtung.
- **Geschwindigkeit:** auf einem Kern des Rechners von GitHub
  - 29 kurze Zeilen in 1,4 s, ein langer Schritt (etwa 55 Wörter) in 1,0 s
  - Ein günstiges Handy ist geschätzt 5- bis 10-mal langsamer: ein kurzes Rezept etwa 10–15 s, ein langes wie die Short Ribs etwa 1,5–3,5 Minuten. Das ist nicht gemessen.
  - Ein Zwischenspeicher beim Erzeugen der Wörter kann das noch etwa halbieren.
  - Folge: Übersetzen mit Fortschrittsanzeige, danach wird das Ergebnis gespeichert.
- **Qualität (verkleinerte Fassung):**
  - Schritte und einfache Zutaten sind meist gut verständlich, z. B. „Mit gehackter Petersilie garnieren.“, „4 Knoblauchzehen, gehackt“, „2 tbsp vinegar“.
  - Englisch → Deutsch: Etwa jede dritte Zeile hat einen falschen Küchenbegriff, z. B. „icing sugar“ → „Zuckervereisung“, „self-raising flour“ → „selbsterziehendes Mehl“, „Fold in“ → „einklappen“, „deglaze“ → „deglieren“.
  - Deutsch → Englisch: Etwa jede vierte bis fünfte Zeile ist falsch, z. B. „Tellerlinsen“ → „glass lenses“, „Ober-/Unterhitze“ → „above/underheat“, „Springform“ → „spring shape“.
  - Kurze Titel und Überschriften gehen oft daneben. Einmal kam reiner Unsinn heraus: „Gremolata:“ → „- Ich weiß.“
  - Die verkleinerte Fassung weicht in etwa einem Drittel der englischen Zeilen vom Originalmodell ab, meist leicht schlechter.
- **Zahlen:** Die verkleinerte Fassung hat in keiner der 49 Zeilen eine Zahl verändert. Die nicht verkleinerte Fassung hat einmal eine Menge weggelassen („2 tbsp milk“ → „Milch“).
- **Mengen getrennt übersetzen (Ü2) verworfen:** Wird die Menge abgetrennt und nur der Rest übersetzt, wird es schlechter, z. B. „200 g Selbstaufzuchtmehl“. Außerdem bleibt dann „2 tbsp“ im deutschen Text stehen. Besser ist es, die ganze Zeile zu übersetzen und danach zu prüfen, ob dieselben Zahlen dastehen. Fehlt eine Zahl oder ist sie anders, bleibt die Originalzeile stehen.
- **Küchenwörterbuch:** Typische Begriffe vorher durch das Zielwort ersetzen, hilft bei Nomen deutlich:
  - „Puderzucker“, „Mehl mit Backpulver“, „Abrieb von 2 Zitronen“, „brown lentils“, „springform pan“, „conventional heat“
  - Bei Verben und Wendungen kann es schaden: „fork-tender“ → „weich“ ergab „Bis zu zwei Stunden.“ statt „etwa 3 Stunden“. Die Prüfung der Zahlen fängt das ab.
  - Das Wörterbuch braucht also eine sorgfältig geprüfte Liste, vor allem mit Nomen.
- **Zerlegung in Wortteile ohne neuen Baustein:** Ein eigener Nachbau stimmte in 49 von 49 Zeilen mit der echten Zerlegung (SentencePiece) überein. Er lässt sich in reinem Kotlin schreiben.
- **Verworfen:**
  - Verkleinerung „pro Kanal“: viel Unsinn, endlose Wiederholungen
  - gründliche Suche mit 4 Varianten: kaum besser, etwa viermal so langsam
- **Bewertung:**
  - Machbar mit dem vorhandenen Baustein, ohne neue Berechtigung, mit einem Download von etwa 175 MB.
  - Die Übersetzung hilft beim Verstehen, ersetzt aber nicht das Original. Der Hinweis „Maschinell übersetzt …“ und ein Tipp zurück zum Original sind Pflicht.
  - Schutzregeln für die Umsetzung:
    - Zahlen prüfen
    - unsinnige oder viel zu lange Ausgaben erkennen und dann die Originalzeile behalten
    - Überschriften und kurze Titel vorsichtig behandeln
    - geprüftes Küchenwörterbuch mit Nomen
  - Offen bleiben die Messung auf einem echten günstigen Handy (#52) und wo das Sprachpaket gehostet wird (z. B. als Datei an einem eigenen GitHub-Release, mit fester Prüfsumme).

## Zurückgezogen / zusammengelegt

- **Zusammengelegt:**
  - K1 Schritt 1 und N1 gehen in S1 auf, zusammen mit F2, P2, D1 und T3 (Rang 1).
  - K1 Schritt 2, Ü3 und N2 gehen in F1 auf (Rang 3).
  - Ü2 und F3 sind zusammengelegt (Rang 3).
  - K3, S3, N3 und P1 bilden die gemeinsame Leitplanke (Rang 4).
- **Abgeschwächt:**
  - P3 auf niedrig, denn es zählt nur, falls Weg 2 je kommt.
  - K2 auf mittel, als Ergänzung.
  - F1 auf Stufe 2 nach 1.0.
  - Texte („erst nach 1.0“) lenkt für Stufe 1 ein.
- **Zurückgezogen:**
  - D2 „nur anzeigen“; Kennzeichnung und Zustände gelten für den Entwurf.
  - Ü1 die Liste der Zielsprachen
  - N2 die zweite Fassung im selben Rezept
  - Sicherheit „neue Felder nur mit neuer Formatversion“: Das war falsch, siehe Rang 3.
  - Sicherheit „zusammen mit #58“, folgt Texte.
  - Performance „in wenigen Tagen fertig“ gilt nur für den Menüeintrag; mit Rückweg ist der Aufwand mittel.

## Anhang: Beiträge und Stellungnahmen

**Design**
- **Beiträge:**
  - D1 Sprachnamen statt Flaggen, ein beschrifteter Eintrag „Übersetzen“ im Menü ⋮
  - D2 Übersetzung immer erkennbar, Original einen Tipp entfernt, gestaltete Zustände („Wird übersetzt …“, Fehler, Eintrag ausblenden)
  - D3 Sprache des Rezepts anzeigen und für den Screenreader auszeichnen
- **Stellungnahme:**
  - Top 3: D3+Ü1, K1 Schritt 1 (mit S1, P2), Ü2
  - für einen eigenen Entwurf, „nur anzeigen“ zurückgezogen
  - Sprache nur beim Bearbeiten wählen; „Original:“ nur bei übersetzten Fassungen
  - Einheiten wie „240 ml (1 cup)“
  - Fund: stummer Ladekreisel

**Funktionalität**
- **Beiträge:**
  - F1 Übersetzung als eigener Entwurf mit `translatedFrom`; Datenbank 2 → 3, Sicherung mit Standardwert; Fotos nicht teilen (Fund im Code)
  - F2 Ablauf mit `ACTION_PROCESS_TEXT`, Fehlerfälle, `<queries>`
  - F3 erst #56, dann Übersetzung; immer Entwurf mit Prüfhinweis
- **Stellungnahme:**
  - Top 3: K1 Schritt 1, D3/Ü1, Ü2
  - gegen „nur anzeigen“ und gegen die zweite Fassung
  - Fund: Eine höhere Formatversion würde ältere Apps aussperren (`NEWER_VERSION`).
  - Zustand „Sprache unbekannt“ mitplanen
  - F1 auf Stufe 2 nach 1.0

**Sicherheit**
- **Beiträge:**
  - S1 Übersetzen nur als bewusstes Weitergeben, Hinweis und Datenschutzerklärung, nur Titel/Zutaten/Schritte
  - S2 zurückkommende Übersetzung ist fremder Inhalt
  - S3 Bedingungen für Weg 2 (ML Kit und NLLB ausgeschlossen, OPUS-MT oder Firefox-Modelle prüfbar, Prüfsumme, Telemetrie-Prüfung)
- **Stellungnahme:**
  - Top 3: S1+T3, F1/Ü3, P1/K3
  - für einen Entwurf, gegen eine zweite Fassung
  - Rückgabe wie geteilten Text behandeln
  - Aussage zur Formatversion zurückgenommen

**Kreativität**
- **Beiträge:**
  - K1 „Übersetzen mit …“ in zwei Schritten (Weitergeben, dann Rückweg als Entwurf)
  - K2 Einheiten umrechnen (cups, oz, lb, °F)
  - K3 kein Modell vor 1.0
- **Stellungnahme:**
  - Top 3: D3, S1, P1
  - Stufe 1 vor 1.0 direkt nach #56, Rückweg nach 1.0
  - K2 nur als Ergänzung (in ml und °C)
  - K1 und K3 gehen in S1, F1 und P1 auf

**Konnektivität**
- **Beiträge:**
  - N1 `ACTION_PROCESS_TEXT` mit Rückgabe, Rückfall Teilen, `<queries>`
  - N2 Übersetzung getrennt speichern (`translationOfWork`)
  - N3 Sprachmodelle nur zum Herunterladen auf Wunsch
- **Stellungnahme:**
  - Top 3: S1, F1, D3
  - für einen eigenen Entwurf, denn „nur anzeigen“ schickt den Text bei jedem Ansehen erneut weg und geht verloren, wenn Android die App beendet
  - N2 zurückgezogen
  - zweiter Rückweg über das vorhandene Teilen

**Performance**
- **Beiträge:**
  - P1 kein Übersetzungsmodell in der App (Größe, Speicher, Geschwindigkeit)
  - P2 Weg 3 kostet die App keine Leistung
  - P3 erst messen, dann über ein Modell entscheiden
- **Stellungnahme:**
  - Top 3: K1 Schritt 1/N1, K3, D3
  - erste Stufe lieber nur anzeigen; beim Speichern höchstens das Titelfoto kopieren
  - nachgeprüft: Die Liste der Sammlung lädt nur Kennung, Titel, Entwurf und Foto
  - Vision-Satz mit „installierter Übersetzer-App“

**Texte**
- **Beiträge:**
  - T1 feste Begriffe (übersetzen, Übersetzung, Originalsprache)
  - T2 Sprachnamen statt Flaggen, „Übersetzt aus: %1$s“
  - T3 ehrliche Hinweise, Datenschutzerklärung, README
- **Stellungnahme:**
  - Top 3: D3+Ü1, S1+T3, K2
  - für einen Entwurf, gegen „Kopie“ und „bitte prüfen“ (bisher „Prüfen, ob …“)
  - lenkt für Stufe 1 vor 1.0 ein, wenn die Texte im selben Release kommen

**Übersetzungen**
- **Beiträge:**
  - Ü1 Sprachnamen statt Flaggen, Originalsprache sichtbar
  - Ü2 Mengen, Einheiten und Gruppen nie durch den Übersetzer (typische Fehler: „cup“ → „Tasse“, „stick butter“, „kosher salt“, Tbsp/tsp)
  - Ü3 „Übersetzen mit …“, Rückweg als Entwurf
- **Stellungnahme:**
  - Top 3: D3, N1/F2, K2
  - Stufe 1 nur in der Übersetzer-App, Stufe 2 als Entwurf
  - Fund: Die Region der Sprache wird abgeschnitten (wichtig für US-Cups).
  - Liste der Zielsprachen zurückgezogen
