# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 17.10.2026 · App-Version 0.53.0 (versionCode 93) · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, nur auf dem Handy. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Git-Stand
- `origin/main` = Version 0.52.1 (Commit `ce4a375`). Arbeitszweig `kochen-teig-einstellungen` liegt 2 Commits davor: 0.53.0 (Kochschrift größer, Teigrechner kompakter mit Vorteig in der Leiste, Einstellungen als aufklappbare Gruppen) und Rundgang-Anpassung. **Noch nicht nach `main` gemergt**; Build-/Rundgangsergebnis des Zweigs prüfen, dann Squash-Merge mit Bildschirmfotos-Sichtung.
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

## Laufende Arbeit (17.10.2026): Zeitplan für Teige (#71), Zweig `teig-zeitplan`, Version 0.54.0 (versionCode 94)
Wunsch des Projektinhabers: Zeitplan für Teige (nicht für Kochschritte), z. B. Biga 1–3 Tage kühl; im Rezept „Teig starten“, dann immer Stand und nächster Zeitpunkt. Entscheidungen: nur Anzeige in der App (kein Wecker, keine Berechtigung), Zeitplan im Rezept, zuerst Pizza und Brot. Plan: `C:\Users\bernd\.claude\plans\mossy-swimming-dahl.md`.
- Umgesetzt (noch nicht gebaut, kein PC-Build möglich): `Recipe.timeline` + Tabellen `recipe_phases`/`dough_runs` (Datenbank 4, AutoMigration 3→4), Sicherung (`timeline` optional), `DoughTimeline`/`DoughRunPlanner`/`DurationUnit`, Zeitplan als Zubereitung in `DoughDraft`, `ui/schedule/ScheduleCard` im Rezept, Kachel „Laufende Teige“ auf der Startseite, Tests, Rundgangtest `teigZeitplan`, Texte beider Sprachen, CHANGELOG, Aufbau, Begriffe in `CLAUDE.md`.
- Der Zweig `teig-zeitplan` wurde von `kochen-teig-einstellungen` (0.53.0, noch nicht in `main`) abgezweigt. Eine parallele Sitzung hat auf ihm den Commit `afa1188` („wieder je zwei Felder pro Reihe“) angelegt, der versehentlich meine ersten Dateien mitnahm; nichts gepusht. Beim Merge zusammen mit 0.53.0 nach `main` (Squash).
- Nächster Schritt: pushen, `gh workflow run build.yml --ref teig-zeitplan`, Fehler beheben, Bildschirmfotos 14p–14s und 14c prüfen (Teigrechner-Entwurf mit Zubereitung), dann Squash-Merge. Offen/Ideen: Startzeit wählbar, Übersetzung der Phasentexte, fertige Läufe von der Startseite ausblenden, Uhr-App-Übergabe (#71 Stufe 2, nur nach Freigabe), Zeitplan bearbeiten.
- `CLAUDE.md` verschlankt (Codeaufbau in `docs/aufbau.md`), Übergabe-Regel eingeführt (früher am Tag erledigt).
