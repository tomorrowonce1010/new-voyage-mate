# 🚀 自动部署配置指南

## 第一步：添加私钥到 GitHub Secrets

### 1. 复制上面显示的私钥

从 `-----BEGIN OPENSSH PRIVATE KEY-----` 到 `-----END OPENSSH PRIVATE KEY-----`（包含这两行）

### 2. 在 GitHub 上配置 Secrets

1. 打开你的 GitHub 仓库页面
2. 点击 **Settings**（设置）
3. 在左侧菜单找到 **Secrets and variables** → **Actions**
4. 点击 **New repository secret**
5. 添加以下 Secret：

**Name**: `SSH_PRIVATE_KEY`  
**Value**: 粘贴刚才复制的完整私钥内容

6. 点击 **Add secret**

### 3. 添加其他必需的 Secrets

再添加两个 Secrets：

**Name**: `SERVER_HOST`  
**Value**: `1.94.200.25`

**Name**: `SERVER_USER`  
**Value**: `root`

---

## 第二步：检查服务器端口

确保服务器防火墙开放了以下端口：

- **8080**: Backend (Spring Boot)
- **3000**: Frontend (React)
- **22**: SSH (用于部署)

### 检查命令：

```bash
# 检查端口是否开放
sudo netstat -tlnp | grep -E '8080|3000|22'

# 如果使用 firewalld
sudo firewall-cmd --list-ports

# 如果使用 ufw
sudo ufw status
```

### 开放端口（如果需要）：

```bash
# 使用 firewalld
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload

# 使用 ufw
sudo ufw allow 8080/tcp
sudo ufw allow 3000/tcp
sudo ufw reload
```

---

## 第三步：准备部署脚本

部署脚本已创建在：`scripts/deploy.sh`

脚本会执行以下操作：
1. 拉取最新代码
2. 重启 Backend 服务
3. 重启 Frontend 服务
4. 验证服务是否运行

---

## 第四步：测试部署

### 本地测试 SSH 连接：

```bash
ssh -i ~/.ssh/github_deploy_key root@1.94.200.25 "echo '✅ SSH 连接成功'"
```

### 手动触发 GitHub Actions 部署：

1. 提交代码到 `main` 分支
2. 访问 GitHub Actions 页面
3. 观察 "完整 CI/CD 流程" 运行
4. 在 "等待人工审批" 步骤批准部署

---

## 第五步：验证部署

部署完成后，访问以下地址：

- **Backend API**: http://1.94.200.25:8080
- **Frontend**: http://1.94.200.25:3000

### 健康检查：

```bash
# 检查 Backend
curl http://1.94.200.25:8080/api/actuator/health

# 检查 Frontend
curl http://1.94.200.25:3000
```

---

## 🔧 故障排查

### SSH 连接失败

```bash
# 检查 SSH 服务
sudo systemctl status sshd

# 查看 SSH 日志
sudo tail -f /var/log/auth.log
```

### 服务未启动

```bash
# 检查服务状态
ps aux | grep java  # Backend
ps aux | grep node  # Frontend

# 查看日志
tail -f backend.log
tail -f frontend.log
```

### 端口被占用

```bash
# 查找占用端口的进程
lsof -i :8080
lsof -i :3000

# 杀死进程
kill -9 <PID>
```

---

## 📝 后续维护

### 更新部署脚本

编辑 `scripts/deploy.sh` 文件以自定义部署流程

### 回滚到上一版本

```bash
cd /root/voyagemate/new-voyage-mate
git reset --hard HEAD~1
bash scripts/deploy.sh
```

### 查看部署历史

```bash
git log --oneline --graph
```

---

## ✅ 完成检查清单

- [ ] 私钥已添加到 GitHub Secrets (`SSH_PRIVATE_KEY`)
- [ ] 服务器信息已添加到 Secrets (`SERVER_HOST`, `SERVER_USER`)
- [ ] 服务器端口已开放 (8080, 3000, 22)
- [ ] SSH 连接测试成功
- [ ] 部署脚本可执行 (`chmod +x scripts/deploy.sh`)
- [ ] 首次手动部署成功
- [ ] GitHub Actions 自动部署成功

---

## 🎉 完成！

现在每次推送到 `main` 分支，GitHub Actions 会自动：

1. ✅ 运行 CI 检查
2. ✅ 构建项目
3. ✅ SSH 连接到服务器
4. ✅ 执行部署脚本
5. ✅ 验证部署结果

**享受你的自动化 CI/CD 流程！** 🚀

