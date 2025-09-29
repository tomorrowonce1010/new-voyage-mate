import os
import json
from typing import List

from src.utils import PROC_DIR, Chunk, split_markdown


def read_jsonl(path: str) -> List[dict]:
	with open(path, "r", encoding="utf-8") as f:
		return [json.loads(line) for line in f]


def process(raw_jsonl: str) -> str:
	records = read_jsonl(raw_jsonl)
	chunks: List[Chunk] = []
	for idx, rec in enumerate(records):
		source = rec.get("url", f"doc-{idx}")
		text = rec.get("markdown", "")
		for j, ch in enumerate(split_markdown(text)):
			chunks.append(Chunk(id=f"{idx}-{j}", source=source, text=ch))
	os.makedirs(PROC_DIR, exist_ok=True)
	out_path = os.path.join(PROC_DIR, os.path.basename(raw_jsonl).replace(".jsonl", "_chunks.jsonl"))
	with open(out_path, "w", encoding="utf-8") as f:
		for c in chunks:
			f.write(json.dumps({"id": c.id, "source": c.source, "text": c.text}, ensure_ascii=False) + "\n")
	return out_path


if __name__ == "__main__":
	import sys
	print(process(sys.argv[1]))

