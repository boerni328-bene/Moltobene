# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

## [0.27.0] – 2026-10-09 (versionCode 48)

### Neu
- **Bildschirm in der Rezeptansicht:** In den Einstellungen gibt es den neuen Abschnitt „Kochen“. Dort lässt sich wählen, ob der Bildschirm in der Rezeptansicht anbleibt (wie bisher), 15 oder 30 Minuten nach dem letzten Tippen ausgeht oder sich wie das Handy verhält. Jede Berührung, auch beim Blättern, lässt die Zeit neu beginnen. Die App braucht dafür keine neue Berechtigung.
- **Backform als Rezeptmenge:** Angaben wie „Springform Ø 26 cm“, „Für eine 26er Springform“, „Blech 30 x 40 cm“ oder “One 9-inch cake” werden jetzt richtig gelesen, aus Internetseiten, Rezeptdateien, Text und Fotos. Größen wie „26 cm“ zählen nicht mehr als Portionen (bisher wurden daraus z. B. 26 Portionen). Ist etwas unklar, bleiben die Portionen leer, statt geraten zu werden. Eine Backform ohne Anzahl steht trotzdem beim Rezept und wird beim Teilen mitgegeben. Zwischen − und + steht jetzt z. B. „Springform (Ø 26 cm): 2“.
- **Lizenz:** Moltobene ist jetzt ausdrücklich freie Software unter der GNU General Public License v3.0 (GPL-3.0). Sie steht unter Einstellungen → Info → „Open-Source-Lizenzen“ an erster Stelle.

### Geändert
- **Sinnvoll gerundet beim Umrechnen:** Beim Ändern der Portionen werden Stückangaben auf ½ gerundet (unter 1 auf ¼), Gramm und Milliliter ab 100 auf 5, ab 10 auf ganze Gramm und darunter auf 0,5. Statt „3,75 Eier“ steht also „≈ 4 Eier“. Gerundete Mengen beginnen mit „≈“, der Screenreader liest „etwa …, umgerechnet“. Stückzahlen stehen als Bruch da, z. B. „1½ Zwiebeln“. Löffel, Tassen, Liter und Kilo werden weiterhin genau umgerechnet. Das gespeicherte Rezept bleibt wie immer unverändert.
- **Packungen in Klammern werden mitgerechnet:** „21 g Frischhefe (½ Würfel)“ wird beim Verdoppeln zu „42 g Frischhefe (1 Würfel)“. Würfel, Päckchen und Packungen werden nur als Anzahl umgerechnet, nie in Gramm.
- **Übersetzung kennt mehr Küchenwörter:** Begriffe zum Backen wie Backform, Kastenform, Blech, Teigkugel, Nudelholz, Küchenmaschine, Gehzeit, Umluft und Ober-/Unterhitze werden jetzt richtig übersetzt, ins Englische mit amerikanischen Wörtern wie “powdered sugar”, “heavy cream” und “green onions”. Aufgenommen wurde nur, was in Probesätzen nachweislich besser wurde. Bereits übersetzte Rezepte werden dafür einmal neu übersetzt, wenn die Übersetzung das nächste Mal angezeigt wird.
- Kleine Textkorrekturen, z. B. lautet der Hinweis im Feld „Portionen“ jetzt „z. B. Stück, Springform Ø 26 cm“.

## [0.26.0] – 2026-10-08 (versionCode 47)

### Neu
- **Darstellung wählbar:** In den Einstellungen gibt es den neuen Abschnitt „Darstellung“. Unter „Hell oder dunkel“ lässt sich „Wie das Handy“ (wie bisher), „Hell“ oder „Dunkel“ wählen, unabhängig von der Einstellung des Handys.
- **Fünf Farbwelten:** Unter „Farben“ stehen „Schiefer“ (wie bisher), „Kobalt“, „Salbei“, „Terrakotta“ und „Safran“ zur Auswahl, jede hell und dunkel. Alle sind gleich gut lesbar wie „Schiefer“. Die ganze App wechselt sofort. M und B im Schriftzug MOLTOBENE bleiben immer im Blau des Logos.

