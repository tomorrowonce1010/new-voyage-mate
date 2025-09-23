#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""delete_single_community_entry.py
从 Elasticsearch 中删除单个社区条目的索引。
用于当行程权限状态变化时清理索引。

使用方法:
    python delete_single_community_entry.py <community_entry_id>
"""

import os
import sys
from elasticsearch import Elasticsearch

# 设置UTF-8编码
if sys.platform.startswith('win'):
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.detach())

# -------------------- 配置区域 --------------------
ES_CONFIG = {
    "hosts": [os.getenv("ES_HOST", "http://localhost:9200")],
    "retry_on_timeout": True,
    "verify_certs": False,  # 禁用SSL证书验证
    "ssl_show_warn": False,  # 禁用SSL警告
}

INDEX_NAME = os.getenv("ES_INDEX", "community_entries")
# -------------------------------------------------

def delete_community_entry(entry_id: int) -> bool:
    """从Elasticsearch删除单个社区条目"""
    try:
        # 1. 初始化Elasticsearch连接
        print(f"开始删除社区条目 ID: {entry_id}")
        
        es = Elasticsearch(**ES_CONFIG)
        
        if not es.ping():
            raise RuntimeError("Elasticsearch cluster is not reachable")
        
        # 2. 检查索引是否存在
        if not es.indices.exists(index=INDEX_NAME):
            print(f"[ERROR] 索引 '{INDEX_NAME}' 不存在")
            return False
        
        # 3. 检查文档是否存在
        doc_id = str(entry_id)
        if not es.exists(index=INDEX_NAME, id=doc_id):
            print(f"[ERROR] 社区条目 ID: {entry_id} 在索引中不存在")
            return False
        
        # 4. 删除文档
        response = es.delete(index=INDEX_NAME, id=doc_id)
        
        if response["result"] == "deleted":
            print(f"[SUCCESS] 成功删除社区条目 ID: {entry_id}")
            return True
        else:
            print(f"[ERROR] 删除失败: {response}")
            return False
            
    except Exception as e:
        print(f"[ERROR] 删除过程中发生错误: {e}")
        return False

def main():
    if len(sys.argv) != 2:
        print("使用方法: python delete_single_community_entry.py <community_entry_id>")
        sys.exit(1)
    
    try:
        entry_id = int(sys.argv[1])
    except ValueError:
        print("[ERROR] 社区条目ID必须是整数")
        sys.exit(1)
    
    success = delete_community_entry(entry_id)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 