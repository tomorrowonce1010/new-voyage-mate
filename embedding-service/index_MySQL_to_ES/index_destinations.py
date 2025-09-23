#!/usr/bin/env python
"""index_destinations.py
批量将 MySQL 中的目的地数据向量化并写入 Elasticsearch。
运行前请先：
1. pip install -r requirements.txt  （见下方 requirements 建议）
2. 确保 Elasticsearch 已运行且安全已关闭或已配置好账号密码。
3. 修改下方 DB_CONFIG / ES_CONFIG / MODEL_PATH 为你的实际环境。
"""

import os
from typing import List, Dict
import mysql.connector
from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch, helpers

# -------------------- 配置区域 --------------------
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 3306)),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", "8001015002yzf"),
    "database": os.getenv("DB_NAME", "voyagemate"),
}

ES_CONFIG = {
    "hosts": [os.getenv("ES_HOST", "http://localhost:9200")],
    "request_timeout": 60,      # 增大全局超时时间，默认 10s 容易超时
    "retry_on_timeout": True,
    # 若开启安全，可在此传用户密码：
    # "basic_auth": (os.getenv("ES_USER", "elastic"), os.getenv("ES_PASS", "changeme"))
}

INDEX_NAME = os.getenv("ES_INDEX", "destinations")
MODEL_PATH = os.getenv("MODEL_PATH", "models-chinese")  # 本地模型目录
BATCH_SIZE = 64  # 一次编码多少条记录
# -------------------------------------------------


def fetch_destinations(conn) -> List[Dict]:
    """从 MySQL 读取 id、name、description"""
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, name, description FROM destinations")
    rows = cursor.fetchall()
    cursor.close()
    return rows


def create_index_if_needed(es: Elasticsearch, dim: int):
    """若索引不存在则创建"""
    if es.indices.exists(index=INDEX_NAME):
        return

    mapping = {
        "mappings": {
            "properties": {
                "id": {"type": "integer"},
                "name": {"type": "text"},
                "description": {"type": "text"},
                "vector": {
                    "type": "dense_vector",
                    "dims": dim,
                    "index": True,
                    "similarity": "cosine",
                },
            }
        }
    }
    es.indices.create(index=INDEX_NAME, body=mapping)
    print(f"Index '{INDEX_NAME}' created.")


def generate_actions(rows: List[Dict], vecs: List[List[float]]):
    """组装 bulk actions"""
    for row, vec in zip(rows, vecs):
        yield {
            "_index": INDEX_NAME,
            "_id": row["id"],
            "_source": {
                "id": row["id"],
                "name": row["name"],
                "description": row["description"],
                "vector": vec,
            },
        }


def main():
    # 1. 初始化资源
    model = SentenceTransformer(MODEL_PATH, device="cpu")
    es = Elasticsearch(**ES_CONFIG)
    # 等待 ES 可用
    if not es.ping():
        raise RuntimeError("Elasticsearch cluster is not reachable. Ensure it is running and accessible at {}".format(ES_CONFIG["hosts"][0]))
    conn = mysql.connector.connect(**DB_CONFIG)

    dim = model.get_sentence_embedding_dimension()
    print(f"Model loaded. Embedding dim = {dim}")

    create_index_if_needed(es, dim)

    # 2. 读取数据
    rows = fetch_destinations(conn)
    print(f"Fetched {len(rows)} destination records.")

    # 3. 批量向量化 + 写入 ES
    total_indexed = 0
    for i in range(0, len(rows), BATCH_SIZE):
        batch = rows[i : i + BATCH_SIZE]
        texts = [f"{r['name']} {r['description'] or ''}" for r in batch]
        vecs = model.encode(texts, normalize_embeddings=True).tolist()
        helpers.bulk(es, generate_actions(batch, vecs))
        total_indexed += len(batch)
        print(f"Indexed {total_indexed}/{len(rows)}")

    print("All done!")
    conn.close()


if __name__ == "__main__":
    main()

# -------------------- requirements.txt 建议 --------------------
# sentence-transformers==2.6.1
# torch>=2.2.0        # 根据环境 GPU/CPU 选择版本
# mysql-connector-python==8.3.0
# elasticsearch==8.13.0
# tqdm==4.66.2        # 可选，用于进度条 