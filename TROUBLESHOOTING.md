# 🔧 故障排查指南

本文档记录常见问题和解决方案。

---

## ✅ 已解决的问题

### 问题 1: Backend 返回 503 状态 (2025-10-15)

**症状:**
```bash
$ curl http://localhost:8080/api/actuator/health
{"status":"DOWN"}
```

**原因:**
- Elasticsearch 服务未运行
- Backend 依赖 Elasticsearch 进行健康检查

**诊断过程:**
```bash
# 1. 检查端口
$ netstat -tuln | grep 8080
tcp6  0  0  :::8080  :::*  LISTEN  ✅ 端口在监听

# 2. 检查进程
$ ps aux | grep java
root  425191  java -jar backend-0.0.1-SNAPSHOT.jar  ✅ 进程运行中

# 3. 测试健康检查
$ curl http://localhost:8080/api/actuator/health
HTTP/1.1 503
{"status":"DOWN"}  ❌ 返回 DOWN

# 4. 检查依赖服务
$ systemctl status elasticsearch
Active: inactive (dead)  ❌ Elasticsearch 未运行

$ systemctl status mysql
Active: active (running)  ✅ MySQL 运行中
```

**解决方案:**
```bash
# 启动 Elasticsearch
sudo systemctl start elasticsearch

# 等待服务启动
sleep 15

# 验证修复
curl http://localhost:8080/api/actuator/health
{"status":"UP"}  ✅ 问题解决
```

**预防措施:**
- 使用 `start_all_services.sh` 确保所有依赖服务都启动
- 设置 Elasticsearch 开机自启: `sudo systemctl enable elasticsearch`

---

## 📚 常见问题排查手册

### Backend 相关问题

#### Q1: Backend 无法启动

**检查清单:**
```bash
# 1. Java 版本
java -version  # 需要 JDK 17

# 2. JAR 文件是否存在
ls -lh /root/voyagemate/new-voyage-mate/backend/target/*.jar

# 3. MySQL 是否运行
systemctl status mysql

# 4. Elasticsearch 是否运行
systemctl status elasticsearch
curl http://localhost:9200

# 5. 端口是否被占用
netstat -tuln | grep 8080

# 6. 查看启动日志
tail -100 /root/voyagemate/new-voyage-mate/backend/backend.log
```

#### Q2: Backend 返回 DOWN

**可能原因和解决方案:**

1. **MySQL 未运行**
   ```bash
   sudo systemctl start mysql
   ```

2. **Elasticsearch 未运行**
   ```bash
   sudo systemctl start elasticsearch
   sleep 15  # 等待启动
   ```

3. **数据库连接失败**
   ```bash
   # 检查数据库配置
   mysql -u voyagemate -p'se_202507' voyagemate -e "SELECT 1;"
   ```

4. **Elasticsearch 连接失败**
   ```bash
   curl http://localhost:9200/_cluster/health
   ```

---

### Embedding Service 相关问题

#### Q1: Embedding 无法启动

**检查清单:**
```bash
# 1. 虚拟环境
ls -la /root/voyagemate/new-voyage-mate/.venv/

# 2. 模型文件
ls -la /root/voyagemate/new-voyage-mate/embedding-service/models-chinese/

# 3. 依赖包
/root/voyagemate/new-voyage-mate/.venv/bin/pip list | grep -E 'fastapi|sentence-transformers|uvicorn'

# 4. 端口占用
netstat -tuln | grep 8000

# 5. 查看日志
tail -100 /root/voyagemate/new-voyage-mate/embedding-service/embedding.log
```

**常见错误:**

1. **模型文件缺失**
   ```
   RuntimeError: Failed to load model from ./models-chinese
   ```
   
   解决: 下载模型文件到 `embedding-service/models-chinese/`

2. **内存不足**
   ```
   RuntimeError: [enforce fail at alloc_cpu.cpp:114]
   ```
   
   解决: 增加系统内存或使用更小的模型

---

### RAG Service 相关问题

#### Q1: RAG 返回 degraded

**这是正常的！** RAG 服务即使知识库未加载也会返回 HTTP 200。

```json
{
  "status": "degraded",
  "knowledge_base_loaded": false,
  "chunks_count": 0,
  "model_loaded": true
}
```

**解决方案:**
```bash
cd /root/voyagemate/new-voyage-mate/rag-service

# 检查索引文件
ls -la index/

# 如果文件不存在，重新生成
/root/voyagemate/new-voyage-mate/.venv/bin/python src/ingest_faiss.py
```

#### Q2: RAG 完全无响应

