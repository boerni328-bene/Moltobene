# Planung: Spracheingabe beim Kochen (#80)

Stand 13.10.2026, Entwurf zur Entscheidung durch den Projektinhaber. Grundlage: Besprechung vom 09.10.2026 (`docs/besprechungen/2026-10-09-sprachfunktionen.md`) und die dort genannten Vorgaben. Es entsteht kein Code, bevor die Fragen unten beantwortet sind.

## Ziel

Beim Kochen mit nassen oder mehligen Händen das Rezept per Sprache bedienen: „Weiter“, „Zurück“, „Wiederholen“, „Zutaten“, „Stopp“. Die Antwort kommt als Vorlesen (#79). Alles läuft auf dem Handy, ohne Konto, ohne Cloud, ohne Google-Dienste.

## Bausteine

| Baustein | Vorschlag | Offen / zu prüfen |
|---|---|---|
| Erkennung | **Vosk** (Kaldi, Apache 2.0, läuft vollständig auf dem Handy, funktioniert ohne Google-Dienste). Alternative: sherpa-onnx (nutzt ONNX Runtime; die App hat es schon, aber ein eigenes Gerüst). | Größe der Bibliothek je Prozessor (arm64-v8a, armeabi-v7a) und Auswirkung auf die APK (heute etwa 46 MB) wurde noch nicht gemessen; die Recherche im Netz hat keine belastbaren Zahlen geliefert. Messen auf dem Arbeitszweig. |
| Erkennungspaket | Kleine Modelle Deutsch und Englisch („small“, etwa 40–50 MB je Sprache nach Herstellerangaben), **nur auf Wunsch geladen** wie das Sprachpaket der Übersetzung (Prüfsumme, Workflow, Verwalten und Löschen in den Einstellungen). | Genaue Größe, Lizenz je Modell und Download-Quelle prüfen; das Paket muss wie das Sprachpaket nachvollziehbar und mit festen Prüfsummen gebaut werden. |
| Wortschatz | Feste Liste weniger Befehle je Sprache (Grammatik), alles andere wird als „unbekannt“ verworfen. Das macht die Erkennung in lauter Küche deutlich verlässlicher und verhindert Fehlbefehle. | Genauigkeit mit kleinem Modell und Dunstabzug nicht bekannt; im Rundgang nicht prüfbar (kein echtes Mikrofon), also ehrlich auf dem Handy testen. |
| Mikrofon | Berechtigung `RECORD_AUDIO`, **erst bei der ersten Benutzung** mit Erklärung vor der Systemabfrage. Bei Ablehnung bleibt alles andere nutzbar. | In `ERLAUBT` (Build-Prüfung), Kommentar im Manifest, Datenschutztext und Vision ergänzen. |
| Bedienung | Mikrofon-Knopf in der Kochleiste: Tippen = **eine Sekunde zuhören, ein Befehl**, dann ist das Mikrofon wieder aus. Kein Dauer-Mithören, kein Signalwort, kein Hintergrunddienst. | Siehe Frage 1. |
| Rückmeldung | Erkannter Befehl erscheint kurz in der Kochleiste und wird ausgeführt; nicht Verstandenes: kurze Meldung „Nicht verstanden“. | |
| Lizenz und Hinweise | Vosk und Modelle in `ui/licenses/Licenses.kt` mit Lizenztext; GPL-3.0-verträglich (Apache 2.0). | Lizenz jedes Modells einzeln bestätigen. |

## Etappen

1. **Vorbereitung ohne Berechtigung:** Erkennungspaket bauen und prüfen (Workflow nach Muster `sprachpaket.yml`), Verwaltung unter „Vorlesen“ bzw. neuem Abschnitt „Sprachbefehle“ in den Einstellungen (laden, Größe, löschen). Noch kein Mikrofon.
2. **Befehle:** Vosk einbinden, Mikrofon-Knopf in der Kochleiste, Berechtigung mit Erklärung, die fünf Befehle, Rückmeldung. Unit-Tests für die Zuordnung Text → Befehl in beiden Sprachen; Rundgang prüft den Ablauf mit nachgestellter Erkennung.
3. **Feinschliff nach dem Test auf dem Handy:** Wortschatz erweitern („Wie viel Mehl?“ nur mit Absprache), Hinweise bei schlechter Erkennung.

Jede Etappe ist eine eigene Version; Etappe 1 verändert für Nutzer nur die Einstellungen.

## Ehrliche Grenzen

- Genauigkeit in einer lauten Küche mit kleinem Modell ist nicht gemessen; Tasten und Kopfhörertasten bleiben der gleichwertige Weg.
- Die App wird um die Bibliothek größer; die Größe steht erst nach dem ersten Bau fest.
- Der Rundgang auf dem Emulator hat kein echtes Mikrofon; geprüft werden Ablauf und Zuordnung, nicht die Erkennung selbst.

## Ergebnis der Erkundung der Modelle (14.10.2026, GitHub-Lauf „Erkennungspaket“ Nr. 1)

Gemessen auf GitHub, nicht aus dem Gedächtnis (Quelle: `https://alphacephei.com/vosk/models/`, kleine Modelle):

| | Deutsch `vosk-model-small-de-0.15` | Englisch `vosk-model-small-en-us-0.15` |
|---|---|---|
| Download (Zip) | 46.499.967 Byte (≈ 46 MB) | 41.205.931 Byte (≈ 41 MB) |
| Entpackt | ≈ 92 MB | ≈ 68 MB |
| SHA-256 der Zip-Datei | `b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43` | `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498` |
| Lizenz im Paket | `COPYING` = Apache License 2.0 | keine Lizenzdatei im Paket; README nennt nur „Copyright 2020 Alpha Cephei Inc“ |
| Genauigkeit laut README | Wortfehlerrate 9–11 % (Test, sauberes Sprechen), 27–31 % (Podcast) | 10,4 % (TEDLIUM), 9,9 % (LibriSpeech) |

Offene Punkte:
- Die Apache-Lizenz des englischen Modells ist nur die Angabe des Herstellers auf der Webseite, im Paket steht sie nicht. Vor dem Veröffentlichen prüfen oder beim Hersteller nachfragen.
- Die Wortfehlerraten gelten für freies Sprechen. Mit einer auf wenige Befehle begrenzten Erkennung ist mit deutlich besseren Werten zu rechnen; das ist aber nicht gemessen und muss auf dem Handy geprüft werden.
- Der Speicher auf dem Handy liegt mit 92 MB (Deutsch) und 68 MB (Englisch) über der ersten Schätzung von 40–50 MB; der Download ist etwa so groß wie geschätzt.
- Die App selbst wächst durch die Vosk-Bibliothek (native Teile für `arm64-v8a` und `armeabi-v7a`); die Größe ist noch nicht gemessen.
