# Besprechung vom 23.10.2026: Tablet-Version und evtl. Windows-Version

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen, Marketing, Sprache
**Moderation:** Claude · **App-Version:** 0.58.0

## Tagesordnung
Wunsch des Projektinhabers: nach Version 0.58.0 an eine Tablet-Version und evtl. eine Windows-Version denken.
1. Wie geht die App heute mit Tablets, Falt-Handys und Chromebooks um, und was ist der kleinste sinnvolle Schritt?
2. Ist eine Windows-Version sinnvoll und machbar?
3. Passt beides zur Vision (Kernsatz „nur auf dem eigenen Handy“, fester Begriff „Handy“) und zum Weg zu 1.0?

## Lagebericht
- Version 0.58.0 veröffentlicht. Das Manifest sperrt Ausrichtung und Fenstergröße nicht, die App läuft also schon auf Tablets, Falt-Handys und Chromebooks, wird dort aber nur gestreckt.
- Im Code gibt es keine Breitenlogik (kein `WindowSizeClass`, keine Begrenzung der Inhaltsbreite). Die Kartenliste im Kochbuch nutzt schon ein adaptives Raster. Der Rundgang testet nur 360 × 760 dp.
- Zu Tablet oder Windows gibt es kein offenes Issue. Offen für 1.0: #73 Store, #52, #62, Prüfrunde.
- Windows 11 führt Android-Apps seit 2025 nicht mehr aus. Eine Windows-Version bräuchte Compose Multiplatform Desktop; Room, ONNX, Vosk, Fotos, Teilen und Mikrofon wären neu anzubinden.

Hinweis zum Ablauf: Die Aussprache (Runde 2) entfiel, weil alle zehn Beiträge in den Kernpunkten übereinstimmen. Die Unterschiede stehen unter „Streitpunkte“.

