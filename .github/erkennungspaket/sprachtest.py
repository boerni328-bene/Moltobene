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

def _lese_app_listen():
    """Befehle und Köder stehen nur in VoiceCommands.kt; der Test liest sie von dort."""
    import re
    quelle = open(os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "java", "com", "moltobene", "app",
                               "data", "voice", "VoiceCommands.kt"), encoding="utf-8").read()
    befehle = re.findall(r'^\s+"([^"]+)" to VoiceCommand\.', quelle, re.M)
    koeder = re.search(r'DECOY_WORDS: String = """(.*?)"""', quelle, re.S).group(1).split()
    return befehle, koeder


BEFEHLE, KOEDER = _lese_app_listen()
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
        "B Befehle + Köder (wie in der App)": BEFEHLE + list(dict.fromkeys(KOEDER)) + ["[unk]"],
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
