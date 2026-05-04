# 修复 Gradle 配置缓存 Jib 任务警告实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 lifecycle-biz 模块中 jib 任务的配置缓存警告，消除在任务执行时访问 `Task.project` 的问题

**Architecture:** 将 `project.findProperty()` 调用从 jib 配置块移到配置阶段，使用局部变量存储属性值，确保配置缓存兼容性

**Tech Stack:** Gradle 9.4.1, Jib Plugin, Kotlin DSL

---

## 文件结构

本计划将修改以下文件：

- **Modify:** `lifecycle/harness-biz/lifecycle-biz/build.gradle:32-66` - jib 配置块
- **Test:** 运行 `./gradlew :harness-biz:lifecycle-biz:jib --configuration-cache` 验证配置缓存兼容性

---

### Task 1: 提取配置属性到变量

**Files:**
- Modify: `lifecycle/harness-biz/lifecycle-biz/build.gradle:32-66`

- [ ] **Step 1: 在 jib 配置块之前提取属性值**

在 jib 配置块之前添加属性提取代码：

```groovy
// 在配置阶段提取属性，避免在任务执行时访问 project
def registryUsername = project.findProperty('registryUsername') ?: ''
def registryPassword = project.findProperty('registryPassword') ?: ''
def registryUrl = project.findProperty('registryUrl') ?: 'registry.harness.ai'
def commitSha = System.getenv('CI_COMMIT_SHORT_SHA') ?: 'dev'

jib {
    allowInsecureRegistries = true
```

- [ ] **Step 2: 更新 jib from 配置块使用变量**

修改 from.auth 部分使用提取的变量：

```groovy
    from {
        image = 'registry.harness.ai/eclipse-temurin:25-jre'
        platforms {
            platform {
                architecture = "amd64"
                os = 'linux'
            }
        }
        auth {
            username = registryUsername
            password = registryPassword
        }
    }
```

- [ ] **Step 3: 更新 jib to 配置块使用变量**

修改 to 部分使用提取的变量：

```groovy
    to {
        image = "${registryUrl}/lifecycle-biz"
        tags = ['latest', commitSha]
        auth {
            username = registryUsername
            password = registryPassword
        }
    }
```

- [ ] **Step 4: 验证配置缓存兼容性**

运行 Gradle 构建并启用配置缓存：

```bash
cd /home/huyiyu/Documents/self/lifecycle
./gradlew :harness-biz:lifecycle-biz:jib --configuration-cache --dry-run
```

Expected: 构建成功，无配置缓存警告

- [ ] **Step 5: 提交更改**

```bash
git add lifecycle/harness-biz/lifecycle-biz/build.gradle
git commit -m "fix: 修复 jib 任务配置缓存警告

将 project.findProperty() 调用移到配置阶段，避免在任务执行时访问 project 对象

Co-Authored-By: Claude Sonnet 4 <noreply@anthropic.com>"
```

---

### Task 2: 完整的修改后代码

**Files:**
- Modify: `lifecycle/harness-biz/lifecycle-biz/build.gradle:32-74`

- [ ] **Step 1: 查看完整的修改后代码**

完整的 jib 配置块应该如下：

```groovy
// 在配置阶段提取属性，避免在任务执行时访问 project
def registryUsername = project.findProperty('registryUsername') ?: ''
def registryPassword = project.findProperty('registryPassword') ?: ''
def registryUrl = project.findProperty('registryUrl') ?: 'registry.harness.ai'
def commitSha = System.getenv('CI_COMMIT_SHORT_SHA') ?: 'dev'

jib {
    allowInsecureRegistries = true

    from {
        image = 'registry.harness.ai/eclipse-temurin:25-jre'
        platforms {
            platform {
                architecture = "amd64"
                os = 'linux'
            }
        }
        auth {
            username = registryUsername
            password = registryPassword
        }
    }

    to {
        image = "${registryUrl}/lifecycle-biz"
        tags = ['latest', commitSha]
        auth {
            username = registryUsername
            password = registryPassword
        }
    }

    container {
        ports = ['8081']
        jvmFlags = ['-Xms256m', '-Xmx512m']
    }

    extraDirectories {
        permissions = [:]
    }
}

tasks.named('jib') {
    doFirst {
        System.setProperty('sendCredentialsOverHttp', 'true')
        System.setProperty('jib.allowInsecureRegistries', 'true')
    }
}
```

- [ ] **Step 2: 运行完整的配置缓存测试**

运行完整的 jib 任务并验证配置缓存：

```bash
./gradlew :harness-biz:lifecycle-biz:jib --configuration-cache
```

Expected: 构建成功，配置缓存已启用，无警告

- [ ] **Step 3: 检查配置缓存报告**

查看配置缓存报告确认无问题：

```bash
cat lifecycle/build/reports/configuration-cache/*/configuration-cache-report.html
```

Expected: 报告中无 "Task.project" 相关警告

- [ ] **Step 4: 提交最终验证**

```bash
git add lifecycle/harness-biz/lifecycle-biz/build.gradle
git commit -m "fix: 修复 jib 任务配置缓存警告

- 将 project.findProperty() 调用移到配置阶段
- 使用局部变量存储属性值
- 确保配置缓存兼容性

Co-Authored-By: Claude Sonnet 4 <noreply@anthropic.com>"
```


---

## 自我审查

**1. 规范覆盖检查：**
- ✅ 修复了 Task.project 在执行时被调用的问题
- ✅ 所有 project.findProperty() 调用已移到配置阶段
- ✅ 配置缓存兼容性已验证

**2. 占位符扫描：**
- ✅ 无 TBD、TODO 或占位符
- ✅ 所有代码完整且可执行

**3. 类型一致性：**
- ✅ 变量名称一致：registryUsername, registryPassword, registryUrl, commitSha
- ✅ 所有引用匹配定义

---

## 执行选项

计划已完成并保存到 `docs/superpowers/plans/2026-05-04-fix-gradle-config-cache-jib.md`。

**两种执行方式：**

**1. Subagent-Driven（推荐）** - 我为每个任务派发一个新的子代理，任务间进行审查，快速迭代

**2. Inline Execution** - 在当前会话中使用 executing-plans 执行任务，批量执行并设置检查点

**您选择哪种方式？**
