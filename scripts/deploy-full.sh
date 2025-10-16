#!/bin/bash

# VoyageMate 完整自动部署脚本
# 包含所有微服务：MySQL, Elasticsearch, Backend, Embedding, RAG, Frontend

set -e  # 遇到错误立即退出

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 VoyageMate 完整服务部署"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 配置变量
PROJECT_DIR="/root/voyagemate/new-voyage-mate"
VENV_PATH="$PROJECT_DIR/.venv/bin/activate"

# 端口配置
BACKEND_PORT=8080
EMBEDDING_PORT=8000
RAG_PORT=8001
FRONTEND_PORT=3000
ELASTICSEARCH_PORT=9200

# ============================================
# 第 1 步：拉取最新代码
# ============================================
echo "📥 [1/8] 拉取最新代码..."
cd $PROJECT_DIR
git fetch origin
git reset --hard origin/main
echo "✅ 代码更新完成"
echo ""

# ============================================
# 第 2 步：停止所有服务
# ============================================
echo "⏹️  [2/8] 停止所有服务..."

# 停止系统服务
echo "   停止 Elasticsearch..."
sudo systemctl stop elasticsearch 2>/dev/null || true

# 停止 PID 文件记录的服务
stop_service() {
    local service_name=$1
    local pid_file=$2
    
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo "   停止 $service_name (PID: $pid)..."
            kill $pid 2>/dev/null || true
            sleep 2
            rm "$pid_file"
        fi
    fi
}

stop_service "Backend" "$PROJECT_DIR/backend/backend.pid"
stop_service "Embedding" "$PROJECT_DIR/embedding-service/embedding.pid"
stop_service "RAG" "$PROJECT_DIR/rag-service/rag.pid"
stop_service "Frontend" "$PROJECT_DIR/frontend/frontend.pid"

# 强制停止可能残留的进程
pkill -f "java -jar" 2>/dev/null || true
pkill -f "uvicorn main:app" 2>/dev/null || true
pkill -f "simple_rag_service.py" 2>/dev/null || true
pkill -f "serve -s build" 2>/dev/null || true

sleep 3
echo "✅ 所有服务已停止"
echo ""

# ============================================
# 第 3 步：启动基础服务（MySQL + Elasticsearch）
# ============================================
echo "🗄️  [3/8] 启动基础服务..."

# MySQL
echo "   启动 MySQL..."
sudo systemctl start mysql
if systemctl is-active --quiet mysql; then
    echo "   ✅ MySQL 运行中"
else
    echo "   ❌ MySQL 启动失败"
    exit 1
fi

# Elasticsearch
echo "   启动 Elasticsearch..."
sudo systemctl start elasticsearch
sleep 15  # 等待 Elasticsearch 启动
if systemctl is-active --quiet elasticsearch; then
    echo "   ✅ Elasticsearch 运行中"
else
    echo "   ⚠️  Elasticsearch 启动失败，继续部署"
fi
echo ""

# ============================================
# 第 4 步：构建并启动 Backend
# ============================================
echo "🔨 [4/8] 构建并启动 Backend..."
cd $PROJECT_DIR/backend

# 清理旧构建
rm -rf target/

