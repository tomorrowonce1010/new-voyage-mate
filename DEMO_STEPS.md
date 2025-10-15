# 🎬 CI/CD 流程实际演示步骤

## 📌 前提条件

- ✅ 已创建完整的 CI/CD workflow (`pr-cicd-demo.yml`)
- ✅ 已提交到 main 分支
- ✅ GitHub Actions 已启用

---

## 🎯 演示 1: CI 失败，自动退回

### 步骤 1: 推送演示分支

```bash
# 当前分支: demo/ci-failure
git push origin demo/ci-failure
```

### 步骤 2: 在 GitHub 创建 Pull Request

1. 访问你的 GitHub 仓库
2. 点击 "Pull requests" → "New pull request"
3. 选择 `demo/ci-failure` → `main`
4. 标题: "Demo: CI Failure Rollback"
5. 点击 "Create pull request"

### 步骤 3: 观察 CI 流程执行

在 PR 页面，你会看到：

```
✅ 阶段 1: 📝 代码质量检查 - 通过
✅ 阶段 2: 🏗️ 构建和测试 - 通过  
✅ 阶段 3: 🔗 集成测试 - 通过
✅ 阶段 4: ✅ CI 阶段总结 - 通过

⏹️ CD 阶段不会执行（因为这是 PR，不是 push to main）
```

**预期结果**：
- ✅ CI 所有阶段通过（因为我们只添加了文档）
- ℹ️ 显示消息："CI 全部通过"
- ⏹️ CD 阶段被跳过（显示 "Skipped"）

---

## 🎯 演示 2: 实际的 CI 失败场景

### 创建一个真实的失败

让我们在 Backend 中故意引入错误：

```bash
# 1. 创建新分支
git checkout -b demo/real-failure

# 2. 在 Backend 代码中引入编译错误
cat >> backend/src/main/java/com/se_07/backend/BrokenTest.java << 'EOF'
package com.se_07.backend;

public class BrokenTest {
    // 故意的语法错误：缺少分号
    public void test() {
        String x = "broken"
    }
}
EOF

# 3. 提交并推送
git add backend/src/main/java/com/se_07/backend/BrokenTest.java
git commit -m "test: intentional compilation error"
git push origin demo/real-failure

# 4. 创建 PR
```

### 观察失败流程

在 GitHub Actions 中你会看到：

```
✅ 阶段 1: 📝 代码质量检查 - 通过
❌ 阶段 2: 🏗️ 构建和测试 - 失败
   └─ Backend 编译错误：';' expected
⏹️ 阶段 3: 🔗 集成测试 - 跳过
❌ 阶段 4: ✅ CI 阶段总结 - 失败
   └─ ❌ CI 失败！流程终止，请修复问题后重新提交

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔙 CD 阶段完全不会执行！
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**关键点**：
- ❌ 构建失败后，后续阶段被跳过
- ❌ CI 总结检测到失败，返回 exit 1
- 🔙 **CD 阶段因为依赖 ci-summary 通过，所以完全不执行**
- 💡 PR 显示红色 ✗，无法合并

### 修复并重新提交

```bash
# 1. 删除错误的文件
rm backend/src/main/java/com/se_07/backend/BrokenTest.java

# 2. 提交修复
git add backend/src/main/java/com/se_07/backend/BrokenTest.java
git commit -m "fix: remove broken test file"
git push origin demo/real-failure

# 3. GitHub Actions 自动重新运行
# 4. 这次所有 CI 检查都会通过 ✅
```

---

## 🎯 演示 3: CI 成功，触发完整 CD

### 步骤 1: 合并到 main 分支

```bash
# 前提：PR 的所有 CI 检查已通过

# 方式 1: 在 GitHub 上点击 "Merge pull request"

# 方式 2: 命令行合并
git checkout main
git merge demo/ci-failure
git push origin main
```

### 步骤 2: 观察完整 CI/CD 流程

推送到 main 后，workflow 会执行完整流程：

```
┌─────────────── CI 阶段 ───────────────┐
│ ✅ 代码质量检查                         │
│ ✅ 构建和测试                          │
│ ✅ 集成测试                            │
│ ✅ CI 总结                             │
└───────────────┬───────────────────────┘
                ↓
          CI 成功！继续...
                ↓
┌─────────────── CD 阶段 ───────────────┐
│ 🐳 构建 Docker 镜像                    │
│    └─ ✅ Backend 镜像构建完成           │
│    └─ ✅ Frontend 镜像构建完成          │
│                                       │
│ 🚀 部署到测试环境                       │
│    └─ ✅ staging.voyagemate.com       │
│                                       │
│ 💨 冒烟测试                            │
│    └─ ✅ 健康检查通过                   │
│                                       │
│ 👤 等待人工审批                         │
│    └─ ⏸️  需要管理员批准               │
└───────────────────────────────────────┘
```

### 步骤 3: 批准生产部署

1. 进入 GitHub Actions 页面
2. 找到正在运行的 workflow
3. 在 "等待人工审批" 步骤
4. 点击 "Review deployments"
5. 选择 "production-approval"
6. 点击 "Approve and deploy"

### 步骤 4: 观察生产部署

批准后继续：

```
┌─────────────────────────────────────┐
│ 🎯 部署到生产环境                     │
│    └─ ✅ 滚动更新完成                 │
│                                     │
│ ✅ 部署后验证                         │
│    └─ ✅ 健康检查通过                 │
│    └─ ✅ 监控指标正常                 │
│                                     │
│ 🎉 流程完成                          │
│    └─ ✅ CI/CD 全部成功              │
└─────────────────────────────────────┘
```

---

## 📊 对比：PR vs Push to Main

### Pull Request (只执行 CI)

```
事件: 创建 PR
触发器: pull_request
分支: feature → main

