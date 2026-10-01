# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

## [0.5.0] – 2026-10-01 (versionCode 13)

Rezepte aus Fotos übernehmen: Die App liest Titel, Zutaten und Zubereitung selbst.

### Neu
- **Text aus Foto erkennen** (beim Bearbeiten eines Rezepts): liest das Rezeptfoto, ausgewählte Fotos oder ein neu aufgenommenes Foto. Leere Felder werden ausgefüllt, Zutaten und Zubereitung ergänzt.
- **Aus Foto übernehmen** (in der Sammlung): ein neues Rezept direkt aus Fotos von Kochbuch, Zettel oder Bildschirmfoto. Bei Rezepten über mehrere Seiten die Fotos einfach der Reihe nach auswählen.
- Die Erkennung kennt Deutsch, Englisch, Italienisch, Französisch und Spanisch und wählt die passende Sprache selbst, damit auch Akzente wie in „Crème brûlée“ stimmen.
- Der vollständige erkannte Text bleibt beim Rezept erhalten und lässt sich beim Bearbeiten anzeigen und kopieren.

### Hinweis
- Erkannter Text sollte geprüft werden: Bei Handschrift, Schatten oder gewölbten Buchseiten macht die Erkennung Fehler.
- Die Texterkennung ist fest eingebaut und läuft nur auf dem Handy, ohne Internet und ohne Google-Dienste. Die App ist dadurch größer geworden.

## [0.4.1] – 2026-10-01 (versionCode 12)

### Behoben
- Beim Teilen eines Rezepts mit Foto kam in manchen Apps, z. B. WhatsApp, nur das Foto an. Der Rezepttext wird jetzt zusammen mit dem Foto übergeben.

## [0.4.0] – 2026-10-01 (versionCode 11)

Rezepte lassen sich jetzt teilen.

### Neu
- **Rezept teilen** (Symbol oben rechts in der Rezeptansicht): Titel, Portionen, Zutaten, Zubereitung, Quelle und Foto gehen als gut lesbarer Text z. B. per Messenger oder E-Mail an andere. Eigene Notizen bleiben dabei privat.
- **Als Rezeptdatei teilen** (Menü ⋮): eine Datei im offenen Standard schema.org, die auch andere Rezept-Apps lesen können. Das Foto steckt mit in der Datei.
- **Neu in Version …:** Nach einem Update zeigt die App einmal kurz, was neu ist. Die Übersicht steht auch in den Einstellungen unter „Info“.

### Geändert
- „Rezept löschen“ steht jetzt im Menü ⋮ oben rechts in der Rezeptansicht.
- Geteilte Fotos werden neu gespeichert, damit garantiert kein Aufnahmeort mitgeht.

## [0.3.1] – 2026-10-01 (versionCode 10)

### Geändert
- Ab Android 13 passt sich das App-Symbol an, wenn in den Android-Einstellungen einfarbige Symbole („Designbasierte Symbole“) eingeschaltet sind.
- Jede neue Version lässt sich auf Echtheit prüfen: Zu jeder APK gibt es eine Prüfsumme, den Fingerabdruck des Signaturzertifikats und einen fälschungssicheren Herkunftsnachweis von GitHub. Wie das geht, steht in der README.
- Bevor eine Version erscheint, wird jetzt automatisch geprüft, ob sie korrekt signiert ist und sich als Update installieren lässt.
- Sicherheitslücken lassen sich vertraulich melden (siehe SECURITY.md).
- Die Projektseite auf GitHub erklärt jetzt, was die App kann und wie man sie installiert.

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
- Keine sichtbaren Änderungen an der App. Jede neue Version wird jetzt vor der Veröffentlichung automatisch auf Fehler geprüft und getestet und nur veröffentlicht, wenn sie korrekt signiert ist.

## [0.2.2] – 2026-09-29 (versionCode 5)

### Geändert
- Die App ist jetzt deutlich kleiner und startet schneller: Beim Bauen wird nicht benötigter Code automatisch entfernt und der Rest optimiert.

## [0.2.1] – 2026-09-29 (versionCode 4)

### Geändert
- Keine sichtbaren Änderungen an der App. Ein Team aus Fachleuten für Gestaltung, Sicherheit, Texte und weitere Bereiche prüft die App ab jetzt regelmäßig und schlägt Verbesserungen vor.

## [0.2.0] – 2026-09-29 (versionCode 3)

### Neu
- Die Startseite zeigt jetzt unter dem App-Namen dezent die aktuelle Versionsnummer an. Sie wird beim Bauen automatisch übernommen.

## [0.1.1] – 2026-09-29 (versionCode 2)

### Geändert
- Keine sichtbaren Änderungen an der App. Die Werkzeuge, mit denen die App gebaut wird, sind auf dem neuesten Stand.

## [0.1.0] – 2026-09-29 (versionCode 1)

### Neu
- Erste Version: eine Startseite mit dem Namen „Moltobene“, das Grundgerüst für alle weiteren Funktionen.
- Läuft auf Android 8.0 und neuer.
- Jede neue Version wird automatisch gebaut, signiert und hier veröffentlicht.
