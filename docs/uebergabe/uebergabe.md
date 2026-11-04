# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 04.11.2026 · `main` = Version 0.73.0 (zusätzliche Hefe im Hauptteig, Freigabe des Inhabers vom 04.11.2026; davor 0.72.0 Teigrechner „Aufklappen“), Release v0.73.0 erschienen (Build 41832772904 grün: Build, Testlauf, Release) · Arbeitszweig `claude/trusting-galileo-xg0tgz` = `main` (hineingemischt) plus diese Übergabe · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Zuletzt: Teigrechner auf einem Bildschirm (0.72.0, auf `main`)
- Anlass: Inhaber musste auf seinem Handy für „Wasser (Hydration)“ und Öl scrollen. Fünf bedienbare Entwürfe auf der privaten Entwurfsseite https://claude.ai/artifact/RAw2psMZkncGoKijB3sHSq; gewählt am 04.11.2026: **Entwurf 5 „Aufklappen“**, dazu zwei Wünsche (Vorteig mit Gesamtgewicht, Zeile „Hauptteig“).
- Umgesetzt: bei normaler Schrift Bereiche Hefe, Vorteig, Teig (`ui/dough/DoughSection.kt`; immer einer offen, Standard „Teig“; Zusammenfassung je Bereich, z. B. „Biga · Mehl 30 % · Wasser 50 % · 277 g“). Ergebniskarte `ResultTop`: Gesamtteig als erste, hervorgehobene Kachel, darunter „Biga: …“ und neu „Hauptteig: Biga, 429 g Mehl, 276 g Wasser, 17 g Salz“ (`data/dough/DoughSummary.kt`, Test `DoughSummaryTest`). Pasta: Größe + Zutaten; Kuchen: Größe, Hefe (nur Hefeteig), Zutaten; Standard offen „Zutaten“. Große Schrift unverändert (Karten zum Scrollen).
- Bewusst so: Bei Biga/Poolish steckt die ganze Hefe im Vorteig (Rechnung unverändert); Hefe in der Zeile „Hauptteig“ gibt es nur beim Sauerteig. Zusätzliche Hefe im Hauptteig bei Biga wäre eine neue Einstellung (Inhaber fragen). „Neu in Version“ nicht geändert (wie 0.61–0.71).
- Rundgang: `openDoughSection(...)` in `AppTourTest` klappt „Vorteig“ vor Biga und „Teig“ vor „Weitere Zutat“ auf; erste Prüfung wartet auf „4 × 250 g“ (`dough_summary_pieces`).
- Build: Lauf 384 rot nur wegen Lint `NonObservableLocale` (`Locale.getDefault()` direkt in einer Composable; behoben mit `LocalConfiguration`), Lauf 385 grün (Lint, Unit-Tests, Release-APK, Rundgang). Bildschirmfotos (Artefakt „bildschirmfotos-0.72.0“) in der Cloud-Sitzung nicht ladbar: Host `productionresultssa4.blob.core.windows.net` von den Netzwerkregeln gesperrt.
- 0.73.0 (auf `main`, Wunsch vom 04.11.2026): Bei Biga/Poolish zweites Hefefeld „Zusätzlich im Hauptteig (% vom Mehl)“, das erste heißt dann „Im Vorteig (% vom Mehl)“. Wert `LeavenedInput.mainYeast` (`DoughField.MAIN_YEAST`, Pizza bis 5 %, Brot bis 3 %), nur bei `Preferment.carriesYeast` gerechnet und geprüft; gemerkt als `mainYeast=` (ältere Werte → 0); Voreinstellungen vergleichen ihn nur mit Biga/Poolish. Zusammenfassung „Frischhefe · Vorteig 0,2 % · Hauptteig 0,1 % · 1,8 g“.
- **Nächster Schritt:** Inhaber prüft Release 0.73.0 auf dem Handy (Aufklappen, Hauptteig-Zeile, zusätzliche Hefe). Neue Arbeit auf dem Arbeitszweig beginnen; er enthält `main` bereits.

