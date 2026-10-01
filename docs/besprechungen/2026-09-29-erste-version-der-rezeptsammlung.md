# Besprechung vom 29.09.2026: Erste Version der Rezeptsammlung

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.2.2

## Tagesordnung

1. Welcher Funktionsumfang gehört in die erste nutzbare Version (0.3.0), und in welcher Reihenfolge folgen die weiteren Versionen?
2. Wie ist ein Rezept aufgebaut (Datenmodell)? Vor allem: Zutaten als Freitext oder strukturiert? Kategorien oder Schlagwörter? Wie werden Fotos, Quelle, Notizen und Portionen gespeichert?
3. Welche Grundlagen müssen von Anfang an stehen (Speicher, Navigation, Bibliotheken, Sicherung, Build-Prüfungen), und welche offenen Issues gehören vor 0.3.0?

## Lagebericht

- Version 0.2.2 ist ein leeres Grundgerüst. R8 ist eingeschaltet, die APK ist 0,7 MB groß.
- Die Vision steht seit heute fest (`CLAUDE.md`): eine Rezeptsammlung, bei der alles lokal auf dem Handy bleibt. Sie hat 5 Kernfunktionen.
- Entscheidungen aus der letzten Besprechung:
  - Die Android-Cloud-Sicherung wird abgeschaltet (#12, noch umzusetzen).
  - Englisch wird Rückfallsprache, Deutsch kommt dazu (#24).
  - Die Update-Suche gibt es nur auf Knopfdruck (#16).
  - Die Texterkennung wird ohne Google-Dienste eingebaut.
- Offene Fachfrage: Werden Zutaten als Freitext oder strukturiert gespeichert?
- Protokoll der Vorbesprechung: `2026-09-29-vision-der-app.md`

## Beschlussvorschläge (gemeinsame Rangfolge)

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Datenmodell „Originalzeile + optional erkannte Menge“** – siehe unten | F1, K2, N1, Ü1, D3, P1, Ü2 | alle 8 (8× Top 3) | hoch | mittel | **Issue #28** |
| 2 | **Sichern und Wiederherstellen schon in 0.3.0**, zusammen mit dem Abschalten der Cloud-Sicherung (#12). Die Sicherung ist eine ZIP-Datei mit `manifest.json` (Formatversion), `recipes.json` und `photos/`. Den Speicherort wählt man über den Android-Dateidialog, ohne Berechtigung. Die Datei wird stückweise geschrieben. Vor dem Wiederherstellen zeigt die App eine Vorschau („12 Rezepte, 3 schon vorhanden“), und es gilt „alles oder nichts“. Die App prüft Dateipfade und Größen und weist darauf hin: „Die Sicherungsdatei ist nicht verschlüsselt.“ Eine Beispiel-Sicherung dient als Testfall. | N2, S2, S3 | Sicherheit, Texte, Kreativität, Konnektivität, Funktionalität, Performance, Design (6× Top 3) | hoch | mittel | **Issue #29**, #12, #6 |
| 3 | **Technische Grundlage:** Room-Datenbank mit KSP, gespeicherten Bauplänen und Migrationstests. Volltextsuche von Anfang an. Aufteilung in Oberfläche, ViewModel und Repository. Navigation Compose. Alle Zugriffe laufen im Hintergrund. Halb getippte Eingaben gehen bei Unterbrechungen nicht verloren. | F2, P1 | Funktionalität, Konnektivität, Performance | hoch | mittel | **Issue #30**, #6, #20 |
| 4 | **Zentrale Foto-Verarbeitung ohne Berechtigungen:** Fotos kommen über die Android-Fotoauswahl oder die Kamera-App ins Rezept. Beim Speichern entstehen ein Detailbild (~1600 px) und ein Vorschaubild (~350 px), der Aufnahmeort wird entfernt. Die Liste lädt nur Vorschaubilder. Rezepte ohne Foto bekommen einen gestalteten Platzhalter. Zum Anzeigen dient die Bildbibliothek Coil, ohne ihr Internet-Modul. | P2, S1, D2 | Sicherheit, Performance | hoch | mittel | **Issue #31** |
| 5 | **Feste Begriffe und Kennungen vor dem ersten Bildschirm:** Titel, Zutaten, Zubereitung (Einträge heißen „Schritt“), Portionen, Quelle, Notizen, Foto, Entwurf, Schlagwort, Favorit, jeweils mit englischer Entsprechung. Mitgelieferte Schlagwörter bekommen eine feste Kennung (z. B. `main_course`), beim Teilen und Sichern steht zusätzlich der lesbare Text dabei. Zählwörter werden von Anfang an als Mehrzahlformen angelegt. | T1, Ü2, Ü3 | Texte, Übersetzungen, Kreativität | hoch | klein | **Issue #32**, #22, #24, #25, #26 |
| 6 | **Bildschirme und Texte für 0.3.0:** drei Bildschirme – Sammlung (Liste, runder Knopf „Rezept hinzufügen“, Leerzustand „Noch keine Rezepte vorhanden“), Rezeptansicht (Foto oben, Bildschirm bleibt an) und Bearbeiten. Keine untere Leiste, Einstellungen im Menü oben rechts. Löschen mit „Löschen“/„Abbrechen“, dazu „Rezept gespeichert“. Hinweis im Zutatenfeld: „Eine Zutat pro Zeile, z. B. 200 g Mehl“. | D1, T2, T3, K1 | Design | hoch | mittel | **Issue #33**, #4 |
| 7 | **Design-Grundlage vor dem ersten Bildschirm:** eigene ruhige Farbpalette für Hell und Dunkel statt der Farben vom Hintergrundbild, das weiße Aufblitzen beheben, große Schrift und Screenreader als Regel für jeden Bildschirm. | D2 | Design | hoch | mittel | **Issue #34**, #1, #2, #4 |
| 8 | **Ein gemeinsamer Entwurfs-Editor** für alle künftigen Erfassungswege. Rezepte haben den Status „Entwurf“ mit dem Hinweis „Übernommen aus [Quelle] – Angaben bitte prüfen“. Bei Übernahmen werden der Originaltext bzw. der Link mitgespeichert. Das Aufteilen von eingefügtem Text in Zeilen kommt erst mit 0.5.0. | K3, T3, N3 | Kreativität, Funktionalität, Konnektivität, Design | mittel | klein | **Issue #35** |
| 9 | **Testsammlung mit 1.000 Beispielrezepten** in der Debug-Version (nicht in der veröffentlichten APK), um das Leistungsziel zu prüfen. Das Baseline-Profil kommt später. | P3 | – | niedrig | klein | #6 |

### Datenmodell (Rang 1) im Detail

- **Rezept:** UUID (weltweit eindeutige Kennung, damit beim Wiederherstellen keine Doppelten entstehen), Titel (einziges Pflichtfeld), Sprache des Rezepts (`inLanguage`), Portionen (Zahl + freie Angabe wie „1 Springform 26 cm“), Quelle (Art: Internetseite/Buch/Person/eigenes, dazu Link bzw. Name und Seite; Links öffnet die App nur mit http/https), Notizen (getrennt vom Original), Schlagwörter, Favorit, Status Entwurf, optional Zubereitungs- und Gesamtzeit (als Zahl), Zeitpunkte „angelegt“ und „geändert“, Formatversion.
- **Zutaten:** geordnete Liste von Zeilen, wahlweise mit Zwischenüberschrift („Für den Teig“). Jede Zeile wird **unverändert als Originaltext** gespeichert. Dazu gibt es Zusatzfelder für die erkannte Menge und die Einheit (Originaltext, keine feste Liste). Diese Felder existieren ab 0.3.0, bleiben aber leer. Die Erkennung kommt mit dem Kochmodus. Eingegeben wird in einem großen mehrzeiligen Feld, das die App beim Speichern in Zeilen aufteilt. Die Zusatzfelder sind im Formular nicht sichtbar.
- **Schritte:** geordnete Liste (damit man sie später abhaken kann).
- **Fotos:** als Dateien im privaten App-Speicher, in der Datenbank steht nur der Dateiname. Das Modell erlaubt mehrere Fotos, die Oberfläche zeigt in 0.3.0 eines.
- **Ordnen:** nur Schlagwörter (keine zweite Ebene „Kategorie“). Mitgelieferte Vorschläge haben feste Kennungen, eigene Schlagwörter sind freier Text.
- Das Modell folgt so weit wie möglich **schema.org/Recipe**, damit Übernehmen, Teilen und Sichern dieselbe Sprache sprechen.

### Etappenplan (Einigkeit nach der Aussprache)

| Version | Inhalt |
|---|---|
| **0.3.0 „Eigene Sammlung“** | Rezept hinzufügen, bearbeiten und löschen; Liste mit Vorschaubild; Titelsuche; Rezeptansicht mit Foto, Bildschirm bleibt an; **Sichern und Wiederherstellen**; Cloud-Sicherung aus (#12); ohne jede Berechtigung. |
| 0.4.0 „Teilen“ | Einzelne Rezepte teilen (schema.org/Recipe bzw. Text). |
| 0.5.0 „Übernehmen, Stufe 1“ | Internetseiten und Koch-Portale, geteilter Text, Zwischenablage. Erst ab hier gibt es die Internet-Berechtigung; der geteilte Link wird sofort als Entwurf gespeichert. Update-Suche (#16) frühestens jetzt. |
| 0.6.0 „Kochen“ | Zutaten und Schritte abhaken, Portionen umrechnen (Mengenerkennung), eigene Notizen beim Kochen. |
| 0.7.0 „Finden und ordnen“ | Schlagwörter filtern, sortieren, nach Zutaten suchen, Favoriten. |
| 0.8.0 „Übernehmen, Stufe 2“ | Texterkennung aus Foto, Bildschirmfoto und PDF (App wird dadurch 5–10 MB größer, wird gesondert bewertet). |
| bis 1.0 | YouTube, Dateien und andere Rezept-Apps. |

## Einigkeit

- **Zutatenfrage entschieden:** Jede Zeile wird als Originaltext gespeichert, die Menge wird zusätzlich erkannt. Das war die einhellige Meinung aller acht.
- **Ordnen nur über Schlagwörter**, keine festen Kategorien.
- **Sichern kommt in 0.3.0**, gleichzeitig mit dem Abschalten der Cloud-Sicherung. Sonst gäbe es eine Version ganz ohne Datensicherung. Funktionalität und Kreativität haben ihre ursprüngliche Einordnung in 0.4.0 dafür zurückgezogen.
- **Kochansicht in 0.3.0:** nur „Bildschirm bleibt an“, kein eigener Kochbildschirm.
- **Reihenfolge ab 0.5.0:** erst Übernehmen, dann Kochen, dann Finden und Ordnen, zuletzt die Texterkennung.
- **Keine Berechtigungen in 0.3.0.** Internet erst mit dem Übernehmen (0.5.0).
- **Rückfrage und automatisches Speichern passen zusammen:** Bei Unterbrechungen (Anruf, Drehen, App wird beendet) speichert die App still. Die Rückfrage „Änderungen verwerfen?“ erscheint nur, wenn man selbst auf „Zurück“ tippt.
- **Vor 0.3.0 zu erledigen (breite Zustimmung):** #12 (in der Form: ab Android 12 Cloud aus und Handy-zu-Handy-Umzug erlaubt, Android 8–11 Cloud aus), #6 (Build-Prüfungen und Tests), #1 (weißes Aufblitzen), #24 und #25 (Sprachen, App-Name), #22 (erledigt durch Rang 5).
- **#18** ist durch die Vision inhaltlich erledigt und kann geschlossen werden.

## Streitpunkte – Entscheidung nötig

1. **Wann werden #8 und #11 erledigt (Absicherung des Build-Ablaufs)?**
   - *Sicherheit:* vor 0.3.0, weil ab dann jede ausgelieferte APK echte Nutzerdaten betrifft.
   - *Kreativität, Konnektivität, Funktionalität:* vor der ersten Verbreitung an andere Nutzer, aber nicht als Bremse für 0.3.0.
2. **Wie viel vom eigenen Farbkonzept (#2) muss vor dem ersten Bildschirm stehen?**
   - *Design:* die vollständige eigene Palette vor dem ersten Bildschirm, sonst müssen alle Kontraste doppelt geprüft werden.
   - *Kreativität / Funktionalität:* vorerst eine schlichte eigene Palette, der Feinschliff folgt parallel oder später.
3. **Freigabe neuer Bibliotheken** (laut `CLAUDE.md` muss der Projektinhaber zustimmen):
   - **Room** mit **KSP**: Datenbank mit Suche
   - **Navigation Compose**: Wechsel zwischen den Bildschirmen
   - **Coil**: Fotos anzeigen, ohne Internet-Modul
   - **kotlinx.serialization**: Sicherungsdatei und Teilen

   Alle vier stammen von Google bzw. JetBrains oder sind etablierte, quelloffene Standardbibliotheken. Keine davon sendet Daten ins Internet.

## Offene Fragen an den Projektinhaber

Alle Fragen wurden im Anschluss an die Besprechung beantwortet.

### Entscheidungen des Projektinhabers

- **Streitpunkt 1:** #8 und #11 werden **vor 0.3.0** erledigt.
- **Streitpunkt 2:** Das Farbkonzept (#2) wird **vollständig vor dem ersten Bildschirm** festgelegt.
- **Streitpunkt 3:** Alle vier Bibliotheken sind **freigegeben**: Room mit KSP, Navigation Compose, Coil ohne Internet-Modul und kotlinx.serialization.
- **Issues:** Rang 1–8 werden angelegt (#28–#35), Rang 9 nicht.

## Zurückgezogen / bereits als Issue vorhanden

- **Zurückgezogen oder abgeschwächt:**
  - F3 und K1: Sichern kommt in 0.3.0 statt in 0.4.0.
  - K1: Die Mini-Kochansicht beschränkt sich auf „Bildschirm bleibt an“.
  - K3: Das Aufteilen von eingefügtem Text wandert nach 0.5.0.
  - P3: auf niedrige Priorität herabgestuft.
  - S3: gilt für 0.3.0 nur mit zwei Punkten (Links nur http/https, Formatversion).
  - T3: bis auf den Hinweis im Zutatenfeld erst ab 0.4.0.
  - Ü3: „Build stoppt bei fehlender Übersetzung“ kommt erst mit #6.
  - N2: Sichern und #12 kommen immer in derselben Version.
- **Verwiesen auf:** #1, #2, #3, #4, #6, #8, #11, #12, #16, #17, #18, #20, #22, #24, #25, #26, #27.

## Anhang: Beiträge und Stellungnahmen

**Design:**
- Beiträge:
  - D1: drei Bildschirme ohne untere Leiste
  - D2: eigene Palette, #1 und #4 vor dem ersten Bildschirm, Platzhalter ohne Foto
  - D3: Zutaten als Freitextzeilen mit Zwischenüberschriften, nur Schlagwörter
- Aussprache: Mengenfelder im Formular unsichtbar; nur ein Foto in der Oberfläche; Sichern schlicht in 0.3.0; Kochen vor Finden. Top 3: Datenmodell, D1 mit T1/T2, D2.

**Funktionalität:**
- Beiträge:
  - F1: Originalzeile plus erkannte Menge, UUID, vollständige Feldliste
  - F2: Room mit Migrationstests, Architektur, keine verlorenen Eingaben
  - F3: Etappenplan
- Aussprache: Kompromiss „still speichern bei Unterbrechung, Rückfrage bei Zurück“; Sichern in 0.3.0; Pflicht vor 0.3.0: #6, #12, #24, #25, #1. Top 3: Datenmodell, F2 mit P1 und #6, N2.

**Sicherheit:**
- Beiträge:
  - S1: 0.3.0 ohne Berechtigungen, EXIF beim Speichern entfernen
  - S2: #12 mit getrennten Regeln für Cloud und Umzug
  - S3: Fremdinhalte als unsicher behandeln
- Aussprache: Coil ohne Internet-Modul; Wiederherstellen nur mit Prüfungen; #8 und #11 vor 0.3.0. Top 3: N2+S2, S1+P2, Datenmodell.

**Kreativität:**
- Beiträge:
  - K1: Etappenplan mit Mini-Kochansicht
  - K2: Datenmodell mit Textzeilen und automatischer Mengenerkennung, Favoriten
  - K3: gemeinsamer Entwurfs-Editor
- Aussprache: eigene Reihenfolge zugunsten von N2 zurückgezogen, Kochansicht nur „Bildschirm an“, Zeilen-Aufteilung nach 0.5.0, #8 und #11 vor der Verbreitung. Top 3: N2+S2, Datenmodell, K3+F2.

**Konnektivität:**
- Beiträge:
  - N1: Datenmodell nach schema.org/Recipe
  - N2: Sichern in 0.3.0 mit ZIP-Format
  - N3: Internet erst mit Übernehmen, Entwurf sofort speichern, #18 schließen
- Aussprache: Kennungen nur intern, beim Teilen lesbarer Text in „keywords“; Sichern immer gekoppelt an #12; #6 vor 0.3.0. Top 3: N2+S2, Datenmodell, F2.

**Performance:**
- Beiträge:
  - P1: Room mit Volltextsuche, Fotos als Dateien
  - P2: zentrale Foto-Verarbeitung und Coil
  - P3: Testsammlung mit 1.000 Rezepten und Baseline-Profil
- Aussprache: Sicherung stückweise schreiben; Volltextsuche ab 0.3.0 im Modell; Texterkennung zuletzt; #1 und #20 vor 0.3.0. Top 3: Datenmodell mit Room/FTS, Foto-Verarbeitung, N2.

**Texte:**
- Beiträge:
  - T1: feste Begriffe für die Rezeptfelder (erledigt #22)
  - T2: Texte für Leerzustand, Löschen und Verwerfen
  - T3: Hinweise in Eingabefeldern und einheitlicher Prüfhinweis
- Aussprache: „Favorit“ und „Entwurf“ in die Begriffsliste; eindeutiger Text vor dem Wiederherstellen; T3 größtenteils später. Top 3: N2, T1+Ü2, Datenmodell.

**Übersetzungen:**
- Beiträge:
  - Ü1: Originalzeile plus Sprache des Rezepts
  - Ü2: feste Kennungen für mitgelieferte Schlagwörter
  - Ü3: #24 bis #26 mit 0.3.0, Mehrzahlformen
- Aussprache: Einheit nur als Originaltext; Sprachpakete für die Texterkennung beachten; Build-Stopp erst mit #6. Top 3: Datenmodell, T1, Ü2.

## Nachtrag vom 01.10.2026

Auf Wunsch des Projektinhabers kommt die Texterkennung aus Fotos vorgezogen als **0.5.0** (Tesseract, Sprachen Deutsch, Englisch, Italienisch, Französisch, Spanisch; Einstiege beim Bearbeiten, in der Sammlung und für mehrere Seiten). Das Übernehmen von Internetseiten, geteiltem Text und Zwischenablage samt Internet-Berechtigung und „Nach neuer Version suchen“ (#16) folgt danach; die weiteren Etappen verschieben sich entsprechend.
