# Besprechung vom 09.10.2026: Sprachausgabe und Spracheingabe

**Teilnehmer:** Sprache (neue Rolle für diese Besprechung), Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen, Marketing
**Moderation:** Claude · **App-Version:** 0.27.0

## Anlass (Projektinhaber, wörtlich)

> „Ich finde Grundlegende Sprachfähigkeiten wären auch super, sowohl um Rezepte in der Sammlung zu suchen/aufzurufen und auch Sprachein- und Ausgabe für das Kochen, z.B. Was ist der nächste Schritt als Spracheingabe? Antwort als Sprachausgabe, oder wiederhole das. Es muss keine Großartige KI dahinter stecken sondern nur grundlegende Sprach- und Ausführungsfunktionen.“

> „Datenschutz muss gewahrt bleiben, es soll Offline funktionieren. Mikrofon mit Zustimmung und einem Button den der User zuvor betätigt könnte ich mir vorstellen.“

## Einigkeit (alle zehn Beiträge)

1. **Stufe 1 ohne Mikrofon:** Vorlesen mit der Android-Sprachausgabe (nur Stimmen ohne Netz, Sprache des angezeigten Rezepts, ehrliche Meldung ohne Stimme) und große Tasten „Zurück | Vorlesen/Wiederholen | Weiter“ (+ „Zutaten“). Das beantwortet „Was ist der nächste Schritt?“ und „wiederhole das“ per Tippen. Keine Berechtigung, kein Download.
2. **Grundlage:** „Weiter/Zurück“ im `RecipeViewModel` (heute gibt es nur einen angetippten, markierten Schritt). Vorlesen und Erkennung hinter kleinen Schnittstellen (`SpeechOutput`, `SpeechInput`), im Rundgang durch nachgestellte Fassungen ersetzt.
3. **Stufe 2 Sprachbefehle:** nur offline, nur nach Tippen auf einen Knopf (etwa 5 Sekunden Zuhören), sichtbarer Zustand „Mikrofon an“, nichts gespeichert oder gesendet, fester Wortschatz (weiter, zurück, wiederholen, Zutaten, stopp), nur harmlose Befehle. Die Android-Spracherkennung (`SpeechRecognizer`, Google) und Assistenten (App Actions) werden **nie** genutzt, auch nicht als Rückfall.
4. **Berechtigung `RECORD_AUDIO`** erst mit Stufe 2, nur nach Freigabe des Projektinhabers, mit Erklärung vor der Systemabfrage, Eintrag in der Erlaubt-Liste (`ERLAUBT`) und Ergänzung von Datenschutzerklärung und Vision. Bei Ablehnung bleibt alles andere nutzbar.
5. **Modell nur auf Wunsch laden** (Muster Sprachpaket: Prüfsumme, Workflow, Verwaltung in den Einstellungen); Vorschlag Sprache: Vosk (Apache 2.0) mit festem Wortschatz, Modelle etwa 40–50 MB je Sprache; Alternative sherpa-onnx. **Neue Bibliothek nur nach Rückfrage.** Zahlen sind Schätzungen, nicht gemessen; Genauigkeit in lauter Küche (Dunstabzug) schlecht, deshalb sind Tasten immer der gleichwertige Weg.
6. **Weglassen/später:** Suche per Stimme in der Sammlung und Diktat für Notizen (Tastatur-Diktat des Handys reicht, die App hört nicht mit; später höchstens Mikrofon-Symbol im Suchfeld); Dauer-Mithören und Signalwort; Hintergrunddienst/MediaSession.
7. **Weitere Bausteine:** Kopfhörer-/Medientasten in der Kochansicht (nur im Vordergrund, ohne Berechtigung); Timer mit Ansage (später, Frage Wecker/Dienst getrennt klären); Zahlen und Einheiten fürs Vorlesen aufbereiten („1.000 g“, „½ EL“, „≈“, Test je Sprache); Befehle folgen der Sprache der Oberfläche, Vorlesen der Sprache des Rezepts; TalkBack und eigene Stimme dürfen sich nicht überlagern; Vorlesen verlängert die „Bildschirm bleibt an“-Frist nur beim Befehl.

## Begriffe (Vorschlag Texte, noch festzulegen)
Vorlesen / read aloud · Sprachbefehl / voice command · Knopf „Sprechen“ / “Speak” · Mikrofon / microphone · Stimme / voice. Ein späteres Paket heißt „Erkennungspaket“ / “recognition pack”, nie „Sprachpaket“ (belegt für die Übersetzung).

## Leitplanken (Abnahme)
Richtwerte vorab (Performance): Zuhören höchstens 5 s, Befehl erkannt höchstens 2 s, Speicherspitze höchstens 150 MB, APK-Zuwachs durch Stufe 1 gleich 0, Erkennung nie gleichzeitig mit Texterkennung geladen. Prüfung im Build: keine zusätzlichen Berechtigungen außer der freigegebenen. Rundgang für jeden Zustand in vier Darstellungen.

## Reihenfolge (Empfehlung)
Vor 1.0 haben #57 (Gruppen) und #66 Vorrang. Stufe 1 (Weiter/Zurück, Vorlesen, Kochleiste) ist klein und kann früh kommen; Stufe 2 erst nach Freigabe von Berechtigung und Bibliothek, vorzugsweise nach einem Test auf einem echten Android-8-Handy.

## Entscheidungen des Projektinhabers (offen)
1. Stufe 1 (Vorlesen + große Tasten, ohne Mikrofon) zuerst bauen?
2. Stufe 2 (Sprachbefehle offline, Download etwa 50 MB je Sprache, neue Berechtigung Mikrofon) grundsätzlich gewünscht, mit Test vorab?
3. Kopfhörertasten und Timer mit Ansage: gewünscht?
