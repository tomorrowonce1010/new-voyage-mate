# 🚀 快速启动和健康检查指南

本指南帮助您快速启动所有服务并验证健康检查端点。

---

## 📋 启动前准备

### 1. 确保依赖服务已安装
```bash
# 检查 MySQL
sudo systemctl status mysql

# 检查 Elasticsearch
sudo systemctl status elasticsearch

# 检查 Java (需要 JDK 17)
java -version

# 检查 Node.js
node -v
npm -v

# 检查 Python 虚拟环境
ls -la /root/voyagemate/new-voyage-mate/.venv/
```

### 2. 确保后端已编译
```bash
cd /root/voyagemate/new-voyage-mate/backend

# 编译 Spring Boot 项目
mvn clean package -DskipTests
```

---

## 🎯 方法 1: 使用一键启动脚本（推荐）

### 启动所有服务
```bash
cd /root/voyagemate/new-voyage-mate

# 运行启动脚本
./start_all_services.sh
```

这个脚本会按顺序启动：
1. MySQL
2. Elasticsearch  
3. Backend (Spring Boot)
4. Embedding Service (FastAPI)
5. RAG Service (FastAPI)
6. Frontend (React)

**预计启动时间:** 约 2-3 分钟

---

## 🎯 方法 2: 手动分步启动

如果一键启动脚本有问题，可以手动启动各个服务：

### 步骤 1: 启动基础服务
```bash
# 启动 MySQL
sudo systemctl start mysql

# 启动 Elasticsearch
sudo systemctl start elasticsearch

# 等待 Elasticsearch 完全启动
sleep 15
```

### 步骤 2: 启动 Backend
```bash
cd /root/voyagemate/new-voyage-mate/backend

# 后台启动 Spring Boot
nohup java -Xms256m -Xmx512m -jar target/backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
echo $! > backend.pid

# 查看日志
tail -f backend.log

# 等待启动完成（Ctrl+C 退出日志查看）
```

### 步骤 3: 启动 Embedding Service
```bash
cd /root/voyagemate/new-voyage-mate/embedding-service

# 使用虚拟环境启动
nohup /root/voyagemate/new-voyage-mate/.venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 > embedding.log 2>&1 &
echo $! > embedding.pid

# 查看日志
tail -f embedding.log

# 等待模型加载完成（约 30 秒）
```

### 步骤 4: 启动 RAG Service
```bash
cd /root/voyagemate/new-voyage-mate/rag-service

# 使用虚拟环境启动
nohup /root/voyagemate/new-voyage-mate/.venv/bin/python simple_rag_service.py > rag.log 2>&1 &
echo $! > rag.pid

# 查看日志
tail -f rag.log

# 等待服务初始化（约 10 秒）
```

### 步骤 5: 启动 Frontend
```bash
cd /root/voyagemate/new-voyage-mate/frontend

# 启动 React 开发服务器
nohup npm start > frontend.log 2>&1 &
echo $! > frontend.pid

# 查看日志
tail -f frontend.log

# 等待编译完成
```

---

## ✅ 验证服务是否启动

### 检查端口监听
```bash
# 一次性检查所有端口
netstat -tuln | grep -E '3000|8000|8001|8080|9200|3306'
```

**预期输出:**
```
tcp    0.0.0.0:3000    # Frontend
tcp    0.0.0.0:8000    # Embedding
tcp    0.0.0.0:8001    # RAG
tcp    0.0.0.0:8080    # Backend
tcp    0.0.0.0:9200    # Elasticsearch
tcp    0.0.0.0:3306    # MySQL
```

### 检查进程
```bash
# 检查 Java 进程 (Backend)
ps aux | grep java

# 检查 Python 进程 (Embedding + RAG)
ps aux | grep python

# 检查 Node 进程 (Frontend)
ps aux | grep node
```

---

## 🏥 运行健康检查

### 使用自动化脚本（推荐）
```bash
cd /root/voyagemate/new-voyage-mate

# 运行健康检查脚本
./check-health.sh
```

**预期输出:**
```
=========================================
   VoyageMate 服务健康检查
=========================================

1️⃣  Backend Service (Spring Boot)
检查 Backend ... ✅ 健康

2️⃣  Frontend Service (React)
检查 Frontend ... ✅ 健康

3️⃣  Embedding Service (FastAPI)
检查 Embedding ... ✅ 健康

4️⃣  RAG Service (FastAPI)
检查 RAG ... ✅ 健康

=========================================
总结: 4/4 服务健康
=========================================
✅ 所有服务运行正常！
```

### 手动测试各个端点
```bash
# Backend (注意 /api 前缀)
echo "=== Backend ==="
curl -s http://localhost:8080/api/actuator/health | jq '.'

# Frontend
echo "=== Frontend ==="
curl -s http://localhost:3000/health

# Embedding
echo "=== Embedding ==="
curl -s http://localhost:8000/health | jq '.'

# RAG
echo "=== RAG ==="
curl -s http://localhost:8001/health | jq '.'
```

