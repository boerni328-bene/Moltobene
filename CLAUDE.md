# CLAUDE.md – Arbeitsregeln für das Projekt Moltobene

## Projektbeschreibung (Vision)

Festgelegt vom Projektinhaber am 29.09.2026 (siehe `docs/besprechungen/2026-09-29-vision-der-app.md`). Änderungen nur mit seiner Zustimmung. Neue Vorschläge werden an dieser Vision gemessen.

**Kernsatz** (wörtlich gleich in README, GitHub und Releases): _Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy._

**Für wen:** Alle, die gern kochen. Öffentlich und kostenlos, verteilt über GitHub Releases.

### Kernfunktionen

1. **Rezept hinzufügen:** Rezepte selbst eintippen (Titel, Zutaten, Zubereitung, eigenes Foto).
2. **Rezept übernehmen** aus anderen Quellen, in dieser Reihenfolge:
   1. Internetseiten und Koch-Portale (Teilen aus dem Browser oder Link einfügen); geteilter Text aus beliebigen Apps (z. B. WhatsApp, E-Mail) und Zwischenablage.
   2. Foto oder Bildschirmfoto von Kochbuch, Zettel oder PDF mit Texterkennung.
   3. YouTube (Videobeschreibung; nur als bestmöglicher Versuch ohne Zusage) sowie Dateien oder andere Rezept-Apps.
   Weitere Erfassungswege sind erwünscht und werden von den Spezialisten vorgeschlagen.
3. **Sammlung finden und ordnen:** suchen, sortieren, einordnen.
4. **Mit dem Rezept kochen:** Bildschirm bleibt an, aus Armlänge lesbar, Zutaten und Schritte abhakbar, Portionen umrechnen, eigene Notizen zum Rezept.
5. **Teilen, sichern und umziehen:** einzelne Rezepte teilen; die ganze Sammlung inklusive Fotos als Datei sichern, wiederherstellen und auf ein neues Handy umziehen.

### Bewusst nicht

Keine Werbung, kein Konto oder Anmelden, kein soziales Netzwerk, kein Tracking, keine Cloud, keine Weitergabe von Daten an Dritte.

### Grundsätze

- **Alles lokal:** Alle Inhalte liegen nur auf dem Gerät. Die automatische Android-Cloud-Sicherung ist abgeschaltet; gesichert wird über die eigene Sicherungsdatei (direkter Umzug von Handy zu Handy bleibt erlaubt).
- **Internet nur auf Wunsch:** Nur wenn der Nutzer ein Rezept übernehmen lässt oder in den Einstellungen „Nach neuer Version suchen“ antippt. Keine automatische Update-Prüfung, keine sonstigen Verbindungen.
- **Nichts geht verloren:** Jede Übernahme endet in einem bearbeitbaren Entwurf. Klappt sie nicht (kein Netz, Seite nicht lesbar), bleiben Link bzw. Text als Entwurf erhalten. Die App weist ehrlich darauf hin, dass übernommene Inhalte geprüft werden sollten.
- **Quelle:** Übernommene Rezepte behalten immer ihre Quelle (Link, Buch), sie wird angezeigt und beim Teilen mitgegeben.
- **Texterkennung** läuft ausschließlich auf dem Gerät und ist in die App eingebaut (funktioniert auch ohne Google-Dienste).
- **Fotos:** werden beim Speichern verkleinert; Listen zeigen nur Vorschaubilder; beim Teilen und Sichern wird der Aufnahmeort entfernt. Jedes Rezept sieht auch ohne Foto vollständig aus.
- **Offene Formate:** Einzelrezepte werden im Standard schema.org/Recipe geteilt; die Sicherung ist eine Datei mit Rezepten, Fotos und Formatversion. Alte Sicherungen bleiben immer lesbar.
- **Sprache:** Oberfläche auf Deutsch und Englisch (Englisch als Rückfallsprache), weitere Sprachen später. Rezepte bleiben in ihrer Originalsprache (keine automatische Übersetzung). Mengen und Einheiten werden wie im Original übernommen, eine Umrechnung kann später folgen.
- **Texte:** ohne direkte Anrede, neutral und knapp („Rezept hinzufügen“, „Rezept wirklich löschen?“, „Noch keine Rezepte vorhanden“).
- **Feste Begriffe:** Rezept, Sammlung, Quelle; *hinzufügen* (neues Rezept), *übernehmen* (aus einer Quelle, nicht „importieren“), *teilen* (an andere), *sichern / wiederherstellen* (ganze Sammlung, nicht „Backup“/„Export“), *Handy*.
- **Gestaltung:** modern und schlicht, Fotos der Gerichte im Mittelpunkt, eigene ruhige Farbpalette. Voll nutzbar mit großer Schrift und Screenreader.
- **Leistung:** Auch mit 1.000 Rezepten mit Fotos bleibt die App auf einem günstigen Android-8-Handy beim Start, Blättern und Suchen flüssig.
- **Fokus:** Neue Funktionen müssen zu einer Kernfunktion passen. Lieber eine Funktion fertig als mehrere halb.

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
- Git-Commits verwenden die GitHub-noreply-Adresse (im Repository lokal eingestellt). Keine privaten E-Mail-Adressen, Passwörter, Schlüssel oder Keystores ins Repository, denn es ist **öffentlich**.

