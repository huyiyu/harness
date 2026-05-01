# Lifecycle 服务编码规范

**日期：** 2026-05-01  
**适用范围：** harness-parent 多模块工程，首个服务 lifecycle

---

## 一、项目结构规范

### 1.1 模块划分

```
harness-parent/
├── harness-api/
│   └── lifecycle-api/          # gRPC 接口定义
├── harness-biz/
│   └── lifecycle-biz/          # 服务主体，Spring Boot server
└── harness-common/
    ├── api-common/             # 共享 VO/DTO/工具类/错误码
    ├── biz-common/             # 通用第三方 starter
    └── auth-common/            # 认证鉴权 starter
```

### 1.2 命名约定

| 规范项 | 约定 |
|--------|------|
| biz 模块名 | `{service}-biz`（如 `lifecycle-biz`） |
| api 模块名 | `{service}-api`（如 `lifecycle-api`） |
| biz 根包名 | `com.harness.{service}`（如 `com.harness.lifecycle`） |
| 启动类 | `Application`（无前缀） |
| 常量类 | `interface` 定义，放 `constant` 包 |
| 枚举类 | 放 `enums` 包 |

### 1.3 包结构

**lifecycle-biz（`com.harness.lifecycle`）**

```
com.harness.lifecycle/
├── Application.java
├── controller/          # HTTP 接口，@RestController
│   └── req/             # HTTP 请求对象
│   └── resp/            # HTTP 响应对象（非 R<T> 包装前的业务对象）
├── rpc/                 # gRPC 服务实现，@GrpcService
├── service/             # 业务逻辑接口
│   └── impl/            # 业务逻辑实现
├── mapper/              # MyBatis-Plus Mapper 接口
├── converter/           # MapStruct 对象转换接口
├── entity/              # 数据库实体
├── constant/            # 常量（interface 定义）
└── enums/               # 枚举
```

**lifecycle-api（`com.harness.lifecycle.api`）**

```
com.harness.lifecycle.api/
├── proto/               # protobuf 生成的 gRPC 类（自动生成）
└── dto/                 # gRPC 请求/响应 DTO
```

**api-common（`com.harness.api.common`）**

```
com.harness.api.common/
├── vo/                  # 跨服务共享 VO
├── util/                # 工具类（JsonUtil 等）
├── enums/               # 全局枚举（错误码等）
└── constant/            # 全局常量
```

### 1.4 util 包约束

- **只允许**在 `api-common` 的 `com.harness.api.common.util` 包下放置工具类
- 所有引用 `api-common` 的模块使用 `compileOnly`，不打入运行时
- **禁止**在 util 中通过 `ApplicationContext.getBean()` 或 `@Autowired static` 获取 Spring Bean
- 需要 Spring 能力的封装为 starter，放 `biz-common` 或 `auth-common`

---

## 二、代码质量约束（Gradle 插件）

### 2.1 插件配置

| 插件 | 职责 | 触发时机 |
|------|------|---------|
| Checkstyle | 代码风格（Google Java Style） | `gradle check` |
| SpotBugs | 静态分析，潜在 bug | `gradle check` |
| OWASP Dependency Check | 依赖安全漏洞扫描 | `gradle check` |
| ArchUnit | 架构约束（测试用例形式） | `gradle test` |

三者全部绑定到 `check` task，`gradle build` 强制通过，不可跳过。

### 2.2 ArchUnit 约束范围

- `@Transactional` 只能出现在 Service 层
- `util` 包只能在 `api-common` 模块
- 禁止裸调用 `Class.forName()`、`Method.invoke()`
- Controller 不能直接调用 Mapper
- starter `config` 包只能通过 SPI 注入

### 2.3 Claude Hook 兜底检查（warn，不阻断）

