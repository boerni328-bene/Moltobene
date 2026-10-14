#!/usr/bin/env python3
"""Baut das Erkennungspaket für die Spracheingabe (#80) nachvollziehbar aus dem Original.

1. Lädt das kleine Vosk-Modell für Deutsch von alphacephei.com (Apache 2.0) und prüft die feste Prüfsumme.
2. Legt alle Dateien unter `de/` ab und ergänzt `NOTICE.txt` mit Herkunft und Lizenz.
3. Schreibt `erkennungspaket.json` (Version, Prüfsumme und Größe jeder Datei).
4. Packt alles in `erkennungspaket-de-<version>.zip`; das Manifest steht als erste Datei im Archiv und das Archiv
   entsteht bei jedem Bau Byte für Byte gleich (feste Reihenfolge, feste Zeitstempel).

Aufruf: build_pack.py <Ausgabeverzeichnis>
"""
import hashlib
import json
import os
import shutil
import sys
import urllib.request
import zipfile

PACK_VERSION = 1

MODEL = "vosk-model-small-de-0.15"
MODEL_URL = f"https://alphacephei.com/vosk/models/{MODEL}.zip"
# Gemessen am 14.10.2026 (Lauf „Erkennungspaket“ Nr. 1); ein verändertes Original wird nicht angenommen.
MODEL_SHA256 = "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"
MODEL_BYTES = 46499967

NOTICE = f"""Erkennungspaket für die Spracheingabe in Moltobene

Enthalten ist unverändert das Modell "{MODEL}" von Alpha Cephei Inc.
Quelle: {MODEL_URL}
Lizenz: Apache License 2.0 (Text in de/COPYING). Copyright 2020 Alpha Cephei Inc.
Es wird nur umverpackt (Archiv mit Prüfsummen), nichts am Modell verändert.
"""


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def main(out):
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    original = os.path.join(out, f"{MODEL}.zip")
    urllib.request.urlretrieve(MODEL_URL, original)
    if sha256(original) != MODEL_SHA256:
        sys.exit(f"Das Original hat eine andere Prüfsumme als erwartet: {sha256(original)}")
    pack = os.path.join(out, "pack")
    os.makedirs(os.path.join(pack, "de"))
    with zipfile.ZipFile(original) as z:
        for info in z.infolist():
            if info.is_dir():
                continue
            name = info.filename.split("/", 1)[1]  # ohne das Verzeichnis „vosk-model-small-de-0.15/“
            target = os.path.join(pack, "de", name)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with z.open(info) as src, open(target, "wb") as dst:
                shutil.copyfileobj(src, dst)
    with open(os.path.join(pack, "NOTICE.txt"), "w", encoding="utf-8") as f:
        f.write(NOTICE)
    files = {}
    for root, _, names in os.walk(pack):
        for name in names:
            path = os.path.join(root, name)
            rel = os.path.relpath(path, pack).replace(os.sep, "/")
            if rel != "erkennungspaket.json":
                files[rel] = {"sha256": sha256(path), "size": os.path.getsize(path)}
    manifest = {"version": PACK_VERSION, "model": MODEL, "files": dict(sorted(files.items()))}
    manifest_path = os.path.join(pack, "erkennungspaket.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, sort_keys=True)
    archive = os.path.join(out, f"erkennungspaket-de-{PACK_VERSION}.zip")
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in ["erkennungspaket.json"] + sorted(files):
            info = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            with open(os.path.join(pack, name), "rb") as f:
                z.writestr(info, f.read(), compresslevel=9)
    print(json.dumps(manifest, indent=2))
    print(f"Verzeichnis: sha256={sha256(manifest_path)}")
    print(f"Paket: {archive}  {os.path.getsize(archive) / 1e6:.1f} MB  sha256={sha256(archive)}")
    total = sum(entry["size"] for entry in files.values())
    print(f"Entpackt: {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main(sys.argv[1])
