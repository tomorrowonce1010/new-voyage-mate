# 简化RAG智能伴游助手

这是一个基于维基百科数据的简化RAG（检索增强生成）系统，为智能伴游助手提供城市旅游知识问答功能。

## 🚀 快速开始

### 1. 环境准备

确保已安装：
- Python 3.8+
- Java 17+
- Node.js 16+
### 2. 虚拟环境
```bash
python -m venv .venv
. .venv/Scripts/activate  # Windows: . .venv/Scripts/Activate.ps1
pip install -r requirements.txt
```

### 3. 安装依赖

```bash
cd rag-service
pip install -r requirements.txt
```

### 4. 构建知识库（两种来源）

```bash
# 方案A：维基旅行城市细节（概述/美食/交通/气候等）
## 一次性抓取并构建（默认城市：北京,上海,广州,深圳,杭州,成都,西安）
python build_simple_knowledge_base.py

## 自定义城市并构建
python src/scrape_city_details.py --cities 北京,上海,苏州
python -m src.process_data data/raw/city_details_*.jsonl
python -m src.ingest_faiss data/processed/city_details_*_chunks.jsonl

# 方案B：OpenStreetMap 城市POI（景点/餐饮/交通站点等）
python build_osm_kb.py --cities 上海,苏州,杭州
:: 运行完成后设置 RAG 环境变量以供服务读取
set RAG_PREFIX=merged_chunks
set RAG_CHUNKS=data/processed/merged_chunks.jsonl
```

### 5. 启动服务


1. 启动RAG服务：
```bash
cd rag-service
python simple_rag_service.py
```

2. 启动后端服务：
```bash
cd backend
mvn spring-boot:run
```

3. 启动前端服务：
```bash
cd frontend
npm start
```

## 📁 项目结构

```
rag-service/
├── build_simple_knowledge_base.py  # 知识库构建脚本
├── simple_rag_service.py          # 简化RAG服务
├── src/
│   ├── scrape_wikivoyage.py       # 维基百科数据抓取
│   ├── process_data.py            # 数据处理
│   ├── ingest_faiss.py           # 向量索引构建
│   └── llm.py                     # LLM接口
├── data/
│   ├── raw/                       # 原始数据
│   └── processed/                 # 处理后数据
└── index/                         # 向量索引
```

## 🔧 API接口

### RAG服务 (端口8001)

- `GET /` - 服务信息
- `GET /health` - 健康检查
- `GET /search?query=问题&top_k=5` - 搜索相关文本
- `GET /ask?question=问题&top_k=5` - 智能问答

### 后端服务 (端口8080)

- `GET /api/rag/health` - RAG服务状态
- `GET /api/rag/search?query=问题&topK=5` - 搜索接口
- `GET /api/rag/ask?question=问题&topK=5` - 问答接口
- `POST /api/rag/ask` - 问答接口（POST）

## 💡 使用示例

### 1. 检查服务状态

```bash
curl http://localhost:8001/health
curl http://localhost:8080/api/rag/health
```

### 2. 智能问答

```bash
# 直接调用RAG服务
curl "http://localhost:8001/ask?question=北京有哪些必游景点？"

# 通过后端服务
curl "http://localhost:8080/api/rag/ask?question=上海的美食推荐"
```

### 3. 搜索相关文本

```bash
curl "http://localhost:8080/api/rag/search?query=杭州交通"
```

## 🎯 功能特点

- ✅ 基于维基百科中文城市数据
- ✅ 支持语义搜索
- ✅ 集成DeepSeek LLM
- ✅ RESTful API接口
- ✅ 跨域支持
- ✅ 健康检查
- ✅ 错误处理


## 🛠️ 故障排除

### 1. RAG服务启动失败
- 检查Python依赖是否安装完整
- 确认知识库文件是否存在
- 查看端口8000是否被占用

### 2. 后端服务连接失败
- 检查RAG服务是否正常运行
- 确认application.properties中的RAG服务URL配置
- 查看后端日志

### 3. 知识库构建失败
- 检查网络连接（需要访问维基百科）
- 确认Python环境正确
- 查看构建日志

## 📝 开发说明

### 添加新的数据源

1. 在`src/`目录下创建新的抓取脚本
2. 修改`build_simple_knowledge_base.py`集成新数据源
3. 重新构建知识库

### 自定义LLM

修改`src/llm.py`中的配置：
- 更换API密钥
- 调整模型参数
- 修改提示词模板

### 扩展API接口

在`simple_rag_service.py`中添加新的端点，并在后端`SimpleRAGController.java`中对应添加接口。
