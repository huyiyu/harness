# 部署文档更新设计

**日期：** 2026-05-05  
**作者：** Claude Sonnet 4  
**状态：** 待批准

## 概述

更新 `docs/deployment.md` 以反映 CI 缓存和镜像优化迭代的成果，补充镜像初始化推送流程和 CI 优化配置说明，为运维人员提供完整的部署和优化指南。

## 目标

1. **补充镜像初始化流程** - 记录首次部署时需要推送到私有 registry 的完整镜像清单和操作步骤
2. **添加 CI 优化说明** - 解释 Gradle 缓存和私有镜像加速的配置原理
3. **提供验证指南** - 说明如何验证优化效果和监控关键指标

## 设计方案

### 方案选择：运维流程导向

采用按操作流程组织的文档结构，符合运维人员的实际使用场景：
- 首次部署时的一次性准备工作
- 日常使用和维护操作
- 优化配置的原理和验证

**理由：**
- 清晰区分一次性操作和日常操作
- CI 优化作为独立主题便于查找
- 不破坏现有文档结构

### 文档结构变更

#### 1. Registry 使用章节重组

**现有结构：**
```
## Registry 使用
- 登录
- 推送镜像
- 拉取镜像
- 重新生成密码
```

**更新后结构：**
```
## Registry 使用

### 镜像准备（首次部署）
- 必需镜像清单
- 批量推送脚本
- 推送验证

### 日常使用
- 登录
- 推送镜像
- 拉取镜像

### 镜像更新维护
- 更新时机
- 更新步骤
- 影响范围
```

#### 2. 新增"CI 优化配置"章节

在"故障排查"章节之前插入新章节：

```
## CI 优化配置

### 优化概述
- Gradle 缓存加速
- 私有镜像加速
- 预期效果

### Gradle 缓存机制
- GRADLE_USER_HOME 配置
- 缓存路径说明
- 本地开发不受影响

### 私有镜像配置
- 镜像加速原理
- Runner 网络配置
- 认证机制

### 验证优化效果
- 镜像推送验证
- CI 缓存验证
- 关键指标
- 监控方法
```

## 详细设计

### 镜像清单

完整的基础设施镜像清单（8个镜像）：

| 镜像用途 | 源镜像 | 私有 Registry 路径 | 使用位置 |
|---------|--------|-------------------|---------|
| CI 构建镜像 | eclipse-temurin:25-jdk | registry.harness.ai/eclipse-temurin:25-jdk | .gitlab-ci.yml (构建阶段) |
| 业务运行镜像 | eclipse-temurin:25-jre | registry.harness.ai/eclipse-temurin:25-jre | Jib 构建的基础镜像 |
| 反向代理 | nginx:alpine | registry.harness.ai/nginx:alpine | docker-compose.yml (nginx) |
| GitLab CE | gitlab/gitlab-ce:latest | registry.harness.ai/gitlab-ce:latest | docker-compose.yml (gitlab) |
| GitLab Runner | gitlab/gitlab-runner:latest | registry.harness.ai/gitlab-runner:latest | docker-compose.yml (runner) |
| 数据库 | mysql:8.0 | registry.harness.ai/mysql:8.0 | docker-compose.yml (mysql) |
| 配置中心 | nobodyiam/apollo-quick-start:latest | registry.harness.ai/apollo-quick-start:latest | docker-compose.yml (apollo) |
| 镜像仓库 | registry:2 | registry.harness.ai/registry:2 | docker-compose.yml (registry) |

**设计要点：**
- JDK 用于 CI 构建任务
- JRE 用于 Jib 打包业务应用的基础镜像（体积更小）
- 所有基础设施镜像都需要预先推送

### 批量推送脚本

**文件位置：** `deploy/push-images.sh`

**功能：**
- 检查 Docker 登录状态
- 循环处理镜像清单
- 拉取 → 打标签 → 推送
- 显示推送进度（当前 X/8）
- 支持断点续传（跳过已推送的镜像）
- 错误处理（网络失败、认证失败）
- 输出推送结果摘要

**使用方式：**
```bash
cd deploy
./push-images.sh
```

### CI 优化配置说明

#### Gradle 缓存机制

**配置内容：**
```yaml
variables:
  GRADLE_USER_HOME: "${CI_PROJECT_DIR}/lifecycle/.gradle"
  
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - lifecycle/.gradle/wrapper
    - lifecycle/.gradle/caches
```

**说明要点：**
- `GRADLE_USER_HOME` 仅在 CI 环境生效
- 将 Gradle 数据目录重定向到项目内
- 缓存路径与实际使用路径匹配
- 本地开发仍使用 `~/.gradle`，互不干扰

