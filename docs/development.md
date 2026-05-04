# 开发指南

面向开发者的完整指南，涵盖从环境准备到构建镜像的全流程。

---

## 目录

- [环境准备](#环境准备)
- [代码结构](#代码结构)
- [核心模块说明](#核心模块说明)
- [本地开发运行](#本地开发运行)
- [构建与镜像打包](#构建与镜像打包)
- [代码质量门禁](#代码质量门禁)
- [测试](#测试)

---

## 环境准备

- **JDK 25**（`build.gradle` 中 `toolchain` 指定为 25，`release` 为 21）
- **Gradle**（推荐通过 Wrapper `./gradlew`）
- **Docker Desktop**（用于启动 MySQL、Apollo 等依赖服务）
- **IDE**：IntelliJ IDEA（推荐启用 Checkstyle 插件，加载 `config/checkstyle/checkstyle.xml`）

---

## 代码结构

```
lifecycle/
├── harness-api/
│   └── lifecycle-api/                  # API 接口与 DTO 定义
│       └── src/main/java/com/harness/lifecycle/...
├── harness-biz/
│   └── lifecycle-biz/                  # 业务主模块（Spring Boot 应用入口）
│       ├── src/main/java/
│       │   └── com/harness/lifecycle/
│       │       ├── Application.java            # 应用入口
│       │       ├── auth/
│       │       │   ├── controller/             # Web 控制器
│       │       │   ├── service/                # 业务服务
│       │       │   ├── entity/                 # 数据实体
│       │       │   └── mapper/                 # MyBatis Mapper
│       │       ├── oauth/
│       │       │   ├── TokenStore.java         # 内存 Token 管理
│       │       │   └── TokenRequest.java       # Token 请求 DTO
│       │       └── config/
│       │           └── MybatisConfig.java      # MyBatis 配置
│       ├── src/main/resources/
│       │   ├── application.properties          # 本地兜底配置
│       │   └── schema.sql                      # 数据库初始化
│       └── src/test/
│           └── java/com/harness/lifecycle/
│               ├── ArchitectureTest.java       # ArchUnit 架构守护
│               ├── auth/controller/
│               │   └── WechatLoginControllerTest.java
│               └── auth/service/
│                   └── AuthServiceTest.java
├── harness-common/
│   ├── api-common/                     # 通用工具（无 Spring 依赖）
│   │   └── src/main/java/com/harness/api/common/util/JsonUtil.java
│   ├── auth-common/                    # GitLab API 客户端 Starter
│   │   ├── src/main/java/com/harness/auth/gitlab/
│   │   │   ├── GitLabApi.java                  # HTTP 接口定义
│   │   │   ├── GitLabAutoConfiguration.java    # 自动配置
│   │   │   ├── GitLabProperties.java           # 配置属性
│   │   │   ├── PasswordGenerator.java          # 强密码生成器
│   │   │   └── dto/                            # DTO 包
│   │   └── src/main/resources/META-INF/spring/
│   │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── biz-common/                     # 业务公共模块
├── config/                             # 质量门禁配置
│   ├── checkstyle/checkstyle.xml
│   ├── pmd/ruleset.xml
│   └── spotbugs/exclude.xml
├── build.gradle                        # 根构建脚本
├── gradle/
│   └── libs.versions.toml              # 版本目录
└── settings.gradle                     # 模块声明
```

---

## 核心模块说明

### auth-common — GitLab API Starter

独立的 Spring Boot Starter 模块，封装 GitLab REST API 调用，设计目标为**可复用**。

| 组件 | 职责 |
|------|------|
| `GitLabApi` | HTTP 接口（`@HttpExchange`），声明用户查询、创建、 impersonation token 生成 |
| `GitLabAutoConfiguration` | 自动配置 `GitLabApi` Bean（基于 `RestClient` + `HttpServiceProxyFactory`） |
| `GitLabProperties` | 配置属性前缀 `gitlab.url`、`gitlab.admin-token` |
| `PasswordGenerator` | 16 位随机强密码生成（大写/小写/数字/符号各至少 1 位） |

使用方式（在其他 Spring Boot 项目中）：

```gradle
implementation project(':harness-common:auth-common')
```

```yaml
gitlab:
  url: http://gitlab.harness.ai
  admin-token: glpat-xxxxx
```

### lifecycle-biz — 业务主模块

| 包 | 职责 |
|----|------|
| `auth.controller` | Web 控制器，处理 OAuth2 授权请求、Mock 微信登录页 |
| `auth.service` | 业务逻辑：GitLab 用户自动创建/绑定、 impersonation token 生成 |
| `auth.entity` | MyBatis-Plus 实体 `WechatGitlabBinding` |
| `auth.mapper` | 数据访问层 |
| `oauth` | 内存 Token 存储（pending/code/access_token 三级映射） |
| `config` | MyBatis 配置（`SqlSessionFactory`、`SqlSessionTemplate`） |

核心表结构：

```sql
CREATE TABLE wechat_gitlab_binding (
  openid           VARCHAR(64)  NOT NULL PRIMARY KEY,
  gitlab_user_id   BIGINT       NOT NULL,
  initial_password VARCHAR(128) NOT NULL DEFAULT '',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 本地开发运行

### 1. 启动依赖服务

```bash
cd deploy
docker compose up -d mysql apollo
```

### 2. 配置本地环境

编辑 `lifecycle/harness-biz/lifecycle-biz/src/main/resources/application.properties`（通常无需修改，已包含本地兜底配置）：

```properties
server.port=8081

# Apollo 配置中心（本地开发可选）
app.id=lifecycle-biz
apollo.meta=http://localhost:8080
apollo.bootstrap.enabled=true

# 本地 MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/harness?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=changeme123
```

### 3. 运行应用

```bash
cd lifecycle
./gradlew :harness-biz:lifecycle-biz:bootRun
```

或直接运行 `Application.java`。

### 4. 测试 OAuth2 流程

浏览器访问：

```
http://localhost:8081/mock/wechat/authorize?redirect_uri=http://localhost:8081/oauth/wechat/callback&state=test
```

这是一个 Mock 微信授权页，输入任意 openid 即可模拟授权回调。

---

## 构建与镜像打包

### 编译与测试

```bash
cd lifecycle
./gradlew build
```

执行内容包括：编译、单元测试、Checkstyle、PMD、OWASP Dependency-Check。

### 构建 Docker 镜像（Jib）

```bash
./gradlew :harness-biz:lifecycle-biz:jibDockerBuild
```

Jib 配置（`lifecycle-biz/build.gradle`）：

| 配置项 | 值 |
|--------|-----|
| 基础镜像 | `eclipse-temurin:25-jre` |
| 目标镜像 | `registry.harness.com/lifecycle-biz:latest` |
| 暴露端口 | `8081` |
| JVM 参数 | `-Xms256m -Xmx512m` |
| 平台 | `linux/amd64` |

> 注意：`jib.dockerClient` 中配置了 Docker 主机路径，在 macOS OrbStack 环境下为 `unix://~/.orbstack/run/docker.sock`，其他环境请按需调整。

---

## 代码质量门禁

所有子项目统一继承根 `build.gradle` 中的质量配置：

| 工具 | 版本 | 作用 | 触发任务 | 失败策略 |
|------|------|------|----------|----------|
| Checkstyle | 10.20.1 | 代码风格检查 | `checkstyleMain` | `maxWarnings = 0` |
| PMD | 7.24.0 | 静态代码分析 | `pmdMain` | 控制台输出 |
| OWASP Dependency-Check | 10.0.4 | 依赖漏洞扫描 | `dependencyCheckAnalyze` | CVSS >= 7 失败 |
| ArchUnit | 1.3.0 | 架构规则守护 | `test` | 测试失败 |

```bash
# 单独运行某项检查
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew dependencyCheckAnalyze
```

---

## 测试

### 单元测试

```bash
./gradlew test
```

### 架构守护测试

`ArchitectureTest.java` 使用 ArchUnit 定义以下规则：

| 规则 | 说明 |
|------|------|
| Controller 不直接调用 Mapper | 必须通过 Service 层 |
| `@Transactional` 仅在 Service 层 | 防止事务边界混乱 |
| 禁止 `Class.forName` / `Method.invoke` | 防止反射滥用 |
| Util 类仅存在于 `api-common` | 统一工具入口 |
| `@ComponentScan` 仅通过 SPI | 禁止显式组件扫描 |
| 注解与包名强制对应 | `@RestController`/`@Service`/`@Mapper`/`enum`/`Constant` 必须在规定包内 |

---

## 版本目录

依赖版本集中管理于 `gradle/libs.versions.toml`：

| 依赖 | 版本 |
|------|------|
| Spring Boot | 4.0.0 |
| gRPC | 1.68.0 |
| Spring gRPC | 0.8.0 |
| MyBatis-Plus | 3.5.16 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.38 |
| ArchUnit | 1.3.0 |
| Jackson | 2.18.2 |
| JUnit | 5.11.4 |
| Mockito | 5.14.2 |
| Spring Authorization Server | 1.5.1 |
| Jib | 3.4.4 |
| Spring Cloud Config | 5.0.0 |
