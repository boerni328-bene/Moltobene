# Moltobene – Übergabe (einzige Datei, wird laufend überschrieben)

Stand: 12.11.2026 · `main` = Version 0.73.0 (Release v0.73.0 erschienen) · Arbeitszweig `claude/trusting-galileo-xg0tgz` = **0.74.0 (versionCode 125)**, Build auf dem Zweig gestartet, noch nicht auf `main` · Regeln in `CLAUDE.md`, Codeaufbau in `docs/aufbau.md`, Verlauf in Git und `CHANGELOG.md`.

## Projekt in drei Sätzen
Moltobene: Android-App (Kotlin, Compose, `com.moltobene.app`, ab Android 8), die Kochrezepte aus allen Quellen sammelt, lokal gespeichert, ohne Cloud. Kostenlos, GPL-3.0, GitHub Releases (`boerni328-bene/Moltobene`, öffentlich). Gebaut nur über GitHub Actions (kein Android Studio/Java auf dem PC). Projektinhaber ist kein Entwickler: Deutsch, kurz, vor größeren Entscheidungen fragen.

## Gerade in Arbeit: 0.74.0 „Gehzeiten und Temperaturen“ (Schritt 1 von 3)
- Inhaber hat am 12.11.2026 **Entwurf 10C** gewählt (ruhige Liste: Zeilen zeigen Werte, Antippen öffnet −/+ nebeneinander, wo nötig die Auswahl). Entwurfsseite (privat): https://claude.ai/artifact/RAw2psMZkncGoKijB3sHSq, Quellen nur im Scratchpad der Cloud-Sitzung.
- Umgesetzt auf dem Zweig (lokal 117 Unit-Tests der Rechenteile grün, App-Build nur auf GitHub):
  - Vierter Bereich im Teigrechner (Pizza/Brot): Raum und Kühlschrank, Vorteig reifen lassen, Teig nach dem Kneten (von Hand/mit Maschine, Wasser mit Temperatur), Teig gehen lassen, Teigkugeln bzw. Laib gehen lassen (Raum/Kühlschrank), Teigkugeln aufwärmen (Pizza aus dem Kühlschrank), Backofen. Bei großer Schrift als Karte.
  - Gehzeiten bei Raumtemperatur × 2^((22 − Raum)/8), auf Stufen gerundet; selbst geänderte fett und fest; Kühlschrank-Gehzeiten fest. Küche (Raum, Kühlschrank, Knetart) gilt für alle Teigarten, Gehzeiten je Teigart; gemerkt in `AppPreferences` (nicht in der Sicherung). „Zurücksetzen“ setzt alles zurück, auch die Küche.
  - Wassertemperatur per Wärmebilanz (Hand 3 °C, Maschine 8 °C Knetwärme; Biga kalt aus dem Kühlschrank), Warnung unter 2 °C / über 40 °C.
  - Mengen vorn: „1,2 g Frischhefe · 0,2 % vom Mehl“, „277 g Biga · Mehl 30 % (184 g) · Wasser 50 % (92 g)“, oben „277 g Biga: …“ und „1000 g Hauptteig: 277 g Biga, …, 276 g Wasser (30 °C), 17 g Salz“; ohne Vorteig „Wasser: 20 °C · Teig nach dem Kneten: 24 °C“.
  - Gespeichertes Rezept: Überschriften mit Gewicht, „277 g Biga (der ganze Vorteig)“, Wasserzeile mit Temperatur; Zeitplan mit den gewählten Gehzeiten (je eine Dauer statt Spanne, Arbeitsschritte mit Spanne) und Zubereitung je Schritt mit Mengen, Ort und Temperatur.
  - °C/°F unter Einstellungen › Teigrechner › Temperaturen; Link „°C/°F“ im Bereich öffnet `SettingsRoute(group = "dough")` direkt in der Gruppe, Zurück führt zum Teigrechner. In °F: Schritte 1 °F, Backofen 25 °F.
  - „Neu in Version 0.74.0“ aktualisiert (zuletzt war es 0.60.0). Rundgang: Bildschirmfotos 14u (Gehzeiten), 14v (Einstellungen Temperaturen), 14w (Teigrechner in °F); der Rundgang setzt vor jedem Test wieder °C.