| 检查项 | 时机 |
|--------|------|
| 敏感字段（password/token）直接打印到日志 | PreToolUse Edit/Write |
| 循环体内出现 `log.` 调用 | PreToolUse Edit/Write |
| `@Transactional` 方法内调用 `RestTemplate`/`FeignClient` | PreToolUse Edit/Write |
| 新增错误码枚举后 i18n 资源文件未同步 | PostToolUse |

---

## 三、代码风格规范

### 3.1 基础风格

- 遵循 **Google Java Style Guide** 作为默认风格标准
- 由 Checkstyle 插件强制执行，构建失败即阻断

### 3.2 方法规范

- 每个方法不超过 **30 行**（Checkstyle `MethodLength` 规则强制）
- 方法职责单一，便于 mock 和单元测试

### 3.3 编码习惯

- 多用 `Optional`，禁止裸 `null` 返回
- 多用 Stream API，减少命令式循环
- 禁止 `System.out.println`，统一使用 SLF4J

### 3.4 Lombok 使用规范

- 全局启用 Lombok，scope 为 `compileOnly` + `annotationProcessor`
- Entity / VO / DTO 使用 `@Data` 或 `@Getter` + `@Setter`
- 构造器优先用 `@RequiredArgsConstructor`（配合 `final` 字段做依赖注入），禁止 `@AllArgsConstructor` 用于 Spring Bean
- 日志统一用 `@Slf4j`，禁止手动声明 `Logger`
- **禁止**在 `@Data` 类中重写 `equals`/`hashCode`（Lombok 已生成，手写会冲突）
- 对象赋值统一使用 `@Accessors(chain = true)` 链式调用，**禁止**使用 `@Builder`

---

## 四、Javadoc 规范

| 位置 | 要求 |
|------|------|
| Entity / VO 类 | 类级别 + 每个字段 |
| Controller 方法 | 方法 + 参数 + 返回值 |
| Service 方法 | 方法 + 参数 + 返回值 |
| gRPC 接口方法 | 方法 + 参数 + 返回值 |
| 工具类 / 配置类 | 类级别即可 |

smart-doc 依赖 Javadoc 生成接口文档，与此规范天然配合，无需额外 Swagger 注解。

---

## 五、响应与校验规范

### 5.1 统一响应

- 响应包装类 `R<T>`，含 `code`、`data`、`msg` 三个字段
- `ResponseBodyAdvice` 自动包装，Controller 直接返回业务对象
- `smart-doc.json` 配置 `responseBodyAdvice`，文档正确识别响应结构

### 5.2 入参校验

- 所有入参必须加 JSR-303 注解（`@NotNull`、`@NotBlank` 等）做基础校验
- Service 层显式做业务规则校验，抛出自定义 `BizException`

### 5.3 错误码与国际化

- 错误码枚举放 `api-common` 的 `enums` 包，code 与 message key 一一对应
- message 通过 i18n 资源文件解析，支持多语言
- i18n 资源文件放 `biz-common`

---

## 六、异常处理规范

- `@RestControllerAdvice` 全局异常处理，统一返回 `R<T>` 格式
- 自定义 `BizException` 携带错误码枚举
- **禁止**空 catch 吞掉异常（SpotBugs 检测）
- **禁止**在事务方法内 try-catch 吞掉异常（会导致事务不回滚）

---

## 七、日志规范

- 使用 SLF4J + Logback，**禁止** `System.out.println`
- **禁止**在循环内打日志
- 敏感字段（手机号、密码、token）必须脱敏后才能打印

---

## 八、事务规范

- `@Transactional` 只加在 Service 层
- 仅加在 insert / update / delete 方法，以及需要长连接的查询方法
- **禁止**在 Controller 层加 `@Transactional`
- **禁止**在事务方法内调用远程接口（RestTemplate / FeignClient）

---

## 九、DAO 层规范

### 9.1 查询原则

- 默认使用单表查询，优先用 MyBatis-Plus 内置方法
- 联表查询仅限"分页 + 条件 + 联表"三者同时出现的复杂场景
- 联表查询写自定义 Mapper XML，不用 `@Select` 内联 SQL

