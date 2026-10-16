# Moltobene – Abnahmeliste für Version 1.0 (Stand 16.10.2026, App-Version 0.48.3)

Vorschlag zu Issue #57, zur Entscheidung durch den Projektinhaber. Maßstab ist die Vision in `CLAUDE.md`. Häkchen = erledigt und geprüft (Rundgang auf dem Emulator oder Messung), „offen“ = fehlt noch.

## 1. Die fünf Kernfunktionen

| Kernfunktion | Stand | Offen |
|---|---|---|
| 1. Rezept hinzufügen (eintippen, Foto) | ✅ | – |
| 2. Rezept übernehmen: Link, Text, Zwischenablage, Foto mit Texterkennung, YouTube, Datei | ✅ | PDF-Seiten (#47, laut früherer Besprechung nicht nötig für 1.0); Umzug aus anderen Rezept-Apps (#76, nicht nötig für 1.0) |
| 3. Sammlung finden und ordnen: Suche, Favoriten, Entwürfe, Schlagwörter und Filter | ⚠️ weitgehend | **Sortieren** (Titel A–Z / zuletzt hinzugefügt, mit richtiger Einordnung von Umlauten) fehlt; Schlagwörter aus Rezeptdateien (`keywords`) und Vorschläge aus `recipeCategory` beim Übernehmen fehlen |
| 4. Mit dem Rezept kochen: Bildschirm an, abhaken, Portionen/Zutat/Backform umrechnen, Notizen, Vorlesen, Spracheingabe, Teigrechner, Teigwissen, Grundrezepte | ⚠️ weitgehend | Große Schrift beim Kochen und Notiz direkt in der Rezeptansicht (#61); Gehzeiten „fertig um …“ (#71, später) |
| 5. Teilen, sichern, umziehen | ✅ | Voreinstellungen des Teigrechners sind noch nicht in der Sicherung (steht in `CLAUDE.md`) – prüfen, ob für 1.0 nötig |

## 2. Qualität und Messungen

- ✅ Berechtigungsprüfung im Build, Signaturprüfung, Prüfsummen der Modelle.
- ✅ Prüflauf Android 8, 1.000 Rezepte mit Fotos (16.10.2026, Version 0.48.3): Kaltstart 1.018 ms, Liste laden 65 ms, Suche beim Tippen 51 ms, Zufallstest der Prüf-Fassung (R8) ohne Absturz (Ergebnis in #52).
- ⚠️ Offen aus #52: Blättern auf einem **echten** günstigen Handy messen; Texterkennung in der R8-Fassung und mit 1 bzw. 6 Seiten messen.
- ✅ Rundgang auf dem Emulator in fünf Darstellungen (hell, dunkel, Schrift 200 %, Englisch, Pseudosprache).
- ⚠️ Rundgang prüft nicht auf echtem Handy: TalkBack beim Vorlesen, Kopfhörertasten, Spracheingabe mit echtem Mikrofon, Vorlesestimmen (nur nachgestellt).
- ⚠️ Teigwissen-Artikel (Mehltypen, Getreide, Gluten) sind noch nicht von einer Fachperson gegengelesen (siehe `docs/recherche/2026-10-13-mehl-ueberblick.md`).

## 3. Texte und Sprachen

- ✅ Deutsch und Englisch vollständig, feste Begriffe, keine direkte Anrede (Tests prüfen das für Teigwissen, Grundrezepte und Hinweise).
- ⚠️ #58 Texte-Durchgang: Leerzustand, Kernsatz in der App, „wiederherstellen“.
- ⚠️ #62 Übersetzen: Sprache beim Bearbeiten ändern; Geschwindigkeit auf einem echten Handy messen.
- ✅ Datenschutzerklärung in der App (nennt Internet, Sprachpaket, Mikrofon, Erkennungspaket).

## 4. Veröffentlichung

- ✅ Dependabot eingerichtet, GPL-3.0, Releases mit Prüfsumme und Herkunftsnachweis.
- ⚠️ #16 Hinweis in der App, wenn eine neue Version verfügbar ist – laut Vision nur auf Antippen („Nach neuer Version suchen“); prüfen, ob schon erledigt.
- ⚠️ #74 IzzyOnDroid zu 1.0: vorher APK-Größe klären (#59 App kleiner, nach 1.0).
- ⚠️ #73 Store-Auftritt: Texte, **eigene** Gerichte-Fotos des Projektinhabers (ohne Aufnahmeort, keine erkennbaren Personen), Spendenlink nur im Store-Eintrag; Markenname „Molto Bene“ vorher im Markenregister prüfen lassen.
- ⚠️ #75 Google-Entwicklerverifizierung: Entscheidung, nicht blockierend für 1.0.
- ⚠️ **Prüfrunde vor 1.0** (laut `CLAUDE.md` vor jeder Änderung der ersten Stelle der Versionsnummer, automatisch).

## 5. Vorschlag: was vor 1.0 noch gebaut wird

Reihenfolge nach Nutzen und Aufwand:

1. **Sortieren** (Titel A–Z, zuletzt hinzugefügt) – klein, schließt „Finden und ordnen“.
2. **Texte-Durchgang (#58)** – klein.
3. **Große Schrift beim Kochen und Notiz in der Rezeptansicht (#61)** – gehört zu Kernfunktion 4.
4. **Schlagwörter beim Übernehmen** (nur als Vorschlag) und aus Rezeptdateien einlesen – klein bis mittel.
5. **Echt-Handy-Prüfung** durch den Projektinhaber: Blättern, Vorlesen/TalkBack, Spracheingabe, Kopfhörertasten.
6. Teigwissen von einer Fachperson gegenlesen lassen (oder Hinweis „ohne Gewähr“ belassen).
7. Store-Fotos, Markenprüfung, dann Prüfrunde und 1.0.

**Nicht nötig für 1.0:** PDF (#47), Umzug aus anderen Apps (#76), Fassungen und Vergleichen (#77/#78), Gehzeiten (#71), kleineres Lesemodell (#59), Baseline-Profil.

## 6. Fragen an den Projektinhaber

1. Passt die Reihenfolge in Abschnitt 5, und gehört Punkt 4 (Schlagwörter beim Übernehmen) zu 1.0?
2. Sollen die eigenen Voreinstellungen des Teigrechners vor 1.0 in die Sicherung?
3. Wann sollen die Store-Fotos entstehen, und wer prüft die Marke „Molto Bene“?
4. Baseline-Profil (zusätzliches Gradle-Modul): ja oder erst nach 1.0?
