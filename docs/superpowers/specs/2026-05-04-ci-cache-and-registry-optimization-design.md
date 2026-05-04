# CI 缓存和镜像优化设计

**日期：** 2026-05-04  
**作者：** Claude Sonnet 4  
**状态：** 已批准

## 概述

优化 GitLab CI 构建流程，解决两个核心问题：
1. Gradle 缓存失效（由于工作目录切换导致）
2. CI 镜像从私有 registry 拉取以提升速度

## 问题分析

### 问题 1：Gradle 缓存失效

**现状：**
- CI 配置中 `before_script: cd lifecycle` 切换到子目录
- 缓存路径配置为 `.gradle/wrapper` 和 `.gradle/caches`
- 实际 Gradle 使用默认的 `~/.gradle` 目录
- 导致缓存路径不匹配，每次构建都重新下载依赖

**影响：**
- 每次 CI 构建都需要下载 Gradle wrapper 和所有依赖
- 构建时间增加 2-5 分钟
- 浪费网络带宽和 CI 资源

### 问题 2：镜像拉取速度

**现状：**
- CI 使用公共镜像 `eclipse-temurin:25-jdk`
- 从 Docker Hub 拉取，速度较慢
- 依赖外部服务可用性

**影响：**
- 镜像拉取时间 30-60 秒
- 外部依赖可能不稳定

## 解决方案

### 方案选择

评估了三种方案后，选择**方案 A：调整缓存路径**

**理由：**
- 改动最小，只需修改 CI 配置
- 不影响本地开发环境
- 立即见效，无需额外工具

### 架构设计

#### 1. 缓存配置

**核心策略：**
- 仅在 CI 环境变量中设置 `GRADLE_USER_HOME`
- 调整缓存路径匹配实际的 Gradle 目录
- 本地开发不受影响

**配置：**
```yaml
variables:
  GRADLE_USER_HOME: "${CI_PROJECT_DIR}/lifecycle/.gradle"
  
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - lifecycle/.gradle/wrapper
    - lifecycle/.gradle/caches
```

**工作原理：**
1. CI 启动时，设置 `GRADLE_USER_HOME` 环境变量
2. Gradle 将所有数据（wrapper、依赖缓存）存储到 `lifecycle/.gradle`
3. GitLab CI 缓存这个目录
4. 后续构建复用缓存，跳过下载

**本地开发保护：**
- `GRADLE_USER_HOME` 仅在 .gitlab-ci.yml 中定义
- 本地开发者运行 `./gradlew` 时使用默认 `~/.gradle`
- 两个环境互不干扰

#### 2. 镜像管理

**实施步骤：**

**步骤 1：准备镜像（一次性）**
```bash
# 拉取公共镜像
docker pull eclipse-temurin:25-jdk

# 重新打 tag
docker tag eclipse-temurin:25-jdk registry.harness.ai/eclipse-temurin:25-jdk

# 推送到私有 registry
docker push registry.harness.ai/eclipse-temurin:25-jdk
```

**步骤 2：更新 CI 配置**
将所有任务的镜像从：
```yaml
image: eclipse-temurin:25-jdk
```
改为：
```yaml
image: registry.harness.ai/eclipse-temurin:25-jdk
```

**影响的任务：**
- checkstyle
- pmd
- dependency-check
- unit-test
- jib-build

**认证机制：**
- 依赖 GitLab CI runner 的全局配置
- Runner 已配置访问 registry.harness.ai 的认证
- 无需在 CI 中显式 docker login

## 实施细节

### CI 配置修改

**文件：** `.gitlab-ci.yml`

**修改内容：**

1. **全局变量部分**
```yaml
variables:
  GIT_STRATEGY: fetch
  GIT_DEPTH: 10
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  GRADLE_USER_HOME: "${CI_PROJECT_DIR}/lifecycle/.gradle"  # 新增
```

2. **缓存配置部分**
```yaml
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - lifecycle/.gradle/wrapper      # 修改
    - lifecycle/.gradle/caches       # 修改
```

3. **所有任务的镜像配置**
```yaml
# 修改前
image: eclipse-temurin:25-jdk

# 修改后
image: registry.harness.ai/eclipse-temurin:25-jdk
```