执行阶段:
├─ ✅ CI 阶段（全部）
│  ├─ 代码检查
│  ├─ 构建测试
│  ├─ 集成测试
│  └─ CI 总结
│
└─ ⏹️ CD 阶段（跳过）
   原因: if: github.event_name == 'push' AND github.ref == 'refs/heads/main'
   结果: CD 不执行
```

### Push to Main (执行完整 CI/CD)

```
事件: 推送到 main
触发器: push
分支: main

执行阶段:
├─ ✅ CI 阶段（全部）
│  ├─ 代码检查
│  ├─ 构建测试
│  ├─ 集成测试
│  └─ CI 总结
│
└─ ✅ CD 阶段（全部）
   ├─ 构建镜像
   ├─ 部署测试环境
   ├─ 冒烟测试
   ├─ 等待审批 ⏸️
   ├─ 部署生产环境
   └─ 部署验证
```

---

## 🔍 如何查看详细日志

### 在 GitHub 上查看

1. 进入仓库页面
2. 点击 "Actions" 标签
3. 选择具体的 workflow run
4. 点击任一 job 查看详细日志

### 关键日志示例

**CI 成功日志**：
```bash
📊 CI 阶段总结报告
━━━━━━━━━━━━━━━━━━━━━━━
✅ 代码检查: success
✅ 构建测试: success
✅ 集成测试: success
━━━━━━━━━━━━━━━━━━━━━━━
✨ CI 全部通过！
```

**CI 失败日志**：
```bash
📊 检查 CI 各阶段状态...
代码检查: success
构建测试: failure
集成测试: skipped

❌ CI 失败！流程终止，请修复问题后重新提交
Error: Process completed with exit code 1.
```

---

## ✅ 验证退回机制

### 确认 CD 不会在 CI 失败时执行

在 workflow 日志中，你会看到：

```
ci-summary
  ❌ The job failed

build-docker
  ⏹️ This job was skipped
  Reason: Dependency job 'ci-summary' failed

deploy-staging
  ⏹️ This job was skipped
  Reason: Dependency job 'ci-summary' failed

... (所有 CD 阶段都被跳过)
```

这证明了：
- ✅ CI 失败被正确检测
- ✅ CD 阶段被正确阻止
- ✅ 退回机制工作正常

---

## 🎓 关键概念总结

### 1. 依赖链 (needs)

```yaml
job-b:
  needs: job-a  # job-a 必须成功，job-b 才会执行
```

### 2. 条件执行 (if)

```yaml
job:
  if: github.ref == 'refs/heads/main'  # 只在 main 分支执行
```

### 3. 失败处理

```yaml
steps:
  - run: |
      if [[ "${{ needs.previous.result }}" == "failure" ]]; then
        echo "❌ 上一步失败，终止流程"
        exit 1  # 这会标记当前 job 为失败
      fi
```

### 4. 始终执行 (always)

```yaml
job:
  if: always()  # 即使依赖失败也执行（用于总结）
```

---

## 🚀 现在就试试！

```bash
# 1. 推送演示分支
cd /root/voyagemate/new-voyage-mate
git push origin demo/ci-failure

# 2. 在 GitHub 创建 PR

# 3. 观察 CI 流程

# 4. 合并后观察完整 CI/CD 流程
```

---

## 📝 注意事项

### 当前的演示 workflow 是**模拟**的

实际的部署步骤（阶段 6-10）使用的是 `echo` 命令模拟。

要实现真实部署，需要：

1. **添加 Dockerfile**:
   ```dockerfile
   # backend/Dockerfile
   FROM openjdk:17-jdk-slim
   COPY target/*.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

2. **配置 GitHub Secrets**:
   - `SSH_PRIVATE_KEY`
   - `SERVER_HOST`
   - `DOCKER_USERNAME`
   - `DOCKER_PASSWORD`

3. **准备服务器**:
   - 测试环境服务器
   - 生产环境服务器
   - 安装 Docker

4. **替换 echo 为实际命令**:
   ```yaml
   # 将
   - run: echo "🚀 部署到测试环境..."
   
   # 改为
   - run: |
       ssh user@${{ secrets.SERVER_HOST }} "
         docker pull myapp:latest
         docker-compose up -d
       "
   ```

但**核心的 CI 失败退回机制已经完全可用**！

---

## ✨ 总结

你现在拥有的能力：

| 功能 | 状态 | 说明 |
|------|------|------|
| PR 触发 CI | ✅ 可用 | 提交 PR 自动运行检查 |
| CI 失败退回 | ✅ 可用 | 检测失败并阻止后续步骤 |
| CI 成功继续 | ✅ 可用 | CI 通过后执行 CD |
| 分支保护 | ✅ 可用 | 只在 main 分支部署 |
| 人工审批 | ✅ 可用 | 生产部署需要批准 |
| 实际部署 | ⚠️ 需配置 | 需要服务器和凭证 |

🎉 **可以开始演示了！**

