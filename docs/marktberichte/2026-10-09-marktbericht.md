# Marktbericht vom 09.10.2026: Konkurrenz, Vermarktung und Preis

**Erstellt von:** Spezialist für Marktforschung und Marketing (`marketing`), erster Auftrag · **App-Version:** 0.26.0 (versionCode 47)

Alle Quellen wurden am 09.10.2026 abgerufen. „unsicher“ heißt: Die Angabe stammt nur aus einer Drittquelle, ist veraltet oder widersprüchlich.

## Auftrag

Wunsch des Projektinhabers, wörtlich:

> „Er soll gleich herausfinden, ob es Konkurrenz-Apps zu Moltobene gibt, wenn ja in welchen Bereichen sie besser sind, Verbesserungen vorschlagen. Außerdem soll er sich überlegen, wie man die fertige App am besten vermarkten kann, welcher Preis, usw.“

## Entscheidungen des Projektinhabers (09.10.2026)

- **Issues:** alle fünf Vorschläge angelegt. Die Nummern stehen bei den Vorschlägen unten.
- **Lizenz:** GPL-3.0.
- **Preis:** kostenlos, mit freiwilliger Spende (z. B. GitHub Sponsors).
  - Der Spendenlink steht nur im README, im Store-Eintrag und unter Einstellungen → Info. Die App bittet nie aktiv darum.
  - Es gibt keine Bezahlversion und kein Abo. Die Vision in CLAUDE.md ist entsprechend ergänzt.
- **Google-Entwicklerverifizierung:** später entscheiden.

**Wichtigster Befund:** Das Repository hatte keine Lizenzdatei (`gh repo view`: `licenseInfo: null`). Rechtlich war Moltobene damit nicht quelloffen, sondern „alle Rechte vorbehalten“. F-Droid und IzzyOnDroid nehmen nur Apps mit freier Lizenz auf.

## 1. Konkurrenz im Überblick

