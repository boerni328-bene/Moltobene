# Teigwissen, Etappe 1: Herkunft der Hinweise „Was bewirkt das?“ (Stand 13.10.2026, überarbeitet nach Prüfbericht, #84)

Die Texte sind **in eigenen Worten** geschrieben (nur Zusammenhänge und grobe Orientierungswerte, nichts wörtlich übernommen) und in Deutsch und Englisch einzeln verfasst. Alle Zahlen gelten als **grobe Orientierung, nicht als Norm**. Grundlage ist der Abgleich mehrerer frei zugänglicher Quellen; die Recherche stützte sich auf Zusammenfassungen von Suchergebnissen. Ein **Gegenlesen durch Menschen mit Backerfahrung** ist weiter offen. Eigene Berechnungen sind als solche gekennzeichnet.

Überarbeitung vom 13.10.2026 nach dem Prüfbericht (siehe `2026-10-13-teigwissen-pruefung.md`): Salz-Hefe-Regel entfernt, feste Kühlschrank-Grenze entfernt, Ölwirkung neutral gefasst, Wasseranteil und Vorteig-Bezug definiert, Aussage zur Frischhaltung durch Säure entfernt.

## Wasseranteil
- **Aussage:** Mehr Wasser macht viele Teige weicher und klebriger und kann eine offenere Krume begünstigen; Wasseraufnahme hängt vom Mehl ab.
  Quellen: America's Test Kitchen, „Baker's percentages explained“ (https://www.americastestkitchen.com/how_tos/5739-bakers-percentages-explained); Crafty Baking zu Backprozenten (https://www.craftybaking.com/); The Fresh Loaf, Beitrag „Baker's Percentage question“ (https://www.thefreshloaf.com/comment/550035); King Arthur Baking, „open crumb focaccia“ (https://www.kingarthurbaking.com/blog/2026/04/09/open-crumb-focaccia).
- **Definition:** Wasseranteil = Wasser in Prozent vom **gesamten** Mehl. Im Rechner zählen Mehl und Wasser eines Vorteigs zur Gesamtmenge (`DoughCalculator`, geprüft durch `DoughCalculatorTest`); Öl zählt nicht als Wasser.
- **Spannen (nur Orientierung):** Brot etwa 57–65 % bis 70 % (Crafty Baking, Lehrrezepturen), sehr feuchte Teige um 80 % (America's Test Kitchen, King Arthur). Neapolitanische Pizza etwa 56–62 %: **eigene Berechnung** aus den Mengen je Liter Wasser in den Regeln des Verbands der neapolitanischen Pizzabäcker (Disciplinare 2024, https://www.pizzanapoletana.org/public/pdf/Disciplinare-2024-ENG.pdf; eine Spiegelung nennt 1,6–1,8 kg Mehl je Liter Wasser). Pizza vom Blech streut in den Quellen von 53 % bis 80 % (General-Mills-Rezeptur https://generalmillsfoodservice.com/industries/pizzeria/support-tool-categories/technical-support/dough-formulas/basic-pan-style-formula; Chain Baker https://www.chainbaker.com/shet-pan-pizza).
- Roggen, Vollkorn und Spezialteige weichen deutlich ab; der Text sagt das.

## Salz
- **Aussage:** Salz verbessert den Geschmack, stärkt die Teigstruktur und verlangsamt die Hefegärung etwas. Quellen: King Arthur Baking, „Why is salt important in yeast bread“ (https://www.kingarthurbaking.com/blog/2020/07/29/why-is-salt-important-in-yeast-bread); Cargill, „Salt in bread dough“ (https://www.cargill.com/salt-in-perspective/salt-in-bread-dough); Universität Valladolid, „Salt in baking“ (https://innograin.uva.es/en/2023/10/20/salt-in-baking/).
- **Spanne:** Brot etwa 1,8–2,2 % (Quellen nennen 1,5–2,5 %). Neapolitanisch etwa 2,8 %: **eigene Berechnung** aus den Mengen (50–55 g Salz je Liter Wasser bei 1,6–1,8 kg Mehl).
- **Entfernt:** Die Angabe „Salz und Hefe nicht länger als etwa 5 Minuten unmittelbar aufeinander“ steht in den Regeln des Pizzabäckerverbands als **Verfahrensvorgabe für dessen Pizza** und gilt nicht als allgemeine Backregel; sie steht deshalb nicht mehr in den Hinweisen. Bei langem, konzentriertem Kontakt belastet Salz Hefezellen osmotisch; im normalen Teig dürfen beide zusammen sein.
- Zur Bräunung: Der Einfluss von Salz auf die Krustenfarbe ist indirekt (über die Gärung) und hängt von Rezept und Backtemperatur ab; er steht nicht in den Hinweisen.

## Öl
- **Aussage (neutral gefasst):** Öl kann die Krume zarter machen und die Dehnbarkeit verändern; die Wirkung hängt von Menge, Zeitpunkt der Zugabe und Rezeptur ab. Quellen: Cookistry, „Role of fat in yeasted doughs“ (https://www.cookistry.com/2010/12/technique-role-of-fat-in-yeasted-doughs.html); Universität Valladolid, „Oils and fats: applications“ (https://innograin.uva.es/en/2024/02/06/oils-and-fats-applications/).
- **Spannen (Orientierung, getrennt für Brot und Pizza):** Brot meist 0–5 %, Blechpizza und sehr weiche Teige bis etwa 10 % (Chain Baker, General Mills). Feste Fette wirken anders als Öl.
- **Entfernt:** die Ursache-Wirkung-Kette „viel Öl → langsamer, dichter“ und die Aussage „hält länger frisch“ ohne Einschränkung. Frischhaltung hängt von Rezept und Lagerung ab.

## Hefe und Gehzeit
- **Aussage:** Hefemenge, Temperatur und Gehzeit bestimmen gemeinsam die Reifung; weniger Hefe bei längerer Gehzeit **kann** den Geschmack begünstigen (keine Garantie). Quellen: Forno Bravo, „Cold fermentation“ (https://www.fornobravo.com/pizzaquest/peters-blog-sept-15th-cold-fermentation/); Pizzamaking-Forum (https://www.pizzamaking.com/forum/index.php/topic,19711.0.html). Die Quelle von Peter Reinhart betont, dass die Zeit wichtiger sein kann als die Kälte.
- **Kälte:** verlangsamt die Gärung, stoppt sie nicht. Die Formulierung „Kälte lässt Aromastoffe entstehen“ wurde entfernt.
- **Entfernt:** die feste Angabe „nach etwa 5–6 Tagen im Kühlschrank lässt die Qualität nach“. Wie lange ein Teig verwendbar bleibt, hängt von Hefemenge, Salz, Mehl und Temperatur ab; sinkende Backqualität ist außerdem nicht dasselbe wie Verderb.
- Es werden bewusst **keine** Hefemengen in Zahlen genannt (Quellen streuen je nach Hefeart und Zeit). Umrechnung zwischen Hefearten ist die bekannte Näherung des Rechners (≈).

## Vorteig
- **Aussage:** Ein Vorteig kann Geschmack und Teigeigenschaften verbessern; Biga eher fester, Poolish dehnbarer. Quellen: Wikipedia, „Pre-ferment“ (https://en.wikipedia.org/wiki/Pre-ferment); BakerPedia (https://bakerpedia.com/?p=3815); American Society of Baking, „Biga“ (https://asbe.org/?p=12467).
- **Spannen:** Biga etwa 50–60 % Wasser vom Mehl im Vorteig, Poolish etwa 100 %; Reifezeit der Biga etwa 8–16 Stunden **bei 15–20 °C**; Temperatur und Zeit werden gemeinsam genannt.
- **Bezugsgröße:** „20–50 %“ meint den Anteil des **vorab angesetzten Mehls am gesamten Mehl**, so wie das Feld „Mehl im Vorteig“ im Rechner. Mehl und Wasser des Vorteigs zählen zur Gesamtmenge.
- **Entfernt:** „längere Frischhaltung durch höhere Säure“ (gilt nicht pauschal; Biga und Poolish mit Hefe sind kein Sauerteig).
- Anzeichen von Überreife (Vorteig fällt zusammen, riecht stechend sauer) stehen als Erfahrungshinweis im Text.

## Offen
- Direkte Belege (Fundstellen, Abrufdatum) je Zahl nach dem Gegenlesen vervollständigen; die Links oben stammen aus den Suchergebnissen und sind noch nicht alle inhaltlich im Volltext geprüft.
- Gehzeit-Hilfe mit Spannen (Etappe 4), Mehlüberblick D/A/CH/IT mit Type und W-Wert (Etappe 3), Nährwerte als Spannen: eigene Recherche und Entscheidung.