### Behoben
- **Rezepte von Blogs ohne Überschrift „Zutaten“:** Manche Seiten leiten die Zutaten mit einem Satz ein, z. B. „For this recipe, you will need:“ (so bei pastagrammar.com), statt „Ingredients“ darüberzuschreiben. Dort meldete „Aus Link übernehmen“ bisher „kein Rezept gefunden“. Jetzt werden solche Einleitungen erkannt, auch auf Deutsch („Für dieses Rezept brauchst du:“), Italienisch, Französisch und Spanisch. Lange Zutatenzeilen mit Erklärung in Klammern bleiben Zutaten, „…, to taste“ bleibt hinten stehen, und „Back to blog“ oder Kommentare unter dem Rezept kommen nicht mehr in die Zubereitung.

## [0.25.0] – 2026-10-08 (versionCode 46)

### Neu
- **Alle Links aus der Videobeschreibung zur Auswahl:** Beim Übernehmen eines YouTube-Videos stehen jetzt alle Links aus der Beschreibung zur Auswahl, der wahrscheinlichste Link zum Rezept oben. Links zu Instagram, Facebook, TikTok, Amazon, anderen Shops und anderen Videos werden weggelassen, dort steht nie das Rezept. Geladen wird erst der angetippte Link. Steht dort kein Rezept, bleibt die Auswahl stehen und ein anderer Link lässt sich versuchen.
- **Neues Feld „Video“:** Der Link zum YouTube-Video wird getrennt von der Quelle gespeichert. Wird das Rezept von einer Internetseite übernommen, ist diese Seite die Quelle und das Video steht unter „Video“. Das Feld lässt sich beim Bearbeiten auch selbst ausfüllen. In der Rezeptansicht ist der Link antippbar. Er kommt in die Sicherung und wird beim Teilen mitgegeben (Notizen weiterhin nie).

### Geändert
- Im Dunkelmodus sind M und B im Schriftzug MOLTOBENE jetzt kräftiger blau und näher am Blau des Logos, aber weiterhin gut lesbar.

## [0.24.0] – 2026-10-08 (versionCode 45)

### Geändert
- **Symbole oben statt großer Knöpfe:** In der Sammlung stehen „Rezept hinzufügen“ (+) und „Rezept übernehmen“ jetzt als Symbole oben neben MOLTOBENE, vor den Einstellungen. Die beiden großen Knöpfe unten rechts sind weg, so verdecken sie keine Rezepte mehr. In der leeren Sammlung bleiben die beiden Knöpfe in der Mitte. Der Screenreader liest bei den Symbolen „Rezept hinzufügen“ und „Rezept übernehmen“.
- **MOLTOBENE auf jedem Bildschirm:** Der Schriftzug steht jetzt oben überall, auch in den Einstellungen, beim Bearbeiten und bei den Open-Source-Lizenzen. Darunter steht groß, wo man gerade ist, z. B. „Einstellungen“ oder „Rezept bearbeiten“. Bei der Seitenübersicht, „Bereich auswählen“ und dem Vollbild steht der Name klein direkt unter MOLTOBENE, damit das Foto möglichst viel Platz hat.
- **Schriftzug wie im Logo:** MOLTOBENE ist fett, M und B sind blau wie im App-Symbol. Im Dunkelmodus ist das Blau heller, damit es gut lesbar bleibt.

## [0.23.1] – 2026-10-08 (versionCode 44)

### Geändert
- **Neues App-Symbol:** ein weißes „MB“ mit Lächeln auf Kobaltblau, rundherum kleine Zeichen für die Quellen der Rezepte: Kochbuch, Foto, Kamera, Internet, Video und Datei. Ab Android 13 passt sich das Symbol auf Wunsch den Farben des Handys an („Designbasierte Symbole“).

## [0.23.0] – 2026-10-08 (versionCode 43)

### Geändert
- **MOLTOBENE oben:** Die Sammlung heißt oben jetzt MOLTOBENE statt „Sammlung“. Auch in der Rezeptansicht steht MOLTOBENE oben in der Leiste und bleibt beim Blättern stehen. Der Screenreader liest „Moltobene“. Ist bei sehr großer Schrift zu wenig Platz, fällt der Name in der Rezeptansicht weg, statt abgeschnitten zu werden.
- **Umschalter „DE | EN“ oben in der Leiste:** Er bleibt beim Blättern sichtbar, auch mitten in der Zubereitung. Die Originalsprache ist mit einem Sternchen markiert, z. B. „DE* | EN“. Die gewählte Sprache ist ausgefüllt und fett. Fortschritt und Hinweise zur Übersetzung stehen weiter unter dem Titel.
- **Teilen im Menü:** In der Rezeptansicht steht „Rezept teilen“ jetzt als erster Eintrag im Menü ⋮, damit oben Platz für MOLTOBENE und den Umschalter ist.
- Der Hinweis unter einer Übersetzung ist kürzer: „Maschinell übersetzt. Mit dem Original vergleichen.“