---

## 🔧 故障排查

### Backend 无法访问
```bash
# 检查日志
tail -50 /root/voyagemate/new-voyage-mate/backend/backend.log

# 检查常见问题
# 1. MySQL 是否运行？
sudo systemctl status mysql

# 2. Elasticsearch 是否运行？
curl http://localhost:9200

# 3. 端口是否被占用？
netstat -tuln | grep 8080

# 4. JAR 文件是否存在？
ls -lh /root/voyagemate/new-voyage-mate/backend/target/*.jar
```

### Embedding Service 无法启动
```bash
# 检查日志
tail -50 /root/voyagemate/new-voyage-mate/embedding-service/embedding.log

# 检查常见问题
# 1. 模型文件是否存在？
ls -la /root/voyagemate/new-voyage-mate/embedding-service/models-chinese/

# 2. 虚拟环境是否激活？
/root/voyagemate/new-voyage-mate/.venv/bin/python --version

# 3. 依赖是否安装？
/root/voyagemate/new-voyage-mate/.venv/bin/pip list | grep -E 'fastapi|sentence-transformers|uvicorn'

# 4. 端口是否被占用？
netstat -tuln | grep 8000
```

### RAG Service 返回 degraded
```bash
# 这是正常的，表示知识库未加载
# 检查索引文件是否存在
ls -la /root/voyagemate/new-voyage-mate/rag-service/index/

# 检查数据文件
ls -la /root/voyagemate/new-voyage-mate/rag-service/data/processed/

# 如果文件不存在，需要先构建知识库
cd /root/voyagemate/new-voyage-mate/rag-service
/root/voyagemate/new-voyage-mate/.venv/bin/python src/ingest_faiss.py
```

### Frontend 404 错误
```bash
# 检查健康检查文件是否存在
ls -la /root/voyagemate/new-voyage-mate/frontend/public/health*

# 检查 React 开发服务器是否运行
netstat -tuln | grep 3000

# 检查前端日志
tail -50 /root/voyagemate/new-voyage-mate/frontend/frontend.log
```

---

## 🛑 停止所有服务

### 使用停止脚本
```bash
cd /root/voyagemate/new-voyage-mate
./stop_all_services.sh
```

### 手动停止
```bash
# 停止基于 PID 的服务
for pid_file in backend/backend.pid embedding-service/embedding.pid rag-service/rag.pid frontend/frontend.pid; do
    if [ -f "$pid_file" ]; then
        kill $(cat "$pid_file") 2>/dev/null
        rm "$pid_file"
    fi
done

# 停止系统服务
sudo systemctl stop elasticsearch
sudo systemctl stop mysql
```

---

## 📊 完整的启动和检查流程

```bash
# 1. 进入项目目录
cd /root/voyagemate/new-voyage-mate

# 2. 确保后端已编译
cd backend && mvn clean package -DskipTests && cd ..

# 3. 启动所有服务
./start_all_services.sh

# 4. 等待所有服务启动（约 2-3 分钟）
sleep 180

# 5. 运行健康检查
./check-health.sh

# 6. 如果所有服务都健康，可以访问应用
echo "前端地址: http://localhost:3000"
```

---

## 📝 快速参考

| 服务 | 端口 | 健康检查 URL | 日志文件 |
|------|------|-------------|----------|
| Frontend | 3000 | http://localhost:3000/health | frontend/frontend.log |
| Backend | 8080 | http://localhost:8080/api/actuator/health | backend/backend.log |
| Embedding | 8000 | http://localhost:8000/health | embedding-service/embedding.log |
| RAG | 8001 | http://localhost:8001/health | rag-service/rag.log |
| Elasticsearch | 9200 | http://localhost:9200 | /var/log/elasticsearch/ |
| MySQL | 3306 | - | /var/log/mysql/ |

---

## 💡 提示

1. **首次启动**: 第一次启动可能需要更长时间，因为需要加载模型和初始化数据库
2. **内存不足**: 如果内存不足，可以先启动必要的服务（Backend + Frontend）
3. **日志查看**: 使用 `tail -f` 可以实时查看服务启动日志
4. **端口冲突**: 如果端口被占用，检查是否有其他服务在运行
5. **虚拟环境**: 所有 Python 服务都应该使用项目的虚拟环境

---

## 📚 相关文档

- 详细健康检查说明: `HEALTH_CHECK_STATUS.md`
- 部署测试指南: `TEST_DEPLOYMENT.md`
- 本地开发指南: `LOCAL_DEVELOPMENT.md`
- 启动脚本: `start_all_services.sh`
- 停止脚本: `stop_all_services.sh`

---

**最后更新:** 2025-10-15

