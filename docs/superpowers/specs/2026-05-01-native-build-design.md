# Native 构建支持设计

**日期：** 2026-05-01  
**适用范围：** harness-parent lifecycle-biz，本地开发 Native 构建

---

## 一、目标

为 lifecycle-biz 提供 GraalVM Native Image 构建模式，通过 `gradle nativeCompile` 生成原生可执行文件，正常启动运行。

---

## 二、核心方案

用 `@MapperScan` 替换 `@Mapper`，让 Spring AOT 在 `processAot` 阶段静态分析 Mapper 接口并生成 BeanDefinition，避免运行时反射扫描。

**为什么不用 `reflect-config.json`：** AOT BeanDefinition 预生成是 Spring Boot 3+ 的官方 Native 路径，比手写 JSON 更可维护，且能覆盖 MyBatis-Plus 的代理生成逻辑。

---

## 三、变更文件

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `Application.java` | 修改 | 添加 `@MapperScan("com.harness.lifecycle.auth.mapper")` |
| `WechatGitlabBindingMapper.java` | 修改 | 移除 `@Mapper` 注解 |
| `gradle/libs.versions.toml` | 修改 | 添加 `native-build-tools = "0.10.4"` |
| `lifecycle-biz/build.gradle` | 修改 | 添加 `org.graalvm.buildtools.native` 插件 |

---

## 四、AOT 处理流程

```
gradle processAot
  → Spring 静态分析所有 Bean（含 @MapperScan 扫描到的 Mapper）
  → 生成 BeanDefinition 源码 + reflect-hints
  → gradle nativeCompile
  → GraalVM 编译为原生可执行文件
```

---

## 五、已知 Native 兼容性

| 组件 | Native 支持状态 |
|------|---------------|
| Spring Boot 4.0 | 内置 AOT 支持 |
| Spring Authorization Server 1.5.1 | 内置 RuntimeHints |
| MapStruct 1.6.3 | 编译期生成，Native 友好 |
| Lombok | 编译期处理，Native 友好 |
| MyBatis-Plus 3.5.16 + @MapperScan | AOT 可静态分析 |
| MySQL Connector/J 9.x | 需 `runtimeOnly`（已配置） |

---

## 六、验证

```bash
gradle :harness-biz:lifecycle-biz:nativeCompile
./harness-biz/lifecycle-biz/build/native/nativeCompile/lifecycle-biz
```

Expected: 服务在 < 1s 内启动，`/auth/wechat/login` 正常响应。
