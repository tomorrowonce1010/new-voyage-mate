#!/bin/bash

# 健康检查测试脚本
# 用于快速验证所有服务的健康状态

echo "========================================="
echo "   VoyageMate 服务健康检查"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_service() {
    local name=$1
    local url=$2
    local expected=$3
    
    echo -n "检查 $name ... "
    
    response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✅ 健康${NC}"
        if [ -n "$body" ]; then
            echo "   响应: $body" | head -c 100
            echo ""
        fi
    else
        echo -e "${RED}❌ 不可用 (HTTP $http_code)${NC}"
        return 1
    fi
    echo ""
    return 0
}

# 计数器
total=0
success=0

# 1. Backend Service
echo "1️⃣  Backend Service (Spring Boot)"
if check_service "Backend" "http://localhost:8080/api/actuator/health" "UP"; then
    ((success++))
fi
((total++))

# 2. Frontend Service
echo "2️⃣  Frontend Service (React)"
if check_service "Frontend" "http://localhost:3000/health" "OK"; then
    ((success++))
fi
((total++))

# 3. Embedding Service
echo "3️⃣  Embedding Service (FastAPI)"
if check_service "Embedding" "http://localhost:8000/health" "healthy"; then
    ((success++))
fi
((total++))

# 4. RAG Service
echo "4️⃣  RAG Service (FastAPI)"
if check_service "RAG" "http://localhost:8001/health" "healthy"; then
    ((success++))
fi
((total++))

# 总结
echo "========================================="
echo "总结: $success/$total 服务健康"
echo "========================================="

if [ $success -eq $total ]; then
    echo -e "${GREEN}✅ 所有服务运行正常！${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  部分服务不可用${NC}"
    exit 1
fi

