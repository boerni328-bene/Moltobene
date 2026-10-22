# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 22.10.2026 · `main` = Version 0.55.0 (versionCode 96), veröffentlicht · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, nur auf dem Handy. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## In Arbeit (22.10.2026): Neues Aussehen, Zweig `neues-design`, Version 0.56.0 (versionCode 97)
- Auf Zweig `zuhoeren-modus` aufgebaut (Audio-Vorarbeit liegt dort, ungebaut/unfertig); beide Zweige gehören zusammen auf `main`. Nichts auf `main`, kein Release.
- Umgesetzt nach Auswahl des Inhabers (Entwurfsseiten im Claude-Artefakt „Moltobene Auswahl“): Startseite D (Foto-Bühne, vier kompakte Kacheln), Rezept A (flaches Foto, Reiter Zutaten/Zubereitung/Notizen), Kochbuch B (Liste mit Zeit/Portionen, Reiter Alle/Favoriten/Entwürfe, Startsymbol links oben), Teigrechner A (Ergebnis oben fest, Minus/Plus-Felder, Vorteig-Karte hervorgehoben, Erklärfenster für Biga/Poolish/Sauerteig/Hefearten), Einstellungen A + „Schnell“-Schalter aus D (Liste, Gruppe öffnet als eigene Ansicht), Hinzufügen D (eigene Seite).
- **„Sammlung“ heißt jetzt „Kochbuch“** (Cookbook), Regeln in `CLAUDE.md` angepasst. Neue Schaltfläche „Aufnehmen“ (Rezept einsprechen) ist beschlossen, aber noch nicht programmiert (gehört zu „Aus Sprache übernehmen“, Zweig `zuhoeren-modus`).
- Build 1 (Run 39766881011) rot: Hilfetexte (Fachregeln: keine Minuten, kein „Sie“, keine Frischhaltung) und 9 Rundgang-Tests; behoben. Build 2 (Run 39770353454) grün. Build 3 (Run 39772873993) nach kleinen Korrekturen (Symbol über Titel in Kacheln, Reiter ohne Wortumbruch) läuft.
- Offen: Bildschirmfotos aller Darstellungen sichten (Ordner `bildschirmfotos/0.56.0`), Erklärtexte (Teigwissen: Biga, Poolish, Sauerteig, Hefen) vom Inhaber/Fachperson gegenlesen, Pasta/Kuchen im Teigrechner haben noch kein festes Ergebnis oben, Teigrechner passt noch nicht ganz auf einen Bildschirm (Voreinstellungskarte).
- Nächster Schritt: Build 3 prüfen, Fotos ansehen, dem Inhaber zeigen, dann Squash-Merge zusammen mit `zuhoeren-modus` (nur nach Freigabe).

## Git-Stand
- `main` = Version 0.54.0 (Squash-Merge von `teig-zeitplan`, Build grün, Release folgt aus dem Build auf main). Zweig `teig-zeitplan` ist erledigt.
- Parallele Sitzungen im selben Ordner kommen vor (am 17.10. erschien während der Sitzung ein neuer Commit): vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen.

