---
name: harness-code-review
description: Code review checklist for harness-parent. Use when reviewing PRs or before committing code. Covers rules that cannot be enforced by Gradle tools (Checkstyle/PMD/ArchUnit).
metadata:
  author: harness
  version: "1.0"
---

# Harness Code Review Checklist

这些规范**无法由 Gradle 工具自动强制**，必须在 Code Review 阶段人工检查。

**使用时机**：PR Review、提交前自查、AI 辅助审查。

---

## 检查流程

对每个改动文件，按以下分类逐条过一遍。发现问题直接在 PR 评论中指出规范章节编号（如"违反 §8.2"）。

---

## §3 编码习惯

- [ ] **§3.3** 无裸 `null` 返回，查询结果用 `Optional` 包装
- [ ] **§3.3** 集合处理优先 Stream API，避免命令式 for 循环
- [ ] **§3.4** Spring Bean 不使用 `@AllArgsConstructor`，用 `@RequiredArgsConstructor` + `final` 字段
- [ ] **§3.4** 日志用 `@Slf4j`，无手动 `private static final Logger`
- [ ] **§3.4** `@Data` 类中无手写 `equals`/`hashCode`
- [ ] **§3.4** 对象赋值用链式调用（`@Accessors(chain=true)`），无 `@Builder` 用于 Spring Bean

---

## §4 Javadoc

- [ ] Entity / VO 类有类级别注释 + 每个字段注释
- [ ] Controller 方法有方法注释 + 参数 + 返回值说明
- [ ] Service 方法有方法注释 + 参数 + 返回值说明

---

## §5 响应与校验

- [ ] Controller 入参有 JSR-303 注解（`@NotNull`、`@NotBlank` 等）
- [ ] Service 层业务规则校验抛 `BizException`，不抛 `RuntimeException`

---

## §6 异常处理

- [ ] 无空 catch 块（`catch (Exception e) {}`）
- [ ] 事务方法内无 try-catch 吞掉异常

---

## §7 日志

- [ ] 循环体内无 `log.` 调用
- [ ] 敏感字段（password、token、手机号）打印前已脱敏

---

## §8 事务

- [ ] `@Transactional` 只加在 Service 层方法
- [ ] `@Transactional` 只加在写操作（insert/update/delete）或需要长连接的查询方法
- [ ] 事务方法内无 `RestTemplate` / `FeignClient` / `WebClient` 调用

---

## §9 DAO 层

- [ ] 联表查询写在 Mapper XML，不用 `@Select` 内联 SQL
- [ ] `LambdaQueryWrapper` 使用方法引用，无魔法字符串字段名（如 `"user_name"`）

---

## §12 依赖管理

- [ ] 新增依赖版本在 `libs.versions.toml` 中管理，无硬编码版本号
- [ ] 无 SNAPSHOT / RC / Beta 版本依赖
- [ ] 依赖 scope 符合最小必要原则（`api-common` 用 `compileOnly`，驱动用 `runtimeOnly`）

---

## §13 安全

- [ ] 无硬编码密码、token、密钥（应从环境变量或配置中心读取）
- [ ] 新增对外接口已接入 auth-common 鉴权

---

## §15 测试

- [ ] 新增 Controller 方法有对应单元测试
- [ ] 新增 Service 方法有对应单元测试
- [ ] 新增自定义 Mapper 方法有对应单元测试

---

## §17 对象映射

- [ ] 无 `BeanUtils.copyProperties` 或 `BeanCopier`
- [ ] 对象转换使用 MapStruct，Mapper 接口放 `converter` 包

---

## §18 配置管理

- [ ] 无裸 `@Value("${xxx}")`，配置读取用 `@ConfigurationProperties`
- [ ] 无敏感配置落入本地 `application.properties`

---

## §19 并发

- [ ] 无 `new Thread()`，异步用 `@Async` 或线程池 Bean

---

## §20 数据库

- [ ] 新增业务表含 `create_time`、`update_time`、`deleted_time` 三个基础字段
- [ ] 无物理删除（`DELETE FROM`），逻辑删除用 `deleted_time`

---

## §21 接口版本

- [ ] 不兼容变更新建版本 Controller，旧版本未直接修改
