# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

## [0.3.0] – 2026-09-29 (versionCode 9)

Die erste Version der Rezeptsammlung.

### Neu
- **Rezepte hinzufügen, bearbeiten und löschen:** Titel, Foto, Portionen, Zutaten (eine pro Zeile, Zwischenüberschriften mit Doppelpunkt), Zubereitung (ein Schritt pro Zeile), Quelle und Notizen.
- **Sammlung:** Liste aller Rezepte mit Vorschaubild und Suche in Titeln, Zutaten und Notizen.
- **Rezeptansicht:** Foto, Zutaten und Zubereitung übersichtlich. Beim Ansehen bleibt der Bildschirm an.
- **Fotos** aus der Galerie auswählen oder mit der Kamera aufnehmen. Sie werden verkleinert und ohne Aufnahmeort gespeichert.
- **Sammlung sichern und wiederherstellen** (Einstellungen): alle Rezepte mit Fotos in einer Datei. Beim Wiederherstellen wird nichts gelöscht.
- **Nichts geht verloren:** Eingaben überstehen Anrufe und das Drehen des Handys. Ein neues Rezept wird automatisch als „Entwurf“ gespeichert, wenn die App in den Hintergrund geht.

### Geändert
- Die App speichert nichts mehr in der Google-Cloud-Sicherung. Gesichert wird über die eigene Sicherungsdatei. Ab Android 12 bleibt der direkte Umzug von Handy zu Handy möglich.
- Die App braucht weiterhin keine einzige Berechtigung und keinen Internetzugang.

## [0.2.5] – 2026-09-29 (versionCode 8)

### Geändert
- Neues Farbkonzept „Schiefer“: ein ruhiges Blaugrau für hellen und dunklen Modus, das den Fotos der Gerichte den Vortritt lässt. Die App übernimmt nicht mehr die Farben vom Hintergrundbild.
- Das App-Symbol hat jetzt die neue Hauptfarbe.

### Behoben
- Beim Start im Dunkelmodus blitzt der Bildschirm nicht mehr weiß auf.
- Die Startseite ist mit großer Schrift und mit der Vorlesefunktion (Screenreader) besser nutzbar.

## [0.2.4] – 2026-09-29 (versionCode 7)

### Geändert
- Die App ist jetzt zweisprachig vorbereitet: Auf deutschsprachigen Handys erscheint sie auf Deutsch, auf allen anderen auf Englisch.
- Ab Android 13 lässt sich die Sprache der App in den Android-Einstellungen unter „App-Sprache“ getrennt vom Handy wählen.

## [0.2.3] – 2026-09-29 (versionCode 6)

### Geändert
- Der automatische Bau ist sicherer und gründlicher geworden: Jede neue Version wird jetzt vor der Veröffentlichung automatisch auf Fehler geprüft und getestet. Außerdem wird sie nur noch veröffentlicht, wenn sie korrekt signiert ist. An der App selbst ändert sich nichts.

## [0.2.2] – 2026-09-29 (versionCode 5)

### Geändert
- Die App ist jetzt deutlich kleiner und startet schneller: Beim Bauen wird nicht benötigter Code automatisch entfernt und der Rest optimiert.

## [0.2.1] – 2026-09-29 (versionCode 4)

### Geändert
- Arbeitsregeln ergänzt: Prüfrunden mit dem Spezialisten-Team (Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen), deren Vorschläge als GitHub-Issues erfasst werden. An der App selbst ändert sich nichts.

## [0.2.0] – 2026-09-29 (versionCode 3)

### Neu
- Die Startseite zeigt jetzt unter dem App-Namen dezent die aktuelle Versionsnummer an. Sie wird beim Bauen automatisch übernommen.

## [0.1.1] – 2026-09-29 (versionCode 2)

### Geändert
- Build-Werkzeuge (GitHub Actions) auf aktuelle Versionen aktualisiert. Das behebt eine Warnung wegen veraltetem Node.js. An der App selbst ändert sich nichts.

## [0.1.0] – 2026-09-29 (versionCode 1)

### Neu
- Grundgerüst der Android-App angelegt (Kotlin, Jetpack Compose).
- Startseite, die den App-Namen „Moltobene“ anzeigt.
- Unterstützt Android 8.0 (API 26) und neuer.
- Gradle-Wrapper hinzugefügt, damit die App später über GitHub Actions gebaut werden kann.
- Arbeitsregeln (CLAUDE.md), dieses Änderungsprotokoll und .gitignore angelegt.
- Automatischer Build über GitHub Actions: Bei jeder Änderung wird eine signierte APK gebaut.
- Für jede neue Version wird automatisch ein GitHub-Release mit der APK veröffentlicht.
