## 🚀 启动服务

### 方式一：使用 Makefile（推荐）

```bash
# 安装所有依赖
make install

# 启动各个服务（在不同的终端窗口）
make dev-backend      # 启动 Backend
make dev-frontend     # 启动 Frontend
make dev-embedding    # 启动 Embedding Service
make dev-rag         # 启动 RAG Service
```
## 🧪 运行测试

### 全部测试

```bash
make test
```

### Backend 测试

```bash
cd backend
mvn test

# 生成覆盖率报告
mvn jacoco:report
# 报告位置：target/site/jacoco/index.html
```

### Frontend 测试

```bash
cd frontend
npm test

# 带覆盖率的测试
npm test -- --coverage
# 报告位置：coverage/lcov-report/index.html
```

### Python Services 测试

```bash
# Embedding Service
cd embedding-service
source venv/bin/activate
pytest tests/ --cov=. --cov-report=html
# 报告位置：htmlcov/index.html

# RAG Service
cd rag-service
source venv/bin/activate
pytest tests/ --cov=. --cov-report=html
```

---

## 📊 代码质量检查

### Backend (Java)

```bash
cd backend

# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check
```

### Frontend (JavaScript)

```bash
cd frontend

# ESLint
npm run lint

# 自动修复
npm run lint -- --fix
```

### Python Services

```bash
# 激活虚拟环境
cd embedding-service
source venv/bin/activate

# Flake8 代码检查
flake8 .

# Black 代码格式化
black .

# isort 导入排序
isort .

# Bandit 安全检查
bandit -r . -ll
```

---
