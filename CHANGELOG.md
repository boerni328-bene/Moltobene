# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

## [0.9.2] – 2026-10-02 (versionCode 22)

### Geändert
- Die Texterkennung ist schneller, besonders bei mehreren Seiten und Bereichen: Das Sprachpaket wird nur noch einmal geladen statt für jede Seite und jeden Bereich neu.
- Während der Erkennung zeigt ein Balken, wie weit sie ist. Bei mehreren Seiten steht dazu z. B. „Seite 2 von 3 wird gelesen …“; der Screenreader sagt das von selbst an.

### Behoben
- „Abbrechen“ wirkte in den ersten Sekunden einer Erkennung manchmal nicht. Jetzt hält es die Erkennung jederzeit an.

## [0.9.1] – 2026-10-01 (versionCode 21)

### Behoben
- Bei Rezepten über mehrere Seiten blieben Seitenzahlen mitten in Zutaten oder Zubereitung stehen. Sie werden jetzt auf jeder Seite einzeln entfernt.
- Eine Zeile, die nur aus einer Zahl besteht (z. B. „47“), gilt nicht mehr als Zutat.
- Überschriften werden auch erkannt, wenn ein Akzent falsch gelesen wurde: Aus „Elaboración“ wurde mit dem falschen Sprachpaket „Elaboraciön“, und die Überschrift landete bei den Zutaten.
- Mehr Einheiten werden erkannt, sodass Mengen am Zeilenende nach vorne kommen: „gr“ (z. B. „Farina 00 300 gr“), „Esslöffel“, „Teelöffel“, „Gramm“, „Liter“, „Stange“, „tablespoon“, „teaspoon“, „tbsp.“, „c. à s.“, „c. à c.“, „cda.“, „cdta.“ und weitere.

### Geändert
- Die App ist gut ein Drittel kleiner zum Herunterladen und Aktualisieren (14,0 statt 21,9 MB): Die Bausteine der Texterkennung werden jetzt gepackt ausgeliefert. Auf dem Handy belegt sie etwa gleich viel Platz wie bisher.
- Die Sprachpakete der Texterkennung gehen beim Umzug auf ein neues Handy nicht mehr mit (rund 12 MB weniger). Sie werden dort beim ersten Lesen wieder bereitgestellt.

## [0.9.0] – 2026-10-01 (versionCode 20)

### Neu
- **Bereiche zuordnen:** In „Bereich auswählen“ lassen sich mit „Bereich hinzufügen“ mehrere Rahmen auf eine Seite legen. Für jeden Rahmen wird unter „Im Rahmen steht:“ gewählt, was darin steht: „Alles“, „Titel“, „Zutaten“ oder „Zubereitung“. Bezeichnete Rahmen gehen ohne Raten direkt ins passende Feld – so werden z. B. Kochbuchseiten mit Zutaten links und Zubereitung rechts richtig gelesen. Die Bezeichnung steht als Text am Rahmen.
- Ein Rahmen „Alles“ liest nicht noch einmal, was schon in einem bezeichneten Rahmen liegt; so entsteht nichts doppelt.
- Ein Rahmen wird durch Antippen gewählt; „Bereich entfernen“ nimmt ihn wieder heraus, „Ganze Seite“ setzt alles zurück. Wer nichts ändert, liest wie bisher die ganze Seite.

### Geändert
- Die Rahmen haben jetzt eine helle Linie unter der farbigen und sind so auf weißen Buchseiten wie auf dunklen Bildschirmfotos gut zu sehen.
- Als Originalseite wird bei mehreren Rahmen der Bereich behalten, der alle Rahmen umfasst.

## [0.8.0] – 2026-10-01 (versionCode 19)

### Neu
- **Sprache des Textes:** Im Menü oben rechts in „Bereich auswählen“ lässt sich festlegen, in welcher Sprache der Text geschrieben ist – Deutsch, English, Italiano, Français oder Español. Das hilft bei kurzen Ausschnitten, an denen sich die Sprache nicht erkennen lässt. Voreingestellt ist „Automatisch erkennen“.

### Behoben
- Beim Lesen einer weiteren Seite fehlten manchmal Zutaten oder Schritte: Stand ein Wort wie „Salz“ schon als Teil einer längeren Zeile („1 TL Salz“) da, wurde es nicht ergänzt. Jetzt werden ganze Zeilen verglichen. Wird dieselbe Seite zweimal gelesen, entstehen trotzdem keine doppelten Zeilen.
- Die Sprache eines Rezepts wird nur noch übernommen, wenn sie eindeutig erkannt oder gewählt wurde, und eine schon vorhandene Angabe wird nicht mehr überschrieben. Beim Teilen geht so keine bloß geratene Sprache mehr mit.
- Ist die erste Seite zu kurz, um die Sprache zu erkennen, prüft die App die folgenden Seiten.

## [0.7.0] – 2026-10-01 (versionCode 18)

Erkannte Rezepte leichter prüfen – und die Vorlage beim Rezept behalten.

