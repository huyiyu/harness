# GitLab Runner DNS 解析问题修复设计

## 概述

修复 GitLab Runner 创建的 CI 作业容器无法解析 `gitlab.harness.ai` 域名的问题，使用 Docker 网络别名和网络模式配置实现静态、可靠的域名解析方案。

## 问题分析

### 错误现象

```
fatal: unable to access 'http://gitlab.harness.ai/harness/harness-all.git/': 
Could not resolve host: gitlab.harness.ai
```

### 根本原因

1. **宿主机配置**：
   - `/etc/hosts` 中有 `127.0.0.1 gitlab.harness.ai` 映射
   - 宿主机可以正常访问 GitLab

2. **容器网络隔离**：
   - Docker 容器不继承宿主机的 `/etc/hosts` 配置
   - 容器内的 `127.0.0.1` 指向容器自身，而非宿主机

3. **CI 作业容器特殊性**：
   - GitLab Runner 通过挂载 `/var/run/docker.sock` 创建 CI 作业容器
   - 这些容器是**兄弟容器**（sibling），不是子容器
   - 默认情况下可能不在 `devops` 网络中
   - 即使在同一网络，也无法解析 `gitlab.harness.ai` 域名

### 为什么不能使用 extra_hosts

常见的 `extra_hosts` 方案存在问题：

```yaml
# ❌ 错误方案
extra_hosts:
  - "gitlab.harness.ai:127.0.0.1"
```

**问题**：容器内的 `127.0.0.1` 指向容器自身，无法访问宿主机或其他容器。

```yaml
# ❌ 错误方案
extra_hosts:
  - "gitlab.harness.ai:172.18.0.3"  # gitlab 容器 IP
```

**问题**：Docker 容器 IP 是动态分配的，重启后会变化，不可靠。

## 解决方案

### 核心策略

使用 **Docker 网络别名（Network Aliases）+ 网络模式配置** 实现静态域名解析。

### 方案架构

```
CI 作业容器（devops 网络）
    ↓ DNS 查询 gitlab.harness.ai
Docker 内部 DNS
    ↓ 解析网络别名
nginx 容器（devops 网络）
    ↓ HTTP 代理
gitlab 容器（devops 网络）
```

### 技术原理

1. **网络别名**：Docker 允许为服务添加网络别名，内部 DNS 会自动解析
2. **动态 IP 管理**：Docker 自动管理容器 IP，别名始终指向正确的容器
3. **网络隔离**：只有同一网络中的容器才能解析网络别名
4. **代理转发**：nginx 作为入口，代理请求到 gitlab 容器

## 详细设计

### 1. 添加网络别名

修改 `deploy/docker-compose.yml`，为 nginx 服务添加网络别名：

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    networks:
      devops:
        aliases:
          - gitlab.harness.ai
          - registry.harness.ai
    depends_on:
      - gitlab
```

**说明**：
- 在 `devops` 网络中添加别名 `gitlab.harness.ai` 和 `registry.harness.ai`
- Docker 内部 DNS 会将这些域名解析到 nginx 容器的 IP
- IP 由 Docker 动态管理，无需硬编码

### 2. 配置 Runner 网络模式

修改 `deploy/register-runner.sh`，在注册时指定网络模式：

```bash
docker exec gitlab-runner gitlab-runner register \
  --non-interactive \
  --url "${GITLAB_INTERNAL_URL}" \
  --token "${RUNNER_TOKEN}" \
  --executor docker \
  --docker-image alpine:latest \
  --docker-network-mode "devops" \
  --description "local-docker-runner" \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock
```

**关键变更**：
- 添加 `--docker-network-mode "devops"` 参数
- 确保所有 CI 作业容器都创建在 `devops` 网络中

**效果**：
- CI 作业容器可以访问 `devops` 网络中的所有服务
- 可以通过网络别名解析域名
- 可以直接访问 gitlab、mysql、registry 等服务

### 3. 副作用：修复其他服务

添加网络别名后，`lifecycle` 等其他服务也能自动解析域名：

```yaml
# lifecycle 服务配置（无需修改）
lifecycle:
  environment:
    GITLAB_URL: http://gitlab.harness.ai  # 自动解析
