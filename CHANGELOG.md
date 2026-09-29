# Änderungsprotokoll

Alle wichtigen Änderungen an der App werden hier festgehalten.
Die neueste Version steht oben. Versionsschema: Major.Minor.Patch.

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
