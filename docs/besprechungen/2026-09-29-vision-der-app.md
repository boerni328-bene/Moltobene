# Besprechung vom 29.09.2026: Vision der App

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen
**Moderation:** Claude · **App-Version:** 0.2.1

## Tagesordnung

Thema: Vision der App. Leitfragen:

1. Welche Fragen muss der Projektinhaber beantworten, damit eine tragfähige Vision entsteht (Zweck, Zielgruppe, Kernfunktionen, Abgrenzung)?
2. Welche Grundsätze aus den einzelnen Fachbereichen gehören von Anfang an in die Vision?
3. Welche Weichen müssen vor der ersten echten Funktion gestellt werden, und was kann warten?

## Lagebericht

- Moltobene 0.2.1 ist ein leeres Grundgerüst: eine Startseite mit App-Name und Versionsnummer.
- Die Projektbeschreibung in `CLAUDE.md` ist noch ein Platzhalter. Zweck, Zielgruppe und Funktionen sind unbekannt.
- Die App braucht heute keine Berechtigungen, auch keinen Internetzugriff. Verteilt wird sie als signierte APK über GitHub Releases, nicht über den Play Store.
- Aus der ersten Prüfrunde gibt es 27 offene Issues. Für dieses Thema sind vor allem wichtig: #15 (Vision festhalten, hoch), #14 (Datenschutz-Grundsätze), #18 (Offline/Datenaustausch), #12 (Datensicherung), #22 (Textregeln), #24 (Standardsprache), #2 (Markenfarben), #16 (Update-Hinweis).
- Dies ist die erste Besprechung, frühere Beschlüsse gibt es nicht.

## Beschlussvorschläge (gemeinsame Rangfolge)

| Rang | Punkt | Von | Unterstützt von | Priorität | Aufwand | Bezug |
|---|---|---|---|---|---|---|
| 1 | **Ein gemeinsamer Fragebogen** mit höchstens 12 Alltagsfragen ohne Fachwörter und mit Antwortbeispielen, geführt als Gespräch. Er ersetzt die über 35 Einzelfragen aller Fachbereiche. Folgefragen (z. B. Verschlüsselung, Synchronisation, Tablet) werden nur gestellt, wenn eine Antwort sie auslöst. Entwurf siehe unten. | Kreativität (K1) + D1, F1, S1, N1, P1, T1, Ü1 | alle 8 (8× Top 3) | hoch | klein | #15 |
| 2 | **Feste Gliederung der Vision:** Kernsatz (höchstens 15 Wörter, „Moltobene hilft [wem] dabei, [was] zu tun.“) – Für wen – 3 bis 5 Kernfunktionen, jeweils mit einem prüfbaren „Muss funktionieren“-Satz und festem Namen – Bewusst nicht – Grundsätze. Dazu Regeln gegen Überladen: Jede neue Funktion muss zu einer Kernfunktion passen, und lieber eine Funktion ganz fertig als mehrere halb. | Kreativität (K2) + T2, F3, T3 | alle 8 (8× Top 3) | hoch | klein | #15, #23, #6 |
| 3 | **Datenblock als Pflichtteil des Fragebogens:** Was wird gespeichert? Ist etwas Persönliches dabei? Wie schlimm wäre ein Verlust? Soll es aufs neue Handy oder zu anderen Personen? Muss die App ohne Internet laufen? Davon hängen Speicherart, Datensicherung, Verschlüsselung und Internetbedarf ab. | Funktionalität (F1), Sicherheit (S1), Konnektivität (N1) | 7× Top 3 | hoch | klein | #15, #18, #12 |
| 4 | **Grundsatz „Lokal zuerst“:** Daten bleiben auf dem Gerät, die App funktioniert ohne Internet. Kein eigener Server, keine Werbung, keine Analyse- oder Tracking-Werkzeuge. Ausnahmen nur nach ausdrücklicher Entscheidung des Projektinhabers, jeweils mit Begründung in `CLAUDE.md` festgehalten. Datenaustausch über das Teilen-Menü bzw. Export/Import-Datei. | Sicherheit (S2), Konnektivität (N2) | 5× Top 3 (D, K, N, S, P) | mittel | klein | #14, #18 |
| 5 | **Technische Grundsätze („Bauregeln“) in `CLAUDE.md`:** Diesen Abschnitt schreibt Claude selbst, der Projektinhaber muss dafür nichts beantworten. Inhalt: Aufteilung Oberfläche / ViewModel / Repository; nichts Langsames auf dem Hauptthread; schneller Start; Hintergrundarbeit nur mit Begründung; jede Bibliothek muss sich rechtfertigen. Außerdem Material 3, Hell- und Dunkelmodus gleichwertig, ausreichende Kontraste, große Tippflächen, Schrift bis 200 %, Screenreader-Beschreibungen, Lade-, Leer- und Fehlerzustand für jeden Bildschirm. Alle Texte kommen aus `strings.xml` (mit Mehrzahlformen), Datum und Zahlen im Systemformat. | Design (D2), Funktionalität (F2), Performance (P2), Übersetzungen (Ü2) | Design, Kreativität, Performance, Übersetzungen, Texte | mittel | klein | #4, #26 |
| 6 | **Liste „Jetzt / Später“** (nur Protokoll, kein eigenes Issue): siehe Abschnitt „Einigkeit“. | K3, D3, S3, N3, P3, Ü3 | alle (zusammengelegt) | – | – | – |