### Behoben
- Im Umschalter brach „DE · Original“ auf zwei Zeilen um, dadurch waren die beiden Hälften unterschiedlich hoch.

## [0.22.0] – 2026-10-07 (versionCode 42)

### Neu
- **Sprache des Rezepts wählen:** Beim Bearbeiten steht unter der Quelle das Feld „Sprache des Rezepts“. Zur Wahl stehen „Automatisch erkennen“ sowie Deutsch, English, Italiano, Français und Español, jeweils mit ihrem eigenen Namen. Die Sprache bestimmt, wie Mengen beim Umrechnen der Portionen gelesen werden und ob der Umschalter „DE | EN“ zum Übersetzen erscheint. Das hilft vor allem bei kurzen Rezepten, deren Sprache die App nicht sicher erkennt.

### Geändert
- Bei neu eingetippten Rezepten erkennt die App die Sprache jetzt am Text. Bisher nahm sie die Sprache des Handys an, sodass z. B. ein englisches Rezept auf einem deutschen Handy als deutsch galt. Lässt sich die Sprache nicht sicher erkennen, bleibt sie offen und wird später erneut am Text erkannt.

## [0.21.0] – 2026-10-07 (versionCode 41)

### Neu
- **Rezepte übersetzen (Deutsch ↔ Englisch):** Bei deutschen und englischen Rezepten steht in der Rezeptansicht unter dem Titel ein Umschalter „DE | EN“. Die Originalsprache ist mit „Original“ markiert, der Screenreader liest den Namen der Sprache vor. Ein Tippen auf die andere Sprache übersetzt Titel, Zutaten und Zubereitung direkt auf dem Handy, ohne Internet. Ein Fortschritt zeigt, wie weit die Übersetzung ist; „Abbrechen“ hält sie an. Notizen werden nie übersetzt.
- Die Übersetzung ist ein maschineller Vorschlag; ein Hinweis erinnert daran, sie mit dem Original zu vergleichen. Unter einem übersetzten Titel steht der Titel im Original. Zeilen, bei denen die Übersetzung nicht stimmen kann, etwa weil eine Zahl fehlt oder sich ändert, bleiben im Original stehen. Typische Küchenbegriffe wie „icing sugar“ (Puderzucker) oder „Tellerlinsen“ (brown lentils) übersetzt ein eigenes Küchenwörterbuch.
- Abhaken, aktueller Schritt und Portionen umrechnen funktionieren auch in der Übersetzung, und die Häkchen bleiben beim Umschalten erhalten. Das gespeicherte Rezept bleibt in seiner Originalsprache. Die Übersetzung wird zusätzlich auf dem Handy gemerkt und erscheint beim nächsten Öffnen sofort; ändert sich das Rezept, wird neu übersetzt.
- **Sprachpaket Deutsch–Englisch:** Für die Übersetzung wird einmal das Sprachpaket gebraucht, unter Einstellungen → Übersetzung. Download etwa 172 MB, auf dem Handy etwa 274 MB, am besten im WLAN. Mit Fortschritt, Abbrechen und Löschen. Die App prüft das Paket beim Herunterladen mit einer festen Prüfsumme. Es kommt nicht in die Sicherung und lässt sich jederzeit neu laden. Fehlt es, zeigt die Rezeptansicht den Weg zu den Einstellungen.
- Die Datenschutzerklärung nennt jetzt auch den Download des Sprachpakets.
- Unter „Open-Source-Lizenzen“ stehen die Modelle des Sprachpakets: OPUS-MT der Universität Helsinki, Lizenz CC BY 4.0.

## [0.20.0] – 2026-10-07 (versionCode 40)

