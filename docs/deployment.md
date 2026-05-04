# 部署与运维指南

面向运维人员和开发者的 Docker Compose 部署手册，涵盖环境搭建、服务配置、日常运维及故障排查。

---

## 目录

- [服务架构总览](#服务架构总览)
- [前置条件](#前置条件)
- [一键部署](#一键部署)
- [验证访问](#验证访问)
- [各服务详细说明](#各服务详细说明)
- [数据持久化](#数据持久化)
- [Registry 使用](#registry-使用)
- [停止与清理](#停止与清理)
- [配置说明](#配置说明)
- [故障排查](#故障排查)

---

## 服务架构总览

```
                    ┌─────────────┐
                    │   Nginx     │  :80 (统一入口)
                    │  (反向代理)  │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────┴────┐      ┌─────┴─────┐     ┌─────┴─────┐
    │ GitLab  │      │ Lifecycle │     │ Registry  │
    │  :8080  │      │  :8081    │     │  :5000    │
    └────┬────┘      └───────────┘     └───────────┘
         │
    ┌────┴────┐
    │ Runner  │  (Docker executor)
    └─────────┘

    ┌─────────┐     ┌─────────┐     ┌─────────┐
    │  MySQL  │     │  Apollo │     │  Nginx  │
    │  :3306  │     │8070/8080│     │   :80   │
    └─────────┘     │  :8090  │     └─────────┘
                    └─────────┘
```

| 服务 | 镜像 | 内部端口 | 外部映射 | 说明 |
|------|------|----------|----------|------|
| Nginx | `nginx:alpine` | 80 | 80 | 反向代理，按域名路由 |
| GitLab CE | `gitlab/gitlab-ce:latest` | 80 / 22 | 8080 / 22222 | 代码托管 & CI/CD |
| GitLab Runner | `gitlab/gitlab-runner:latest` | - | - | CI/CD 执行器 |
| MySQL | `mysql:8.0` | 3306 | 3306 | Lifecycle + Apollo 共享 |
| Lifecycle | `lifecycle-biz:latest` | 8081 | 8081 | 业务服务 |
| Apollo | `nobodyiam/apollo-quick-start:latest` | 8070/8080/8090 | 同左 | 配置中心 |
| Registry | `registry:2` | 5000 | 5000 | Docker 私有镜像仓库 |

---

## 前置条件

### 1. 硬件资源

- **Docker Desktop**（建议分配 **8GB+ 内存**，GitLab CE 占用较大）
- **磁盘空间**：至少 20GB 可用空间

### 2. 端口占用检查

以下端口必须未被占用：

| 端口 | 服务 |
|------|------|
| 80 | Nginx |
| 3306 | MySQL |
| 8070 | Apollo Portal |
| 8080 | Apollo Config/Admin |
| 8081 | Lifecycle |
| 8090 | Apollo Eureka |
| 22222 | GitLab SSH |
| 5000 | Registry |

```bash
# 检查端口占用（Linux/macOS）
for port in 80 3306 8070 8080 8081 8090 22222 5000; do
  echo "Port $port: $(lsof -Pi :$port -sTCP:LISTEN 2>/dev/null | wc -l) processes"
done
```

### 3. Hosts 配置

以下域名需指向 `127.0.0.1`：

```
127.0.0.1  gitlab.harness.ai
127.0.0.1  lifecycle.harness.ai
127.0.0.1  registry.harness.ai
127.0.0.1  apollo.harness.ai
```

**Linux/macOS**：编辑 `/etc/hosts`
**Windows**：编辑 `C:\Windows\System32\drivers\etc\hosts`

> **注意**：hosts 配置仅对宿主机生效。Docker 容器内的 DNS 解析通过 Docker 网络别名实现，详见下文。

### 4. Docker 权限配置（Linux）

Linux 用户需要将当前用户添加到 docker 组以执行 Docker 命令：

```bash
sudo usermod -aG docker $USER
```

然后重新登录或执行：

```bash
newgrp docker
```

验证权限：

```bash
docker ps
```

---

## 一键部署

### 1. 准备环境变量

```bash
cd deploy
# 复制模板并编辑
cp .env.example .env  # 如果存在模板，否则直接创建
```

编辑 `.env` 文件：

```bash
# 数据目录
GITLAB_HOME=./gitlab
RUNNER_HOME=./runner

# GitLab 配置
GITLAB_PORT=80
GITLAB_SSH_PORT=22222
GITLAB_ROOT_PASSWORD=changeme123          # root 登录密码

# 数据库
MYSQL_ROOT_PASSWORD=changeme123

# GitLab Admin Token（用于 API 调用，创建 Runner 等）
GITLAB_ADMIN_TOKEN=glpat-xxxxx

# Lifecycle 镜像
LIFECYCLE_IMAGE=lifecycle-biz:latest
```

### 2. 启动所有服务

```bash
docker compose up -d
```

首次启动会拉取镜像并初始化数据，耗时约 5-10 分钟。

### 3. 注册 GitLab Runner

等待 GitLab 完全就绪（约 3-5 分钟后）：

```bash
./register-runner.sh
```

该脚本自动完成：
1. 轮询等待 GitLab 就绪
2. 通过 Rails console 创建 root 用户的 Personal Access Token
3. 调用 GitLab API 创建 Runner
4. 执行 `gitlab-runner register` 完成注册，**配置 CI 作业容器使用 `deploy_devops` 网络**

注册成功后，在 GitLab Admin → CI/CD → Runners 中可见。

> **重要**：Runner 注册时会配置 `--docker-network-mode "deploy_devops"`，确保 CI 作业容器能够通过 Docker 网络别名解析 `gitlab.harness.ai` 等域名。

---

## 验证访问

| 服务 | URL | 默认账号 | 首次访问说明 |
|------|-----|----------|--------------|
| GitLab | http://gitlab.harness.ai | `root` / `.env` 中 `GITLAB_ROOT_PASSWORD` | 首次启动需等待初始化 |
| Lifecycle | http://lifecycle.harness.ai:8081 | - | 健康检查：`/actuator/health` |
| Apollo Portal | http://apollo.harness.ai:8070 | `apollo` / `admin` | 需先配置应用和 Namespace |
| Registry | http://registry.harness.ai | `admin` / `admin` | Basic Auth 认证 |

### GitLab 首次登录

1. 访问 http://gitlab.harness.ai
2. 用户名：`root`
3. 密码：`.env` 中配置的 `GITLAB_ROOT_PASSWORD`

### Apollo 首次配置

1. 访问 Portal：http://apollo.harness.ai:8070
2. 登录后创建应用：AppId = `lifecycle-biz`
3. 创建 Namespace：`application`
4. 添加配置项（如 `gitlab.url`、`gitlab.admin-token`）
5. 发布配置

---

## 各服务详细说明

### Nginx

**网络别名配置**：Nginx 服务配置了 Docker 网络别名，使容器间可以通过域名访问：

```yaml
networks:
  devops:
    aliases:
      - gitlab.harness.ai
      - registry.harness.ai
```

这样，`deploy_devops` 网络中的所有容器（包括 CI 作业容器）都可以通过这些域名访问 Nginx，Nginx 再代理到后端服务。

当前配置（`nginx/nginx.conf`）仅启用了 GitLab 和 Registry 反向代理：

```nginx
server {
    listen 80;
    server_name gitlab.harness.ai;
    location / {
        proxy_pass http://gitlab:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

如需扩展，取消对应 `server` 块的注释即可。

### GitLab CE

Omnibus 配置通过环境变量注入：

```yaml
environment:
  GITLAB_OMNIBUS_CONFIG: |
    external_url 'http://gitlab.harness.ai'
    nginx['listen_port'] = 80
    nginx['listen_https'] = false
    gitlab_rails['gitlab_shell_ssh_port'] = 22222
    gitlab_rails['initial_root_password'] = 'changeme123'
    puma['port'] = 8181
```

> 注释中保留了 OAuth2 通用 provider 配置，用于将 Lifecycle 作为 GitLab 的外部 OAuth2 认证源（开发中功能）。

### MySQL

- 数据库 `harness`：Lifecycle 业务数据
- 数据库 `ApolloConfigDB` / `ApolloPortalDB`：Apollo 配置中心数据（通过 `apollo-sql/` 初始化）

### Lifecycle

环境变量覆盖（`docker-compose.yml`）：

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/harness?useSSL=false&allowPublicKeyRetrieval=true
  SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  GITLAB_URL: http://gitlab.harness.ai
  GITLAB_ADMIN_TOKEN: ${GITLAB_ADMIN_TOKEN}
```

### Apollo Quick Start

使用 `nobodyiam/apollo-quick-start` 一体化镜像，包含：
- **Portal** (:8070)：配置管理界面
- **Admin Service** (:8090)：配置修改接口
- **Config Service** (:8080)：配置读取接口 + Eureka 注册中心

---

## 数据持久化

通过 Docker named volumes 持久化，容器重建不丢失数据：

| Volume | 服务 | 数据内容 |
|--------|------|----------|
| `gitlab-config` | GitLab | 系统配置 |
| `gitlab-logs` | GitLab | 日志 |
| `gitlab-data` | GitLab | 仓库数据 |
| `runner-config` | Runner | 注册信息 |
| `mysql-data` | MySQL | 数据库 |
| `registry-data` | Registry | 镜像数据 |

运行时生成的本地目录（已加入 `.gitignore`）：

```
deploy/
├── gitlab/data/          # GitLab 运行时数据
├── gitlab/logs/          # GitLab 日志
└── runner/config/        # Runner 配置
```

---

## Registry 使用

Registry 已启用 **Basic Auth**，默认账号 `admin` / `admin`。

### 登录

```bash
docker login registry.harness.ai
# 输入用户名 admin，密码 admin
```

### 推送镜像

```bash
# 标记镜像
docker tag my-image:latest registry.harness.ai/my-image:latest

# 推送
docker push registry.harness.ai/my-image:latest
```

### 拉取镜像

```bash
docker pull registry.harness.ai/my-image:latest
```

### 重新生成密码

```bash
docker run --rm httpd:2 htpasswd -Bbn admin <新密码> > deploy/registry-auth/htpasswd
docker compose restart registry
```

---

## 停止与清理

```bash
cd deploy

# 停止所有服务，保留数据
docker compose down

# 停止并清除所有数据（包括 named volumes）
docker compose down -v

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f <service-name>
```

---

## 配置说明

### 环境变量（`.env`）

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `GITLAB_HOME` | 否 | `./gitlab` | GitLab 数据目录 |
| `RUNNER_HOME` | 否 | `./runner` | Runner 数据目录 |
| `GITLAB_PORT` | 否 | `80` | GitLab 内部 HTTP 端口 |
| `GITLAB_SSH_PORT` | 否 | `22222` | GitLab SSH 端口映射 |
| `GITLAB_ROOT_PASSWORD` | 是 | - | root 初始密码 |
| `MYSQL_ROOT_PASSWORD` | 是 | - | MySQL root 密码 |
| `GITLAB_ADMIN_TOKEN` | 是 | - | GitLab Admin PAT（用于 API） |
| `LIFECYCLE_IMAGE` | 否 | `lifecycle-biz:latest` | Lifecycle 镜像名 |

### Apollo 配置（`deploy/config/`）

| 文件 | 说明 |
|------|------|
| `application.properties` | Apollo 公共配置 |
| `lifecycle-biz.properties` | Lifecycle 默认配置（`gitlab.url`） |
| `lifecycle-biz-dev.properties` | Lifecycle 开发环境覆盖（数据库连接） |

---

## 故障排查

### GitLab 启动极慢或 OOM

**现象**：GitLab 容器反复重启，或页面 502。

**原因**：GitLab CE 最低需要 4GB 内存，建议 Docker Desktop 分配 8GB+。

**解决**：增加 Docker Desktop 内存限制，或减小 `puma['worker_processes']`。

### Runner 注册失败

**现象**：`register-runner.sh` 报错或卡住。

**原因**：GitLab 尚未完全初始化，或 API Token 权限不足。

**解决**：
1. 等待更长时间：`docker compose logs -f gitlab` 观察初始化进度
2. 手动检查 GitLab 状态：`curl -sf http://gitlab.harness.ai/users/sign_in`
3. 手动创建 Token 并注册

### Lifecycle 无法连接 MySQL

**现象**：Lifecycle 日志显示数据库连接超时。

**原因**：MySQL 尚未就绪，或环境变量未正确传递。

**解决**：
1. 确认 MySQL 健康：`docker compose ps mysql`
2. 检查环境变量：`docker compose exec lifecycle env | grep SPRING`
3. 手动测试连接：`docker compose exec mysql mysql -uroot -p -e "SHOW DATABASES;"`

### Apollo 配置未生效

**现象**：Lifecycle 使用了 `application.properties` 中的兜底值，而非 Apollo 配置。

**原因**：Apollo 配置未发布，或 `apollo.meta` 不可达。

**解决**：
1. 确认 Apollo Portal 中配置已发布
2. 检查 Lifecycle 日志中的 Apollo 连接状态
3. 临时关闭 Apollo：`apollo.bootstrap.enabled=false`

### 端口冲突

**现象**：`docker compose up` 报错 `bind: address already in use`。

**解决**：
```bash
# 查找占用进程
lsof -Pi :8080 -sTCP:LISTEN
# 或修改 .env / docker-compose.yml 中的端口映射
```

### CI 作业容器 DNS 解析失败

**现象**：CI pipeline 失败，日志显示：
```
fatal: unable to access 'http://gitlab.harness.ai/...': Could not resolve host: gitlab.harness.ai
```
或
```
Failed to connect to gitlab.harness.ai port 80: Couldn't connect to server
```

**原因**：
1. Docker 网络别名未正确配置
2. Runner 未配置使用 `deploy_devops` 网络
3. Nginx 容器未正确重启以应用网络别名

**排查步骤**：

1. **检查 Runner 网络配置**：
```bash
docker exec gitlab-runner cat /etc/gitlab-runner/config.toml | grep network_mode
# 应输出: network_mode = "deploy_devops"
```

2. **检查 Nginx 网络别名**：
```bash
docker inspect nginx --format='{{json .NetworkSettings.Networks.deploy_devops.Aliases}}'
# 应包含: ["nginx","gitlab.harness.ai","registry.harness.ai"]
```

3. **测试容器内 DNS 解析**：
```bash
docker exec lifecycle curl -I http://gitlab.harness.ai
# 应返回 HTTP 响应头
```

**解决方案**：

1. **重新创建 Nginx 容器**（如果网络别名未生效）：
```bash
cd deploy
docker compose up -d --force-recreate nginx
```

2. **重新注册 Runner**（如果网络模式未配置）：
```bash
docker exec gitlab-runner gitlab-runner unregister --all-runners
cd deploy && ./register-runner.sh
```

3. **验证修复**：
```bash
# 检查 Runner 配置
docker exec gitlab-runner cat /etc/gitlab-runner/config.toml | grep network_mode

# 触发 CI 测试
git commit --allow-empty -m "Test CI" && git push
```

> **注意**：使用 `docker restart nginx` 不会应用 docker-compose.yml 中的网络配置变更，必须使用 `docker compose up -d --force-recreate`。