### 9.2 API 使用

- 单表操作使用 `IService` / `LambdaQueryWrapper`，**禁止**魔法字符串字段名
- 多用 `Optional` 处理查询结果
- 多用 Stream API 处理集合

### 9.3 Native 兼容

- 所有 Mapper 接口加 `@Mapper` 注解
- 配合 `@RegisterReflectionForBinding` 注册 Entity/VO，支持 GraalVM AOT

---

## 十、JSON 规范

### 10.1 ObjectMapper 配置

单例 `ObjectMapper` 注册为 Spring Bean，统一配置：

| 配置项 | 值 |
|--------|-----|
| null 字段 | 不序列化输出（`NON_NULL`） |
| 空集合 | 不输出（`NON_EMPTY`） |
| 日期格式 | `yyyy-MM-dd` / `yyyy-MM-dd HH:mm:ss`，禁用时间戳 |
| 循环引用 | 开启检测，防止嵌套死循环 |
| 未知字段 | 忽略，兼容接口扩展 |

### 10.2 JsonUtil

- 位置：`api-common` 的 `com.harness.api.common.util.JsonUtil`
- 除 Controller 层外，所有业务 JSON 转换必须通过 `JsonUtil`
- 提供静态方法：

| 方法 | 说明 |
|------|------|
| `object2Json(Object)` | 对象转 JSON 字符串 |
| `json2Object(String, Class<T>)` | JSON 转对象 |
| `json2List(String, Class<T>)` | JSON 转 List |
| `json2Object(String, TypeReference<T>)` | 支持复杂泛型 |

---

## 十一、Starter 规范

### 11.1 目录结构

```
{starter-name}/
├── config/       # Java 配置类，只允许 SPI 注入
├── provider/     # 对外提供的能力类
└── property/     # @ConfigurationProperties + @RefreshScope 热更新
```

### 11.2 约束

- `config` 包只允许通过 SPI（`AutoConfiguration.imports`）注入，禁止 `@ComponentScan`
- `property` 配合 `spring-configuration-metadata` 生成 IDE 配置提示
- 所有第三方框架集成尽最大可能封装为 starter，biz 层只依赖 provider 接口

### 11.3 归属

| starter 类型 | 所在模块 |
|-------------|---------|
| 通用基础能力（Redis、MQ、DB 等） | `biz-common` |
| 认证鉴权相关 | `auth-common` |

---

## 十二、依赖管理规范

- **技术栈：** JDK 25 + Spring Boot 4.0.x + GraalVM 25，虚拟线程暂不启用
- Spring Boot 4.0.x 最新稳定版
- 版本统一用 Gradle Version Catalog（`gradle/libs.versions.toml`）管理
- 所有第三方包使用 stable 版本，**禁止** SNAPSHOT / RC / Beta
- `build.gradle` 中显式 `resolutionStrategy` 解决所有版本冲突，构建不得有 warning

### 12.1 Scope 最小必要原则

按最小必要原则设置依赖 scope，不得扩大：

| Scope | 使用场景 |
|-------|---------|
| `implementation` | 运行时需要，不对外暴露 API |
| `api` | 运行时需要，且需要传递给依赖方（谨慎使用） |
| `compileOnly` | 仅编译期需要，运行时由容器或其他模块提供（如 `api-common`、Lombok、注解处理器） |
| `runtimeOnly` | 仅运行时需要，编译期不可见（如 JDBC 驱动） |
| `testImplementation` | 仅测试代码使用 |
| `annotationProcessor` | 编译期注解处理器（MapStruct、Lombok） |

**典型约定：**
- `api-common` 模块：所有引用方使用 `compileOnly`
- Lombok：`compileOnly` + `annotationProcessor`
- MapStruct：`compileOnly` + `annotationProcessor`
- 测试框架（JUnit、Mockito）：`testImplementation`
- 数据库驱动：`runtimeOnly`