### Neu
- **Zutaten abhaken:** In der Rezeptansicht hat jede Zutat ein Kästchen. Ein Tippen auf die Zeile hakt sie ab: Sie bekommt ein Häkchen und wird durchgestrichen. Der Screenreader sagt „abgehakt“ bzw. „nicht abgehakt“.
- **Aktueller Schritt:** Ein Tippen auf einen Schritt der Zubereitung markiert ihn als aktuellen Schritt, mit Pfeil und farbigem Hintergrund. So geht die Stelle beim Kochen nicht verloren. Ein weiteres Tippen hebt die Markierung auf.
- **Portionen umrechnen:** Neben den Portionen stehen − und +. Die Mengen werden für die neue Anzahl umgerechnet, fett hervorgehoben und so geschrieben wie im Rezept: „1.000 g“ wird zu „2.000 g“, „½ TL“ zu „1 TL“, „1 1/2 cups“ zu „3 cups“. Umgerechnet werden Mengen am Anfang einer Zeile und Gewicht oder Volumen in Klammern, z. B. „3 EL (45 g)“. Zahlen mitten im Text wie „Saft von 2 Zitronen“ bleiben, wie sie sind; ein Hinweis sagt das. „Zurücksetzen“ zeigt wieder die Mengen des Rezepts. Das gespeicherte Rezept ändert sich dabei nie.
- Im Menü oben setzt „Abhaken zurücksetzen“ alle Häkchen und den aktuellen Schritt zurück.
- Häkchen, aktueller Schritt und Portionen bleiben erhalten, wenn das Handy gedreht wird oder Android die App im Hintergrund beendet.

### Geändert
- Beim Bearbeiten heißt das Feld neben den Portionen jetzt „Einheit (optional)“ statt „Angabe (optional)“.

## [0.19.1] – 2026-10-07 (versionCode 39)

### Behoben
- **Aus Link übernehmen:** Auf manchen Seiten fand die App kein Rezept, obwohl Zutaten und Zubereitung dort stehen, z. B. bei joshuaweissman.com. Betroffen waren Seiten, die mit bestimmten Baukästen für Internetseiten gestaltet sind. Jetzt kommen die Zutaten (auch in Gruppen wie „Gremolata:“) und die Zubereitung ins Rezept, dazu Titel, Portionen, Zeiten und das Foto der Seite.
- Kommentare und Hinweise auf weitere Rezepte unter einem Rezept landen nicht mehr in der Zubereitung.

## [0.19.0] – 2026-10-07 (versionCode 38)

### Neu
- **Rezepte aus YouTube-Videos:** „Aus Link übernehmen“ und „Teilen mit…“ aus der YouTube-App nehmen jetzt auch Links zu Kochvideos an, auch Shorts. Die App liest die Videobeschreibung. Steht dort ein Rezept, kommen Zutaten, Portionen und, falls vorhanden, die Zubereitung ins Rezept, dazu der Titel und das Vorschaubild des Videos. Werbung, Links, Kapitelmarken und Schlagwörter bleiben draußen. Als Quelle wird der Link zum Video gespeichert, ohne den Zusatz, mit dem YouTube verfolgt, wer einen Link geteilt hat. Ein Hinweis erinnert daran, das Rezept mit dem Video zu vergleichen, denn in Beschreibungen fehlt oft etwas.
- **Link zum ganzen Rezept:** Verweist die Videobeschreibung auf das Rezept auf einer Internetseite, steht das unter dem Rezept, z. B. „Die Videobeschreibung verweist auf ein Rezept bei …“. Mit „Aus Link übernehmen“ wird das Rezept von dort übernommen und ersetzt, was aus der Beschreibung kam, solange daran noch nichts geändert wurde. Geladen wird diese Seite nur auf Wunsch.
- Steht in der Beschreibung kein Rezept, bleiben Titel und Link des Videos als Entwurf erhalten, die ganze Beschreibung steht unter „Übernommener Text“.

### Geändert
- Einige Koch-Portale haben die App bisher abgewiesen (z. B. REWE, EDEKA oder Cookie and Kate). Die App fragt Seiten jetzt so an wie ein üblicher Browser, deshalb lassen sich auch dort Rezepte übernehmen. Dabei werden keine Angaben zum Handy oder zur Person mitgeschickt.
- „So einfach geht’s“ wird wie „Zubereitung“ als Überschrift erkannt, auch bei „Aus Text übernehmen“.

YouTube kann den Aufbau seiner Seiten jederzeit ändern. Die Übernahme aus Videos ist deshalb ein bestmöglicher Versuch ohne Zusage.

## [0.18.0] – 2026-10-07 (versionCode 37)

