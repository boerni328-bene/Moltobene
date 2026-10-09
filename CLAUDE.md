# CLAUDE.md – Arbeitsregeln für das Projekt Moltobene

## Projektbeschreibung (Vision)

Festgelegt vom Projektinhaber am 29.09.2026 (siehe `docs/besprechungen/2026-09-29-vision-der-app.md`), ergänzt am 07.10.2026 um die Übersetzung (#60, `docs/besprechungen/2026-10-07-rezepte-uebersetzen.md`). Änderungen nur mit seiner Zustimmung. Neue Vorschläge werden an dieser Vision gemessen.

**Kernsatz** (wörtlich gleich in README, GitHub und Releases): _Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy._

**Für wen:** Alle, die gern kochen. Öffentlich und kostenlos, verteilt über GitHub Releases. Eine freiwillige Spende ist möglich (Link nur im README, im Store-Eintrag und unter Einstellungen → Info; die App bittet nie aktiv darum, keine Bezahlversion, kein Abo; entschieden am 09.10.2026, siehe `docs/marktberichte/2026-10-09-marktbericht.md`). Lizenz: GPL-3.0 (#72).

### Kernfunktionen

1. **Rezept hinzufügen:** Rezepte selbst eintippen (Titel, Zutaten, Zubereitung, eigenes Foto).
2. **Rezept übernehmen** aus anderen Quellen, in dieser Reihenfolge:
   1. Internetseiten und Koch-Portale (Teilen aus dem Browser oder Link einfügen); geteilter Text aus beliebigen Apps (z. B. WhatsApp, E-Mail) und Zwischenablage.
   2. Foto oder Bildschirmfoto von Kochbuch, Zettel oder PDF mit Texterkennung.
   3. YouTube (Videobeschreibung; nur als bestmöglicher Versuch ohne Zusage) sowie Dateien oder andere Rezept-Apps.
   Weitere Erfassungswege sind erwünscht und werden von den Spezialisten vorgeschlagen.
3. **Sammlung finden und ordnen:** suchen, sortieren, einordnen.
4. **Mit dem Rezept kochen:** Bildschirm bleibt an (in den Einstellungen änderbar), aus Armlänge lesbar, Zutaten und Schritte abhakbar, Portionen umrechnen (auch nach einer Zutat oder Backform), eigene Notizen zum Rezept. Dazu ein **Teigrechner** für Pizza, Brot, Pasta und Kuchen, jede Teigart mit eigenen Angaben (z. B. Vorteig wie Biga oder Poolish, verschiedene Hefen); das Ergebnis wird ein Rezept. (Ergänzt am 08.10.2026, siehe `docs/besprechungen/2026-10-08-teige-pizza-pasta-kuchen.md`.)
5. **Teilen, sichern und umziehen:** einzelne Rezepte teilen; die ganze Sammlung inklusive Fotos als Datei sichern, wiederherstellen und auf ein neues Handy umziehen.

### Bewusst nicht

Keine Werbung, kein Konto oder Anmelden, kein soziales Netzwerk, kein Tracking, keine Cloud, keine Weitergabe von Daten an Dritte.

### Grundsätze

- **Alles lokal:** Alle Inhalte liegen nur auf dem Gerät. Die automatische Android-Cloud-Sicherung ist abgeschaltet; gesichert wird über die eigene Sicherungsdatei (direkter Umzug von Handy zu Handy bleibt erlaubt).
- **Internet nur auf Wunsch:** Nur wenn der Nutzer ein Rezept übernehmen lässt, das Sprachpaket für die Übersetzung herunterlädt oder in den Einstellungen „Nach neuer Version suchen“ antippt. Keine automatische Update-Prüfung, keine sonstigen Verbindungen.
- **Nichts geht verloren:** Jede Übernahme endet in einem bearbeitbaren Entwurf. Klappt sie nicht (kein Netz, Seite nicht lesbar), bleiben Link bzw. Text als Entwurf erhalten. Die App weist ehrlich darauf hin, dass übernommene Inhalte geprüft werden sollten.
- **Quelle:** Übernommene Rezepte behalten immer ihre Quelle (Link, Buch), sie wird angezeigt und beim Teilen mitgegeben.
- **Texterkennung** läuft ausschließlich auf dem Gerät und ist in die App eingebaut (funktioniert auch ohne Google-Dienste).
- **Fotos:** werden beim Speichern verkleinert; Listen zeigen nur Vorschaubilder; beim Teilen und Sichern wird der Aufnahmeort entfernt. Jedes Rezept sieht auch ohne Foto vollständig aus.
- **Offene Formate:** Einzelrezepte werden im Standard schema.org/Recipe geteilt; die Sicherung ist eine Datei mit Rezepten, Fotos und Formatversion. Alte Sicherungen bleiben immer lesbar.
- **Sprache:** Oberfläche auf Deutsch und Englisch (Englisch als Rückfallsprache), weitere Sprachen später. Rezepte werden in ihrer Originalsprache gespeichert; übersetzt wird nur auf Wunsch, direkt auf dem Handy und gekennzeichnet, das Original bleibt erhalten. Mengen und Einheiten werden wie im Original übernommen, eine Umrechnung kann später folgen.
- **Texte:** ohne direkte Anrede, neutral und knapp („Rezept hinzufügen“, „Rezept wirklich löschen?“, „Noch keine Rezepte vorhanden“).
- **Feste Begriffe:** siehe Abschnitt „Texte und Übersetzungen“.
- **Gestaltung:** modern und schlicht, Fotos der Gerichte im Mittelpunkt, eigene ruhige Farbpalette. Voll nutzbar mit großer Schrift und Screenreader.
- **Leistung:** Auch mit 1.000 Rezepten mit Fotos bleibt die App auf einem günstigen Android-8-Handy beim Start, Blättern und Suchen flüssig.
- **Fokus:** Neue Funktionen müssen zu einer Kernfunktion passen. Lieber eine Funktion fertig als mehrere halb.

## Texte und Übersetzungen

### Feste Begriffe

Immer dieses Wort verwenden, nie die Alternativen in Klammern.

| Deutsch | Englisch | Nicht verwenden |
|---|---|---|
| Rezept | Recipe | |
| Sammlung | Collection | Bibliothek, Liste |
| Titel | Title | Name |
| Zutaten | Ingredients | |
| Zubereitung (einzelner Eintrag: Schritt) | Instructions (Step) | Anleitung |
| Portionen | Servings | Personen |
| Quelle | Source | Herkunft |
| Video (Link zum Video des Rezepts, getrennt von der Quelle) | Video | Film, Clip |
| Notizen | Notes | Kommentar |
| Foto | Photo | Bild |
| Originalseite (Foto der Vorlage, z. B. der Kochbuchseite) | Original page | Scan, Vorlage |
| Schlagwort | Tag | Kategorie, Tag |
| Favorit | Favorite | |
| Entwurf | Draft | |
| abhaken (Zutat beim Kochen) | check off | ankreuzen, erledigt |
| aktueller Schritt | current step | |
| umrechnen (Mengen für andere Portionen) | adjust | hochrechnen, skalieren |
| hinzufügen (neues Rezept) | add | anlegen, erstellen |
| übernehmen (aus einer Quelle) | import | importieren |
| teilen (an andere) | share | exportieren |
| sichern / wiederherstellen (ganze Sammlung) | back up / restore | Backup, Export |
| Handy | device | Gerät, Smartphone |
| Einstellungen | Settings | |
| übersetzen (Rezept in eine andere Sprache) | translate | übertragen |
| Übersetzung | Translation | |
| Originalsprache | Original language | Ursprache |
| Sprachpaket | Language pack | Sprachmodell |
| Teigrechner | dough calculator | Pizzarechner, Assistent |
| Teigart (Pizza, Brot, Pasta, Kuchen) | dough type | Kategorie (gilt nur für Schlagwörter) |
| Teigkugel | dough ball | Teigling |
| Wasseranteil, „% vom Mehl“ | hydration, “% of flour” | Hydration (im Deutschen), Bäckerprozente |
| Gehzeit | rising time | Gare, Stockgare |
| Vorteig | preferment | |
| Hauptteig | final dough | |
| Biga, Poolish | biga, poolish | – (Namen, nicht übersetzen: `translatable="false"`) |
| Frischhefe | fresh yeast | |
| Trockenhefe (Instant) | instant yeast | |
| Aktive Trockenhefe | active dry yeast | |
| Sauerteig | sourdough starter | |
| Backform (mit „Ø 26 cm“) | pan | Form allein, tin |
| Blech | baking sheet | |
| pro Portion | per serving | pro Person |

„Originaltext“ und „Kopie“ werden beim Übersetzen nicht verwendet (Verwechslung mit „Originalseite“ und „Übernommener Text“).

Das Englisch der Oberfläche ist amerikanisch („Favorite“, „Colors“, „pan“, „parentheses“). Funktionen heißen nach der Tätigkeit, nicht mit einem Sammelnamen wie „Teige“: „Nach Zutat umrechnen“ / “Adjust by ingredient”, „Backform umrechnen“ / “Adjust for pan size”. Erklärsätze für den Teigrechner (Besprechung vom 08.10.2026, #64): „Vorteig: Ein Teil von Mehl, Wasser und Hefe wird vorher angesetzt. Das gibt mehr Geschmack und einen lockereren Teig.“ – „Biga: Fester Vorteig mit wenig Wasser.“ – „Poolish: Flüssiger Vorteig mit gleich viel Wasser wie Mehl.“ – „Hauptteig: Alles, was danach dazukommt.“ Mitgelieferte Auswahlwerte haben feste englische Kennungen: Hefe `fresh`, `instant_dry`, `active_dry`, `sourdough`; Vorteig `biga`, `poolish`; Teigart `pizza`, `bread`, `pasta`, `cake`. Mehltypen werden nie gleichgesetzt (Type 405, 00, all-purpose haben keine genauen Gegenstücke).

### Textregeln

- Keine direkte Anrede, neutral und knapp: „Rezept hinzufügen“, „Rezept wirklich löschen?“, „Noch keine Rezepte vorhanden“.
- Schaltflächen nennen die Aktion („Löschen“, „Speichern“, „Verwerfen“), nie „OK“ oder „Ja“.
- Erfassungswege heißen einheitlich „Aus Link übernehmen“, „Aus Foto übernehmen“, „Aus Text übernehmen“, „Aus Datei übernehmen“ (englisch “Import from link / photo / text / file”). „Text erkennen“ ist nur die Schaltfläche in der Seitenübersicht vor der Texterkennung.
- Meldungen sagen, was passiert ist und was man tun kann; keine Fachbegriffe, kein Amtsdeutsch.
- Deutsche Anführungszeichen „…“, im Englischen “…”.

### Übersetzbarkeit

- Englisch ist die Rückfallsprache (`res/values/strings.xml`, `tools:locale="en"`), Deutsch die Übersetzung (`res/values-de/strings.xml`). Jeder neue Text kommt **immer in beide Dateien**. Mitgeliefert werden nur diese Sprachen (`localeFilters` in `app/build.gradle.kts`).
- Alle sichtbaren Texte inkl. Beschreibungen für den Screenreader stehen in `strings.xml`, nie im Code.
- Mengenangaben als Mehrzahlformen (`plurals`): „1 Rezept“ / „2 Rezepte“.
- Platzhalter nummeriert (`%1$s`, `%2$d`); Sätze nie aus Einzelteilen zusammensetzen.
- Datum und Zahlen über das Format des Handys, nie fest zusammengesetzt.
- Namen, die nicht übersetzt werden (z. B. `app_name`), mit `translatable="false"`.
- Mitgelieferte Werte (z. B. vorgeschlagene Schlagwörter) werden mit fester englischer Kennung gespeichert (z. B. `main_course`), angezeigt wird der Text aus `strings.xml`. Eigene Eingaben der Nutzer werden nie automatisch übersetzt.
- Rezeptinhalte werden in ihrer Originalsprache gespeichert; eine Übersetzung kommt nur auf Wunsch dazu (#60), Notizen werden nie übersetzt.

## Gestaltung

- **Farbpalette „Schiefer“** (ruhiges Blaugrau, vom Projektinhaber gewählt, Standard): Hauptfarbe hell `#3E5A6E`, dunkel `#A9C6DB`; Hintergrund hell `#F7F8F9`, dunkel `#121518`. Die Werte stehen in `ui/theme/Color.kt`, die Fensterfarben in `res/values(-night)/colors.xml` – beide müssen zueinander passen. Keine Farben vom Hintergrundbild (dynamische Farben).
- **Darstellung in den Einstellungen** (vom Projektinhaber am 08.10.2026 gewählt): „Wie das Handy“ (Standard), „Hell“, „Dunkel“ und die Farbwelten „Schiefer“, „Kobalt“, „Salbei“, „Terrakotta“, „Safran“ (`data/Appearance.kt`, gespeichert in `AppPreferences` mit fester englischer Kennung). Alle Farbwelten stehen in `ui/theme/Palettes.kt`; die neuen sind aus „Schiefer“ abgeleitet (gleiche Helligkeit jeder Farbe, nur Farbton und Sättigung gedreht), `PalettesTest` prüft die Kontraste jeder Farbwelt. Eine neue Farbwelt nur mit Zustimmung des Projektinhabers und mit bestandenem `PalettesTest`. Ab Android 12 meldet die App „Hell“/„Dunkel“ zusätzlich an Android (`ui/theme/NightMode.kt`), damit das Startbild passt.
- Farben im Code **nur** über `MaterialTheme.colorScheme` (Ausnahmen: die Markenfarbe des Schriftzugs über `MoltobeneTheme.brand`, Kobaltblau `#2747D9`, im Dunkelmodus etwas heller `#5A78FF`, in jeder Farbwelt gleich; die Farbmuster der Auswahl in den Einstellungen über `colorSchemeOf`), Schriftgrößen nur über `MaterialTheme.typography`, Ecken über `MaterialTheme.shapes`, Abstände über `Spacing` – nie feste Werte.
- Schrift: Standardschrift von Android, Material-3-Größenskala.
- **App-Symbol:** Logo des Projektinhabers vom 08.10.2026 (Variante F3a): weißes „MB“ mit Lächeln auf Kobaltblau `#2747D9`, rundherum die Quellen der Rezepte. Vorlagen unter `docs/logo/`, im App-Code `drawable/ic_launcher_foreground.xml` und `ic_launcher_background` in `values/colors.xml`. Die Farben in der App bleiben „Schiefer“.
- **MOLTOBENE oben auf jedem Bildschirm** (Wunsch des Projektinhabers vom 08.10.2026): `ui/components/AppTitle` – fett, „M“ und „B“ in der Markenfarbe; darunter der Name des Bildschirms als Überschrift (`ScreenTitle`), bei Foto-Bildschirmen klein in der Leiste (`AppTitleWithScreen`). Neue Bildschirme ebenso. In der Sammlung stehen Hinzufügen (+), Übernehmen und Einstellungen als Symbole oben (keine großen Knöpfe unten); in der Rezeptansicht der Umschalter „DE | EN“ (Originalsprache mit Sternchen, z. B. „DE*“), Bearbeiten und ⋮ – „Teilen“ steht dort im Menü.
- Hell- und Dunkelmodus gleichwertig; Kontraste mindestens WCAG AA.
- Tippflächen mindestens 48 dp; Informationen nie nur über Farbe.
- Jeder Bildschirm funktioniert mit Schriftgröße 200 % (Vorschau mit `fontScale = 2f`) und mit dem Screenreader: Überschriften mit `semantics { heading() }`, jedes Symbol ohne Text mit Beschreibung aus `strings.xml`.
- Jeder Bildschirm hat einen gestalteten Lade-, Leer- und Fehlerzustand.

## Kommunikation

- Immer auf **Deutsch** kommunizieren.
- Der Projektinhaber ist **kein Entwickler**: kurz und verständlich erklären, was gemacht wird und warum. Fachbegriffe vermeiden oder kurz erklären.
- **Vor größeren Entscheidungen oder bei Unklarheiten erst nachfragen**, z. B. bei neuen Bibliotheken, Architekturänderungen, Berechtigungen, Datenschutzfragen, Kosten oder wenn eine Anforderung mehrdeutig ist.

## Technischer Rahmen

- Android-App in **Kotlin** mit **Jetpack Compose**.
- Paketname / applicationId: `com.moltobene.app`
- Mindestversion: Android 8.0 (API 26).
- Auf dem Windows-PC gibt es **kein Android Studio und kein Java**. Es wird nicht lokal gebaut, sondern **ausschließlich über GitHub Actions**.
- Abhängigkeiten und Versionen stehen zentral in `gradle/libs.versions.toml`.
- **Lizenz: GPL-3.0** (#72, Entscheidung des Projektinhabers vom 09.10.2026): `LICENSE` im Repository-Stamm mit dem unveränderten Text der FSF (SHA-256 `3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986`), in der App als erster Eintrag unter „Open-Source-Lizenzen“ (`assets/licenses/gpl-3.0.txt`). Neue Bausteine müssen mit der GPL-3.0 verträglich sein (Apache 2.0, MIT und CC BY 4.0 sind es).
- Git-Commits verwenden die GitHub-noreply-Adresse (im Repository lokal eingestellt). Keine privaten E-Mail-Adressen, Passwörter, Schlüssel oder Keystores ins Repository, denn es ist **öffentlich**. Die Versionsgeschichte wurde am 01.10.2026 bereinigt (#10); eine ältere Kopie des Repositorys auf dem PC vor der weiteren Arbeit mit `git fetch origin` und `git reset --hard origin/main` abgleichen.

### Aufbau des Codes

- `data/` – Rezept-Modell (`Recipe.kt`), Textumwandlung und Suche (reines Kotlin, per Unit-Test prüfbar), `AmountScaling` (Portionen umrechnen, #56: liest Mengen nach der Sprache des Rezepts – „1.000 g“, „1,5 l“, „1 1/2“, „½“ – und rechnet nur Mengen am Zeilenanfang sowie Gewicht, Volumen und Packungen in Klammern um, nur in der Anzeige; #65: Stück und Packungen auf ½ bzw. ¼, Gramm und Milliliter ab 100 auf 5, ab 10 auf 1, darunter auf 0,5 gerundet, gerundete Mengen mit „≈“, der Screenreader liest „etwa …, umgerechnet“; Packungen nur als Anzahl, nie in Gramm), `RecipeUnits` (gemeinsame Einheitenliste für Umrechnen und Texterkennung, je Einheit die Art des Rundens), `RecipeYield` (einzige Stelle, die die Rezeptmenge liest, #63: Portionen, Stück oder Backform; Größen wie „Ø 26 cm“, „26er“, „30 x 40“ oder „9-inch“ sind nie eine Anzahl, Unklares bleibt leer; für Internetseiten, Rezeptdateien, Text und Fotos), `db/` (Room-Datenbank mit Volltextsuche), `photos/` (einzige Stelle für Fotos), `backup/` (Sicherungsdatei), `share/` (Teilen als Text und als schema.org-Rezeptdatei; Notizen werden nie geteilt; `IncomingImages`, `IncomingText` und `IncomingRecipeFile` nehmen Geteiltes an – nur content:// anderer Apps, Größe begrenzt), `web/` (einzige Stelle für Internetzugriffe, #55: `WebAddress` prüft Links – nur https, keine Adressen im eigenen Netz –, `HttpPageLoader` lädt mit Zeit- und Größengrenzen ohne Cookies, `RecipeJsonLdReader` liest schema.org/Recipe auch aus Rezeptdateien, `RecipePage` liest eine Seite: JSON-LD, Microdata, sonst Seitentext; `YouTube` liest Titel, Beschreibung und Vorschaubild eines Videos, `VideoDescription` das Rezept und die Links aus der Beschreibung, die zum Rezept führen können (bestmöglicher Versuch; zur Auswahl ohne soziale Netzwerke, Shops und Videos, vom Projektinhaber am 08.10.2026 so gewählt). Der Link zum Video steht im Feld „Video“ (`Recipe.videoUrl`, Datenbank-Version 3, in Sicherung und beim Teilen als schema.org `video`), getrennt von der Quelle; wird das Rezept von einer gewählten Seite übernommen, ist diese die Quelle; `WebImporter` führt alles zusammen; `FileDownloader` lädt das Sprachpaket mit denselben Regeln, aber ohne Zeitgrenze für den ganzen Download; der Rundgang ersetzt den Lader über `AppContainer.pageLoader` durch nachgestellte Seiten), `ocr/` (Texterkennung mit PP-OCRv6: `PaddleOcr` führt die Modelle über ONNX Runtime aus – `TextDetection` findet die Zeilen, `OcrInput` richtet jede Zeile gerade, `CtcDecoder` liest sie; `ReadingOrder` bringt die Zeilen in Lesereihenfolge, trennt Spalten und liest Tabellen „Zutat | Menge“ zeilenweise; `AmountText` ordnet Mengen und korrigiert Einheiten; `CropArea` ist der Bereich aus „Bereich auswählen“; `RecipeTextParser` teilt Text in Titel, Portionen, Zutaten und Zubereitung, `TextLanguage` erkennt die Sprache – alles ohne Android-Abhängigkeiten, mit Unit-Tests; `PaddleOcrTest` prüft die ganze Erkennung mit den echten Modellen an selbst erzeugten, nachgestellten Handyfotos unter `test/resources/ocr/`; Fotos aus echten Kochbüchern kommen wegen des Urheberrechts nicht ins Repository), `translate/` („Rezept übersetzen“ Deutsch ↔ Englisch, #60: `SentencePiece` zerlegt Text in Wortteile wie das Original, `MarianTranslator` übersetzt eine Zeile mit OPUS-MT über ONNX Runtime, `KitchenGlossary` setzt Küchenbegriffe vorher ein – nur, wo das Modell sie nachweislich falsch übersetzt; `LanguagePackTest` zeigt jede Beispielzeile aus `reference.py` mit und ohne Wörterbuch, neue Einträge erst nach diesem Vergleich –, `TranslationGuard` verwirft unplausible Zeilen – Zahlen müssen bleiben –, `RecipeTranslator` übersetzt ein Rezept Zeile für Zeile und Satz für Satz; `LanguagePackInstaller` entpackt das Sprachpaket schon beim Herunterladen und prüft jede Datei, `LanguagePackManager` lädt und löscht es, `TranslationStore` merkt Übersetzungen als Dateien in `noBackupFilesDir` – Zwischenspeicher, nicht in der Datenbank, nicht in der Sicherung, nie geteilt; der Rundgang ersetzt den Übersetzer über `AppContainer.translationEngine`), `ScreenOn` („Bildschirm in der Rezeptansicht“ in den Einstellungen unter „Kochen“, #68: „Bleibt an“ (Standard), 15 oder 30 Minuten nach dem letzten Tippen, „Wie das Handy“; nur über `keepScreenOn` der Ansicht, nie `WAKE_LOCK`, kein Wecker, kein Hintergrunddienst; jede Berührung startet die Frist neu, `ScreenOnTimer` wird mit nachgestellter Uhr geprüft), `AppPreferences` (kleine Merkwerte, auch die Darstellung und die Bildschirm-Einstellung; beide werden gleich beim Start der App im Hintergrund gelesen), `RecipeRepository` als zentrale Stelle für Speichern und Laden.
- `ui/` – je Bildschirm ein Ordner mit Screen und ViewModel; Navigation Compose mit typsicheren Zielen (`ui/navigation/Routes.kt`). Die gemeinsamen Bausteine hält `AppContainer` (in `MoltobeneApplication.kt`), alles wird erst bei Bedarf erzeugt.
- `androidTest/` – Rundgang durch die App auf dem Emulator (`tour/AppTourTest`): bedient die wichtigsten Wege wie ein Mensch, prüft das Ergebnis und fotografiert jeden Bildschirm, in vier Darstellungen (`DisplayVariant`: hell, dunkel, Schrift 200 %, Englisch). Die Beispielrezepte (`SampleRecipes`) sind selbst geschrieben, ihre Fotos werden gezeichnet. Neue Bildschirme, Dialoge und Wege dort ergänzen; die Nummer im Namen des Fotos gibt die Reihenfolge vor.
- Datei-, Datenbank- und Bildzugriffe laufen nie auf dem Hauptthread. Formulareingaben liegen im `SavedStateHandle`.
- Freigegebene Bibliotheken: Room (mit KSP), Navigation Compose, Coil (ohne Internet-Modul), kotlinx.serialization, ONNX Runtime (führt die Modelle der Texterkennung aus; in Unit-Tests die PC-Fassung; ihre Microsoft-Telemetrie ist dreifach abgeschaltet, #49: Startbaustein und `ACCESS_NETWORK_STATE` im Manifest entfernt, `ORT_DISABLE_TELEMETRY` in `MoltobeneApplication`, Prüfung im Build – auch mit Internet-Berechtigung), jsoup (liest HTML für „Aus Link übernehmen“, #55; geladen wird nicht mit jsoup, sondern mit `HttpsURLConnection` von Android), Robolectric (nur für Tests, z. B. Datenbank-Umbauten mit einer echten Datenbank), AndroidX Test und Compose UI Test (nur für den Rundgang auf dem Emulator). Weitere nur nach Rückfrage.
- Jeder mitgelieferte quelloffene Baustein steht mit Lizenz in `ui/licenses/Licenses.kt` (Lizenztexte unter `assets/licenses/`), angezeigt unter Einstellungen → Info → „Open-Source-Lizenzen“. Neue Bausteine dort ergänzen.
- Texterkennung: Modelle PP-OCRv6 von PaddleOCR (Lizenz Apache 2.0) unter `assets/ocr/` – Zeilensuche „tiny“ (`det.onnx`), Lesen „small“ (`rec.onnx`, kennt Deutsch, Englisch, Italienisch, Französisch, Spanisch und mehr zugleich) und seine Zeichenliste (`keys.txt`); Herkunft: Umwandlung nach ONNX aus dem Paket `onnxocr` 4.0.0 (PyPI, Apache 2.0). Die Zeilen werden auf höchstens 1600 px gesucht, aus dem etwa 2400 px großen Foto gelesen; Einstellungen in `TextDetection` und `PaddleOcr` waren in Tests mit nachgestellten Handyfotos am zuverlässigsten. Native Bibliotheken nur für `armeabi-v7a` und `arm64-v8a`.
- Prüfsummen (#41): Die Modelle der Texterkennung werden per Unit-Test (`ModelDataTest`) gegen feste SHA-256-Werte geprüft. Wird ein Modell ersetzt, bricht der Build ab, bis die neuen Werte bewusst eingetragen sind – vorher mit dem Original vergleichen. ONNX Runtime kommt von Maven Central.
- Sprachpaket Deutsch–Englisch (#60): Modelle OPUS-MT `opus-mt-en-de` und `opus-mt-de-en` der Universität Helsinki (Lizenz CC BY 4.0, mit Namensnennung unter „Open-Source-Lizenzen“ und in `NOTICE.txt` im Paket). Es ist nicht in der App, sondern wird auf Wunsch einmal heruntergeladen (etwa 172 MB, auf dem Handy 274 MB). `.github/workflows/sprachpaket.yml` baut es nachvollziehbar mit festen Werkzeug- und Modellständen: nach ONNX umgewandelt, auf 8 Bit verkleinert, Wortteile als Text; das Paket entsteht bei jedem Bau Byte für Byte gleich. `LanguagePackTest` prüft dort, ob der Übersetzer der App Wort für Wort dasselbe liefert wie das Original (`reference.py`) und ob die App das Paket annimmt. Die App kennt die Prüfsumme des Verzeichnisses (`LanguagePack.MANIFEST_SHA256`), das Verzeichnis die Prüfsumme jeder Datei. Ein geändertes Paket bekommt eine neue `PACK_VERSION`/`LanguagePack.VERSION` und eine neue Prüfsumme. Veröffentlicht wird es mit „Run workflow“ auf `main` (Häkchen „Als Release veröffentlichen“) als eigenes Release `sprachpaket-de-en-<version>`, das nie als neueste Version gilt.

### Daten dürfen nie verloren gehen

- **Datenbank:** Bei jeder Änderung am Aufbau `version` in `MoltobeneDatabase` erhöhen, eine Migration schreiben und testen. Der Build legt den Bauplan als Artefakt „datenbank-schema-…“ ab; die JSON-Datei wird nach `app/schemas/` ins Repository übernommen.
- **Sicherungsdatei:** Alte Sicherungen müssen immer lesbar bleiben (Regeln in `data/backup/BackupFormat.kt`). Die Beispiel-Sicherung unter `app/src/test/resources/backup/` wird nie verändert, nur um neue Formatversionen ergänzt.
- Einzige Berechtigung ist der Internetzugang (#55, vom Projektinhaber am 06.10.2026 freigegeben), nur für „Aus Link übernehmen“ und den Download des Sprachpakets (#60). Neue Berechtigungen nur nach Rückfrage.

### Sicherheit und Datenschutz

- So wenig Daten wie möglich: Inhalte verlassen das Handy nur, wenn Nutzer sie selbst teilen oder sichern.
- Keine Bibliotheken für Werbung, Tracking, Analyse oder Absturzberichte.
- Berechtigungen nur, wenn eine Funktion sie zwingend braucht, nach Rückfrage und mit Begründung als Kommentar im `AndroidManifest.xml`.
- Internet (ab dem Übernehmen von Rezepten) nur über HTTPS und nur für die in der Vision genannten Zwecke. Spätestens mit der ersten Internet-Funktion gibt es eine kurze Datenschutzerklärung.
- Fremde Inhalte gelten als unsicher (übernommene Rezepte, Sicherungs- und Rezeptdateien): Größe begrenzen, Format prüfen, Links nur mit http/https öffnen.
- Sicherheitslücken werden vertraulich gemeldet (`SECURITY.md`, „Private vulnerability reporting“).

### Größere Arbeiten

Umfangreiche Versionen werden auf einem Arbeitszweig entwickelt und dort über „Run workflow“ (`gh workflow run build.yml --ref <zweig>`) gebaut – ohne Release. Erst wenn der Build grün ist, kommt alles zusammengefasst auf `main`.

## Build, Signatur und Veröffentlichung

- Workflow: `.github/workflows/build.yml` läuft bei jedem Push auf `main` und baut eine **signierte Release-APK**.
- Vor dem Bauen prüft er, ob CHANGELOG.md einen Abschnitt für die aktuelle `versionName` mit passendem `versionCode` hat und ob der `versionCode` größer ist als beim letzten Release (#5). Nach dem Bauen prüft er die Signatur gegen den bekannten Fingerabdruck (`SIGNATUR_FINGERABDRUCK` in `build.yml`) und die Berechtigungen der APK gegen die Erlaubt-Liste `ERLAUBT` (#49; heute die app-interne `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` von AndroidX und `INTERNET` für „Aus Link übernehmen“, #55). Verboten bleiben `TelemetryInitializer` und `ACCESS_NETWORK_STATE`. Neue Einträge nur nach Freigabe.
- **Testlauf auf dem Emulator:** Parallel zum Bauen startet der Job `testlauf` ein virtuelles Android-Handy (Android 14, 360 × 760 dp wie ein günstiges Handy, nur Werkzeuge von Google), baut eine Debug-Version ohne Secrets und führt den Rundgang aus `androidTest/` aus. Ergebnisse als Artefakte: „bildschirmfotos-<versionName>“ (je Darstellung ein Ordner) und „testbericht-<versionName>“. Ein Release entsteht nur, wenn auch der Testlauf grün ist.
- **Bildschirmfotos ansehen:** `gh run download <run-id> -n bildschirmfotos-<versionName> -D bildschirmfotos/<versionName>` lädt sie in den Ordner `bildschirmfotos/` (nicht im Repository). Prüfrunden und Besprechungen geben den Spezialisten diesen Ordner mit.
- Gibt es für die aktuelle `versionName` noch kein Release, legt er automatisch das GitHub-Release `v<versionName>` an: APK `moltobene-<versionName>.apk`, Prüfsummen-Datei `.sha256` und ein Herkunftsnachweis von GitHub. Die Beschreibung erzeugt `.github/scripts/release-text.sh` aus Kernsatz, passendem Abschnitt aus CHANGELOG.md, Installationshinweis und Angaben zur Echtheit. Sonst wird nur gebaut.
- Sprachpaket: `.github/workflows/sprachpaket.yml` läuft bei Änderungen an Übersetzer oder Paket auf Arbeitszweigen und auf Wunsch; veröffentlicht nur auf `main` mit Häkchen (siehe „Sprachpaket Deutsch–Englisch“ oben). Das Paket-Release muss vor der App-Version mit der passenden Prüfsumme erscheinen.
- Dependabot (`.github/dependabot.yml`) schlägt monatlich neue Versionen der Bausteine und GitHub-Actions als Pull-Requests vor. Das sind Vorschläge: Übernommen wird nur nach Freigabe, gebaut und geprüft auf einem Arbeitszweig.
- Signatur-Secrets im Repository: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Der Keystore liegt **außerhalb** des Repositorys unter `D:\Claude\Schluessel\moltobene`. Er darf niemals ins Repository gelangen und nicht ersetzt werden, sonst lassen sich Updates nicht mehr installieren.
- Portables JDK (nur für Werkzeuge wie `keytool`): `D:\Claude\Werkzeuge\jdk-21.0.12.1+1`. GitHub CLI: `C:\Program Files\GitHub CLI\gh.exe`.

## Spezialisten-Team und Prüfrunden

- Auf Benutzerebene gibt es neun nur lesende Spezialisten (Subagents): `design`, `funktionalitaet`, `sicherheit`, `kreativitaet`, `konnektivitaet`, `performance`, `texte`, `uebersetzungen` und `marketing` (Marktforschung und Marketing: Konkurrenz-Apps, Vermarktung, Vertriebswege, Preis; seit 09.10.2026, Etikett „Marketing“).
- `/pruefrunde` ruft alle nacheinander auf und legt ihre Vorschläge als GitHub-Issues an (Etikett des Spezialisten + `Priorität: hoch|mittel|niedrig`).
- **Vor jeder Erhöhung der ersten Stelle der Versionsnummer** (z. B. 1.x → 2.0.0) wird **automatisch eine /pruefrunde** durchgeführt, bevor die neue Version veröffentlicht wird.
- **Umgesetzt wird nur, was der Projektinhaber freigibt.** Issues aus Prüfrunden sind Vorschläge, keine Aufträge.
- Erledigte Issues werden im Commit mit `Fixes #12` referenziert (GitHub erkennt nur die englischen Schlüsselwörter), damit sie beim Push auf `main` automatisch geschlossen werden.
- `/besprechung [Thema]` ist eine gemeinsame Besprechung aller Spezialisten: Beiträge, Aussprache untereinander, gemeinsame Rangfolge. Das Protokoll wird unter `docs/besprechungen/` gespeichert. **Issues daraus werden erst angelegt, wenn der Projektinhaber festlegt, welche Punkte übernommen werden.**

## Ablauf nach jeder Änderung

**Ausnahme für reine Dokumentation:** Ändern sich ausschließlich Dokumente (`docs/`, `CLAUDE.md`, `README.md`), entfallen Schritt 1 und 2 – keine neue Versionsnummer, kein CHANGELOG-Eintrag, kein neues Release. Commit, Push und Build-Prüfung (Schritt 3–5) gelten weiterhin. Sobald App-Code, Ressourcen oder Build-Dateien betroffen sind, gilt der volle Ablauf.

1. **CHANGELOG.md** auf Deutsch ergänzen (neuer Eintrag oben, Überschrift `## [<versionName>] – <Datum> (versionCode <n>)`). Der Abschnitt wird zur Beschreibung des Releases, deshalb aus Sicht der Nutzer schreiben, ohne Fachbegriffe (#21). Rein interne Änderungen beginnen mit „Keine sichtbaren Änderungen an der App.“
2. **Versionsnummer erhöhen** in `app/build.gradle.kts`:
   - `versionCode` um **+1** erhöhen.
   - `versionName` nach **Major.Minor.Patch**:
     - Patch (x.y.**Z**): Fehlerbehebungen, Kleinigkeiten
     - Minor (x.**Y**.0): neue Funktionen
     - Major (**X**.0.0): große, grundlegende Änderungen
   - Bei neuen sichtbaren Funktionen „Neu in Version …“ aktualisieren: `whats_new_items` in beiden `strings.xml` sowie `VERSION_CODE` und `VERSION_NAME` in `ui/whatsnew/WhatsNew.kt`.
3. **Commit** mit einer deutschen, verständlichen Beschreibung.
4. **Push** zu GitHub (`main`).
5. **Build auf GitHub prüfen** (GitHub Actions, z. B. mit `gh run list` / `gh run watch`), einschließlich Testlauf auf dem Emulator. Schlägt der Build fehl: Fehler selbstständig analysieren und beheben (jeweils wieder mit Schritt 1–4), **bis der Build grün ist**. Bei sichtbaren Änderungen die Bildschirmfotos der betroffenen Bildschirme ansehen. Erst danach die Aufgabe als erledigt melden.
