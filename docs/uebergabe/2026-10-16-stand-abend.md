# Moltobene – Übergabe: Stand am Abend des 16.10.2026 (App-Version 0.51.0)

Zum Kopieren in einen neuen Chat. Ergänzt `docs/uebergabe/2026-10-16-stand-aller-sitzungen.md` (Projekt, Arbeitsweise, Aufbau; weiterhin gültig) und `docs/uebergabe/2026-10-16-abnahmeliste-1-0.md`. Regeln stehen in `CLAUDE.md`.

## 1. Was in diesem Chat passiert ist (alles auf `main`, Release 0.51.0 ist draußen)

| Version | Inhalt |
|---|---|
| 0.48.1 | Link-Dialog: Hinweis „ohne Cookies und ohne Anmeldung“ (Rang 8) |
| 0.48.2 | Rezeptansicht: Foto reicht bis unter die durchsichtige Leiste, Verlauf oben (Rang 9); „Weiter“ scrollt den Schritt nicht unter die Leiste (`BringIntoView` mit Rand) |
| 0.48.3 | Rundgang mit fünfter Darstellung „pseudo“ (Pseudosprache `en-XA`, `isPseudoLocalesEnabled` im Debug-Build) gegen abgeschnittene Texte (Rang 7); `DisplayVariant.contentLanguage` für mitgelieferte Inhalte |
| 0.49.0 | Sammlung sortieren: „Titel A–Z“ (mit `Collator`, Umlaute bei ihrem Buchstaben) und „Zuletzt hinzugefügt“; Symbol neben der Suche, Wahl in `AppPreferences`, `data/CollectionSort.kt` |
| 0.49.1 | Texte-Durchgang #58: Leerzustand, Kernsatz unter Einstellungen → Info (`app_tagline`), „wiederherstellen“ statt „übernehmen“ bei der Sicherung, englischer Absatz im Release-Text |
| 0.50.0 | #61: „Große Schrift beim Kochen“ im Menü ⋮ der Rezeptansicht (gemerkt), „Notiz hinzufügen/bearbeiten“ direkt in der Rezeptansicht |
| 0.51.0 | Schlagwörter beim Übernehmen: Kategorie/Stichwörter einer Seite als Vorschläge zum Antippen; aus Rezeptdateien werden Schlagwörter eingelesen (Deutsch und Englisch erkannt) |

Weiteres: Prüflauf #52 neu gemessen (Android 8, 1,5 GB, 1.000 Rezepte): Kaltstart 1.018 ms (zuvor 907), Liste 65 ms, Suche 51 ms, kein Absturz; Blättern nicht aussagekräftig (Emulator ohne Grafikchip). Issues #49, #79–#84, #58, #61 geschlossen. Abnahmeliste für 1.0 als Dokument angelegt (Kommentar in #57). Außerhalb des Projekts: `D:\Claude\Mods-und-Plugins.txt` beschreibt Mod „usage-band“, Spezialisten und Skills samt Einrichtung.