### Neu
- **Sicherung geprüft:** Nach dem Sichern liest die App die Sicherungsdatei noch einmal: das Inhaltsverzeichnis und die Liste der Rezepte. Erst wenn alles da ist, kommt „Sammlung gesichert“. Fotos werden dafür nicht noch einmal ausgepackt, das Sichern dauert also kaum länger. Ist die Datei unvollständig, etwa weil der Speicher voll war oder die App beendet wurde, wird sie entfernt. So bleibt keine Sicherung liegen, die sich später nicht wiederherstellen lässt.
- **Zuletzt gesichert:** In den Einstellungen steht unter „Sammlung sichern“, wann zuletzt gesichert wurde und wie viele Rezepte es waren, oder „Noch nie gesichert“. Nach dem Wiederherstellen auf einem neuen Handy gilt die verwendete Sicherung als letzte.
- **Ruhiger Hinweis in der Sammlung:** Sind 10 oder mehr Rezepte neu oder geändert und noch nicht gesichert, steht oben in der Sammlung z. B. „12 Rezepte sind noch nicht gesichert.“ mit „Sichern“. Der Hinweis lässt sich ausblenden und erscheint erst nach 10 weiteren Rezepten wieder. Es gibt kein Fenster und keine Benachrichtigung.

### Geändert
- Ist beim Sichern oder Wiederherstellen kein Speicherplatz mehr frei, sagt die App das jetzt genau so und nennt den nächsten Schritt. Bisher hieß es beim Wiederherstellen fälschlich „Die Sicherungsdatei ist beschädigt“.

## [0.17.1] – 2026-10-07 (versionCode 36)

### Geändert
- Keine sichtbaren Änderungen an der App. GitHub prüft die Bausteine der App jetzt auch auf bekannte Sicherheitslücken und warnt, falls eine bekannt wird. Dafür darf GitHub die Liste der Bausteine ermitteln, ohne die App zu bauen. Dass auf GitHub nie eine unsignierte Version entsteht, ist weiterhin sichergestellt.

## [0.17.0] – 2026-10-06 (versionCode 35)

### Neu
- **Aus Datei übernehmen:** Rezeptdateien lassen sich jetzt übernehmen. Gemeint sind etwa Dateien, die jemand mit „Als Rezeptdatei teilen“ aus Moltobene verschickt hat, oder Rezepte anderer Apps im offenen Standardformat. Eine solche Datei öffnet sich direkt aus WhatsApp, E-Mail oder dem Dateimanager über „Öffnen mit…“ bzw. „Teilen mit…“. Alternativ steht „Aus Datei übernehmen“ im Fenster „Rezept übernehmen“. Titel, Zutaten, Zubereitung, Portionen, Zeiten, Quelle und das mitgeschickte Foto kommen ins Rezept.
- Auch gespeicherte Rezeptseiten (HTML) werden gelesen.
- Dateien werden ohne Internet gelesen. Steht in einer Datei nur ein Link auf ein Foto im Internet, wird es nicht geladen. Dateien über 10 MB werden nicht angenommen.

### Behoben
- Ein mit Moltobene geteilter Rezepttext endet mit „Quelle: …“. Beim Übernehmen mit „Aus Text übernehmen“ landete diese Zeile bisher als letzter Schritt in der Zubereitung. Jetzt wird sie zur Quelle; bei Büchern kommt auch die Seite ins Feld „Seite“.
- Portionsangaben wie „Für 1 Zopf“ oder „Für 1 Springform (26 cm)“ wurden zu „1 Portion“. Jetzt bleibt die Einheit erhalten. Zeitangaben wie „Für 10 Minuten“ gelten nicht mehr als Portionen.

## [0.16.0] – 2026-10-06 (versionCode 34)

### Neu
- **Aus Link übernehmen:** Rezepte von Koch-Portalen und Rezept-Blogs lassen sich jetzt mit ihrem Link übernehmen – mit Titel, Zutaten, Zubereitung, Portionen, Zeiten und Foto. Der Weg steht im Fenster „Rezept übernehmen“ an erster Stelle und beim Bearbeiten neben „Aus Foto übernehmen“ und „Aus Text übernehmen“. Ein Link lässt sich einfügen oder direkt aus der Zwischenablage übernehmen.
- **„Teilen“ aus dem Browser:** Wird eine Rezeptseite aus dem Browser an Moltobene geteilt, lädt die App die Seite gleich und macht daraus ein Rezept.
- Fast alle großen Koch-Portale und die üblichen Rezept-Blogs enthalten ihre Rezepte zusätzlich in einem Standardformat, das die App vollständig liest. Steht auf einer Seite kein Rezept in diesem Format, ordnet die App den Text der Seite ein, wie bei „Aus Text übernehmen“. Klappt auch das nicht, bleibt der Link als Quelle gespeichert.
- **Nichts geht verloren:** Der Link wird schon vor dem Laden als Quelle gespeichert. Ohne Internet, bei einer Seite, die nicht antwortet, oder bei einer gesperrten Seite sagt eine Meldung, was passiert ist und was sich tun lässt. Das Laden lässt sich jederzeit abbrechen.
- Übernommene Rezepte bleiben in ihrer Originalsprache und behalten ihre Quelle. Ein Hinweis bittet darum, das Ergebnis zu prüfen.
- **Zeiten:** Vorbereitungs- und Gesamtzeit aus übernommenen Rezepten stehen in der Rezeptansicht, z. B. „Gesamtzeit: 45 Min.“.
- **Datenschutzerklärung:** Unter Einstellungen → Info steht jetzt kurz, was die App speichert und wann sie ins Internet geht.

