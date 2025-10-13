# VoyageMate - 智能伴游助手 🌏

[![Backend CI/CD](https://github.com/your-org/new-voyage-mate/workflows/Backend%20CI/CD/badge.svg)](https://github.com/your-org/new-voyage-mate/actions)
[![Frontend CI/CD](https://github.com/your-org/new-voyage-mate/workflows/Frontend%20CI/CD/badge.svg)](https://github.com/your-org/new-voyage-mate/actions)
[![Python Services](https://github.com/your-org/new-voyage-mate/workflows/Python%20Services%20CI/CD/badge.svg)](https://github.com/your-org/new-voyage-mate/actions)
[![Code Quality](https://github.com/your-org/new-voyage-mate/workflows/Code%20Quality%20%26%20Security/badge.svg)](https://github.com/your-org/new-voyage-mate/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> 互联网产品开发与运维课程项目 - 基于人工智能的智能旅游规划助手

## 📖 项目简介

VoyageMate 是一个智能伴游助手系统，利用人工智能技术为用户提供个性化的旅游规划和推荐服务。系统结合了传统的数据库查询、向量化语义搜索和 RAG（检索增强生成）技术，为用户提供智能化的旅游体验。

### ✨ 核心特性

- 🎯 **智能景点推荐**: 基于用户偏好和历史行为的个性化推荐
- 🔍 **语义搜索**: 使用向量化技术进行自然语言景点搜索
- 🤖 **智能问答**: RAG 技术支持的智能旅游咨询
- 📍 **地图可视化**: 集成高德地图的交互式地图展示
- 💬 **实时通信**: WebSocket 支持的实时消息推送
- 📊 **数据分析**: 旅游数据的多维度分析和统计

## 🏗️ 技术架构

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                             │
│                  React 18 + Ant Design                       │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/WebSocket
┌───────────────────────┴─────────────────────────────────────┐
│                      Backend Gateway                         │
│                   Spring Boot 3.2.0                          │
└───┬──────────────────┬────────────────┬────────────────┬────┘
    │                  │                │                │
    │                  │                │                │
┌───▼────┐      ┌──────▼─────┐  ┌──────▼───────┐  ┌────▼────┐
│ MySQL  │      │ Embedding  │  │ RAG Service  │  │  ES     │
│        │      │  Service   │  │   (Python)   │  │         │
└────────┘      └────────────┘  └──────────────┘  └─────────┘
```

### 技术栈

#### Backend (Spring Boot)
- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **构建工具**: Maven
- **数据库**: MySQL 8.0 + Elasticsearch 8.13
- **WebSocket**: STOMP over WebSocket
- **测试**: JUnit 5 + Mockito
- **代码覆盖率**: JaCoCo

#### Frontend (React)
- **框架**: React 18
- **UI 库**: Ant Design 5.x
- **路由**: React Router v6
- **地图**: 高德地图 JS API
- **实时通信**: STOMP.js + SockJS
- **测试**: Jest + React Testing Library

#### Embedding Service (Python)
- **框架**: FastAPI
- **向量化**: Sentence Transformers
- **深度学习**: PyTorch
- **搜索引擎**: Elasticsearch 8.x
- **数据库**: MySQL Connector

#### RAG Service (Python)
- **框架**: FastAPI
- **向量数据库**: FAISS
- **NLP**: Sentence Transformers
- **爬虫**: BeautifulSoup4 + Requests
- **数据处理**: NumPy + Scikit-learn

## 🚀 快速开始

### 方式一：本地开发（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/your-org/new-voyage-mate.git
cd new-voyage-mate

# 2. 安装所有依赖
make install

# 3. 配置数据库和环境变量（参考 LOCAL_DEVELOPMENT.md）
# 创建 MySQL 数据库
# 配置 backend/src/main/resources/application.properties
# 创建 .env 文件

# 4. 在不同的终端窗口启动各个服务
make dev-backend      # 终端1：启动 Backend
make dev-frontend     # 终端2：启动 Frontend
make dev-embedding    # 终端3：启动 Embedding Service
make dev-rag         # 终端4：启动 RAG Service
```

**详细步骤请参考 [本地开发指南](./LOCAL_DEVELOPMENT.md)**

### 方式二：使用 Makefile 命令

```bash
# 查看所有可用命令
make help

# 安装依赖
make install

# 运行测试
make test

# 检查服务状态
make check

# 运行所有验证
make verify
```

访问服务：
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Embedding Service**: http://localhost:8000
- **RAG Service**: http://localhost:8001

## 📦 项目结构

```
new-voyage-mate/
├── backend/                 # Spring Boot 后端服务
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # React 前端应用
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── Dockerfile
├── embedding-service/       # 向量化服务
│   ├── main.py
│   ├── requirements.txt
│   └── Dockerfile
├── rag-service/            # RAG 服务
│   ├── simple_rag_service.py
│   ├── requirements.txt
│   └── Dockerfile
├── .github/
│   ├── workflows/          # GitHub Actions CI/CD
│   └── ISSUE_TEMPLATE/     # Issue 模板
├── Makefile               # 本地开发便捷命令
├── LOCAL_DEVELOPMENT.md   # 本地开发详细指南 ⭐
├── GITHUB_SETUP_GUIDE.md  # GitHub CI/CD 配置指南 ⭐
├── SETUP_CHECKLIST.md     # 完整配置清单
├── CI-CD-SUMMARY.md       # CI/CD 功能总结
└── README.md              # 本文件
```

## 🧪 测试

### 运行所有测试
```bash
make test
```

### 分别测试各服务

```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && npm test

# Python Services
cd embedding-service && pytest tests/
cd rag-service && pytest tests/
```

### 代码覆盖率

项目集成了代码覆盖率工具：
- Backend: JaCoCo
- Frontend: Jest Coverage
- Python: pytest-cov

查看覆盖率报告：
```bash
# Backend
cd backend && mvn jacoco:report
open target/site/jacoco/index.html

# Frontend
cd frontend && npm test -- --coverage
open coverage/lcov-report/index.html
```

## 🔄 CI/CD Pipeline

项目使用 GitHub Actions 实现完整的 CI/CD 流程，专注于代码质量和自动化测试：

### 工作流

1. **Main Pipeline** - 协调所有服务的构建和测试
2. **Backend Pipeline** - Java 应用的构建、测试和打包
3. **Frontend Pipeline** - React 应用的构建和测试
4. **Python Services Pipeline** - Python 服务的测试和代码检查
5. **Code Quality** - 代码质量和安全扫描（CodeQL, Trivy）
6. **Release** - 自动化版本发布和构建产物上传

**特点：**
- ✅ 自动化测试和代码检查
- ✅ 代码覆盖率报告（JaCoCo, Jest, pytest-cov）
- ✅ 安全漏洞扫描
- ✅ 依赖自动更新（Dependabot）
- ✅ 专为本地开发优化，无需 Docker

详细信息请查看 [CI/CD 文档](./CI-CD-README.md)

### Pipeline 状态

| 服务 | 状态 | 覆盖率 |
|------|------|--------|
| Backend | ![Backend](https://github.com/your-org/new-voyage-mate/workflows/Backend%20CI/CD/badge.svg) | [![codecov](https://codecov.io/gh/your-org/new-voyage-mate/branch/main/graph/badge.svg?flag=backend)](https://codecov.io/gh/your-org/new-voyage-mate) |
| Frontend | ![Frontend](https://github.com/your-org/new-voyage-mate/workflows/Frontend%20CI/CD/badge.svg) | [![codecov](https://codecov.io/gh/your-org/new-voyage-mate/branch/main/graph/badge.svg?flag=frontend)](https://codecov.io/gh/your-org/new-voyage-mate) |
| Embedding | ![Embedding](https://github.com/your-org/new-voyage-mate/workflows/Python%20Services%20CI/CD/badge.svg) | [![codecov](https://codecov.io/gh/your-org/new-voyage-mate/branch/main/graph/badge.svg?flag=embedding-service)](https://codecov.io/gh/your-org/new-voyage-mate) |
| RAG | ![RAG](https://github.com/your-org/new-voyage-mate/workflows/Python%20Services%20CI/CD/badge.svg) | [![codecov](https://codecov.io/gh/your-org/new-voyage-mate/branch/main/graph/badge.svg?flag=rag-service)](https://codecov.io/gh/your-org/new-voyage-mate) |

## 📊 功能模块

### 1. 用户管理
- 用户注册和登录
- 个人资料管理
- 偏好设置

### 2. 景点管理
- 景点信息浏览
- 景点详情查看
- 地图定位

### 3. 行程规划
- 创建行程
- 编辑行程
- 行程分享
- 协作规划

### 4. 智能推荐
- 基于偏好的推荐
- 基于位置的推荐
- 协同过滤推荐

### 5. 语义搜索
- 自然语言搜索
- 向量化相似度匹配
- 多维度过滤

### 6. 智能问答
- RAG 技术支持
- 上下文理解
- 知识库检索

## 🔐 安全

### 安全措施
- 密码 BCrypt 加密
- SQL 注入防护
- XSS 攻击防护
- CSRF 保护
- 依赖漏洞扫描

### 定期安全检查
- CodeQL 安全分析
- Dependabot 依赖更新
- Trivy 容器扫描

## 📈 性能

### 优化措施
- Redis 缓存 (计划中)
- 数据库索引优化
- 前端代码分割
- 图片懒加载
- API 响应压缩

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献流程

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 开发规范

- 遵循代码风格指南
- 编写单元测试
- 更新相关文档
- 确保 CI/CD 通过

详细信息请查看 [贡献指南](./CONTRIBUTING.md)（如果有）

## 📖 相关文档

推荐阅读顺序：

1. **[GitHub CI/CD 配置指南](./GITHUB_SETUP_GUIDE.md)** ⭐⭐⭐ - 如何在 GitHub 上启用 CI/CD（必读）
2. **[本地开发指南](./LOCAL_DEVELOPMENT.md)** ⭐⭐⭐ - 详细的本地开发环境配置和使用
3. [CI/CD 功能总结](./CI-CD-SUMMARY.md) - CI/CD Pipeline 功能概览
4. [完整配置清单](./SETUP_CHECKLIST.md) - 详细的配置步骤清单

## 📝 版本历史

查看 [CHANGELOG.md](./CHANGELOG.md)（如果有）或 [Releases](https://github.com/your-org/new-voyage-mate/releases)

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](./LICENSE) 文件了解详情

## 👥 团队

- **开发团队**: SE_07 小组
- **项目课程**: 互联网产品开发与运维

## 📞 联系方式

- 项目 Issues: [GitHub Issues](https://github.com/your-org/new-voyage-mate/issues)
- 邮箱: your-email@example.com

## 🙏 致谢

- Spring Boot 社区
- React 社区
- Hugging Face Transformers
- Elasticsearch
- 所有开源项目贡献者

---

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**

**Made with ❤️ by SE_07 Team**
