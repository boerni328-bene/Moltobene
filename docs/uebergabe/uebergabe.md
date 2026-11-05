# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 05.11.2026 · `main` = Version 0.73.0 (Release v0.73.0 erschienen, Build 41832772904 grün) · Arbeitszweig `claude/trusting-galileo-xg0tgz` = `main` plus diese Übergabe · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Gerade in Arbeit: Temperaturen, Mengen vorne, hilfreicher Zeitplan (nur Entwurf, noch kein Code)
- Bedienbarer **Entwurf 6** auf der privaten Entwurfsseite https://claude.ai/artifact/RAw2psMZkncGoKijB3sHSq (Seite „Temperaturen und Zeitplan“; Quelldateien nur im Scratchpad der Cloud-Sitzung). Drei Ansichten: Teigrechner, gespeichertes Rezept (Zeitplan | Zutaten), Einstellungen › Teigrechner.
- Entscheidungen des Inhabers vom 05.11.2026: Temperaturen gehören in den **Teigrechner** (eigener vierter Bereich „Temperaturen“: Raum, Kühlschrank, Teig nach dem Kneten, Backofen, Kneten „Von Hand | Mit Maschine“); die App rechnet die **Wassertemperatur**; die **Backofentemperatur** steht beim Backen; **°C/°F umschaltbar** unter Einstellungen › Teigrechner (neben Gewichte, Plus und Minus).
- Mengen vorne: Hefe „1,2 g Frischhefe · 0,2 % vom Mehl“ (mit zusätzlicher Hefe „1,8 g Frischhefe · Vorteig 0,2 % (1,2 g) · Hauptteig 0,1 % (0,6 g)“); Vorteig „277 g Biga · Mehl 30 % (184 g) · Wasser 50 % (92 g)“; Ergebniskarte „277 g Biga: …“ und „1000 g Hauptteig: 277 g Biga, 429 g Mehl, 276 g Wasser (30 °C), 17 g Salz“; im Rezept „Für den Vorteig (277 g Biga)“, „Für den Hauptteig (1000 g)“ mit erster Zeile „277 g Biga (der ganze Vorteig)“.
- Zeitplan im Entwurf: Schritte mit Mengen, Temperaturen, Dauer und Erkennungszeichen (Biga „leicht säuerlich, etwas aufgegangen“, Poolish „voller Bläschen, beginnt einzusinken“), Salz nach etwa 5 Minuten, Teigtemperatur mit Thermometer prüfen, Backofen rechtzeitig vorheizen. Ort „bei Raumtemperatur, 22 °C“ (heute „bei Zimmertemperatur“).
- Rechnung (Richtwerte, **vor dem Einbau prüfen/recherchieren**): Wärmebilanz Mehl 1,7 und Wasser 4,18 kJ/(kg·K), Knetwärme von Hand 3 °C, Maschine 8 °C; Mehl und Poolish/Sauerteig mit Raumtemperatur, Biga mit Kühlschranktemperatur; Hinweis unter 2 °C und über 40 °C. Beispiel Pizza mit Biga: Wasser 30 °C von Hand, 19 °C mit Maschine.
- Beim Einbau beachten: Temperaturen merken wie die übrigen Werte (`DoughMemory`, alte Werte lesbar); prüfen, ob `TimelinePhase` neue Felder braucht (dann Datenbank-Migration und Sicherungsformat); neue Texte in beide `strings.xml`; Rundgang ergänzen. Mit offenem Bereich „Temperaturen“ passt auf dem kleinen Testhandy (360 × 712) nicht mehr alles ohne Scrollen.
- **Nächster Schritt:** Inhaber prüft Entwurf 6 und gibt frei oder nennt Änderungen; offen ist auch, ob der Zeitplan Uhrzeiten rückwärts vom Backzeitpunkt rechnen soll. Danach Umsetzung auf dem Arbeitszweig (Version 0.74.0).

## Offen / zurückgestellt
- Inhaber prüft Release 0.73.0 auf dem Handy (Aufklappen, Zeile „Hauptteig“, zusätzliche Hefe).
- Schrift 200 % verbessern (Inhaber: „später“; Bildschirm und Gerät nannte er nicht).
- YouTube-Link: Feld „Video“ unter „Quelle“ gibt es schon; Inhaber nennt bei Bedarf ein Rezept, bei dem er fehlt.
- Wiederherstellen mit Auswahl (ältere ersetzen, fehlende ergänzen, Doppelte einzeln bestätigen): eigenes Vorhaben, noch auszuarbeiten.
- Windows-Version: erst nach 1.0 (Besprechung `docs/besprechungen/2026-10-23-tablet-und-windows.md`).
- Echtes Handy/Tablet prüfen: Kochleiste oben (Handy quer), Menü beim langen Berühren, Schriftgrößen, Kopfhörer-Befehle.

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
- Cloud-Sitzung (ohne `gh`): Build per GitHub-Werkzeug „run_workflow“ auf dem Zweig starten; Stand ohne Anmeldung über `https://api.github.com/repos/boerni328-bene/Moltobene/actions/runs/<id>/jobs` abfragen. Android-SDK und Artefakte lassen sich dort nicht laden (dl.google.com und der Artefakt-Speicher sind gesperrt), also nur in GitHub bauen. Push auf `main` nur nach ausdrücklicher Freigabe je Version; kein Force-Push.
- Fehlersuche: Artefakt `testbericht-<version>` herunterladen (in den Scratchpad-Ordner) und die HTML-Berichte unter `app/build/reports/androidTests/connected/debug/` lesen; `--log-failed` zeigt oft nur „exit code 1“. Zeilen mit `texterkennung[...]` und `umrechnenNachBackform[PSEUDO]` sind bewusst übersprungen. Lint-Fehler (z. B. `NonObservableLocale`: in Composables `LocalConfiguration` statt `Locale.getDefault()`) lassen den Build scheitern.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug schreiben; `gh` unter `C:\Program Files\GitHub CLI\gh.exe`; kein Python; `git push` scheitert gelegentlich kurz, wiederholen. Warten auf Builds mit `until`-Schleife im Hintergrund, nicht mit `sleep`.
- Parallele Sitzungen im selben Ordner kommen vor: vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen. Ein alter Stash („Doppelte 0.14.1-Fassung“) liegt unberührt.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern (`SideEffect`); Eigenschaften in ViewModels vor dem `init`-Block deklarieren; in Compose Lambdas statt Funktionsverweisen (`{ select(it) }` statt `::select`, sonst nach „Zurücksetzen“ veraltet); bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)`; Pseudosprache setzt Werte in »…«; GitHub-Actions-Speicher kann voll sein (Sicherung per `.github/scripts/artefakte-sichern.ps1`, bei über 350 MB nachfragen).
- Token sparen: Aufbau-Datei nur bei Bedarf lesen, erst suchen dann gezielt lesen, Spezialisten/Prüfrunden nur gezielt.