### Geändert
- Die App braucht jetzt eine einzige Berechtigung: den Internetzugang. Sie geht nur ins Internet, wenn ein Rezept aus einem Link übernommen wird. Dann wird genau diese eine Seite geladen, bei Bedarf auch ihr Foto. Die Verbindung ist immer verschlüsselt, Cookies werden nicht gespeichert, und es gibt keinen Abruf im Hintergrund. Die Übermittlung an Microsoft aus Version 0.14.0 bleibt abgeschaltet.
- Unter „Open-Source-Lizenzen“ steht jetzt auch jsoup, das die Internetseiten liest.

## [0.15.1] – 2026-10-06 (versionCode 33)

### Behoben
- **Texterkennung auf Handys mit wenig Speicher:** Reicht der Speicher für die Texterkennung nicht, stürzt die App nicht mehr ab. Stattdessen erscheint: „Für die Texterkennung ist gerade zu wenig Speicher frei. Andere Apps schließen und erneut versuchen oder weniger Seiten auf einmal lesen.“
- **Seiten bleiben nach einem Fehler erhalten:** Klappt die Texterkennung nicht, geht es zurück zur Seitenübersicht. Die Fotos müssen nicht neu ausgewählt werden; „Text erkennen“ lässt sich gleich wiederholen, und einzelne Seiten lassen sich vorher entfernen.
- Vor dem Lesen der Seiten werden die bisherigen Eingaben eines neuen Rezepts als Entwurf gesichert, falls Android die App währenddessen beendet.

### Geändert
- Die Texterkennung braucht weniger vom knappen Arbeitsspeicher der App: Das große Lesemodell (21 MB) wird nicht mehr doppelt im Speicher gehalten.

## [0.15.0] – 2026-10-05 (versionCode 32)

### Neu
- **Fotos ganz ansehen:** Das Foto eines Rezepts wird jetzt in seinem eigenen Format gezeigt, Hoch- wie Querformat, statt auf einen festen Ausschnitt zugeschnitten. Antippen öffnet es als Vollbild; vergrößern geht mit zwei Fingern, doppeltem Tippen oder den Schaltflächen. Das gilt in der Rezeptansicht und beim Bearbeiten.
- **Fotoserie ohne Unterbrechung:** Beim Fotografieren von Rezeptseiten öffnet sich die Kamera nach jedem Foto gleich wieder für die nächste Seite. „Zurück“ in der Kamera beendet die Serie und zeigt die Seitenübersicht; nach 6 Seiten endet sie von selbst.

### Behoben
- **Kochbuchseiten mit mehreren schmalen Spalten** werden jetzt Spalte für Spalte gelesen. Bisher lief die Zubereitung quer über alle Spalten und konnte mitten in der zweiten Spalte beginnen.
- **Titel neben der Zutatenliste:** Ein großer Titel oben auf der Seite wird jetzt auch übernommen, wenn links daneben z. B. eine Zutaten-Karte steht. Ein Titel über zwei Zeilen wird zu einem Titel zusammengefügt.
- „Sie brauchen:“ und ähnliche Überschriften (auch auf Englisch, Italienisch, Französisch und Spanisch) werden als Überschrift der Zutaten erkannt.
- Nährwerte wie „Enthält pro Portion … kcal“ hängen nicht mehr am letzten Schritt, sondern stehen als eigener Eintrag am Ende der Zubereitung.

## [0.14.1] – 2026-10-05 (versionCode 31)

