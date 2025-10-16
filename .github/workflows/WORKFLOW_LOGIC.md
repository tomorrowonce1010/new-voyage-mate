# CI/CD 工作流逻辑说明

## 📋 完整流程

### 场景 1：提交 PR（分支 → main）

**触发条件**：
```yaml
pull_request:
  branches: [ main ]
  types: [opened, synchronize, reopened]
```

**执行阶段**：
```
✅ CI 阶段
├── backend-ci (运行)
├── frontend-ci (运行，允许失败)
├── python-ci (运行)
└── ci-summary (运行，检查结果)

❌ CD 阶段（不运行）
├── deploy-staging (跳过，条件不满足)
├── smoke-test (跳过)
├── approval-gate (跳过)
└── deploy-production (跳过)
```

**条件判断**：
- `deploy-staging` 的条件：`github.event_name == 'push' && github.ref == 'refs/heads/main'`
- 因为这是 `pull_request` 事件，所以 CD 阶段不会运行 ✓

---

### 场景 2：Merge PR（合并到 main）

**触发条件**：
- 点击 "Merge pull request" 按钮
- GitHub 自动触发 `push` 到 `main` 分支

```yaml
push:
  branches: [ main ]
```

**执行阶段**：
```
✅ CI 阶段（重新运行，确保代码质量）
├── backend-ci (运行)
├── frontend-ci (运行，允许失败)
├── python-ci (运行)
└── ci-summary (运行，检查结果)

✅ CD 阶段（CI 成功后自动运行）
├── deploy-staging (运行部署到测试环境)
├── smoke-test (运行冒烟测试)
├── approval-gate (等待人工审批)
└── deploy-production (部署到生产环境)
```

**条件判断**：
- `deploy-staging` 的条件：
  ```yaml
  if: github.event_name == 'push' && 
      github.ref == 'refs/heads/main' && 
      needs.ci-summary.outputs.ci_status == 'success'
  ```
  ✅ 所有条件都满足，CD 开始执行

---

## 🔍 关键判断逻辑

### CI 阶段

#### backend-ci & python-ci
- **无条件运行**，每次 PR 或 push 都执行
- **失败会阻止 CD**

#### frontend-ci
- **无条件运行**
- **失败不阻止 CD**（在 ci-summary 中被排除）

#### ci-summary
```bash
# 只检查 backend 和 python，前端允许失败
if [[ "${{ needs.backend-ci.result }}" == "failure" ]] || \
   [[ "${{ needs.python-ci.result }}" == "failure" ]]; then
  echo "status=failed"
  exit 1
else
  echo "status=success"
fi
```

---

### CD 阶段

#### deploy-staging（关键入口）
```yaml
if: github.event_name == 'push' && 
    github.ref == 'refs/heads/main' && 
    needs.ci-summary.outputs.ci_status == 'success'
```

**三个条件**：
1. ✅ 必须是 `push` 事件（不是 `pull_request`）
2. ✅ 必须是推送到 `main` 分支
3. ✅ CI 必须成功（backend + python 都通过）

#### smoke-test / approval-gate / deploy-production
```yaml
needs: [前一个 job]
# 无需额外条件，依赖链自动传递
```

**原理**：
- 如果 `deploy-staging` 没有运行，这些 job 也不会运行
- 形成依赖链：`deploy-staging` → `smoke-test` → `approval-gate` → `deploy-production`

---

## 📊 流程对比表

| 事件 | CI 运行 | CD 运行 | 说明 |
|------|---------|---------|------|
| **提交 PR** | ✅ | ❌ | 只验证代码，不部署 |
| **更新 PR** | ✅ | ❌ | 重新验证新代码 |
| **Merge PR** | ✅ | ✅ | 验证 + 自动部署 |
| **直接 push to main** | ✅ | ✅ | 验证 + 自动部署 |

---

## 🎯 使用示例

### 典型开发流程

```bash
# 1️⃣ 在功能分支开发
git checkout -b feature/new-feature
# ... 编写代码 ...
git commit -m "feat: add new feature"
git push origin feature/new-feature

# 2️⃣ 在 GitHub 上创建 PR
# 👉 自动触发 CI（只运行测试，不部署）

# 3️⃣ 等待 CI 通过
# ✅ Backend CI: passed
# ⚠️  Frontend CI: failed (允许失败)
# ✅ Python CI: passed
# ✅ CI Summary: success

# 4️⃣ 点击 "Merge pull request"
# 👉 自动触发 push to main
# 👉 重新运行 CI（确保合并后代码正常）
# 👉 CI 成功后，自动触发 CD
# 👉 部署到测试环境 → 冒烟测试 → 等待审批 → 生产部署
```

---

## ⚠️ 注意事项

### 1. 为什么 Merge 后要重新运行 CI？

虽然 PR 时已经运行过 CI，但：
- 可能有其他 PR 先合并，导致代码冲突
- 确保合并后的最终代码仍然通过测试
- 这是业界最佳实践

### 2. 前端 CI 失败为什么允许部署？

```yaml
# ci-summary 中的逻辑
if [[ "${{ needs.backend-ci.result }}" == "failure" ]] || \
   [[ "${{ needs.python-ci.result }}" == "failure" ]]; then
  # 注意：这里没有检查 frontend-ci
```

- 前端测试可能不完整或不稳定
- 后端是核心服务，必须通过
- 可以根据项目需要调整

### 3. 如何手动触发部署？

```yaml
workflow_dispatch:  # 支持手动触发
```

在 GitHub Actions UI 中点击 "Run workflow" 即可手动触发整个流程（CI + CD）。

---

## 🔧 故障排查

### 问题 1：PR 合并后没有部署

**检查**：
1. CI 是否成功？（查看 ci-summary 输出）
2. 是否合并到了 main 分支？
3. 查看 deploy-staging 的日志，确认条件判断

### 问题 2：PR 时就开始部署了

**原因**：`deploy-staging` 的条件可能有误

**确认**：
```yaml
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```
必须同时满足这两个条件才会部署。

### 问题 3：CI 失败但仍然部署了

**检查** `ci-summary` 的输出：
```yaml
needs.ci-summary.outputs.ci_status == 'success'
```
只有这个值为 `success` 时才会部署。

---

## 📚 参考资料

- [GitHub Actions - Events that trigger workflows](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows)
- [Workflow syntax - needs](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idneeds)
- [Workflow syntax - if](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idif)

**最后更新**: 2025-10-16

