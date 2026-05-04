# Lifecycle 项目 GitLab CI 配置设计

## 目标

为 lifecycle 项目配置 GitLab CI/CD 流水线，实现：
- 代码质量检查（Checkstyle、PMD、OWASP Dependency Check）
- 自动化单元测试
- 容器镜像构建和推送到私有仓库
- 根据分支/标签差异化执行策略

## 整体架构

### 阶段划分

流水线包含 3 个阶段：

1. **validate** - 代码质量检查
   - checkstyle：代码风格检查
   - pmd：静态代码分析
   - dependency-check：依赖安全扫描

2. **test** - 测试执行
   - unit-test：运行单元测试并收集报告

3. **build** - 镜像构建和推送
   - jib-build：使用 Jib 构建容器镜像并推送到私有仓库

### 执行策略

| 触发条件 | 执行阶段 | 说明 |
|---------|---------|------|
| Merge Request | validate + test | 仅质量检查和测试，不构建镜像 |
| main 分支推送 | validate + test + build | 完整流程，镜像标签：latest + commit hash |
| 标签推送 | validate + test + build | 完整流程，镜像标签：tag name |

## Validate 阶段详细配置

### checkstyle 作业

**目的**：检查代码风格是否符合规范

**配置**：
- 镜像：`eclipse-temurin:25-jdk`
- 命令：`./gradlew checkstyleMain checkstyleTest`
- 规则文件：`config/checkstyle/checkstyle.xml`
- 失败条件：maxWarnings = 0（任何警告都会导致失败）

**产物**：
- 报告路径：`build/reports/checkstyle/`
- 保留时间：30 天

### pmd 作业

**目的**：静态代码分析，检测潜在问题

**配置**：
- 镜像：`eclipse-temurin:25-jdk`
- 命令：`./gradlew pmdMain pmdTest`
- 规则文件：`config/pmd/ruleset.xml`
- 输出：控制台输出（consoleOutput = true）

**产物**：
- 报告路径：`build/reports/pmd/`
- 保留时间：30 天

### dependency-check 作业

**目的**：扫描依赖项的已知安全漏洞

**配置**：
- 镜像：`eclipse-temurin:25-jdk`
- 命令：`./gradlew dependencyCheckAnalyze`
- 失败条件：CVSS >= 7
- 抑制文件：`config/owasp-suppressions.xml`

**产物**：
- 报告路径：`build/reports/dependency-check-report.html`
- 保留时间：30 天

**注意事项**：
- 首次运行会下载 NVD 数据库，耗时较长
- 建议配置缓存以加速后续执行

## Test 阶段详细配置

### unit-test 作业

**目的**：执行所有单元测试并生成测试报告

**配置**：
- 镜像：`eclipse-temurin:25-jdk`
- 命令：`./gradlew test`
- 测试框架：JUnit 5

**产物**：
- 测试报告：`build/reports/tests/test/`（HTML 格式）
- JUnit XML：`build/test-results/test/`
- 保留时间：30 天

**报告展示**：
- GitLab 会自动解析 JUnit XML 并在 MR 中显示测试结果
- 失败的测试会在 Pipeline 页面高亮显示

## Build 阶段详细配置

### jib-build 作业

**目的**：使用 Jib 构建容器镜像并推送到私有仓库

**配置**：
- 镜像：`eclipse-temurin:25-jdk`
- 命令：`./gradlew :harness-biz:lifecycle-biz:jib`
- 目标仓库：`registry.harness.ai`
- 基础镜像：`registry.harness.ai/eclipse-temurin:25-jre`

**执行条件**：
- 仅在 main 分支或标签推送时执行
- MR 不执行此作业

**镜像标签策略**：

| 触发条件 | 标签 | 示例 |
|---------|------|------|
| main 分支推送 | `latest` + `<commit-hash>` | `latest`, `a1b2c3d` |
| 标签推送 | `<tag-name>` | `v1.0.0` |

**认证配置**：
- 使用 GitLab CI Variables 存储凭据
- 变量名：`REGISTRY_USERNAME`、`REGISTRY_PASSWORD`
- 通过 gradle.properties 传递给 Jib

## 缓存策略

### Gradle 依赖缓存

**目的**：加速构建，避免重复下载依赖

**配置**：
```yaml
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .gradle/wrapper
    - .gradle/caches
```

**说明**：
- 使用分支名作为缓存键
- 缓存 Gradle wrapper 和依赖
- 不同分支使用独立缓存

### OWASP NVD 数据库缓存

**目的**：避免每次都下载漏洞数据库

**配置**：
```yaml
cache:
  paths:
    - .gradle/dependency-check-data
```

**说明**：
- NVD 数据库较大（~200MB）
- 首次运行耗时 5-10 分钟
- 缓存后仅需增量更新

## 环境变量和凭据管理

### GitLab CI Variables

需要在 GitLab 项目设置中配置以下变量：

| 变量名 | 类型 | 说明 | 示例值 |
|-------|------|------|--------|
| `REGISTRY_URL` | Variable | 私有仓库地址 | `registry.harness.ai` |
| `REGISTRY_USERNAME` | Variable | 仓库用户名 | `admin` |
| `REGISTRY_PASSWORD` | Masked | 仓库密码 | `***` |