### Entwurf des gemeinsamen Fragebogens (zu Rang 1 und 3)

**A. Zweck und Alltag**
1. In welcher Situation im Alltag würdest du zu dieser App greifen?
2. Wie löst du das heute (Zettel, Excel, andere App), und was stört dich daran?
3. Welche eine Sache muss in wenigen Sekunden klappen?
4. Welche drei Dinge muss die erste richtige Version können, damit du sie wirklich benutzt?
5. Was soll die App ausdrücklich **nicht** können oder werden?
6. Woran merkst du nach einem Monat, dass sich die App gelohnt hat?

**B. Menschen und Sprache**
7. Wer nutzt die App: nur du, Familie/Freunde oder auch Fremde? Sind ältere Menschen dabei oder jemand, der schlecht sieht?
8. Wo und wie oft wird sie benutzt (kurz unterwegs, abends in Ruhe …)? Soll sie auch etwas tun, wenn sie geschlossen ist, z. B. an etwas erinnern?
9. Wie soll sich die App anfühlen (drei Eigenschaften, z. B. „ruhig, herzlich, knapp“)? Soll sie „du“ oder „Sie“ sagen? Nur für Deutschsprachige oder auch für andere Länder?
10. Was bedeutet der Name „Moltobene“ für dich, und soll er in der App eine Rolle spielen?

**C. Deine Daten**
11. Was gibt man in die App ein (Texte, Fotos, Zahlen …)? Ist etwas Persönliches dabei, das niemand sehen soll, auch über andere Personen?
12. Wie schlimm wäre es, wenn diese Daten weg wären? Sollen sie aufs neue Handy, auf ein zweites Gerät oder zu anderen Personen? Muss die App ohne Internet funktionieren?

## Einigkeit

