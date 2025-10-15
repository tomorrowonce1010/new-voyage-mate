#!/bin/bash

# CI/CD 流程演示脚本
# 用途：自动创建 PR 并演示 CI/CD 流程

set -e  # 遇到错误立即退出

echo "========================================="
echo "   CI/CD 流程演示"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查是否在正确的目录
if [ ! -d ".github/workflows" ]; then
    echo -e "${RED}错误: 请在项目根目录运行此脚本${NC}"
    exit 1
fi

# 步骤 1: 确保在 main 分支
echo -e "${BLUE}步骤 1/6: 切换到 main 分支${NC}"
git checkout main
git pull origin main
echo -e "${GREEN}✅ 已更新到最新的 main 分支${NC}"
echo ""

# 步骤 2: 创建演示分支
BRANCH_NAME="demo/ci-cd-$(date +%Y%m%d-%H%M%S)"
echo -e "${BLUE}步骤 2/6: 创建演示分支 $BRANCH_NAME${NC}"
git checkout -b "$BRANCH_NAME"
echo -e "${GREEN}✅ 演示分支已创建${NC}"
echo ""

# 步骤 3: 选择演示场景
echo -e "${YELLOW}请选择演示场景:${NC}"
echo "1. ✅ CI 成功场景（添加文档）"
echo "2. ❌ CI 失败场景（添加失败的测试）"
echo "3. 🔧 CI 失败后修复场景"
echo ""
read -p "请输入选择 (1/2/3): " SCENARIO

case $SCENARIO in
    1)
        # 场景 1: 成功场景
        echo -e "${BLUE}步骤 3/6: 添加文档（会通过 CI）${NC}"
        cat > CI_CD_DEMO.md << 'EOF'
# CI/CD 演示文档

本文档用于演示 CI/CD 流程。

## 演示内容
- ✅ 文档修改不会影响测试
- ✅ CI 会全部通过
- ✅ 可以成功合并到 main

## 时间
创建时间: $(date)
EOF
        git add CI_CD_DEMO.md
        git commit -m "docs: 添加 CI/CD 演示文档"
        echo -e "${GREEN}✅ 文档已添加${NC}"
        ;;
        
    2)
        # 场景 2: 失败场景
        echo -e "${BLUE}步骤 3/6: 添加会失败的测试${NC}"
        mkdir -p backend/src/test/java/com/se_07/backend/demo
        cat > backend/src/test/java/com/se_07/backend/demo/DemoFailingTest.java << 'EOF'
package com.se_07.backend.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoFailingTest {
    @Test
    public void demonstrateCIFailure() {
        // 这个测试会失败，演示 CI 阻止合并
        fail("这是一个演示 CI 失败的测试");
    }
}
EOF
        git add backend/src/test/java/com/se_07/backend/demo/DemoFailingTest.java
        git commit -m "test: 添加演示失败的测试"
        echo -e "${YELLOW}⚠️  失败测试已添加${NC}"
        ;;
        
    3)
        # 场景 3: 失败后修复
        echo -e "${BLUE}步骤 3/6: 先添加失败的测试${NC}"
        mkdir -p backend/src/test/java/com/se_07/backend/demo
        cat > backend/src/test/java/com/se_07/backend/demo/DemoTest.java << 'EOF'
package com.se_07.backend.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoTest {
    @Test
    public void firstAttempt() {
        fail("第一次提交 - 会失败");
    }
}
EOF
        git add backend/src/test/java/com/se_07/backend/demo/DemoTest.java
        git commit -m "test: 第一次提交（会失败）"
        echo -e "${YELLOW}⚠️  失败测试已添加${NC}"
        
        echo ""
        echo -e "${BLUE}然后修复测试${NC}"
        cat > backend/src/test/java/com/se_07/backend/demo/DemoTest.java << 'EOF'
package com.se_07.backend.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemoTest {
    @Test
    public void afterFix() {
        // 修复后 - 会通过
        assertTrue(true, "测试已修复");
    }
}
EOF
        git add backend/src/test/java/com/se_07/backend/demo/DemoTest.java
        git commit -m "fix: 修复测试"
        echo -e "${GREEN}✅ 测试已修复${NC}"
        ;;
        
    *)
        echo -e "${RED}无效的选择${NC}"
        exit 1
        ;;
esac

echo ""

# 步骤 4: 推送到 GitHub
echo -e "${BLUE}步骤 4/6: 推送到 GitHub${NC}"
git push origin "$BRANCH_NAME"
echo -e "${GREEN}✅ 代码已推送到分支: $BRANCH_NAME${NC}"
echo ""

# 步骤 5: 提供 PR 链接
REPO_URL=$(git remote get-url origin | sed 's/git@github.com:/https:\/\/github.com\//' | sed 's/\.git$//')
PR_URL="${REPO_URL}/compare/main...${BRANCH_NAME}?expand=1"

echo -e "${BLUE}步骤 5/6: 创建 Pull Request${NC}"
echo ""
echo -e "${GREEN}请访问以下链接创建 PR:${NC}"
echo -e "${YELLOW}$PR_URL${NC}"
echo ""
echo "或者手动访问:"
echo "1. 访问: $REPO_URL"
echo "2. 点击 'Compare & pull request' 按钮"
echo "3. 填写 PR 信息并提交"
echo ""

# 步骤 6: 说明观察要点
echo -e "${BLUE}步骤 6/6: 观察 CI/CD 流程${NC}"
echo ""
echo "创建 PR 后，您将看到:"
echo ""

case $SCENARIO in
    1)
        echo -e "${GREEN}✅ 预期结果: CI 全部通过${NC}"
        echo "  - ✅ Backend CI/CD — 通过"
        echo "  - ✅ Frontend CI/CD — 通过"
        echo "  - ✅ Python Services CI/CD — 通过"
        echo "  - ✅ Merge 按钮启用，可以合并"
        echo ""
        echo "合并后:"
        echo "  → 触发 CD 流程"
        echo "  → 自动部署到测试环境"
        echo "  → 运行冒烟测试"
        ;;
        
    2)
        echo -e "${RED}❌ 预期结果: Backend CI 失败${NC}"
        echo "  - ❌ Backend CI/CD — 失败"
        echo "     原因: DemoFailingTest.demonstrateCIFailure"
        echo "  - ✅ Frontend CI/CD — 通过"
        echo "  - ✅ Python Services CI/CD — 通过"
        echo "  - ⚠️  Merge 按钮被禁用"
        echo ""
        echo "修复方法:"
        echo "  1. 删除失败的测试文件"
        echo "  2. 提交并推送"
        echo "  3. CI 自动重新运行"
        ;;
        
    3)
        echo -e "${YELLOW}📊 预期结果: 先失败后成功${NC}"
        echo ""
        echo "第一次提交:"
        echo "  - ❌ Backend CI/CD — 失败"
        echo ""
        echo "第二次提交（修复后）:"
        echo "  - ✅ Backend CI/CD — 通过"
        echo "  - ✅ Merge 按钮启用"
        ;;
esac

echo ""
echo "========================================="
echo "观察 CI 运行:"
echo "${REPO_URL}/actions"
echo "========================================="
echo ""
echo -e "${GREEN}演示准备完成！${NC}"
echo ""
echo "下一步:"
echo "1. 访问上面的 PR 链接创建 Pull Request"
echo "2. 观察 CI 检查运行"
echo "3. 根据场景决定是否合并或修复"
echo ""

