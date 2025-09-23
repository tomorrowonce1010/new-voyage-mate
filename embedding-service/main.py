"""FastAPI embedding service
启动:
    uvicorn main:app --host 0.0.0.0 --port 8000

POST /embed
Body: {"texts": ["北京美食", "杭州西湖"]}
Return: [[0.1, ...], [0.2, ...]]
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer
from typing import List
import os

MODEL_PATH = os.getenv("MODEL_PATH", "models-chinese")
DEVICE = os.getenv("DEVICE", "cpu")  # 可设 cuda

try:
    model = SentenceTransformer(MODEL_PATH, device=DEVICE)
except Exception as e:
    raise RuntimeError(f"Failed to load model from {MODEL_PATH}: {e}")

app = FastAPI(title="Embedding Service", version="1.0")

class Texts(BaseModel):
    texts: List[str] = Field(..., description="List of input sentences")

@app.post("/embed")
async def embed(texts: Texts):
    if not texts.texts:
        raise HTTPException(status_code=400, detail="texts must not be empty")
    try:
        vectors = model.encode(texts.texts, normalize_embeddings=True).tolist()
        return vectors
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) 