# Maven 构建
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "   ✅ Backend 构建成功"
    
    # 启动 Backend (内存限制)
    nohup java -Xms256m -Xmx512m -jar target/*.jar > $PROJECT_DIR/backend.log 2>&1 &
    echo $! > backend.pid
    
    BACKEND_PID=$(cat backend.pid)
    echo "   Backend 启动中... (PID: $BACKEND_PID)"
    
    # 等待启动
    sleep 10
    
    # 检查是否成功
    if ps -p $BACKEND_PID > /dev/null; then
        echo "   ✅ Backend 运行中 (端口: $BACKEND_PORT)"
    else
        echo "   ❌ Backend 启动失败"
        exit 1
    fi
else
    echo "   ❌ Backend 构建失败"
    exit 1
fi
echo ""

# ============================================
# 第 5 步：启动 Embedding Service
# ============================================
echo "🤖 [5/8] 启动 Embedding Service..."
cd $PROJECT_DIR/embedding-service

# 检查虚拟环境
if [ ! -f "$VENV_PATH" ]; then
    echo "   ⚠️  虚拟环境不存在，跳过 Embedding Service"
else
    # 启动 uvicorn
    nohup $PROJECT_DIR/.venv/bin/uvicorn main:app --host 0.0.0.0 --port $EMBEDDING_PORT > embedding.log 2>&1 &
    echo $! > embedding.pid
    
    EMBEDDING_PID=$(cat embedding.pid)
    echo "   Embedding Service 启动中... (PID: $EMBEDDING_PID)"
    echo "   等待模型加载..."
    sleep 20
    
    if ps -p $EMBEDDING_PID > /dev/null; then
        echo "   ✅ Embedding Service 运行中 (端口: $EMBEDDING_PORT)"
    else
        echo "   ⚠️  Embedding Service 启动失败，继续部署"
    fi
fi
echo ""

# ============================================
# 第 6 步：启动 RAG Service
# ============================================
echo "🧠 [6/8] 启动 RAG Service..."
cd $PROJECT_DIR/rag-service

if [ ! -f "$VENV_PATH" ]; then
    echo "   ⚠️  虚拟环境不存在，跳过 RAG Service"
else
    # 检查 simple_rag_service.py 是否存在
    if [ -f "simple_rag_service.py" ]; then
        nohup $PROJECT_DIR/.venv/bin/python simple_rag_service.py > rag.log 2>&1 &
        echo $! > rag.pid
        
        RAG_PID=$(cat rag.pid)
        echo "   RAG Service 启动中... (PID: $RAG_PID)"
        sleep 10
        
        if ps -p $RAG_PID > /dev/null; then
            echo "   ✅ RAG Service 运行中 (端口: $RAG_PORT)"
        else
            echo "   ⚠️  RAG Service 启动失败，继续部署"
        fi
    else
        echo "   ⚠️  simple_rag_service.py 不存在，跳过"
    fi
fi
echo ""

# ============================================
# 第 7 步：构建 Frontend 并通过 Nginx 发布
# ============================================
echo "🎨 [7/8] 构建 Frontend 并发布到 Nginx..."
cd $PROJECT_DIR/frontend

# 安装/更新依赖
npm ci

# 构建生产版本
CI=false npm run build

if [ $? -eq 0 ]; then
    echo "   ✅ Frontend 构建成功"

    # 目标静态目录（Nginx 根目录）
    NGINX_ROOT_DIR="/var/www/voyagemate"

    echo "   同步静态资源到 $NGINX_ROOT_DIR ..."
    sudo mkdir -p "$NGINX_ROOT_DIR"
    sudo rm -rf "$NGINX_ROOT_DIR"/*
    sudo cp -r build/* "$NGINX_ROOT_DIR"/

    echo "   校验 Nginx 配置..."
    if sudo nginx -t; then
        echo "   重新加载 Nginx..."
        sudo systemctl reload nginx || sudo systemctl restart nginx
        echo "   ✅ Nginx 已重新加载"
    else
        echo "   ❌ Nginx 配置校验失败"
        exit 1
    fi
else
    echo "   ❌ Frontend 构建失败"
    exit 1
fi
echo ""

# ============================================
echo ""

# ============================================
# 部署完成总结
# ============================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 部署完成！"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 服务状态:"
echo "   MySQL:       $(systemctl is-active mysql)"
echo "   Elasticsearch: $(systemctl is-active elasticsearch)"
echo "   Backend:     http://1.94.200.25:$BACKEND_PORT"
echo "   Embedding:   http://1.94.200.25:$EMBEDDING_PORT"
echo "   RAG:         http://1.94.200.25:$RAG_PORT"
echo "   Frontend:    http://1.94.200.25 (Nginx)"
echo ""
echo "📝 查看日志:"
echo "   Backend:    tail -f $PROJECT_DIR/backend.log"
echo "   Embedding:  tail -f $PROJECT_DIR/embedding-service/embedding.log"
echo "   RAG:        tail -f $PROJECT_DIR/rag-service/rag.log"
echo "   Frontend:   tail -f $PROJECT_DIR/frontend.log"
echo ""
echo "🔧 管理命令:"
echo "   查看所有进程: ps aux | grep -E 'java|uvicorn|serve'"
echo "   停止所有服务: bash $PROJECT_DIR/stop_all_services.sh"
echo "   重新部署: bash $PROJECT_DIR/scripts/deploy-full.sh"
echo ""