| App | Plattformen | Preis/Modell | Daten | Stärken | Quelle |
|---|---|---|---|---|---|
| Paprika 3 | Android, iOS, Mac, Windows | Einmalkauf je Plattform, Handy 4,99 $ (Stand 2018, heute unsicher) | lokal, Sync über Paprika-Konto | eingebauter Browser, Wochenplan, Einkaufsliste nach Gängen, Timer aus dem Text, metrisch/imperial, Import vieler Fremdformate | [paprikaapp.com](https://www.paprikaapp.com/), [Hilfe Android](https://www.paprikaapp.com/help/android/), [Blog 2018](https://paprikaapp.com/blog/2018/11/14/paprika-3-0-is-now-available-for-android/) |
| Recipe Keeper | Android, iOS, Windows, Mac | gratis testen, Vollversion (Grenze und Preis unsicher) | Cloud-Sync | Foto und PDF mit Texterkennung, Instagram/TikTok, Plan, Liste, PDF-Kochbuch | [recipekeeperonline.com](https://www.recipekeeperonline.com/) |
| Cookmate | Android, iOS, Web | gratis bis 60 Rezepte mit Werbung; Premium 6 €/Quartal oder 20 €/Jahr | Cloud-Konto | Import aus MealMaster, MasterCook, Rezkonv; Scan kostet „Credits“ | [cookmate.online](https://www.cookmate.online/) |
| Samsung Food | Android, iOS, Web | gratis mit Werbung; Food+ 6,99 $/Monat bzw. 59,99 $/Jahr | Konto/Cloud | Link, Instagram, TikTok; Foto-Scan nur in Food+ | [RecipeSage-Vergleich, Stand Aug. 2026, Quelle ist ein Mitbewerber](https://recipesage.com/alternatives/samsung-food/) |
| Plan to Eat | Android, iOS, Web | 5,95 $/Monat oder 49 $/Jahr, 14 Tage Test | Konto/Cloud | Import aus sozialen Netzwerken, Plan, Liste, Nährwerte, Timer | [plantoeat.com/pricing](https://www.plantoeat.com/pricing) |
| ReciMe | iOS, Android (seit etwa 2026) | Abo nach 7 Tagen Test, Preis nicht gefunden | Konto/Cloud | Instagram, TikTok, YouTube, Kochbuchfoto | [Hilfe](https://help.recime.app/en/articles/11594896-what-is-recime), [App Store](https://apps.apple.com/us/app/recime-recipes-meal-planner/id1593779280) |
| Chefkoch | Android, iOS | gratis mit Werbung, Kochbuch nur mit Registrierung; Plus etwa 50 €/Jahr (unsicher) | Konto | riesige Rezeptsammlung | [Weser-Kurier](https://www.weser-kurier.de/ratgeber/digitales/app-der-woche-mit-chefkoch-neue-gerichte-entdecken-doc7pq6qboy9c316tz4t2ax), [mein-deal](https://www.mein-deal.com/chefkoch-plus-abo/) |
| RecipeSage (quelloffen, AGPL) | Web, Android, iOS, Desktop | kostenlos, freiwillige Beiträge | Konto, Server (selbst betreibbar) | Foto, PDF, Word; Massenimport aus Paprika, Plan to Eat u. a. | [recipesage.com](https://recipesage.com/) |
| Mealie / Tandoor (quelloffen) | eigener Server; Android nur über fremde Apps (kitshn, Mealient) | kostenlos | eigener Server | Haushalt; Nährwerte (Tandoor) | [selfhosting.sh](https://selfhosting.sh/compare/tandoor-vs-mealie/) |
| Broccoli (F-Droid, GPL-3.0) | Android 8+ | kostenlos, Spende über Ko-fi | lokal, ohne Konto | Import von Blogs, Kategorien und Hashtags, Saisonkalender, Kochansicht; nur 9 MiB | [F-Droid, v1.4.7 vom 18.09.2026](https://f-droid.org/en/packages/com.flauschcode.broccoli/) |
| openCook (F-Droid, GPL-3.0) | Android 11+ | kostenlos | lokal; Foto-Scan nur über eigenen KI-Server | Wochenplan, Einkaufsliste, Abgleich Handy↔Handy im WLAN; sehr neu (0.3) | [F-Droid](https://f-droid.org/en/packages/com.food.opencook/) |
| nur Apple: Crouton, Mela, Pestle | iPhone, iPad, Mac | Einmalkauf bzw. Abo | iCloud | sehr gepflegte Gestaltung; Mela übersetzt Anleitungen | [RecipeSage zu Crouton](https://recipesage.com/alternatives/crouton/), [iphone-ticker zu Mela](https://www.iphone-ticker.de/rezept-verwaltung-mela-kann-jetzt-kochanleitungen-uebersetzen-246826/) |
| **Teigrechner:** PizzaPlan | Android, iOS | gratis; Pro 2,99 € einmalig (Biga, Poolish, Li.Co.Li) | lokal, ohne Konto und Tracking | Hefe nach Gehzeit und Temperatur, Kühlschrankgare | [Similarweb](https://www.similarweb.com/app/google/com.bsigned.pizzaplan/), [AppBrain](https://www.appbrain.com/app/pizzaplan-dough-calculator/com.bsigned.pizzaplan) |
| PizzApp+ | iOS (Android unsicher) | gratis mit Werbung; laut App Store werden Daten zum Tracking genutzt | – | Teigkugeln, Gehzeit/Temperatur, Rezepte speichern | [App Store](https://apps.apple.com/app/id1228158792) |
| Baker's Percentage Calculator (F-Droid, Apache-2.0) | Android 7+ | kostenlos | lokal | Bäckerprozente, eigene Rezepte, JSON-Sicherung | [F-Droid](https://f-droid.org/en/packages/com.pep1lo.bakerspercentagecalculator/) |

## 2. Wo die Konkurrenz besser ist

- **Übernehmen:**
  - **Soziale Netzwerke:** Instagram, TikTok und Pinterest können ReciMe, Samsung Food, Plan to Eat und Recipe Keeper. Moltobene liest nur YouTube; Instagram geht heute nur über ein Bildschirmfoto und „Aus Foto übernehmen“.
  - **Umzug aus anderen Apps:** Paprika (.mmf, .mx2, .fdx …), Cookmate (MealMaster, Rezkonv) und RecipeSage (Paprika, Plan to Eat, CSV) übernehmen Sammlungen aus anderen Apps. Moltobene liest nur schema.org-Dateien. Laut #57 ist das für 1.0 „nicht nötig“, aus Marktsicht aber die größte Hürde für alle, die wechseln wollen.
  - **PDF:** Recipe Keeper und RecipeSage können es, bei Moltobene ist #47 offen.
- **Finden und ordnen:** Alle großen Apps und auch Broccoli haben Kategorien bzw. Schlagwörter, Favoriten und Filter. Moltobene hat nur die Suche (#57). Das ist für 1.0 Pflicht.
- **Kochen:** Paprika und Plan to Eat erkennen Timer im Text, Paprika rechnet metrisch/imperial um. Moltobene ist mit Abhaken, Bildschirm an, Portionen und Übersetzung sonst gleichwertig.
- **Planen** (laut Vision bewusst nicht):
  - Wochenplan und Einkaufsliste haben fast alle. Ehrlich gesagt: Wer vor allem plant, wählt eine dieser Apps.
  - Zur Vision würde höchstens „Zutaten teilen“ an eine beliebige Listen-App passen.
  - Nährwerte bräuchten eine Nährwert-Datenbank.
- **Teilen, Sichern, Umziehen:**
  - Die Konkurrenz gleicht Handy, Tablet und PC ab und teilt mit der Familie, über Konto und Cloud. Moltobene macht das bewusst nicht; das ist ein echter Nachteil für Haushalte mit mehreren Geräten.
  - openCook zeigt einen Weg ohne Cloud (Handy↔Handy im WLAN). Er könnte nach 1.0 zur Vision passen, denn der direkte Umzug von Handy zu Handy ist erlaubt.
- **Gestaltung:**
  - Mela und Crouton gelten als besonders gepflegt, Samsung Food stellt große Fotos in den Mittelpunkt.
  - Die Rundgang-Fotos von Moltobene zeigen gezeichnete Teller und Besteck-Platzhalter. Für einen Store-Auftritt taugen sie so nicht.
- **Teigrechner:**
  - Bei PizzaPlan, PizzApp+ und ähnlichen Apps ist die Hefe nach Gehzeit und Temperatur die Hauptfunktion. In #69 steht genau sie unter „Später“; ohne sie ist der Rechner schwächer als PizzaPlan für 2,99 €.
  - Empfehlung: den Rechner erst bewerben, wenn dieser Richtwert da ist.
  - Vorteil von Moltobene: Biga und Poolish kostenlos, und das Ergebnis landet als Rezept in der eigenen Sammlung.

## 3. Wo Moltobene heraussticht

- **Texterkennung auf dem Handy:** kostenlos, ohne Konto, auch ohne Google-Dienste. Bei der Konkurrenz kostet der Foto-Scan Geld (Samsung Food+, Cookmate-Credits), läuft über einen Server (RecipeSage) oder braucht einen eigenen KI-Server (openCook). Unter den gefundenen Android-Apps ist das einzigartig.
- **Übersetzung auf dem Handy** (DE↔EN) mit dem Original daneben: Auf Android nichts Vergleichbares gefunden; auf dem iPhone kann Mela Ähnliches.
- **Alle Wege in einer App:** Link, Foto über mehrere Seiten, Text, Datei und YouTube, ohne Rezeptgrenze, Werbung oder Abo.
- **Offene Formate:** schema.org beim Teilen, die Sicherung ist eine Datei, die Quelle bleibt erhalten.
- **Nachweisbare Echtheit:** Fingerabdruck, Prüfsumme und Herkunftsnachweis, dazu der offen dokumentierte Vorfall aus 0.14.0.
- **Für wen das zählt:**
  1. Datenschutzbewusste und Nutzer von Handys ohne Google
  2. Besitzer von Kochbüchern und Familienrezepten auf Zetteln
  3. Mehrsprachige Köche (DE, EN, IT, FR, ES)
  4. Wer von Abos und Werbung genug hat

## 4. Vermarktung (zu 1.0)

**Zielgruppen, in dieser Reihenfolge:**
1. Datenschutz- und Open-Source-Android-Szene: am leichtesten und kostenlos zu erreichen.
2. Hobbyköche mit Kochbüchern (DE, IT).
3. Pizza- und Brotbäcker, nach 1.0 mit dem Teigrechner.

**Kernbotschaft:** der Kernsatz plus drei Belege, zum Beispiel: „Kochbuchseite fotografieren, Link teilen, Text einfügen – ohne Konto, ohne Abo, ohne Werbung.“ Store-Texte folgen den Textregeln, also ohne direkte Anrede.

**Vertriebswege:**

| Weg | Aufwand | Voraussetzungen und Hürden |
|---|---|---|
| GitHub Releases (heute) | – | keine automatischen Updates (#16); Hinweis auf die Update-App Obtainium im README möglich |
| IzzyOnDroid | klein | freie Lizenz; APK im GitHub-Release (vorhanden); keine Tracker. Größe: Faustregel 30 MB, Moltobene hat rund 46 MB → Ausnahme anfragen oder je Prozessorart eine eigene APK, dazu #59. Danach Updates automatisch über die F-Droid-Apps. [Regeln](https://izzyondroid.org/docs/general/AppInclusionPolicy/) |
| F-Droid (Hauptarchiv) | mittel bis groß | freie Lizenz; F-Droid baut selbst aus dem Quelltext; Bausteine frei lizenziert ([Regeln](https://f-droid.org/docs/Inclusion_Policy)). Ob ONNX Runtime als fertig gebautes Paket mit Maschinencode durchgeht, ist **unsicher** – bei F-Droid erfragen. Die Modelle (Apache 2.0, CC BY 4.0) sind frei lizenziert. Mit einem reproduzierbaren Build bleibt die eigene Signatur erhalten. |
| Google Play | mittel, dazu laufende Pflege | 25 $, Identitätsprüfung; neue Privatkonten brauchen einen geschlossenen Test mit mindestens 12 Testern über 14 Tage ([Google-Hilfe](https://support.google.com/googleplay/android-developer/answer/14151465)); Datenschutz-Webseite, Angaben zur Datensicherheit. **Weicht von der Vision ab – Entscheidung des Projektinhabers.** |

**Google-Entwicklerverifizierung:**
- Seit 30.09.2026 gilt sie in Brasilien, Indonesien, Singapur und Thailand, weltweit ist sie ab 2027 geplant.
- Danach lassen sich Apps nicht bestätigter Entwickler nur noch über einen „erweiterten Ablauf“ mit einem Tag Wartezeit oder per Kabel (ADB) installieren.
- Das Konto für volle Verteilung kostet 25 $ und verlangt einen Identitätsnachweis; der kostenlose Weg ist auf 20 Geräte begrenzt.
- Quellen: [Google-Blog, März 2026](https://android-developers.googleblog.com/2026/03/android-developer-verification.html), [FAQ, Stand 30.09.2026](https://developer.android.com/developer-verification/guides/faq), [Help Net Security, Juni 2026](https://www.helpnetsecurity.com/2026/06/19/android-developer-verification-rollout-markets/)

**Store-Auftritt:**
- Kurzbeschreibung = Kernsatz, Langtext entlang der fünf Kernfunktionen, auf DE und EN.
- Bildschirmfotos in dieser Reihenfolge: Sammlung, Rezept mit Foto, Abhaken, Foto übernehmen, Übersetzung, Datenschutz. Dafür braucht es echte Fotos von Gerichten.

**Communities und Presse** (kostenlos; die Regeln zur Eigenwerbung je Forum beachten):
- „This Week in F-Droid“ nennt neu aufgenommene Apps.
- Reddit (r/fossdroid, r/androidapps, r/degoogle), Lemmy, Mastodon, ein Eintrag bei AlternativeTo.
- Deutschsprachig: Kuketz-Blog und -Forum, mobilsicher.de, izzysoft.de.
- Teigrechner später: r/Pizza, das Forum pizzamaking.com, r/Breadit.

**Rechtliche Hinweise (keine Rechtsberatung):**
- Der Name „Molto Bene“ ist bereits als Kochsendung und Kochbuch von Benedetta Parodi in Gebrauch ([thetvdb](https://www.thetvdb.com/dereferrer/series/337752)). Eine App unter dem Namen wurde nicht gefunden. Vor einem Store-Start im Markenregister (DPMA/EUIPO) prüfen lassen.
- Bei Spenden: Impressumspflicht und Steuer prüfen lassen.

**Zeitplan:**
- **Bis 1.0:** Lizenz, Store-Texte und echte Fotos, Größenfrage klären, Verifizierung entscheiden.
- **Mit 1.0:** Antrag bei IzzyOnDroid, einmalige Ankündigung mit Bildern, AlternativeTo.
- **1.x:** F-Droid, Ankündigung des Teigrechners.
- **Vor 2027:** Paketname registrieren, falls so entschieden.
- **Google Play:** frühestens danach, nur nach Entscheidung.

## 5. Preis und Geschäftsmodell

**Was die Konkurrenz verlangt:**
- Abo für 20–60 € bzw. $ pro Jahr: Cookmate 20 €, Plan to Eat 49 $, Samsung Food+ 59,99 $, Chefkoch Plus etwa 50 € (unsicher).
- Einmalkauf je Plattform: Paprika etwa 5 $ (unsicher).
- Gratisversion mit Rezeptgrenze: Cookmate, 60 Rezepte.
- Freie Apps sind gratis und leben von Spenden (Broccoli über Ko-fi, RecipeSage).
- Teigrechner kosten 2–3 € einmalig (PizzaPlan 2,99 €).

| Modell | Vorteile | Nachteile | Vision |
|---|---|---|---|
| Kostenlos | höchstes Vertrauen, kein Aufwand | keine Einnahmen | passt |
| Kostenlos mit freiwilliger Spende (GitHub Sponsors, Liberapay, Ko-fi) | üblich in der Szene; F-Droid zeigt Spendenlinks an | Höhe nicht planbar; gilt steuerlich als Einnahme | passt, wenn die App nicht darum bittet – **gewählt** |
| Gratis auf F-Droid/GitHub, kostenpflichtig im Play Store | Einnahmen von denen, die es bequem wollen | Play-Konto, Testpflicht, als „Händler“ wohl öffentliche Adresse (unsicher), Steuer | weicht von der Vision ab |
| Einmalkauf | verbreitet (Paprika) | bei offenem Quelltext kaum durchzusetzen | weicht ab |
| Abo | planbare Einnahmen | Moltobene hat keine Serverkosten, ein Abo wäre nicht zu begründen; schreckt die Zielgruppe ab | weicht stark ab |

**Empfehlung:** Moltobene bleibt kostenlos, mit einer freiwilligen Spende über GitHub Sponsors (eventuell Liberapay). „Kostenlos ohne Haken“ ist gegen alle kommerziellen Apps mit Rezeptgrenzen, Werbung und Abos die stärkste Botschaft.

## Vorschläge

| Nr. | Vorschlag | Priorität | Aufwand | Issue |
|---|---|---|---|---|
| M1 | Eigene Open-Source-Lizenz festlegen (entschieden: GPL-3.0) | hoch | klein | #72 |
| M2 | Store-Auftritt vorbereiten: Texte im Repository, Bildschirmfotos mit echten Fotos, Spendenlink | hoch | mittel | #73 |
| M3 | Zu 1.0 bei IzzyOnDroid, danach im F-Droid-Hauptarchiv; vorher die APK-Größe klären | hoch | mittel | #74 |
| M4 | Google-Entwicklerverifizierung vor 2027 entscheiden | hoch | klein | #75 |
| M5 | Umzug aus anderen Rezept-Apps vorziehen (Paprika, RecipeSage, MealMaster) | mittel | mittel | #76 |

**Übersprungen, weil bereits als Issue vorhanden:**
- #57 Finden und ordnen
- #47 PDF
- #16 Update-Hinweis
- #59 App kleiner (wichtig für M3)
- #69 Teigrechner. Aus Marktsicht sollte der Hefe-Richtwert nach Gehzeit und Temperatur früher kommen.
- #61 Kochen, Teil 2
- #58 Texte-Durchgang