---

## 十三、安全规范

- 所有对外接口必须经过 auth-common 鉴权，禁止裸暴露
- **禁止**在代码中硬编码密码、token、密钥，使用配置中心或环境变量
- gRPC 使用 spring-grpc 框架 + 自定义 TLS 证书，禁止明文传输

---

## 十四、构建与部署

- 镜像构建使用 Jib Gradle 插件，无需本地 Docker daemon
- Native 部署：慎用反射、动态代理、类路径扫描；反射必须在 `reflect-config.json` 显式注册

---

## 十五、测试规范

- Controller、Service、自定义 Mapper 方法必须有单元测试
- 框架：JUnit 5 + Mockito + MockMvc
- 方法不超过 30 行，保证可 mock 性

---

## 十六、接口文档规范

- 使用 smart-doc，编译期静态分析，无需运行时注解
- 导出格式：Postman Collection / Torna 推送
- 依赖 Javadoc 注释生成，与第四节规范配合

---

## 十七、对象映射规范

| 规范 | 说明 |
|------|------|
| **禁止** | `BeanUtils.copyProperties`、`BeanCopier` 等反射复制 |
| **强制** | 使用 MapStruct 做对象映射，编译期生成代码，Native 友好 |
| 位置 | MapStruct Mapper 接口放 `converter` 包，与 MyBatis Mapper 包区分 |

---

## 十八、配置管理规范

- 配置文件格式统一使用 `.properties`，不用 `.yml`
- 配置读取统一用 `@ConfigurationProperties`，**禁止**裸 `@Value`
- 环境隔离：`application.properties`（公共默认值）+ `application-{env}.properties`（环境差异）
- 敏感配置（密码、token、密钥）统一放 Apollo，不落本地文件
- Apollo namespace 按 env 区分（dev / test / prod）
- 配置类加 `@RefreshScope`，支持 Apollo 热刷新
- Apollo starter 封装在 `biz-common`，业务模块直接引用

---

## 十九、并发规范

- **禁止** `new Thread()`，统一使用 Spring `@Async` 或线程池 Bean
- 线程池配置封装为 starter，放 `biz-common`
- 共享可变状态必须加注释说明并发安全策略
- 虚拟线程（`spring.threads.virtual.enabled=true`）暂不启用，待评估后按需开启

---

## 二十、数据库规范

### 20.1 字段命名

- 表字段命名使用下划线（snake_case），Java 字段使用驼峰，MyBatis-Plus 自动映射
- 所有业务表必须包含以下基础字段，由 MyBatis-Plus 自动填充：

| 字段 | 类型 | 说明 |
|------|------|------|
| `create_time` | datetime | 创建时间，自动填充 |
| `update_time` | datetime | 更新时间，自动填充 |
| `deleted_time` | datetime | 逻辑删除时间，默认值 `0001-01-01 00:00:00` |

### 20.2 逻辑删除

- 统一使用 `deleted_time` 标记逻辑删除，**禁止**物理删除业务数据
- 未删除默认值：`0001-01-01 00:00:00`（非 null，支持唯一索引）
- 已删除：`deleted_time = 实际删除时间`，支持多次删除记录

---

## 二十一、接口版本规范

### 21.1 HTTP 接口版本

使用 Spring Framework 7 / Spring Boot 4 原生 API Versioning：

```java
// 在 Controller 或方法上声明版本
@HttpExchange(version = "1.0")
@RestController
public class LifecycleController { ... }
```

- 版本声明在 Controller 类级别，同一 Controller 内方法共享版本
- 不兼容变更必须新建版本 Controller，旧版本保留至少一个迭代周期

### 21.2 gRPC 接口版本

- proto 文件必须声明 package 版本：

```protobuf
package com.harness.lifecycle.v1;
```

- 不兼容变更新建 `v2` package，旧版本 proto 保留至少一个迭代周期
