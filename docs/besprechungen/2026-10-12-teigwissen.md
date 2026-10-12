# Besprechung vom 12.10.2026: Teigwissen im Teigrechner

**Teilnehmer:** Funktionalität, Design, Texte, Übersetzungen, Sicherheit, Performance, Kreativität, Marketing
**Moderation:** Claude · **App-Version:** 0.36.0 in Arbeit · **Issue:** #84

Die Beiträge sind von Claude zusammengestellt (simulierte Besprechung wie bisher), gestützt auf die Vision, die festen Begriffe und den heutigen Teigrechner. Fachliche Zahlen stehen hier nicht als Aussagen, sondern als Aufgaben für die Recherche; vor jeder Aufnahme in die App gilt der Weg wie bei den Grundrezepten (mehrere Quellen abgleichen, eigene Worte, Quellenliste unter `docs/recherche/`, Gegenlesen).

## Anlass (Projektinhaber, wörtlich, 12.10.2026)

> „Für den Teigrechner müssen wir dann auch noch etwas Know How einbauen (interaktiv oder als Wiki? Beides?). Was bewirken Veränderungen der Mengenverhältnisse. Wie lang sollen welche Gehzeiten dauern. Wofür sind Gehzeiten wichtig, was bewirken sie? Warm- / Kalt-Vermentation. Welche Mehle können wofür verwendet werden. Wie werden Mehle in unterschiedlichen Ländern (vorerst D, A, CH, It) benannt (W480, W550 usw.). Weizen, Dinkel, Roggen, Vollkorn usw. Glutenbildung, Elastizität, Eiweißanteil, Kalorien usw. sind weiter Punkte die wir in irgendeiner Weise integrieren sollten.“

## Die Themen, geordnet
1. **Wirkung der Mengenverhältnisse** (Wasseranteil, Salz, Öl, Hefe, Vorteig): was sich in Teig, Krume, Kruste, Geschmack und Gehzeit ändert, wenn ein Wert höher oder niedriger wird.
2. **Gehzeiten:** wofür sie da sind (Geschmack, Lockerung, Teigstruktur), wie lang bei welcher Hefemenge und Temperatur, **warm und kalt** (Kühlschrank-Reife), Stückgare/Gehzeit nach dem Formen; wann zu kurz, wann zu lang. Fester Begriff im Deutschen: **Gehzeit** (nicht „Gare“, „Stockgare“).
3. **Mehle:** Getreidearten (Weizen, Dinkel, Roggen, Vollkorn u. a.), wofür sie taugen, Eiweiß- und Glutenanteil, Elastizität und Dehnbarkeit, Mineralstoffe, typische Nährwerte (kcal je 100 g) als Spannen.
4. **Mehlbezeichnungen D, A, CH, IT:** Typenzahlen, Bezeichnungen, Stärkeklassen; wer was meint.

## Einigkeit

