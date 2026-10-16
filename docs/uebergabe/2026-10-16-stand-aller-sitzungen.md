# Moltobene – Übergabe: Stand aller Sitzungen (16.10.2026, App-Version 0.48.0)

Zum Kopieren in eine neue Sitzung. Alles Wesentliche steht hier; Regeln im Detail stehen in `CLAUDE.md` (wird beim Start automatisch gelesen).

## 1. Projekt in drei Sätzen
Moltobene ist eine Android-App (Kotlin, Jetpack Compose, `com.moltobene.app`, ab Android 8): Sie sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem Handy. Kostenlos, GPL-3.0, Verteilung über GitHub Releases (Repository `boerni328-bene/Moltobene`, öffentlich), später IzzyOnDroid/F-Droid. Gebaut wird **nur über GitHub Actions** (kein Android Studio, kein Java auf dem PC).
Der Projektinhaber ist **kein Entwickler**: immer Deutsch, kurz und verständlich, vor größeren Entscheidungen fragen.

## 2. Arbeitsweise (hat sich bewährt)
- Eigener Arbeitszweig je Aufgabe in einem Git-Worktree im Scratchpad: `git worktree add -b <zweig> <pfad> origin/main`; bauen mit `gh workflow run build.yml --ref <zweig>`, auf grün warten, **Bildschirmfotos ansehen** (`gh run download <run-id> -n bildschirmfotos-<version> -D bildschirmfotos/<version>`), dann Squash-Merge auf `main` mit `Fixes #<Nr>`, Zweig und Worktree löschen, Build und Release auf `main` prüfen.
- Vor jedem Commit/Push/Versionssprung **`git fetch origin` und `origin/main` prüfen**: Es arbeitet oft eine zweite („Remote“-)Sitzung parallel an `main`. Versionsnummer und `versionCode` kollidieren dann; beim Zusammenführen CHANGELOG, `app/build.gradle.kts`, `WhatsNew.kt` und die `whats_new_items` in beiden `strings.xml` per Hand auflösen und die eigene Version hochsetzen. Nie parallel an denselben Dateien (Kochleiste, Teigrechner) arbeiten.
- Ablauf nach jeder Änderung: CHANGELOG (deutsch, aus Nutzersicht) → Version erhöhen (`versionCode` +1; Patch = Fehler, Minor = neue Funktion) → bei sichtbaren neuen Funktionen „Neu in Version …“ (`whats_new_items` beide Sprachen, `VERSION_CODE/NAME` in `ui/whatsnew/WhatsNew.kt`; bei reinen Fehlerbehebungen **nicht** anfassen) → Commit → Push → Build grün (Build, Emulator-Rundgang `AppTourTest` in 4 Darstellungen: hell, dunkel, Schrift 200 %, Englisch, und Release). Reine Dokumentation: keine Version, kein CHANGELOG.
- Jeder neue Text kommt in **beide** `strings.xml` (Englisch amerikanisch, Deutsch ohne direkte Anrede); feste Begriffe stehen in `CLAUDE.md`. Farben/Schrift/Ecken/Abstände nur über Theme und `Spacing`.
- Werkzeug-Hinweise (Windows/Git Bash): Mehrzeilige Heredocs mit Umlauten gehen manchmal schief → Datei mit dem Write-Werkzeug schreiben, dann per `sed`/`cat` einfügen. `gh` liegt unter `C:\Program Files\GitHub CLI\gh.exe`. Spezialisten sind nur lesende Subagents (`design`, `funktionalitaet`, `sicherheit`, `kreativitaet`, `konnektivitaet`, `performance`, `texte`, `uebersetzungen`, `marketing`, `sprache`); `/besprechung` und `/pruefrunde` rufen alle auf; Issues erst nach Freigabe anlegen.
- Entscheidungen des Projektinhabers stehen in `CLAUDE.md` und in `docs/besprechungen/` (Protokolle). Die Memory-Dateien (`memory/`) kennen nur: parallele Sitzungen im selben Ordner und eine Idee zur Rezeptsuche.