## 2. Arbeitsweise, die sich bewährt hat (Ergänzungen)
- Je Aufgabe ein Zweig (`git checkout -b …`), bauen mit `gh workflow run build.yml --ref <zweig>`, auf grün warten, **Bildschirmfotos ansehen** (`gh run download <id> -n bildschirmfotos-<version> -D bildschirmfotos/<name>`), dann `git merge --squash <zweig>` auf `main`, Zweig löschen. Vor jedem Push `git fetch origin`. Wenn zwei Zweige aufeinander aufbauen, lieber den fertigen Baum übernehmen (`git checkout <zweig> -- .`) als zweimal squashen (Konflikte in CHANGELOG/Version).
- Warten auf Builds: `until [ "$(gh run view <id> --json status -q .status)" = "completed" ]; do sleep 20; done` (Befehl läuft nach 10 Minuten im Hintergrund weiter und meldet sich).
- Fehlersuche im Testlauf: `gh run view <id> --log | grep "##\[error\]com"` listet fehlgeschlagene Tests; Zeilen mit `texterkennung[...]` und `umrechnenNachBackform[PSEUDO]` sind bewusst übersprungene Tests (Assumption), keine Fehler. Der Testbericht (`testbericht-<version>`) nennt die Zeile.
- Stolpersteine: Beispielrezept Tomatensoße hat schon eine Notiz (Knopf heißt „Notiz bearbeiten“); `remember { … }` darf nicht `Unit` zurückgeben (Lint-Fehler, nimm `SideEffect`); nach `performScrollTo` liegt das Ziel bei durchsichtiger Leiste unter der Leiste → im Test `performSemanticsAction(OnClick)`; Pseudosprache setzt eingefügte Werte in »…«, genaue Texttests dort überspringen; `git push` zu GitHub scheitert gelegentlich kurz, wiederholen.
- Kein Python auf dem PC; Dateien mit `sed`/Edit-Werkzeug ändern. Mehrzeilige `sed`-Ersetzungen mit `\n` in Anführungszeichen gehen manchmal schief → Ergebnis prüfen.

## 3. Offener Stand
- **Vor 1.0 noch zu tun (Abnahmeliste):** Prüfung auf einem **echten** Handy durch den Projektinhaber (Blättern, Vorlesen mit TalkBack, Spracheingabe mit echtem Mikrofon, Kopfhörertasten, Texterkennung/Übersetzung messen: #52, #62); Teigwissen-Artikel von einer Fachperson gegenlesen lassen; Store-Auftritt (#73: eigene Gerichte-Fotos, Texte, Spendenlink nur im Store-Eintrag) und Markenprüfung „Molto Bene“; **Prüfrunde vor 1.0** (`/pruefrunde`, laut `CLAUDE.md` vor Änderung der ersten Stelle der Versionsnummer, automatisch).
- **Vier Fragen an den Projektinhaber (noch unbeantwortet):** (1) Passt die Reihenfolge der Abnahmeliste, gehört „Schlagwörter beim Übernehmen“ zu 1.0 (inzwischen umgesetzt)? (2) Sollen die eigenen Voreinstellungen des Teigrechners vor 1.0 in die Sicherung? (3) Wann entstehen die Store-Fotos, wer prüft die Marke? (4) Baseline-Profil (zusätzliches Gradle-Modul): vor oder nach 1.0?
- **Offene Issues:** #16 neue Version melden (laut Vision nur auf Antippen; prüfen, ob schon erledigt), #47 PDF, #52 (Messung echtes Handy, Texterkennung R8), #57 (Rest: nichts Wesentliches, kann geschlossen werden, sobald Abnahmeliste entschieden ist), #59 App kleiner (nach 1.0), #62, #71, #73, #74, #75, #76, #77, #78.
- **Bekannte kleine Mängel:** Nach dem Umschalten der Sortierung bleibt die Liste an der Scrollposition stehen (springt nicht nach oben). Die Gesamtmengen-Kacheln im Teigrechner werden bei langen Texten am Rand abgeschnitten (wischbar, bewusste Gestaltung). Bildschirmfoto „26a-schlagwort-vorschlaege“ zeigt die Chips nicht (liegen unter dem Bildrand). Pseudosprache-Fotos der Rezeptansicht noch nicht ausführlich gesichtet.
- **Nächster sinnvoller Schritt:** Fragen mit dem Projektinhaber klären, dann Prüfrunde vor 1.0 (`/pruefrunde`, Issues erst nach Freigabe) und die Aufgaben „echtes Handy“ planen.

## 4. Nicht vergessen
Neue Berechtigungen, Bibliotheken und Architekturänderungen nur nach Rückfrage; alle Texte in beide `strings.xml`; Versionsnummer und CHANGELOG bei jeder Änderung außer reiner Dokumentation; Antworten auf Deutsch, kurz und ohne Fachbegriffe (Projektinhaber ist kein Entwickler).
