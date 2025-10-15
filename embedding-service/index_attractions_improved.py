#!/usr/bin/env python
"""index_attractions_improved.py
改进的景点向量化索引脚本，包含更好的错误处理和数据质量检查
"""

import os
from typing import List, Dict
import mysql.connector
from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch, helpers
import traceback

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
}

DESTINATION_ID = int(os.getenv("DESTINATION_ID", "36"))
INDEX_NAME = os.getenv("ES_INDEX", f"attractions_destination_{DESTINATION_ID}")
MODEL_PATH = "./models-chinese"
BATCH_SIZE = 64

def fetch_attractions(conn, destination_id: int) -> List[Dict]:
    """从 MySQL 读取指定目的地的景点数据"""
    cursor = conn.cursor(dictionary=True)
    query = """
    SELECT 
        a.id, 
        a.name, 
        a.description, 
        a.category,
        a.latitude,
        a.longitude,
        a.join_count,
        a.tag_scores,
        d.name as destination_name
    FROM attractions a
    JOIN destinations d ON a.destination_id = d.id
    WHERE a.destination_id = %s
    ORDER BY a.join_count DESC
    """
    cursor.execute(query, (destination_id,))
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
                "id": {"type": "integer"},
                "name": {"type": "text"},
                "description": {"type": "text"},
                "category": {"type": "keyword"},
                "latitude": {"type": "float"},
                "longitude": {"type": "float"},
                "join_count": {"type": "integer"},
                "tag_scores": {"type": "text"},
                "destination_name": {"type": "text"},
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
    name = row["name"] or ""
    description = row["description"] or ""
    category = row["category"] or ""
    destination_name = row["destination_name"] or ""
    
    # 组合文本：景点名称 + 描述 + 分类 + 目的地名称
    text_parts = [name, description, category, destination_name]
    return " ".join(filter(None, text_parts))

def validate_attraction_data(row: Dict) -> tuple[bool, str]:
    """验证景点数据质量"""
    # 检查必要字段
    if not row["name"] or not row["name"].strip():
        return False, "名称为空"
    
    # 检查文本长度
    text = prepare_text_for_embedding(row)
    if len(text.strip()) < 2:
        return False, f"文本太短: '{text}'"
    
    # 检查特殊字符
    name = row["name"] or ""
    desc = row["description"] or ""
    
    # 检查控制字符
    control_chars = ['\x00', '\x01', '\x02', '\x03', '\x04', '\x05', '\x06', '\x07', 
                     '\x08', '\x0b', '\x0c', '\x0e', '\x0f']
    
    for char in control_chars:
        if char in name or char in desc:
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
                    "name": row["name"],
                    "description": row["description"] or "",
                    "category": row["category"],
                    "latitude": float(row["latitude"]) if row["latitude"] else None,
                    "longitude": float(row["longitude"]) if row["longitude"] else None,
                    "join_count": row["join_count"] or 0,
                    "tag_scores": row["tag_scores"] or "",
                    "destination_name": row["destination_name"],
                    "vector": vec,
                },
            }
        except Exception as e:
            print(f"生成action时出错 (ID {row['id']}): {e}")
            continue

def main():
    print(f"=== 开始为目的地 {DESTINATION_ID} 的景点创建向量索引 ===")
    
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
    print(f"2. 读取目的地 {DESTINATION_ID} 的景点数据...")
    try:
        rows = fetch_attractions(conn, DESTINATION_ID)
        print(f"Fetched {len(rows)} attraction records for destination {DESTINATION_ID}.")
    except Exception as e:
        print(f"❌ 读取数据失败: {e}")
        return

    if len(rows) == 0:
        print(f"警告：目的地 {DESTINATION_ID} 没有找到任何景点数据！")
        return

    # 4. 数据质量检查
    print("3. 数据质量检查...")
    valid_rows = []
    invalid_rows = []
    
    for row in rows:
        is_valid, reason = validate_attraction_data(row)
        if is_valid:
            valid_rows.append(row)
        else:
            invalid_rows.append({
                "id": row["id"],
                "name": row["name"],
                "reason": reason
            })
    
    print(f"有效数据: {len(valid_rows)} 个景点")
    print(f"无效数据: {len(invalid_rows)} 个景点")
    
    if invalid_rows:
        print("无效景点详情:")
        for invalid in invalid_rows[:10]:
            print(f"  - ID {invalid['id']}: {invalid['name']} - {invalid['reason']}")
        if len(invalid_rows) > 10:
            print(f"  ... 还有 {len(invalid_rows) - 10} 个无效景点")

    # 5. 批量向量化 + 写入 ES
    print("4. 开始向量化和索引...")
    total_indexed = 0
    failed_indices = []
    
    for i in range(0, len(valid_rows), BATCH_SIZE):
        batch = valid_rows[i : i + BATCH_SIZE]
        batch_num = i // BATCH_SIZE + 1
        total_batches = (len(valid_rows) + BATCH_SIZE - 1) // BATCH_SIZE
        
        try:
            print(f"处理批次 {batch_num}/{total_batches} ({len(batch)} 个景点)...")
            
            # 准备文本
            texts = []
            for row in batch:
                try:
                    text = prepare_text_for_embedding(row)
                    texts.append(text)
                except Exception as e:
                    failed_indices.append({
                        "id": row["id"],
                        "name": row["name"],
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
                        "name": row["name"],
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
                                "name": "unknown",
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
                        "name": row["name"],
                        "error": f"ES写入失败: {e}"
                    })
            
        except Exception as e:
            print(f"处理批次 {batch_num} 时出错: {e}")
            traceback.print_exc()
            for row in batch:
                failed_indices.append({
                    "id": row["id"],
                    "name": row["name"],
                    "error": f"批次处理失败: {e}"
                })

    # 6. 验证索引结果
    print("5. 验证索引结果...")
    try:
        stats = es.indices.stats(index=INDEX_NAME)
        actual_count = stats['indices'][INDEX_NAME]['total']['docs']['count']
        print(f"索引验证：Elasticsearch中实际索引了 {actual_count} 个景点")
        
        expected_count = len(valid_rows)
        if actual_count != expected_count:
            print(f"⚠️  警告：索引数量不匹配。有效数据{expected_count}个，但Elasticsearch中只有{actual_count}个")
            if failed_indices:
                print(f"失败索引的景点数量: {len(failed_indices)}")
                print("前10个失败的景点:")
                for i, failed in enumerate(failed_indices[:10]):
                    print(f"  {i+1}. ID {failed['id']}: {failed['name']} - {failed['error']}")
                if len(failed_indices) > 10:
                    print(f"  ... 还有 {len(failed_indices) - 10} 个失败的景点")
        else:
            print("✅ 索引数量匹配！")
            
    except Exception as e:
        print(f"验证索引结果时出错: {e}")

    # 7. 总结
    print("=== 索引完成 ===")
    print(f"索引名称：{INDEX_NAME}")
    print(f"目的地ID：{DESTINATION_ID}")
    print(f"原始景点数量：{len(rows)}")
    print(f"有效景点数量：{len(valid_rows)}")
    print(f"无效景点数量：{len(invalid_rows)}")
    print(f"成功索引数量：{total_indexed}")
    print(f"失败索引数量：{len(failed_indices)}")
    print(f"向量维度：{dim}")
    
    if invalid_rows or failed_indices:
        print("\n💡 建议:")
        print("1. 运行诊断脚本查看详细信息: python diagnose_attractions.py")
        print("2. 清理或修复无效的景点数据")
        print("3. 重新运行索引脚本")
    
    conn.close()

if __name__ == "__main__":
    main() 