## 3. Versionsverlauf dieses Strangs (jüngste zuerst)
| Version | Inhalt |
|---|---|
| 0.48.0 | Spracheingabe (#80): Mikrofon-Knopf in der Kochleiste, Erkennungspaket (≈46 MB, einmalig herunterladen) – Remote-Sitzung |
| 0.47.1–0.47.3 | Kochleiste umgebaut (Zurück/Weiter nebeneinander, Vorlesen als Lautsprecher-Symbol ganz rechts, „Zutaten vorlesen“ entfällt), Übersetzungsbalken fest oben – Remote |
| 0.47.0 / 0.46.1 | Kopfhörertasten-Belegung einstellbar; Erkennungspaket-Bau – Remote |
| **0.46.0** | **Teigrechner: eine Bedienung pro Wert (Regler + antippbare Zahl), Plus/Minus nur bei der Anzahl, „Standard 250 g“ + „Auf Standard“ nur bei Abweichung (#86)** |
| **0.45.0** | **Kochleiste: „Schritt 2 von 5“, „Weiter“ betont, Balken am aktuellen Schritt, Foto niedriger (≤ 5:3) (#89)** |
| **0.44.0** | **Hinweiskarte (`NoticeCard`): Prüfhinweis oben im Bearbeiten, Fehler beim Übernehmen als bleibende Karte mit „Erneut versuchen“, Übernehmen-Chips umbrechen (#90)** |
| **0.43.0** | **Startseite: „Was koche ich heute?“ als Foto-Karte, „Sammlung“ mit letzten Rezepten als Bilder, Teigrechner/Grundrezepte nebeneinander (#87)** |
| 0.42.x–0.39.0 | Teigwissen (Hinweise „Was bewirkt das?“, Artikel zu Mehltypen/Getreide/Gluten, Info-Symbol im Teigrechner), Vorlesen: Stimme/Tempo/Tonhöhe, Kopfhörertasten – Remote |
| **0.38.1** | **Meldungen bei großer Schrift lesbar (`AppSnackbarHost`, Aktion unter dem Text) (#88)** |
| 0.38.0–0.33.0 | Voreinstellungen im Teigrechner, Regler, Plus/Minus, Standardwerte, Grundrezepte, Schlagwörter und Filter, Kochleiste mit Vorlesen, Startseite mit Kacheln – Remote |
| **0.32.0** | **Teigrechner Pasta (Eierteig, Wasserteig) und Kuchen (6 Arten, Mehl aus Backform) (#70)** |
| **0.31.0** | **Design-Umbau: Foto-Karten-Sammlung, Hauptknopf „Hinzufügen“, Filter, Favoriten, Teigrechner in Karten mit fester Ergebnisleiste** |
| **0.30.0** | **Teigrechner Pizza/Brot mit Vorteig, Entwurf als Rezept (#69)** |
| 0.29.0 und früher | Backform umrechnen (#67), Umrechnen nach Zutat (#66), Vorlesen/Kochen, Prüflauf (#52 erster Teil), … |

(Fett = in dieser Sitzung gebaut; alles andere hat die Remote-Sitzung veröffentlicht.)

## 4. Aufbau des Teigrechners (wichtig für weitere Arbeit)
`data/dough/`: `DoughModel` (Teigart `pizza/bread/pasta/cake`, Hefe `fresh/instant_dry/active_dry`, Vorteig `none/biga/poolish/sourdough`), `DoughLimits` (nur für Pizza/Brot), `DoughCalculator` (Pizza/Brot mit Vorteig), `PastaCakeModel/Calculator/Memory` (Pasta, Kuchen; alle Prozente vom gesamten Mehl; Kuchenmehl aus Backform über `PanScaling`, Bezug Ø 26 cm), `DoughDraft` (Ergebnis → Rezept-Entwurf, Quelle „eigenes Rezept“, Zutatenzeilen mit Menge vorn und „(xx %)“, Portionsangabe so, dass `RecipeYield`/`PanScaling.parse` sie lesen), `DoughMemory` (gemerkte Werte, beim Laden gegen Grenzen geprüft), `DoughPresets`, `DoughStepper`. Oberfläche: `ui/dough/DoughScreen.kt` (Teigart-Schalter, Karten, `NumberField` mit Regler, feste `ResultBar` unten), `PastaCakeForms.kt`, `PresetCard.kt`, `DoughHelp.kt`. Rechenlogik bleibt in `data/` mit Unit-Tests; die Oberfläche gibt nur Werte weiter. Rundgang-Test `AppTourTest.teigrechner*`.

## 5. Design-Besprechung vom 12.10.2026 (Protokoll `docs/besprechungen/2026-10-12-design-verbessern.md`)
Rang 1–5 (alle erledigt): #86 Teigrechner, #87 Startseite, #88 Meldungen, #89 Kochansicht, #90 Hinweiskarte.
**Noch offen aus der Besprechung:**
- Rang 6: Messbasis – Prüflauf wie beim Nutzer auf Android 8 mit 1.000 Rezepten (**#52**); Baseline-Profil „prüfen“ (braucht zusätzliches Gradle-Modul, vorher mit dem Projektinhaber abstimmen).
- Rang 7: Regel gegen Abschneiden steht in `CLAUDE.md`; **noch offen:** Rundgang-Test mit Pseudosprache (`en-XA`, `pseudoLocalesEnabled` im Debug-Build) oder künstlich langen Texten.
- Rang 8: Datenschutz-Zusage sichtbar – kleiner Halbsatz im Link-Dialog („ohne Cookies, ohne Anmeldung“); Info → Datenschutz besteht schon.
- Rang 9: nur noch „Verlauf unter der Leiste in der Rezeptansicht“ (Foto unter die Leiste, einfacher Zwei-Farben-Verlauf, kein Blur).
- Rang 10: Store-Fotos (#73) **ganz zum Schluss**, mit selbst fotografierten Gerichten des Projektinhabers (ohne Aufnahmeort, keine erkennbaren Personen); Markennamen „Molto Bene“ vor dem Store-Start im Markenregister prüfen lassen.
**Entscheidungen des Projektinhabers (12.10.):** Teigrechner = Regler + Zahl, Plus/Minus nur bei kleinen Zahlen; Foto beim Kochen niedriger statt einklappbar; „Zutaten“ bei Schrift 200 % ins Menü (inzwischen von der Remote-Sitzung anders gelöst: Knopf entfällt); bei TalkBack liest die App **selbst** vor (zu prüfen auf echtem Handy).

## 6. Offene Issues (Stand 16.10.)
#16 neue Version melden · #47 PDF-Seiten lesen · #49 (erledigt seit 0.14.1, schließen) · #52 Prüflauf Android 8 · #57 Etappe „Finden und ordnen“ + Abnahmeliste 1.0 · #58 Texte-Durchgang (Leerzustand, Kernsatz in der App) · #59 App kleiner (nach 1.0) · #61 Große Schrift und Notiz beim Kochen · #62 Übersetzen: Sprache beim Bearbeiten ändern, auf echtem Handy messen · #71 Gehzeiten „fertig um …“ (später Uhr-App) · #73 Store-Auftritt · #74 IzzyOnDroid/F-Droid zu 1.0 · #75 Google-Entwicklerverifizierung (später, als Privatperson, erst nach Verbesserung/Test) · #76 Umzug aus anderen Rezept-Apps · #77/#78 Fassungen und Vergleichen · #79/#80/#85 Sprache (großenteils durch die Remote-Sitzung umgesetzt, Issues ggf. schließen) · #81/#82/#83/#84 (durch Remote umgesetzt, prüfen und schließen).
Offene Fragen an den Projektinhaber: Baseline-Profil (zusätzliches Modul) ja/nein-Zeitpunkt; Zeitpunkt der Store-Fotos.

## 7. Vision und feste Grenzen (Kurzfassung; Vollständiges in `CLAUDE.md`)
Alles lokal, kein Konto, keine Werbung/kein Tracking, Internet nur auf Wunsch (Rezept übernehmen, Sprachpaket/Erkennungspaket laden, „Nach neuer Version suchen“). Spende nur im README, Store-Eintrag und unter Einstellungen → Info, nie aktiv. Jede Übernahme endet in einem bearbeitbaren Entwurf und behält die Quelle. Leistung: 1.000 Rezepte mit Fotos flüssig auf günstigem Android-8-Handy (keine Blur-/Schatten-Effekte, nur Vorschaubilder). Neue Berechtigungen, Bibliotheken und Architekturänderungen nur nach Rückfrage.

## 8. Nützliche Pfade
- `CLAUDE.md` (Regeln), `CHANGELOG.md`, `docs/besprechungen/`, `docs/recherche/`, `docs/marktberichte/2026-10-09-marktbericht.md`.
- `ui/components/` gemeinsame Bausteine: `AppTitle`, `NoticeCard` (Hinweiskarte), `AppSnackbarHost`, `RecipePhoto`, `OptionButton`, `LargeText.isLargeText()`.
- `ui/collection/CollectionParts.kt` (Karten, Filter, Hinzufügen-Auswahl), `ui/home/HomeScreen.kt`, `ui/recipe/RecipeScreen.kt` (Kochleiste, Schritte), `ui/edit/EditScreen.kt`, `androidTest/tour/AppTourTest.kt` (Rundgang, Fotonamen = Reihenfolge).
- Schlüssel/Keystore: `D:\Claude\Schluessel\moltobene` (nie ins Repository).
