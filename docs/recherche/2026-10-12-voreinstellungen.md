# Voreinstellungen im Teigrechner: Herkunft der Werte (Stand 12.10.2026, #83 Etappe 3)

Die Werte sind **gerundete Richtwerte in eigener Festlegung**, nicht aus Büchern oder Portalen kopiert (wie die Grenzen in `DoughLimits`). Sie beruhen auf dem Abgleich mehrerer frei zugänglicher Angaben; jede Zahl ist in der App frei änderbar. Die Prüfung stützt sich auf Zusammenfassungen von Suchergebnissen und braucht noch ein Gegenlesen durch Menschen mit Backerfahrung (offen, wie bei den Grundrezepten, #82).

## Pizza
- **Neapolitanisch:** Wasseranteil 60 %, Salz 2,8 %, kein Öl, Frischhefe 0,2 %.
  - Die Regeln des Verbands der neapolitanischen Pizzabäcker (Disciplinare, Abschnitt zu den Mengen je Liter Wasser) nennen etwa 1,6 bis 1,8 kg Mehl, 50 bis 55 g Salz und 3 g Hefe je Liter Wasser. Daraus folgen rechnerisch etwa 56 bis 62 % Wasser, 2,8 % Salz und 0,2 % Frischhefe (eigene Rechnung, keine wörtliche Angabe).
  - Ein frei zugänglicher Rechner für runde Pizzen nutzt 62 % Wasser, etwa 2,8 % Salz und 0,15 % Trockenhefe.
- **Pizza vom Blech:** Wasseranteil 70 %, Salz 2 %, Öl 5 %, Trockenhefe (Instant) 0,9 %.
  - Die Angaben aus den Quellen streuen stark: 53 % Wasser mit 6 % Öl und 1 % Instanthefe (Großbäckerei-Rezeptur), 80 % Wasser mit 10 % Öl, 2 % Salz und 0,9 % Instanthefe (Heimrezept), 78 % Wasser mit 2 % Salz und Öl (Forum). Der Wert liegt bewusst in der Mitte des Bereichs.

## Brot
- **Weißbrot:** Wasseranteil 65 %, Salz 2 %, kein Öl, Frischhefe 1 %.
  - Salz üblich 1,5 bis 2,5 %, Wasser für Standardbrote 57 bis 72 % je nach Quelle; Hefe 0,1 bis 0,5 % bei langer kalter Gare, 1 bis 2 % bei schneller Gare (jeweils Instanthefe-Größenordnung, in Frischhefe umgerechnet etwa das Dreifache).
- **Brot mit Poolish:** Wasseranteil 70 %, Salz 2 %, Frischhefe 0,3 %, Vorteig Poolish mit 30 % des Mehls und 100 % Wasser vom Mehl im Vorteig.
  - Poolish ist laut allgemeiner Backliteratur ein Vorteig mit gleich viel Wasser wie Mehl; Mehlanteile von etwa 20 bis 50 % sind üblich. Die geringe Hefemenge gilt für lange Vorteigführung.
- **Nicht aufgenommen:** Mischbrot mit Sauerteig (keine belastbaren Angaben gefunden), Ciabatta und weitere. Mehr Voreinstellungen erst nach dem Gegenlesen.

## Sicherheit
- Alle Werte liegen innerhalb der Grenzen von `DoughLimits`; `DoughPresetsTest` prüft das und dass jede Voreinstellung rechnet.

## Ergänzt am 31.10.2026 (Version 0.65.0, Wunsch des Projektinhabers)
Gerundete Richtwerte in eigener Festlegung nach gängigen Angaben für diese Stile; nicht aus einer einzelnen Quelle übernommen, vor einer Veröffentlichung gegenlesen.
- **New York:** Wasser 62 %, Salz 2 %, Öl 2 %, Frischhefe 0,5 % (Teig, der ein bis drei Tage kalt reift).
- **Detroit:** Wasser 72 %, Salz 2 %, Öl 2 %, Instanthefe 0,8 % (feuchter Teig in der Form).
- **Focaccia:** Wasser 75 %, Salz 2,2 %, Öl 4 %, Frischhefe 1 % (zusätzlich Öl in der Form und obenauf, nicht eingerechnet).

## Ergänzt am 31.10.2026 (Version 0.66.0, Wunsch des Projektinhabers)
Ebenfalls gerundete Richtwerte in eigener Festlegung, vor einer Veröffentlichung gegenlesen. Mehl, Zucker, Milch, Joghurt, Malz und Streusalz sind nicht eingerechnet.
- **Pizza:** Pinsa romana (Wasser 80 %, Salz 2,2 %, Öl 2 %, Frischhefe 0,2 %), Grandma (65 %, 2 %, 3 %, Instanthefe 0,7 %), Chicago Deep Dish (52 %, 1,8 %, 8 %, Instanthefe 0,7 %).
- **Brot:** Ciabatta (78 %, 2 %, 2 %, Frischhefe 0,5 %), Baguette (68 %, 2 %, 0 %, 0,5 %), Semmel (58 %, 2 %, 2 %, 2 %, Backmalz 1 %), Bürli (62 %, 2 %, 2 %, 1,5 %), Salzstängel (52 %, 2 %, 4 %, 2 %), Laugen (54 %, 2 %, 4 %, 1,5 %), Naan (Wasser 35 %, 1,8 %, 5 %, Instanthefe 1,2 %, Joghurt 25 %), Toastbrot (Wasser 38 %, 2 %, 5 %, Frischhefe 2 %, Milch 20 %, Zucker 4 %), Bagel (55 %, 2 %, 0 %, 1 %, Backmalz 1 %).
- „Brötchen“ ist als „Semmel“ enthalten.