## Build, Signatur und Veröffentlichung

- Workflow: `.github/workflows/build.yml` läuft bei jedem Push auf `main` und baut eine **signierte Release-APK**.
- Gibt es für die aktuelle `versionName` noch kein Release, legt er automatisch das GitHub-Release `v<versionName>` an (APK `moltobene-<versionName>.apk`, Beschreibung = passender Abschnitt aus CHANGELOG.md). Sonst wird nur gebaut.
- Signatur-Secrets im Repository: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Der Keystore liegt **außerhalb** des Repositorys unter `D:\Claude\Schluessel\moltobene`. Er darf niemals ins Repository gelangen und nicht ersetzt werden, sonst lassen sich Updates nicht mehr installieren.
- Portables JDK (nur für Werkzeuge wie `keytool`): `D:\Claude\Werkzeuge\jdk-21.0.12.1+1`. GitHub CLI: `C:\Program Files\GitHub CLI\gh.exe`.

## Spezialisten-Team und Prüfrunden

- Auf Benutzerebene gibt es acht nur lesende Spezialisten (Subagents): `design`, `funktionalitaet`, `sicherheit`, `kreativitaet`, `konnektivitaet`, `performance`, `texte`, `uebersetzungen`.
- `/pruefrunde` ruft alle nacheinander auf und legt ihre Vorschläge als GitHub-Issues an (Etikett des Spezialisten + `Priorität: hoch|mittel|niedrig`).
- **Vor jeder Erhöhung der ersten Stelle der Versionsnummer** (z. B. 1.x → 2.0.0) wird **automatisch eine /pruefrunde** durchgeführt, bevor die neue Version veröffentlicht wird.
- **Umgesetzt wird nur, was der Projektinhaber freigibt.** Issues aus Prüfrunden sind Vorschläge, keine Aufträge.
- Erledigte Issues werden im Commit referenziert (z. B. `Behebt #12` bzw. `Fixes #12`), damit GitHub sie beim Push auf `main` automatisch schließt.
- `/besprechung [Thema]` ist eine gemeinsame Besprechung aller Spezialisten: Beiträge, Aussprache untereinander, gemeinsame Rangfolge. Das Protokoll wird unter `docs/besprechungen/` gespeichert. **Issues daraus werden erst angelegt, wenn der Projektinhaber festlegt, welche Punkte übernommen werden.**

## Ablauf nach jeder Änderung

**Ausnahme für reine Dokumentation:** Ändern sich ausschließlich Dokumente (`docs/`, `CLAUDE.md`, `README.md`), entfallen Schritt 1 und 2 – keine neue Versionsnummer, kein CHANGELOG-Eintrag, kein neues Release. Commit, Push und Build-Prüfung (Schritt 3–5) gelten weiterhin. Sobald App-Code, Ressourcen oder Build-Dateien betroffen sind, gilt der volle Ablauf.

1. **CHANGELOG.md** auf Deutsch ergänzen (neuer Eintrag oben, mit Version, Datum und versionCode).
2. **Versionsnummer erhöhen** in `app/build.gradle.kts`:
   - `versionCode` um **+1** erhöhen.
   - `versionName` nach **Major.Minor.Patch**:
     - Patch (x.y.**Z**): Fehlerbehebungen, Kleinigkeiten
     - Minor (x.**Y**.0): neue Funktionen
     - Major (**X**.0.0): große, grundlegende Änderungen
3. **Commit** mit einer deutschen, verständlichen Beschreibung.
4. **Push** zu GitHub (`main`).
5. **Build auf GitHub prüfen** (GitHub Actions, z. B. mit `gh run list` / `gh run watch`). Schlägt der Build fehl: Fehler selbstständig analysieren und beheben (jeweils wieder mit Schritt 1–4), **bis der Build grün ist**. Erst danach die Aufgabe als erledigt melden.
