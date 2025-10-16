# VoyageMate 环境变量配置指南

本文档说明如何为 VoyageMate 项目配置环境变量，以保护敏感信息（如 API keys 和数据库密码）。

## 🔒 安全说明

**重要**：所有包含敏感信息的 `.env` 文件都已被添加到 `.gitignore`，不会被提交到版本控制系统。请勿将敏感信息直接写入代码。

## 📋 配置步骤

### 1. 后端配置

#### 1.1 复制环境变量模板

```bash
cd /path/to/new-voyage-mate
cp .env.example .env
```

#### 1.2 编辑 `.env` 文件并填入真实的配置值

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=voyagemate
DB_USERNAME=你的数据库用户名
DB_PASSWORD=你的数据库密码

# DeepSeek AI 配置
DEEPSEEK_API_KEY=你的DeepSeek_API_Key
DEEPSEEK_API_URL=https://api.deepseek.com/v1/chat/completions
```

#### 1.3 Spring Boot 读取环境变量

`backend/src/main/resources/application.properties` 已配置为从环境变量读取：

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:voyagemate}?...
spring.datasource.username=${DB_USERNAME:voyagemate}
spring.datasource.password=${DB_PASSWORD:}
deepseek.api.key=${DEEPSEEK_API_KEY:}
```

### 2. 前端配置

#### 2.1 创建前端环境变量文件

```bash
cd frontend
cp .env.example .env
```

#### 2.2 编辑 `frontend/.env` 文件

```bash
# 高德地图配置
REACT_APP_AMAP_KEY=你的高德地图API_Key
REACT_APP_AMAP_SECURITY_CODE=你的高德地图安全密钥
```

#### 2.3 React 读取环境变量

前端代码通过 `process.env.REACT_APP_*` 读取环境变量：

```javascript
const amapConfig = {
    key: process.env.REACT_APP_AMAP_KEY || '',
    securityJsCode: process.env.REACT_APP_AMAP_SECURITY_CODE || '',
};
```

### 3. Python 服务配置

Python 服务（RAG/Embedding）通过 `os.getenv()` 读取环境变量：

```python
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "")
```

可以在运行前设置环境变量，或使用 `.env` 文件配合 `python-dotenv`：

```bash
export DEEPSEEK_API_KEY=your_key
export MYSQL_HOST=localhost
export MYSQL_PASSWORD=your_password
python simple_rag_service.py
```

## 🚀 运行项目

### 后端

```bash
cd backend
# 确保 .env 文件存在且已配置
mvn spring-boot:run
```

### 前端

```bash
cd frontend
# 确保 .env 文件存在且已配置
npm start
```

## 📝 如何获取 API Keys

### DeepSeek API Key

1. 访问 [DeepSeek 官网](https://www.deepseek.com/)
2. 注册账号并登录
3. 在控制台中创建 API Key

### 高德地图 API Key

1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 注册开发者账号
3. 创建应用并获取 Key 和安全密钥（jscode）

## ⚠️ 注意事项

1. **切勿提交 `.env` 文件到 Git**：已在 `.gitignore` 中配置忽略
2. **团队协作**：每个开发者需要自己创建 `.env` 文件
3. **生产环境**：在服务器上设置环境变量或使用密钥管理服务
4. **CI/CD**：在 GitHub Secrets 中配置敏感信息

## 🔍 故障排查

### 后端连接数据库失败

```
检查 DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD 是否正确
确保 MySQL 服务正在运行
```

### DeepSeek API 调用失败

```
检查 DEEPSEEK_API_KEY 是否有效
确保 API Key 有足够的配额
```

### 前端高德地图无法加载

```
检查 REACT_APP_AMAP_KEY 是否正确
确保 Key 的服务平台已配置为 "Web端(JS API)"
检查 REACT_APP_AMAP_SECURITY_CODE 是否与 Key 匹配
```

## 📚 相关文档

- [Spring Boot 外部配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [React 环境变量](https://create-react-app.dev/docs/adding-custom-environment-variables/)
- [高德地图 JS API 文档](https://lbs.amap.com/api/javascript-api/summary)
- [DeepSeek API 文档](https://platform.deepseek.com/api-docs/)

