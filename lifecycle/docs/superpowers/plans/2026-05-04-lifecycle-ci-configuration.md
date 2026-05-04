# Lifecycle GitLab CI 配置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 lifecycle 项目配置 GitLab CI/CD 流水线，实现代码质量检查、单元测试和容器镜像构建推送

**Architecture:** 创建 `.gitlab-ci.yml` 文件，定义 3 个阶段（validate、test、build），配置 Gradle 缓存和环境变量

**Tech Stack:** GitLab CI/CD, Gradle, Jib, Checkstyle, PMD, OWASP Dependency Check

---

## 文件结构

**创建：**
- `.gitlab-ci.yml` - GitLab CI 配置文件

**修改：**
- 无需修改现有文件（Jib 配置已完成）

---

### 任务 1: 创建基础 CI 配置文件

**Files:**
- Create: `.gitlab-ci.yml`

- [ ] **步骤 1: 创建 .gitlab-ci.yml 文件并定义阶段和全局变量**

```yaml
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
```

- [ ] **步骤 2: 验证文件创建**

Run: `cat .gitlab-ci.yml`
Expected: 文件内容显示正确

- [ ] **步骤 3: 提交基础配置**

```bash
git add .gitlab-ci.yml
git commit -m "ci: 添加 GitLab CI 基础配置

- 定义 3 个阶段：validate、test、build
- 配置 Git 策略和缓存
- 设置 Gradle 选项

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### 任务 2: 配置 Validate 阶段作业

**Files:**
- Modify: `.gitlab-ci.yml`

- [ ] **步骤 1: 添加 checkstyle 作业**

在 `.gitlab-ci.yml` 末尾添加：

```yaml
checkstyle:
  stage: validate
  image: eclipse-temurin:25-jdk
  script:
    - ./gradlew checkstyleMain checkstyleTest
  artifacts:
    reports:
      junit: build/reports/checkstyle/*.xml
    paths:
      - build/reports/checkstyle/
    expire_in: 30 days
    when: always
```

- [ ] **步骤 2: 添加 pmd 作业**

在 checkstyle 作业后添加：

```yaml
pmd:
  stage: validate
  image: eclipse-temurin:25-jdk
  script:
    - ./gradlew pmdMain pmdTest
  artifacts:
    paths:
      - build/reports/pmd/
    expire_in: 30 days
    when: always
```

- [ ] **步骤 3: 添加 dependency-check 作业**

在 pmd 作业后添加：

```yaml
dependency-check:
  stage: validate
  image: eclipse-temurin:25-jdk
  script:
    - ./gradlew dependencyCheckAnalyze
  artifacts:
    paths:
      - build/reports/dependency-check-report.html
    expire_in: 30 days
    when: always
  allow_failure: true
```

- [ ] **步骤 4: 验证配置**

Run: `cat .gitlab-ci.yml | grep -A 10 "checkstyle:"`
Expected: 显示 checkstyle 作业配置

- [ ] **步骤 5: 提交 validate 阶段配置**

```bash
git add .gitlab-ci.yml
git commit -m "ci: 添加 validate 阶段作业配置

- checkstyle: 代码风格检查
- pmd: 静态代码分析
- dependency-check: 依赖安全扫描

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### 任务 3: 配置 Test 阶段作业

**Files:**
- Modify: `.gitlab-ci.yml`

- [ ] **步骤 1: 添加 unit-test 作业**

在 dependency-check 作业后添加：

```yaml
unit-test:
  stage: test
  image: eclipse-temurin:25-jdk
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: build/test-results/test/*.xml
    paths:
      - build/reports/tests/test/
      - build/test-results/test/
    expire_in: 30 days
    when: always
```

- [ ] **步骤 2: 验证配置**

Run: `cat .gitlab-ci.yml | grep -A 12 "unit-test:"`
Expected: 显示 unit-test 作业配置

- [ ] **步骤 3: 提交 test 阶段配置**

```bash
git add .gitlab-ci.yml
git commit -m "ci: 添加 test 阶段作业配置

- unit-test: 运行单元测试并生成报告
- 配置 JUnit XML 报告自动解析

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### 任务 4: 配置 Build 阶段作业

**Files:**
- Modify: `.gitlab-ci.yml`

- [ ] **步骤 1: 添加 jib-build 作业**

在 unit-test 作业后添加：

```yaml
jib-build:
  stage: build
  image: eclipse-temurin:25-jdk
  script:
    - ./gradlew :harness-biz:lifecycle-biz:jib -PregistryUrl=$REGISTRY_URL -PregistryUsername=$REGISTRY_USERNAME -PregistryPassword=$REGISTRY_PASSWORD
  only:
    - main
    - tags
  except:
    - merge_requests
```

- [ ] **步骤 2: 验证配置**

Run: `cat .gitlab-ci.yml | grep -A 10 "jib-build:"`
Expected: 显示 jib-build 作业配置

- [ ] **步骤 3: 提交 build 阶段配置**

```bash
git add .gitlab-ci.yml
git commit -m "ci: 添加 build 阶段作业配置

- jib-build: 使用 Jib 构建并推送容器镜像
- 仅在 main 分支和标签推送时执行
- 通过环境变量传递仓库凭据

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### 任务 5: 测试和验证 CI 配置

**Files:**
- Modify: `.gitlab-ci.yml`

- [ ] **步骤 1: 验证 YAML 语法**

Run: `cat .gitlab-ci.yml`
Expected: 文件内容完整，无语法错误

- [ ] **步骤 2: 推送到远程仓库**

```bash
git push origin jib-registry-integration
```

- [ ] **步骤 3: 检查 GitLab Pipeline 状态**

访问 GitLab 项目页面，查看 Pipeline 是否自动触发。

Expected: Pipeline 开始运行，显示 validate 和 test 阶段

- [ ] **步骤 4: 验证作业执行**

在 GitLab Pipeline 页面检查：
- checkstyle 作业状态
- pmd 作业状态
- dependency-check 作业状态
- unit-test 作业状态

Expected: 所有作业成功执行或按预期失败

- [ ] **步骤 5: 创建说明文档**

在项目根目录创建 `CI-SETUP.md` 说明文件：

```markdown
# GitLab CI 配置说明

## 前置条件

在 GitLab 项目设置中配置以下 CI/CD Variables：

1. 进入项目 Settings → CI/CD → Variables
2. 添加以下变量：

| 变量名 | 值 | 类型 |
|-------|-----|------|
| REGISTRY_URL | registry.harness.ai | Variable |
| REGISTRY_USERNAME | admin | Variable |
| REGISTRY_PASSWORD | (仓库密码) | Variable (Masked) |

## Pipeline 说明

- **Merge Request**: 执行 validate + test 阶段
- **Main 分支**: 执行完整流程（validate + test + build）
- **标签推送**: 执行完整流程（validate + test + build）

## 镜像标签

- Main 分支: `latest` + `<commit-hash>`
- 标签: `<tag-name>`
```

- [ ] **步骤 6: 提交说明文档**

```bash
git add CI-SETUP.md
git commit -m "docs: 添加 CI 配置说明文档

- 说明前置条件和变量配置
- 描述 Pipeline 执行策略
- 列出镜像标签规则

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push origin jib-registry-integration
```

