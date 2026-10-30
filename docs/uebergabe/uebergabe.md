# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 30.10.2026 · `main` = Version 0.61.0 (versionCode 108), Release folgt aus dem Build auf `main` · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud (neuer Kernsatz seit 0.59.2). Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Stand 26./27.10.2026 (alles auf `main`, Zweige gelöscht)
- 0.59.0 Tablet (#91): zweispaltige Rezeptansicht ab 720 dp (`ui/components/WideLayout.kt`), Pfeiltasten (Auf/Ab blättern, Links/Rechts wechseln Reiter), Sprachbefehle beim Vorlesen mit Kopfhörern (`data/speech/Headphones.kt`; über Lautsprecher nur „Stopp“, keine Pausen gewünscht).
- 0.59.1 (#92–#94): „Aufnehmen“ überlebt Drehen/Falten, Foto-Auswahl-Höhe aus dem freien Platz, Hinweiskarte zur Sicherungsdatei (kein Abgleich, unverschlüsselt).
- 0.59.2: Kernsatz „… an einem Ort, lokal gespeichert, ohne Cloud.“ (README, CLAUDE.md, Release-Text, App, GitHub-Beschreibung).
- 0.59.3: Handy quer (Höhe < 480 dp, nicht bei großer Schrift): Kochleiste (Zurück, Weiter, Mikrofon, Vorlesen, „Schritt x von y“) steht oben in der Leiste (`CookingBarTop`, `CookingBarHolder` in `RecipeScreen.kt`). Rundgang-Darstellung `PHONE_LANDSCAPE` (800×360).
- 0.60.3: Mengen rechtsbündig in eigener Spalte (`ui/components/IngredientLine.kt`, bei großer Schrift aus), Grundrezepte im Design der Rezepte.
- 0.61.0: Teigrechner Pizza/Brot auf einem Bild (normale Schrift; `CompactChoice`, Voreinstellungszeile mit Menü ⋮), Plus/Minus mit Dauerdruck (`ui/components/RepeatIconButton.kt`). Offen: Pasta und Kuchen ebenso ohne Scrollen, dann weitere Verbesserungen am Teigrechner vor 1.0.
- 0.60.1: Listenmenü ohne Schriftgröße (Wunsch Inhaber), Foto Handy quer flacher.
- 0.60.0: Menü beim langen Berühren eines Rezepts im Kochbuch (gleich wie ⋮ im Rezept, `ui/recipe/RecipeMenuItems.kt`, Aktionen in `CollectionViewModel`); Schriftgröße klein/mittel/groß (`data/CookingTextSize.kt`, ersetzt „Große Schrift beim Kochen“, alter Wert wird als „groß“ übernommen).
- Lehre: Eigenschaften in ViewModels vor dem `init`-Block deklarieren (sonst NullPointerException).

## Offen / zurückgestellt
- Schrift 200 % verbessern (Inhaber: „später“; Bildschirm und Gerät nannte er nicht).
- YouTube-Link: Feld „Video“ unter „Quelle“ gibt es schon; Inhaber nennt bei Bedarf ein Rezept, bei dem er fehlt.
- (erledigt in 0.60.1: Foto bei Handy quer flacher)
- (erledigt in 0.60.2: „Handy“ → „Gerät“ in deutschen Texten und README; „Wie das Gerät“ in den Einstellungen)
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
- Fehlersuche: Artefakt `testbericht-<version>` herunterladen (in den Scratchpad-Ordner) und die HTML-Berichte unter `app/build/reports/androidTests/connected/debug/` lesen; `--log-failed` zeigt oft nur „exit code 1“. Zeilen mit `texterkennung[...]` und `umrechnenNachBackform[PSEUDO]` sind bewusst übersprungen.
- Windows: Mehrzeiliges mit Umlauten per Write-Werkzeug schreiben; `gh` unter `C:\Program Files\GitHub CLI\gh.exe`; kein Python; `git push` scheitert gelegentlich kurz, wiederholen. Warten auf Builds mit `until`-Schleife im Hintergrund, nicht mit `sleep`.
- Parallele Sitzungen im selben Ordner kommen vor: vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen. Ein alter Stash („Doppelte 0.14.1-Fassung“) liegt unberührt.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern (`SideEffect`); bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)`; Pseudosprache setzt Werte in »…«; GitHub-Actions-Speicher kann voll sein (Sicherung per `.github/scripts/artefakte-sichern.ps1`, bei über 350 MB nachfragen).
- Token sparen: Aufbau-Datei nur bei Bedarf lesen, erst suchen dann gezielt lesen, Spezialisten/Prüfrunden nur gezielt.