## Beschlussvorschläge (gemeinsame Rangfolge)
| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Tablet-Grundschritt:** Inhaltsbreite begrenzen (etwa 600–840 dp, zentriert) als gemeinsamer Baustein in `ui/components`; Foto-Karten im Kochbuch mit adaptivem Raster; Vorschaugröße zu den Karten prüfen | alle zehn (D1, F1, K1, N1, P1, M1, Ü1, T3, S2, V3) | alle | mittel | klein | #73 |
| 2 | **Rundgang erweitern:** Tablet quer/hoch (z. B. 800 × 1280 dp) und Handy quer (760 × 360 dp) in `DisplayVariant`; Bildschirmfotos auf zu lange Zeilen prüfen | D2, F1, P1, T3, Ü1, M1 | alle | mittel | klein | Rundgang |
| 3 | **Drehen und Falten absichern:** laufendes Vorlesen, Spracheingabe und Aufnehmen dürfen beim Drehen nicht unbemerkt weiterlaufen; „Mikrofon an“ immer sichtbar; Eingaben im `SavedStateHandle` testen | V1, P2, F1, D2 | S2, P | mittel | klein | #79, #80 |
| 4 | **Feste Höhenannahme in `AreaSelectionScreen.kt:123`** (`screenHeightDp`) durch echte Layout-Maße ersetzen (Querformat, Mehrfenster) | F3 | P | niedrig | klein | |
| 5 | **Sicherungsdatei als Hauptweg zwischen Geräten:** Hinweis „kein automatischer Abgleich“ (kein Wort „Synchronisieren“) und Hinweis, dass die Datei unverschlüsselt ist, jeweils als `NoticeCard` | T2, S3 | N2, Ü3 | mittel | klein | |
| 6 | **Begriff „Handy“ vorerst unverändert**; Android-Tablets sind mitgemeint. In neuen Texten „Handy“ meiden, wo es ohne Verlust geht. Umstellung (z. B. „Gerät“) nur gemeinsam mit einer Windows-Entscheidung | T1, Ü2, M3, K3 | D3, V, S1 | niedrig | klein | CLAUDE.md |
| 7 | **Windows nicht vor 1.0.** Danach nur bei echter Nachfrage als eigene Besprechung. Günstige Zwischenlösung: reiner Betrachter für Sicherungsdatei oder schema.org-Rezepte (lokal, ohne Hochladen) oder eine README-Anleitung | D3, K2, F2, M2, N, Ü3, S1, P3, V2 | alle | niedrig | groß | Vision |
| 8 | **Sicherungsformat plattformneutral halten** (keine Android-Pfade oder -URIs), alte Sicherungen bleiben lesbar | N2 | Ü3, D3 | niedrig | klein | `BackupFormat.kt` |
| 9 | **Manifest-Prüfung beim Tablet-Schritt:** exportierte Komponenten und Intent-Filter (auch Ziehen und Ablegen) nehmen nicht mehr an als heute; keine neue Berechtigung | S2 | | niedrig | klein | `build.yml` (`ERLAUBT`) |
| 10 | **Zwei-Spalten-Ansicht** (Liste und Rezept nebeneinander, `WindowSizeClass`) erst nach 1.0 | D2, F1, N1 | P1 | niedrig | mittel–groß | |
| 11 | **Store-Eintrag (#73):** Satz „läuft auch auf Tablets“ und 1–2 Tablet-Bildschirmfotos, sobald Rang 1–2 stimmen | M3, K3 | | niedrig | klein | #73 |

## Einigkeit
- Tablet ist ein kleiner Schritt und passt vor 1.0: Breite begrenzen, Raster, Rundgang mit Tablet-Größen. Ohne neue Bibliothek, ohne neue Berechtigung, ohne neue Texte.
- Windows gehört frühestens nach 1.0, nur bei Nachfrage und als eigenes Vorhaben. Gründe: zweite Plattform und zweiter Bauweg, Android-gebundene Bausteine, kein Android Studio, Fokus „lieber eine Funktion fertig“.
- Der Austausch läuft nur über die Sicherungsdatei, ohne Cloud und ohne Konto. Das Format ist offen und bleibt es.
- Der Kernsatz bleibt unverändert, solange es nur Android gibt.

## Streitpunkte – Entscheidung nötig
1. **Umfang des ersten Tablet-Schritts.** Nur Breite begrenzen und Raster (Design, Konnektivität, Performance, Funktionalität) oder zusätzlich die Rezeptansicht ab etwa 600 dp zweispaltig (Zutaten links, Zubereitung rechts; Kreativität, „klein bis mittel“). Die Zweispaltigkeit hilft am meisten beim Kochen mit dem Tablet in der Küche, ändert aber die Rezeptansicht, die gerade erst mit Reitern und Wischen (0.58.0) neu gestaltet wurde.
2. **Verschlüsselte Sicherung** (Sicherheit, optionales Passwort, neue Formatversion, mittlerer Aufwand) gegenüber nur einem Hinweis (Rang 5). Wurde nicht von anderen Teilnehmern aufgegriffen.
3. **Wann das Wort „Handy“ geklärt wird.** Jetzt (Texte: kostet wenig, verhindert spätere Umbenennung von 27 Texten) oder erst bei einer Windows-Entscheidung (Übersetzungen, Marketing, Kreativität).

## Antworten des Projektinhabers (23.10.2026)
- **Tablets werden offiziell unterstützt.**
- **Streitpunkt 1 entschieden:** Auf dem Tablet sollen Zutaten und Zubereitung gleichzeitig auf einem Bildschirm sichtbar sein (zweispaltige Rezeptansicht), nicht nur eine begrenzte Breite.
- **Windows ist erwünscht:** Rezepte mit Maus und Tastatur bearbeiten, und am PC Rezepte recherchieren und importieren. Damit ist ein reiner Betrachter zu wenig; der Wunsch geht in Richtung einer vollwertigen Fassung. Vorgehen und Zeitpunkt sind noch zu klären (eigene Besprechung zu Windows, Vision und Begriff „Handy“ danach anpassen).
- **Windows kommt erst am Ende** (nach 1.0 und nach dem Tablet), kein Vorhaben für jetzt.
- **Streitpunkt 2 entschieden:** keine Verschlüsselung der Sicherungsdatei.
- **Streitpunkt 3 entschieden:** „Handy“ wird durch „lokale Speicherung“ o. ä. ersetzt; genauer Wortlaut des Kernsatzes und der festen Begriffe noch festzulegen.
- **Kernsatz neu:** „… an einem Ort, lokal gespeichert, ohne Cloud.“ (statt „gespeichert nur auf dem eigenen Handy“); der feste Begriff „Handy“ entfällt, Umsetzung in CLAUDE.md, README, Texten und Store-Eintrag steht noch aus.
- Ein Testtablet ist vorhanden. Sprache (Vorlesen, Spracheingabe, Aufnehmen) gilt auf Tablets ausdrücklich mit.
- **Wiederherstellen der Sicherung (noch auszuarbeiten):** Auswahl, ob nur ältere Rezepte ersetzt, fehlende ergänzt werden usw.; bei doppelten oder ähnlichen Rezepten einzeln bestätigen.

## Angelegte Issues
Rang 1 und 2: #91 · Rang 3: #92 · Rang 4: #93 · Rang 5: #94 (ohne Verschlüsselung). Rang 6 (Begriff „Handy“) ist entschieden, aber noch nicht als Issue; Rang 7–11 nicht angelegt.

## Offene Fragen an den Projektinhaber (Stand der Besprechung)
- Soll die App Tablets offiziell unterstützen (Vision „Handy oder Tablet“) oder nur still mitlaufen?
- Steht ein Tablet oder Falt-Handy zum Testen zur Verfügung? Tonqualität und Stimme (Vorlesen, Spracheingabe) lassen sich nur auf echten Geräten prüfen.
- Gibt es einen konkreten Anlass für Windows (eigener PC in der Küche, Erfassen von Rezepten am PC)? Davon hängt ab, ob ein Betrachter genügt.
- Soll die Sprache (Vorlesen, Spracheingabe) auf Tablets ausdrücklich mitgehen?

## Zurückgezogen / bereits als Issue vorhanden
Nichts zurückgezogen. Kein bestehendes Issue betrifft Tablet oder Windows. Bezug nur zu #73 (Store), #79 und #80 (Sprache).

## Anhang: Beiträge
- **Design:** Breite begrenzen und Raster (D1); Zwei-Spalten-Ansicht nach 1.0 (D2); Windows jetzt nicht, höchstens schreibgeschützter Betrachter (D3).
- **Funktionalität:** Rundgang mit Quer- und Tablet-Größe, dann Breite begrenzen (F1); Windows nicht jetzt (F2); feste Höhe in `AreaSelectionScreen` (F3).
- **Sicherheit:** Windows ohne App-Sandbox, Signatur und zweite Lieferkette (S1); Manifest unverändert halten (S2); Sicherungsdatei als Hauptweg, Hinweis oder Passwort (S3).
- **Kreativität:** Breite begrenzen, Kochansicht oder Rezeptansicht zweispaltig (K1); Windows erst nach 1.0, Betrachter (K2); Tablets still mitmeinen (K3).
- **Konnektivität:** Tablet-Grundlage (N1); Sicherungsformat plattformneutral (N2); Windows nicht vor 1.0.
- **Performance:** Breite begrenzen, Vorschaugröße prüfen (P1); Drehen und Größenwechsel ohne Verlust (P2); Windows nach 1.0 (P3).
- **Texte:** Begriff „Handy“ klären (T1); „kein automatischer Abgleich“ erklären (T2); lange Zeilen im Tablet-Rundgang (T3).
- **Übersetzungen:** Tablet-Darstellung ohne neue Texte (Ü1); „Handy“ vor Windows klären (Ü2); Windows braucht anderes Ressourcenformat (Ü3).
- **Marketing:** Tablet nur absichern (M1); Windows nur bei Nachfrage nach 1.0 (M2); Kernsatz unverändert, Store-Satz (M3). Aussagen ohne neue Websuche, unbelegt.
- **Sprache:** Mikrofon- und Vorlesezustand beim Drehen sichern (V1); Windows zunächst ohne Sprachfunktionen (V2); erst Android-Tablets (V3).
