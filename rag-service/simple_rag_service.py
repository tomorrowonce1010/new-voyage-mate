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

# 配置 - 修改为使用本地模型路径
MODEL_BASE_PATH = os.path.dirname(os.path.abspath(__file__))
EMBED_MODEL = os.path.join(MODEL_BASE_PATH, "models", "paraphrase-multilingual-MiniLM-L12-v2")
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
        # 检查模型文件是否存在
        self._check_model_files()
        
        # 加载本地模型
        print(f"正在加载本地模型: {EMBED_MODEL}")
        self.embedder = SentenceTransformer(EMBED_MODEL)
        
        self.index = None
        self.chunks = []
        self.metas = []
        self._load_knowledge_base()
    
    def _check_model_files(self):
        """检查模型文件完整性"""
        required_files = [
            "config.json",
            "model.safetensors",  # 或者 pytorch_model.bin
            "tokenizer_config.json",
            "vocab.txt",
            "sentence_bert_config.json"
        ]
        
        missing_files = []
        for file in required_files:
            file_path = os.path.join(EMBED_MODEL, file)
            if not os.path.exists(file_path):
                missing_files.append(file)
        
        if missing_files:
            print(f"⚠️ 缺少以下模型文件: {missing_files}")
            print("尝试继续加载，但可能会遇到问题...")
        else:
            print("✅ 所有必需的模型文件都存在")
        
        # 列出实际存在的文件
        existing_files = os.listdir(EMBED_MODEL)
        print(f"模型目录中的文件: {existing_files}")
    
    def _load_knowledge_base(self):
        """加载知识库"""
        try:
            # 检查索引目录是否存在
            if not os.path.exists(INDEX_DIR):
                raise FileNotFoundError(f"索引目录不存在: {INDEX_DIR}")
            
            # 查找可用的索引文件
            index_files = [f for f in os.listdir(INDEX_DIR) if f.endswith('.faiss')]
            if not index_files:
                raise FileNotFoundError("未找到FAISS索引文件")
            
            # 使用第一个找到的索引
            index_name = index_files[0].replace('.faiss', '')
            index_path = os.path.join(INDEX_DIR, f"{index_name}.faiss")
            meta_path = os.path.join(INDEX_DIR, f"{index_name}_meta.json")
            chunks_path = os.path.join(DATA_DIR, f"{index_name}.jsonl")
            
            # 检查文件是否存在
            for path in [index_path, meta_path, chunks_path]:
                if not os.path.exists(path):
                    raise FileNotFoundError(f"文件不存在: {path}")
            
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
            # 创建一个空的索引和文本块，让服务至少能启动
            self.index = None
            self.chunks = []
            self.metas = []
    
    def search(self, query: str, top_k: int = 5) -> str:
        """搜索相关文本"""
        if self.index is None:
            # 如果知识库未加载，返回空上下文
            return "知识库暂不可用，请检查索引文件。"
        
        try:
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
                    score = scores[0][i]
                    context_parts.append(f"来源: {source} (相似度: {score:.3f})\n\n{text}")
            
            if not context_parts:
                return "未找到相关信息。"
            
            return "\n\n---\n\n".join(context_parts)
            
        except Exception as e:
            return f"搜索过程中出错: {str(e)}"

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
        "model_path": EMBED_MODEL,
        "endpoints": {
            "/health": "健康检查",
            "/search": "搜索相关文本",
            "/ask": "智能问答"
        }
    }

@app.get("/health")
async def health_check():
    """健康检查"""
    status = "healthy"
    if rag_service is None or rag_service.index is None:
        status = "degraded"
    
    return {
        "status": status,
        "knowledge_base_loaded": rag_service is not None and rag_service.index is not None,
        "chunks_count": len(rag_service.chunks) if rag_service else 0,
        "model_loaded": rag_service is not None and rag_service.embedder is not None
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
            "chunks_found": len(context.split("---")) if context and "---" in context else 0
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
            "chunks_used": len(context.split("---")) if context and "---" in context else 0
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
