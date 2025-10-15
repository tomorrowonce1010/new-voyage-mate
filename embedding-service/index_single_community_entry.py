#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""index_single_community_entry.py
为单个社区条目生成向量并索引到 Elasticsearch。
用于实时索引新分享的行程。

使用方法:
    python index_single_community_entry.py <community_entry_id>
"""

import os
import sys
import mysql.connector
from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch
import json
from typing import Dict, Optional

# 设置UTF-8编码
if sys.platform.startswith('win'):
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.detach())

# -------------------- 配置区域 --------------------
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 3306)),
    "user": os.getenv("DB_USER", "voyagemate"),
    "password": os.getenv("DB_PASSWORD", "se_202507"),
    "database": os.getenv("DB_NAME", "voyagemate"),
}

ES_CONFIG = {
    "hosts": [os.getenv("ES_HOST", "http://localhost:9200")],
    "request_timeout": 60,
    "retry_on_timeout": True,
    "verify_certs": False,  # 禁用SSL证书验证
    "ssl_show_warn": False,  # 禁用SSL警告
}

INDEX_NAME = os.getenv("ES_INDEX", "community_entries")
MODEL_PATH = "./models-chinese"
# -------------------------------------------------

def fetch_single_community_entry(conn, entry_id: int) -> Optional[Dict]:
    """从 MySQL 读取单个社区条目数据，包含关联信息"""
    cursor = conn.cursor(dictionary=True)
    query = """
    SELECT 
        ce.id,
        ce.share_code,
        ce.description,
        ce.view_count,
        ce.created_at,
        ce.updated_at,
        i.title as itinerary_title,
        i.start_date,
        i.end_date,
        GROUP_CONCAT(DISTINCT d.name SEPARATOR ', ') as destinations,
        u.username as author_username,
        u.id as author_id,
        GROUP_CONCAT(DISTINCT t.tag SEPARATOR ', ') as tags
    FROM community_entries ce
    JOIN itineraries i ON ce.itinerary_id = i.id
    JOIN user u ON i.user_id = u.id
    LEFT JOIN itinerary_days iday ON i.id = iday.itinerary_id
    LEFT JOIN itinerary_activities ia ON iday.id = ia.itinerary_day_id
    LEFT JOIN attractions a ON ia.attraction_id = a.id
    LEFT JOIN destinations d ON a.destination_id = d.id
    LEFT JOIN community_entry_tags cet ON ce.id = cet.share_entry_id
    LEFT JOIN tags t ON cet.tag_id = t.id
    WHERE ce.id = %s AND i.permission_status = '所有人可见'
    GROUP BY ce.id, ce.share_code, ce.description, ce.view_count, ce.created_at, ce.updated_at,
             i.title, i.start_date, i.end_date, u.username, u.id
    """
    cursor.execute(query, (entry_id,))
    row = cursor.fetchone()
    cursor.close()
    return row

def prepare_text_for_embedding(row: Dict) -> str:
    """准备用于向量化的文本"""
    # 组合文本：行程名称 + 描述 + 作者名称 + 目的地 + 标签
    text_parts = []
    
    # 行程名称
    if row["itinerary_title"]:
        text_parts.append(row["itinerary_title"])
    
    # 描述
    if row["description"]:
        text_parts.append(row["description"])
    
    # 作者名称
    if row["author_username"]:
        text_parts.append(f"作者: {row['author_username']}")
    
    # 目的地
    if row["destinations"]:
        text_parts.append(f"目的地: {row['destinations']}")
    
    # 标签
    if row["tags"]:
        text_parts.append(f"标签: {row['tags']}")
    
    return " ".join(text_parts)

def validate_community_entry_data(row: Dict) -> tuple[bool, str]:
    """验证社区条目数据质量"""
    # 检查必要字段
    if not row["itinerary_title"] or not row["itinerary_title"].strip():
        return False, "行程名称为空"
    
    # 检查文本长度
    text = prepare_text_for_embedding(row)
    if len(text.strip()) < 1:
        return False, f"文本太短: '{text}'"
    
    # 检查特殊字符
    title = row["itinerary_title"] or ""
    desc = row["description"] or ""
    
    # 检查控制字符
    control_chars = ['\x00', '\x01', '\x02', '\x03', '\x04', '\x05', '\x06', '\x07', 
                     '\x08', '\x0b', '\x0c', '\x0e', '\x0f']
    
    for char in control_chars:
        if char in title or char in desc:
            return False, f"包含控制字符: {repr(char)}"
    
    return True, ""

def index_community_entry(entry_id: int) -> bool:
    """索引单个社区条目"""
    try:
        # 1. 初始化资源
        print(f"开始索引社区条目 ID: {entry_id}")
        
        model = SentenceTransformer(MODEL_PATH, device="cpu")
        es = Elasticsearch(**ES_CONFIG)
        
        if not es.ping():
            raise RuntimeError("Elasticsearch cluster is not reachable")
        
        conn = mysql.connector.connect(**DB_CONFIG)
        
        # 2. 读取数据
        row = fetch_single_community_entry(conn, entry_id)
        if not row:
            print(f"[ERROR] 未找到社区条目 ID: {entry_id}")
            return False
        
        print(f"找到社区条目: {row['itinerary_title']}")
        
        # 3. 数据质量检查
        is_valid, reason = validate_community_entry_data(row)
        if not is_valid:
            print(f"[ERROR] 数据质量检查失败: {reason}")
            return False
        
        # 4. 准备文本并向量化
        text = prepare_text_for_embedding(row)
        print(f"准备向量化文本: {text[:100]}...")
        
        vector = model.encode([text], normalize_embeddings=True)[0].tolist()
        
        # 5. 写入 Elasticsearch
        doc = {
            "id": row["id"],
            "share_code": row["share_code"],
            "description": row["description"] or "",
            "view_count": row["view_count"] or 0,
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
            "itinerary_title": row["itinerary_title"],
            "start_date": row["start_date"],
            "end_date": row["end_date"],
            "destinations": row["destinations"] or "",
            "author_username": row["author_username"],
            "author_id": row["author_id"],
            "tags": row["tags"] or "",
            "vector": vector,
        }
        
        # 检查索引是否存在
        if not es.indices.exists(index=INDEX_NAME):
            print(f"[ERROR] 索引 '{INDEX_NAME}' 不存在，请先运行 index_community_entries.py")
            return False
        
        # 写入文档
        response = es.index(index=INDEX_NAME, id=str(entry_id), body=doc)
        
        if response["result"] in ["created", "updated"]:
            print(f"[SUCCESS] 成功索引社区条目 ID: {entry_id}")
            return True
        else:
            print(f"[ERROR] 索引失败: {response}")
            return False
            
    except Exception as e:
        print(f"[ERROR] 索引过程中发生错误: {e}")
        return False
    finally:
        if 'conn' in locals():
            conn.close()

def main():
    if len(sys.argv) != 2:
        print("使用方法: python index_single_community_entry.py <community_entry_id>")
        sys.exit(1)
    
    try:
        entry_id = int(sys.argv[1])
    except ValueError:
        print("[ERROR] 社区条目ID必须是整数")
        sys.exit(1)
    
    success = index_community_entry(entry_id)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 