### 验证策略

#### 1. 镜像推送验证

**执行环境：** 本地或有权限的机器

**步骤：**
```bash
# 拉取并推送
docker pull eclipse-temurin:25-jdk
docker tag eclipse-temurin:25-jdk registry.harness.ai/eclipse-temurin:25-jdk
docker push registry.harness.ai/eclipse-temurin:25-jdk

# 验证
docker pull registry.harness.ai/eclipse-temurin:25-jdk
```

**成功标准：**
- 推送成功，无认证错误
- 可以成功拉取镜像

#### 2. CI 缓存验证

**步骤：**
1. 提交修改后的 .gitlab-ci.yml
2. 触发 CI 构建（第一次）
3. 观察构建日志，确认：
   - 使用了新的镜像路径
   - Gradle 下载了依赖到正确的目录
   - 缓存被保存
4. 再次触发 CI 构建（第二次）
5. 观察构建日志，确认：
   - 缓存被恢复
   - Gradle 跳过了依赖下载
   - 构建时间明显减少

**成功标准：**
- 第二次构建使用缓存
- 构建时间减少 30-50%
- 无缓存相关错误

### 预期效果

**首次构建（缓存为空）：**
- 与之前相同，需要下载所有依赖
- 镜像从私有 registry 拉取，速度提升 20-30%

**后续构建（缓存命中）：**
- Gradle 依赖缓存命中，跳过下载
- 镜像从私有 registry 拉取
- **总构建时间减少 30-50%**

**具体时间估算：**
- 镜像拉取：从 60s 减少到 20s（节省 40s）
- Gradle 依赖下载：从 120s 减少到 5s（节省 115s）
- 总节省时间：约 2-3 分钟/次构建

## 错误处理

### 可能的问题和解决方案

**问题 1：私有 registry 不可用**
- **现象：** CI 失败，镜像拉取错误
- **解决：** 检查 registry.harness.ai 服务状态
- **临时方案：** 回退到公共镜像

**问题 2：缓存损坏**
- **现象：** Gradle 构建失败，依赖解析错误
- **解决：** 在 GitLab CI 界面清除缓存，重新构建

**问题 3：Runner 认证失败**
- **现象：** 401 Unauthorized 错误
- **解决：** 检查 runner 的 registry 认证配置

## 维护和监控

### 日常维护

**镜像更新：**
- 当 eclipse-temurin 发布新版本时
- 重新拉取、打 tag、推送到私有 registry
- 更新 .gitlab-ci.yml 中的镜像版本

**缓存管理：**
- 定期检查缓存大小（通过 GitLab CI 界面）
- 如果缓存过大（>500MB），考虑清理旧版本

### 监控指标

**关键指标：**
1. **缓存命中率** - 目标 >80%
2. **平均构建时间** - 目标减少 30-50%
3. **镜像拉取时间** - 目标 <30s

**监控方法：**
- 查看 GitLab CI 构建日志
- 对比优化前后的构建时间
- 观察缓存恢复日志

## 风险和限制

### 风险

1. **私有 registry 单点故障**
   - 如果 registry.harness.ai 不可用，所有 CI 构建失败
   - 缓解：确保 registry 高可用，或保留公共镜像作为备份

2. **缓存大小增长**
   - Gradle 缓存可能随时间增长
   - 缓解：定期清理，或设置缓存过期策略

### 限制

1. **仅优化 CI 环境**
   - 本地开发不受影响（这是设计目标）
   - 本地开发者仍使用默认 Gradle 配置

2. **依赖 runner 配置**
   - 需要 runner 已配置 registry 认证
   - 新 runner 需要相同配置

## 总结

本设计通过两个简单的配置修改，显著优化了 CI 构建流程：

1. **调整 Gradle 缓存路径** - 解决缓存失效问题
2. **使用私有 registry 镜像** - 提升镜像拉取速度

**优点：**
- 改动最小，风险低
- 不影响本地开发
- 立即见效
- 预期节省 30-50% 构建时间

**下一步：**
- 编写详细的实施计划
- 执行镜像推送
- 更新 CI 配置
- 验证优化效果
