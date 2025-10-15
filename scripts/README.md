# 🚀 部署脚本说明

本目录包含两个部署脚本，请根据你的需求选择：

## 📝 脚本对比

| 特性 | deploy.sh | deploy-full.sh |
|------|-----------|----------------|
| Backend (Spring Boot) | ✅ | ✅ |
| Frontend (React) | ✅ | ✅ |
| MySQL | ❌ | ✅ |
| Elasticsearch | ❌ | ✅ |
| Embedding Service | ❌ | ✅ |
| RAG Service | ❌ | ✅ |
| 适用场景 | 简单部署 | 完整微服务部署 |

---

## 📜 deploy.sh - 基础部署脚本

### 用途
仅部署核心应用（Backend + Frontend）

### 部署内容
- ✅ Backend (Spring Boot) - 端口 8080
- ✅ Frontend (React) - 端口 3000

### 使用场景
- 快速测试 Backend 和 Frontend 更新
- 不需要 AI 功能的简单部署
- 开发环境快速迭代

### 运行方式
```bash
bash /root/voyagemate/new-voyage-mate/scripts/deploy.sh
```

### 执行步骤
1. 拉取最新代码
2. 停止 Backend 和 Frontend
3. Maven 构建 Backend
4. npm 构建 Frontend
5. 启动服务
6. 健康检查

---

## 📜 deploy-full.sh - 完整微服务部署脚本 ⭐

### 用途
部署完整的 VoyageMate 系统（所有微服务）

### 部署内容
- ✅ MySQL 数据库
- ✅ Elasticsearch (端口 9200)
- ✅ Backend (Spring Boot) - 端口 8080
- ✅ Embedding Service (Python/uvicorn) - 端口 8000
- ✅ RAG Service (Python) - 端口 8001
- ✅ Frontend (React) - 端口 3000

### 使用场景
- 生产环境部署
- 完整功能测试（包括 AI 功能）
- CI/CD 自动部署

### 运行方式
```bash
bash /root/voyagemate/new-voyage-mate/scripts/deploy-full.sh
```

### 执行步骤
1. 拉取最新代码
2. 停止所有服务
3. 启动 MySQL
4. 启动 Elasticsearch
5. 构建并启动 Backend
6. 启动 Embedding Service
7. 启动 RAG Service
8. 构建并启动 Frontend
9. 全面健康检查

---

## 🔧 前置要求

### deploy.sh 需要
- ✅ Java 17
- ✅ Maven
- ✅ Node.js & npm
- ✅ serve (自动安装)

### deploy-full.sh 额外需要
- ✅ MySQL (systemctl)
- ✅ Elasticsearch (systemctl)
- ✅ Python 虚拟环境 (`$PROJECT_DIR/.venv`)
- ✅ uvicorn (在虚拟环境中)

---

## 📊 端口使用

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend | 3000 | React 应用 |
| Backend | 8080 | Spring Boot API |
| Embedding | 8000 | Python 向量化服务 |
| RAG | 8001 | Python RAG 服务 |
| Elasticsearch | 9200 | 搜索引擎 |
| MySQL | 3306 | 数据库 |

---

## 🚨 常见问题

### Q1: 哪个脚本会被 CI/CD 使用？

**A**: GitHub Actions 使用 `deploy-full.sh`，确保完整功能部署。

### Q2: Python 服务启动失败怎么办？

**A**: 检查虚拟环境：
```bash
ls -la /root/voyagemate/new-voyage-mate/.venv/
source /root/voyagemate/new-voyage-mate/.venv/bin/activate
pip list  # 查看已安装的包
```

### Q3: Elasticsearch 内存不足？

**A**: 脚本已优化为 512MB，如果还不够：
```bash
sudo nano /etc/elasticsearch/jvm.options.d/low-memory.conf
# 调整 -Xms256m -Xmx256m
```

### Q4: 如何只重启某个服务？

**A**: 
```bash
# 只重启 Backend
cd /root/voyagemate/new-voyage-mate/backend
kill $(cat backend.pid)
nohup java -jar target/*.jar > backend.log 2>&1 &
echo $! > backend.pid

# 只重启 Frontend
cd /root/voyagemate/new-voyage-mate/frontend
kill $(cat frontend.pid)
nohup serve -s build -l 3000 > frontend.log 2>&1 &
echo $! > frontend.pid
```

---

## 📝 日志位置

```bash
# Backend
tail -f /root/voyagemate/new-voyage-mate/backend.log

# Frontend
tail -f /root/voyagemate/new-voyage-mate/frontend.log

# Embedding Service
tail -f /root/voyagemate/new-voyage-mate/embedding-service/embedding.log

# RAG Service
tail -f /root/voyagemate/new-voyage-mate/rag-service/rag.log

# Elasticsearch
sudo journalctl -u elasticsearch -f
```

---

## ✅ 推荐配置

### 开发环境
使用 `deploy.sh` - 快速迭代

### 测试/生产环境
使用 `deploy-full.sh` - 完整功能

### CI/CD
已配置使用 `deploy-full.sh`

---

## 🎯 下一步

1. **测试基础部署**：先手动运行 `deploy.sh` 确认基础功能
2. **测试完整部署**：手动运行 `deploy-full.sh` 确认所有服务
3. **配置 CI/CD**：将 SSH 密钥添加到 GitHub Secrets
4. **触发自动部署**：推送代码到 main 分支

🚀 **祝部署顺利！**

