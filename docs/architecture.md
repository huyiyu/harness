# 架构与流程

深入理解 Harness 项目的设计决策、模块关系和数据流转。适合技术分享、代码审查和新成员 onboarding。

---

## 目录

- [模块依赖关系](#模块依赖关系)
- [微信 OAuth2 登录流程](#微信-oauth2-登录流程)
- [GitLab 用户自动绑定机制](#gitlab-用户自动绑定机制)
- [配置管理策略](#配置管理策略)
- [架构守护规则](#架构守护规则)

---

## 模块依赖关系

### Gradle 模块图

```
harness-parent
├── harness-common
│   ├── api-common               # 纯工具，零 Spring 依赖
│   ├── auth-common              # Spring Boot Starter（GitLab API 封装）
│   │   └── 依赖：api-common, spring-boot-starter-web
│   └── biz-common               # 业务公共模块
│       └── 依赖：spring-boot-starter-web
├── harness-api
│   └── lifecycle-api            # API / DTO 定义层
│       └── 依赖：api-common (compileOnly)
└── harness-biz
    └── lifecycle-biz            # Spring Boot 应用入口
        ├── 依赖：biz-common
        ├── 依赖：auth-common
        ├── compileOnly：api-common
        └── compileOnly：lifecycle-api
```

### 设计原则

| 原则 | 说明 |
|------|------|
| **api-common 零 Spring 依赖** | 仅包含 `JsonUtil` 等纯工具类，可被任何项目复用 |
| **auth-common 独立成 Starter** | 封装 GitLab API + 自动配置，遵循 Spring Boot 自动装配规范（`META-INF/spring/AutoConfiguration.imports`） |
| **lifecycle-api compileOnly** | API 模块仅定义 DTO，编译期可见，运行时由实现方提供，避免循环依赖 |
| **biz-common 轻量公共** | 业务层面的公共代码，保持最小化 |

---

## 微信 OAuth2 登录流程

当前实现为 **Mock 模式**，用于本地开发和演示。完整流程如下：

### 时序图

```
用户浏览器
    │
    │ ① GET /oauth/authorize?redirect_uri=...
    │    (GitLab 作为 OAuth Client 发起)
    ▼
Lifecycle —— oauth/authorize
    │
    │ ② 生成 pending state，重定向到 Mock 微信页
    ▼
用户浏览器
    │
    │ ③ GET /mock/wechat/authorize?redirect_uri=...
    │    (显示 Mock 微信授权页)
    ▼
用户输入 openid 并提交
    │
    │ ④ GET /oauth/wechat/callback?code=xxx&state=xxx
    ▼
Lifecycle —— oauth/wechat/callback
    │
    │ ⑤ 校验 state，生成 auth code，重定向回 GitLab callback
    ▼
用户浏览器
    │
    │ ⑥ GET /users/auth/oauth2_generic/callback?code=...
    │    (GitLab 收到 auth code)
    ▼
GitLab
    │
    │ ⑦ POST /oauth/token (GitLab → Lifecycle)
    │    grant_type=authorization_code&code=...
    ▼
Lifecycle —— oauth/token
    │
    │ ⑧ 校验 code，返回 access_token
    ▼
GitLab
    │
    │ ⑨ GET /api/v4/user (GitLab → Lifecycle)
    │    Authorization: Bearer <access_token>
    ▼
Lifecycle —— api/v4/user
    │
    │ ⑩ 查询/创建 GitLab 用户，返回用户 info
    ▼
GitLab
    │
    │ ⑪ 用户登录成功，跳转首页
    ▼
用户浏览器（已登录 GitLab）
```

### Token 流转

`TokenStore` 使用三个 ConcurrentHashMap 管理 OAuth2 状态：

| Map | Key | Value | 生命周期 |
|-----|-----|-------|----------|
| `pending` | UUID (state) | `redirect_uri\|original_state` | 一次授权流程 |
| `codes` | UUID (auth code) | openid | 一次 token 换取 |
| `tokens` | UUID (access token) | openid | 长期有效（当前实现） |

### Mock 微信授权页

`WechatLoginController.mockAuthorize()` 返回一个简单 HTML 表单：

- 自动填充随机 openid（`mock_` 前缀 + 随机字符串）
- 用户可手动修改 openid
- 提交后进入标准 OAuth2 callback 流程

> 生产环境应替换为真实微信网页授权接口（`https://open.weixin.qq.com/connect/oauth2/authorize`）。

---

## GitLab 用户自动绑定机制

### 首次登录

```
openid 首次出现
    │
    ├──→ 查询 wechat_gitlab_binding 表 → 无记录
    │
    ├──→ 查询 GitLab 是否已有 wechat_<openid> 用户
    │       ├── 无 → 调用 GitLab API 创建用户（随机强密码）
    │       └── 有 → 复用现有用户 ID
    │
    ├──→ 插入 wechat_gitlab_binding 记录
    │       (openid, gitlab_user_id, initial_password, created_at)
    │
    └──→ 生成 impersonation token → 用户直达 GitLab
```

### 重复登录

```
openid 再次出现
    │
    ├──→ 查询 wechat_gitlab_binding 表 → 命中记录
    │
    └──→ 直接复用 gitlab_user_id → 生成新 impersonation token
```

### 密码策略

`PasswordGenerator` 生成 16 位随机密码，强制包含：
- 至少 1 个大写字母
- 至少 1 个小写字母
- 至少 1 位数字
- 至少 1 个特殊符号
- 剩余位随机填充
- 最后整体打乱顺序

```java
// 示例密码
xK9#mP2$vL5nQ8wR
```

---

## 配置管理策略

采用 **Apollo 配置中心为主，本地 properties 兜底** 的双层策略。

### 优先级

```
Apollo 已发布配置  >  环境变量  >  application.properties 本地配置
```

### 配置分布

| 配置源 | 位置 | 用途 |
|--------|------|------|
| Apollo（生产/容器） | `apollo.meta=http://apollo:8080` | 动态配置，支持热更新 |
| 本地 properties（开发） | `application.properties` | 本地开发兜底 |
| Apollo Namespace | `deploy/config/*.properties` | 多环境配置模板 |

### 关键配置项

```properties
# application.properties（本地兜底）
server.port=8081

app.id=lifecycle-biz
apollo.meta=http://apollo:8080
apollo.bootstrap.enabled=true
apollo.bootstrap.namespaces=application

spring.datasource.url=jdbc:mysql://localhost:3306/harness?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=${MYSQL_ROOT_PASSWORD:changeme123}

# GitLab 连接
gitlab.url=http://gitlab.harnesss.com
gitlab.admin-token=${GITLAB_ADMIN_TOKEN:}
```

### 环境变量覆盖（Docker Compose）

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/harness?useSSL=false&allowPublicKeyRetrieval=true
  SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  GITLAB_URL: http://gitlab.harness.ai
  GITLAB_ADMIN_TOKEN: ${GITLAB_ADMIN_TOKEN}
```

Spring Boot 的 `Relaxed Binding` 会自动将环境变量映射到配置属性，例如 `GITLAB_URL` → `gitlab.url`。

---

## 架构守护规则

通过 **ArchUnit** 在测试阶段强制执行架构约束，防止代码腐化。

### 规则清单

```java
// 1. 分层隔离：Controller 不允许直接调用 Mapper
noClasses().that().resideInAPackage("..controller..")
    .should().dependOnClassesThat().resideInAPackage("..mapper..");

// 2. 事务边界：@Transactional 只能在 Service 层
noMethods().that().areDeclaredInClassesThat()
    .resideOutsideOfPackage("..service..")
    .should().beAnnotatedWith("Transactional");

// 3. 安全约束：禁止 Class.forName / Method.invoke
noClasses().should().callMethod(Class.class, "forName", String.class);
noClasses().should().callMethod(Method.class, "invoke", Object.class, Object[].class);

// 4. 工具类集中：Util 类只能放在 api-common
noClasses().that().resideInAPackage("..util..")
    .should().resideOutsideOfPackage("com.harness.api.common.util..");

// 5. 组件扫描规范：只能通过 SPI 自动装配，禁止显式 @ComponentScan
noClasses().that().resideInAPackage("..config..")
    .should().beAnnotatedWith("ComponentScan");

// 6. 包名与注解强制对应
@RestController → 必须在 ..controller.. 或 ..oauth.. 包
@Service → 必须在 ..service.. 或 ..oauth.. 包
@Mapper → 必须在 ..mapper.. 包
enum → 必须在 ..enums.. 包
*Constant 接口 → 必须在 ..constant.. 包
```

### 违反示例

```java
// ❌ 错误：Controller 直接注入 Mapper
@RestController
public class UserController {
    @Autowired
    private UserMapper mapper;  // ArchUnit 会报错
}

// ✅ 正确：Controller → Service → Mapper
@RestController
public class UserController {
    @Autowired
    private UserService service;
}
```

### 运行架构测试

```bash
cd lifecycle
./gradlew :harness-biz:lifecycle-biz:test --tests "*ArchitectureTest*"
```

---

## 扩展阅读

- [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [GitLab Impersonation Tokens](https://docs.gitlab.com/ee/api/users.html#create-an-impersonation-token)
- [Apollo 配置中心](https://www.apolloconfig.com/)
- [ArchUnit 用户指南](https://www.archunit.org/userguide/html/000_Index.html)
