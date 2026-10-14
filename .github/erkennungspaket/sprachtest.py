#!/usr/bin/env python3
"""Prüft die Befehlserkennung (#80) mit dem echten deutschen Modell aus dem Erkennungspaket.

Die Befehle und Alltagssätze werden mit einer Computerstimme (espeak-ng) gesprochen und mit zwei Wortlisten erkannt:
A) nur die Befehle (+ "[unk]"), B) die Befehle plus häufige Alltagswörter als "Köder", damit normales Gerede dort einen Platz
findet. Ein Befehl zählt nur, wenn der erkannte Text als Ganzes genau ein Befehl ist (so wie die App es prüft).
Das ersetzt keinen Test mit echter Stimme auf dem Handy. Aufruf: sprachtest.py <Modellverzeichnis> <Arbeitsverzeichnis>
"""
import json
import os
import subprocess
import sys
import wave

from vosk import KaldiRecognizer, Model, SetLogLevel

BEFEHLE = [
    "weiter", "zurück", "wiederholen", "vorlesen", "stopp",
    "mikro aus", "mikrofon aus", "ausschalten", "ende", "beenden", "fertig",
]
KOEDER = """ich du er sie es wir ihr und oder aber das die der den dem ein eine einen einem ist sind war hat haben wird kann
muss soll will nicht kein mit ohne für von zu bei nach vor auf an in im am um dann wenn weil auch noch schon nur sehr
mehr weniger viel wenig alle man mal gleich jetzt später heute morgen hier da dort wo wie was wann warum wer bitte danke
ja nein gut schön lecker schnell langsam heiß kalt warm groß klein neu alt
salz pfeffer zwiebel zwiebeln knoblauch öl butter mehl eier ei milch wasser zucker nudeln reis kartoffeln tomaten fleisch
gemüse suppe soße pfanne topf herd ofen minuten stunde gramm esslöffel teelöffel kochen braten backen rühren schneiden
mischen geben nehmen brauchen holen gehen kommen machen sagen sehen essen trinken einkaufen telefon licht tür fenster kind
kinder hunger durst zeit tag abend mittag frühstück musik radio fernseher film nachrichten wetter""".split()
ALLTAG = [
    "ich brauche noch zwei eier und etwas mehl",
    "die zwiebeln müssen goldbraun werden",
    "kannst du mir bitte das salz geben",
    "das wasser kocht schon",
    "gleich ist das essen fertig",
    "wir gehen danach weiter einkaufen",
    "mach bitte das licht aus",
    "ich habe heute keine zeit",
    "die kinder haben hunger",
    "wo ist der große topf",
    "das ist das ende vom film",
    "gib mir mal den löffel",
    "stell die musik leiser",
    "wir müssen noch milch kaufen",
    "bis die nudeln fertig sind dauert es zehn minuten",
    "was gibt es heute zum abendessen",
    "bitte schneide die tomaten klein",
    "hast du den schlüssel",
    "der ofen ist noch nicht heiß",
    "ich komme gleich zurück",
]
GESCHWINDIGKEITEN = [120, 150, 180]


def sprechen(text, ziel, tempo):
    roh = ziel + ".roh.wav"
    subprocess.run(["espeak-ng", "-v", "de", "-s", str(tempo), "-w", roh, text], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", roh, "-ar", "16000", "-ac", "1", ziel], check=True)


def erkennen(modell, wav, grammatik):
    rec = KaldiRecognizer(modell, 16000, json.dumps(grammatik, ensure_ascii=False))
    rec.SetWords(True)
    with wave.open(wav, "rb") as f:
        while True:
            daten = f.readframes(4000)
            if not daten:
                break
            rec.AcceptWaveform(daten)
    return json.loads(rec.FinalResult())


def ist_befehl(text):
    """Wie in der App: nur wenn der ganze erkannte Text genau ein Befehl ist."""
    return " ".join(text.split()) in BEFEHLE


def main(modell_pfad, arbeit):
    os.makedirs(arbeit, exist_ok=True)
    SetLogLevel(-1)
    modell = Model(modell_pfad)
    listen = {
        "A nur Befehle": BEFEHLE + ["[unk]"],
        "B Befehle + Köder": BEFEHLE + sorted(set(KOEDER) - {"aus"} | {"aus"}) + ["[unk]"],
    }
    # Aufnahmen einmal erzeugen.
    befehlsaufnahmen = []
    for befehl in BEFEHLE:
        for tempo in GESCHWINDIGKEITEN:
            wav = os.path.join(arbeit, f"b_{befehl.replace(' ', '_')}_{tempo}.wav")
            sprechen(befehl, wav, tempo)
            befehlsaufnahmen.append((befehl, tempo, wav))
    alltagsaufnahmen = []
    for nummer, satz in enumerate(ALLTAG):
        wav = os.path.join(arbeit, f"a_{nummer}.wav")
        sprechen(satz, wav, 150)
        alltagsaufnahmen.append((satz, wav))
    for name, grammatik in listen.items():
        print(f"\n######## Liste {name} ({len(grammatik)} Einträge)")
        treffer = 0
        for befehl, tempo, wav in befehlsaufnahmen:
            text = erkennen(modell, wav, grammatik).get("text", "")
            richtig = text == befehl
            treffer += richtig
            if not richtig:
                print(f"  Befehl verfehlt: {befehl!r} @{tempo}: {text!r}")
        print(f"Befehle richtig erkannt: {treffer} von {len(befehlsaufnahmen)}")
        falsch = 0
        for satz, wav in alltagsaufnahmen:
            ergebnis = erkennen(modell, wav, grammatik)
            text = ergebnis.get("text", "")
            alarm = ist_befehl(text)
            falsch += alarm
            print(f"  {'FALSCHALARM' if alarm else 'ruhig     '} {satz!r}: {text!r}")
        print(f"Fehlalarme (Gesamttext ist genau ein Befehl): {falsch} von {len(alltagsaufnahmen)}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