## Arbeitsweise (bewährt)
- Ein Zweig je Aufgabe, bauen mit `gh workflow run build.yml --ref <zweig>`, auf grün warten, **Bildschirmfotos ansehen** (`gh run download <id> -n bildschirmfotos-<version> -D bildschirmfotos/<name>`), dann `git merge --squash` auf `main` mit `Fixes #<Nr>`.
- Fehlersuche: `gh run view <id> --log-failed` bzw. `--log | grep "##\[error\]com"`; Zeilen mit `texterkennung[...]` und `umrechnenNachBackform[PSEUDO]` sind bewusst übersprungen.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug schreiben, nicht per Heredoc; `gh` unter `C:\Program Files\GitHub CLI\gh.exe`; kein Python auf dem PC; `git push` scheitert gelegentlich kurz, wiederholen.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern (`SideEffect` nehmen); bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)` statt `performScrollTo`; Pseudosprache setzt Werte in »…«.
- Token sparen: Aufbau-Datei nur bei Bedarf lesen, erst suchen dann gezielt lesen, Spezialisten/Prüfrunden nur gezielt, eine Sitzung pro Thema.

## Erledigt seit Version 0.48 (Kurzform)
Spracheingabe (Beta-Kennzeichnung), Sortieren der Sammlung, Texte-Durchgang, große Kochschrift und Notiz in der Rezeptansicht, Schlagwörter beim Übernehmen, eigene Teigrechner-Voreinstellungen in der Sicherung (0.52.0, mit Schutz der Fotos beim Wiederherstellen 0.52.1), Einstellungen als Gruppen (0.53.0). Prüflauf #52 (Android 8, 1.000 Rezepte): Kaltstart ≈1,0 s, Liste 65 ms, Suche 51 ms.

## Weg zu Version 1.0 (Abnahmeliste, Kopie des Wesentlichen)
Offen:
1. **Prüfung auf echtem Handy** durch den Projektinhaber: Blättern, Vorlesen mit TalkBack, Spracheingabe mit Mikrofon, Kopfhörertasten, Texterkennung/Übersetzung messen (#52, #62).
2. Teigwissen-Artikel von einer Fachperson gegenlesen (oder „ohne Gewähr“ belassen).
3. Store-Auftritt (#73): eigene Gerichte-Fotos des Projektinhabers (ohne Aufnahmeort, keine erkennbaren Personen), Texte, Spendenlink nur im Store-Eintrag; Markenname „Molto Bene“ vorher im Markenregister prüfen.
4. **Prüfrunde** (`/pruefrunde`) vor 1.0, automatisch laut `CLAUDE.md`; Issues erst nach Freigabe.
Nicht nötig für 1.0: PDF (#47), Umzug aus Apps (#76), Fassungen/Vergleichen (#77/#78), Gehzeiten (#71), kleineres Lesemodell (#59), Baseline-Profil.

## Offene Fragen an den Projektinhaber
- Zeitpunkt der Store-Fotos, wer prüft die Marke?
- Baseline-Profil (zusätzliches Gradle-Modul): vor oder nach 1.0?
- Passt die Reihenfolge der Abnahmeliste? (Frage 2 zu Voreinstellungen in der Sicherung ist mit 0.52.0 erledigt.)

## Offene Issues
#16 neue Version melden (nur auf Antippen; prüfen ob erledigt) · #47 · #52 · #57 (schließbar nach Entscheidung) · #59 · #62 · #71 · #73 · #74 · #75 · #76 · #77 · #78.

## Bekannte kleine Mängel
Nach Umschalten der Sortierung bleibt die Liste an der Scrollposition. Gesamtmengen-Kacheln im Teigrechner werden bei langen Texten am Rand abgeschnitten (wischbar). Bildschirmfoto „26a-schlagwort-vorschlaege“ zeigt die Chips nicht. Pseudosprache-Fotos der Rezeptansicht nicht ausführlich gesichtet.

## Stand 21.10.2026: YouTube ohne Rezepttext (Zweig `video-hinweis`, Version 0.54.1, lokal committet, NICHT gepusht)
- GitHub-Guthaben (Free-Konto) für Oktober aufgebraucht: kein Build/Release möglich. Zweig erst pushen und bauen, wenn Guthaben da ist oder ein Ersatz-Weg steht (Frage an den Inhaber: kostenlose Alternative zu GitHub Actions).
- Stufe 3 erledigt (nur Text): Meldung `video_no_recipe` nennt den Weg über Bildschirmfoto und „Aus Foto übernehmen“.
- Stufe 1 (automatische Untertitel) am 21.10. geprüft und NICHT machbar: Die Videoseite enthält zwar `captionTracks`, aber `timedtext` antwortet ohne Token (PO-Token) mit leerer Antwort (HTTP 200, 0 Bytes); der Android-Client der Innertube-Schnittstelle gibt 400. Umgehen wäre fragil und ein Eingriff in YouTubes Schutz – nicht weiterverfolgt.
- Ursache des „Guthabens“: Actions-Speicher 0,5 GB voll (1.762 Artefakte, ca. 18 GB gemeldet), Minuten sind frei. Sicherung auf `D:\Claude\Sicherung\github-artefakte` per `.github/scripts/artefakte-sichern.ps1` (lief am 21.10. ohne `-Loeschen`; Protokoll prüfen, dann mit `-Loeschen`). Entscheidung des Inhabers: keine Windows-Aufgabe; bei ca. 350 MB Artefakten nachfragen. Offen: `retention-days` in `build.yml`.
- **Version 0.55.0 ist veröffentlicht** (22.10.2026, Release v0.55.0 mit APK und Prüfsumme, Build grün; Squash-Commit 9e34316 auf `main`, enthält auch 0.54.1). Einstellungen → Speicher mit Löschknöpfen für Pakete und Zwischenspeicher. „Sonstiges“ wird unter 1 kB ausgeblendet (Zweig `zuhoeren-modus`, ungebaut). Zweige `video-hinweis` und `speicher-anzeige` sind erledigt (lokal und auf GitHub löschbar). GitHub-Artefakte am 22.10. gesichert und aufgeräumt (332 MB; bei über 350 MB wieder nachfragen) (`.github/scripts/artefakte-sichern.ps1`, 7-Tage-Grenze greift jetzt).
- Alternative zu Stufe 1 (nicht beschlossen): Zuhören-Modus (Mikrofon + vorhandene Spracherkennung #80) oder Hinweis zu angehefteten Kommentaren.

## Zuletzt erledigt (17.10.2026): Version 0.54.0
Zeitplan für Teige (#71, Stufe 1, Issue bleibt offen): „Teig starten“, Stand und nächster Zeitpunkt im Rezept und als Kachel auf der Startseite, nur Anzeige. Dazu: schlankere Regler im Teigrechner, Rezeptansicht wieder in normaler Schrift (nur aktueller Schritt groß), gestrichelte Linien zwischen Zutaten.
- Ideen für Stufe 2: Startzeit wählbar, Übersetzung der Phasentexte, fertige Läufe ausblenden, Zeitplan bearbeiten.
- Nächster Schritt: Build/Release auf main prüfen, dann Weg zu 1.0 (Abnahmeliste oben).

## In Arbeit: „Aus Audio übernehmen“ und „Aus Sprache übernehmen“ (Zweig `zuhoeren-modus`, Beta, freigegeben 22.10.2026)
Freigabe des Inhabers: Mikrofon-Zweck erweitert um „Rezept einsprechen“, neue Erfassungswege (Vision/CLAUDE.md, Manifest-Kommentar und Datenschutztext sind angepasst). Verworfen: Download oder Mitschnitt von YouTube-Ton (Bedingungen von YouTube, technisch blockiert). Kein Mikrofon bei „Aus Audio“.
- Schritt 1 erledigt: `SpokenNumbers`, `SpokenRecipe` + Tests. Schritt 2 programmiert (ungebaut, 22.10.): `AudioDecoder`, `PcmConverter`, `VoskTranscriber`, `EditViewModel.importAudio`, Eintrag „Aus Audio übernehmen“ im Hinzufügen-Menü (Home und Sammlung), Texte DE/EN, Rundgang `hinzufuegenMitAudio`. **Ungeprüft:** Kompilieren, Unit-Tests, echte Erkennung (Qualität des kleinen Modells mit einer echten Aufnahme am Handy prüfen), Verhalten bei Dateien ohne Längenangabe.
- Schritt 3: „Aus Sprache übernehmen“ mit Mikrofon, Text erscheint live, Prüfen vor dem Entwurf.
- Danach: Texte DE/EN (feste Begriffe in CLAUDE.md ergänzen), Rundgang, Version 0.56.0, CHANGELOG, Qualität an echtem Beispiel prüfen (kleines Modell).
