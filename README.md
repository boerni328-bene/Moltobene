# Moltobene

_Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy._

Kostenlos, ohne Werbung, ohne Konto und ohne Tracking. Die App braucht keine einzige Berechtigung und keinen Internetzugang.

## Was die App kann

- Rezepte hinzufügen und bearbeiten: Titel, Foto, Portionen, Zutaten, Zubereitung, Quelle und eigene Notizen
- Rezepte aus Fotos übernehmen: Die Texterkennung liest Titel, Zutaten und Zubereitung aus Fotos von Kochbuch, Zettel oder Bildschirmfoto, auch über mehrere Seiten (Deutsch, Englisch, Italienisch, Französisch, Spanisch)
- Die Sammlung durchsuchen, mit Vorschaubildern
- Beim Kochen bleibt der Bildschirm an
- Einzelne Rezepte teilen: als gut lesbaren Text mit Foto oder als Rezeptdatei im offenen Standard schema.org. Eigene Notizen bleiben dabei privat.
- Die ganze Sammlung inklusive Fotos als Datei sichern und wiederherstellen, z. B. für den Umzug auf ein neues Handy

Geplant sind unter anderem: Rezepte aus Internetseiten und anderen Apps übernehmen sowie eine Kochansicht zum Abhaken und Umrechnen der Portionen.

## Installation

1. Unter [Releases](https://github.com/boerni328-bene/Moltobene/releases/latest) die Datei `moltobene-<Version>.apk` herunterladen.
2. Die Datei auf dem Handy öffnen. Beim ersten Mal fragt Android, ob der Browser bzw. Dateimanager Apps installieren darf; das einmal erlauben.

Voraussetzung ist Android 8.0 oder neuer.

**Updates** werden genauso installiert, die Sammlung bleibt dabei erhalten. Eine Sicherung vorher (Einstellungen → Sammlung sichern) schadet trotzdem nie.

## Echtheit prüfen

Moltobene wird nicht über den Play Store verteilt. Ob eine APK wirklich aus diesem Projekt stammt und unverändert ist, lässt sich auf drei Wegen prüfen:

- **Fingerabdruck des Signaturzertifikats (SHA-256).** Alle Versionen sind mit demselben Schlüssel signiert:

  `2A:8A:99:1C:E9:68:47:D2:0B:AC:DF:74:4F:1E:E8:95:E7:28:35:8B:06:83:EA:99:3E:42:58:65:F6:D5:2C:1A`

  Android installiert ein Update nur, wenn dieser Schlüssel passt. Prüfen lässt er sich mit `apksigner verify --print-certs moltobene-<Version>.apk` aus den Android-Build-Tools.
- **Prüfsumme (SHA-256).** Sie steht in der Beschreibung jedes Releases und liegt dort als Datei `moltobene-<Version>.apk.sha256`. Prüfen unter Windows mit `certutil -hashfile moltobene-<Version>.apk SHA256`, unter Linux mit `sha256sum -c moltobene-<Version>.apk.sha256`.
- **Herkunftsnachweis von GitHub** (ab Version 0.3.1). `gh attestation verify moltobene-<Version>.apk --repo boerni328-bene/Moltobene` bestätigt, dass die Datei in diesem Repository von GitHub Actions gebaut wurde.

## Datenschutz

- Rezepte und Fotos liegen nur auf dem Handy. Keine Cloud, kein Konto, keine Weitergabe an Dritte.
- Die automatische Android-Cloud-Sicherung ist abgeschaltet. Gesichert wird über die eigene Sicherungsdatei.
- Fotos werden verkleinert und ohne Aufnahmeort gespeichert und geteilt.
- Die Texterkennung ([Tesseract](https://github.com/tesseract-ocr/tesseract), quelloffen) ist fest eingebaut und läuft nur auf dem Handy, ohne Internet und ohne Google-Dienste.

## Weiteres

- [Änderungsprotokoll](CHANGELOG.md)
- [Sicherheitslücken melden](SECURITY.md)

## English

_Moltobene collects recipes from every source in one place, stored only on the phone itself._

Free, no ads, no account, no tracking, no permissions and no internet access. The app is available in English and German. To install, download `moltobene-<version>.apk` from [Releases](https://github.com/boerni328-bene/Moltobene/releases/latest) and open it on an Android 8.0+ phone. The signing certificate fingerprint and checksums are listed above under “Echtheit prüfen”.
