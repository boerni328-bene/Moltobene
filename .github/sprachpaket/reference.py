"""
Vergleichswerte für den Kotlin-Übersetzer (#60), zwei Schritte in zwei getrennten Umgebungen:

    python reference.py tokens <ausgabeordner>      (Umgebung mit transformers: Wortteile wie das Original)
    python reference.py translate <ausgabeordner>   (Umgebung mit ONNX Runtime wie in der App, 1 Kern)

Ergebnis: `fixtures.json` mit Zeile, Kennungen der Wortteile, Kennungen und Text der Übersetzung. Der Test
`LanguagePackTest` prüft damit, ob die App Wort für Wort dasselbe Ergebnis liefert. Alle Zeilen sind selbst
geschrieben.
"""
import json
import os
import sys

LINES = {
    "en-de": [
        "Lemon drizzle cake",
        "200 g soft butter", "200 g sugar", "3 eggs", "200 g self-raising flour", "2 tbsp milk",
        "zest of 2 lemons", "For the drizzle:", "juice of 2 lemons", "100 g icing sugar",
        "Heat the oven to 180 °C and line a loaf tin with baking paper.",
        "Beat butter and sugar until pale, then add the eggs one by one.",
        "Fold in flour, milk and lemon zest, fill the tin and bake for about 50 minutes.",
        "Stir lemon juice and icing sugar together and pour over the warm cake.",
        "4 cloves garlic, minced", "Kosher salt and freshly ground black pepper, to taste",
        "2 cups all-purpose flour", "1 bay leaf", "Preheat the oven to 350°F.",
        "Whisk the eggs with a pinch of salt.", "Let the dough rest for 30 minutes.",
        "Season to taste and serve immediately.", "Garnish with chopped parsley.",
    ],
    "de-en": [
        "Linsensuppe",
        "250 g Tellerlinsen", "1 Bund Suppengemüse", "2 Kartoffeln", "1,5 l Gemüsebrühe", "2 EL Essig",
        "Suppengemüse und Kartoffeln würfeln.", "Mit Linsen und Brühe 30 Minuten köcheln lassen.",
        "Mit Essig abschmecken.", "1 Prise Salz", "2 EL Olivenöl", "Für die Soße:",
        "Zwiebeln in feine Würfel schneiden.", "Abschmecken und mit Petersilie bestreuen.",
        "Die Butter schaumig rühren.", "Den Teig 30 Minuten ruhen lassen.", "Mit Salz und Pfeffer würzen.",
        "Eine Springform (26 cm) einfetten.",
    ],
}


def tokens(out):
    from transformers import MarianTokenizer

    result = {}
    for direction, lines in LINES.items():
        tok = MarianTokenizer.from_pretrained(os.path.join(out, "raw", direction))
        result[direction] = [{"line": line, "input_ids": tok([line])["input_ids"][0]} for line in lines]
    json.dump(result, open(os.path.join(out, "tokens.json"), "w"), ensure_ascii=False, indent=1)


def translate(out):
    import numpy as np
    import onnxruntime as ort
    import sentencepiece as spm

    data = json.load(open(os.path.join(out, "tokens.json"), encoding="utf-8"))
    result = {}
    for direction, entries in data.items():
        pack = os.path.join(out, "pack", direction)
        cfg = json.load(open(os.path.join(pack, "config.json")))
        vocab = {}
        for row in open(os.path.join(pack, "vocab.tsv"), encoding="utf-8"):
            index, token = row.rstrip("\n").split("\t")
            vocab[int(index)] = token
        target = spm.SentencePieceProcessor(model_file=os.path.join(out, "raw", direction, "target.spm"))
        options = ort.SessionOptions()
        options.intra_op_num_threads = 1
        options.inter_op_num_threads = 1
        enc = ort.InferenceSession(os.path.join(pack, "encoder.onnx"), options, providers=["CPUExecutionProvider"])
        dec = ort.InferenceSession(os.path.join(pack, "decoder.onnx"), options, providers=["CPUExecutionProvider"])
        rows = []
        for entry in entries:
            ids = np.array([entry["input_ids"]], dtype=np.int64)
            mask = np.ones_like(ids)
            hidden = enc.run(None, {"input_ids": ids, "attention_mask": mask})[0]
            out_ids = [cfg["start"]]
            for _ in range(cfg["maxLength"]):
                logits = dec.run(None, {
                    "input_ids": np.array([out_ids], dtype=np.int64),
                    "encoder_hidden_states": hidden,
                    "encoder_attention_mask": mask,
                })[0]
                last = logits[0, -1].copy()
                last[cfg["pad"]] = -np.inf
                nxt = int(last.argmax())
                out_ids.append(nxt)
                if nxt == cfg["eos"]:
                    break
            pieces = [vocab[i] for i in out_ids if i not in (cfg["eos"], cfg["pad"], cfg["unk"])]
            rows.append({**entry, "output_ids": out_ids, "text": target.decode_pieces(pieces)})
            print(f"{direction}: {entry['line']}  ->  {rows[-1]['text']}")
        result[direction] = rows
    json.dump(result, open(os.path.join(out, "fixtures.json"), "w"), ensure_ascii=False, indent=1)


if __name__ == "__main__":
    {"tokens": tokens, "translate": translate}[sys.argv[1]](sys.argv[2])
