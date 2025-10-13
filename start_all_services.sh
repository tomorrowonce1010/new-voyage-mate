#!/bin/bash

echo "=== 启动所有服务 (修正版) ==="

BASE_DIR="/root/voyagemate/new-voyage-mate"
VENV_PATH="$BASE_DIR/.venv/bin/activate"

# 设置交换空间
setup_swap() {
    if [ ! -f /swapfile ]; then
        echo "创建交换空间..."
        sudo fallocate -l 4G /swapfile
        sudo chmod 600 /swapfile
        sudo mkswap /swapfile
        sudo swapon /swapfile
        echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    fi
    echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
    sudo sysctl -p
}

# 启动MySQL
start_mysql() {
    echo "启动MySQL..."
    sudo systemctl start mysql
    # 优化MySQL内存
    mysql -e "SET GLOBAL innodb_buffer_pool_size=256*1024*1024;" 2>/dev/null || true
}

# 启动ElasticSearch（优化内存）
start_elasticsearch() {
    echo "启动ElasticSearch..."
    
    # 创建低内存配置
    sudo mkdir -p /etc/elasticsearch/jvm.options.d
    sudo tee /etc/elasticsearch/jvm.options.d/low-memory.conf << EOF
-Xms512m
-Xmx512m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=150
-Des.enforce.bootstrap.checks=false
EOF

    sudo systemctl start elasticsearch
    echo "等待ElasticSearch启动..."
    sleep 15
}

# 启动Spring Boot后端（内存限制）
start_backend() {
    echo "启动Spring Boot后端..."
    cd $BASE_DIR/backend
    
    # 使用内存限制启动
    nohup java -Xms256m -Xmx512m -jar target/backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
    echo $! > backend.pid
    echo "后端启动中... (PID: $(cat backend.pid))"
}

# 启动Python Embedding服务
start_embedding() {
    echo "启动Embedding服务..."
    cd $BASE_DIR/embedding-service
    
    # 使用虚拟环境直接启动uvicorn
    nohup $BASE_DIR/.venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 > embedding.log 2>&1 &
    echo $! > embedding.pid
    echo "Embedding服务启动中... (PID: $(cat embedding.pid))"
    
    # 等待模型加载
    echo "等待Embedding服务模型加载..."
    sleep 30
}

# 启动Python RAG服务
start_rag() {
    echo "启动RAG服务..."
    cd $BASE_DIR/rag-service
    
    # 使用虚拟环境直接启动Python
    nohup $BASE_DIR/.venv/bin/python simple_rag_service.py > rag.log 2>&1 &
    echo $! > rag.pid
    echo "RAG服务启动中... (PID: $(cat rag.pid))"
    
    # 等待RAG服务初始化
    sleep 10
}

# 启动前端
start_frontend() {
    echo "启动前端..."
    cd $BASE_DIR/frontend
    
    # 使用内存限制启动Node.js
    export NODE_OPTIONS="--max-old-space-size=512"
    nohup npm start > frontend.log 2>&1 &
    echo $! > frontend.pid
    echo "前端启动中... (PID: $(cat frontend.pid))"
}

# 监控服务状态
monitor_services() {
    echo ""
    echo "=== 服务状态检查 ==="
    
    # 系统服务状态
    services=("mysql" "elasticsearch")
    for service in "${services[@]}"; do
        if systemctl is-active --quiet $service; then
            echo "✅ $service 运行中"
        else
            echo "❌ $service 未运行"
        fi
    done
    
    # 检查进程状态
    check_process() {
        local service_name=$1
        local pid_file=$2
        local port=$3
        
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            if ps -p $pid > /dev/null 2>&1; then
                # 检查端口是否在监听
                if netstat -tln 2>/dev/null | grep ":$port " > /dev/null; then
                    echo "✅ $service_name 运行中 (PID: $pid, Port: $port)"
                else
                    echo "⚠️  $service_name 进程存在但端口未监听 (PID: $pid, Port: $port)"
                fi
            else
                echo "❌ $service_name 进程不存在 (PID文件: $pid_file)"
            fi
        else
            echo "❌ $service_name PID文件不存在: $pid_file"
        fi
    }
    
    check_process "后端" "$BASE_DIR/backend/backend.pid" "8080"
    check_process "Embedding" "$BASE_DIR/embedding-service/embedding.pid" "8000"
    check_process "RAG" "$BASE_DIR/rag-service/rag.pid" "8001"
    check_process "前端" "$BASE_DIR/frontend/frontend.pid" "3000"
    
    echo ""
    echo "=== 内存使用情况 ==="
    free -h
    
    echo ""
    echo "=== 服务访问地址 ==="
    echo "前端: http://$(curl -s ifconfig.me):3000"
    echo "后端API: http://$(curl -s ifconfig.me):8080"
    echo "Embedding服务: http://$(curl -s ifconfig.me):8000"
    echo "RAG服务: http://$(curl -s ifconfig.me):8001"
    echo "ElasticSearch: http://$(curl -s ifconfig.me):9200"
}

# 主执行流程
main() {
    echo "基础目录: $BASE_DIR"
    echo "虚拟环境: $VENV_PATH"
    
    setup_swap
    start_mysql
    start_elasticsearch
    start_backend
    start_embedding
    start_rag
    start_frontend
    
    # 最终状态检查
    sleep 10
    monitor_services
    
    echo ""
    echo "🎉 所有服务启动完成!"
    echo "查看日志: tail -f $BASE_DIR/*/*.log"
    echo "停止所有服务: ./stop_all_services.sh"
}

main
