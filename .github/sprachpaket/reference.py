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
        "1 packet active dry yeast", "Divide the dough into 4 dough balls.",
        "Line a baking sheet with parchment paper.", "Knead with the dough hook for 8 minutes.",
        "Dust with powdered sugar.", "2 1/4 tsp instant yeast", "Let the preferment rest overnight.",
        "Place the dough balls on a sheet pan.", "Bake in a convection oven at 200 °C.", "1 cup heavy cream",
        "Grease a 9-inch cake pan.", "Grease a tart pan.", "Grease a Bundt pan.", "Line a muffin pan with paper cups.",
        "Pour into a baking pan.", "Roll out the dough with a rolling pin.", "Mix in a stand mixer for 5 minutes.",
        "1/2 cup sour cream", "200 g cream cheese", "1 tsp vanilla extract", "100 g sourdough starter",
        "Rising time: 2 hours", "Place on a baking tray.", "Spread the final dough evenly.",
    ],
    "de-en": [
        "Linsensuppe",
        "250 g Tellerlinsen", "1 Bund Suppengemüse", "2 Kartoffeln", "1,5 l Gemüsebrühe", "2 EL Essig",
        "Suppengemüse und Kartoffeln würfeln.", "Mit Linsen und Brühe 30 Minuten köcheln lassen.",
        "Mit Essig abschmecken.", "1 Prise Salz", "2 EL Olivenöl", "Für die Soße:",
        "Zwiebeln in feine Würfel schneiden.", "Abschmecken und mit Petersilie bestreuen.",
        "Die Butter schaumig rühren.", "Den Teig 30 Minuten ruhen lassen.", "Mit Salz und Pfeffer würzen.",
        "Eine Springform (26 cm) einfetten.",
        "21 g Frischhefe (½ Würfel)", "1 Päckchen Trockenhefe", "Den Vorteig 16 Stunden gehen lassen.",
        "Den Teig in 4 Teigkugeln teilen.", "Ein Blech mit Backpapier belegen.",
        "Bei 220 °C Ober-/Unterhitze 12 Minuten backen.", "Mit Puderzucker bestäuben.", "200 ml Schlagsahne",
        "3 Frühlingszwiebeln", "7 g Trockenhefe", "1 Würfel Frischhefe", "Den Vorteig mit dem Hauptteig verkneten.",
        "Die Teiglinge 2 Stunden gehen lassen.", "Bei 180 °C Umluft 25 Minuten backen.", "Den Teig auf das Blech geben.",
        "1 Päckchen Backpulver", "Den Teig mit dem Knethaken kneten.",
        "Eine Kastenform einfetten.", "Eine Gugelhupfform einfetten.", "Ein Muffinblech einfetten.",
        "Eine Tarteform mit dem Teig auslegen.", "Den Teig in die Backform füllen.", "Den Teig mit dem Nudelholz ausrollen.",
        "In der Küchenmaschine 5 Minuten kneten.", "1 Päckchen Vanillezucker", "200 g Schmand", "100 g Sauerteig",
        "Gehzeit: 2 Stunden", "Den Hauptteig gut verkneten.",
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
