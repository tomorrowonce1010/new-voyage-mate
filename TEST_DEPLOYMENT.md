# 🧪 部署流程测试指南

按照这个顺序测试，确保每个阶段都成功后再进行下一阶段。

---

## 阶段 1️⃣：本地测试部署脚本（不涉及 GitHub）

### 目的
验证部署脚本在服务器上能正常运行

### 步骤 1.1：测试部署脚本语法

```bash
cd /root/voyagemate/new-voyage-mate
bash -n scripts/deploy-full.sh
```

**预期结果**：无输出 = 语法正确 ✅

---

### 步骤 1.2：dry-run 模拟运行（推荐）

先看看脚本会执行什么操作，不实际运行：

```bash
# 查看脚本的主要步骤
grep -E "^echo.*\[" scripts/deploy-full.sh
```

**预期结果**：显示 8 个主要步骤 ✅

---

### 步骤 1.3：实际运行部署脚本

⚠️ **注意**：这会重启所有服务！

```bash
cd /root/voyagemate/new-voyage-mate
bash scripts/deploy-full.sh
```

**观察要点**：
- ✅ 每个步骤都有 [1/8] 到 [8/8] 的进度提示
- ✅ Git 拉取成功
- ✅ 服务停止成功
- ✅ MySQL 和 Elasticsearch 启动
- ✅ Backend 构建和启动
- ✅ Embedding 和 RAG 服务启动
- ✅ Frontend 构建和启动
- ✅ 健康检查通过

**如果失败**：查看错误信息，常见问题：
```bash
# 问题1: Maven 构建失败
cd backend && mvn clean compile

# 问题2: npm 构建失败
cd frontend && npm ci

# 问题3: Python 服务失败
source .venv/bin/activate
pip list

# 问题4: MySQL 未启动
sudo systemctl status mysql
sudo systemctl start mysql
```

---

### 步骤 1.4：验证所有服务运行

```bash
# 检查所有服务进程
ps aux | grep -E 'java|uvicorn|serve' | grep -v grep

# 检查所有端口
sudo netstat -tlnp | grep -E '3000|8080|8000|8001|9200|3306'
```

**预期结果**：
```
✅ 3000  - Frontend (serve)
✅ 8080  - Backend (java)
✅ 8000  - Embedding (uvicorn)
✅ 8001  - RAG (python)
✅ 9200  - Elasticsearch
✅ 3306  - MySQL
```

---

### 步骤 1.5：手动健康检查

```bash
# Backend (注意：context-path 是 /api)
curl http://localhost:8080/api/actuator/health

# Frontend (静态健康检查文件)
curl http://localhost:3000/health
curl http://localhost:3000/health.json

# Embedding Service
curl http://localhost:8000/health

# RAG Service
curl http://localhost:8001/health
```

**预期结果**：
- Backend: 返回 JSON，包含 `{"status":"UP"}` ✅
- Frontend: 返回 `OK` 或 JSON 格式的健康信息 ✅
- Embedding: 返回 JSON，包含 `{"status":"healthy"}` ✅
- RAG: 返回 JSON，包含 `{"status":"healthy"}` 或 `{"status":"degraded"}` ✅

---

## ✅ 阶段 1 完成标志

如果以上步骤全部成功，说明：
- ✅ 部署脚本可以正常运行
- ✅ 所有服务可以正常启动
- ✅ 健康检查通过

**可以进入阶段 2！**

---

## 阶段 2️⃣：测试 SSH 自动部署（模拟 GitHub Actions）

### 目的
验证 SSH 连接和远程执行部署脚本

### 步骤 2.1：测试 SSH 密钥连接

```bash
# 测试 SSH 连接
ssh -i ~/.ssh/github_deploy_key root@1.94.200.25 "echo '✅ SSH 连接成功'"
```

**预期结果**：输出 "✅ SSH 连接成功"

**如果失败**：
```bash
# 检查密钥权限
ls -l ~/.ssh/github_deploy_key
chmod 600 ~/.ssh/github_deploy_key

# 检查公钥是否在 authorized_keys
grep -f ~/.ssh/github_deploy_key.pub ~/.ssh/authorized_keys
```

---

### 步骤 2.2：测试远程执行简单命令