1. **Beides, aber in Etappen, und klein anfangen** (Funktionalität, Design, Marketing/Fokus): zuerst **Hinweise am Feld**, dann ein eigener Bereich **Teigwissen** zum Nachschlagen, zuletzt Verknüpfungen (z. B. „Mehl wählen“). Ein Wiki ohne vorherige Hinweise am Feld bleibt ungelesen, Hinweise ohne Nachschlagewerk bleiben oberflächlich.
2. **Hinweise am Feld („Was bewirkt das?“):** Jedes Feld im Teigrechner bekommt ein kleines Fragezeichen-Symbol mit Beschreibung für den Screenreader; es öffnet ein kurzes Fenster mit drei Teilen: *Was der Wert bewirkt*, *typische Spanne und wofür sie üblich ist*, *worauf zu achten ist*. Höchstens 4–6 Sätze, ohne direkte Anrede, in beiden Sprachen einzeln geschrieben. Dazu optional ein **Wirkungs-Hinweis** unter dem Wert, der bei Änderung wechselt („Mehr Wasser: offenere Krume, weicherer Teig“) – nur als Richtung, nie mit erfundenen Zahlen.
3. **Teigwissen als Artikelliste** (Design, Texte, Performance): eigener Bildschirm (Kachel auf der Startseite oder Einstieg aus dem Teigrechner), Artikel einzeln, je Artikel Überschriften, kurze Absätze, bei Zahlen Spannen mit Hinweis „Richtwert“; offline als JSON in `assets/` (wie die Grundrezepte), nicht in Datenbank oder Sicherung, kein Download, wenige Dutzend KB; Suche über die Titel. Voll nutzbar bei Schrift 200 % und mit Screenreader (Überschriften mit `heading()`).
4. **Gehzeit-Hilfe statt nur Text** (Funktionalität, kreativ): eine Tabelle oder kleine Hilfe „Gehzeit schätzen“ mit Temperatur und Hefemenge als Eingabe und einer **Spanne** als Ergebnis (nie eine einzelne genaue Zahl); erst nach Recherche mit mehreren Quellen, mit Hinweis, dass Raumtemperatur, Mehl und Teigtemperatur die Zeit ändern. Warm- und Kaltführung als zwei gleichwertige Wege erklären.
5. **Mehlbezeichnungen sind eine Tabelle, keine Gleichung** (Sicherheit/Übersetzungen, CLAUDE.md: „Mehltypen werden nie gleichgesetzt“): je Land eine Liste der gebräuchlichen Bezeichnungen mit Bedeutung und Verwendung, **ohne** „entspricht“-Spalte. Nur Hinweise wie „ähnlich in Ausmahlungsgrad, aber nicht gleich“. Länder zuerst D, A, CH, IT, später weitere.
6. **Stolperstein „W“:** In der Anfrage stehen „W480“ und „W550“. In Österreich bezeichnet das W vor einer Zahl die **Type** (Mineralstoffgehalt, z. B. Weizenmehl W480/W700), in Italien ist **W** eine **Kennzahl für die Backkraft** (Teigfestigkeit, aus dem Alveografen), die nichts mit der Type zu tun hat. Beides nebeneinander ohne Erklärung wäre irreführend; die Seite muss das ausdrücklich erklären. Die genauen Zahlenspannen sind Recherche, nicht Gedächtnis.
7. **Nährwerte und Gluten** (Sicherheit): Kalorien, Eiweiß und Kohlenhydrate nur als **typische Spannen je 100 g Mehl**, mit Hinweis „Richtwert“; keine Heilsversprechen, keine Aussagen zu Verträglichkeit oder Krankheiten. Gluten wird als Eigenschaft des Teigs erklärt; für Menschen mit Glutenunverträglichkeit steht der Hinweis, dass die gezeigten Mehle (Weizen, Dinkel, Roggen) Gluten enthalten. Allergene-Hinweise wie bei den Grundrezepten.
8. **Mehlwahl im Rechner nur als Hinweis** (Funktionalität): In der ersten Fassung beeinflusst die Mehlart **nichts** in der Rechnung; sie kann höchstens einen Hinweis zeigen („Vollkornmehl nimmt mehr Wasser auf, Wasseranteil eher höher wählen“). Ein Vorschlag für den Wasseranteil je Mehl braucht eigene Recherche und eine eigene Entscheidung.
9. **Leitplanken wie bei den Grundrezepten:** selbst geschrieben (nur Tatsachen, Zusammenhänge und Richtwerte in eigenem Wortlaut), Quellenliste im Repository (Datei unter `docs/recherche/`), Gegenlesen durch jemanden mit Backerfahrung, DE/EN einzeln geschrieben, keine Marken, keine Fotos aus fremden Quellen (eigene einfache Zeichnungen möglich).
10. **Feste Begriffe vorab** (Texte): **Teigwissen / dough know-how** (Bereich), **Gehzeit / rising time** (bleibt), **Kaltführung / cold rise**? und **Warmführung / warm rise**? (zu klären, Alternativen: „Gehzeit im Kühlschrank“), **Backkraft (W-Wert) / baking strength (W value)**, **Type (Mehl) / flour type** — Tabelle im Abschnitt „Feste Begriffe“ erweitern, bevor der erste Text entsteht. Nicht „Gare“ oder „Stockgare“.

## Streitpunkte
- **Reihenfolge:** Funktionalität: erst die Hinweise am Feld, weil sie sofort beim Rechnen helfen. Kreativität: erst die Gehzeit-Hilfe, weil sie das Wertvollste ist. Marketing: erst der Mehl-Überblick DACH+Italien, weil das einen Suchbegriff und einen Unterschied zu anderen Teig-Apps bietet. Mehrheit: Hinweise am Feld zuerst.
- **Umfang der Mehltabelle:** Funktionalität/Fokus: wenige verbreitete Mehle je Land (etwa 6–8), Performance/Texte: ja, aber vollständig statt „Auswahl mit Lücken“ — dann lieber später.
- **Nährwerte überhaupt:** Sicherheit: nur mit sauberer Quelle und als Spanne; Marketing: ja, wird gesucht; Design: nur im Artikel, nicht im Rechner.
- **Eigenes Fenster oder Teil des Rechners:** Design: Fenster für die Hinweise, Bildschirm für Teigwissen; Funktionalität: ebenso.

## Reihenfolge (Empfehlung)
1. **Recherche und Begriffe:** Wirkung der Werte, Gehzeiten warm/kalt, Mehltypen D/A/CH/IT, Gluten/Eiweiß/Nährwerte als Spannen; Quellenliste in `docs/recherche/`; Begriffe in CLAUDE.md.
2. **Etappe 1 (klein):** Hinweise am Feld für Wasseranteil, Salz, Öl, Hefe, Vorteig und Gehzeit-Grundlagen.
3. **Etappe 2:** Teigwissen als Bereich mit Artikeln (Wirkung der Werte, Gehzeiten, Warm- und Kaltführung, Gluten).
4. **Etappe 3:** Mehl-Überblick je Land mit Erklärung von Type und W-Wert.
5. **Etappe 4:** Gehzeit-Hilfe mit Spannen; danach Verknüpfungen (Mehlhinweise im Rechner).
Jede Etappe mit Rundgang in vier Darstellungen und einem Test, der alle Texte auf Vollständigkeit in beiden Sprachen und auf direkte Anrede prüft (wie bei den Grundrezepten).

## Offene Entscheidungen des Projektinhabers
Reihenfolge (Hinweise am Feld zuerst?); ob Nährwerte aufgenommen werden; ob die Mehlwahl im Rechner zunächst nur Hinweise gibt; wer die Texte gegenliest (jemand mit Backerfahrung); ob die Länder-Tabelle mit D, A, CH, IT beginnt und wie viele Mehle je Land.
