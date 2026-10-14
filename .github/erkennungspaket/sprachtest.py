#!/usr/bin/env python3
"""Prüft die Befehlserkennung (#80) mit dem echten deutschen Modell aus dem Erkennungspaket.

Die Befehle werden mit einer Computerstimme (espeak-ng) gesprochen und mit der festen Befehlsliste erkannt, so wie die App
sie benutzt. Das ersetzt keinen Test mit echter Stimme auf dem Handy, zeigt aber, ob die Wörter im Modell vorkommen und ob
Alltagssätze fälschlich einen Befehl auslösen. Aufruf: sprachtest.py <Modellverzeichnis> <Arbeitsverzeichnis>
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
GRAMMATIK = BEFEHLE + ["[unk]"]
# Sätze aus der Küche, die keinen Befehl auslösen dürfen.
ALLTAG = [
    "ich brauche noch zwei eier und etwas mehl",
    "die zwiebeln müssen goldbraun werden",
    "kannst du mir bitte das salz geben",
    "das wasser kocht schon",
    "gleich ist das essen fertig",
    "wir gehen danach weiter einkaufen",
]
GESCHWINDIGKEITEN = [120, 150, 180]


def sprechen(text, ziel, tempo):
    roh = ziel + ".roh.wav"
    subprocess.run(["espeak-ng", "-v", "de", "-s", str(tempo), "-w", roh, text], check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", roh, "-ar", "16000", "-ac", "1", ziel], check=True)


def erkennen(modell, wav):
    rec = KaldiRecognizer(modell, 16000, json.dumps(GRAMMATIK, ensure_ascii=False))
    rec.SetWords(True)
    with wave.open(wav, "rb") as f:
        while True:
            daten = f.readframes(4000)
            if not daten:
                break
            rec.AcceptWaveform(daten)
    return json.loads(rec.FinalResult())


def worte(ergebnis):
    return [(w["word"], round(w.get("conf", 0), 2)) for w in ergebnis.get("result", [])]


def main(modell_pfad, arbeit):
    os.makedirs(arbeit, exist_ok=True)
    SetLogLevel(0)
    modell = Model(modell_pfad)
    treffer = 0
    gesamt = 0
    print("== Befehle (erwartet: genau der gesprochene Befehl)")
    for befehl in BEFEHLE:
        for tempo in GESCHWINDIGKEITEN:
            wav = os.path.join(arbeit, f"{befehl.replace(' ', '_')}_{tempo}.wav")
            sprechen(befehl, wav, tempo)
            ergebnis = erkennen(modell, wav)
            text = ergebnis.get("text", "")
            gesamt += 1
            richtig = text == befehl
            treffer += richtig
            print(f"{'OK  ' if richtig else 'FEHL'} {befehl!r} @{tempo}: {text!r} {worte(ergebnis)}")
    print(f"Befehle erkannt: {treffer} von {gesamt}")
    print("== Alltagssätze (erwartet: kein Befehl; Wörter aus der Befehlsliste wären ein Fehlalarm)")
    falsch = 0
    for satz in ALLTAG:
        wav = os.path.join(arbeit, "alltag.wav")
        sprechen(satz, wav, 150)
        ergebnis = erkennen(modell, wav)
        text = ergebnis.get("text", "")
        ausgeloest = [b for b in BEFEHLE if b in text]
        falsch += bool(ausgeloest)
        print(f"{'FALSCHALARM' if ausgeloest else 'ruhig     '} {satz!r}: {text!r} {worte(ergebnis)}")
    print(f"Fehlalarme: {falsch} von {len(ALLTAG)}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