```bash
# 测试远程执行
ssh -i ~/.ssh/github_deploy_key root@1.94.200.25 "
  echo '📋 服务器信息：'
  echo 'Hostname:' \$(hostname)
  echo 'IP:' \$(hostname -I)
  echo 'Uptime:' \$(uptime -p)
  echo '✅ 远程命令执行成功'
"
```

**预期结果**：显示服务器信息 ✅

---

### 步骤 2.3：测试远程执行部署脚本

```bash
# 模拟 GitHub Actions 的部署命令
ssh -i ~/.ssh/github_deploy_key -o StrictHostKeyChecking=no \
  root@1.94.200.25 \
  'bash /root/voyagemate/new-voyage-mate/scripts/deploy-full.sh'
```

**这个命令和 GitHub Actions 执行的完全一样！**

**预期结果**：
- 看到完整的部署过程
- 所有 8 个步骤都成功
- 最后显示 "🎉 部署完成！"

---

### 步骤 2.4：验证远程部署结果

从外网访问：

```bash
# 在另一台电脑或手机浏览器访问：
http://1.94.200.25:3000   # Frontend
http://1.94.200.25:8080   # Backend

# 或在服务器上验证：
curl http://1.94.200.25:8080/api/actuator/health
curl -I http://1.94.200.25:3000
```

**预期结果**：可以从外网访问 ✅

---

## ✅ 阶段 2 完成标志

如果以上步骤全部成功，说明：
- ✅ SSH 密钥配置正确
- ✅ 可以远程执行部署脚本
- ✅ 部署后服务可从外网访问

**可以进入阶段 3！**

---

## 阶段 3️⃣：完整 CI/CD 测试（真实 GitHub Actions）

### 目的
测试完整的自动化流程

### 步骤 3.1：配置 GitHub Secrets

1. 打开浏览器，访问：
   ```
   https://github.com/tomorrowonce1010/new-voyage-mate/settings/secrets/actions
   ```

2. 点击 **"New repository secret"**，添加 3 个 Secrets：

   **Secret 1:**
   - Name: `SSH_PRIVATE_KEY`
   - Value: 复制 `cat ~/.ssh/github_deploy_key` 的完整内容
   
   **Secret 2:**
   - Name: `SERVER_HOST`
   - Value: `1.94.200.25`
   
   **Secret 3:**
   - Name: `SERVER_USER`
   - Value: `root`

3. 确认看到 3 个 Secrets：
   ```
   ✅ SSH_PRIVATE_KEY      ••••••••
   ✅ SERVER_HOST          ••••••••
   ✅ SERVER_USER          ••••••••
   ```

---

### 步骤 3.2：提交并推送代码

```bash
cd /root/voyagemate/new-voyage-mate

# 查看要提交的文件
git status

# 添加所有更改
git add .github/workflows/ scripts/ DEPLOY_SETUP.md TEST_DEPLOYMENT.md

# 提交
git commit -m "feat: complete auto-deployment system with full microservices

✨ Features:
- Full deployment script (deploy-full.sh)
- All 6 services: MySQL, ES, Backend, Embedding, RAG, Frontend
- Real SSH-based deployment in CI/CD
- Comprehensive health checks
- Complete documentation

🚀 Ready for production auto-deployment!"

# 推送到 main 分支（触发 CI/CD）
git push origin main
```

---

### 步骤 3.3：观察 GitHub Actions 运行

1. 推送后，立即访问：
   ```
   https://github.com/tomorrowonce1010/new-voyage-mate/actions
   ```

2. 你会看到一个新的 workflow run 开始：
   ```
   🟡 完整 CI/CD 流程（实际可用版）
      Running...
   ```

3. 点击进入，观察各个阶段：

   **CI 阶段（约 5-8 分钟）：**
   ```
   ✅ 🔨 Backend CI
   ✅ 🎨 Frontend CI
   ✅ 🐍 Python Services CI
   ✅ ✅ CI 阶段总结
   ```

   **CD 阶段（约 5-8 分钟，仅在 main 分支）：**
   ```
   ✅ 🐳 构建 Docker 镜像
   ✅ 🚀 部署到测试环境  ← 这里会执行 SSH 部署
   ✅ 💨 冒烟测试
   ⏸️  👤 等待人工审批  ← 需要你手动批准
   ```

