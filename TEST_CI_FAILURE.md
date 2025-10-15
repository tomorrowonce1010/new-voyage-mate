# 测试 CI 失败场景

这个文件用于触发 CI 流程。

## 模拟的问题

故意引入一些会导致构建失败的代码：

```java
// 在 Backend 中添加编译错误
public class BrokenClass {
    public void brokenMethod() {
        // 缺少分号，会导致编译失败
        String x = "test"
    }
}
```

当这个 PR 被创建时：
1. ✅ 代码检查会通过（这只是测试文件）
2. ❌ 构建测试会失败（如果我们真的添加了错误代码）
3. ⏹️ 集成测试不会执行
4. ❌ CI 总结检测到失败
5. 🔙 **流程终止，不进入 CD 阶段**