```

**原因**：lifecycle 已经在 `devops` 网络中，网络别名对所有网络成员生效。

## 实施步骤

### 前置条件

- Docker Compose 环境已部署
- GitLab 容器正常运行
- 有权限修改 docker-compose.yml 和注册脚本

### 步骤 1：修改 docker-compose.yml

在 nginx 服务的 networks 配置中添加别名：

```yaml
networks:
  devops:
    aliases:
      - gitlab.harness.ai
      - registry.harness.ai
```

### 步骤 2：重启 nginx 容器

```bash
cd deploy
docker-compose up -d nginx
```

**验证**：在 devops 网络中的任意容器内测试：
```bash
docker exec lifecycle ping -c 1 gitlab.harness.ai
```

### 步骤 3：修改 register-runner.sh

在 `gitlab-runner register` 命令中添加 `--docker-network-mode "devops"` 参数。

### 步骤 4：重新注册 Runner

如果 Runner 已注册，需要先注销：

```bash
docker exec gitlab-runner gitlab-runner unregister --all-runners
```

然后重新注册：

```bash
cd deploy
./register-runner.sh
```

### 步骤 5：验证配置

检查 Runner 配置：

```bash
docker exec gitlab-runner cat /etc/gitlab-runner/config.toml
```

确认包含：
```toml
[runners.docker]
  network_mode = "devops"
```

## 验证方法

### 1. 网络别名验证

在 devops 网络中的容器内测试：

```bash
# 测试 DNS 解析
docker exec lifecycle nslookup gitlab.harness.ai

# 测试 HTTP 访问
docker exec lifecycle curl -I http://gitlab.harness.ai
```

**预期结果**：能够解析域名并返回 HTTP 响应。

### 2. CI 作业验证

触发一个 CI pipeline，观察作业日志：

```bash
# 在 CI 作业中应该能成功 clone
git clone http://gitlab.harness.ai/harness/harness-all.git
```

**预期结果**：git clone 成功，无 DNS 解析错误。

### 3. Runner 配置验证

```bash
docker exec gitlab-runner gitlab-runner verify
```

**预期结果**：Runner 状态为 alive。

## 注意事项

### 1. 网络别名作用域

- 网络别名只在指定的 Docker 网络内生效
- 不在 `devops` 网络中的容器无法解析这些别名
- 宿主机也无法通过别名访问（仍需 /etc/hosts）

### 2. Runner 重新注册

- 修改注册脚本后，需要重新注册 Runner 才能生效
- 注销 Runner 会删除现有配置，确保已备份重要信息
- 重新注册需要 GitLab 的 Runner token

### 3. 端口映射

- nginx 容器映射了宿主机的 80 端口
- 容器间通信不经过宿主机端口，直接通过 Docker 网络
- CI 作业访问 `http://gitlab.harness.ai` 会直接到达 nginx 容器的 80 端口

### 4. 其他域名

如果还有其他 `*.harness.ai` 域名需要解析，也添加到网络别名中：

```yaml
networks:
  devops:
    aliases:
      - gitlab.harness.ai
      - registry.harness.ai
      - lifecycle.harness.ai
      - apollo.harness.ai
```

## 优势

1. **静态配置**：无需硬编码 IP，Docker 自动管理
2. **可维护性**：配置集中在 docker-compose.yml 和注册脚本中
3. **可复现性**：基础设施即代码，易于重建环境
4. **扩展性**：添加新域名只需修改别名列表
5. **性能**：容器间直接通信，无需经过宿主机网络栈

## 后续改进

1. **自动化注册**：将 Runner 注册集成到 docker-compose 启动流程
2. **健康检查**：添加 DNS 解析健康检查脚本
3. **文档完善**：更新部署文档，说明网络配置
4. **监控告警**：监控 Runner 状态和 CI 作业成功率
