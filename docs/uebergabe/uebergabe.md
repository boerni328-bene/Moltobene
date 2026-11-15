# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 15.11.2026 · `main` = **Version 0.74.4** (Release v0.74.4 erschienen, Build 43687203670 grün) · Zweig `hefe-automatik` = **0.75.0 Hefe-Automatik** (Build 43698726260 grün, Fotos 14x angesehen, wartet auf Freigabe für `main`) · `verbesserungen-0741`, `guava-sicherheit`, `guava-sicherheit-2` sind vollständig auf `main` und können gelöscht werden · Cloud-Zweig `claude/trusting-galileo-xg0tgz` = alter Stand 0.74.0 plus Übergabe · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## In Arbeit: 0.75.0 Hefe-Automatik (Zweig `hefe-automatik`)
- `data/dough/DoughYeast` (Entwurf 8): Gärfaktor 2^((T − 24)/8); ohne Vorteig 1,6 / (Gehzeit Teig + Teigkugeln + Aufwärmen bei 24 °C), Biga 3,08 bzw. Poolish 6,73 × Vorteiganteil / Reifezeit bei 24 °C; in Hefeart umgerechnet, auf 0,01 gerundet, in Grenzen. Standardwerte ergeben 0,2 % (Pizza), Brot ohne Vorteig 0,63 % (bisher 0,5).
- `DoughProcess.yeastAuto` (Standard an, gemerkt als `yeastAuto=`; alte Werte starten mit an); Schalter `ui/dough/YeastAutoSwitch` im Bereich Hefe; Tippen oder −/+ am Hefefeld schaltet aus; Zusammenfassung endet mit „automatisch“. Sauerteig ohne Automatik.
- Eigene Entscheidung (dem Inhaber gemeldet): Mit Automatik zählt die Hefe beim Vergleich mit Voreinstellungen nicht; die Hefe der Voreinstellung wird erst wirksam, wenn die Automatik aus ist.
- Unit-Tests `DoughYeastTest`; Rundgang 14x (Schalter an/aus).
- Bewusst noch groß: `DoughScreen.kt` (~1770 Zeilen), `EditViewModel.kt` (~1660).
## Offene Entscheidungen des Inhabers
- Nach 0.75.0: erst 0.76.0 (Uhrzeiten) oder erst der Weg zu 1.0?
- Teigrechner in zwei Stufen („einfach“ und „Feinheiten“), erst als Entwurf?
- Zwischenstände als Vorabversion statt als „neueste Version“?
- Frühe Prüfung auf einem echten Gerät (0.73.0/0.74.x auf dem Handy).

## Technische Notizen
- Dependabot #10 (Guava) **behoben** mit 0.74.3/0.74.4: Root-`build.gradle.kts` hebt Guava im Klassenpfad der Bauwerkzeuge (`constraints`) und in allen Konfigurationen des App-Moduls (`eachDependency`, Room-Compiler, Lint, Robolectric) auf mindestens `guava` aus `libs.versions.toml`. Prüfen geht vor dem Push mit `gh api repos/…/dependency-graph/compare/main...<zweig>`.
- Tablet-Test `kochbuchUndRezept` (Pfeiltaste, `TabletTourTest.kt:62`) schlug einmal auf drei Darstellungen fehl und lief beim Wiederholen grün: wackelig, ggf. längere Wartezeit.

## Geplant (nach Freigabe)
- **0.76.0 Uhrzeiten** (Liste aus 7A im Rezept, „Backen am … um …“, geplanter Teig auf der Startseite aus 7D). Dafür muss das Rezept die Teigrechner-Werte speichern (Datenbank-Migration und Sicherungsformat) und `DoughRun` eine Backzeit bekommen.
- Entwürfe 10C (Gehzeiten): https://claude.ai/artifact/RAw2psMZkncGoKijB3sHSq (privat).

## Offen / zurückgestellt
- Schrift 200 % verbessern (Inhaber: „später“).
- YouTube-Link: Feld „Video“ gibt es; Inhaber nennt bei Bedarf ein Rezept, bei dem er fehlt.
- Wiederherstellen mit Auswahl (ersetzen, ergänzen, Doppelte bestätigen): eigenes Vorhaben.
- Windows-Version: erst nach 1.0 (`docs/besprechungen/2026-10-23-tablet-und-windows.md`).
- Echtes Handy/Tablet prüfen: Kochleiste oben (quer), Menü beim langen Berühren, Schriftgrößen, Kopfhörer-Befehle.
- Cloud-Umgebung „Moltobene“: Artefakte liegen teils auf anderen Speicherhosts (z. B. `productionresultssa17…`); ergänzen nur nach Rückfrage.

## Weg zu Version 1.0 (Abnahmeliste)
1. Prüfung auf echtem Handy durch den Projektinhaber: Blättern, Vorlesen mit TalkBack, Spracheingabe, Kopfhörertasten, „Aufnehmen“, „Aus Audio“, Texterkennung/Übersetzung messen (#52, #62).
2. Teigwissen-Artikel und Erklärtexte im Teigrechner gegenlesen lassen (oder „ohne Gewähr“).
3. Store-Auftritt (#73): eigene Gerichte-Fotos, Texte, Spendenlink nur im Store-Eintrag; Marke „Molto Bene“ vorher prüfen.
4. `/pruefrunde` vor 1.0 (automatisch laut `CLAUDE.md`); Issues erst nach Freigabe.
Offene Issues: #16 (prüfen ob erledigt), #47, #52, #57, #59, #62, #71, #73–#78.

## Arbeitsweise (bewährt)
- Ein Zweig je Aufgabe, bauen mit „Run workflow“ auf dem Zweig, auf grün warten, **Bildschirmfotos ansehen**, dann als Squash auf `main` (bei Konflikten durch alte Squash-Basis: Baum des Zweigs übernehmen, wenn `main` sein Vorgänger-Inhalt ist). Ein Build dauert ca. 25 Minuten.
- Cloud-Sitzung (ohne `gh`): Build per GitHub-Werkzeug „run_workflow“ (`build.yml`, ref = Zweig); Stand über `https://api.github.com/repos/boerni328-bene/Moltobene/actions/runs/<id>/jobs`. Push auf `main` nur nach ausdrücklicher Freigabe je Version; kein Force-Push.
- Fehlersuche: Artefakt `testbericht-<version>`; Lint-Fehler (z. B. `NonObservableLocale`) lassen den Build scheitern.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern; Eigenschaften in ViewModels vor `init`; in Compose Lambdas statt Funktionsverweisen; bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)`; Pseudosprache setzt Werte in »…«; Actions-Speicher kann voll sein.
- Parallele Sitzungen kommen vor: vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen.