#### 私有镜像配置

**配置内容：**
所有 CI 任务使用私有 registry 镜像：
```yaml
image: registry.harness.ai/eclipse-temurin:25-jdk
```

**说明要点：**
- 避免 Docker Hub 限流和网络延迟
- Runner 通过 Docker 网络直接访问 registry
- 依赖 Runner 的全局认证配置
- 镜像拉取时间从 60s 降至 <30s

### 验证和监控

#### 镜像推送验证

**步骤：**
1. 推送完成后，尝试拉取每个镜像
2. 对比镜像 digest 确认完整性
3. 检查 registry UI 或 API

**成功标准：**
- 所有镜像都能成功拉取
- digest 与源镜像一致

#### CI 缓存验证

**步骤：**
1. 提交代码触发 CI（第一次构建）
2. 观察构建日志，确认依赖下载
3. 再次触发 CI（第二次构建）
4. 确认日志中出现 "Extracting cache"
5. 对比两次构建时间

**成功标准：**
- 第二次构建使用缓存
- 构建时间减少 30-50%
- 无缓存相关错误

#### 关键指标

| 指标 | 优化前 | 目标 | 监控方法 |
|------|--------|------|----------|
| 缓存命中率 | 0% | >80% | CI 日志关键字搜索 |
| 镜像拉取时间 | 60s | <30s | CI 日志时间戳对比 |
| 总构建时间 | 基准值 | 减少 30-50% | GitLab CI 构建历史 |

#### 监控方法

**日志关键字：**
- 缓存恢复：`Extracting cache`
- 缓存保存：`Creating cache`
- 镜像拉取：`Pulling from registry.harness.ai`

**趋势分析：**
- 记录每次构建时间
- 绘制时间趋势图
- 识别缓存失效的构建

## 实施细节

### 文档修改范围

**文件：** `docs/deployment.md`

**修改内容：**
1. 重组 "Registry 使用" 章节（约 310-344 行）
2. 在 "故障排查" 章节前插入 "CI 优化配置" 章节（约 390 行前）
3. 更新目录索引

**新增文件：**
- `deploy/push-images.sh` - 镜像批量推送脚本

### 内容编写原则

1. **面向运维人员** - 使用运维视角的语言，避免过多开发细节
2. **操作优先** - 先说怎么做，再解释为什么
3. **可验证性** - 每个步骤都有明确的成功标准
4. **错误处理** - 预见常见问题并提供解决方案
5. **保持简洁** - 避免冗长的理论说明

### 与现有文档的关系

**引用关系：**
- CI 优化配置章节引用 `2026-05-04-ci-cache-and-registry-optimization-design.md` 作为详细设计参考
- 故障排查章节新增 CI 优化相关的问题条目

**一致性：**
- 镜像清单与 `.gitlab-ci.yml` 和 `docker-compose.yml` 保持同步
- 配置说明与实际配置文件一致

## 预期效果

### 对运维人员

1. **首次部署更顺畅** - 清晰的镜像准备流程，避免遗漏
2. **理解优化原理** - 知道为什么这样配置，便于故障排查
3. **可验证的结果** - 明确的指标和验证方法

### 对文档质量

1. **完整性** - 覆盖从部署到优化的完整流程
2. **可维护性** - 结构清晰，便于后续更新
3. **实用性** - 提供可执行的脚本和命令

## 风险和限制

### 风险

1. **镜像清单过时**
   - 如果 CI 配置或 docker-compose.yml 更新，镜像清单需要同步更新
   - 缓解：在文档中说明镜像清单的来源，便于后续维护

2. **脚本兼容性**
   - push-images.sh 可能在不同环境下表现不同
   - 缓解：使用标准 bash 语法，添加环境检查

### 限制

1. **仅覆盖当前架构**
   - 文档基于当前的 Docker Compose 部署方式
   - 如果迁移到 Kubernetes 等其他平台，需要重新编写

2. **监控方法手动**
   - 当前提供的监控方法需要手动查看日志
   - 未来可以集成自动化监控工具

## 总结

本设计通过重组和扩展部署文档，为运维人员提供完整的镜像准备和 CI 优化指南：

**核心改进：**
1. 补充镜像初始化流程和批量推送脚本
2. 新增 CI 优化配置章节，解释原理和验证方法
3. 保持运维流程导向的文档结构

**优点：**
- 文档完整性提升，覆盖首次部署到优化验证的全流程
- 结构清晰，便于查找和维护
- 提供可执行的脚本和明确的验证标准

**下一步：**
- 编写详细的实施计划
- 更新 deployment.md
- 创建 push-images.sh 脚本
- 验证文档的可操作性
