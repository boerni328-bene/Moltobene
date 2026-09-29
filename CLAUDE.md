# CLAUDE.md – Arbeitsregeln für das Projekt Moltobene

## Projektbeschreibung

_Platzhalter – wird später ausgefüllt (Zweck der App, Zielgruppe, geplante Funktionen)._

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

## Ablauf nach jeder Änderung

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
