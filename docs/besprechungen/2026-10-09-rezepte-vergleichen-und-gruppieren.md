# Besprechung vom 09.10.2026: Rezepte suchen, vergleichen, Fassungen und Gruppen

**Teilnehmer:** Design, Funktionalität, Sicherheit, Kreativität, Konnektivität, Performance, Texte, Übersetzungen, Marketing
**Moderation:** Claude · **App-Version:** 0.27.0 (versionCode 48)

## Anlass (Projektinhaber, wörtlich)

> „Ich hätte gerne eine Recherchefunktion, um neue oder bestehende Rezept auf Internetplattformen, Blogs, Foren usw. zu suchen und zu vergleichen und mir dann entweder die beste Version auszusuchen, oder aus allen gefundenen Rezepten mir das beste für mein Rezept herauszunehmen. Evtl. wären auch mehrere Versionen eines Rezeptes denkbar. Die Gruppierung verschiedener Rezept wäre auch sinnvoll, z.B. Saucen, Pastagerichte, Aufläufe, Desserts, ... usw.“

> „Aber eine Rezeptvergleich auf Verschiedenen Plattformen wäre doch auch ohne KI denkbar.“

> Szenario: „Ich möchte z.B. eine Bolognese-Sauce machen. Mit dieser Suchfunktion sollen die Rezept-Links von z. B. den 30 besten Treffern von zuvor definierten Plattformen angeführt werden.“

## Entscheidung des Projektinhabers (09.10.2026)

> „OK, die Suche über mehrere Portale machen wir nicht. Aber mehrere gleiche oder ähnliche Rezept möchte ich schon gerne vergleichen und eine Gruppierungsfunktion und verschiedene Versionen wären dann auch hilfreich.“

- **Keine eigene Suche** in Portalen, Suchdiensten oder Foren (Wege B und C). Die Vision bleibt unverändert: „Internet nur auf Wunsch“, keine Weitergabe von Suchwörtern.
- **Ja:** mehrere gleiche oder ähnliche Rezepte vergleichen, Gruppierung, mehrere Fassungen eines Rezepts.

## Gemeinsame Linie der Beiträge

1. **Gruppen = Schlagwörter (#57):** mitgelieferte Vorschläge mit fester englischer Kennung (z. B. `sauce`, `pasta`, `casserole`, `dessert`, `soup`, `salad`), Filterzeile in der Sammlung, keine zweite Ebene „Kategorie“ (Beschluss vom 29.09.2026). Beim Übernehmen wird ein Schlagwort höchstens vorgeschlagen, nie still gesetzt.
2. **Fassungen:** jede Fassung ist ein normales Rezept mit Verweis auf das Ausgangsrezept (Datenbank-Version 4, Migration und Test; Fotos nie geteilt oder kopiert ohne Besitzer-Regel; Verknüpfungen beim Wiederherstellen prüfen, keine Kreisverweise). Suchen, Kochen, Umrechnen, Übersetzen, Teilen und Sichern funktionieren damit sofort.
3. **Vergleich ohne KI:** 2 bis 4 Rezepte aus der Sammlung (auch Entwürfe), gleiche Portionen oder gleiche Menge einer Zutat (#66), eine Spalte „Zutat für Zutat“, Fassungen mit Buchstaben A–D statt Farben, „in 2 von 4 Rezepten“. Zuordnung nur bei gleichem Namen innerhalb einer Sprache; Unsicheres steht einzeln. Einheiten werden nie gleichgesetzt. Die App bewertet nie („beste“, Sterne).
4. **Eigene Fassung zusammenstellen:** Zeilen antippen, daraus entsteht ein Entwurf mit allen Quellen („Zusammengestellt aus …“, beim Teilen schema.org `isBasedOn`). Nur Rezepte derselben Sprache mischen.
5. **Rezepte kommen wie heute:** Seiten aus dem Browser teilen, jede wird ein Entwurf. Ein Knopf „Im Browser suchen“ (Titel an den Browser des Handys, die App sendet selbst nichts) wurde vorgeschlagen, ist **nicht beschlossen**.

## Begriffe (Vorschlag Texte/Übersetzungen, noch nicht festgelegt)

„Version“ ist durch die App-Version belegt, „Original“ durch Originalsprache und Originalseite, „Kopie“ ist ausgeschlossen. Zur Wahl: „Fassung“ / “version” oder „Variante“ / “variation”. Gruppen heißen in der Oberfläche „Schlagwort“. „Vergleichen“ / “compare”, „zusammenstellen“ / “combine”.

## Leitplanken

- Grenzen: höchstens 4 Rezepte im Vergleich, keine Fotos fremder Seiten für Kandidaten, Vergleich nur in der Anzeige (nichts gespeichert).
- Ehrliche Hinweise: „Zutaten werden nach dem Wortlaut zugeordnet“, „Schritte können sich auf andere Mengen beziehen“.
- Abnahme: Rundgang in vier Darstellungen, Test mit 1.000 Gerichten und Fassungen (Teil von #52), Screenreader liest je Zeile die Fassung.

## Reihenfolge (Empfehlung)

1. #57 Gruppen und Filter (für 1.0 ohnehin nötig)
2. #66 Umrechnen nach Zutat (Grundlage für „gleiche Menge“ im Vergleich)
3. Fassungen anlegen (Verweisfeld, Datenbank-Version 4)
4. Vergleich nur zum Ansehen
5. Eigene Fassung zusammenstellen

## Offen

- Wort für die Fassung (siehe oben).
- Erscheinen Fassungen in der Sammlung einzeln oder als „Tiramisu · 3 Fassungen“? (Design/Performance raten zu einem Eintrag je Gericht.)
- Nach dem Zusammenstellen: Vergleichs-Entwürfe behalten oder löschen anbieten (nie automatisch löschen).
- „Im Browser suchen“: gewünscht oder nicht?

## Hinweise zur Prüfung der Portale

Das Gutachten zur Portalsuche (Chefkoch und GialloZafferano technisch möglich, Marmiton, Allrecipes und BBC Good Food kaum, rechtliches Risiko durch robots.txt und Nutzungsbedingungen, Vision-Ergänzung nötig) beruhte auf Fachwissen, nicht auf einer Messung. Da die Suche nicht gebaut wird, entfällt der Messlauf.