- Die Vision (#15) ist die wichtigste offene Aufgabe. Alle Fachbereiche brauchen sie, um sinnvolle Vorschläge machen zu können.
- Der Projektinhaber bekommt **nur einen kurzen Fragebogen** ohne Fachwörter. Heikle Punkte werden ruhig und neutral gefragt, nicht als Liste von Kategorien. Alle Detailfragen leiten die Fachleute selbst aus den Antworten ab.
- Technische Regeln gehören in einen eigenen Abschnitt „Technische Grundsätze“, nicht in die Vision.
- „Lokal zuerst, ohne Internet und ohne Tracking“ ist die Voreinstellung.
- **Jetzt entscheiden** (vor der ersten Funktion): Zweck, Nutzerkreis, Nicht-Ziele und die **eine** Kernfunktion, mit der Version 0.3.0 beginnt; die Datenfrage; die Standardsprache (#24). Die Navigationsform wird zusammen mit der ersten Kernfunktion festgelegt, weil sie direkt daraus folgt. Design und Kreativität sind sich hier einig.
- **Mit der ersten Funktion, die Daten speichert:** Speicher-Versionierung, damit Daten bei Updates erhalten bleiben. Export und Import von Anfang an, weil es ohne Play Store keine automatische Übernahme aufs neue Handy gibt. Den Export versieht die App mit einem Hinweis, dass die Datei unverschlüsselt ist (Auflage der Sicherheit). Große Exporte laufen im Hintergrund (Auflage der Performance). Die Datensicherung (#12) wird gleichzeitig entschieden. Verschlüsselung ist nur nötig, wenn die Datenfrage „heikle Daten“ ergibt.
- **Kann warten:** Markenfarben (#2), Logo (#3), „Was ist neu“ (#17), weitere Sprachen (#27), Datenschutzerklärung und SECURITY.md (#14) bis vor eine öffentliche Verteilung, Play Store, Konten und Synchronisation (nur wenn die Vision es verlangt), Baseline-Profile und Benchmarks, Gradle-Cache (#20).

## Streitpunkte – Entscheidung nötig

1. **Wann wird die Code-Verkleinerung R8 (#19) eingeschaltet?**
   - *Performance:* Jetzt. Das ist heute ohne Risiko, und mit wachsendem Code wird das Einrichten aufwendiger.
   - *Funktionalität / Konnektivität:* Erst, wenn der Build automatisch prüft (Lint/Tests, #6). Sonst fallen Fehler erst in der fertigen APK auf. Ab der ersten Datenfunktion braucht es außerdem Schutzregeln, damit alte Sicherungen lesbar bleiben.
   - **Frage an dich:** R8 sofort einschalten oder erst nach #6?

2. **Update-Hinweis (#16) gegen den Grundsatz „Lokal zuerst“**
   - *Kreativität (#16):* Ein Hinweis auf neue Versionen ist nützlich, weil es ohne Play Store keine automatischen Updates gibt.
   - *Sicherheit / Konnektivität:* Das wäre die erste Internetfunktion. Sie bricht die Voreinstellung, und dabei gehen Gerätedaten wie die IP-Adresse an GitHub. Das braucht eine bewusste Entscheidung und einen Hinweis für die Nutzer.
   - **Frage an dich:** Soll die App für diesen einen Zweck ins Internet dürfen? Oder bleibt sie vorerst ganz offline, und Updates holst du dir selbst über GitHub?

## Offene Fragen an den Projektinhaber

- Den Fragebogen oben beantworten. Am einfachsten im Gespräch mit Claude, es dauert etwa 20 Minuten. Daraus entsteht dann die Vision in `CLAUDE.md` (#15).
- Die beiden Streitpunkte oben entscheiden.

## Zurückgezogen / bereits als Issue vorhanden

- **Zurückgezogen bzw. zusammengelegt:** D1, F1, S1, N1, T1, Ü1 (als eigene Fragenblöcke) sind im gemeinsamen Fragebogen aufgegangen. F3 und T2 gehen in der Gliederung auf (K2). T3 ist keine eigene Tabelle mehr, sondern ein Begriffsfeld in der Gliederung. S3 und K3 gehen in der Liste „Jetzt / Später“ auf. N2 und S2 sind zu einem Grundsatz zusammengelegt.
- **Abgeschwächt:** D3 (jetzt nur die Navigationsform, und die erst mit der Kernfunktion), P1 (nur noch zwei Fragen), P2 (auf zwei Sätze gekürzt), Ü3 (nur #24 ist dringend).
- **Bestehende Issues, auf die verwiesen wurde:** #15, #14, #18, #12, #22, #24, #25, #26, #27, #2, #3, #4, #6, #16, #17, #19, #20, #23, #1.

## Nachtrag: Entscheidungen des Projektinhabers und Runde 3

Im Anschluss an die Besprechung hat der Projektinhaber die Vision festgelegt. Die vollständige Fassung steht in `CLAUDE.md` unter „Projektbeschreibung (Vision)“. Damit ist #15 erledigt.

**Antworten des Projektinhabers:**
- Moltobene ist ein Sammelwerk für Kochrezepte mit vielen Wegen zur Erfassung. Alle Inhalte werden lokal auf dem Gerät gespeichert.
- Erfassungswege: selbst eintippen, von Internetseiten, per Foto von Kochbuch oder Zettel, aus Dateien und anderen Apps, außerdem von Plattformen wie YouTube und Koch-Portalen. Weitere Vorschläge der Spezialisten sind erwünscht.
- Internet ist nur zum Abholen von Rezepten erlaubt. Nutzerkreis: alle Interessierten. Weitergeben und Umziehen: beides gewünscht.
- Sprache: Deutsch und Englisch, später weitere. Texte ohne direkte Anrede. Stimmung: modern und schlicht.
- Bewusst nicht: keine Werbung, kein Konto, kein soziales Netzwerk, kein Tracking.

**Runde 3: Kritik der Spezialisten an der Vision.** Das Urteil war einstimmig: tragfähig. Eingearbeitet wurden:
- Kochmodus als eigene Kernfunktion (Kreativität, Design)
- Jede Übernahme endet in einem bearbeitbaren Entwurf, nichts geht verloren (Funktionalität, Konnektivität, Texte)
- Feste Reihenfolge der Erfassungswege; YouTube nur als bestmöglicher Versuch (Kreativität, Funktionalität, Konnektivität)
- Quelle immer mitspeichern (Kreativität, Sicherheit, Texte)
- Fotos verkleinern, Vorschaubilder verwenden, Aufnahmeort entfernen, Rezepte ohne Foto gestalten (Performance, Design, Sicherheit)
- Rezepte in Originalsprache, Einheiten wie im Original (Übersetzungen)
- Weitere Erfassungswege: geteilter Text, Zwischenablage, Bildschirmfoto und PDF (Kreativität, Konnektivität)
- Offene Formate (Konnektivität), feste Begriffe (Texte), Leistungsmaßstab mit 1.000 Rezepten (Performance)

**Entschiedene Punkte:**
- Streitpunkt 2 / #16: Es gibt keine automatische Update-Prüfung. Stattdessen gibt es in den Einstellungen „Nach neuer Version suchen“, das nur auf Knopfdruck ins Internet geht.
- #12: Die automatische Android-Cloud-Sicherung wird abgeschaltet. Gesichert wird über die eigene Sicherungsdatei.
- Die Texterkennung wird in die App eingebaut und funktioniert ohne Google-Dienste. Die App wird dadurch ca. 5–10 MB größer.
- Kernsatz: „Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy.“
- #24: Englisch ist die Rückfallsprache, Deutsch kommt als Übersetzung dazu.

- Streitpunkt 1 / #19: R8 wird sofort eingeschaltet (Version 0.2.2). Jede neue Version wird kurz auf dem Handy ausprobiert.

- Issues aus dieser Besprechung: keine neuen. Die Beschlussvorschläge Rang 1–5 sind in der Vision (`CLAUDE.md`) aufgegangen.

**Offene Fachfrage für die nächste Besprechung:** Werden Zutaten als Freitext gespeichert oder strukturiert (Menge, Einheit, Zutat)? Davon hängen die Suche nach Zutaten und das Umrechnen von Portionen ab.

## Anhang: Beiträge und Stellungnahmen

**Design:** D1 Fragen zu Nutzern, Nutzungssituation, Häufigkeit, Stimmung und Gerät. D2 Design-Mindeststandards (Material 3, Hell/Dunkel, Kontraste, 48 dp, Schrift bis 200 %, Screenreader, Lade-/Leer-/Fehlerzustände). D3 Weichen: Navigationsform, eigene Farbe gegen Farben vom Hintergrundbild, Schrift- und Abstandsskala. *Aussprache:* D1 geht in K1 auf. Farbe (#2) kann warten, die Navigation hängt an der Kernfunktion. Export, Import und Löschen sollen verständlich in die Einstellungen, nicht in ein Technik-Menü. Top 3: K1, K2, D2+S2+N2.

**Funktionalität:** F1 Datenfrage (was wird gespeichert, was darf nie verloren gehen). F2 Bauplan Oberfläche / ViewModel / Repository vor der ersten Funktion. F3 Kernfunktionen als prüfbare Sätze plus Nicht-Ziele, mit Tests (#6). *Aussprache:* F1 geht im Datenblock auf, F3 in K2, F2 in die Bauregeln. Bedenken: R8 erst nach #6. Top 3: K2, K1 mit Datenblock, N3.

**Sicherheit:** S1 Datenfrage zu personenbezogenen und heiklen Daten. S2 Voreinstellung „nur auf dem Gerät, kein Internet, kein Tracking“. S3 Jetzt/Später (Backup #12, Verschlüsselung; DSGVO-Transparenz, sobald Fremde die App nutzen). *Aussprache:* Export-Dateien sind unverschlüsselt, deshalb ein Hinweis beim Export. #16 bricht die Voreinstellung. S3 zurückgezogen. Top 3: K1+K2, Datenblock, S2+N2.

**Kreativität:** K1 Fragebogen mit 8 Alltagsfragen. K2 feste Gliederung und Regeln gegen Überladen. K3 zuerst die eine Kernfunktion für 0.3.0, #2, #16 und #17 warten. *Aussprache:* Vorschlag eines Fragebogens in drei Blöcken (Zweck, Menschen und Sprache, Daten) mit höchstens 12 Fragen. Technische Regeln gehören nicht ins Gespräch. Top 3: gemeinsamer Fragebogen, K2 mit T2 und F3, S2+N2.

**Konnektivität:** N1 fünf Ja/Nein-Fragen zu Daten und Verbindungen. N2 „lokal zuerst, kein Server ohne Entscheidung“. N3 Speicher-Versionierung und Export/Import ab der ersten Datenfunktion, Internet erst bei Bedarf. *Aussprache:* N1 schrumpft auf zwei Fragen im Datenblock. R8 braucht später Schutzregeln für Export und Datenbank. Top 3: K1+K2, F1+S1, S2+N2.

**Performance:** P1 fünf Leistungsfragen. P2 Leistungsgrundsätze (schneller Start, Hauptthread frei, Hintergrundarbeit nur begründet). P3 R8 (#19) und #1 jetzt, Feinschliff später. *Aussprache:* P1 auf zwei Fragen gekürzt (Arbeit bei geschlossener App, viele Fotos), die Frage nach älteren Geräten beantwortet das Team selbst. Große Exporte müssen im Hintergrund laufen. P3 bleibt bestehen. Top 3: K1, K2, F1+S1.

**Texte:** T1 drei Sprachfragen (wer, wie, welche Sprache). T2 verbindlicher Kernsatz, überall gleich. T3 Begriffsliste für Kernfunktionen. *Aussprache:* Die Fragen an den Projektinhaber müssen ohne Fachwörter auskommen, heikle Punkte ruhig und neutral gefragt werden. T1 geht in K1 auf, T3 wird ein Begriffsfeld in K2. Top 3: K1, K2 mit T2 und F3, Datenfrage.

**Übersetzungen:** Ü1 Sprachen und Regionen, Rückfallsprache, Italien-Bezug. Ü2 alle Texte übersetzbar in `strings.xml`, mit Mehrzahlformen und Systemformaten. Ü3 jetzt #24 und #25, später #27. *Aussprache:* Der Kernsatz muss in `strings.xml` liegen. Ü1 schrumpft auf eine Frage („nur Deutschsprachige oder auch andere Länder?“). Top 3: K1, K2, Datenfrage.
