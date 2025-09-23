#!/usr/bin/env python
"""index_community_entries.py
将 MySQL 中的社区条目数据向量化并写入 Elasticsearch。
专门为社区页面的语义搜索功能设计。

运行前请先：
1. pip install -r requirements.txt
2. 确保 Elasticsearch 已运行且安全已关闭或已配置好账号密码
3. 修改下方 DB_CONFIG / ES_CONFIG / MODEL_PATH 为你的实际环境
4. 确保 MySQL 中有社区条目数据（community_entries表）
"""

import os
from typing import List, Dict
import mysql.connector
from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch, helpers
import time

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
    "request_timeout": 60,
    "retry_on_timeout": True,
    "verify_certs": False,  # 禁用SSL证书验证
    "ssl_show_warn": False,  # 禁用SSL警告
}

INDEX_NAME = os.getenv("ES_INDEX", "community_entries")
MODEL_PATH = os.getenv("MODEL_PATH", "models-chinese")
BATCH_SIZE = 32  # 一次编码多少条记录，社区条目通常较少，使用较小的批次
# -------------------------------------------------

def fetch_community_entries(conn) -> List[Dict]:
    """从 MySQL 读取所有社区条目数据，包含关联信息"""
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
    WHERE i.permission_status = '所有人可见'
    GROUP BY ce.id, ce.share_code, ce.description, ce.view_count, ce.created_at, ce.updated_at,
             i.title, i.start_date, i.end_date, u.username, u.id
    ORDER BY ce.created_at DESC
    """
    cursor.execute(query)
    rows = cursor.fetchall()
    cursor.close()
    return rows

def create_index_if_needed(es: Elasticsearch, dim: int):
    """若索引不存在则创建"""
    if es.indices.exists(index=INDEX_NAME):
        print(f"Index '{INDEX_NAME}' already exists.")
        return

    mapping = {
        "mappings": {
            "properties": {
                "id": {"type": "long"},
                "share_code": {"type": "keyword"},
                "description": {"type": "text"},
                "view_count": {"type": "integer"},
                "created_at": {"type": "date"},
                "updated_at": {"type": "date"},
                "itinerary_title": {"type": "text"},
                "start_date": {"type": "date"},
                "end_date": {"type": "date"},
                "destinations": {"type": "text"},
                "author_username": {"type": "text"},
                "author_id": {"type": "long"},
                "tags": {"type": "text"},
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
    if len(text.strip()) < 2:
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

def generate_actions(rows: List[Dict], vecs: List[List[float]]):
    """组装 bulk actions"""
    for row, vec in zip(rows, vecs):
        try:
            yield {
                "_index": INDEX_NAME,
                "_id": row["id"],
                "_source": {
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
                    "vector": vec,
                },
            }
        except Exception as e:
            print(f"生成action时出错 (ID {row['id']}): {e}")
            continue

def main():
    print(f"=== 开始为社区条目创建向量索引 ===")
    
    # 1. 初始化资源
    print("1. 初始化模型和连接...")
    try:
        model = SentenceTransformer(MODEL_PATH, device="cpu")
        es = Elasticsearch(**ES_CONFIG)
        
        if not es.ping():
            raise RuntimeError(f"Elasticsearch cluster is not reachable")
        
        conn = mysql.connector.connect(**DB_CONFIG)
        dim = model.get_sentence_embedding_dimension()
        print(f"Model loaded. Embedding dim = {dim}")
    except Exception as e:
        print(f"❌ 初始化失败: {e}")
        return

    # 2. 创建索引
    try:
        create_index_if_needed(es, dim)
    except Exception as e:
        print(f"❌ 创建索引失败: {e}")
        return

    # 3. 读取数据
    print("2. 读取社区条目数据...")
    try:
        rows = fetch_community_entries(conn)
        print(f"Fetched {len(rows)} community entry records.")
    except Exception as e:
        print(f"❌ 读取数据失败: {e}")
        return

    if len(rows) == 0:
        print("警告：没有找到任何社区条目数据！")
        return

    # 4. 数据质量检查
    print("3. 数据质量检查...")
    valid_rows = []
    invalid_rows = []
    
    for row in rows:
        is_valid, reason = validate_community_entry_data(row)
        if is_valid:
            valid_rows.append(row)
        else:
            invalid_rows.append({
                "id": row["id"],
                "title": row["itinerary_title"],
                "reason": reason
            })
    
    print(f"有效数据: {len(valid_rows)} 个条目")
    print(f"无效数据: {len(invalid_rows)} 个条目")
    
    if invalid_rows:
        print("无效条目详情:")
        for invalid in invalid_rows[:10]:
            print(f"  - ID {invalid['id']}: {invalid['title']} - {invalid['reason']}")
        if len(invalid_rows) > 10:
            print(f"  ... 还有 {len(invalid_rows) - 10} 个无效条目")

    # 5. 批量向量化 + 写入 ES
    print("4. 开始向量化和索引...")
    total_indexed = 0
    failed_indices = []
    start_time = time.time()
    
    for i in range(0, len(valid_rows), BATCH_SIZE):
        batch = valid_rows[i : i + BATCH_SIZE]
        batch_num = i // BATCH_SIZE + 1
        total_batches = (len(valid_rows) + BATCH_SIZE - 1) // BATCH_SIZE
        
        try:
            print(f"处理批次 {batch_num}/{total_batches} ({len(batch)} 个条目)...")
            
            # 准备文本
            texts = []
            for row in batch:
                try:
                    text = prepare_text_for_embedding(row)
                    texts.append(text)
                except Exception as e:
                    failed_indices.append({
                        "id": row["id"],
                        "title": row["itinerary_title"],
                        "error": f"文本准备失败: {e}"
                    })
                    print(f"准备文本失败 (ID {row['id']}): {e}")
                    continue
            
            if not texts:
                print(f"批次 {batch_num} 没有有效文本，跳过")
                continue
            
            # 向量化
            try:
                vecs = model.encode(texts, normalize_embeddings=True).tolist()
            except Exception as e:
                print(f"向量化失败 (批次 {batch_num}): {e}")
                for row in batch:
                    failed_indices.append({
                        "id": row["id"],
                        "title": row["itinerary_title"],
                        "error": f"向量化失败: {e}"
                    })
                continue
            
            # 批量写入ES
            try:
                actions = list(generate_actions(batch, vecs))
                if actions:
                    success_count, errors = helpers.bulk(es, actions, raise_on_error=False)
                    total_indexed += success_count
                    
                    if errors:
                        for error in errors:
                            failed_indices.append({
                                "id": error.get("index", {}).get("_id", "unknown"),
                                "title": "unknown",
                                "error": f"ES写入失败: {error.get('index', {}).get('error', {}).get('reason', 'unknown error')}"
                            })
                    
                    print(f"批次 {batch_num} 完成: {success_count}/{len(batch)} 成功")
                else:
                    print(f"批次 {batch_num} 没有有效actions")
                    
            except Exception as e:
                print(f"ES批量写入失败 (批次 {batch_num}): {e}")
                for row in batch:
                    failed_indices.append({
                        "id": row["id"],
                        "title": row["itinerary_title"],
                        "error": f"ES写入失败: {e}"
                    })
            
        except Exception as e:
            print(f"处理批次 {batch_num} 时出错: {e}")
            for row in batch:
                failed_indices.append({
                    "id": row["id"],
                    "title": row["itinerary_title"],
                    "error": f"批次处理失败: {e}"
                })

    # 6. 验证索引结果
    processing_time = time.time() - start_time
    print("5. 验证索引结果...")
    try:
        stats = es.indices.stats(index=INDEX_NAME)
        actual_count = stats['indices'][INDEX_NAME]['total']['docs']['count']
        print(f"索引验证：Elasticsearch中实际索引了 {actual_count} 个条目")
        
        expected_count = len(valid_rows)
        if actual_count != expected_count:
            print(f"⚠️  警告：索引数量不匹配。有效数据{expected_count}个，但Elasticsearch中只有{actual_count}个")
            if failed_indices:
                print(f"失败索引的条目数量: {len(failed_indices)}")
                print("前10个失败的条目:")
                for i, failed in enumerate(failed_indices[:10]):
                    print(f"  {i+1}. ID {failed['id']}: {failed['title']} - {failed['error']}")
                if len(failed_indices) > 10:
                    print(f"  ... 还有 {len(failed_indices) - 10} 个失败的条目")
        else:
            print("✅ 索引数量匹配！")
            
    except Exception as e:
        print(f"验证索引结果时出错: {e}")

    # 7. 总结
    print("=== 索引完成 ===")
    print(f"索引名称：{INDEX_NAME}")
    print(f"原始条目数量：{len(rows)}")
    print(f"有效条目数量：{len(valid_rows)}")
    print(f"无效条目数量：{len(invalid_rows)}")
    print(f"成功索引数量：{total_indexed}")
    print(f"失败索引数量：{len(failed_indices)}")
    print(f"向量维度：{dim}")
    print(f"处理耗时：{processing_time:.2f}秒")
    
    if invalid_rows or failed_indices:
        print("\n💡 建议:")
        print("1. 检查失败的条目数据质量")
        print("2. 清理或修复无效的条目数据")
        print("3. 重新运行索引脚本")
    
    conn.close()

if __name__ == "__main__":
    main()

# -------------------- requirements.txt 建议 --------------------
# sentence-transformers==2.6.1
# torch>=2.2.0
# mysql-connector-python==8.3.0
# elasticsearch==8.13.0
# tqdm==4.66.2 