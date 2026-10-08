<img src="docs/logo/moltobene-icon-512.png" alt="Logo von Moltobene: ein weißes „MB“ mit Lächeln auf Blau, rundherum Kochbuch, Foto, Kamera, Internet, Video und Datei" width="128" align="right">

# Moltobene

_Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy._

Kostenlos, ohne Werbung, ohne Konto und ohne Tracking. Ins Internet geht die App nur auf Wunsch: wenn ein Rezept aus einem Link übernommen oder das Sprachpaket für die Übersetzung heruntergeladen wird. Dafür braucht sie als einzige Berechtigung den Internetzugang.

> **Hinweis zu Version 0.14.0:** Sie enthielt unbemerkt einen Baustein von Microsoft, der nach jeder Texterkennung Nutzungsdaten an Microsoft gesendet hat (in einer Prüfung nachgewiesen, [#49](https://github.com/boerni328-bene/Moltobene/issues/49)), und dafür die Internet-Berechtigung. Ab Version 0.14.1 ist dieser Baustein entfernt; jede neue Version wird beim Bauen darauf geprüft. Bitte auf die neueste Version aktualisieren.

## Was die App kann

- Rezepte hinzufügen und bearbeiten: Titel, Foto, Portionen, Zutaten, Zubereitung, Quelle und eigene Notizen
- Rezepte aus Internetseiten übernehmen: Link einfügen oder die Seite aus dem Browser an Moltobene teilen. Titel, Zutaten, Zubereitung, Portionen, Zeiten und Foto kommen ins Rezept, die Quelle bleibt erhalten.
- Rezepte aus YouTube-Videos übernehmen (bestmöglicher Versuch): Die App liest das Rezept aus der Videobeschreibung. Ihre Links stehen zur Auswahl, so lässt sich das ganze Rezept auf Wunsch von einer Internetseite übernehmen. Der Link zum Video bleibt getrennt von der Quelle unter „Video“ gespeichert.
- Rezeptdateien übernehmen: Dateien, die jemand aus Moltobene geteilt hat, oder Rezepte im offenen Standard schema.org direkt aus WhatsApp, E-Mail oder dem Dateimanager öffnen – ohne Internet, mit Foto
- Rezepte aus Fotos übernehmen: Die Texterkennung liest Titel, Zutaten und Zubereitung aus Fotos von Kochbuch, Zettel oder Bildschirmfoto, auch über mehrere Seiten (Deutsch, Englisch, Italienisch, Französisch, Spanisch)
- Rezepte aus Text übernehmen: eingefügt aus Nachrichten, E-Mails oder Notizen oder per „Teilen mit…“ aus anderen Apps; Titel, Zutaten und Zubereitung werden automatisch eingeordnet
- Die Sammlung durchsuchen, mit Vorschaubildern
- Mit dem Rezept kochen: Der Bildschirm bleibt an, Zutaten lassen sich abhaken, der aktuelle Schritt markieren und die Portionen umrechnen
- Rezepte übersetzen (Deutsch ↔ Englisch): direkt auf dem Handy und ohne Internet, nachdem einmal das Sprachpaket heruntergeladen wurde (etwa 172 MB). Das Original bleibt erhalten, Abhaken und Portionen umrechnen gehen auch in der Übersetzung.
- Einzelne Rezepte teilen: als gut lesbaren Text mit Foto oder als Rezeptdatei im offenen Standard schema.org. Eigene Notizen bleiben dabei privat.
- Die ganze Sammlung inklusive Fotos als Datei sichern und wiederherstellen, z. B. für den Umzug auf ein neues Handy
- Darstellung wählen: hell, dunkel oder wie das Handy, in fünf ruhigen Farbwelten (Schiefer, Kobalt, Salbei, Terrakotta, Safran)

Geplant sind unter anderem Schlagwörter und Favoriten.

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
- Die Texterkennung ([PP-OCRv6](https://github.com/PaddlePaddle/PaddleOCR) mit [ONNX Runtime](https://onnxruntime.ai), beides quelloffen) ist fest eingebaut und läuft nur auf dem Handy, ohne Internet und ohne Google-Dienste.
- Ins Internet geht die App nur auf Wunsch, bei „Aus Link übernehmen“ und beim Herunterladen des Sprachpakets. Bei „Aus Link übernehmen“ wird genau diese eine Seite geladen und, falls vorhanden, ihr Foto. Die Seite erfährt dabei – wie bei jedem Browser – die IP-Adresse des Handys, die Uhrzeit und die eingestellte Sprache. Die Verbindung ist immer verschlüsselt, Cookies werden nicht gespeichert, Angaben zum Handy werden nicht mitgeschickt. Dieselbe kurze Datenschutzerklärung steht in der App unter Einstellungen → Info → „Datenschutz“.
- Das Sprachpaket für die Übersetzung kommt von dieser Projektseite auf GitHub; GitHub erfährt dabei die IP-Adresse des Handys und die Uhrzeit. Rezepte werden nie gesendet: Übersetzt wird vollständig auf dem Handy, mit den quelloffenen Modellen [OPUS-MT](https://huggingface.co/Helsinki-NLP) der Universität Helsinki (CC BY 4.0).

## Weiteres

- [Änderungsprotokoll](CHANGELOG.md)
- [Sicherheitslücken melden](SECURITY.md)

## English

_Moltobene collects recipes from every source in one place, stored only on the phone itself._

Free, no ads, no account, no tracking. The app only goes online on request: when a recipe is imported from a link or the language pack for translation is downloaded. The internet permission is the only one it needs. Note: version 0.14.0 unintentionally contained a Microsoft component that sent usage data to Microsoft after each text recognition (verified in a test, [#49](https://github.com/boerni328-bene/Moltobene/issues/49)), along with the internet permission. The component was removed in 0.14.1, and every build is now checked for it; please update to the latest version. The app is available in English and German. To install, download `moltobene-<version>.apk` from [Releases](https://github.com/boerni328-bene/Moltobene/releases/latest) and open it on an Android 8.0+ phone. The signing certificate fingerprint and checksums are listed above under “Echtheit prüfen”.