**安全配置**：
- `REGISTRY_PASSWORD` 必须设置为 Masked（掩码）
- 不要在日志中打印敏感信息
- 仅在 protected 分支上可用（可选）

### Gradle 属性传递

在 CI 作业中通过环境变量传递给 Gradle：

```bash
./gradlew jib \
  -PregistryUrl=$REGISTRY_URL \
  -PregistryUsername=$REGISTRY_USERNAME \
  -PregistryPassword=$REGISTRY_PASSWORD
```

## 代码检出配置

### Git 策略

**配置**：
```yaml
variables:
  GIT_STRATEGY: fetch
  GIT_DEPTH: 10
```

**说明**：
- `GIT_STRATEGY: fetch` - 增量拉取，比 clone 更快
- `GIT_DEPTH: 10` - 浅克隆，仅拉取最近 10 次提交
- 减少网络传输和磁盘占用

### 访问权限

**默认行为**：
- GitLab CI 使用项目的 Deploy Token 或 CI/CD Token
- 无需额外配置 Access Token
- Runner 自动获得项目读取权限

**私有依赖**（如需要）：
- 如果项目依赖其他私有仓库，需配置 SSH Key 或 Access Token
- 当前项目无此需求

## 网络配置

### HTTP 代理

**问题**：
- CI Runner 可能需要通过代理访问外部资源（Maven Central、Docker Hub 等）
- 私有仓库应绕过代理直接访问

**解决方案**：
```yaml
variables:
  GRADLE_OPTS: >-
    -Dhttp.proxyHost=127.0.0.1
    -Dhttp.proxyPort=7897
    -Dhttps.proxyHost=127.0.0.1
    -Dhttps.proxyPort=7897
    -Dhttp.nonProxyHosts=registry.harness.ai|localhost|127.0.0.1
```

**说明**：
- 根据实际 Runner 环境调整代理配置
- `nonProxyHosts` 确保私有仓库不走代理
- 如果 Runner 在内网，可能不需要代理

## 实施注意事项

### 前置条件

1. **基础镜像准备**
   - ✅ `registry.harness.ai/eclipse-temurin:25-jre` 已推送到私有仓库
   - 确保 CI Runner 可以访问私有仓库

2. **GitLab CI Variables 配置**
   - 在项目设置中添加 `REGISTRY_URL`、`REGISTRY_USERNAME`、`REGISTRY_PASSWORD`
   - 确保 `REGISTRY_PASSWORD` 设置为 Masked

3. **Runner 配置**
   - 确认 Runner 有足够的磁盘空间（至少 10GB）
   - 确认 Runner 可以拉取 Docker 镜像
   - 根据需要配置网络代理

### 实施步骤

1. **创建 `.gitlab-ci.yml` 文件**
   - 定义 3 个阶段：validate、test、build
   - 配置各个作业的镜像、脚本、产物

2. **配置 GitLab CI Variables**
   - 在项目 Settings → CI/CD → Variables 中添加凭据

3. **测试流水线**
   - 创建测试分支，推送触发 MR Pipeline
   - 验证 validate 和 test 阶段正常执行
   - 合并到 main 分支，验证 build 阶段正常执行

4. **验证镜像**
   - 检查私有仓库中是否有新镜像
   - 验证镜像标签是否正确

### 潜在问题和解决方案

1. **首次运行 dependency-check 耗时长**
   - 原因：需要下载 NVD 数据库
   - 解决：配置缓存，后续运行仅需增量更新

2. **Jib 构建失败**
   - 检查 GitLab CI Variables 是否正确配置
   - 检查私有仓库是否可访问
   - 检查基础镜像是否存在

3. **网络超时**
   - 调整 Gradle 超时配置
   - 检查代理配置是否正确
   - 考虑使用本地 Maven 镜像

## 设计优势

1. **清晰的阶段划分**
   - 质量检查、测试、构建分离
   - 失败快速反馈，节省资源

2. **差异化执行策略**
   - MR 仅执行检查和测试，快速反馈
   - main 分支和标签才构建镜像，避免浪费

3. **完善的缓存策略**
   - Gradle 依赖缓存加速构建
   - NVD 数据库缓存避免重复下载

4. **安全的凭据管理**
   - 使用 GitLab CI Variables 存储敏感信息
   - Masked 变量防止泄露

5. **灵活的镜像标签**
   - main 分支：latest + commit hash
   - 标签：使用标签名
   - 便于版本追踪和回滚

## 后续优化方向

1. **并行执行**
   - validate 阶段的 3 个作业可以并行执行
   - 进一步缩短流水线时间

2. **增量构建**
   - 使用 Gradle Build Cache
   - 仅构建变更的模块

3. **镜像扫描**
   - 集成 Trivy 或 Clair 扫描镜像漏洞
   - 在 build 阶段后添加 scan 阶段

4. **部署自动化**
   - 添加 deploy 阶段
   - 自动部署到测试/生产环境