### Behoben
- **Keine Berechtigung und keine Telemetrie mehr:** Version 0.14.0 enthielt unbemerkt einen Baustein von Microsoft, der Nutzungsdaten der Texterkennung ins Internet senden konnte, und dafür die Internet-Berechtigung. Beides ist entfernt; die App hat wieder keine Berechtigung. Ob tatsächlich etwas gesendet wurde, ist nicht belegt.
- Damit das nicht wieder passiert, wird jede neue Version vor der Veröffentlichung auf Berechtigungen geprüft.
- Beim Start räumt die App Reste auf: Dateien, die dieser Baustein angelegt haben kann, und die alten Sprachpakete der früheren Texterkennung (rund 12 MB).

## [0.14.0] – 2026-10-03 (versionCode 30)

### Neu
- **Neue Texterkennung:** Fotos von Kochbüchern, Zetteln und Bildschirmen werden jetzt deutlich zuverlässiger gelesen – auch schräg aufgenommene, gewölbte oder teilweise im Schatten liegende Seiten. In Tests mit nachgestellten Handyfotos stieg der Anteil richtig gelesener Wörter bei schwierigen Fotos von etwa 40 % auf über 95 %. Umlaute und Akzente (z. B. „È più buono“) bleiben erhalten.
- **Zutaten und Zubereitung automatisch erkannt:** Die App erkennt jetzt selbst, was auf einer Seite nebeneinander steht. Bei Kochbuchseiten mit zwei Spalten kommen erst alle Zutaten, dann die Zubereitung – ohne dass Bereiche markiert werden müssen. Tabellen wie in Rezept-Apps (Zutat links, Menge rechts) werden Zeile für Zeile zur Zutat mit Menge. Getrennte Absätze der Zubereitung bleiben getrennte Schritte.
- Die Texterkennung läuft weiterhin nur auf dem Handy, ohne Internet und ohne Google-Dienste. Sie kennt alle Sprachen der App zugleich; die Sprache des Rezepts wird am Text erkannt.

### Geändert
- Die App ist größer (etwa 45 MB statt 14 MB), weil das neue Erkennungsprogramm und seine Modelle mitgeliefert werden. Die alten Sprachpakete (rund 12 MB) werden beim ersten Lesen vom Handy entfernt.
- „Bereich auswählen“ ist nur noch selten nötig, z. B. um Werbung oder Knöpfe auf Bildschirmfotos auszulassen.
- Unter „Open-Source-Lizenzen“ stehen jetzt PaddleOCR, OnnxOCR und ONNX Runtime; lange Lizenztexte werden abschnittweise angezeigt.

## [0.13.0] – 2026-10-03 (versionCode 29)

### Neu
- **Mehrere Fotos hintereinander:** Längere Rezepte lassen sich jetzt Seite für Seite fotografieren, ohne dass nach jedem Foto gelesen wird. Die Seiten sammeln sich in einer Übersicht mit Vorschaubildern; dort gibt es „Weitere Seite fotografieren“ und „Weitere Fotos auswählen“. Erst „Text erkennen“ liest alle Seiten zusammen und macht daraus ein Rezept. Bis zu 6 Seiten auf einmal.
- In der Übersicht lässt sich jede Seite wieder entfernen. Wer abbricht, wird vorher gefragt, damit keine Fotos aus Versehen verloren gehen.

### Geändert
- „Bereich auswählen“ kommt nicht mehr bei jedem Foto, sondern nur noch bei Bedarf über das Symbol an der Seite in der Übersicht, z. B. bei Kochbuchseiten mit zwei Spalten. Ohne Bereich wird die ganze Seite gelesen.
- Die Sprache des Textes wird jetzt im Menü oben rechts in der Übersicht gewählt.
- Auch Fotos aus „Teilen mit…“ und das Rezeptfoto kommen zuerst in die Übersicht.

## [0.12.0] – 2026-10-02 (versionCode 28)

### Neu
- **„Aus Text übernehmen“:** Ein Rezept aus einer Nachricht, einer E-Mail oder einer Notiz einfügen – von Hand oder mit „Aus der Zwischenablage einfügen“. Titel, Portionen, Zutaten und Zubereitung werden automatisch eingeordnet; der vollständige Text bleibt beim Rezept erhalten, damit nichts verloren geht. Ein Link im Text wird zur Quelle.
- **„Teilen mit…“ für Text:** Moltobene erscheint jetzt auch im Teilen-Menü, wenn Text geteilt wird, z. B. aus WhatsApp oder einer E-Mail. Der Text kommt direkt in ein neues Rezept. Wird nur ein Link geteilt, etwa aus dem Browser, speichert die App ihn mit dem Titel der Seite als Quelle. Das Lesen von Internetseiten selbst folgt in einer späteren Version; bis dahin lässt sich der Rezepttext von der Seite kopieren und mit „Aus Text übernehmen“ einfügen.
- Die Zwischenablage wird nur gelesen, wenn „Aus der Zwischenablage einfügen“ angetippt wird. Die App bleibt ohne Internet und braucht keine Berechtigung.

