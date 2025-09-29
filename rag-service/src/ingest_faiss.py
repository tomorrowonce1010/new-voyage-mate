import os
import json
from typing import List, Tuple

import numpy as np
import faiss
from sentence_transformers import SentenceTransformer

from src.utils import INDEX_DIR

EMBED_MODEL = "paraphrase-multilingual-MiniLM-L12-v2"


def read_chunks(path: str) -> List[dict]:
	with open(path, "r", encoding="utf-8") as f:
		return [json.loads(line) for line in f]


def build_index(chunks_path: str) -> Tuple[str, str]:
	chunks = read_chunks(chunks_path)
	texts = [c.get("text", "") for c in chunks]
	metas: List[Tuple[str, str]] = []
	for i, c in enumerate(chunks):
		cid = c.get("id", str(i))
		src = c.get("source", c.get("url", ""))
		metas.append((cid, src))
	model = SentenceTransformer(EMBED_MODEL)
	emb = model.encode(texts, batch_size=64, convert_to_numpy=True, show_progress_bar=True, normalize_embeddings=True)
	index = faiss.IndexFlatIP(emb.shape[1])
	index.add(emb)
	os.makedirs(INDEX_DIR, exist_ok=True)
	# name prefix from file stem
	stem = os.path.splitext(os.path.basename(chunks_path))[0]
	index_path = os.path.join(INDEX_DIR, f"{stem}.faiss")
	meta_path = os.path.join(INDEX_DIR, f"{stem}_meta.json")
	faiss.write_index(index, index_path)
	with open(meta_path, "w", encoding="utf-8") as f:
		json.dump({"model": EMBED_MODEL, "metas": metas}, f, ensure_ascii=False)
	return index_path, meta_path


if __name__ == "__main__":
	import sys
	print(build_index(sys.argv[1]))
