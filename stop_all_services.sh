#!/bin/bash

echo "=== 停止所有服务 ==="

# 停止系统服务
sudo systemctl stop elasticsearch

# 停止进程文件中的服务
stop_service() {
    local dir=$1
    local service=$2
    local pid_file="$dir/$service.pid"
    
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null; then
            echo "停止 $service (PID: $pid)"
            kill $pid
            rm "$pid_file"
        fi
    fi
}

stop_service "/root/voyagemate/new-voyage-mate/backend" "backend"
stop_service "/root/voyagemate/new-voyage-mate/embedding-service" "embedding" 
stop_service "/root/voyagemate/new-voyage-mate/rag-service" "rag"
stop_service "/root/voyagemate/new-voyage-mate/frontend" "frontend"

# 确保所有相关进程停止
pkill -f "java -jar" 2>/dev/null || true
pkill -f "node.*react-scripts" 2>/dev/null || true
pkill -f "python.*main.py" 2>/dev/null || true
pkill -f "python.*simple_rag_service.py" 2>/dev/null || true

echo "✅ 所有服务已停止"
