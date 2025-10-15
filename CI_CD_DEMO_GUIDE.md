# CI/CD 流程完整演示指南

## 📋 目录

1. [CI/CD 流程图](#cicd-流程图)
2. [什么是 CI 失败退回](#什么是-ci-失败退回)
3. [完整演示步骤](#完整演示步骤)
4. [流水线各环节详解](#流水线各环节详解)
5. [实际操作示例](#实际操作示例)

---

## 🔄 CI/CD 流程图

```
开发者工作流程：

1. 创建功能分支
   ↓
2. 编写代码并提交
   ↓
3. Push 到 GitHub
   ↓
4. 创建 Pull Request
   ↓
5. ✅ CI 阶段（自动触发）
   │
   ├─ Backend CI (测试、构建)
   ├─ Frontend CI (测试、构建)  
   └─ Python Services CI (测试、Lint)
   │
   ├─ ✅ 所有 CI 通过
   │   ├─ PR 可以合并
   │   └─ 继续 → 6
   │
   └─ ❌ 任一 CI 失败
       ├─ PR 被阻止合并
       ├─ 开发者修复问题
       └─ 重新提交 → 3

6. 合并 PR 到 main 分支
   ↓
7. 🚀 CD 阶段（自动触发）
   │
   ├─ 部署到测试环境
   ├─ 冒烟测试
   ├─ 等待人工审批（可选）
   └─ 部署到生产环境
   │
   ├─ ✅ 部署成功 → 完成
   └─ ❌ 部署失败 → 回滚
```

---

## 🔍 什么是 CI 失败退回？

### ❌ 常见误解

**错误理解**: "CI 失败会删除我的代码或回退提交"

**✅ 正确理解**: "CI 失败会阻止代码合并到主分支，但不影响已推送的分支"

### 📊 代码状态对比

| 操作 | 功能分支 | Main 分支 | PR 状态 |
|------|----------|-----------|---------|
| **Push 代码** | ✅ 代码已存在 | ❌ 未影响 | - |
| **CI 运行中** | ✅ 代码已存在 | ❌ 未影响 | 🟡 检查中 |
| **CI 通过** | ✅ 代码已存在 | ❌ 未影响 | ✅ 可合并 |
| **CI 失败** | ✅ 代码已存在 | ❌ 未影响 | ❌ 阻止合并 |
| **合并 PR** | ✅ 代码已存在 | ✅ 代码进入 | ✅ 已合并 |

### 🛡️ 分支保护规则

GitHub 通过 **Branch Protection Rules** 实现：

```
Settings → Branches → Branch protection rules

规则配置:
☑ Require status checks to pass before merging
  ☑ Require branches to be up to date before merging
  ☑ Status checks that are required:
      - Backend CI/CD
      - Frontend CI/CD  
      - Python Services CI/CD

效果:
- CI 失败 → Merge 按钮禁用
- CI 通过 → Merge 按钮启用
```

---

## 🎯 完整演示步骤

### 准备工作

```bash
cd /root/voyagemate/new-voyage-mate

# 确保在 main 分支且是最新的
git checkout main
git pull origin main
```

---

### 步骤 1: 创建功能分支

```bash
# 创建新的功能分支
git checkout -b feature/demo-ci-cd

# 查看当前分支
git branch
```

**说明**: 
- 分支命名规范: `feature/功能名称`
- 其他类型: `bugfix/`, `hotfix/`, `docs/`

---

### 步骤 2: 模拟代码修改

#### 场景 A: 会导致 CI 失败的修改

```bash
# 修改一个测试文件，故意引入错误
cat > backend/src/test/java/com/se_07/backend/DemoTest.java << 'EOF'
package com.se_07.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoTest {
    @Test
    public void testThatWillFail() {
        // 故意失败的测试
        assertEquals(1, 2, "这个测试会失败");
    }
}
EOF

# 提交
git add backend/src/test/java/com/se_07/backend/DemoTest.java
git commit -m "test: 添加演示测试（会失败）"
git push origin feature/demo-ci-cd
```

#### 场景 B: 会通过 CI 的修改

```bash
# 添加一个简单的文档修改
echo "# CI/CD 演示" > DEMO.md
git add DEMO.md
git commit -m "docs: 添加演示文档"
git push origin feature/demo-ci-cd
```

---

### 步骤 3: 创建 Pull Request

#### 在 GitHub 网站操作

1. **访问仓库页面**
   ```
   https://github.com/tomorrowonce1010/new-voyage-mate
   ```

2. **看到提示**
   ```
   feature/demo-ci-cd had recent pushes
   [Compare & pull request] 按钮
   ```

3. **点击 "Compare & pull request"**

4. **填写 PR 信息**
   ```
   Title: CI/CD 流程演示
   
   Description:
   ## 目的
   演示 CI/CD 流程
   
   ## 改动
   - 添加演示测试
   
   ## 测试
   - [ ] 本地测试通过
   - [ ] CI 检查通过
   ```

5. **创建 PR**
   - 点击 "Create pull request"

---

### 步骤 4: 观察 CI 执行

#### PR 页面会显示

```
┌─────────────────────────────────────────────────────────┐
│ CI/CD 流程演示 #126                                      │
│ Open  tomorrowonce1010 wants to merge 1 commit          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Some checks haven't completed yet                      │
│                                                         │
│ 🟡 Backend CI/CD — In progress                         │
│ 🟡 Frontend CI/CD — In progress                        │
│ 🟡 Python Services CI/CD — In progress                 │
│                                                         │
│ [View details →]                                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 场景 A: CI 失败时

```
┌─────────────────────────────────────────────────────────┐
│ Some checks were unsuccessful                           │
│                                                         │
│ ❌ Backend CI/CD — Failed                               │
│    × Build and Test / Run tests (pull_request)         │
│    [Details]                                            │
│                                                         │
│ ✅ Frontend CI/CD — Successful                          │
│ ✅ Python Services CI/CD — Successful                   │
│                                                         │
│ ⚠️  Merging is blocked                                  │
│ This branch has conflicts that must be resolved        │
│                                                         │
│ [Merge pull request ▼] ← 按钮被禁用                     │
└─────────────────────────────────────────────────────────┘
```

**这时候**:
- ✅ 代码已经在 `feature/demo-ci-cd` 分支
- ❌ 但不能合并到 `main` 分支
- 🔧 需要修复失败的测试

#### 场景 B: CI 通过时

```
┌─────────────────────────────────────────────────────────┐
│ All checks have passed                                  │
│                                                         │
│ ✅ Backend CI/CD — Successful in 2m 15s                │
│ ✅ Frontend CI/CD — Successful in 1m 45s               │
│ ✅ Python Services CI/CD — Successful in 1m 30s        │
│                                                         │
│ ✅ This branch has no conflicts with the base branch    │
│                                                         │
│ [Merge pull request ▼] ← 按钮启用                      │
│   - Create a merge commit                               │
│   - Squash and merge                                    │
│   - Rebase and merge                                    │
└─────────────────────────────────────────────────────────┘
```

**这时候**:
- ✅ 代码已经在 `feature/demo-ci-cd` 分支
- ✅ 可以合并到 `main` 分支
- ✅ CD 流程会在合并后自动触发

---

### 步骤 5A: 修复 CI 失败（如果失败）

```bash
# 修复失败的测试
cat > backend/src/test/java/com/se_07/backend/DemoTest.java << 'EOF'
package com.se_07.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoTest {
    @Test
    public void testThatWillPass() {
        // 正确的测试
        assertEquals(1, 1, "这个测试会通过");
    }
}
EOF

# 提交修复
git add backend/src/test/java/com/se_07/backend/DemoTest.java
git commit -m "fix: 修复测试失败"
git push origin feature/demo-ci-cd
```

**效果**:
- CI 会自动重新运行
- 不需要重新创建 PR
- 同一个 PR 会更新

---

### 步骤 5B: 合并 PR（CI 通过后）

#### 在 GitHub PR 页面

1. **点击 "Merge pull request"**
2. **选择合并方式**
   - **Merge commit**: 保留所有提交历史
   - **Squash and merge**: 将所有提交压缩为一个
   - **Rebase and merge**: 变基合并

3. **确认合并**
   - 输入提交信息（可选）
   - 点击 "Confirm merge"

4. **删除分支（可选）**
   - 点击 "Delete branch"

---

### 步骤 6: 观察 CD 执行

#### 合并后自动触发

```
合并到 main 分支
    ↓
触发 FINAL CI/CD workflow
    ↓
CI 阶段（再次运行）
    ↓
CD 阶段开始
```

#### CD 流程详情

```
1. ✅ CI Summary
   - 确认所有 CI 通过

2. 🚀 Deploy to Staging
   - SSH 连接到服务器
   - 拉取最新代码
   - 构建应用
   - 重启服务

3. 💨 Smoke Test
   - 测试 Backend: http://server:8080/api/actuator/health
   - 测试 Frontend: http://server:3000
   - 测试 Embedding: http://server:8000/health
   - 测试 RAG: http://server:8001/health

4. 👤 Approval Gate（可选）
   - 等待人工审批
   - 可以在 GitHub 网站上审批

5. 🎯 Deploy to Production
   - 同 Staging 流程
   - 但部署到生产环境

6. ✅ Post Deployment
   - 验证生产环境健康状态
   - 生成部署报告
```

---

## 📊 流水线各环节详解

### CI 阶段（Pull Request 触发）

#### 1️⃣ Backend CI/CD

**位置**: `.github/workflows/backend.yml`

**步骤**:
```yaml
jobs:
  build-and-test:
    steps:
      - Checkout code          # 拉取代码
      - Set up JDK 17         # 配置 Java 环境
      - Cache Maven packages  # 缓存依赖
      - Run tests             # 运行测试 ← 失败会阻止合并
      - Build                 # 构建 JAR 包
      - Upload artifacts      # 上传构建产物
```

**运行时间**: ~2-3 分钟

**失败原因**:
- 单元测试失败
- 编译错误
- 代码规范检查失败

**日志示例**:
```
Run tests
  mvn test
  
[INFO] Tests run: 1040, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
✅ Tests passed
```

#### 2️⃣ Frontend CI/CD

**位置**: `.github/workflows/frontend.yml`

**步骤**:
```yaml
jobs:
  build-and-test:
    steps:
      - Checkout code
      - Set up Node.js
      - Cache npm packages
      - Install dependencies  # npm install
      - Run tests            # npm test ← 失败会阻止合并
      - Build                # npm run build
```

**运行时间**: ~1-2 分钟

**失败原因**:
- Jest 测试失败
- ESLint 检查失败
- 构建错误

#### 3️⃣ Python Services CI/CD

**位置**: `.github/workflows/python-services.yml`

**步骤**:
```yaml
jobs:
  test:
    strategy:
      matrix:
        service: [embedding-service, rag-service]
    steps:
      - Checkout code
      - Set up Python
      - Install dependencies
      - Run linting          # flake8/pylint
      - Run tests            # pytest
```

**运行时间**: ~1-2 分钟

**失败原因**:
- pytest 测试失败
- 代码风格检查失败
- 类型检查失败

---

### CD 阶段（Push to main 触发）

#### 4️⃣ Deploy to Staging

**触发条件**:
```yaml
if: github.event_name == 'push' && 
    github.ref == 'refs/heads/main' && 
    needs.ci-summary.result == 'success'
```

**步骤详解**:

```bash
# 1. SSH 连接到服务器
ssh user@server

# 2. 导航到项目目录
cd /root/voyagemate/new-voyage-mate

# 3. 拉取最新代码
git pull origin main

# 4. 停止旧服务
./stop_all_services.sh

# 5. 构建 Backend
cd backend
mvn clean package -DskipTests

# 6. 启动所有服务
cd ..
./start_all_services.sh

# 7. 等待服务启动
sleep 30
```

**运行时间**: ~3-5 分钟

#### 5️⃣ Smoke Test（冒烟测试）

**目的**: 快速验证基本功能

**测试项**:
```bash
# Backend 健康检查
curl http://server:8080/api/actuator/health
# 期望: {"status":"UP"}

# Frontend 可访问性
curl -I http://server:3000
# 期望: HTTP/1.1 200 OK

# Embedding Service
curl http://server:8000/health
# 期望: {"status":"healthy"}

# RAG Service  
curl http://server:8001/health
# 期望: {"status":"healthy"}
```

**失败处理**:
- 任一测试失败 → CD 流程终止
- 发送通知
- 需要手动回滚

#### 6️⃣ Approval Gate（可选）

**配置**:
```yaml
approval-gate:
  environment:
    name: production-approval  # 需要在 GitHub 设置
```

**审批流程**:
1. Workflow 暂停
2. GitHub 发送通知
3. 指定审批人在网站上审批
4. 审批通过 → 继续
5. 审批拒绝 → 流程终止

---

## 💻 实际操作示例

### 示例 1: 故意制造 CI 失败

```bash
# 1. 创建分支
git checkout -b demo/ci-failure
cd /root/voyagemate/new-voyage-mate

# 2. 添加失败的测试
mkdir -p backend/src/test/java/com/se_07/backend/demo
cat > backend/src/test/java/com/se_07/backend/demo/FailingTest.java << 'EOF'
package com.se_07.backend.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FailingTest {
    @Test
    public void thisWillFail() {
        fail("演示 CI 失败");
    }
}
EOF

# 3. 提交并推送
git add .
git commit -m "test: 添加会失败的测试"
git push origin demo/ci-failure

# 4. 创建 PR
# 在 GitHub 网站创建 PR

# 5. 观察 CI 失败
# PR 页面会显示 ❌ Backend CI/CD Failed
```

**GitHub PR 页面显示**:
```
❌ Some checks were unsuccessful

Backend CI/CD — Failed
  Details: 
  Tests run: 1041, Failures: 1, Errors: 0
  FailingTest.thisWillFail:9 演示 CI 失败

⚠️ Merging is blocked
The required status check "Backend CI/CD" is failing
```

**修复**:
```bash
# 删除失败的测试
rm backend/src/test/java/com/se_07/backend/demo/FailingTest.java
git add .
git commit -m "fix: 移除失败的测试"
git push origin demo/ci-failure

# CI 自动重新运行
# PR 现在显示 ✅ All checks have passed
```

---

### 示例 2: 完整的成功流程

```bash
# 1. 创建功能分支
git checkout main
git pull origin main
git checkout -b feature/add-documentation

# 2. 添加文档
echo "# 新功能文档" > docs/NEW_FEATURE.md
git add docs/NEW_FEATURE.md
git commit -m "docs: 添加新功能文档"

# 3. 推送
git push origin feature/add-documentation

# 4. 创建 PR（在 GitHub 网站）
# Title: 添加新功能文档
# Description: 为新功能添加说明文档

# 5. CI 自动运行
# ✅ Backend CI/CD — 通过（文档修改不影响）
# ✅ Frontend CI/CD — 通过
# ✅ Python Services CI/CD — 通过

# 6. 合并 PR
# 点击 "Merge pull request"
# 选择 "Squash and merge"
# 确认合并

# 7. CD 自动触发
# 因为合并到了 main 分支
# → Deploy to Staging
# → Smoke Test
# → Deploy to Production
```

---

## 🔧 常见问题

### Q1: CI 失败后，我的代码会被删除吗？

**答**: 不会！
- 代码已经在你的功能分支上
- CI 失败只是阻止合并到 main
- 你可以继续在分支上修改

### Q2: 如何跳过 CI 检查？

**答**: 不建议跳过，但如果必须：
- 需要 Admin 权限
- 在 PR 页面点击 "Merge without waiting for requirements to be met"
- 这会留下记录，影响代码质量

### Q3: CI 通过了但 CD 失败怎么办？

**答**: 
- CD 失败不影响代码已经合并到 main
- 但服务可能没有成功部署
- 需要：
  1. 查看 CD 日志找到失败原因
  2. 修复问题
  3. 重新推送触发 CD
  4. 或手动部署

### Q4: 如何重新运行失败的 CI？

**答**:
- GitHub Actions 页面 → 选择失败的 run → "Re-run failed jobs"
- 或者在 PR 分支推送新的提交（推荐）

---

## 📚 相关文档

- 查看 `.github/workflows/` 目录下的所有 workflow 文件
- GitHub Actions 文档: https://docs.github.com/en/actions
- Branch Protection: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches

---

**最后更新**: 2025-10-15

