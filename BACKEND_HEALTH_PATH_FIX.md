# Backend 健康检查路径修复记录

**日期:** 2025-10-15  
**问题:** Backend 健康检查路径缺少 `/api/` 前缀  
**影响:** CI/CD 和部署文档中的健康检查会失败

---

## 问题描述

Backend 应用配置了 `server.servlet.context-path=/api`，因此所有端点都需要加上 `/api/` 前缀。

### 错误的路径
```bash
❌ http://localhost:8080/actuator/health
```

### 正确的路径
```bash
✅ http://localhost:8080/api/actuator/health
```

---

## 修复的文件

### 1. CI/CD 配置
**文件:** `.github/workflows/complete-cicd.yml`

#### 第 145 行 - 冒烟测试
```yaml
# 修复前
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${{ secrets.SERVER_HOST }}:8080/actuator/health || echo "000")

# 修复后
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${{ secrets.SERVER_HOST }}:8080/api/actuator/health || echo "000")
```

#### 第 265 行 - 部署后验证（注释部分）
```yaml
# 修复前
# HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${{ secrets.SERVER_HOST }}:8080/actuator/health || echo "000")

# 修复后
# HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${{ secrets.SERVER_HOST }}:8080/api/actuator/health || echo "000")
```

### 2. 测试部署文档
**文件:** `TEST_DEPLOYMENT.md`

#### 第 203 行 - 远程验证
```bash
# 修复前
curl http://1.94.200.25:8080/actuator/health

# 修复后
curl http://1.94.200.25:8080/api/actuator/health
```

### 3. 部署设置文档
**文件:** `DEPLOY_SETUP.md`

#### 第 111 行 - 健康检查
```bash
# 修复前
curl http://1.94.200.25:8080/actuator/health

# 修复后
curl http://1.94.200.25:8080/api/actuator/health
```

### 4. Makefile
**文件:** `Makefile`

#### 第 114 行 - 服务检查
```makefile
# 修复前
@curl -s http://localhost:8080/actuator/health || echo "❌ Backend 未运行"

# 修复后
@curl -s http://localhost:8080/api/actuator/health || echo "❌ Backend 未运行"
```

---

## 验证

### 本地验证
```bash
# 测试正确的路径
curl http://localhost:8080/api/actuator/health
# 响应: {"status":"UP"}

# 测试错误的路径（应该 404）
curl http://localhost:8080/actuator/health
# 响应: HTTP 404
```

### 自动化验证
```bash
cd /root/voyagemate/new-voyage-mate

# 使用健康检查脚本
./check-health.sh
# 应该显示: ✅ Backend 健康

# 使用 Makefile
make check
# 应该显示 Backend 状态正常
```

---

## 正确使用的文件

以下文件从一开始就使用了正确的路径：

✅ `check-health.sh` - 第 49 行
```bash
check_service "Backend" "http://localhost:8080/api/actuator/health" "UP"
```

✅ `HEALTH_CHECK_STATUS.md` - 多处
✅ `HEALTH_CHECK_SUMMARY.md` - 多处
✅ `QUICK_START_HEALTH_CHECK.md` - 多处
✅ `TROUBLESHOOTING.md` - 多处

---

## 为什么会有这个问题？

### Backend 配置
在 `backend/src/main/resources/application.properties` 中：
```properties
server.servlet.context-path=/api
```

这个配置使得所有端点都需要加上 `/api/` 前缀，包括：
- 业务端点: `/api/destinations`, `/api/attractions` 等
- Actuator 端点: `/api/actuator/health`, `/api/actuator/info` 等

### 常见误区
很多 Spring Boot 应用不设置 context-path，所以 Actuator 端点直接是 `/actuator/health`。但我们的应用设置了 context-path，所以需要加上前缀。

---

## 相关配置

### application.properties
```properties
# 服务器配置
server.port=8080
server.servlet.context-path=/api  # 关键配置

# Actuator配置 - 健康检查端点
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
management.health.defaults.enabled=true
```

### 端点映射
| 原始端点 | 实际访问路径 | 说明 |
|---------|-------------|------|
| `/actuator/health` | `/api/actuator/health` | 健康检查 |
| `/actuator/info` | `/api/actuator/info` | 应用信息 |
| `/destinations` | `/api/destinations` | 目的地列表 |
| `/attractions` | `/api/attractions` | 景点列表 |

---

## 测试清单

- [x] 本地健康检查正常
- [x] CI/CD 配置已修复
- [x] 部署文档已更新
- [x] Makefile 已修复
- [x] 所有文档保持一致
- [x] 自动化脚本验证通过

---

## 预防措施

### 1. 使用统一的健康检查脚本
始终使用 `check-health.sh` 进行健康检查，而不是手动输入路径。

### 2. 配置验证
在 CI/CD 流程中，使用变量定义健康检查路径：
```yaml
env:
  BACKEND_HEALTH_URL: http://${{ secrets.SERVER_HOST }}:8080/api/actuator/health
```

### 3. 文档同步
修改配置时，确保同步更新所有相关文档和脚本。

---

## 参考文档

- Backend 配置: `backend/src/main/resources/application.properties`
- 健康检查指南: `HEALTH_CHECK_STATUS.md`
- 快速启动指南: `QUICK_START_HEALTH_CHECK.md`
- 故障排查: `TROUBLESHOOTING.md`

---

**修复状态:** ✅ 已完成  
**验证状态:** ✅ 已通过  
**最后更新:** 2025-10-15

