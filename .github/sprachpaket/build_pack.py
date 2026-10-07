"""
Baut das Sprachpaket Deutsch <-> Englisch für „Rezept übersetzen“ (#60), nachvollziehbar und mit festen
Werkzeugversionen (siehe .github/workflows/sprachpaket.yml):

1. lädt OPUS-MT `opus-mt-en-de` und `opus-mt-de-en` der Universität Helsinki (Lizenz CC-BY 4.0),
2. wandelt Encoder und Decoder nach ONNX um und verkleinert sie auf 8 Bit (Ergebnis der Machbarkeitsprobe),
3. schreibt die Wortteile (SentencePiece) als einfache Textdateien, die die App ohne zusätzlichen Baustein liest,
4. packt alles in `sprachpaket-de-en-<version>.zip` mit `sprachpaket.json` (Prüfsummen jeder Datei).

Aufruf: python build_pack.py <ausgabeordner>
Die Testsätze für den Vergleich mit dem Kotlin-Übersetzer schreibt `reference.py`.
"""
import hashlib
import json
import os
import sys
import zipfile

PACK_VERSION = 1
DIRECTIONS = {"en-de": "Helsinki-NLP/opus-mt-en-de", "de-en": "Helsinki-NLP/opus-mt-de-en"}
# Feste Stände der Modelle auf Hugging Face, damit das Paket immer gleich entsteht (in sprachpaket.yml eingetragen;
# „main“ nimmt den neuesten Stand, das Paket nennt dann dessen Kennung).
REVISIONS = {
    "Helsinki-NLP/opus-mt-en-de": os.environ.get("REVISION_EN_DE", "main"),
    "Helsinki-NLP/opus-mt-de-en": os.environ.get("REVISION_DE_EN", "main"),
}


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def resolve(model, revision):
    """Genaue Kennung (Commit) des Modellstands auf Hugging Face."""
    from huggingface_hub import HfApi

    return HfApi().model_info(model, revision=revision).sha


def export(model, revision, target, raw):
    """Umwandeln nach ONNX in [raw] (bleibt außerhalb des Pakets), verkleinert nach [target]."""
    from onnxruntime.quantization import QuantType, quantize_dynamic
    from optimum.exporters.onnx import main_export

    main_export(model, output=raw, task="text2text-generation", revision=revision)
    os.makedirs(target, exist_ok=True)
    for name in ["encoder", "decoder"]:
        quantize_dynamic(
            os.path.join(raw, f"{name}_model.onnx"),
            os.path.join(target, f"{name}.onnx"),
            weight_type=QuantType.QInt8,
        )
    return raw


def write_vocab(raw, target):
    """Wortteile als Text: `vocab.tsv` (Kennung, Wortteil) und `source.tsv` (Wortteil, Wert, Kennung)."""
    import sentencepiece as spm

    vocab = json.load(open(os.path.join(raw, "vocab.json"), encoding="utf-8"))
    config = json.load(open(os.path.join(raw, "config.json"), encoding="utf-8"))
    unk = vocab["<unk>"]
    for token in vocab:
        assert "\t" not in token and "\n" not in token, f"Wortteil mit Tab oder Zeilenumbruch: {token!r}"
    with open(os.path.join(target, "vocab.tsv"), "w", encoding="utf-8", newline="\n") as f:
        for token, index in sorted(vocab.items(), key=lambda item: item[1]):
            f.write(f"{index}\t{token}\n")
    sp = spm.SentencePieceProcessor(model_file=os.path.join(raw, "source.spm"))
    with open(os.path.join(target, "source.tsv"), "w", encoding="utf-8", newline="\n") as f:
        for i in range(sp.get_piece_size()):
            if sp.is_control(i) or sp.is_unknown(i):
                continue
            piece = sp.id_to_piece(i)
            f.write(f"{piece}\t{sp.get_score(i):.6f}\t{vocab.get(piece, unk)}\n")
    settings = {
        "eos": config["eos_token_id"],
        "pad": config["pad_token_id"],
        "start": config.get("decoder_start_token_id", config["pad_token_id"]),
        "unk": unk,
        "vocabSize": config["vocab_size"],
        "maxLength": 256,
    }
    json.dump(settings, open(os.path.join(target, "config.json"), "w"), indent=2)


def write_notice(model, revision, target):
    """Herkunft und Lizenz: CC-BY 4.0 verlangt Namensnennung, Link zur Lizenz und den Hinweis auf Änderungen."""
    with open(os.path.join(target, "NOTICE.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write(
            f"{model.split('/')[-1]}\n"
            f"Source: https://huggingface.co/{model} (revision {revision})\n"
            "Authors: OPUS-MT, Language Technology Research Group at the University of Helsinki\n"
            "License: Creative Commons Attribution 4.0 International (CC BY 4.0), "
            "https://creativecommons.org/licenses/by/4.0/\n"
            "Changes: converted to ONNX, weights quantized to 8 bit, vocabulary written as text files "
            "for Moltobene (https://github.com/boerni328-bene/Moltobene).\n"
        )


def main():
    out = sys.argv[1]
    pack = os.path.join(out, "pack")
    os.makedirs(pack, exist_ok=True)
    revisions = {model: resolve(model, revision) for model, revision in REVISIONS.items()}
    for direction, model in DIRECTIONS.items():
        target = os.path.join(pack, direction)
        raw = export(model, revisions[model], target, os.path.join(out, "raw", direction))
        write_vocab(raw, target)
        write_notice(model, revisions[model], target)
    files = {}
    for root, _, names in os.walk(pack):
        for name in sorted(names):
            path = os.path.join(root, name)
            files[os.path.relpath(path, pack).replace(os.sep, "/")] = {"sha256": sha256(path), "size": os.path.getsize(path)}
    manifest = {
        "format": 1,
        "name": "de-en",
        "version": PACK_VERSION,
        "models": {d: {"source": m, "revision": revisions[m], "license": "CC-BY-4.0"} for d, m in DIRECTIONS.items()},
        "files": dict(sorted(files.items())),
    }
    json.dump(manifest, open(os.path.join(pack, "sprachpaket.json"), "w"), indent=2)
    archive = os.path.join(out, f"sprachpaket-de-en-{PACK_VERSION}.zip")
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in ["sprachpaket.json"] + sorted(files):
            info = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            with open(os.path.join(pack, name), "rb") as f:
                z.writestr(info, f.read(), compresslevel=9)
    print(json.dumps(manifest, indent=2))
    # Die App kennt die Prüfsumme des Verzeichnisses (LanguagePack.MANIFEST_SHA256) und prüft damit jede Datei.
    print(f"Verzeichnis: sha256={sha256(os.path.join(pack, 'sprachpaket.json'))}")
    print(f"Paket: {archive}  {os.path.getsize(archive) / 1e6:.1f} MB  sha256={sha256(archive)}")


if __name__ == "__main__":
    main()