- **Nächster Schritt:** Build auf dem Zweig bis grün (Lint, Unit-Tests, Release-APK, Testlauf auf dem Emulator), Bildschirmfotos 14a/14b/14u/14v/14w ansehen, dann Inhaber berichten. Auf `main` erst nach „Pushe die Version 0.74.0“.
- Danach (nach Freigabe): **0.75.0 Hefe-Automatik** (Entwurf 8: Frischhefe % = 1,6 / Gehzeit bei 24 °C ohne Vorteig, Biga 3,08 × Vorteiganteil / …, Poolish 6,73 × …; −/+ an der Hefe schaltet die Automatik aus; Sauerteig von Hand) und **0.76.0 Uhrzeiten** (Liste aus 7A im Rezept, „Backen am … um …“, geplanter Teig auf der Startseite aus 7D). Dafür muss das Rezept die Teigrechner-Werte speichern (Datenbank-Migration und Sicherungsformat) und `DoughRun` eine Backzeit bekommen.

## Offen / zurückgestellt
- Inhaber prüft Release 0.73.0 auf dem Handy (Aufklappen, Zeile „Hauptteig“, zusätzliche Hefe).
- Schrift 200 % verbessern (Inhaber: „später“).
- YouTube-Link: Feld „Video“ gibt es; Inhaber nennt bei Bedarf ein Rezept, bei dem er fehlt.
- Wiederherstellen mit Auswahl (ersetzen, ergänzen, Doppelte bestätigen): eigenes Vorhaben.
- Windows-Version: erst nach 1.0 (`docs/besprechungen/2026-10-23-tablet-und-windows.md`).
- Echtes Handy/Tablet prüfen: Kochleiste oben (quer), Menü beim langen Berühren, Schriftgrößen, Kopfhörer-Befehle.

## Weg zu Version 1.0 (Abnahmeliste)
1. Prüfung auf echtem Handy durch den Projektinhaber: Blättern, Vorlesen mit TalkBack, Spracheingabe, Kopfhörertasten, „Aufnehmen“, „Aus Audio“, Texterkennung/Übersetzung messen (#52, #62).
2. Teigwissen-Artikel und Erklärtexte im Teigrechner gegenlesen lassen (oder „ohne Gewähr“).
3. Store-Auftritt (#73): eigene Gerichte-Fotos, Texte, Spendenlink nur im Store-Eintrag; Marke „Molto Bene“ vorher prüfen.
4. `/pruefrunde` vor 1.0 (automatisch laut `CLAUDE.md`); Issues erst nach Freigabe.
Offene Issues: #16 (prüfen ob erledigt), #47, #52, #57, #59, #62, #71, #73–#78.

## Arbeitsweise (bewährt)
- Ein Zweig je Aufgabe, bauen mit „Run workflow“ auf dem Zweig, auf grün warten, **Bildschirmfotos ansehen**, dann auf `main`. Ein Build dauert ca. 20 Minuten.
- Cloud-Sitzung (ohne `gh`): Build per GitHub-Werkzeug „run_workflow“ (`build.yml`, ref = Zweig); Stand über `https://api.github.com/repos/boerni328-bene/Moltobene/actions/runs/<id>/jobs`. Android-SDK und Artefakte lassen sich dort nicht laden; Rechenteile lokal mit dem Gradle-Gerüst im Scratchpad (`jvmcheck`, nur `data/dough` u. a.) prüfen. Push auf `main` nur nach ausdrücklicher Freigabe je Version; kein Force-Push.
- Fehlersuche: Artefakt `testbericht-<version>` (HTML unter `app/build/reports/androidTests/connected/debug/`); Lint-Fehler (z. B. `NonObservableLocale`) lassen den Build scheitern.
- Stolpersteine: `remember { }` darf nicht `Unit` liefern; Eigenschaften in ViewModels vor `init`; in Compose Lambdas statt Funktionsverweisen; bei durchsichtiger Leiste Tests mit `performSemanticsAction(OnClick)`; Pseudosprache setzt Werte in »…«; Actions-Speicher kann voll sein.
- Parallele Sitzungen kommen vor: vor Commit, Push und Versionssprung `git fetch origin`, Zweig und `origin/main` prüfen.
