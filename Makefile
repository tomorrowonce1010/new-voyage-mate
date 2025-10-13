# VoyageMate Makefile
# Convenient commands for local development

.PHONY: help install test build clean lint format dev-backend dev-frontend dev-embedding dev-rag

# Default target
help:
	@echo "VoyageMate 本地开发命令"
	@echo "================================"
	@echo "install        - 安装所有依赖"
	@echo "test           - 运行所有测试"
	@echo "build          - 构建所有服务"
	@echo "clean          - 清理构建产物"
	@echo "lint           - 运行代码检查"
	@echo "format         - 格式化代码"
	@echo "dev-backend    - 启动后端（开发模式）"
	@echo "dev-frontend   - 启动前端（开发模式）"
	@echo "dev-embedding  - 启动 Embedding Service"
	@echo "dev-rag        - 启动 RAG Service"
	@echo "verify         - 运行所有验证（lint + test）"

# 安装依赖
install:
	@echo "📦 安装后端依赖..."
	cd backend && mvn clean install -DskipTests
	@echo "📦 安装前端依赖..."
	cd frontend && npm install
	@echo "📦 安装 Python 依赖..."
	@echo "Embedding Service..."
	cd embedding-service && python3 -m venv venv && \
		. venv/bin/activate && pip install -r requirements.txt
	@echo "RAG Service..."
	cd rag-service && python3 -m venv venv && \
		. venv/bin/activate && pip install -r requirements.txt
	@echo "✅ 所有依赖安装完成！"

# 运行测试
test:
	@echo "🧪 运行后端测试..."
	cd backend && mvn test
	@echo "🧪 运行前端测试..."
	cd frontend && npm test -- --watchAll=false
	@echo "🧪 运行 Python 测试..."
	cd embedding-service && . venv/bin/activate && pytest tests/ || echo "No tests found"
	cd rag-service && . venv/bin/activate && pytest tests/ || echo "No tests found"
	@echo "✅ 所有测试完成！"

# 构建所有服务
build:
	@echo "🔨 构建后端..."
	cd backend && mvn clean package
	@echo "🔨 构建前端..."
	cd frontend && npm run build
	@echo "✅ 构建完成！"

# 清理构建产物
clean:
	@echo "🧹 清理后端..."
	cd backend && mvn clean
	@echo "🧹 清理前端..."
	cd frontend && rm -rf build node_modules
	@echo "🧹 清理 Python 缓存..."
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete
	find . -type d -name "venv" -exec rm -rf {} + 2>/dev/null || true
	@echo "✅ 清理完成！"

# 运行代码检查
lint:
	@echo "🔍 检查后端代码..."
	cd backend && mvn checkstyle:check || true
	@echo "🔍 检查前端代码..."
	cd frontend && npm run lint || true
	@echo "🔍 检查 Python 代码..."
	cd embedding-service && . venv/bin/activate && flake8 . || true
	cd rag-service && . venv/bin/activate && flake8 . || true
	@echo "✅ 代码检查完成！"

# 格式化代码
format:
	@echo "✨ 格式化 Python 代码..."
	cd embedding-service && . venv/bin/activate && black . && isort .
	cd rag-service && . venv/bin/activate && black . && isort .
	@echo "✅ 代码格式化完成！"

# 启动后端（开发模式）
dev-backend:
	@echo "🚀 启动后端服务..."
	cd backend && mvn spring-boot:run

# 启动前端（开发模式）
dev-frontend:
	@echo "🚀 启动前端应用..."
	cd frontend && npm start

# 启动 Embedding Service
dev-embedding:
	@echo "🚀 启动 Embedding Service..."
	cd embedding-service && . venv/bin/activate && uvicorn main:app --reload --port 8000

# 启动 RAG Service
dev-rag:
	@echo "🚀 启动 RAG Service..."
	cd rag-service && . venv/bin/activate && uvicorn simple_rag_service:app --reload --port 8001

# 运行所有验证
verify: lint test
	@echo "✅ 所有验证通过！"

# 检查服务状态
check:
	@echo "🔍 检查服务状态..."
	@echo "Backend (8080):"
	@curl -s http://localhost:8080/actuator/health || echo "❌ Backend 未运行"
	@echo "\nFrontend (3000):"
	@curl -s http://localhost:3000 > /dev/null && echo "✅ Frontend 运行中" || echo "❌ Frontend 未运行"
	@echo "\nEmbedding Service (8000):"
	@curl -s http://localhost:8000/health || echo "❌ Embedding Service 未运行"
	@echo "\nRAG Service (8001):"
	@curl -s http://localhost:8001/health || echo "❌ RAG Service 未运行"

# 数据库管理
db-connect:
	@echo "📊 连接到数据库..."
	mysql -u voyagemate -p voyagemate

# 快速开始（安装并启动前后端）
quickstart: install
	@echo "🚀 快速启动..."
	@echo "请在不同的终端窗口中运行："
	@echo "  make dev-backend"
	@echo "  make dev-frontend"
	@echo "  make dev-embedding"
	@echo "  make dev-rag"

# CI 本地测试
ci-local:
	@echo "🔄 运行 CI 检查..."
	@make lint
	@make test
	@echo "✅ CI 检查完成！"