### Geändert
- In der Sammlung heißt der Knopf jetzt „Rezept übernehmen“ und führt zur Auswahl „Aus Foto übernehmen“ oder „Aus Text übernehmen“.
- „Erkannter Text“ heißt jetzt „Übernommener Text“, weil er auch aus eingefügtem Text stammen kann.
- Hinweise beim Bearbeiten bleiben länger sichtbar.

## [0.11.2] – 2026-10-02 (versionCode 27)

### Geändert
- Keine sichtbaren Änderungen an der App. Die Bausteine, aus denen die App besteht, und die Werkzeuge, mit denen sie gebaut wird, sind auf dem neuesten Stand – darunter die Bausteine für Bildschirme, Datenbank und Fotos sowie die Programmiersprache Kotlin. Neuere Bausteine enthalten Fehlerbehebungen und Verbesserungen und halten die App für künftige Android-Versionen fit.

## [0.11.1] – 2026-10-02 (versionCode 26)

### Geändert
- Keine sichtbaren Änderungen an der App. Jede neue Version wird vor der Veröffentlichung automatisch auf einem virtuellen Android-Handy durchgeklickt: Rezept hinzufügen, Titel vergessen, suchen, ansehen, löschen, aus Foto übernehmen und die Einstellungen – jeweils im hellen und dunklen Modus, mit 200 % Schrift und auf Englisch. Klappt dabei etwas nicht, wird die Version nicht veröffentlicht. Von jedem Bildschirm entsteht ein Foto, damit Aussehen und Texte vor der Veröffentlichung geprüft werden können.

## [0.11.0] – 2026-10-02 (versionCode 25)

### Neu
- **„Teilen mit…“:** Moltobene erscheint jetzt im Teilen-Menü von Android, wenn Bilder geteilt werden. Ein Bildschirmfoto oder ein Foto aus WhatsApp oder E-Mail lässt sich so direkt übernehmen: Die App öffnet ein neues Rezept und beginnt gleich mit „Bereich auswählen“. Auf einmal werden höchstens 6 Bilder gelesen.
- Angenommen werden nur Bilder, die eine andere App ausdrücklich freigibt. Sie werden sofort in die App kopiert; danach behält Moltobene keinen Zugriff. Eine Berechtigung ist dafür nicht nötig, und die App bleibt ohne Internet.

### Geändert
- Einheitliche Namen: Der Weg heißt beim Bearbeiten jetzt wie in der Sammlung „Aus Foto übernehmen“ (bisher „Text aus Foto erkennen“); „Rezeptfoto lesen“ heißt jetzt „Rezeptfoto verwenden“.

## [0.10.1] – 2026-10-02 (versionCode 24)

### Geändert
- Keine sichtbaren Änderungen. Die Bausteine der Texterkennung werden jetzt bei jedem Bauen gegen feste Prüfsummen geprüft: das Texterkennungs-Programm Tesseract, das nicht signiert ist, und die fünf Sprachpakete, die genau den Originalen entsprechen müssen. Ein unbemerkt ausgetauschter Baustein käme so nie in die App – wichtig, bevor die App für das Übernehmen von Internetseiten erstmals ins Internet darf.

## [0.10.0] – 2026-10-02 (versionCode 23)

### Neu
- **Seite der Quelle:** Neben „Quelle“ gibt es das Feld „Seite“. In der Rezeptansicht und beim Teilen erscheint sie als „S. 47“ (auf Englisch „p. 47“). Bei einem Link gibt es keine Seite.
- **Vorschläge für die Quelle:** Nach der Texterkennung stehen unter der Quelle antippbare Vorschläge – die zuletzt genutzten Bücher aus der eigenen Sammlung und die Seitenzahl, die auf der Seite erkannt wurde. Wer mehrere Rezepte aus demselben Kochbuch übernimmt, muss den Buchtitel nicht jedes Mal neu tippen. Es gibt dafür kein zusätzliches Fenster.

### Geändert
- Eine Quelle mit Seite gilt als Buch. Ältere Angaben wie „Omas Kochbuch, S. 42“ in einem Feld bleiben unverändert erhalten.

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
