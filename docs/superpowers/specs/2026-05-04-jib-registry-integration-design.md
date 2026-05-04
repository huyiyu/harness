# Jib 构建与私有镜像仓库集成设计

## 概述

配置 Jib 插件构建 Spring Boot 应用容器镜像，并推送到本地私有 Docker Registry (`registry.harness.ai`)，使用强密码认证，支持多标签推送。

## 目标

1. 修正 Jib 配置中的 registry 地址（从 `registry.harness.com` 改为 `registry.harness.ai`）
2. 为 registry 的 admin 用户生成 32 位强密码
3. 配置 Jib 使用 gradle.properties 中的认证信息
4. 支持同时推送 `latest` 和 git short hash 两个标签

## 架构设计

### 组件关系

```
lifecycle/gradle.properties (认证配置)
    ↓
lifecycle-biz/build.gradle (Jib 配置)
    ↓
Jib Plugin (构建镜像)
    ↓
registry.harness.ai (私有镜像仓库)
    ↑
deploy/registry-auth/htpasswd (认证文件)
```

### 认证流程

1. 使用 pwgen 生成 32 位强密码
2. 使用 htpasswd 工具生成 bcrypt hash
3. 更新 `deploy/registry-auth/htpasswd` 文件
4. 将明文密码保存到 `lifecycle/gradle.properties`
5. Jib 从 gradle.properties 读取认证信息并连接 registry

## 详细设计

### 1. 密码生成与配置

**生成密码**：
```bash
pwgen -s 32 1
```

**更新 htpasswd**：
```bash
htpasswd -Bbn admin <生成的密码> > deploy/registry-auth/htpasswd
```

**配置 gradle.properties**：
在 `lifecycle/gradle.properties` 中添加：
```properties
registryUrl=registry.harness.ai
registryUsername=admin
registryPassword=<生成的32位密码>
```

### 2. Jib 配置修改

修改 `lifecycle/harness-biz/lifecycle-biz/build.gradle` 中的 jib 配置块：

```gradle
jib {
    from {
        image = 'eclipse-temurin:25-jre'
        platforms {
            platform {
                architecture = "amd64"
                os = 'linux'
            }
        }
    }
    
    to {
        image = "${project.findProperty('registryUrl')}/lifecycle-biz"
        tags = ['latest', 'git rev-parse --short HEAD'.execute().text.trim()]
        auth {
            username = project.findProperty('registryUsername')
            password = project.findProperty('registryPassword')
        }
    }
    
    container {
        ports = ['8081']
        jvmFlags = ['-Xms256m', '-Xmx512m']
    }
}
```

### 3. 镜像标签策略

每次构建推送两个标签：
- `registry.harness.ai/lifecycle-biz:latest` - 始终指向最新构建
- `registry.harness.ai/lifecycle-biz:<git-short-hash>` - 对应具体 commit

Git short hash 通过 `git rev-parse --short HEAD` 命令获取（7位）。

### 4. 构建流程

**构建命令**：
```bash
cd lifecycle
./gradlew :harness-biz:lifecycle-biz:jib
```

**执行步骤**：
1. Gradle 读取 `gradle.properties` 中的配置
2. Jib 执行 git 命令获取当前 commit hash
3. Jib 构建镜像层（无需本地 Docker daemon）
4. 使用 bcrypt 认证连接到 registry.harness.ai
5. 推送 latest 和 hash 标签到 registry

## 文件修改清单

| 文件路径 | 修改内容 |
|---------|---------|
| `lifecycle/gradle.properties` | 新增 registryUrl、registryUsername、registryPassword |
| `lifecycle/harness-biz/lifecycle-biz/build.gradle` | 修改 jib.to 配置，添加认证和标签 |
| `deploy/registry-auth/htpasswd` | 更新 admin 用户的密码 hash |
| `lifecycle/.gitignore` | 确保 gradle.properties 被排除（如果尚未排除） |

## 安全考虑

1. **gradle.properties 不提交到 git**：通过 .gitignore 排除，避免密码泄露
2. **使用 bcrypt hash**：htpasswd 使用 -B 参数生成 bcrypt hash，安全性高
3. **32 位强密码**：使用 pwgen -s 生成安全随机密码
4. **临时方案**：当前设计为临时方案，后续可迁移到环境变量或密钥管理系统

## 验证方式

**验证镜像推送成功**：
```bash
curl -u admin:<password> https://registry.harness.ai/v2/lifecycle-biz/tags/list
```

**验证双标签存在**：
返回的 JSON 应包含 `latest` 和对应的 git hash。

## 后续改进建议

1. 将认证信息迁移到环境变量或 CI/CD 密钥管理
2. 考虑使用 Gradle Credentials Plugin 管理敏感信息
3. 添加镜像签名验证
4. 配置镜像扫描（如 Trivy）集成到构建流程
