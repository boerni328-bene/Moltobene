# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 14.11.2026 · `main` = **Version 0.74.2** (Release v0.74.2 erschienen, Build 43572656252 grün) · Zweig `verbesserungen-0741` ist vollständig auf `main` (als Squash), kann gelöscht werden · Cloud-Zweig `claude/trusting-galileo-xg0tgz` = alter Stand 0.74.0 plus Übergabe · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Zuletzt erschienen: 0.74.2 (enthält 0.74.1), 14.11.2026
- 0.74.1: Kachelreihen laufen am Rand weich aus (`ui/components/ScrollFade.kt`, `Modifier.scrollFade(state)` vor `horizontalScroll`); bei großer Schrift zeigt die Leiste unten nur Gesamtteig und „Als Rezept speichern“; Hinweis unter Gehzeiten beginnt mit „Zeiten und Temperaturen sind Richtwerte.“
- 0.74.2 (unsichtbar): `RecipeScreen.kt` aufgeteilt in `CookingBar.kt`, `RecipeParts.kt`, `RecipeTranslation.kt`; `EditScreen.kt` in `EditImportParts.kt`, `EditSections.kt` (`private` → `internal`).
- CHANGELOG-Abschnitt 0.74.2 nennt die Änderungen aus 0.74.1 mit, weil das Release nur den Abschnitt der aktuellen Version zeigt.
- Bewusst noch groß: `DoughScreen.kt` (~1740 Zeilen), `EditViewModel.kt` (~1660 Zeilen, Aufteilen ändert Logik), Funktionen `RecipeContent` und `EditScreen`.
- **Cloud-Sitzung:** vor 0.75.0 von `main` aus neu ansetzen (nicht vom alten Cloud-Zweig), sonst Konflikte in `DoughScreen.kt`, `PresetCard.kt`, `strings.xml`, `CHANGELOG.md`.

## Offene Entscheidungen des Inhabers
- Reihenfolge: erst 1.0, oder erst 0.75.0 (Hefe-Automatik) und 0.76.0 (Uhrzeiten)?
- Teigrechner in zwei Stufen („einfach“ und „Feinheiten“), erst als Entwurf?
- Zwischenstände als Vorabversion statt als „neueste Version“?
- Frühe Prüfung auf einem echten Gerät (0.73.0/0.74.x auf dem Handy).
- Dependabot-Meldung #10 („hoch“): Guava in den Build-Werkzeugen (`settings.gradle.kts`), nicht in der App; behoben ab Guava 33.7.2. Vorschlag: eigener Zweig, wartet auf Ja.

## Geplant (nach Freigabe)
- **0.75.0 Hefe-Automatik** (Entwurf 8: Frischhefe % = 1,6 / Gehzeit bei 24 °C ohne Vorteig, Biga 3,08 × Vorteiganteil / …, Poolish 6,73 × …; −/+ an der Hefe schaltet die Automatik aus; Sauerteig von Hand).
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