---

### 步骤 3.4：批准生产部署（可选）

如果要测试完整流程到生产：

1. 在 "等待人工审批" 步骤
2. 点击 **"Review deployments"**
3. 勾选 **"production-approval"**
4. 点击 **"Approve and deploy"**

然后继续观察：
```
✅ 🎯 部署到生产环境
✅ ✅ 部署后验证
✅ 🎉 流程完成
```

---

### 步骤 3.5：查看部署日志

点击每个步骤可以看到详细日志，特别关注：

**"部署到测试环境"日志**：
```
🚀 部署到测试环境
📍 目标服务器: 1.94.200.25
🔄 执行部署脚本...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 VoyageMate 完整服务部署
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📥 [1/8] 拉取最新代码...
✅ 代码更新完成

⏹️  [2/8] 停止所有服务...
✅ 所有服务已停止

🗄️  [3/8] 启动基础服务...
✅ MySQL 运行中
✅ Elasticsearch 运行中

... (更多日志)

🎉 部署完成！
```

---

### 步骤 3.6：验证自动部署结果

访问这些地址验证：

```
http://1.94.200.25:3000   # Frontend
http://1.94.200.25:8080   # Backend API
http://1.94.200.25:8000   # Embedding Service
http://1.94.200.25:8001   # RAG Service
```

---

## ✅ 阶段 3 完成标志

如果以上步骤全部成功，恭喜！你已经实现：

- ✅ 完整的 CI/CD 自动化流程
- ✅ 代码推送自动触发部署
- ✅ CI 失败自动阻止部署
- ✅ SSH 自动连接服务器部署
- ✅ 所有微服务自动启动
- ✅ 健康检查自动验证
- ✅ 人工审批门控

**🎉 你的 CI/CD 系统已经完全运行！**

---

## 🚨 故障排查

### 问题 1: SSH 连接失败

```bash
# 检查 GitHub Secrets 中的私钥
# 在 workflow 日志中看到: "Permission denied"

# 解决：重新复制私钥到 GitHub Secrets
cat ~/.ssh/github_deploy_key
# 确保包含 -----BEGIN 和 -----END 行
```

### 问题 2: 部署脚本执行失败

```bash
# 在服务器上手动运行查看详细错误
ssh root@1.94.200.25
cd /root/voyagemate/new-voyage-mate
bash -x scripts/deploy-full.sh  # -x 显示详细执行过程
```

### 问题 3: 健康检查失败

```bash
# 检查服务日志
ssh root@1.94.200.25 "
  tail -50 /root/voyagemate/new-voyage-mate/backend.log
  tail -50 /root/voyagemate/new-voyage-mate/frontend.log
"
```

### 问题 4: 端口无法访问

```bash
# 检查防火墙
ssh root@1.94.200.25 "
  sudo ufw status
  sudo ufw allow 3000
  sudo ufw allow 8080
  sudo ufw allow 8000
  sudo ufw allow 8001
"
```

---

## 📝 测试检查清单

打印这个清单，逐项测试：

### 阶段 1：本地测试
- [ ] 脚本语法检查通过
- [ ] 本地运行部署脚本成功
- [ ] 所有 6 个服务都启动
- [ ] 健康检查全部通过

### 阶段 2：SSH 测试
- [ ] SSH 连接成功
- [ ] 远程执行命令成功
- [ ] 远程执行部署脚本成功
- [ ] 外网可以访问服务

### 阶段 3：CI/CD 测试
- [ ] GitHub Secrets 已配置
- [ ] 代码成功推送
- [ ] CI 阶段全部通过
- [ ] CD 部署成功
- [ ] 冒烟测试通过
- [ ] 从外网验证服务正常

---

## 🎯 快速开始

想马上开始测试？运行：

```bash
# 一键本地测试
cd /root/voyagemate/new-voyage-mate
bash scripts/deploy-full.sh

# 一键 SSH 测试
ssh -i ~/.ssh/github_deploy_key root@1.94.200.25 \
  'bash /root/voyagemate/new-voyage-mate/scripts/deploy-full.sh'
```

**祝测试顺利！🚀**