## Offen / zurückgestellt
- Schrift 200 % verbessern (Inhaber: „später“; Bildschirm und Gerät nannte er nicht).
- YouTube-Link: Feld „Video“ unter „Quelle“ gibt es schon; Inhaber nennt bei Bedarf ein Rezept, bei dem er fehlt.
- Wiederherstellen mit Auswahl (ältere ersetzen, fehlende ergänzen, Doppelte einzeln bestätigen): eigenes Vorhaben, noch auszuarbeiten.
- Windows-Version: erst nach 1.0 (Besprechung `docs/besprechungen/2026-10-23-tablet-und-windows.md`).
- Echtes Handy/Tablet prüfen: Kochleiste oben (Handy quer), Menü beim langen Berühren, Schriftgrößen, Kopfhörer-Befehle, Teigrechner „Aufklappen“.

## Weg zu Version 1.0 (Abnahmeliste)
1. Prüfung auf echtem Handy durch den Projektinhaber: Blättern, Vorlesen mit TalkBack, Spracheingabe, Kopfhörertasten, „Aufnehmen“, „Aus Audio“, Texterkennung/Übersetzung messen (#52, #62).
2. Teigwissen-Artikel von einer Fachperson gegenlesen (oder „ohne Gewähr“ belassen); Erklärtexte im Teigrechner gegenlesen.
3. Store-Auftritt (#73): eigene Gerichte-Fotos (ohne Aufnahmeort, keine erkennbaren Personen), Texte, Spendenlink nur im Store-Eintrag; Markenname „Molto Bene“ vorher im Markenregister prüfen.
4. `/pruefrunde` vor 1.0 (automatisch laut `CLAUDE.md`); Issues erst nach Freigabe.
Nicht nötig für 1.0: PDF (#47), Umzug aus Apps (#76), Fassungen/Vergleichen (#77/#78), Gehzeiten (#71 Stufe 2), kleineres Lesemodell (#59), Baseline-Profil.

Offene Fragen an den Inhaber: Zeitpunkt der Store-Fotos, wer prüft die Marke; Baseline-Profil vor oder nach 1.0; Reihenfolge der Abnahmeliste.
Offene Issues: #16 (prüfen ob erledigt), #47, #52, #57 (schließbar nach Entscheidung), #59, #62, #71, #73–#78.

## Arbeitsweise (bewährt)
- Ein Zweig je Aufgabe, bauen mit `gh workflow run build.yml --ref <zweig>`, auf grün warten, **Bildschirmfotos ansehen** (`gh run download <id> -n bildschirmfotos-<version> -D bildschirmfotos/<name>`), dann `git merge --squash` auf `main` mit `Fixes #<Nr>`. Ein Build dauert ca. 20 Minuten.
- Cloud-Sitzung (ohne `gh`): Build per GitHub-Werkzeug „run_workflow“ auf dem Zweig starten; Stand ohne Anmeldung über `https://api.github.com/repos/boerni328-bene/Moltobene/actions/runs/<id>/jobs` abfragen. Android-SDK lässt sich dort nicht laden (dl.google.com gesperrt), also nur in GitHub bauen.
- Fehlersuche: Artefakt `testbericht-<version>` herunterladen (in den Scratchpad-Ordner) und die HTML-Berichte unter `app/build/reports/androidTests/connected/debug/` lesen; `--log-failed` zeigt oft nur „exit code 1“. Zeilen mit `texterkennung[...]` und `umrechnenNachBackform[PSEUDO]` sind bewusst übersprungen.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug schreiben; `gh` unter `C:\Program Files\GitHub CLI\gh.exe`; kein Python; `git push` scheitert gelegentlich kurz, wiederholen. Warten auf Builds mit `until`-Schleife im Hintergrund, nicht mit `sleep`.
- Parallele Sitzungen im selben Ordner kommen vor: vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen. Ein alter Stash („Doppelte 0.14.1-Fassung“) liegt unberührt.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern (`SideEffect`); Eigenschaften in ViewModels vor dem `init`-Block deklarieren; in Compose Lambdas statt Funktionsverweisen (`{ select(it) }` statt `::select`, sonst nach „Zurücksetzen“ veraltet); bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)`; Pseudosprache setzt Werte in »…«; GitHub-Actions-Speicher kann voll sein (Sicherung per `.github/scripts/artefakte-sichern.ps1`, bei über 350 MB nachfragen).
- Token sparen: Aufbau-Datei nur bei Bedarf lesen, erst suchen dann gezielt lesen, Spezialisten/Prüfrunden nur gezielt.