**检查清单:**
```bash
# 1. 进程状态
ps aux | grep simple_rag_service

# 2. 端口状态
netstat -tuln | grep 8001

# 3. 查看日志
tail -100 /root/voyagemate/new-voyage-mate/rag-service/rag.log

# 4. 模型文件
ls -la /root/voyagemate/new-voyage-mate/rag-service/models/
```

---

### Frontend 相关问题

#### Q1: Frontend 健康检查返回 404

**可能原因:**

1. **文件缺失**
   ```bash
   ls -la /root/voyagemate/new-voyage-mate/frontend/public/health*
   ```
   
   解决: 创建健康检查文件
   ```bash
   echo "OK" > /root/voyagemate/new-voyage-mate/frontend/public/health
   ```

2. **React 服务未启动**
   ```bash
   netstat -tuln | grep 3000
   ps aux | grep react-scripts
   ```

3. **需要重启服务**
   ```bash
   cd /root/voyagemate/new-voyage-mate/frontend
   pkill -f "react-scripts"
   npm start > frontend.log 2>&1 &
   ```

---

## 🛠️ 诊断工具

### 快速健康检查
```bash
cd /root/voyagemate/new-voyage-mate
./check-health.sh
```

### 查看所有服务端口
```bash
netstat -tuln | grep -E '3000|8000|8001|8080|9200|3306'
```

### 查看所有服务进程
```bash
# Java 进程 (Backend)
ps aux | grep java | grep -v grep

# Python 进程 (Embedding + RAG)
ps aux | grep python | grep -v grep

# Node 进程 (Frontend)
ps aux | grep node | grep -v grep
```

### 查看服务日志
```bash
# Backend
tail -f /root/voyagemate/new-voyage-mate/backend/backend.log

# Embedding
tail -f /root/voyagemate/new-voyage-mate/embedding-service/embedding.log

# RAG
tail -f /root/voyagemate/new-voyage-mate/rag-service/rag.log

# Frontend
tail -f /root/voyagemate/new-voyage-mate/frontend/frontend.log
```

### 检查系统资源
```bash
# 内存使用
free -h

# CPU 使用
top -bn1 | head -20

# 磁盘空间
df -h
```

---

## 🔄 重启服务

### 重启单个服务

**Backend:**
```bash
cd /root/voyagemate/new-voyage-mate/backend
kill $(cat backend.pid) 2>/dev/null
nohup java -jar target/backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
echo $! > backend.pid
```

**Embedding:**
```bash
cd /root/voyagemate/new-voyage-mate/embedding-service
kill $(cat embedding.pid) 2>/dev/null
nohup ../.venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 > embedding.log 2>&1 &
echo $! > embedding.pid
```

**RAG:**
```bash
cd /root/voyagemate/new-voyage-mate/rag-service
kill $(cat rag.pid) 2>/dev/null
nohup ../.venv/bin/python simple_rag_service.py > rag.log 2>&1 &
echo $! > rag.pid
```

**Frontend:**
```bash
cd /root/voyagemate/new-voyage-mate/frontend
kill $(cat frontend.pid) 2>/dev/null
nohup npm start > frontend.log 2>&1 &
echo $! > frontend.pid
```

### 重启所有服务
```bash
cd /root/voyagemate/new-voyage-mate
./stop_all_services.sh
./start_all_services.sh
```

---

## 📊 健康检查端点详解

### Backend
```bash
curl http://localhost:8080/api/actuator/health
```

**正常响应:**
```json
{
  "status": "UP"
}
```

**详细信息 (如果配置了 show-details):**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "elasticsearch": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

**异常响应:**
```json
{
  "status": "DOWN"
}
```
- 检查 MySQL
- 检查 Elasticsearch
- 查看 backend.log

### Embedding
```bash
curl http://localhost:8000/health
```

**正常响应:**
```json
{
  "status": "healthy",
  "service": "Embedding Service",
  "model_loaded": true,
  "model_path": "./models-chinese"
}
```

### RAG
```bash
curl http://localhost:8001/health
```

**正常响应:**
```json
{
  "status": "healthy",
  "knowledge_base_loaded": true,
  "chunks_count": 306970,
  "model_loaded": true
}
```

**降级响应 (仍然可用):**
```json
{
  "status": "degraded",
  "knowledge_base_loaded": false,
  "chunks_count": 0,
  "model_loaded": true
}
```

### Frontend
```bash
curl http://localhost:3000/health
```

**正常响应:**
```
OK
```

---

## 📞 获取帮助

如果问题仍未解决:

1. 查看完整日志文件
2. 检查系统资源 (内存、磁盘)
3. 参考相关文档:
   - `QUICK_START_HEALTH_CHECK.md`
   - `HEALTH_CHECK_STATUS.md`
   - `LOCAL_DEVELOPMENT.md`

---

**最后更新:** 2025-10-15