### Neu
- **Seiten ansehen:** Nach der Texterkennung lassen sich alle gelesenen Seiten als Vollbild ansehen und vergrößern – mit zwei Fingern, doppeltem Tippen oder den Schaltflächen „Vergrößern“ und „Verkleinern“. So lässt sich der erkannte Text Zeile für Zeile mit der Vorlage vergleichen.
- **Originalseiten:** Beim Speichern fragt die App einmal „Gelesene Seiten behalten?“. Behaltene Seiten liegen getrennt vom Foto des Gerichts beim Rezept und lassen sich in der Rezeptansicht mit „Originalseite ansehen“ öffnen – praktisch z. B. für handgeschriebene Familienrezepte. Gespeichert wird nur der gewählte Bereich, ohne Aufnahmeort. Originalseiten werden mitgesichert, aber nie geteilt, und lassen sich beim Bearbeiten unter „Seiten ansehen“ einzeln entfernen.
- Eine gelesene Seite lässt sich mit „Als Rezeptfoto verwenden“ zum Foto des Rezepts machen, z. B. wenn die Buchseite das Gericht zeigt.
- Der erkannte Text lässt sich mit „Erkannten Text entfernen“ löschen, z. B. wenn ein Bildschirmfoto fremde Namen enthielt.

### Geändert
- Die erste gelesene Seite wird nicht mehr automatisch zum Rezeptfoto. Die Sammlung zeigt so Fotos der Gerichte statt Textseiten.
- Statt der kurzen Meldung bleibt nach der Erkennung der Hinweis „Text ins Rezept übernommen. Mengen und Einheiten auf Lesefehler prüfen.“ bis zum Speichern sichtbar. Die Frage „Foto behalten?“ direkt nach der Erkennung entfällt.
- Meldungen zur Texterkennung sagen jetzt, was sich tun lässt, z. B. „Kein Text gefunden. Am besten klappt es mit einem scharfen, gerade aufgenommenen Foto bei gutem Licht.“
- Sicherungen enthalten jetzt auch die Originalseiten. Ältere Sicherungen bleiben lesbar.

## [0.6.1] – 2026-10-01 (versionCode 17)

### Behoben
- Die Texterkennung geht nicht mehr verloren, wenn während des Lesens eine andere App geöffnet wird und Android Moltobene im Hintergrund beendet:
  - Ausgewählte Fotos und Kamerafotos werden sofort in die App übernommen und bleiben bis zum Ende der Erkennung erhalten.
  - Wurde die Erkennung im Hintergrund noch fertig, steht ihr Ergebnis beim Zurückkehren im Rezept. Ein neues Rezept wird dann zusätzlich als Entwurf gespeichert.
  - Wurde sie unterbrochen, erscheint „Texterkennung wurde unterbrochen.“ – mit „Erneut erkennen“ werden dieselben Seiten mit denselben Bereichen noch einmal gelesen.
- Die übernommenen Fotos liegen nur in einem eigenen App-Ordner, der weder in Sicherungen noch beim Umzug auf ein neues Handy mitgeht. Sie werden nach dem Erkennen gelöscht, Reste beim nächsten Start.

## [0.6.0] – 2026-10-01 (versionCode 16)

### Neu
- **Bereich auswählen:** Vor der Texterkennung erscheint jedes Foto mit einem Rahmen. Gelesen wird nur, was im Rahmen liegt – bei einem Bildschirmfoto z. B. nur die Zutaten, ohne Kopfzeile, Knöpfe und Werbung. Ecken und Ränder lassen sich verschieben, „Ganze Seite“ setzt den Rahmen zurück. Bei mehreren Fotos geht es mit „Nächste Seite“ weiter.

### Geändert
- Bildschirmfotos werden sauberer gelesen: Status- und Browserleiste gelten nicht mehr als Zutaten-Tabelle. Was die Erkennung nur aus Symbolen, Knöpfen oder Fotos „liest“, und Wortfetzen wie „A Wa u“ werden weggelassen. Dadurch entstehen kaum noch falsche Zutaten.

## [0.5.2] – 2026-10-01 (versionCode 15)

### Geändert
- Texterkennung: Zutaten-Tabellen, wie sie viele Rezept-Seiten und -Apps zeigen (Zutat links, Menge rechts, Trennlinien dazwischen), werden jetzt Zeile für Zeile gelesen. Jede Zeile wird eine Zutat mit der Menge vorne, z. B. „500 g Hackfleisch gemischt“ – auch wenn die Zutat über zwei Zeilen geht.
- Mengen am Zeilenende („Oregano getrocknet 1 Pr“) kommen nach vorne, „etwas Salz, Pfeffer“ zählt als Zutat, und typische Lesefehler bei Einheiten werden korrigiert („500 q“ wird „500 g“).
- Zutaten über zwei Zeilen („Hackfleisch“ / „gemischt“) werden zusammengefügt.
- Werbung auf Bildschirmfotos („Anzeige“) und unlesbare Bildreste werden nicht mehr übernommen.

## [0.5.1] – 2026-10-01 (versionCode 14)

### Neu
- Nach der Texterkennung fragt die App „Foto behalten?“. Zeigt das Foto nur die Buchseite, lässt es sich mit einem Tipp entfernen und spart Speicherplatz. Gelöscht wird es erst beim Speichern des Rezepts.
- Einstellungen → Info → „Open-Source-Lizenzen“: die quelloffenen Bausteine, auf denen die App aufbaut, mit ihren Lizenzen.

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
