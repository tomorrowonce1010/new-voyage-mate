#!/usr/bin/env python3
"""
简化的RAG服务
提供城市旅游问答功能
"""
import os
import json
from typing import List, Tuple
import faiss
from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sentence_transformers import SentenceTransformer
from src.llm import answer_with_deepseek

# 配置
EMBED_MODEL = "paraphrase-multilingual-MiniLM-L12-v2"
INDEX_DIR = "index"
DATA_DIR = "data/processed"

app = FastAPI(title="简化RAG智能伴游助手")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class SimpleRAGService:
    def __init__(self):
        self.embedder = SentenceTransformer(EMBED_MODEL)
        self.index = None
        self.chunks = []
        self.metas = []
        self._load_knowledge_base()
    
    def _load_knowledge_base(self):
        """加载知识库"""
        try:
            # 查找可用的索引文件
            index_files = [f for f in os.listdir(INDEX_DIR) if f.endswith('.faiss')]
            if not index_files:
                raise FileNotFoundError("未找到FAISS索引文件")
            
            # 使用第一个找到的索引
            index_name = index_files[0].replace('.faiss', '')
            index_path = os.path.join(INDEX_DIR, f"{index_name}.faiss")
            meta_path = os.path.join(INDEX_DIR, f"{index_name}_meta.json")
            chunks_path = os.path.join(DATA_DIR, f"{index_name}.jsonl")
            
            # 加载索引
            self.index = faiss.read_index(index_path)
            
            # 加载元数据
            with open(meta_path, 'r', encoding='utf-8') as f:
                meta_data = json.load(f)
                self.metas = meta_data['metas']
            
            # 加载文本块
            with open(chunks_path, 'r', encoding='utf-8') as f:
                self.chunks = [json.loads(line) for line in f]
            
            print(f"✅ 知识库加载成功: {len(self.chunks)} 个文本块")
            
        except Exception as e:
            print(f"❌ 知识库加载失败: {e}")
            raise
    
    def search(self, query: str, top_k: int = 5) -> str:
        """搜索相关文本"""
        if self.index is None:
            raise HTTPException(status_code=500, detail="知识库未加载")
        
        # 生成查询向量
        query_embedding = self.embedder.encode([query], convert_to_numpy=True, normalize_embeddings=True)
        
        # 搜索
        scores, indices = self.index.search(query_embedding, top_k)
        
        # 构建上下文
        context_parts = []
        for i, idx in enumerate(indices[0]):
            if idx < len(self.chunks):
                chunk = self.chunks[idx]
                source = chunk.get('source', '未知来源')
                text = chunk.get('text', '')
                context_parts.append(f"来源: {source}\n\n{text}")
        
        return "\n\n---\n\n".join(context_parts)

# 全局RAG服务实例
rag_service = None

@app.on_event("startup")
async def startup_event():
    """启动时加载知识库"""
    global rag_service
    try:
        rag_service = SimpleRAGService()
        print("🚀 RAG服务启动成功")
    except Exception as e:
        print(f"❌ RAG服务启动失败: {e}")

@app.get("/")
async def root():
    """根路径"""
    return {
        "message": "简化RAG智能伴游助手",
        "status": "running",
        "endpoints": {
            "/health": "健康检查",
            "/search": "搜索相关文本",
            "/ask": "智能问答"
        }
    }

@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "knowledge_base_loaded": rag_service is not None and rag_service.index is not None,
        "chunks_count": len(rag_service.chunks) if rag_service else 0
    }

@app.get("/search")
async def search_text(query: str = Query(..., min_length=2), top_k: int = 5):
    """搜索相关文本"""
    if rag_service is None:
        raise HTTPException(status_code=500, detail="RAG服务未初始化")
    
    try:
        context = rag_service.search(query, top_k)
        return {
            "query": query,
            "context": context,
            "chunks_found": len(context.split("---")) if context else 0
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")

@app.get("/ask")
async def ask_question(question: str = Query(..., min_length=2), top_k: int = 5):
    """智能问答"""
    if rag_service is None:
        raise HTTPException(status_code=500, detail="RAG服务未初始化")
    
    try:
        # 搜索相关文本
        context = rag_service.search(question, top_k)
        
        # 生成回答
        answer = answer_with_deepseek(question, context)
        
        return {
            "question": question,
            "answer": answer,
            "context": context,
            "chunks_used": len(context.split("---")) if context else 0
        }
    except Exception as e:
        return {
            "question": question,
            "answer": f"抱歉，无法生成回答: {str(e)}",
            "context": rag_service.search(question, top_k) if rag_service else "",
            "error": str(e)
        }

if __name__ == "__main__":
    import uvicorn
    import os
    port = int(os.getenv("PORT", "8001"))
    uvicorn.run(app, host="0.0.0.0", port=port)
