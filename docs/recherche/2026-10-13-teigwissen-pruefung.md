# Prüfbericht zu „Teigwissen Etappe 1“ und Teigrechner (13.10.2026) – Umsetzung

Der Prüfbericht (vom Projektinhaber am 13.10.2026 vorgelegt) bewertet die Hinweise als brauchbare Grundlage mit Nachbesserungsbedarf. Stand der Umsetzung in Version 0.39.1:

| Punkt des Berichts | Priorität | Umsetzung |
|---|---|---|
| Fünf-Minuten-Regel (Salz/Hefe) als Verfahrensvorgabe kennzeichnen | hoch | Aus den Hinweisen entfernt, in der Quellenliste als Verfahren des Pizzaverbands vermerkt |
| Wasseranteil und Vorteig-Anteil eindeutig definieren, Vorteig in der Gesamtbilanz | hoch | Wasseranteil als „Wasser in % vom gesamten Mehl“ im Text; Vorteig-Anteil als „des gesamten Mehls“; Rechner zählt Vorteig zur Gesamtmenge (war schon so, durch Test belegt) |
| 5–6 Tage Kühlschrank / Kälte „erzeugt“ Aroma | hoch | Feste Grenze entfernt; „Kälte verlangsamt, stoppt nicht“; Haltbarkeit hängt von mehreren Größen ab |
| Direkte Quellenlinks | hoch | Links je Aussage in `2026-10-13-teigwissen-hinweise.md`; Prüfung im Volltext und Abrufdatum bleiben offen |
| Ölaussagen differenzieren, keine Ursache-Wirkungs-Kette | mittel | Neutral gefasst, Brot und Pizza getrennt, Öl vs. feste Fette; Frischhaltung ohne Garantie |
| Deutsche und englische Texte gegeneinander prüfen | mittel | Beide Sprachen gleichzeitig überarbeitet; `DoughHelpTextsTest` prüft Vorhandensein, Platzhalter, Länge, Anrede und die neuen Leitplanken |
| Vorteig: Säure/Frischhaltung, Reifezeit mit Temperatur, Überreife, Sauerteig ≠ Poolish | mittel | Umgesetzt (Biga 8–16 h bei 15–20 °C, Überreife-Anzeichen, Hinweis auf Sauerteig) |
| Presets als Startpunkte kennzeichnen | Regel | Hinweistext der Karte „Voreinstellung“ angepasst |
| Presets, Regler, Eingabe gemeinsam planen | mittel | Bereits umgesetzt (Version 0.36.0 bis 0.38.0); Zahlenwert immer sichtbar, Plus/Minus und Eingabe per Tastatur |

## Noch nicht umgesetzt (Vorschläge des Berichts, zur Entscheidung)
- Beim Wechsel einer Voreinstellung **nachfragen**, wenn eigene Änderungen vorliegen.
- **Ungewöhnliche Kombinationen** sichtbar markieren (ohne sie zu verbieten).
- **Gehzeit-Rechner** aus Hefetyp, Teigtemperatur, Kühlschranktemperatur und Wunschzeit, als Spanne und mit „Näherung“ gekennzeichnet (Teigwissen Etappe 4).
- Texte von jemandem mit Backerfahrung **gegenlesen** lassen und die Quellenliste um Abrufdatum und Volltextprüfung ergänzen.
- Die Release-Dokumentation soll künftig genau sagen, was geprüft wurde (Build, Rundgang in vier Darstellungen, fachliche Freigabe ist getrennt davon).
