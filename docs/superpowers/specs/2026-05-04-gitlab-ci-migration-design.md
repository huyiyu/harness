# GitLab CI 迁移到根目录设计

## 概述

将 lifecycle 项目的 GitLab CI 配置从 worktree 迁移到项目根目录，实现集中化管理，确保 CI pipeline 在 GitLab 上正常运行。

## 目标

1. 将 `.gitlab-ci.yml` 从 worktree 迁移到项目根目录
2. 使用全局 `before_script` 切换工作目录到 lifecycle
3. 保持所有现有 CI 功能正常运行
4. 不创建额外的 CLAUDE.md 或 superpowers 配置

## 当前状态

**源文件位置：**
`.worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml`

**现有 CI 功能：**
- **验证阶段 (validate)**：checkstyle、PMD、dependency-check
- **测试阶段 (test)**：单元测试
- **构建阶段 (build)**：Jib 镜像构建并推送到私有 registry

**当前配置特点：**
- 配置文件位于 lifecycle 目录内
- 使用相对路径 `./gradlew` 执行构建
- 缓存 Gradle wrapper 和依赖

## 设计方案

### 架构变更

```
迁移前：
.worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml
    ↓ (在 lifecycle 目录内执行)
lifecycle/gradlew

迁移后：
.gitlab-ci.yml (根目录)
    ↓ before_script: cd lifecycle
lifecycle/gradlew
```

### 配置调整

**核心变更：添加全局 before_script**

在 `.gitlab-ci.yml` 文件顶部（`stages` 之前）添加：

```yaml
before_script:
  - cd lifecycle
```

**保持不变的配置：**
- 所有 stages 定义：`validate`, `test`, `build`
- 所有 job 配置和脚本命令
- 缓存配置：key 为 `${CI_COMMIT_REF_SLUG}`，paths 保持相对路径
- 环境变量：`GRADLE_OPTS`, `GIT_STRATEGY`, `GIT_DEPTH`
- Artifact 路径：相对于 lifecycle 目录的路径

**路径说明：**
- `./gradlew` 命令保持不变（因为已 cd 到 lifecycle）
- Artifact paths 如 `build/reports/` 保持相对路径
- 缓存路径如 `.gradle/wrapper` 保持相对路径

### 完整配置结构

```yaml
before_script:
  - cd lifecycle

stages:
  - validate
  - test
  - build

variables:
  GIT_STRATEGY: fetch
  GIT_DEPTH: 10
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .gradle/wrapper
    - .gradle/caches
    - .gradle/dependency-check-data

# ... 其余 job 配置保持不变
```

## 迁移步骤

1. **复制文件**
   ```bash
   cp .worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml ./.gitlab-ci.yml
   ```

2. **添加 before_script**
   在文件顶部（`stages` 之前）添加：
   ```yaml
   before_script:
     - cd lifecycle
   ```

3. **提交到 git**
   ```bash
   git add .gitlab-ci.yml
   git commit -m "Migrate GitLab CI to root directory"
   ```

## 验证方法

### 语法验证

**本地验证（如果安装了 gitlab-ci-lint）：**
```bash
gitlab-ci-lint .gitlab-ci.yml
```

**在线验证：**
在 GitLab 项目页面使用 CI/CD > Editor 的 "Validate" 功能

### 功能验证

**推送后检查：**
1. 推送到 GitLab 触发 pipeline
2. 观察每个 job 的执行日志
3. 确认 `before_script` 正确执行 `cd lifecycle`
4. 确认所有 job 成功完成

**关键检查点：**
- checkstyle job 能找到配置文件
- PMD job 能找到配置文件
- 单元测试能正确执行
- Jib 构建能找到 gradle.properties 并成功推送镜像

## 回滚方案

如果迁移后出现问题：

**方案 1：Git 回滚**
```bash
git revert <commit-hash>
git push
```

**方案 2：临时禁用**
```bash
git rm .gitlab-ci.yml
git commit -m "Temporarily disable root CI"
git push
```

## 后续考虑

1. **多项目支持**：如果未来添加其他子项目（如 deploy），可以：
   - 使用 `rules` 或 `only/except` 为不同项目定义不同的 pipeline
   - 或为每个子项目创建独立的 `.gitlab-ci.yml`

2. **缓存优化**：如果添加多个子项目，考虑为不同项目使用不同的 cache key：
   ```yaml
   cache:
     key: lifecycle-${CI_COMMIT_REF_SLUG}
   ```

3. **工作目录策略**：如果某些 job 需要在根目录执行，可以在特定 job 中覆盖 `before_script`：
   ```yaml
   deploy-job:
     before_script:
       - echo "Running in root directory"
   ```
